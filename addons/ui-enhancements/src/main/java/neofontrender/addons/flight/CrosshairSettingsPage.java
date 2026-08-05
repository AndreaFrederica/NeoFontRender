package neofontrender.addons.flight;

import com.cleanroommc.modularui.api.widget.IWidget;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.api.client.settings.NfrSettingsPage;
import neofontrender.api.client.settings.NfrSettingsPageContext;
import neofontrender.api.client.settings.NfrSettingsPageSession;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.views.NfrContentView;

import java.util.Arrays;

final class CrosshairSettingsPage implements NfrSettingsPage {
    @Override public String id() { return NfrUiEnhancements.MOD_ID + ":crosshair"; }
    @Override public String titleKey() { return "neofontrender_ui_enhancements.gui.crosshair.category"; }
    @Override public String title() { return AddonI18n.tr(titleKey()); }
    @Override public int order() { return 1017; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final CrosshairConfig.Snapshot original = CrosshairConfig.snapshot();
        private boolean editingDrawnCrosshair;

        @Override public IWidget createView(NfrSettingsPageContext context) {
            if (editingDrawnCrosshair) {
                return new CrosshairEditorView(
                        () -> {
                            editingDrawnCrosshair = false;
                            context.refresh();
                        },
                        () -> {
                            editingDrawnCrosshair = false;
                            context.refresh();
                        });
            }
            NfrSettingsControls c = context.controls();
            NfrOptionsGrid grid = c.grid();
            grid.add(toggle(c, "enabled", "enabled", () -> CrosshairConfig.customEnabled, v -> CrosshairConfig.customEnabled = v));
            grid.add(toggle(c, "prefer_mod", "prefer_mod", () -> CrosshairConfig.preferModCrosshair,
                    v -> CrosshairConfig.preferModCrosshair = v));
            grid.add(toggle(c, "hide_flight", "hide_flight", () -> CrosshairConfig.hideVanillaDuringFlightHud, v -> CrosshairConfig.hideVanillaDuringFlightHud = v));
            grid.add(toggle(c, "hide_forge_flight", "hide_forge_flight", () -> CrosshairConfig.hideForgeLayerDuringFlightHud, v -> CrosshairConfig.hideForgeLayerDuringFlightHud = v));
            grid.add(c.dropdownText("crosshair_style", () -> tr("gui.crosshair.style"),
                    () -> CrosshairConfig.style, value -> CrosshairConfig.style = value,
                    Arrays.asList("vanilla", "vanilla_plus", "cross", "dot", "circle", "square", "triangle", "arrow", "chevron", "debug", "drawn"),
                    value -> tr("gui.crosshair.style." + value)).size(260, 24));
            grid.add(toggle(c, "keep_debug", "keep_debug", () -> CrosshairConfig.keepDebugCrosshair, v -> CrosshairConfig.keepDebugCrosshair = v));
            grid.add(c.colorText("crosshair_color", () -> tr("gui.crosshair.color"), () -> CrosshairConfig.color, v -> CrosshairConfig.color = v, true).size(260, 24));
            grid.add(toggle(c, "adaptive", "adaptive", () -> CrosshairConfig.adaptiveColor, v -> CrosshairConfig.adaptiveColor = v));
            grid.add(slider(c, "width", () -> CrosshairConfig.width, v -> CrosshairConfig.width = v, 0, 50));
            grid.add(slider(c, "height", () -> CrosshairConfig.height, v -> CrosshairConfig.height = v, 0, 50));
            grid.add(slider(c, "gap", () -> CrosshairConfig.gap, v -> CrosshairConfig.gap = v, 0, 50));
            grid.add(slider(c, "thickness", () -> CrosshairConfig.thickness, v -> CrosshairConfig.thickness = v, 1, 10));
            grid.add(slider(c, "rotation", () -> CrosshairConfig.rotation, v -> CrosshairConfig.rotation = v, 0, 360));
            grid.add(slider(c, "scale", () -> CrosshairConfig.scalePercent, v -> CrosshairConfig.scalePercent = v, 25, 500));
            grid.add(slider(c, "offset_x", () -> CrosshairConfig.offsetX, v -> CrosshairConfig.offsetX = v, -500, 500));
            grid.add(slider(c, "offset_y", () -> CrosshairConfig.offsetY, v -> CrosshairConfig.offsetY = v, -500, 500));

            grid.add(toggle(c, "visible_default", "visibility", () -> CrosshairConfig.visibleByDefault, v -> CrosshairConfig.visibleByDefault = v));
            grid.add(toggle(c, "visible_hidden_gui", "visibility", () -> CrosshairConfig.visibleWithHiddenGui, v -> CrosshairConfig.visibleWithHiddenGui = v));
            grid.add(toggle(c, "visible_debug", "visibility", () -> CrosshairConfig.visibleInDebug, v -> CrosshairConfig.visibleInDebug = v));
            grid.add(toggle(c, "visible_third_person", "shoulder_surfing", () -> CrosshairConfig.visibleInThirdPerson, v -> CrosshairConfig.visibleInThirdPerson = v));
            grid.add(toggle(c, "visible_spectator", "visibility", () -> CrosshairConfig.visibleAsSpectator, v -> CrosshairConfig.visibleAsSpectator = v));
            grid.add(toggle(c, "visible_ranged", "visibility", () -> CrosshairConfig.visibleHoldingRanged, v -> CrosshairConfig.visibleHoldingRanged = v));
            grid.add(toggle(c, "visible_throwable", "visibility", () -> CrosshairConfig.visibleHoldingThrowable, v -> CrosshairConfig.visibleHoldingThrowable = v));
            grid.add(toggle(c, "visible_spyglass", "spyglass", () -> CrosshairConfig.visibleUsingSpyglass, v -> CrosshairConfig.visibleUsingSpyglass = v));

            grid.add(toggle(c, "outline", "outline", () -> CrosshairConfig.outlineEnabled, v -> CrosshairConfig.outlineEnabled = v));
            grid.add(c.colorText("crosshair_outline_color", () -> tr("gui.crosshair.outline_color"), () -> CrosshairConfig.outlineColor, v -> CrosshairConfig.outlineColor = v, true).size(260, 24));
            grid.add(toggle(c, "dot_enabled", "dot", () -> CrosshairConfig.dotEnabled, v -> CrosshairConfig.dotEnabled = v));
            grid.add(c.colorText("crosshair_dot_color", () -> tr("gui.crosshair.dot_color"), () -> CrosshairConfig.dotColor, v -> CrosshairConfig.dotColor = v, true).size(260, 24));
            grid.add(toggle(c, "dynamic_attack", "dynamic_attack", () -> CrosshairConfig.dynamicAttack, v -> CrosshairConfig.dynamicAttack = v));
            grid.add(toggle(c, "dynamic_bow", "dynamic_bow", () -> CrosshairConfig.dynamicBow, v -> CrosshairConfig.dynamicBow = v));

            grid.add(toggle(c, "highlight_hostiles", "highlight", () -> CrosshairConfig.highlightHostiles, v -> CrosshairConfig.highlightHostiles = v));
            grid.add(c.colorText("crosshair_hostile_color", () -> tr("gui.crosshair.hostile_color"), () -> CrosshairConfig.hostileColor, v -> CrosshairConfig.hostileColor = v, true).size(260, 24));
            grid.add(toggle(c, "highlight_passives", "highlight", () -> CrosshairConfig.highlightPassives, v -> CrosshairConfig.highlightPassives = v));
            grid.add(c.colorText("crosshair_passive_color", () -> tr("gui.crosshair.passive_color"), () -> CrosshairConfig.passiveColor, v -> CrosshairConfig.passiveColor = v, true).size(260, 24));
            grid.add(toggle(c, "highlight_players", "highlight", () -> CrosshairConfig.highlightPlayers, v -> CrosshairConfig.highlightPlayers = v));
            grid.add(c.colorText("crosshair_player_color", () -> tr("gui.crosshair.player_color"), () -> CrosshairConfig.playerColor, v -> CrosshairConfig.playerColor = v, true).size(260, 24));

            grid.add(toggle(c, "cooldown", "cooldown", () -> CrosshairConfig.itemCooldownEnabled, v -> CrosshairConfig.itemCooldownEnabled = v));
            grid.add(c.colorText("crosshair_cooldown_color", () -> tr("gui.crosshair.cooldown_color"), () -> CrosshairConfig.itemCooldownColor, v -> CrosshairConfig.itemCooldownColor = v, true).size(260, 24));
            grid.add(toggle(c, "rainbow", "rainbow", () -> CrosshairConfig.rainbowEnabled, v -> CrosshairConfig.rainbowEnabled = v));
            grid.add(slider(c, "rainbow_speed", () -> CrosshairConfig.rainbowSpeed, v -> CrosshairConfig.rainbowSpeed = v, 0, 1000));
            grid.add(toggle(c, "tool_damage", "indicators", () -> CrosshairConfig.toolDamageEnabled, v -> CrosshairConfig.toolDamageEnabled = v));
            grid.add(toggle(c, "projectiles", "indicators", () -> CrosshairConfig.projectileIndicatorEnabled, v -> CrosshairConfig.projectileIndicatorEnabled = v));
            grid.add(slider(c, "drawn_size", () -> CrosshairConfig.drawnSize, v -> CrosshairConfig.drawnSize = v, 7, 57));
            grid.add(c.action(() -> tr("gui.crosshair.edit_drawn"), 260, 24, () -> {
                editingDrawnCrosshair = true;
                context.refresh();
            }));
            return new PageView(grid);
        }

        @Override public void apply() { CrosshairConfig.save(); }
        @Override public void cancel() { original.restore(); }
    }

    private interface BoolGet { boolean get(); }
    private interface BoolSet { void set(boolean value); }
    private interface IntGet { int get(); }
    private interface IntSet { void set(int value); }

    private static IWidget toggle(NfrSettingsControls c, String label, String tooltip, BoolGet getter, BoolSet setter) {
        return c.toggleText(() -> tr("gui.crosshair." + label), () -> tr("tooltip.crosshair." + tooltip),
                getter::get, setter::set);
    }

    private static IWidget slider(NfrSettingsControls c, String label, IntGet getter, IntSet setter, int min, int max) {
        return c.decimalSlider(() -> tr("gui.crosshair." + label),
                () -> (float) getter.get(), value -> setter.set(Math.round(value)), min, max, 1);
    }

    private static String tr(String key) { return AddonI18n.tr("neofontrender_ui_enhancements." + key); }

    private static final class PageView extends NfrContentView<PageView> {
        private PageView(NfrOptionsGrid grid) { super(section(grid, grid::preferredHeight)); }
    }
}
