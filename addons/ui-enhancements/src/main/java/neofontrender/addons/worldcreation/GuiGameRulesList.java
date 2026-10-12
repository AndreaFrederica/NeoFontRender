package neofontrender.addons.worldcreation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.GameRules;
import neofontrender.addons.tooltips.AddonI18n;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scrollable game-rule editor shown as the "Game Rules" tab of the create-world screen,
 * mirroring newer Minecraft versions. Boolean rules render as toggle buttons, numerical
 * and function rules as text fields. Edits live in the shared {@code values} map keyed by
 * rule name, seeded with vanilla defaults, and are applied to the new world on creation
 * via {@link CreateWorldGameRulesState}.
 *
 * <p>1.7.10 differences: {@code GuiSlot} draws and consumes its own mouse events inside
 * {@code drawScreen} (so no {@code handleMouseInput} override exists), its
 * {@code overlayBackground} is private and therefore keeps the vanilla container shading, and
 * {@code GuiTextField} has no validator, so numeric rules reject invalid keystrokes here.</p>
 */
public final class GuiGameRulesList extends GuiSlot {
    public static final int SLOT_HEIGHT = 24;

    private final Map<String, String> values;
    private final List<Entry> entries = new ArrayList<>();

    public GuiGameRulesList(Minecraft mc, int width, int height, int top, int bottom,
                            Map<String, String> values) {
        super(mc, width, height, top, bottom, SLOT_HEIGHT);
        this.values = values;
        // 1.7.10 name of the "centerListVertically" flag.
        this.field_148163_i = false;
        this.setShowSelectionBox(false);

        GameRules defaults = new GameRules();
        for (String rule : defaults.getRules()) {
            String defaultValue = defaults.getGameRuleStringValue(rule);
            values.putIfAbsent(rule, defaultValue);
            GameRuleValueType type = GameRuleValueType.of(defaultValue);
            if (type == GameRuleValueType.BOOLEAN) {
                entries.add(new BooleanEntry(rule));
            } else {
                entries.add(new TextEntry(rule, type == GameRuleValueType.NUMERIC));
            }
        }
    }

    /** Returns the values that differ from vanilla defaults; blank edits are ignored. */
    public static Map<String, String> collectOverrides(Map<String, String> values) {
        GameRules defaults = new GameRules();
        Map<String, String> overrides = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getValue();
            if (value == null || value.isEmpty()) continue;
            if (defaults.hasRule(entry.getKey())
                    && !value.equals(defaults.getGameRuleStringValue(entry.getKey()))) {
                overrides.put(entry.getKey(), value);
            }
        }
        return overrides;
    }

    /** Forwards a key press to the focused text entry, if any. */
    public void forwardKeyTyped(char typedChar, int keyCode) {
        for (Entry entry : entries) entry.keyTyped(typedChar, keyCode);
    }

    /** Ticks text-entry cursors; call from the screen's updateScreen. */
    public void tick() {
        for (Entry entry : entries) entry.tick();
    }

    @Override
    public int getListWidth() {
        return Math.min(320, width - 48);
    }

    @Override
    protected int getScrollBarX() {
        return width / 2 + getListWidth() / 2 + 2;
    }

    @Override
    protected int getSize() {
        return entries.size();
    }

    @Override
    protected boolean isSelected(int slotIndex) {
        return false;
    }

    @Override
    protected void drawBackground() {}

    @Override
    protected void drawContainerBackground(Tessellator tessellator) {
        Gui.drawRect(left, top, right, bottom, 0x60101115);
    }

    @Override
    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
        if (slotIndex < 0 || slotIndex >= entries.size()) return;
        for (Entry entry : entries) entry.setFocused(false);
        entries.get(slotIndex).click(mouseX, mouseY);
    }

    @Override
    protected void drawSlot(int slotIndex, int xPos, int yPos, int heightIn, Tessellator tessellator,
                            int mouseXIn, int mouseYIn) {
        entries.get(slotIndex).draw(xPos, yPos, mouseXIn, mouseYIn);
    }

    private abstract class Entry {
        final String rule;

        Entry(String rule) {
            this.rule = rule;
        }

        String label() {
            String key = "neofontrender_ui_enhancements.gamerule." + rule;
            String text = AddonI18n.tr(key);
            return key.equals(text) ? rule : text;
        }

        int rightEdge(int x) {
            return x + getListWidth() - 10;
        }

        void drawLabel(int x, int y, int maxWidth) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            font.drawString(font.trimStringToWidth(label(), maxWidth), x + 2,
                    y + (SLOT_HEIGHT - font.FONT_HEIGHT) / 2 + 1, 0xFFE0E0E0, false);
        }

        abstract void draw(int x, int y, int mouseX, int mouseY);

        abstract void click(int mouseX, int mouseY);

        void setFocused(boolean focused) {}

        void keyTyped(char typedChar, int keyCode) {}

        void tick() {}
    }

    private final class BooleanEntry extends Entry {
        private final GuiButton toggle;

        BooleanEntry(String rule) {
            super(rule);
            toggle = new GuiButton(0, 0, 0, 64, 18, "");
        }

        @Override
        void draw(int x, int y, int mouseX, int mouseY) {
            drawLabel(x, y, getListWidth() - 90);
            boolean on = Boolean.parseBoolean(values.get(rule));
            toggle.displayString = I18n.format(on ? "options.on" : "options.off");
            toggle.packedFGColour = on ? 0x52E875 : 0;
            toggle.xPosition = rightEdge(x) - toggle.width;
            toggle.yPosition = y + (SLOT_HEIGHT - toggle.height) / 2;
            toggle.drawButton(Minecraft.getMinecraft(), mouseX, mouseY);
        }

        @Override
        void click(int mouseX, int mouseY) {
            Minecraft mc = Minecraft.getMinecraft();
            if (toggle.mousePressed(mc, mouseX, mouseY)) {
                values.put(rule, Boolean.toString(!Boolean.parseBoolean(values.get(rule))));
                toggle.func_146113_a(mc.getSoundHandler());
            }
        }
    }

    private final class TextEntry extends Entry {
        private final GuiTextField field;
        private final boolean numerical;

        TextEntry(String rule, boolean numerical) {
            super(rule);
            this.numerical = numerical;
            Minecraft mc = Minecraft.getMinecraft();
            field = new GuiTextField(mc.fontRenderer, 0, 0, 90, 18);
            field.setMaxStringLength(64);
            field.setText(values.get(rule));
        }

        @Override
        void draw(int x, int y, int mouseX, int mouseY) {
            drawLabel(x, y, getListWidth() - 116);
            field.xPosition = rightEdge(x) - field.width;
            field.yPosition = y + (SLOT_HEIGHT - field.height) / 2;
            field.drawTextBox();
        }

        @Override
        void click(int mouseX, int mouseY) {
            field.mouseClicked(mouseX, mouseY, 0);
        }

        @Override
        void setFocused(boolean focused) {
            field.setFocused(focused);
        }

        @Override
        void keyTyped(char typedChar, int keyCode) {
            if (!field.isFocused()) return;
            String previous = field.getText();
            if (!field.textboxKeyTyped(typedChar, keyCode)) return;
            if (numerical && !field.getText().matches("-?\\d*")) {
                field.setText(previous);
                field.setCursorPositionEnd();
                return;
            }
            values.put(rule, field.getText());
        }

        @Override
        void tick() {
            field.updateCursorCounter();
        }
    }
}
