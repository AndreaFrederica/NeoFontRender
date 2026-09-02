package neofontrender.addons.cjk;

import neofontrender.api.text.pipeline.ParagraphLayoutMiddleware;
import neofontrender.api.text.pipeline.ProcessedText;
import neofontrender.api.text.pipeline.TextPipelineApi;
import neofontrender.api.text.pipeline.TextMiddlewareRegistration;
import neofontrender.core.font.pipeline.builtin.TinkersAntiqueTextPreprocessor;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tiqian.linebreak.EnglishHyphenation;

import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TiqianParagraphProviderTest {
    private static final Set<Integer> FORBIDDEN_START = Set.of(
            (int) '，', (int) '。', (int) '！', (int) '？', (int) '）', (int) '》');
    private TextMiddlewareRegistration tinkersRegistration;

    @BeforeEach
    void enableTiqian() {
        CjkTypographyConfig.engine = CjkTypographyConfig.ENGINE_TIQIAN;
        tinkersRegistration = TextPipelineApi.register(TinkersAntiqueTextPreprocessor.INSTANCE);
        TiqianParagraphProvider.INSTANCE.clearCache();
    }

    @AfterEach
    void clearCache() {
        TiqianParagraphProvider.INSTANCE.clearCache();
        tinkersRegistration.close();
    }

    @Test
    void laysOutSimplifiedChineseWithKinsokuBoundaries() {
        String text = "这是第一句，后面还有第二句。中文排版需要避免标点出现在行首。";
        ParagraphLayoutMiddleware.Layout layout = TiqianParagraphProvider.INSTANCE.layout(
                request(text, 45, "zh_cn"));

        assertNotNull(layout);
        assertTrue(layout.lines().size() > 1);
        for (int index = 1; index < layout.lines().size(); index++) {
            int start = layout.lines().get(index).rawStart();
            if (start < text.length()) {
                assertFalse(FORBIDDEN_START.contains(text.codePointAt(start)));
            }
        }
    }

    @Test
    void preservesRawFormattingBoundaries() {
        String text = "中文\u00a7l粗体中文，继续显示";
        ParagraphLayoutMiddleware.Layout layout = TiqianParagraphProvider.INSTANCE.layout(
                request(text, 27, "zh_cn"));

        assertNotNull(layout);
        int boundary = layout.firstRawBoundary(text.length());
        assertTrue(boundary >= 0 && boundary <= text.length());
        assertFalse(boundary > 0 && text.charAt(boundary - 1) == '\u00a7');
    }

    @Test
    void registersThroughThePublicTextPipeline() {
        try (TextMiddlewareRegistration registration =
                     TextPipelineApi.register(TiqianParagraphProvider.INSTANCE)) {
            assertEquals("neofontrender_ui_enhancements:tiqian",
                    TiqianParagraphProvider.INSTANCE.id());
        }
    }

    @Test
    void treatsEnabledTinkersRgbMarkersAsZeroWidthLayoutState() {
        String marker = tinkersRgb(0x12, 0x80, 0xFE);
        String text = "中" + marker + "文着色";
        ParagraphLayoutMiddleware.Layout layout = TiqianParagraphProvider.INSTANCE.layout(
                request(text, 100, "zh_cn"));

        assertNotNull(layout);
        assertEquals(text.length(), layout.lines().get(layout.lines().size() - 1).rawEnd());
        StringBuilder visible = new StringBuilder();
        boolean foundColor = false;
        for (ParagraphLayoutMiddleware.Line line : layout.lines()) {
            for (ParagraphLayoutMiddleware.Run run : line.runs()) {
                ProcessedText decoded = TextPipelineApi.processRaw(run.formattedText());
                visible.append(decoded.visibleText());
                foundColor |= decoded.modernText().runs().stream().anyMatch(modernRun ->
                        modernRun.hasColorOverride() && modernRun.rgb() == 0x1280FE);
            }
        }
        assertEquals("中文着色", visible.toString());
        assertTrue(foundColor);
    }

    @Test
    void laysOutRealTinkersDurabilityTextWithoutExpandingColorRuns() {
        String text = "\u8010\u4e45: " + tinkersRgb(0x5B, 0xCC, 0x47) + "1,224"
                + "\u00a77/" + tinkersRgb(0x47, 0xCC, 0x47) + "1,320\u00a7r\u00a7r";
        ParagraphLayoutMiddleware.Layout layout = TiqianParagraphProvider.INSTANCE.layout(
                request(text, 1_000_000, "zh_cn"));

        assertNotNull(layout);
        assertEquals(1, layout.lines().size());
        List<ParagraphLayoutMiddleware.Run> runs = layout.lines().get(0).runs();
        StringBuilder visible = new StringBuilder();
        float renderedRight = 0.0F;
        boolean foundCurrent = false;
        boolean foundMaximum = false;
        boolean foundGraySlash = false;
        for (ParagraphLayoutMiddleware.Run run : runs) {
            ProcessedText decoded = TextPipelineApi.processRaw(run.formattedText());
            visible.append(decoded.visibleText().replace("\u00a77", "").replace("\u00a7r", ""));
            renderedRight = Math.max(renderedRight,
                    run.xOffset() + measure(run.formattedText()));
            foundCurrent |= decoded.modernText().runs().stream().anyMatch(value ->
                    value.hasColorOverride() && value.rgb() == 0x5BCC47);
            foundMaximum |= decoded.modernText().runs().stream().anyMatch(value ->
                    value.hasColorOverride() && value.rgb() == 0x47CC47);
            foundGraySlash |= decoded.modernText().runs().stream().anyMatch(value ->
                    !value.hasColorOverride() && value.text().contains("\u00a77/"));
        }

        assertEquals("\u8010\u4e45: 1,224/1,320", visible.toString());
        assertTrue(foundCurrent);
        assertTrue(foundMaximum);
        assertTrue(foundGraySlash);
        assertTrue(renderedRight <= measure(text) + 10.0F,
                "Tinkers color runs expanded to " + renderedRight
                        + "px from a natural " + measure(text) + "px line");
    }

    @Test
    void publicParagraphDispatchPreservesHeiPrefixedTinkersRgbRuns() {
        String text = "\u00a77\u8010\u4e45: " + tinkersRgb(0x5B, 0xCC, 0x47) + "1,224"
                + "\u00a77/" + tinkersRgb(0x47, 0xCC, 0x47) + "1,320\u00a7r\u00a7r";
        ParagraphLayoutMiddleware.Layout layout;
        try (TextMiddlewareRegistration registration =
                     TextPipelineApi.register(TiqianParagraphProvider.INSTANCE)) {
            layout = TextPipelineApi.layoutParagraph(
                    request(text, 1_000_000, "zh_cn"));
        }

        assertNotNull(layout);
        assertEquals(1, layout.lines().size());
        boolean foundCurrent = false;
        boolean foundMaximum = false;
        StringBuilder visible = new StringBuilder();
        for (ParagraphLayoutMiddleware.Run run : layout.lines().get(0).runs()) {
            assertNoPartialTinkersMarker(run.formattedText());
            ProcessedText decoded = TextPipelineApi.processRaw(run.formattedText());
            visible.append(decoded.visibleText());
            foundCurrent |= decoded.modernText().runs().stream().anyMatch(value ->
                    value.hasColorOverride() && value.rgb() == 0x5BCC47);
            foundMaximum |= decoded.modernText().runs().stream().anyMatch(value ->
                    value.hasColorOverride() && value.rgb() == 0x47CC47);
        }

        assertEquals("\u8010\u4e45: 1,224/1,320",
                TextFormatting.getTextWithoutFormattingCodes(visible.toString()));
        assertTrue(foundCurrent);
        assertTrue(foundMaximum);
    }

    @Test
    void fallsBackForUnsupportedLocaleOrLegacyMode() {
        assertNull(TiqianParagraphProvider.INSTANCE.layout(request("中文测试", 30, "zh_tw")));
        assertNull(TiqianParagraphProvider.INSTANCE.layout(
                request("This is an English-only HUD message", 30, "zh_cn")));
        CjkTypographyConfig.engine = CjkTypographyConfig.ENGINE_LEGACY;
        assertNull(TiqianParagraphProvider.INSTANCE.layout(request("中文测试", 30, "zh_cn")));
    }

    @Test
    void drawsAndExportsSyntheticEnglishHyphensInMixedText() {
        String text = "请运行 internationalization 命令";
        List<Integer> points = EnglishHyphenation.INSTANCE.getEnUs()
                .hyphenate("internationalization");
        assertFalse(points.isEmpty(), "bundled en-US patterns did not load");
        ParagraphLayoutMiddleware.Layout layout = TiqianParagraphProvider.INSTANCE.layout(
                request(text, 90, "zh_cn"));

        assertNotNull(layout);
        assertTrue(layout.lines().stream()
                .flatMap(line -> line.runs().stream())
                .anyMatch(run -> run.formattedText().endsWith("-")),
                () -> layout.lines().stream()
                        .map(line -> line.runs().stream()
                                .map(ParagraphLayoutMiddleware.Run::formattedText)
                                .reduce("", String::concat))
                        .reduce("", (left, right) -> left + "|" + right));

        TextComponentString component = new TextComponentString(text);
        List<ITextComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new ParagraphLayoutMiddleware.ComponentRequest(component, 90, 9,
                        "zh_cn", true, true, TiqianParagraphProviderTest::measure,
                        ParagraphLayoutMiddleware.ComponentRequest.Surface.BOOK));
        assertNotNull(lines);
        assertFalse(lines.stream().anyMatch(line -> line.getUnformattedText().contains("-")));
    }

    @Test
    void componentSplitKeepsFormattingAndInteractionStyle() {
        TextComponentString component = new TextComponentString("可点击的中文书页内容");
        component.getStyle().setColor(TextFormatting.RED).setClickEvent(
                new ClickEvent(ClickEvent.Action.CHANGE_PAGE, "2"));

        List<ITextComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new ParagraphLayoutMiddleware.ComponentRequest(component, 45, 9,
                        "zh_cn", true, true, TiqianParagraphProviderTest::measure,
                        ParagraphLayoutMiddleware.ComponentRequest.Surface.BOOK));

        assertNotNull(lines);
        assertTrue(lines.size() > 1);
        ITextComponent firstStyled = lines.get(0).iterator().next();
        if (firstStyled.getUnformattedComponentText().isEmpty()
                && lines.get(0).iterator().hasNext()) {
            java.util.Iterator<ITextComponent> iterator = lines.get(0).iterator();
            iterator.next();
            firstStyled = iterator.next();
        }
        assertTrue(lines.get(0).getUnformattedText().contains("\u00a7c"));
        assertEquals("2", firstStyled.getStyle().getClickEvent().getValue());
    }

    @Test
    void chatSplitKeepsEveryLineInsideTheViewportWithoutChangingCopiedText() {
        String text = "<Nullpinter> " + "我草这对吗".repeat(13)
                + "，，，，，请运行 internationalization 命令";
        TextComponentString component = new TextComponentString(text);

        List<ITextComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new ParagraphLayoutMiddleware.ComponentRequest(component, 180, 9,
                        "zh_cn", false, true, TiqianParagraphProviderTest::measure,
                        ParagraphLayoutMiddleware.ComponentRequest.Surface.CHAT));

        assertNotNull(lines);
        assertTrue(lines.size() > 1);
        StringBuilder copied = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            ITextComponent line = lines.get(index);
            assertTrue(line instanceof PositionedTextLine);
            PositionedTextLine positioned = (PositionedTextLine) line;
            assertTrue(positioned.nfrUi$width() <= 180.01F,
                    () -> "line width was " + positioned.nfrUi$width());
            float renderedRight = positioned.nfrUi$runs().stream()
                    .map(run -> run.xOffset() + measure(run.formattedText()))
                    .max(Float::compareTo).orElse(0.0F);
            assertTrue(renderedRight <= 180.01F,
                    () -> "rendered right edge was " + renderedRight);
            assertTrue(positioned.nfrUi$visibleOffsetAt(positioned.nfrUi$width())
                    <= TextFormatting.getTextWithoutFormattingCodes(
                            line.getUnformattedText()).length());

            String clean = TextFormatting.getTextWithoutFormattingCodes(
                    line.getUnformattedText());
            copied.append(clean);
            if (index > 0 && !clean.isEmpty()) {
                assertFalse(FORBIDDEN_START.contains(clean.codePointAt(0)), clean);
            }
        }
        assertEquals(text, copied.toString());
    }

    @Test
    void chatKeepsAngleBracketPlayerNameWholeWhenItFits() {
        assertChatTokenIsNotSplit("<Nullpinter> 中文消息", "<Nullpinter>", 65);
    }

    @Test
    void chatKeepsPlayerNameWholeAfterTimestampPrefix() {
        assertChatTokenIsNotSplit("[12:34] <Nullpinter> 中文消息", "<Nullpinter>", 65);
    }

    @Test
    void chatDrawsAContinuousUnderlinedPlayerNameAsOneRun() {
        List<ITextComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new ParagraphLayoutMiddleware.ComponentRequest(
                        new TextComponentString("\u00a7n<Nullpinter>\u00a7r 中文消息"),
                        180, 9, "zh_cn", false, true,
                        TiqianParagraphProviderTest::measure,
                        ParagraphLayoutMiddleware.ComponentRequest.Surface.CHAT));

        assertNotNull(lines);
        PositionedTextLine first = (PositionedTextLine) lines.get(0);
        assertTrue(first.nfrUi$runs().stream().anyMatch(run ->
                run.formattedText().contains("\u00a7n")
                        && "<Nullpinter>".equals(TextFormatting.getTextWithoutFormattingCodes(
                                run.formattedText()))));
    }

    @Test
    void positionedLineMapsClicksAndSelectionToItsOriginalComponent() {
        TextComponentString child = new TextComponentString("测试");
        TiqianLineComponent line = new TiqianLineComponent(List.of(), 18.0F, 2);
        line.nfrUi$addCell(0, 1, 0.0F, 9.0F);
        line.nfrUi$addCell(1, 2, 9.0F, 18.0F);
        line.nfrUi$addComponentSpan(0, 2, child);

        assertEquals(1, line.nfrUi$visibleOffsetAt(9.0F));
        assertEquals(13.5F, line.nfrUi$xAtVisibleOffset(1) + 4.5F);
        assertSame(child, line.nfrUi$componentAt(13.0F));
        assertEquals(0.0F, line.nfrUi$componentLeft(child));
        assertEquals(18.0F, line.nfrUi$componentRight(child));
    }

    @Test
    void genericComponentSplitsStayOnTheSafeDefaultPath() {
        List<ITextComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new ParagraphLayoutMiddleware.ComponentRequest(
                        new TextComponentString("默认调用不应携带聊天几何"), 45, 9,
                        "zh_cn", true, true, TiqianParagraphProviderTest::measure));

        assertNull(lines);
    }

    private static ParagraphLayoutMiddleware.Request request(
            String text, int width, String language) {
        return new ParagraphLayoutMiddleware.Request(text, width, 9, language,
                TiqianParagraphProviderTest::measure);
    }

    private static void assertChatTokenIsNotSplit(String text, String token, int width) {
        List<ITextComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new ParagraphLayoutMiddleware.ComponentRequest(
                        new TextComponentString(text), width, 9, "zh_cn", false, true,
                        TiqianParagraphProviderTest::measure,
                        ParagraphLayoutMiddleware.ComponentRequest.Surface.CHAT));

        assertNotNull(lines);
        int tokenStart = text.indexOf(token);
        int tokenEnd = tokenStart + token.length();
        int boundary = 0;
        boolean foundWholeToken = false;
        StringBuilder copied = new StringBuilder();
        for (ITextComponent line : lines) {
            String clean = TextFormatting.getTextWithoutFormattingCodes(line.getUnformattedText());
            copied.append(clean);
            foundWholeToken |= clean.contains(token);
            boundary += clean.length();
            assertFalse(boundary > tokenStart && boundary < tokenEnd,
                    "speaker token split at source offset " + boundary);
        }
        assertTrue(foundWholeToken);
        assertEquals(text, copied.toString());
    }

    private static float measure(String formatted) {
        formatted = TextPipelineApi.processRaw(formatted).visibleText();
        float width = 0;
        boolean bold = false;
        for (int index = 0; index < formatted.length();) {
            if (formatted.charAt(index) == '\u00a7' && index + 1 < formatted.length()) {
                char code = Character.toLowerCase(formatted.charAt(index + 1));
                if (code == 'l') bold = true;
                if (code == 'r' || "0123456789abcdef".indexOf(code) >= 0) bold = false;
                index += 2;
                continue;
            }
            int codePoint = formatted.codePointAt(index);
            float advance = Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN
                    || codePoint >= 0x3000 ? 9 : codePoint == ' ' ? 4 : 5;
            width += bold ? advance + 1 : advance;
            index += Character.charCount(codePoint);
        }
        return width;
    }

    private static String tinkersRgb(int red, int green, int blue) {
        return new String(new char[]{
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + red),
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + green),
                (char) (TinkersAntiqueTextPreprocessor.MARKER_START + blue)
        });
    }

    private static void assertNoPartialTinkersMarker(String text) {
        for (int index = 0; index < text.length();) {
            if (!TinkersAntiqueTextPreprocessor.isMarker(text.charAt(index))) {
                index++;
                continue;
            }
            assertTrue(index + 2 < text.length()
                            && TinkersAntiqueTextPreprocessor.isMarker(text.charAt(index + 1))
                            && TinkersAntiqueTextPreprocessor.isMarker(text.charAt(index + 2)),
                    "Tiqian split a Tinkers RGB triplet: " + Integer.toHexString(text.charAt(index)));
            index += 3;
        }
    }
}
