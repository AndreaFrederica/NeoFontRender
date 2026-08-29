package neofontrender.addons.cursor;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrTextButton;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;
import com.cleanroommc.modularui.drawable.Rectangle;

/** Complete settings card for the GUI cursor service. */
public final class CursorSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":cursor"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.cursor.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1019; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final CursorConfig.Snapshot original = CursorConfig.snapshot();

        @Override public IWidget createView(NfrSettingsPageContext context) {
            NfrSettingsControls controls = context.controls();
            NfrOptionsGrid grid = controls.grid()
                    .add(toggle(controls, "enabled", () -> CursorConfig.enabled,
                            value -> CursorConfig.enabled = value))
                    .add(toggle(controls, "text_fields", () -> CursorConfig.textFields,
                            value -> CursorConfig.textFields = value))
                    .add(toggle(controls, "buttons", () -> CursorConfig.buttons,
                            value -> CursorConfig.buttons = value))
                    .add(toggle(controls, "disabled_buttons", () -> CursorConfig.disabledButtons,
                            value -> CursorConfig.disabledButtons = value))
                    .add(selector("default", () -> CursorConfig.defaultImage,
                            value -> CursorConfig.defaultImage = value))
                    .add(selector("text", () -> CursorConfig.textImage,
                            value -> CursorConfig.textImage = value))
                    .add(selector("button", () -> CursorConfig.buttonImage,
                            value -> CursorConfig.buttonImage = value))
                    .add(selector("disabled", () -> CursorConfig.disabledButtonImage,
                            value -> CursorConfig.disabledButtonImage = value))
                    .add(new NfrTextButton(() -> tr("gui.cursor.refresh"), true)
                            .background(new Rectangle().color(0xB0000000))
                            .hoverBackground(new Rectangle().color(0xB8333333))
                            .onMousePressed(button -> {
                                CursorAssetCatalog.INSTANCE.refresh();
                                return true;
                            }))
                    .add(new NfrTextButton(() -> tr("gui.cursor.open_folder"), true)
                            .background(new Rectangle().color(0xB0000000))
                            .hoverBackground(new Rectangle().color(0xB8333333))
                            .onMousePressed(button -> {
                                CursorAssetCatalog.INSTANCE.openDirectory();
                                return true;
                            }));
            return new PageView(grid);
        }

        @Override public void apply() {
            CursorConfig.save();
            CursorManager.restoreDefault();
        }

        @Override public void cancel() { original.restore(); }
    }

    private static IWidget toggle(NfrSettingsControls controls, String key,
                                  java.util.function.Supplier<Boolean> getter,
                                  java.util.function.Consumer<Boolean> setter) {
        return controls.toggleText(() -> tr("gui.cursor." + key),
                () -> tr("tooltip.cursor." + key), getter, setter);
    }

    private static IWidget selector(String key, java.util.function.Supplier<String> getter,
                                    java.util.function.Consumer<String> setter) {
        return new CursorImageSelector("cursor_image_" + key,
                () -> tr("gui.cursor.image." + key), getter, setter);
    }

    private static String tr(String key) {
        return AddonI18n.tr("neofontrender_ui_enhancements." + key);
    }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
