package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.value.IStringValue;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Simple live String binding for ModularUI text fields. */
public final class NfrStringValue implements IStringValue<String> {
    private final Supplier<String> getter;
    private final Consumer<String> setter;

    public NfrStringValue(Supplier<String> getter, Consumer<String> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public String getStringValue() {
        return getter.get();
    }

    @Override
    public void setStringValue(String value) {
        setter.accept(value);
    }

    @Override
    public String getValue() {
        return getStringValue();
    }

    @Override
    public void setValue(String value) {
        setStringValue(value);
    }

    @Override
    public Class<String> getValueType() {
        return String.class;
    }
}
