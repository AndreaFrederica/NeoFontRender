package neofontrender.addons.flight;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.TextWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrTextButton;
import org.jetbrains.annotations.NotNull;

/** Drawn-crosshair editor embedded in the standard NFR settings shell. */
final class CrosshairEditorView extends ParentWidget<CrosshairEditorView> implements ILayoutWidget {
    private static final int GAP = 8;
    private static final int BUTTON_HEIGHT = 24;

    private final IWidget title = new TextWidget(tr("editor_title")).color(0xFFFFFF);
    private final IWidget help = new TextWidget(tr("editor_help")).color(0xA9B5C5);
    private final PixelGrid grid = new PixelGrid();
    private final NfrTextButton done;
    private final NfrTextButton cancel;
    private final NfrTextButton clear;
    private final NfrTextButton invert;

    CrosshairEditorView(Runnable doneAction, Runnable cancelAction) {
        done = button(() -> I18n.format("gui.done"), () -> {
            CrosshairConfig.drawnPixels = CrosshairPattern.serialize(grid.pixels());
            doneAction.run();
        });
        cancel = button(() -> I18n.format("gui.cancel"), cancelAction);
        clear = button(() -> tr("clear"), grid::clear);
        invert = button(() -> tr("invert"), grid::invert);
        child(title);
        child(help);
        child(grid);
        child(done);
        child(cancel);
        child(clear);
        child(invert);
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        int height = getArea().h();
        NfrLayout.place(title, 0, 0, width, 16);
        NfrLayout.place(help, 0, 18, width, 14);

        int buttonY = Math.max(34, height - BUTTON_HEIGHT);
        NfrLayout.place(grid, 0, 36, width, Math.max(1, buttonY - 36 - GAP));

        int buttonWidth = Math.min(140, Math.max(54, (width - GAP * 3) / 4));
        int totalWidth = buttonWidth * 4 + GAP * 3;
        int x = Math.max(0, (width - totalWidth) / 2);
        NfrLayout.place(done, x, buttonY, buttonWidth, BUTTON_HEIGHT);
        NfrLayout.place(cancel, x += buttonWidth + GAP, buttonY, buttonWidth, BUTTON_HEIGHT);
        NfrLayout.place(clear, x += buttonWidth + GAP, buttonY, buttonWidth, BUTTON_HEIGHT);
        NfrLayout.place(invert, x + buttonWidth + GAP, buttonY, buttonWidth, BUTTON_HEIGHT);
        return true;
    }

    private static NfrTextButton button(java.util.function.Supplier<String> label, Runnable action) {
        return new NfrTextButton(label, true).onMousePressed(mouseButton -> {
            if (mouseButton != 0) return false;
            action.run();
            return true;
        });
    }

    private static final class PixelGrid extends Widget<PixelGrid> implements Interactable {
        private boolean[][] pixels = CrosshairPattern.parse(CrosshairConfig.drawnPixels, CrosshairConfig.drawnSize);
        private int gridX;
        private int gridY;
        private int cell = 1;
        private int paintButton = -1;

        boolean[][] pixels() {
            return pixels;
        }

        void clear() {
            pixels = new boolean[pixels.length][pixels.length];
        }

        void invert() {
            for (int x = 0; x < pixels.length; x++) {
                for (int y = 0; y < pixels[x].length; y++) pixels[x][y] = !pixels[x][y];
            }
        }

        @Override
        public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
            super.draw(context, widgetTheme);
            updateGrid();
            int size = cell * pixels.length;
            Gui.drawRect(gridX - 2, gridY - 2, gridX + size + 2, gridY + size + 2, 0xCC11151A);
            for (int x = 0; x < pixels.length; x++) {
                for (int y = 0; y < pixels[x].length; y++) {
                    int left = gridX + x * cell;
                    int top = gridY + y * cell;
                    int checker = ((x + y) & 1) == 0 ? 0xFF30363D : 0xFF39414A;
                    Gui.drawRect(left, top, left + cell, top + cell,
                            pixels[x][y] ? CrosshairConfig.color : checker);
                    if (cell >= 4) {
                        Gui.drawRect(left, top, left + cell, top + 1, 0x55202020);
                        Gui.drawRect(left, top, left + 1, top + cell, 0x55202020);
                    }
                }
            }
            Platform.setupDrawFont();
            String sizeText = pixels.length + " x " + pixels.length;
            Minecraft.getMinecraft().fontRenderer.drawString(sizeText,
                    Math.max(0, getArea().w() - Minecraft.getMinecraft().fontRenderer.getStringWidth(sizeText)),
                    Math.max(0, getArea().h() - Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT), 0xFF7F8A96);
        }

        @Override
        public @NotNull Result onMousePressed(int mouseButton) {
            if (mouseButton != 0 && mouseButton != 1) return Result.IGNORE;
            updateGrid();
            paintButton = mouseButton;
            return paint() ? Result.SUCCESS : Result.ACCEPT;
        }

        @Override
        public void onMouseDrag(int mouseButton, long timeSinceClick) {
            if (paintButton == 0 || paintButton == 1) paint();
        }

        @Override
        public boolean onMouseRelease(int mouseButton) {
            boolean wasPainting = paintButton >= 0;
            paintButton = -1;
            return wasPainting;
        }

        private boolean paint() {
            int mouseX = getContext().getMouseX();
            int mouseY = getContext().getMouseY();
            int x = (mouseX - gridX) / cell;
            int y = (mouseY - gridY) / cell;
            if (mouseX < gridX || mouseY < gridY || x < 0 || y < 0 || x >= pixels.length || y >= pixels.length) {
                return false;
            }
            pixels[x][y] = paintButton == 0;
            return true;
        }

        private void updateGrid() {
            int availableWidth = Math.max(1, getArea().w() - 6);
            int availableHeight = Math.max(1, getArea().h() - 6);
            cell = Math.max(1, Math.min(10, Math.min(availableWidth, availableHeight) / pixels.length));
            int size = cell * pixels.length;
            gridX = Math.max(0, (getArea().w() - size) / 2);
            gridY = Math.max(0, (getArea().h() - size) / 2);
        }
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements.gui.crosshair." + key);
    }
}
