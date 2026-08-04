package neofontrender.addons.cjk

import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import neofontrender.api.text.CjkParagraphLayoutProvider
import org.tiqian.clreq.CjkPunctuationGlyphPolicy
import org.tiqian.clreq.ClreqProfile
import org.tiqian.clreq.ClreqProfileResolver
import org.tiqian.clreq.HangingPunctuationStyle
import org.tiqian.clreq.KinsokuLevel
import org.tiqian.clreq.KinsokuMode
import org.tiqian.core.Cluster
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
import java.util.TreeMap

/** Tiqian-backed paragraph provider kept entirely inside the optional UIE addon. */
object TiqianParagraphProvider : CjkParagraphLayoutProvider {
    private const val CACHE_LIMIT = 512
    private const val PROFILE_LOCALE = "zh-Hans"

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
        val parsed = ParsedText.parse(request.formattedText())
        if (!containsCjk(parsed.visible)) return null
        val key = CacheKey(
            request.formattedText(), request.maxWidth(), request.lineHeight(),
            normalizeLanguage(request.languageCode()), metricProbe(request.measurer()),
        )
        synchronized(cache) { cache[key]?.let { return it } }
        val built = buildLayout(parsed, request.maxWidth(), request.lineHeight(), request.measurer())
        synchronized(cache) { cache[key] = built.layout }
        return built.layout
    }

    override fun splitComponents(
        request: CjkParagraphLayoutProvider.ComponentRequest,
    ): List<ITextComponent>? {
        if (!enabledFor(request.languageCode())) return null
        if (request.surface() == CjkParagraphLayoutProvider.ComponentRequest.Surface.DEFAULT) return null

        val segments = mutableListOf<ComponentSegment>()
        val formatted = StringBuilder()
        var visibleOffset = 0
        for (component in request.component()) {
            val text = component.unformattedComponentText
            if (text.isEmpty()) continue
            formatted.append('\u00a7').append('r')
                .append(component.style.formattingCode)
                .append(text)
            val end = visibleOffset + text.length
            segments += ComponentSegment(visibleOffset, end, text, component)
            visibleOffset = end
        }
        if (segments.isEmpty()) return listOf(TextComponentString(""))

        val parsed = ParsedText.parse(formatted.toString())
        if (!containsCjk(parsed.visible)) return null
        val built = buildLayout(
            parsed, request.maxWidth(), request.lineHeight(), request.measurer(), request.surface(),
        )
        return built.visibleLines.mapIndexed { lineIndex, range ->
            var start = range.start
            if (request.removeLeadingSpace() && start < range.end && parsed.visible[start] == ' ') {
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
                val child = TextComponentString(
                        segment.source.style.formattingCode +
                            segment.text.substring(localStart, localEnd),
                    )
                    .setStyle(segment.source.style.createDeepCopy())
                line.appendSibling(child)
                line.`nfrUi$addComponentSpan`(
                    overlapStart - start, overlapEnd - start, child,
                )
            }
            line
        }.ifEmpty { listOf(TextComponentString("")) }
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
        parsed: ParsedText,
        maxWidth: Int,
        lineHeight: Int,
        measurer: CjkParagraphLayoutProvider.TextMeasurer,
        surface: CjkParagraphLayoutProvider.ComponentRequest.Surface =
            CjkParagraphLayoutProvider.ComponentRequest.Surface.DEFAULT,
    ): BuiltLayout {
        if (parsed.visible.isEmpty()) {
            val line = CjkParagraphLayoutProvider.Line(0, 0, 0f, false, emptyList())
            return BuiltLayout(CjkParagraphLayoutProvider.Layout(listOf(line)), listOf(TextRange(0, 0)))
        }

        val em = maxOf(1f, measurer.measureFormatted("汉"))
        val baseStyle = TextStyle(
            fontFamilies = listOf("minecraft"),
            fontSize = em,
            locale = PROFILE_LOCALE,
        )
        val spans = parsed.styleRanges().map { (range, state) ->
            TextSpan(
                range,
                baseStyle.copy(
                    fontWeight = if (state.bold) 700 else 400,
                    italic = state.italic,
                ),
            )
        }
        val content = TiqianTextContent(
            text = parsed.visible,
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
            val runs = positionedByLine[lineIndex].orEmpty().mapNotNull { positioned ->
                val cluster = result.clusters[positioned.clusterIndex]
                if (cluster.displayText.isEmpty() || cluster.displayText == "\n" ||
                    cluster.displayText == "\r"
                ) {
                    null
                } else {
                    CjkParagraphLayoutProvider.Run(
                        parsed.formattedDisplay(cluster.range.start, cluster.displayText),
                        positioned.drawX,
                        parsed.rawBoundary(cluster.range.start),
                        parsed.rawBoundary(cluster.range.end),
                    )
                }
            }.toMutableList()
            if (line.hyphenAdvance > 0f) {
                val boundary = parsed.rawBoundary(line.range.end)
                val styleOffset = (line.range.end - 1).coerceAtLeast(line.range.start)
                runs += CjkParagraphLayoutProvider.Run(
                    parsed.formattedDisplay(styleOffset, "-"),
                    line.indent + line.visualWidth,
                    boundary,
                    boundary,
                )
            }
            CjkParagraphLayoutProvider.Line(
                parsed.rawBoundary(line.range.start),
                parsed.rawBoundary(line.range.end),
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

    private class HostTextShaper(
        private val parsed: ParsedText,
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
        val text: String,
        val source: ITextComponent,
    )

    private data class CacheKey(
        val text: String,
        val maxWidth: Int,
        val lineHeight: Int,
        val language: String,
        val metricProbe: Int,
    )

    private data class FormatState(
        var color: Char? = null,
        var random: Boolean = false,
        var bold: Boolean = false,
        var strike: Boolean = false,
        var underline: Boolean = false,
        var italic: Boolean = false,
    ) {
        fun apply(codeValue: Char) {
            val code = codeValue.lowercaseChar()
            if (code in "0123456789abcdef") {
                color = code
                random = false
                bold = false
                strike = false
                underline = false
                italic = false
            } else {
                when (code) {
                    'k' -> random = true
                    'l' -> bold = true
                    'm' -> strike = true
                    'n' -> underline = true
                    'o' -> italic = true
                    'r' -> {
                        color = null
                        random = false
                        bold = false
                        strike = false
                        underline = false
                        italic = false
                    }
                }
            }
        }

        fun prefix(): String = buildString {
            color?.let { append('\u00a7').append(it) }
            if (random) append("\u00a7k")
            if (bold) append("\u00a7l")
            if (strike) append("\u00a7m")
            if (underline) append("\u00a7n")
            if (italic) append("\u00a7o")
        }
    }

    private class ParsedText private constructor(
        val raw: String,
        val visible: String,
        private val rawBoundaries: IntArray,
        private val states: TreeMap<Int, FormatState>,
    ) {
        fun rawBoundary(visibleOffset: Int): Int =
            rawBoundaries[visibleOffset.coerceIn(0, rawBoundaries.lastIndex)]

        fun stateAt(visibleOffset: Int): FormatState =
            states.floorEntry(visibleOffset)?.value ?: FormatState()

        fun formattedDisplay(visibleStart: Int, displayText: String): String =
            stateAt(visibleStart).prefix() + displayText

        fun codePointRanges(): List<TextRange> {
            val ranges = mutableListOf<TextRange>()
            var offset = 0
            while (offset < visible.length) {
                val next = offset + Character.charCount(visible.codePointAt(offset))
                ranges += TextRange(offset, next)
                offset = next
            }
            return ranges
        }

        fun styleRanges(): List<Pair<TextRange, FormatState>> {
            if (visible.isEmpty()) return emptyList()
            val ranges = mutableListOf<Pair<TextRange, FormatState>>()
            var start = 0
            var state = stateAt(0)
            var offset = Character.charCount(visible.codePointAt(0))
            while (offset < visible.length) {
                val nextState = stateAt(offset)
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

        companion object {
            fun parse(raw: String): ParsedText {
                val visible = StringBuilder()
                val rawBoundaries = mutableListOf(0)
                val states = TreeMap<Int, FormatState>()
                val state = FormatState()
                var rawOffset = 0
                while (rawOffset < raw.length) {
                    if (raw[rawOffset] == '\u00a7' && rawOffset + 1 < raw.length) {
                        state.apply(raw[rawOffset + 1])
                        rawOffset += 2
                        continue
                    }
                    val codePoint = raw.codePointAt(rawOffset)
                    val count = Character.charCount(codePoint)
                    states[visible.length] = state.copy()
                    visible.appendCodePoint(codePoint)
                    repeat(count) { index -> rawBoundaries += rawOffset + index + 1 }
                    rawOffset += count
                }
                states[visible.length] = state.copy()
                return ParsedText(
                    raw,
                    visible.toString(),
                    rawBoundaries.toIntArray(),
                    states,
                )
            }
        }
    }
}
