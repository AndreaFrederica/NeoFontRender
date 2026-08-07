package neofontrender.addons.cjk;

import neofontrender.api.text.CjkParagraphLayoutProvider;
import neofontrender.core.font.preprocess.PreprocessedText;
import neofontrender.core.font.preprocess.TextPreprocessingPipeline;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
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

    @BeforeEach
    void enableTiqian() {
        CjkTypographyConfig.engine = CjkTypographyConfig.ENGINE_TIQIAN;
        TiqianParagraphProvider.INSTANCE.clearCache();
    }

    @AfterEach
    void clearCache() {
        TiqianParagraphProvider.INSTANCE.clearCache();
    }

    @Test
    void laysOutSimplifiedChineseWithKinsokuBoundaries() {
        String text = "这是第一句，后面还有第二句。中文排版需要避免标点出现在行首。";
        CjkParagraphLayoutProvider.Layout layout = TiqianParagraphProvider.INSTANCE.layout(
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
        CjkParagraphLayoutProvider.Layout layout = TiqianParagraphProvider.INSTANCE.layout(
                request(text, 27, "zh_cn"));

        assertNotNull(layout);
        int boundary = layout.firstRawBoundary(text.length());
        assertTrue(boundary >= 0 && boundary <= text.length());
        assertFalse(boundary > 0 && text.charAt(boundary - 1) == '\u00a7');
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
        CjkParagraphLayoutProvider.Layout layout = TiqianParagraphProvider.INSTANCE.layout(
                request(text, 90, "zh_cn"));

        assertNotNull(layout);
        assertTrue(layout.lines().stream()
                .flatMap(line -> line.runs().stream())
                .anyMatch(run -> run.formattedText().endsWith("-")),
                () -> layout.lines().stream()
                        .map(line -> line.runs().stream()
                                .map(CjkParagraphLayoutProvider.Run::formattedText)
                                .reduce("", String::concat))
                        .reduce("", (left, right) -> left + "|" + right));

        ChatComponentText component = new ChatComponentText(text);
        List<IChatComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new CjkParagraphLayoutProvider.ComponentRequest(component, 90, 9,
                        "zh_cn", true, true, TiqianParagraphProviderTest::measure,
                        CjkParagraphLayoutProvider.ComponentRequest.Surface.BOOK));
        assertNotNull(lines);
        assertFalse(lines.stream().anyMatch(line -> line.getUnformattedText().contains("-")));
    }

    @Test
    void componentSplitKeepsFormattingAndInteractionStyle() {
        ChatComponentText component = new ChatComponentText("可点击的中文书页内容");
        component.getChatStyle().setColor(EnumChatFormatting.RED).setChatClickEvent(
                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "2"));

        List<IChatComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new CjkParagraphLayoutProvider.ComponentRequest(component, 45, 9,
                        "zh_cn", true, true, TiqianParagraphProviderTest::measure,
                        CjkParagraphLayoutProvider.ComponentRequest.Surface.BOOK));

        assertNotNull(lines);
        assertTrue(lines.size() > 1);
        IChatComponent firstStyled = lines.get(0).getSiblings().get(0);
        if (firstStyled.getUnformattedTextForChat().isEmpty()
                && lines.get(0).getSiblings().size() > 1) {
            firstStyled = lines.get(0).getSiblings().get(1);
        }
        assertTrue(lines.get(0).getUnformattedText().contains("\u00a7c"));
        assertEquals("2", firstStyled.getChatStyle().getChatClickEvent().getValue());
    }

    @Test
    void chatSplitKeepsEveryLineInsideTheViewportWithoutChangingCopiedText() {
        String text = "<Nullpinter> " + "我草这对吗".repeat(13)
                + "，，，，，请运行 internationalization 命令";
        ChatComponentText component = new ChatComponentText(text);

        List<IChatComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new CjkParagraphLayoutProvider.ComponentRequest(component, 180, 9,
                        "zh_cn", false, true, TiqianParagraphProviderTest::measure,
                        CjkParagraphLayoutProvider.ComponentRequest.Surface.CHAT));

        assertNotNull(lines);
        assertTrue(lines.size() > 1);
        StringBuilder copied = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            IChatComponent line = lines.get(index);
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
                    <= EnumChatFormatting.getTextWithoutFormattingCodes(
                            line.getUnformattedText()).length());

            String clean = EnumChatFormatting.getTextWithoutFormattingCodes(
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
        List<IChatComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new CjkParagraphLayoutProvider.ComponentRequest(
                        new ChatComponentText("\u00a7n<Nullpinter>\u00a7r 中文消息"),
                        180, 9, "zh_cn", false, true,
                        TiqianParagraphProviderTest::measure,
                        CjkParagraphLayoutProvider.ComponentRequest.Surface.CHAT));

        assertNotNull(lines);
        PositionedTextLine first = (PositionedTextLine) lines.get(0);
        assertTrue(first.nfrUi$runs().stream().anyMatch(run ->
                run.formattedText().contains("\u00a7n")
                        && "<Nullpinter>".equals(EnumChatFormatting.getTextWithoutFormattingCodes(
                                run.formattedText()))));
    }

    @Test
    void positionedLineMapsClicksAndSelectionToItsOriginalComponent() {
        ChatComponentText child = new ChatComponentText("测试");
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
        List<IChatComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new CjkParagraphLayoutProvider.ComponentRequest(
                        new ChatComponentText("默认调用不应携带聊天几何"), 45, 9,
                        "zh_cn", true, true, TiqianParagraphProviderTest::measure));

        assertNull(lines);
    }

    private static CjkParagraphLayoutProvider.Request request(
            String text, int width, String language) {
        return new CjkParagraphLayoutProvider.Request(text, width, 9, language,
                TiqianParagraphProviderTest::measure);
    }

    private static void assertChatTokenIsNotSplit(String text, String token, int width) {
        List<IChatComponent> lines = TiqianParagraphProvider.INSTANCE.splitComponents(
                new CjkParagraphLayoutProvider.ComponentRequest(
                        new ChatComponentText(text), width, 9, "zh_cn", false, true,
                        TiqianParagraphProviderTest::measure,
                        CjkParagraphLayoutProvider.ComponentRequest.Surface.CHAT));

        assertNotNull(lines);
        int tokenStart = text.indexOf(token);
        int tokenEnd = tokenStart + token.length();
        int boundary = 0;
        boolean foundWholeToken = false;
        StringBuilder copied = new StringBuilder();
        for (IChatComponent line : lines) {
            String clean = EnumChatFormatting.getTextWithoutFormattingCodes(line.getUnformattedText());
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
        formatted = TextPreprocessingPipeline.process(formatted).visibleText();
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

}
