package neofontrender.client.gui.views;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.minecraft.client.resources.I18n;
import neofontrender.client.gui.component.base.NfrDoubleValue;
import neofontrender.client.gui.component.base.NfrLabeledSlider;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrScrollablePane;
import neofontrender.client.gui.component.base.NfrStringValue;
import neofontrender.client.gui.component.base.NfrTrackSliderWidget;
import neofontrender.client.gui.component.business.NfrFontForm;
import neofontrender.client.gui.component.business.NfrFontList;
import neofontrender.client.gui.component.business.NfrFontMetricsFields;
import neofontrender.client.gui.component.business.NfrFontPreview;
import neofontrender.client.gui.component.business.NfrFontSelector;
import neofontrender.client.gui.component.business.NfrSettingsControls;
import neofontrender.client.gui.font.FontEntry;
import neofontrender.client.gui.model.NfrSettingsDraft;
import neofontrender.client.gui.model.NfrFontFieldRefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static neofontrender.client.gui.model.NfrSettingsDraft.SOURCE_BUILTIN;
import static neofontrender.client.gui.model.NfrSettingsDraft.SOURCE_FOLDER;
import static neofontrender.client.gui.model.NfrSettingsDraft.SOURCE_SYSTEM;
import static neofontrender.client.gui.model.NfrSettingsDraft.TARGET_COSMIC_BOLD;
import static neofontrender.client.gui.model.NfrSettingsDraft.TARGET_COSMIC_BOLD_ITALIC;
import static neofontrender.client.gui.model.NfrSettingsDraft.TARGET_COSMIC_ITALIC;
import static neofontrender.client.gui.model.NfrSettingsDraft.TARGET_COSMIC_REGULAR;
import static neofontrender.client.gui.model.NfrSettingsDraft.TARGET_COUNT;
import static neofontrender.client.gui.model.NfrSettingsDraft.TARGET_FALLBACK;
import static neofontrender.client.gui.model.NfrSettingsDraft.TARGET_SHADOW_MASK;
import static neofontrender.client.gui.util.FontCatalog.builtinFonts;
import static neofontrender.client.gui.util.FontCatalog.folderFonts;
import static neofontrender.client.gui.util.FontCatalog.localFonts;
import static neofontrender.core.util.ConfigValueParser.parseFloat;

/** Font route content and composition: browser beside a scrollable font form. */
public final class NfrFontSettingsView extends ParentWidget<NfrFontSettingsView> implements ILayoutWidget {
    private static final int GAP = 12;
    private static final int MIN_SELECTOR_WIDTH = 120;
    private static final int PREFERRED_SELECTOR_MIN = 180;
    private static final int MAX_SELECTOR_WIDTH = 240;
    private static final int MIN_FORM_WIDTH = 180;

    private final IWidget selector;
    private final NfrScrollablePane form;
    private final java.util.function.IntSupplier formHeight;

    public NfrFontSettingsView(NfrSettingsDraft draft, NfrSettingsControls controls, Runnable preview,
                               Runnable openFontFolder) {
        NfrFontFieldRefs refs = new NfrFontFieldRefs();
        NfrFontList[] listRef = new NfrFontList[1];
        this.selector = selector(draft, controls, refs, listRef, preview, openFontFolder);
        NfrFontForm fontForm = form(draft, controls, refs, preview);
        this.form = new NfrScrollablePane(fontForm);
        this.formHeight = fontForm::preferredHeight;
        child(selector);
        child(form.widget());
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        int height = getArea().h();
        int preferredSelectorWidth = clamp(width / 4, PREFERRED_SELECTOR_MIN, MAX_SELECTOR_WIDTH);
        int selectorWidth = Math.min(preferredSelectorWidth,
                Math.max(MIN_SELECTOR_WIDTH, width - GAP - MIN_FORM_WIDTH));
        selectorWidth = Math.min(selectorWidth, Math.max(0, width));
        int formX = selectorWidth + GAP;
        int formWidth = Math.max(0, width - formX);
        NfrLayout.place(selector, 0, 0, selectorWidth, height);
        form.layout(formX, 0, formWidth, height, Math.max(0, formWidth - 6),
                Math.max(height, formHeight.getAsInt()));
        return true;
    }

    private static NfrFontSelector selector(NfrSettingsDraft d, NfrSettingsControls c, NfrFontFieldRefs refs,
                                            NfrFontList[] listRef, Runnable preview, Runnable openFolder) {
        TextFieldWidget search = new TextFieldWidget().setMaxLength(128)
                .value(new NfrStringValue(() -> d.search, value -> {
                    d.search = value;
                    if (listRef[0] != null) listRef[0].refresh();
                }));
        List<String> targets = new ArrayList<>();
        for (int target = 0; target < TARGET_COUNT; target++) targets.add(Integer.toString(target));
        IWidget target = c.dropdown("font_target", "neofontrender.gui.label.target",
                () -> Integer.toString(d.fontTarget), value -> {
                    d.fontTarget = Integer.parseInt(value);
                    if (listRef[0] != null) listRef[0].refresh();
                }, targets, value -> targetName(Integer.parseInt(value)));
        IWidget source = c.compactDropdown("font_source", () -> Integer.toString(d.fontSource), value -> {
            d.fontSource = Integer.parseInt(value);
            if (listRef[0] != null) listRef[0].reloadFonts();
        }, Arrays.asList("0", "1", "2"), value -> sourceName(Integer.parseInt(value)));
        NfrFontList list = new NfrFontList(() -> fontEntries(d.fontSource),
                font -> d.matchesSearch(font.displayName), d::isSelected, d::selectFont, () -> {
                    refs.refresh(d.fontName, d.fontPath, d.fontFallbacks, d.shadowMaskFonts,
                            d.cosmicRegular, d.cosmicBold, d.cosmicItalic, d.cosmicBoldItalic);
                    preview.run();
                });
        listRef[0] = list;
        return new NfrFontSelector(label(tr("neofontrender.gui.label.search_fonts")), search, target, source,
                c.action("neofontrender.gui.button.refresh", 72, 18, list::reloadFonts),
                c.action("neofontrender.gui.button.open_folder", 100, 18, openFolder),
                dynamicLabel(() -> sourceTitle(d.fontSource)), list);
    }

    private static NfrFontForm form(NfrSettingsDraft d, NfrSettingsControls c, NfrFontFieldRefs refs,
                                    Runnable preview) {
        refs.fontName = field(() -> d.fontName, value -> d.fontName = value, 256);
        refs.fontPath = field(() -> d.fontPath, value -> d.fontPath = value, 512);
        refs.fallbacks = field(() -> d.fontFallbacks, value -> d.fontFallbacks = value, 512);
        refs.shadowMasks = field(() -> d.shadowMaskFonts, value -> d.shadowMaskFonts = value, 512);
        refs.cosmicRegular = field(() -> d.cosmicRegular, value -> d.cosmicRegular = value, 512);
        refs.cosmicBold = field(() -> d.cosmicBold, value -> d.cosmicBold = value, 512);
        refs.cosmicItalic = field(() -> d.cosmicItalic, value -> d.cosmicItalic = value, 512);
        refs.cosmicBoldItalic = field(() -> d.cosmicBoldItalic, value -> d.cosmicBoldItalic = value, 512);
        return new NfrFontForm(
                labeled("neofontrender.gui.label.font_name", refs.fontName),
                labeled("neofontrender.gui.label.local_path", refs.fontPath),
                labeled("neofontrender.gui.label.fallbacks", refs.fallbacks),
                labeled("neofontrender.gui.label.shadow_masks", refs.shadowMasks),
                labeled("neofontrender.gui.label.cosmic_regular", refs.cosmicRegular),
                labeled("neofontrender.gui.label.cosmic_bold", refs.cosmicBold),
                labeled("neofontrender.gui.label.cosmic_italic", refs.cosmicItalic),
                labeled("neofontrender.gui.label.cosmic_bold_italic", refs.cosmicBoldItalic),
                c.toggle("neofontrender.gui.option.cosmic_variant_font_only",
                        "neofontrender.tooltip.cosmic_variant_font_only",
                        () -> d.cosmicVariantOverridesOnlySwitchFont,
                        value -> d.cosmicVariantOverridesOnlySwitchFont = value),
                metrics(d), oversample(d, preview), new NfrFontPreview(d::selectedFont),
                "cosmic".equals(NfrSettingsControls.normalizeEngine(d.engine)));
    }

    private static NfrFontMetricsFields metrics(NfrSettingsDraft d) {
        return new NfrFontMetricsFields(
                labeled("neofontrender.gui.label.size", field(() -> d.fontSize, value -> d.fontSize = value, 8)),
                labeled("neofontrender.gui.label.weight", field(() -> d.variableWeight, value -> d.variableWeight = value, 4)),
                labeled("neofontrender.gui.label.baseline", field(() -> d.baselineShift, value -> d.baselineShift = value, 8)));
    }

    private static NfrLabeledSlider oversample(NfrSettingsDraft d, Runnable preview) {
        SliderWidget slider = new NfrTrackSliderWidget()
                .value(new NfrDoubleValue(() -> (double) parseFloat(d.oversample, 8.0F, 1.0F, 16.0F), value -> {
                    d.oversample = String.format(Locale.ROOT, "%.1f", Math.max(1.0D, Math.min(16.0D, value)));
                    preview.run();
                }))
                .bounds(1.0D, 16.0D).stopper(1.0D, 2.0D, 3.0D, 4.0D, 6.0D, 8.0D, 12.0D, 16.0D)
                .sliderSize(8, 14).stopperSize(2, 6);
        return new NfrLabeledSlider(() -> tr("neofontrender.gui.label.oversample") + ": " + d.oversample + "x"
                + (d.adaptiveRasterScale ? " (" + tr("neofontrender.gui.option.autoscale") + ")"
                : " (" + tr("neofontrender.gui.manual") + ")"), slider);
    }

    private static TextFieldWidget field(Supplier<String> getter, Consumer<String> setter, int maxLength) {
        return new TextFieldWidget().setMaxLength(maxLength).value(new NfrStringValue(getter, setter));
    }

    private static NfrLabeledTextField labeled(String key, TextFieldWidget field) {
        return new NfrLabeledTextField(tr(key), field);
    }

    private static TextWidget label(String text) {
        return new TextWidget(IKey.str(text)).alignment(Alignment.CenterLeft).color(0xA9B5C5);
    }

    private static TextWidget dynamicLabel(Supplier<String> text) {
        return new TextWidget(IKey.dynamic(text)).alignment(Alignment.CenterLeft).color(0xA9B5C5);
    }

    private static String sourceName(int source) {
        if (source == SOURCE_FOLDER) return tr("neofontrender.gui.source.game");
        if (source == SOURCE_BUILTIN) return tr("neofontrender.gui.source.builtin");
        return tr("neofontrender.gui.source.system");
    }

    private static String sourceTitle(int source) {
        if (source == SOURCE_FOLDER) return tr("neofontrender.gui.source_title.game");
        if (source == SOURCE_BUILTIN) return tr("neofontrender.gui.source_title.builtin");
        return tr("neofontrender.gui.source_title.system");
    }

    private static String targetName(int target) {
        switch (target) {
            case TARGET_FALLBACK: return tr("neofontrender.gui.target.fallback");
            case TARGET_COSMIC_REGULAR: return tr("neofontrender.gui.target.cosmic_regular");
            case TARGET_COSMIC_BOLD: return tr("neofontrender.gui.target.cosmic_bold");
            case TARGET_COSMIC_ITALIC: return tr("neofontrender.gui.target.cosmic_italic");
            case TARGET_COSMIC_BOLD_ITALIC: return tr("neofontrender.gui.target.cosmic_bold_italic");
            case TARGET_SHADOW_MASK: return tr("neofontrender.gui.target.shadow_mask");
            default: return tr("neofontrender.gui.target.primary");
        }
    }

    private static List<FontEntry> fontEntries(int source) {
        if (source == SOURCE_SYSTEM) {
            List<FontEntry> entries = new ArrayList<>();
            for (String font : localFonts()) entries.add(new FontEntry(font, ""));
            return entries;
        }
        return source == SOURCE_FOLDER ? folderFonts() : builtinFonts();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String tr(String key) {
        return I18n.format(key);
    }
}
