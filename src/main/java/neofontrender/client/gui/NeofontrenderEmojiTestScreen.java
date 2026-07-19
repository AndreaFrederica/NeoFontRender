package neofontrender.client.gui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import neofontrender.core.font.FontManager;

/**
 * Visual emoji rendering test screen.
 * Renders test strings through the currently active Minecraft FontRenderer path.
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
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            super.draw(context, widgetTheme);

            int areaX = getArea().x();
            int areaY = getArea().y();
            int areaW = getArea().w();
            int areaH = getArea().h();

            Gui.drawRect(areaX, areaY, areaX + areaW, areaY + areaH, 0xCC111111);

            Minecraft minecraft = Minecraft.getMinecraft();
            String activeEngine = FontManager.INSTANCE.isCosmicActive() ? "Cosmic"
                    : FontManager.INSTANCE.isSkiaActive() ? "Skia"
                    : FontManager.INSTANCE.isSfrActive() ? "SFR" : "Vanilla";

            int y = areaY + 8;
            minecraft.fontRenderer.drawString(
                    "Emoji Render Test (Active: " + activeEngine + ")",
                    areaX + 8, y, 0xFFD700);
            y += 14;

            minecraft.fontRenderer.drawString("Type emoji below:", areaX + 8, y, 0xA9B5C5);
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
                minecraft.fontRenderer.drawString("Codepoints:", areaX + 8, y, 0xA9B5C5);
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
                    minecraft.fontRenderer.drawString(
                            codepoints.substring(i, Math.min(i + maxCharsPerLine, codepoints.length())),
                            areaX + 8, y, 0x888888);
                    y += 10;
                }
                y += 2;

                // String length info
                minecraft.fontRenderer.drawString(
                        "length=" + userText.length() + " chars, bytes=" + userText.getBytes().length,
                        areaX + 8, y, 0x666666);
                y += 12;
            }

            if (userText != null && !userText.isEmpty()) {
                minecraft.fontRenderer.drawString(activeEngine + " result:", areaX + 8, y, 0x88AAFF);
                y += 12;
                int width = minecraft.fontRenderer.getStringWidth(userText);
                minecraft.fontRenderer.drawString(userText, areaX + 16, y, 0xFFFFFF);
                minecraft.fontRenderer.drawString("w=" + width, areaX + 16 + width + 8, y + 2, 0x66FF66);
                y += 18;
            }

            Gui.drawRect(areaX + 8, y, areaX + areaW - 8, y + 1, 0x44FFFFFF);
            y += 6;
            minecraft.fontRenderer.drawString("Preset tests (" + activeEngine + "):", areaX + 8, y, 0xA9B5C5);
            y += 12;

            for (int i = 0; i < TEST_STRINGS.length; i++) {
                String test = TEST_STRINGS[i];
                if (y + 18 > areaY + areaH) break;

                int width = minecraft.fontRenderer.getStringWidth(test);
                minecraft.fontRenderer.drawString("[" + i + "]", areaX + 8, y + 2, 0x888888);
                minecraft.fontRenderer.drawString(test, areaX + 40, y, 0xFFFFFF);
                minecraft.fontRenderer.drawString("w=" + width,
                        areaX + 40 + width + 8, y + 2, 0x66FF66);
                y += 16;
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
