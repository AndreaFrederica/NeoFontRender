package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.api.widget.IFocusedWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.api.navigation.NavigationAction;
import com.cleanroommc.modularui.api.navigation.NavigationAxis;
import com.cleanroommc.modularui.api.navigation.NavigationInfo;
import com.cleanroommc.modularui.api.navigation.NavigationRole;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * Compact decimal slider using the same panel, hover and typography treatment as NFR dropdowns.
 */
public final class NfrDecimalSlider extends SliderWidget implements IFocusedWidget {
    private static final Rectangle TRACK = new Rectangle().color(0xFF475569);
    private static final Rectangle FILL = new Rectangle().color(0xFF00AEB8);
    private static final Rectangle HANDLE = new Rectangle().color(0xFFE6ECF3);
    private static final int VALUE_MIN_WIDTH = 42;
    private final Supplier<String> label;
    private final Supplier<String> displayValue;
    private String editValue = "";
    private int editCursor;
    private boolean editing;
    private boolean selectAll;
    private boolean cancelEdit;

    public NfrDecimalSlider(Supplier<String> label, Supplier<String> displayValue) {
        this.label = label;
        this.displayValue = displayValue;
        navigationInfo(NavigationInfo.builder(NavigationRole.SLIDER)
                .label(label)
                .actions(NavigationAction.INCREMENT, NavigationAction.DECREMENT,
                        NavigationAction.BEGIN_EDIT, NavigationAction.END_EDIT)
                .primaryAxis(NavigationAxis.HORIZONTAL)
                .build());
        background(new Rectangle().color(0xB0000000));
        hoverBackground(new Rectangle().color(0xB8333333));
    }

    @Override
    public void drawBackground(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        super.drawBackground(context, theme);
        int width = Math.max(0, getArea().w() - 8);
        int y = Math.max(0, getArea().h() - 5);
        TRACK.draw(context, 4, y, width, 2, theme.getTheme());
        double range = getMax() - getMin();
        double normalized = range <= 0.0D ? 0.0D
                : Math.max(0.0D, Math.min(1.0D, (getSliderValue() - getMin()) / range));
        FILL.draw(context, 4, y, (int) Math.round(width * normalized), 2, theme.getTheme());
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        int pos = Math.max(4, Math.min(getArea().w() - 4, valueToPos(getSliderValue())));
        HANDLE.draw(context, pos - 2, Math.max(0, getArea().h() - 8), 5, 8, theme.getTheme());
        Platform.setupDrawFont();
        Minecraft mc = Minecraft.getMinecraft();
        int y = Math.max(0, (getArea().h() - mc.fontRenderer.FONT_HEIGHT) / 2 - 2);
        String left = mc.fontRenderer.trimStringToWidth(label.get(), Math.max(1, getArea().w() / 2 - 8));
        String right = editing ? editValue : displayValue.get();
        int valueLeft = valueAreaLeft(right);
        if (editing) {
            Gui.drawRect(valueLeft, 2, getArea().w() - 3,
                    Math.min(getArea().h() - 7, y + mc.fontRenderer.FONT_HEIGHT + 2), 0xA0182733);
        }
        mc.fontRenderer.drawString(left, 4, y, 0xFFFFFF);
        int rightX = Math.max(valueLeft + 3,
                getArea().w() - mc.fontRenderer.getStringWidth(right) - 5);
        if (editing && selectAll) {
            Gui.drawRect(rightX - 1, y - 1,
                    Math.min(getArea().w() - 4, rightX + mc.fontRenderer.getStringWidth(right) + 1),
                    y + mc.fontRenderer.FONT_HEIGHT, 0xA04A6A8A);
        }
        mc.fontRenderer.drawString(right, rightX, y, editing ? 0xFFFFFF : 0xE0E0E0);
        if (editing && !selectAll && (Minecraft.getSystemTime() / 500L & 1L) == 0L) {
            int cursorX = rightX + mc.fontRenderer.getStringWidth(right.substring(0,
                    Math.min(editCursor, right.length())));
            Gui.drawRect(cursorX, y - 1, cursorX + 1, y + mc.fontRenderer.FONT_HEIGHT, 0xFFFFFFFF);
        }
    }

    @Override
    public @NotNull Interactable.Result onMousePressed(int mouseButton) {
        if (mouseButton == 0 && getContext().getMouseX() >= valueAreaLeft(displayValue.get())) {
            beginEdit();
            getContext().focus(this);
            return Interactable.Result.SUCCESS;
        }
        if (editing) getContext().removeFocus();
        return super.onMousePressed(mouseButton);
    }

    @Override
    public void onMouseDrag(int mouseButton, long timeSinceClick) {
        if (!editing) super.onMouseDrag(mouseButton, timeSinceClick);
    }

    @Override
    public @NotNull Interactable.Result onKeyPressed(char typedChar, int keyCode) {
        if (!editing) return Interactable.Result.IGNORE;
        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            getContext().removeFocus();
            return Interactable.Result.SUCCESS;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            cancelEdit = true;
            getContext().removeFocus();
            return Interactable.Result.SUCCESS;
        }
        if (GuiScreen.isKeyComboCtrlA(keyCode)) {
            selectAll = true;
            editCursor = editValue.length();
            return Interactable.Result.SUCCESS;
        }
        if (GuiScreen.isKeyComboCtrlC(keyCode)) {
            GuiScreen.setClipboardString(selectAll ? editValue : "");
            return Interactable.Result.SUCCESS;
        }
        if (GuiScreen.isKeyComboCtrlX(keyCode)) {
            if (selectAll) {
                GuiScreen.setClipboardString(editValue);
                editValue = "";
                editCursor = 0;
                selectAll = false;
            }
            return Interactable.Result.SUCCESS;
        }
        if (GuiScreen.isKeyComboCtrlV(keyCode)) {
            insert(GuiScreen.getClipboardString().trim());
            return Interactable.Result.SUCCESS;
        }
        switch (keyCode) {
            case Keyboard.KEY_HOME:
                selectAll = false;
                editCursor = 0;
                return Interactable.Result.SUCCESS;
            case Keyboard.KEY_END:
                selectAll = false;
                editCursor = editValue.length();
                return Interactable.Result.SUCCESS;
            case Keyboard.KEY_LEFT:
                selectAll = false;
                editCursor = Math.max(0, editCursor - 1);
                return Interactable.Result.SUCCESS;
            case Keyboard.KEY_RIGHT:
                selectAll = false;
                editCursor = Math.min(editValue.length(), editCursor + 1);
                return Interactable.Result.SUCCESS;
            case Keyboard.KEY_BACK:
                erase(false);
                return Interactable.Result.SUCCESS;
            case Keyboard.KEY_DELETE:
                erase(true);
                return Interactable.Result.SUCCESS;
            default:
                if (isNumberCharacter(typedChar)) {
                    insert(String.valueOf(typedChar));
                    return Interactable.Result.SUCCESS;
                }
                return Interactable.Result.STOP;
        }
    }

    @Override
    public boolean isFocused() {
        return isValid() && getContext().isFocused(this);
    }

    @Override
    public void onFocus(ModularGuiContext context) {
        // Mouse clicks in the track may focus the slider for keyboard navigation without editing.
    }

    @Override
    public void onRemoveFocus(ModularGuiContext context) {
        if (!editing) return;
        if (!cancelEdit) commitEdit();
        editing = false;
        selectAll = false;
        cancelEdit = false;
    }

    private void beginEdit() {
        if (!editing) editValue = editableValue(getSliderValue());
        editing = true;
        cancelEdit = false;
        selectAll = true;
        editCursor = editValue.length();
    }

    private void commitEdit() {
        try {
            double parsed = Double.parseDouble(editValue.trim());
            if (Double.isFinite(parsed)) setValue(parsed, true);
        } catch (NumberFormatException ignored) {
            // Invalid or incomplete input leaves the previous slider value unchanged.
        }
    }

    private void insert(String text) {
        if (text == null || text.isEmpty() || !text.matches("[0-9eE+\\-.]+")) return;
        if (selectAll) {
            editValue = text;
            editCursor = text.length();
            selectAll = false;
            return;
        }
        editValue = editValue.substring(0, editCursor) + text + editValue.substring(editCursor);
        editCursor += text.length();
    }

    private void erase(boolean forward) {
        if (selectAll) {
            editValue = "";
            editCursor = 0;
            selectAll = false;
        } else if (forward && editCursor < editValue.length()) {
            editValue = editValue.substring(0, editCursor) + editValue.substring(editCursor + 1);
        } else if (!forward && editCursor > 0) {
            editValue = editValue.substring(0, editCursor - 1) + editValue.substring(editCursor);
            editCursor--;
        }
    }

    private int valueAreaLeft(String value) {
        int textWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(value == null ? "" : value);
        int areaWidth = Math.max(VALUE_MIN_WIDTH, textWidth + 12);
        return Math.max(getArea().w() / 2, getArea().w() - areaWidth);
    }

    private static boolean isNumberCharacter(char character) {
        return character >= '0' && character <= '9' || character == '-' || character == '+'
                || character == '.' || character == 'e' || character == 'E';
    }

    private static String editableValue(double value) {
        if (!Double.isFinite(value)) return "0";
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
