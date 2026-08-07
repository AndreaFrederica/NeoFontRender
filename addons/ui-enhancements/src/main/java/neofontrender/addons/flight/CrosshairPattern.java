package neofontrender.addons.flight;

import java.util.ArrayList;
import java.util.List;

/** Compact, forgiving serialization for CCM-style user-drawn crosshairs. */
final class CrosshairPattern {
    private CrosshairPattern() {}

    static boolean[][] parse(String value, int size) {
        int safeSize = Math.max(1, size);
        boolean[][] pixels = new boolean[safeSize][safeSize];
        if (value == null || value.trim().isEmpty()) return pixels;
        for (String coordinate : value.split(";")) {
            String[] parts = coordinate.trim().split(",");
            if (parts.length != 2) continue;
            try {
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                if (x >= 0 && x < safeSize && y >= 0 && y < safeSize) pixels[x][y] = true;
            } catch (NumberFormatException ignored) { }
        }
        return pixels;
    }

    static String serialize(boolean[][] pixels) {
        if (pixels == null) return "";
        List<String> coordinates = new ArrayList<>();
        for (int x = 0; x < pixels.length; x++) {
            if (pixels[x] == null) continue;
            for (int y = 0; y < pixels[x].length; y++) {
                if (pixels[x][y]) coordinates.add(x + "," + y);
            }
        }
        return String.join(";", coordinates);
    }
}
