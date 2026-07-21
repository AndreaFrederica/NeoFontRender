package neofontrender.client.gui.model;

import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

/** Mutable bindings for font form fields refreshed by font-list selection. */
public final class NfrFontFieldRefs {
    public TextFieldWidget fontName;
    public TextFieldWidget fontPath;
    public TextFieldWidget fallbacks;
    public TextFieldWidget shadowMasks;
    public TextFieldWidget cosmicRegular;
    public TextFieldWidget cosmicBold;
    public TextFieldWidget cosmicItalic;
    public TextFieldWidget cosmicBoldItalic;

    public void refresh(String name, String path, String fallbackList, String masks,
                        String regular, String bold, String italic, String boldItalic) {
        set(fontName, name);
        set(fontPath, path);
        set(fallbacks, fallbackList);
        set(shadowMasks, masks);
        set(cosmicRegular, regular);
        set(cosmicBold, bold);
        set(cosmicItalic, italic);
        set(cosmicBoldItalic, boldItalic);
    }

    private static void set(TextFieldWidget field, String value) {
        if (field != null) field.setText(value == null ? "" : value);
    }
}
