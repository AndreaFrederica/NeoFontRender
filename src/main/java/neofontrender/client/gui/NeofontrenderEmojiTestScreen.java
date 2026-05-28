package neofontrender.client.gui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.skia.SkijaTextRenderer;

/**
 * Visual emoji rendering test screen.
 * Renders test strings using Skia and displays the results.
 * Includes a vanilla GuiTextField for user input testing.
 */
@SideOnly(Side.CLIENT)
public final class NeofontrenderEmojiTestScreen {

    private static final String[] TEST_STRINGS = {
            "Hello World",
            "😀",
            "Hello 😀 World",
            "❤️",
            "Test 😀 ❤️ 🎉",
            "👨‍👩‍👧‍👦",
            "🙏 😥 😱 😎",
    };

    private static String userText = "Hello 😀 World ❤️";

    private NeofontrenderEmojiTestScreen() {
    }

    public static void open() {
        ClientGUI.open(new ModularScreen("neofontrender_emoji_test", buildPanel()).pausesGame(false));
    }

    private static ModularPanel buildPanel() {
        ModularPanel panel = new ModularPanel("emoji_test_panel")
                .relativeToScreen()
                .full();
        panel.child(new EmojiTestWidget().relativeToParent().full());
        return panel;
    }

    private static final class EmojiTestWidget extends Widget<EmojiTestWidget> implements Interactable {
        private GuiTextField inputField;

        @Override
        public void onInit() {
            super.onInit();
            Minecraft mc = Minecraft.getMinecraft();
            inputField = new GuiTextField(0, mc.fontRenderer, getArea().x() + 16, 0, getArea().w() - 32, 16);
            inputField.setMaxStringLength(256);
            inputField.setFocused(true);
            inputField.setText(userText);
            inputField.setCanLoseFocus(true);
        }

        @Override
        public void draw(ModularGuiContext context, WidgetTheme widgetTheme) {
            super.draw(context, widgetTheme);

            int areaX = getArea().x();
            int areaY = getArea().y();
            int areaW = getArea().w();
            int areaH = getArea().h();

            Gui.drawRect(areaX, areaY, areaX + areaW, areaY + areaH, 0xCC111111);

            boolean skiaActive = FontManager.INSTANCE.isSkiaActive();
            SkijaTextRenderer renderer = skiaActive ? FontManager.INSTANCE.getSkijaTextRenderer() : null;

            int y = areaY + 8;
            Minecraft.getMinecraft().fontRenderer.drawString(
                    "Emoji Render Test (Skia: " + (skiaActive ? "ON" : "OFF") + ")",
                    areaX + 8, y, 0xFFD700);
            y += 14;

            Minecraft.getMinecraft().fontRenderer.drawString("Type emoji below:", areaX + 8, y, 0xA9B5C5);
            y += 12;

            if (inputField != null) {
                inputField.x = areaX + 16;
                inputField.y = y;
                inputField.width = areaW - 32;

                String currentInput = inputField.getText();
                if (!currentInput.equals(userText)) {
                    userText = currentInput;
                }

                inputField.drawTextBox();
            }
            y += 22;

            // Codepoint display
            if (userText != null && !userText.isEmpty()) {
                Minecraft.getMinecraft().fontRenderer.drawString("Codepoints:", areaX + 8, y, 0xA9B5C5);
                y += 10;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < userText.length(); i++) {
                    char c = userText.charAt(i);
                    if (Character.isHighSurrogate(c) && i + 1 < userText.length() && Character.isLowSurrogate(userText.charAt(i + 1))) {
                        int cp = Character.toCodePoint(c, userText.charAt(i + 1));
                        sb.append(String.format("U+%04X ", cp));
                        i++; // skip low surrogate
                    } else {
                        sb.append(String.format("U+%04X ", (int) c));
                    }
                }
                String codepoints = sb.toString();
                // Wrap long codepoint strings
                int maxCharsPerLine = (areaW - 16) / 6;
                for (int i = 0; i < codepoints.length(); i += maxCharsPerLine) {
                    if (y + 10 > areaY + areaH) break;
                    Minecraft.getMinecraft().fontRenderer.drawString(
                            codepoints.substring(i, Math.min(i + maxCharsPerLine, codepoints.length())),
                            areaX + 8, y, 0x888888);
                    y += 10;
                }
                y += 2;

                // String length info
                Minecraft.getMinecraft().fontRenderer.drawString(
                        "length=" + userText.length() + " chars, bytes=" + userText.getBytes().length,
                        areaX + 8, y, 0x666666);
                y += 12;
            }

            if (skiaActive && renderer != null && userText != null && !userText.isEmpty()) {
                Minecraft.getMinecraft().fontRenderer.drawString("Skia result:", areaX + 8, y, 0x88AAFF);
                y += 12;
                SkijaTextRenderer.RenderedText rendered = renderer.render(userText, 0xFFFFFFFF, false, false);
                if (rendered.advance() > 0) {
                    GlStateManager.enableTexture2D();
                    GlStateManager.enableAlpha();
                    GlStateManager.enableBlend();
                    rendered.draw(areaX + 16, y, 1.0F);
                    String info = "w=" + String.format("%.1f", rendered.advance());
                    Minecraft.getMinecraft().fontRenderer.drawString(info, areaX + 16 + (int) rendered.advance() + 8, y + 2, 0x66FF66);
                } else {
                    Minecraft.getMinecraft().fontRenderer.drawString("FAILED", areaX + 16, y, 0xFF6666);
                }
                y += 18;
            }

            Gui.drawRect(areaX + 8, y, areaX + areaW - 8, y + 1, 0x44FFFFFF);
            y += 6;
            Minecraft.getMinecraft().fontRenderer.drawString("Preset tests:", areaX + 8, y, 0xA9B5C5);
            y += 12;

            if (!skiaActive || renderer == null) {
                Minecraft.getMinecraft().fontRenderer.drawString(
                        "Skia engine not active.", areaX + 8, y, 0xFF6666);
                return;
            }

            for (int i = 0; i < TEST_STRINGS.length; i++) {
                String test = TEST_STRINGS[i];
                if (y + 18 > areaY + areaH) break;

                SkijaTextRenderer.RenderedText rendered = renderer.render(test, 0xFFFFFFFF, false, false);
                float advance = rendered.advance();

                Minecraft.getMinecraft().fontRenderer.drawString("[" + i + "]", areaX + 8, y + 2, 0x888888);

                if (advance > 0) {
                    GlStateManager.enableTexture2D();
                    GlStateManager.enableAlpha();
                    GlStateManager.enableBlend();
                    rendered.draw(areaX + 40, y, 1.0F);
                    Minecraft.getMinecraft().fontRenderer.drawString(
                            "w=" + String.format("%.1f", advance),
                            areaX + 40 + (int) advance + 8, y + 2, 0x66FF66);
                } else {
                    Minecraft.getMinecraft().fontRenderer.drawString("FAILED", areaX + 40, y, 0xFF6666);
                }
                y += 16;
            }

            y += 6;
            Gui.drawRect(areaX + 8, y, areaX + areaW - 8, y + 1, 0x44FFFFFF);
            y += 6;
            Minecraft.getMinecraft().fontRenderer.drawString("Vanilla (via fontRenderer):", areaX + 8, y, 0xA9B5C5);
            y += 12;
            if (userText != null && !userText.isEmpty()) {
                Minecraft.getMinecraft().fontRenderer.drawString(userText, areaX + 16, y, 0xFFFFFF);
                y += 14;
            }
            for (int i = 0; i < TEST_STRINGS.length; i++) {
                if (y + 12 > areaY + areaH) break;
                Minecraft.getMinecraft().fontRenderer.drawString(TEST_STRINGS[i], areaX + 16, y, 0xFFFFFF);
                y += 12;
            }
        }

        @Override
        public Result onMousePressed(int mouseButton) {
            if (inputField != null) {
                inputField.mouseClicked(inputField.x, inputField.y, mouseButton);
            }
            return Result.ACCEPT;
        }

        @Override
        public Result onKeyPressed(char typedChar, int keyCode) {
            if (inputField != null) {
                inputField.textboxKeyTyped(typedChar, keyCode);
                String currentInput = inputField.getText();
                if (!currentInput.equals(userText)) {
                    userText = currentInput;
                }
            }
            return Result.ACCEPT;
        }
    }
}
