package neofontrender.addons.controller;

/** Fixed-size time series backing the Arc3D diagnostics graph. */
final class ControllerInputHistory {
    static final int CAPACITY = 180;
    private final float[] raw = new float[CAPACITY];
    private final float[] filtered = new float[CAPACITY];
    private final float[] mapped = new float[CAPACITY];
    private int size;
    private long lastSample;

    void add(long sample, float rawValue, float filteredValue, float mappedValue) {
        if (sample == 0L || sample == lastSample) return;
        lastSample = sample;
        if (size < CAPACITY) {
            raw[size] = rawValue;
            filtered[size] = filteredValue;
            mapped[size] = mappedValue;
            size++;
            return;
        }
        System.arraycopy(raw, 1, raw, 0, CAPACITY - 1);
        System.arraycopy(filtered, 1, filtered, 0, CAPACITY - 1);
        System.arraycopy(mapped, 1, mapped, 0, CAPACITY - 1);
        raw[CAPACITY - 1] = rawValue;
        filtered[CAPACITY - 1] = filteredValue;
        mapped[CAPACITY - 1] = mappedValue;
    }

    int size() { return size; }
    float raw(int index) { return raw[index]; }
    float filtered(int index) { return filtered[index]; }
    float mapped(int index) { return mapped[index]; }

    void clear() {
        size = 0;
        lastSample = 0L;
    }
}
