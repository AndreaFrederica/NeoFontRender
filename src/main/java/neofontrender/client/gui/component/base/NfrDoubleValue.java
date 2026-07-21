package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.value.IDoubleValue;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Simple live double binding for ModularUI numeric widgets. */
public final class NfrDoubleValue implements IDoubleValue<Double> {
    private final Supplier<Double> getter;
    private final Consumer<Double> setter;

    public NfrDoubleValue(Supplier<Double> getter, Consumer<Double> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public double getDoubleValue() {
        return getter.get();
    }

    @Override
    public void setDoubleValue(double value) {
        setter.accept(value);
    }

    @Override
    public Double getValue() {
        return getDoubleValue();
    }

    @Override
    public void setValue(Double value) {
        setDoubleValue(value);
    }

    @Override
    public Class<Double> getValueType() {
        return Double.class;
    }
}
