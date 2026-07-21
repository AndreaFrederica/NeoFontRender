package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.RichTooltip;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import neofontrender.client.gui.component.base.NfrContentButton;
import neofontrender.client.gui.component.base.NfrDoubleValue;
import neofontrender.client.gui.component.base.NfrLabeledSlider;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrOptionDropdown;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.base.NfrTextButton;
import neofontrender.client.gui.component.base.NfrToggleIndicator;
import neofontrender.client.gui.component.base.NfrTrackSliderWidget;
import neofontrender.client.gui.model.NfrSettingsDraft;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import static neofontrender.core.util.ConfigValueParser.parseFloat;

/** Shared settings-control factory. Route views own composition; this class owns repeated controls. */
public final class NfrSettingsControls {
    private static final String[] AA_MODES = {
            "off", "on", "gasp", "lcd_hrgb", "lcd_hbgr", "lcd_vrgb", "lcd_vbgr"
    };

    private final NfrSettingsDraft draft;
    private final Runnable preview;
    private final IntConsumer reloadRoute;
    private final BooleanSupplier skiaAvailable;

    public NfrSettingsControls(NfrSettingsDraft draft, Runnable preview, IntConsumer reloadRoute,
                               BooleanSupplier skiaAvailable) {
        this.draft = draft;
        this.preview = preview;
        this.reloadRoute = reloadRoute;
        this.skiaAvailable = skiaAvailable;
    }

    public NfrOptionsGrid grid() {
        return new NfrOptionsGrid(260, 24, 8, true);
    }

    public void reload(int route) {
        reloadRoute.accept(route);
    }

    public IWidget toggle(String labelKey, String tooltipKey, Supplier<Boolean> getter,
                          Consumer<Boolean> setter) {
        return toggle(labelKey, tooltipKey, getter, setter, preview);
    }

    public IWidget toggle(String labelKey, String tooltipKey, Supplier<Boolean> getter,
                          Consumer<Boolean> setter, Runnable afterChange) {
        return tooltip(new NfrContentButton(() -> tr(labelKey), false, new NfrToggleIndicator(getter))
                .size(260, 24)
                .onMousePressed(mouseButton -> {
                    setter.accept(!getter.get());
                    afterChange.run();
                    return true;
                }), tooltipKey);
    }

    public NfrOptionDropdown dropdown(String name, String labelKey, Supplier<String> getter,
                                      Consumer<String> setter, Iterable<String> values,
                                      java.util.function.Function<String, String> display) {
        return new NfrOptionDropdown(name, () -> tr(labelKey), getter, setter, values, display, false);
    }

    public NfrOptionDropdown compactDropdown(String name, Supplier<String> getter, Consumer<String> setter,
                                             Iterable<String> values,
                                             java.util.function.Function<String, String> display) {
        return new NfrOptionDropdown(name, () -> "", getter, setter, values, display, true);
    }

    public NfrTextButton action(String labelKey, int width, int height, Runnable action) {
        return new NfrTextButton(() -> tr(labelKey), true)
                .size(width, height)
                .onMousePressed(mouseButton -> {
                    action.run();
                    return true;
                });
    }

    public IWidget engine(int route) {
        List<String> engines = skiaAvailable.getAsBoolean()
                ? Arrays.asList("sfr", "skia", "cosmic", "vanilla")
                : Arrays.asList("sfr", "cosmic", "vanilla");
        return tooltip(dropdown("engine", "neofontrender.gui.option.engine",
                () -> normalizeEngine(draft.engine), value -> {
                    draft.engine = value;
                    draft.enabled = !"vanilla".equals(value);
                    reloadRoute.accept(route);
                }, engines, value -> engineName(value, skiaAvailable.getAsBoolean())).size(260, 24),
                "neofontrender.tooltip.engine");
    }

    public IWidget antialias() {
        return tooltip(dropdown("antialias", "neofontrender.gui.option.antialias",
                () -> draft.antialiasMode, value -> {
                    draft.antialiasMode = value;
                    draft.antialias = !"off".equals(value);
                    preview.run();
                }, Arrays.asList(AA_MODES), NfrSettingsControls::aaModeName).size(260, 24),
                "neofontrender.tooltip.antialias");
    }

    public IWidget style() {
        return tooltip(dropdown("font_style", "neofontrender.gui.option.style",
                () -> Integer.toString(draft.fontStyle), value -> {
                    draft.fontStyle = Integer.parseInt(value);
                    preview.run();
                }, Arrays.asList("0", "1", "2", "3"), value -> styleName(Integer.parseInt(value)))
                .size(260, 24), "neofontrender.tooltip.style");
    }

    public IWidget shadowMode() {
        return tooltip(dropdown("shadow_mode", "neofontrender.gui.option.shadow_mode",
                () -> draft.shadowMode, value -> {
                    draft.shadowMode = value;
                    preview.run();
                }, Arrays.asList("mask", "emoji", "all", "none"),
                value -> tr("neofontrender.gui.shadow_mode." + value)).size(260, 24),
                "neofontrender.tooltip.shadow_mode");
    }

    public IWidget shadowValue(boolean opacity) {
        List<String> values = opacity
                ? Arrays.asList("0.0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.0")
                : Arrays.asList("0.0", "0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0");
        return dropdown("shadow_" + (opacity ? "opacity" : "length"),
                opacity ? "neofontrender.gui.option.shadow_opacity" : "neofontrender.gui.option.shadow_length",
                () -> String.format(Locale.ROOT, "%.1f", opacity ? draft.shadowOpacity : draft.shadowLength),
                value -> {
                    if (opacity) draft.shadowOpacity = Float.parseFloat(value);
                    else draft.shadowLength = Float.parseFloat(value);
                    preview.run();
                }, values, value -> value).size(260, 24);
    }

    public NfrLabeledSlider brightness() {
        SliderWidget slider = new NfrTrackSliderWidget()
                .value(new NfrDoubleValue(
                        () -> (double) parseFloat(draft.brightness, 3.0F, 0.0F, 12.0F),
                        value -> {
                            draft.brightness = String.format(Locale.ROOT, "%.1f",
                                    Math.max(0.0D, Math.min(12.0D, value)));
                            preview.run();
                        }))
                .bounds(0.0D, 12.0D)
                .stopper(0.0D, 1.0D, 2.0D, 3.0D, 4.0D, 6.0D, 8.0D, 12.0D)
                .sliderSize(8, 14)
                .stopperSize(2, 6);
        return new NfrLabeledSlider(() -> tr("neofontrender.gui.label.brightness") + ": " + draft.brightness,
                slider);
    }

    public NfrLabeledTextField cacheField(String key, Supplier<String> getter, Consumer<String> setter) {
        return new NfrLabeledTextField(tr(key), new TextFieldWidget().setMaxLength(10)
                .value(new NfrStringValue(getter, setter)));
    }

    private static <T extends Widget<?>> T tooltip(T widget, String key) {
        if (key != null && !key.isEmpty()) {
            widget.tooltip(new RichTooltip().showUpTimer(8).addLine(tr(key)));
        }
        return widget;
    }

    public static String engineName(String engine, boolean skiaAvailable) {
        switch (normalizeEngine(engine)) {
            case "skia":
                return tr("neofontrender.gui.engine.skia")
                        + (skiaAvailable ? "" : " (" + tr("neofontrender.gui.unavailable") + ")");
            case "cosmic": return tr("neofontrender.gui.engine.cosmic");
            case "vanilla": return tr("neofontrender.gui.engine.vanilla");
            default: return tr("neofontrender.gui.engine.sfr");
        }
    }

    public static String normalizeEngine(String engine) {
        if (engine == null) return "sfr";
        String value = engine.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if ("skija".equals(value) || "skia".equals(value)) return "skia";
        if ("cosmic_text".equals(value) || "cosmic".equals(value)) return "cosmic";
        if ("vanilla".equals(value) || "original".equals(value) || "minecraft".equals(value)) return "vanilla";
        return "sfr";
    }

    private static String aaModeName(String mode) {
        String value = mode == null ? "on" : mode.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (String allowed : AA_MODES) if (allowed.equals(value)) return tr("neofontrender.gui.aa." + value);
        return tr("neofontrender.gui.aa.on");
    }

    private static String styleName(int style) {
        switch (style & 3) {
            case 1: return tr("neofontrender.gui.style.bold");
            case 2: return tr("neofontrender.gui.style.italic");
            case 3: return tr("neofontrender.gui.style.bold_italic");
            default: return tr("neofontrender.gui.style.plain");
        }
    }

    private static String tr(String key) {
        return I18n.format(key);
    }
}
