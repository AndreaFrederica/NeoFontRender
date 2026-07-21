package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widget.ParentWidget;
import neofontrender.client.gui.component.base.NfrLabeledSlider;
import neofontrender.client.gui.component.base.NfrLabeledTextField;
import neofontrender.client.gui.component.base.NfrLayout;

/** Complete font-editing form used inside the Font settings view. */
public final class NfrFontForm extends ParentWidget<NfrFontForm> implements ILayoutWidget {
    private static final int FIELD_HEIGHT = 38;
    private static final int GAP = 8;

    private final NfrLabeledTextField fontName;
    private final NfrLabeledTextField fontPath;
    private final NfrLabeledTextField fallbacks;
    private final NfrLabeledTextField shadowMasks;
    private final NfrLabeledTextField[] cosmicFields;
    private final IWidget variantOnly;
    private final IWidget metrics;
    private final NfrLabeledSlider oversample;
    private final IWidget preview;
    private final boolean cosmic;

    public NfrFontForm(NfrLabeledTextField fontName, NfrLabeledTextField fontPath,
                       NfrLabeledTextField fallbacks, NfrLabeledTextField shadowMasks,
                       NfrLabeledTextField regular, NfrLabeledTextField bold,
                       NfrLabeledTextField italic, NfrLabeledTextField boldItalic,
                       IWidget variantOnly, IWidget metrics, NfrLabeledSlider oversample,
                       IWidget preview, boolean cosmic) {
        this.fontName = fontName;
        this.fontPath = fontPath;
        this.fallbacks = fallbacks;
        this.shadowMasks = shadowMasks;
        this.cosmicFields = new NfrLabeledTextField[]{regular, bold, italic, boldItalic};
        this.variantOnly = variantOnly;
        this.metrics = metrics;
        this.oversample = oversample;
        this.preview = preview;
        this.cosmic = cosmic;

        child(fontName);
        child(fontPath);
        child(fallbacks);
        child(shadowMasks);
        for (NfrLabeledTextField field : cosmicFields) {
            child(field);
            field.setEnabled(cosmic);
        }
        child(variantOnly);
        child(metrics);
        child(oversample);
        child(preview);
        variantOnly.setEnabled(cosmic);
    }

    public int preferredHeight() {
        int fields = 5 + (cosmic ? cosmicFields.length : 0);
        return fields * (FIELD_HEIGHT + GAP) + (cosmic ? 32 : 0)
                + oversample.preferredHeight() + GAP * 2 + 150;
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        int height = getArea().h();
        int y = 0;
        y = placeField(fontName, width, y);
        y = placeField(fontPath, width, y);
        y = placeField(fallbacks, width, y);
        y = placeField(shadowMasks, width, y);
        if (cosmic) {
            for (IWidget field : cosmicFields) y = placeField(field, width, y);
            NfrLayout.place(variantOnly, 0, y, width, 24);
            y += 24 + GAP;
        }
        y = placeField(metrics, width, y);
        int sliderHeight = oversample.preferredHeight();
        NfrLayout.place(oversample, 0, y, width, sliderHeight);
        y += sliderHeight + GAP;
        NfrLayout.place(preview, 0, y, width, Math.max(0, height - y));
        return true;
    }

    private static int placeField(IWidget field, int width, int y) {
        NfrLayout.place(field, 0, y, width, FIELD_HEIGHT);
        return y + FIELD_HEIGHT + GAP;
    }
}
