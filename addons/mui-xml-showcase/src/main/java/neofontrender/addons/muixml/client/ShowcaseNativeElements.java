package neofontrender.addons.muixml.client;

import com.cleanroommc.modularui.api.component.MuiElementDescriptor;
import com.cleanroommc.modularui.api.component.MuiElementRegistry;
import com.cleanroommc.modularui.api.component.MuiPropertyDescriptor;
import com.cleanroommc.modularui.api.dom.MuiElement;
import com.cleanroommc.modularui.api.state.MuiStore;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

final class ShowcaseNativeElements {
    private ShowcaseNativeElements() {}

    static void register(MuiElementRegistry registry, MuiStore store) {
        registry.register(MuiElementDescriptor.builder("nfr:slider", SliderWidget.class,
                        element -> slider(element, store))
                .property(new MuiPropertyDescriptor<>("min", value -> decimal(value, 0),
                        (widget, value) -> widget.bounds(value, widget.getMax())))
                .property(new MuiPropertyDescriptor<>("max", value -> decimal(value, 100),
                        (widget, value) -> widget.bounds(widget.getMin(), value)))
                .property(new MuiPropertyDescriptor<>("value", value -> decimal(value, 50),
                        (widget, value) -> widget.setValue(value, true)))
                .build());
        registry.register(MuiElementDescriptor.builder("nfr:text-field", TextFieldWidget.class,
                        element -> textField(element, store))
                .property(new MuiPropertyDescriptor<>("max-length", value -> integer(value, 64),
                        TextFieldWidget::setMaxLength))
                .build());
    }

    private static SliderWidget slider(MuiElement element, MuiStore store) {
        double min = decimal(element.getAttribute("min"), 0);
        double max = decimal(element.getAttribute("max"), 100);
        double initial = decimal(element.getAttribute("value"), (min + max) / 2);
        String key = element.getAttribute("store-key");
        DoubleValue.Dynamic value = new DoubleValue.Dynamic(
                () -> number(store, key, initial), changed -> {
                    if (key != null) store.set(key, changed);
                });
        NfrXmlSliderWidget slider = new NfrXmlSliderWidget();
        slider.bounds(min, max).value(value);
        double step = decimal(element.getAttribute("step"), 0);
        if (step > 0) slider.stopper(step);
        return slider;
    }

    private static TextFieldWidget textField(MuiElement element, MuiStore store) {
        String key = element.getAttribute("store-key");
        String fallback = element.getAttribute("value");
        StringValue.Dynamic value = new StringValue.Dynamic(
                () -> string(store, key, fallback == null ? "" : fallback), changed -> {
                    if (key != null) store.set(key, changed);
                });
        return new TextFieldWidget().value(value).autoUpdateOnChange(true)
                .setMaxLength(integer(element.getAttribute("max-length"), 64));
    }

    private static double number(MuiStore store, String key, double fallback) {
        Object value = key == null ? null : store.get(key);
        return value instanceof Number ? ((Number) value).doubleValue() : fallback;
    }

    private static String string(MuiStore store, String key, String fallback) {
        Object value = key == null ? null : store.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static double decimal(String value, double fallback) {
        return value == null ? fallback : Double.parseDouble(value.trim());
    }

    private static int integer(String value, int fallback) {
        return value == null ? fallback : Integer.parseInt(value.trim());
    }
}
