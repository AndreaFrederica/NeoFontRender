package neofontrender.typst;

import java.util.ArrayList;
import java.util.List;

/** Immutable download/compiler status transferred independently of raster work. */
public record TypstEvent(String state, String packageSpec, long downloaded, long total, String detail) {
    public boolean failed() { return state.endsWith("failed"); }
    public boolean active() {
        return state.equals("connecting") || state.equals("downloading")
                || state.equals("extracting") || state.equals("compiling")
                || state.equals("queued") || state.equals("deleting");
    }

    static List<TypstEvent> decode(String[] fields) {
        if (fields == null || fields.length % 5 != 0) {
            throw new IllegalStateException("Invalid Typst status payload");
        }
        List<TypstEvent> result = new ArrayList<>();
        for (int i = 0; i < fields.length; i += 5) {
            result.add(new TypstEvent(fields[i], fields[i + 1], Long.parseLong(fields[i + 2]),
                    fields[i + 3].isEmpty() ? -1 : Long.parseLong(fields[i + 3]), fields[i + 4]));
        }
        return List.copyOf(result);
    }
}
