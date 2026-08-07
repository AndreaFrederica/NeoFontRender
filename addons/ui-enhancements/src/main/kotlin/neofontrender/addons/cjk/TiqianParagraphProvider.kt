package neofontrender.addons.cjk

import net.minecraft.util.IChatComponent
import net.minecraft.util.ChatComponentText
import neofontrender.api.text.CjkParagraphLayoutProvider
import neofontrender.core.font.preprocess.LayoutText
import org.tiqian.clreq.CjkPunctuationGlyphPolicy
import org.tiqian.clreq.ClreqProfile
import org.tiqian.clreq.ClreqProfileResolver
import org.tiqian.clreq.HangingPunctuationStyle
import org.tiqian.clreq.KinsokuLevel
import org.tiqian.clreq.KinsokuMode
import org.tiqian.core.Cluster
import org.tiqian.core.DecorationKind
import org.tiqian.core.DecorationSpan
import org.tiqian.core.Glyph
import org.tiqian.core.GlyphRun
import org.tiqian.core.Ic
import org.tiqian.core.LayoutConstraints
import org.tiqian.core.LayoutInput
import org.tiqian.core.LineEndReason
import org.tiqian.core.LineLengthGrid
import org.tiqian.core.ParagraphStyle
import org.tiqian.core.TextRange
import org.tiqian.core.TextSpan
import org.tiqian.core.TextStyle
import org.tiqian.core.TiqianTextContent
import org.tiqian.core.positionedClusters
import org.tiqian.layout.ExplainableStubParagraphLayoutEngine
import org.tiqian.linebreak.EnglishHyphenation
import org.tiqian.shaping.ShapingInput
import org.tiqian.shaping.ShapingResult
import org.tiqian.shaping.TextShaper
import java.util.LinkedHashMap
import java.util.Locale
import kotlin.math.abs

/** Tiqian-backed paragraph provider kept entirely inside the optional UIE addon. */
object TiqianParagraphProvider : CjkParagraphLayoutProvider {
    private const val CACHE_LIMIT = 512
    private const val PROFILE_LOCALE = "zh-Hans"
    private val chatSpeakerPrefix = Regex(
        "^\\s*(?:\\[[^\\]\\r\\n]{1,32}]\\s*)*(<[^<>\\s]{1,64}>)",
    )

    private val cache = object : LinkedHashMap<CacheKey, CjkParagraphLayoutProvider.Layout>(
        CACHE_LIMIT + 1, 0.75f, true,
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<CacheKey, CjkParagraphLayoutProvider.Layout>,
        ): Boolean = size > CACHE_LIMIT
    }

    override fun id(): String = "tiqian"

    override fun priority(): Int = 100

    override fun layout(
        request: CjkParagraphLayoutProvider.Request,
    ): CjkParagraphLayoutProvider.Layout? {
        if (!enabledFor(request.languageCode())) return null
        val parsed = LayoutText.process(request.formattedText())
        if (!containsCjk(parsed.visibleText())) return null
        val key = CacheKey(
            request.formattedText(), request.maxWidth(), request.lineHeight(),
            normalizeLanguage(request.languageCode()), metricProbe(request.measurer()),
            parsed.fingerprint(),
        )
        synchronized(cache) { cache[key]?.let { return it } }
        val built = buildLayout(parsed, request.maxWidth(), request.lineHeight(), request.measurer())
        synchronized(cache) { cache[key] = built.layout }
        return built.layout
    }

    override fun splitComponents(
        request: CjkParagraphLayoutProvider.ComponentRequest,
    ): List<IChatComponent>? {
        if (!enabledFor(request.languageCode())) return null
        if (request.surface() == CjkParagraphLayoutProvider.ComponentRequest.Surface.DEFAULT) return null

        val segments = mutableListOf<ComponentSegment>()
        val formatted = StringBuilder()
        var visibleOffset = 0
        for (component in request.component()) {
            val chatComponent = component as IChatComponent
            val text = chatComponent.getUnformattedTextForChat()
            if (text.isEmpty()) continue
            val componentLayout = LayoutText.process(text)
            formatted.append('\u00a7').append('r')
                .append(chatComponent.getChatStyle().getFormattingCode())
                .append(text)
            val end = visibleOffset + componentLayout.visibleText().length
            segments += ComponentSegment(visibleOffset, end, componentLayout, chatComponent)
            visibleOffset = end
        }
        if (segments.isEmpty()) return listOf(ChatComponentText(""))

        val parsed = LayoutText.process(formatted.toString())
        if (!containsCjk(parsed.visibleText())) return null
        val built = buildLayout(
            parsed, request.maxWidth(), request.lineHeight(), request.measurer(), request.surface(),
        )
        return built.visibleLines.mapIndexed { lineIndex, range ->
            var start = range.start
            if (request.removeLeadingSpace() && start < range.end && parsed.visibleText()[start] == ' ') {
                start++
            }
            val apiLine = built.layout.lines()[lineIndex]
            val line = TiqianLineComponent(
                apiLine.runs(), built.lineWidths[lineIndex], range.end - start,
            )
            for (cell in built.positionedLines[lineIndex]) {
                val overlapStart = maxOf(start, cell.range.start)
                val overlapEnd = minOf(range.end, cell.range.end)
                if (overlapStart < overlapEnd) {
                    line.`nfrUi$addCell`(
                        overlapStart - start, overlapEnd - start, cell.left, cell.right,
                    )
                }
            }
            for (segment in segments) {
                val overlapStart = maxOf(start, segment.start)
                val overlapEnd = minOf(range.end, segment.end)
                if (overlapStart >= overlapEnd) continue
                val localStart = overlapStart - segment.start
                val localEnd = overlapEnd - segment.start
                val display = segment.layout.visibleText().substring(localStart, localEnd)
                val child = ChatComponentText(segment.source.getChatStyle().getFormattingCode() +
                        segment.layout.formattedDisplay(localStart, display))
                    .setChatStyle(segment.source.getChatStyle().createDeepCopy())
                line.appendSibling(child)
                line.`nfrUi$addComponentSpan`(
                    overlapStart - start, overlapEnd - start, child,
                )
            }
            line
        }.ifEmpty { listOf(ChatComponentText("")) }
    }

    fun clearCache() {
        synchronized(cache) { cache.clear() }
    }

    private fun enabledFor(languageCode: String): Boolean =
        CjkTypographyConfig.tiqianEnabled() && normalizeLanguage(languageCode) in
            setOf("zh_cn", "zh_hans", "zh_sg")

    private fun normalizeLanguage(languageCode: String): String =
        languageCode.trim().lowercase(Locale.ROOT).replace('-', '_')

    private fun containsCjk(text: String): Boolean {
        var offset = 0
        while (offset < text.length) {
            val codePoint = text.codePointAt(offset)
            val script = Character.UnicodeScript.of(codePoint)
            if (script == Character.UnicodeScript.HAN ||
                script == Character.UnicodeScript.HIRAGANA ||
                script == Character.UnicodeScript.KATAKANA ||
                codePoint in 0x3000..0x303F || codePoint in 0xFF00..0xFFEF
            ) return true
            offset += Character.charCount(codePoint)
        }
        return false
    }

    private fun metricProbe(measurer: CjkParagraphLayoutProvider.TextMeasurer): Int {
        var hash = 1
        for (sample in listOf("汉", "Aa", "，。", "\u00a7l汉")) {
            hash = 31 * hash + measurer.measureFormatted(sample).toBits()
        }
        return hash
    }

    private fun buildLayout(
        parsed: LayoutText,
        maxWidth: Int,
        lineHeight: Int,
        measurer: CjkParagraphLayoutProvider.TextMeasurer,
        surface: CjkParagraphLayoutProvider.ComponentRequest.Surface =
            CjkParagraphLayoutProvider.ComponentRequest.Surface.DEFAULT,
    ): BuiltLayout {
        if (parsed.visibleText().isEmpty()) {
            val line = CjkParagraphLayoutProvider.Line(0, 0, 0f, false, emptyList())
            return BuiltLayout(CjkParagraphLayoutProvider.Layout(listOf(line)), listOf(TextRange(0, 0)))
        }

        val em = maxOf(1f, measurer.measureFormatted("汉"))
        val baseStyle = TextStyle(
            fontFamilies = listOf("minecraft"),
            fontSize = em,
            locale = PROFILE_LOCALE,
        )
        val spans = styleRanges(parsed).map { (range, state) ->
            TextSpan(
                range,
                baseStyle.copy(
                    fontWeight = if (state.bold()) 700 else 400,
                    italic = state.italic(),
                ),
            )
        }
        val content = TiqianTextContent(
            text = parsed.visibleText(),
            spans = spans,
        )
        var guiProfile = ClreqProfile.MainlandHorizontal.copy(
            punctuationGlyphPolicy = CjkPunctuationGlyphPolicy.PreserveInput,
        )
        if (surface == CjkParagraphLayoutProvider.ComponentRequest.Surface.CHAT) {
            guiProfile = guiProfile.copy(
                kinsokuMode = KinsokuMode.Fixed(
                    KinsokuLevel.Basic, HangingPunctuationStyle.Disabled,
                ),
            )
        }
        val engine = ExplainableStubParagraphLayoutEngine(
            clreqProfileResolver = ClreqProfileResolver { guiProfile },
            textShaper = HostTextShaper(parsed, measurer),
            hyphenator = EnglishHyphenation.enUs,
        )
        val result = engine.layout(
            LayoutInput(
                content = content,
                textStyle = baseStyle,
                // Tiqian already gives Mourning spans the exact semantics chat needs here:
                // keep the range whole when it fits, but permit emergency splitting when the
                // range itself is wider than the viewport. UIE does not paint decorations.
                decorations = chatSpeakerDecoration(parsed.visibleText(), surface),
                paragraphStyle = ParagraphStyle(
                    lineHeight = lineHeight.toFloat(),
                    firstLineIndent = Ic.Zero,
                    lineLengthGrid = LineLengthGrid(enabled = false),
                ),
                constraints = LayoutConstraints(maxWidth.toFloat()),
            ),
        )
        val positionedByLine = result.positionedClusters().groupBy { it.lineIndex }
        val apiLines = result.lines.mapIndexed { lineIndex, line ->
            val runs = mutableListOf<CjkParagraphLayoutProvider.Run>()
            var pendingText = StringBuilder()
            var pendingStart = -1
            var pendingEnd = -1
            var pendingX = 0f
            var pendingRight = 0f
            var pendingState: LayoutText.State? = null

            fun flushPending() {
                if (pendingStart < 0) return
                runs += CjkParagraphLayoutProvider.Run(
                    parsed.formattedDisplay(pendingStart, pendingText.toString()),
                    pendingX,
                    parsed.rawStartBoundary(pendingStart),
                    parsed.rawEndBoundary(pendingEnd),
                )
                pendingText = StringBuilder()
                pendingStart = -1
                pendingEnd = -1
                pendingState = null
            }

            for (positioned in positionedByLine[lineIndex].orEmpty()) {
                val cluster = result.clusters[positioned.clusterIndex]
                if (cluster.displayText.isEmpty() || cluster.displayText == "\n" ||
                    cluster.displayText == "\r"
                ) {
                    flushPending()
                    continue
                }
                val state = parsed.stateAt(cluster.range.start)
                val joinsPending = pendingStart >= 0 && pendingEnd == cluster.range.start &&
                    pendingState == state && (state.underline() || state.strike()) &&
                    abs(positioned.drawX - pendingRight) <= 0.01f
                if (!joinsPending) {
                    flushPending()
                    pendingStart = cluster.range.start
                    pendingX = positioned.drawX
                    pendingState = state
                }
                pendingText.append(cluster.displayText)
                pendingEnd = cluster.range.end
                pendingRight = positioned.drawX + cluster.advance
            }
            flushPending()
            if (line.hyphenAdvance > 0f) {
                val boundary = parsed.rawEndBoundary(line.range.end)
                val styleOffset = (line.range.end - 1).coerceAtLeast(line.range.start)
                runs += CjkParagraphLayoutProvider.Run(
                    parsed.formattedDisplay(styleOffset, "-"),
                    line.indent + line.visualWidth,
                    boundary,
                    boundary,
                )
            }
            CjkParagraphLayoutProvider.Line(
                parsed.rawStartBoundary(line.range.start),
                parsed.rawEndBoundary(line.range.end),
                lineIndex * lineHeight.toFloat(),
                line.endReason == LineEndReason.MandatoryBreak,
                runs,
            )
        }
        return BuiltLayout(
            CjkParagraphLayoutProvider.Layout(apiLines),
            result.lines.map { it.range },
            result.lines.mapIndexedNotNull { index, line ->
                index.takeIf { line.hyphenAdvance > 0f }
            }.toSet(),
            result.lines.mapIndexed { index, line ->
                positionedByLine[index].orEmpty().map { positioned ->
                    PositionedCell(positioned.range, positioned.left, positioned.right)
                }
            },
            result.lines.map { line ->
                line.indent + line.visualWidth + line.hyphenAdvance
            },
        )
    }

    private fun chatSpeakerDecoration(
        text: String,
        surface: CjkParagraphLayoutProvider.ComponentRequest.Surface,
    ): List<DecorationSpan> {
        if (surface != CjkParagraphLayoutProvider.ComponentRequest.Surface.CHAT) return emptyList()
        val token = chatSpeakerPrefix.find(text)?.groups?.get(1) ?: return emptyList()
        return listOf(
            DecorationSpan(
                TextRange(token.range.first, token.range.last + 1),
                DecorationKind.Mourning,
            ),
        )
    }

    private class HostTextShaper(
        private val parsed: LayoutText,
        private val measurer: CjkParagraphLayoutProvider.TextMeasurer,
    ) : TextShaper {
        override fun shape(input: ShapingInput): ShapingResult {
            val source = input.text.substring(input.range.start, input.range.end)
            val formatted = parsed.formattedDisplay(input.range.start, input.displayText)
            val advance = maxOf(0f, measurer.measureFormatted(formatted))
            val fontKey = input.fontDecision.candidate.key
            val cluster = Cluster(
                range = input.range,
                text = source,
                displayText = input.displayText,
                fontKey = fontKey,
                advance = advance,
            )
            val glyph = Glyph(
                id = 0u,
                clusterRange = input.range,
                advance = advance,
            )
            return ShapingResult(
                clusters = listOf(cluster),
                glyphRuns = listOf(
                    GlyphRun(
                        range = input.range,
                        fontKey = fontKey,
                        glyphs = listOf(glyph),
                        advance = advance,
                    ),
                ),
            )
        }
    }

    private data class BuiltLayout(
        val layout: CjkParagraphLayoutProvider.Layout,
        val visibleLines: List<TextRange>,
        val hyphenatedLines: Set<Int> = emptySet(),
        val positionedLines: List<List<PositionedCell>> = emptyList(),
        val lineWidths: List<Float> = emptyList(),
    )

    private data class PositionedCell(
        val range: TextRange,
        val left: Float,
        val right: Float,
    )

    private data class ComponentSegment(
        val start: Int,
        val end: Int,
        val layout: LayoutText,
        val source: IChatComponent,
    )

    private data class CacheKey(
        val text: String,
        val maxWidth: Int,
        val lineHeight: Int,
        val language: String,
        val metricProbe: Int,
        val layoutFingerprint: Int,
    )

    private fun styleRanges(parsed: LayoutText): List<Pair<TextRange, LayoutText.State>> {
        val visible = parsed.visibleText()
        if (visible.isEmpty()) return emptyList()
        val ranges = mutableListOf<Pair<TextRange, LayoutText.State>>()
        var start = 0
        var state = parsed.stateAt(0)
        var offset = Character.charCount(visible.codePointAt(0))
        while (offset < visible.length) {
            val nextState = parsed.stateAt(offset)
            if (nextState != state) {
                ranges += TextRange(start, offset) to state
                start = offset
                state = nextState
            }
            offset += Character.charCount(visible.codePointAt(offset))
        }
        ranges += TextRange(start, visible.length) to state
        return ranges
    }
}
