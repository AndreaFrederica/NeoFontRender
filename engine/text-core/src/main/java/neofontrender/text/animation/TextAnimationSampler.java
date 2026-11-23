package neofontrender.text.animation;

import neofontrender.text.StructuredEffectSpan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Deterministic, allocation-light sampler for TextAnimator-compatible effects. */
public final class TextAnimationSampler {
    private static final int[] TYPEWRITER_INTERVALS = {4, 8, 12, 16, 20, 27, 36, 50, 70};
    private static final java.util.Map<String, TypewriterTrack> TYPEWRITER_TRACKS =
            java.util.Collections.synchronizedMap(new java.util.LinkedHashMap<String, TypewriterTrack>() {
                @Override protected boolean removeEldestEntry(
                        java.util.Map.Entry<String, TypewriterTrack> eldest) {
                    return size() > 512;
                }
            });

    public Sample sample(TextAnimationEngine.GlyphAnimation glyph, long timeMillis) {
        return sample(glyph, timeMillis, false, 0.0F, 0.0F, 0xFFFFFFFF);
    }

    public Sample sample(TextAnimationEngine.GlyphAnimation glyph, long timeMillis, boolean shadow) {
        return sample(glyph, timeMillis, shadow, 0.0F, 0.0F, 0xFFFFFFFF);
    }

    public Sample sample(TextAnimationEngine.GlyphAnimation glyph, long timeMillis, boolean shadow,
                         float shadowOffsetX, float shadowOffsetY, int baseArgb) {
        Sample out = new Sample(baseArgb);
        for (StructuredEffectSpan span : glyph.effects()) {
            String name = effectName(span);
            if ("neon".equals(name)) {
                // Shader capture renders the glyph once into an alpha mask. Keep the original
                // repeated-glyph implementation for direct/fallback rendering only.
                if (TextAnimationFrame.neonOverdrawSuppressed()) continue;
                out.addGlow(new Glow((int) Math.max(4, number(span, "p", 10)),
                        (float) number(span, "r", 2.0),
                        (float) number(span, "a", 0.12)));
                continue;
            }
            int size = out.mutableLayers.size();
            for (int index = 0; index < size; index++) {
                apply(out, out.mutableLayers.get(index), span, glyph, timeMillis, shadow,
                        shadowOffsetX, shadowOffsetY);
            }
        }
        out.updateLegacySiblingView();
        return out;
    }

    private void apply(Sample root, Sample out, StructuredEffectSpan span,
                       TextAnimationEngine.GlyphAnimation glyph, long millis, boolean shadowPass,
                       float shadowOffsetX, float shadowOffsetY) {
        int index = glyph.animationIndex();
        int codePoint = glyph.codePoint();
        String name = effectName(span);
        switch (name) {
            case "wave":
                out.y += Math.sin(millis * 0.01 * number(span, "f", 1.0)
                        + index * number(span, "w", 1.0)) * 2.0 * number(span, "a", 1.0);
                break;
            case "wiggle":
                double direction = direction(codePoint);
                double delta = Math.sin(millis * 0.01 * number(span, "f", 1.0)
                        + index * 2.0 * number(span, "w", 1.0)) * 1.5
                        * number(span, "a", 1.0);
                out.x += Math.cos(direction) * delta;
                out.y += Math.sin(direction) * delta;
                break;
            case "shake": shake(out, span, index, codePoint, millis); break;
            case "bounce": out.y -= bounce(millis, span, index); break;
            case "swing": out.rotation += Math.sin(millis * 0.003 * number(span, "f", 1.0)
                    + index * number(span, "w", 0.0)) * number(span, "a", 1.0) * 0.5; break;
            case "pend":
                double pendPhase = millis * 0.002 * number(span, "f", 1.0) - index * 0.1;
                out.pendulumRotation += Math.sin(pendPhase)
                        * Math.toRadians(number(span, "a", 30.0));
                double radius = number(span, "r", 0.0);
                out.x += Math.cos(pendPhase) * radius;
                out.y += Math.sin(pendPhase) * radius;
                break;
            case "turb":
                double amp = number(span, "a", 1.0) * 1.5;
                double t = millis * 0.002 * number(span, "f", 1.0);
                out.x += Math.sin(t * 1.7 + index * 0.31 + codePoint * 0.07) * amp;
                out.y += Math.sin(t * 2.3 + index * 0.27 + codePoint * 0.11) * amp;
                break;
            case "fade":
                double fadePhase = millis * 0.002 * number(span, "f", 1.0)
                        + index * number(span, "w", 0.0);
                double minAlpha = number(span, "a", 0.3);
                out.alpha *= (float) (minAlpha + (1.0 - minAlpha)
                        * (0.5 + 0.5 * Math.sin(fadePhase)));
                break;
            case "pulse":
                if (shadowPass) break;
                double pulsePhase = millis * 0.002 * number(span, "f", 1.0)
                        + index * number(span, "w", 0.0);
                // Keep TextAnimator's base/a contract (a is a quarter-range amplitude).
                // Explicit min/max is an opt-in NFR extension for unambiguous endpoints.
                boolean explicitRange = span.parameters().containsKey("min")
                        || span.parameters().containsKey("max");
                boolean explicitLegacy = span.parameters().containsKey("base")
                        || span.parameters().containsKey("a");
                double minimum = explicitRange ? number(span, "min", 0.75)
                        : explicitLegacy ? number(span, "base", 0.75)
                        : number(span, "_defaultMin", 0.75);
                double amplitude = explicitRange ? number(span, "max", 1.0) - minimum
                        : explicitLegacy ? number(span, "a", 1.0) * 0.25
                        : number(span, "_defaultMax", 1.0) - minimum;
                double brightness = minimum + amplitude * (0.5 + 0.5 * Math.sin(pulsePhase));
                out.colorMultiplier *= (float) brightness;
                out.multiplyColor(brightness);
                break;
            case "glitch":
                glitch(root, out, span, index, codePoint, millis, shadowPass,
                        shadowOffsetX, shadowOffsetY);
                break;
            case "typewriter":
                int speed = (int) Math.max(1, Math.min(9, number(span, "speed", 5)));
                int revealed = typewriterIndex(glyph.timelineKey(span), millis,
                        TYPEWRITER_INTERVALS[9 - speed]);
                boolean byWord = "by_word".equals(span.parameters().get("mode"));
                out.visible &= glyph.revealIndex(span, byWord) < revealed;
                break;
            case "rainb":
                if (shadowPass) break;
                double hue = ((millis * 0.02 * number(span, "f", 1.0)
                        + index * number(span, "w", 1.0)) % 30.0) / 30.0;
                out.setRgb(hsv((hue % 1.0 + 1.0) % 1.0, 0.8, 0.8));
                break;
            case "grad": if (!shadowPass) out.setRgb(gradient(span, index, millis)); break;
            case "shadow":
                if (!shadowPass) break;
                out.shadow = true;
                out.shadowX = number(span, "x", 0.0);
                out.shadowY = number(span, "y", 0.0);
                out.x += out.shadowX;
                out.y += out.shadowY;
                out.alpha *= (float) number(span, "a", 1.0);
                String color = span.parameters().get("c");
                out.setRgb(color == null ? componentColor(span)
                        : parseColor(color, componentColor(span)));
                break;
            case "scroll": out.x -= (millis * 0.04 * number(span, "f", 1.0)) % 40.0; break;
            default: break;
        }
    }

    private static String effectName(StructuredEffectSpan span) {
        String id = span.effectId();
        return id.substring(id.lastIndexOf(':') + 1);
    }

    private static int typewriterIndex(String key, long now, int interval) {
        synchronized (TYPEWRITER_TRACKS) {
            TypewriterTrack track = TYPEWRITER_TRACKS.get(key);
            if (track == null || now < track.changedSince || now - track.lastAccess > 1_000L) {
                track = new TypewriterTrack(now);
                TYPEWRITER_TRACKS.put(key, track);
            } else if (now - track.changedSince > interval) {
                // The original advances at most once per rendered frame instead of catching up
                // after the text has not been drawn for a while.
                track.changedSince += interval;
                track.index++;
            }
            track.lastAccess = now;
            return track.index;
        }
    }

    private static int gradient(StructuredEffectSpan span, int index, long millis) {
        int a = color(span, "from", 0x5BCEFA), b = color(span, "to", 0xF5A9B8);
        double spanSize = number(span, "sp", 20.0);
        double amount = spanSize > 0 ? (index % spanSize) / spanSize : 0.0;
        amount = (amount + (number(span, "f", 0.0) > 0
                ? (millis * 0.001 * number(span, "f", 0.0)) % 1.0 : 0.0)) % 1.0;
        if (!bool(span, "uni", false)) {
            amount = amount < 0.5 ? amount * 2.0 : 2.0 - amount * 2.0;
        }
        if (bool(span, "hue", false)) {
            float[] ah = java.awt.Color.RGBtoHSB(a >> 16 & 255, a >> 8 & 255, a & 255, null);
            float[] bh = java.awt.Color.RGBtoHSB(b >> 16 & 255, b >> 8 & 255, b & 255, null);
            float hue = (float) ((ah[0] + (((bh[0] - ah[0] + 1.0) % 1.0 > 0.5 ?
                    (bh[0] - ah[0] - 1.0) : (bh[0] - ah[0])) * amount) + 1.0) % 1.0);
            return hsv(hue, ah[1] + (bh[1] - ah[1]) * amount,
                    ah[2] + (bh[2] - ah[2]) * amount);
        }
        int r = (int) (((a >> 16 & 255) * (1 - amount)) + ((b >> 16 & 255) * amount));
        int g = (int) (((a >> 8 & 255) * (1 - amount)) + ((b >> 8 & 255) * amount));
        int bl = (int) (((a & 255) * (1 - amount)) + ((b & 255) * amount));
        return r << 16 | g << 8 | bl;
    }

    private static int color(StructuredEffectSpan span, String key, int fallback) {
        return parseColor(span.parameters().get(key), fallback);
    }

    private static int parseColor(String value, int fallback) {
        if (value == null) return fallback;
        try { return (int) Long.parseLong(value.replace("#", ""), 16) & 0xFFFFFF; }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static int componentColor(StructuredEffectSpan span) {
        int red = colorComponent(number(span, "r", 0.0));
        int green = colorComponent(number(span, "g", 0.0));
        int blue = colorComponent(number(span, "b", 0.0));
        return red << 16 | green << 8 | blue;
    }

    private static int colorComponent(double value) {
        return (int) Math.max(0, Math.min(255, Math.round(value * 255.0)));
    }

    private static int hsv(double h, double s, double v) {
        int rgb = java.awt.Color.HSBtoRGB((float) h, (float) s, (float) v);
        return rgb & 0xFFFFFF;
    }

    private static double number(StructuredEffectSpan span, String key, double fallback) {
        try { return Double.parseDouble(span.parameters().getOrDefault(key, String.valueOf(fallback))); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private static boolean bool(StructuredEffectSpan span, String key, boolean fallback) {
        String value = span.parameters().get(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private static double direction(long index) {
        long hash = index * 0x9E3779B97F4A7C15L;
        return ((hash ^ (hash >>> 33)) & 0xFFFF) / 65535.0 * Math.PI * 2.0;
    }

    private static double bounce(long millis, StructuredEffectSpan span, int index) {
        double t = (millis * 0.001 * number(span, "f", 1.0) - index
                * number(span, "w", 1.0) * 0.2) % 1.0;
        double offset;
        if (t < 0.2) offset = Math.sin(t / 0.2 * Math.PI / 2.0);
        else if (t < 0.8) {
            double eased = (t - 0.2) / 0.6;
            if (eased < 1.0 / 2.75) offset = 7.5625 * eased * eased;
            else if (eased < 2.0 / 2.75) {
                eased -= 1.5 / 2.75;
                offset = 7.5625 * eased * eased + 0.75;
            } else if (eased < 2.5 / 2.75) {
                eased -= 2.25 / 2.75;
                offset = 7.5625 * eased * eased + 0.9375;
            } else {
                eased -= 2.625 / 2.75;
                offset = 7.5625 * eased * eased + 0.984375;
            }
            offset = 1.0 - offset;
        } else {
            offset = 0.0;
        }
        return offset * number(span, "a", 1.0) * 4.0;
    }

    private static void shake(Sample out, StructuredEffectSpan span, int index,
                              int codePoint, long millis) {
        double amplitude = number(span, "a", 1.0);
        // Epoch milliseconds already exceed int range after scaling. Narrowing the double
        // to int saturates at MAX_VALUE and freezes every glyph at the same direction.
        // Keep the time bucket as a long through hashing; the hash overflow is intentional.
        long directionSeed = (long) (millis * 0.01 * number(span, "f", 1.0)
                + codePoint + index);
        double angle = direction(directionSeed);
        out.x += Math.cos(angle) * 0.6 * amplitude;
        out.y += Math.sin(angle) * 0.6 * amplitude;
    }

    private static void glitch(Sample root, Sample out, StructuredEffectSpan span, int index,
                               int codePoint, long millis, boolean shadowPass,
                               float shadowOffsetX, float shadowOffsetY) {
        double time = millis * 0.025 * number(span, "f", 1.0);
        int pulse = (int) time % 3;
        java.util.Random random = new java.util.Random(index + codePoint + (long) (time * 1000));
        random.nextFloat();
        if (pulse == 1 && random.nextFloat() < number(span, "j", 0.015)) {
            out.x += (random.nextFloat() - 0.5) * 8.0;
            out.y += (random.nextFloat() - 0.5) * 4.0;
        }
        if (random.nextFloat() < number(span, "b", 0.003)) {
            out.alpha *= random.nextFloat() < 0.3F ? 0.0F : 0.3F;
        }
        float shiftChance = (float) number(span, "s", 0.08);
        time *= 2.0;
        long effectSeed = 31L * span.effectId().hashCode() + span.parameters().hashCode();
        java.util.Random sliceRandom = new java.util.Random((long) time * 1000L * effectSeed);
        if (sliceRandom.nextFloat() < shiftChance) {
            if (shadowPass) {
                root.mutableLayers.add(out.copyLayer());
                out.x -= shadowOffsetX;
                out.y -= shadowOffsetY;
            }
            float mask = 0.5F + (sliceRandom.nextFloat() - 0.5F) * 0.5F;
            double offset = 0.75 + sliceRandom.nextFloat() * 0.75;
            if (sliceRandom.nextBoolean()) offset = -offset;
            Sample sibling = out.copyLayer();
            sibling.x += offset;
            sibling.alpha *= Math.min(1.0F, 0.5F + sliceRandom.nextFloat());
            sibling.maskBottom = mask;
            if (shadowPass) {
                sibling.y -= 1.0;
                sibling.applyGlitchShadowSiblingColor();
            }
            root.mutableLayers.add(sibling);
            out.maskTop = 1.0F - mask;
            out.x -= offset;
            out.alpha *= Math.min(1.0F, 0.5F + sliceRandom.nextFloat());
            if (shadowPass) {
                out.y += 1.0;
                out.applyGlitchShadowMainColor();
            }
        }
    }

    public static final class Glow {
        public final int passes;
        public final float radius;
        public final float alphaMultiplier;

        private Glow(int passes, float radius, float alphaMultiplier) {
            this.passes = passes;
            this.radius = radius;
            this.alphaMultiplier = alphaMultiplier;
        }
    }

    public static final class Sample {
        /** Swing and pendulum rotations are stored in radians, matching the source mod. */
        public double x, y, rotation, pendulumRotation;
        public float alpha = 1.0F, scale = 1.0F, colorMultiplier = 1.0F;
        public boolean visible = true;
        public int rgb = -1;
        public int argb;
        public boolean glow, shadow;
        public float glowStrength, glowRadius;
        public int glowPasses;
        public double shadowX, shadowY;
        public boolean sibling;
        public double siblingX, siblingY;
        public float siblingAlpha = 1.0F;
        public float maskTop, maskBottom, siblingMaskTop, siblingMaskBottom;

        private final List<Sample> mutableLayers;
        private final List<Glow> mutableGlows;

        public Sample() {
            this(0xFFFFFFFF);
        }

        private Sample(int argb) {
            this(argb, true);
        }

        private Sample(int argb, boolean root) {
            this.argb = argb;
            this.mutableLayers = root ? new ArrayList<>() : null;
            this.mutableGlows = root ? new ArrayList<>() : null;
            if (root) this.mutableLayers.add(this);
        }

        public List<Sample> layers() {
            return mutableLayers == null
                    ? Collections.singletonList(this)
                    : Collections.unmodifiableList(mutableLayers);
        }

        public List<Glow> glows() {
            return mutableGlows == null
                    ? Collections.emptyList()
                    : Collections.unmodifiableList(mutableGlows);
        }

        private void addGlow(Glow value) {
            mutableGlows.add(value);
            glow = true;
            glowPasses = value.passes;
            glowRadius = value.radius;
            glowStrength = value.alphaMultiplier;
        }

        private Sample copyLayer() {
            Sample copy = new Sample(argb, false);
            copy.x = x;
            copy.y = y;
            copy.rotation = rotation;
            copy.pendulumRotation = pendulumRotation;
            copy.alpha = alpha;
            copy.scale = scale;
            copy.colorMultiplier = colorMultiplier;
            copy.visible = visible;
            copy.rgb = rgb;
            copy.shadow = shadow;
            copy.shadowX = shadowX;
            copy.shadowY = shadowY;
            copy.maskTop = maskTop;
            copy.maskBottom = maskBottom;
            return copy;
        }

        private void setRgb(int value) {
            rgb = value & 0xFFFFFF;
            argb = (argb & 0xFF000000) | rgb;
        }

        private void multiplyColor(double factor) {
            int red = clampColor(((argb >>> 16) & 255) * factor);
            int green = clampColor(((argb >>> 8) & 255) * factor);
            int blue = clampColor((argb & 255) * factor);
            setRgb(red << 16 | green << 8 | blue);
        }

        private void applyGlitchShadowSiblingColor() {
            int red = toggleHalf((argb >>> 16) & 255);
            int green = clampColor(((argb >>> 8) & 255) * 0.2);
            int blue = clampColor((argb & 255) * 0.2);
            setRgb(red << 16 | green << 8 | blue);
        }

        private void applyGlitchShadowMainColor() {
            int red = clampColor(((argb >>> 16) & 255) * 0.2);
            int green = toggleHalf((argb >>> 8) & 255);
            int blue = toggleHalf(argb & 255);
            setRgb(red << 16 | green << 8 | blue);
        }

        private void updateLegacySiblingView() {
            if (mutableLayers.size() <= 1) return;
            Sample extra = mutableLayers.get(1);
            sibling = true;
            siblingX = extra.x - x;
            siblingY = extra.y - y;
            siblingAlpha = alpha == 0.0F ? 0.0F : extra.alpha / alpha;
            siblingMaskTop = extra.maskTop;
            siblingMaskBottom = extra.maskBottom;
        }

        private static int toggleHalf(int component) {
            float value = component / 255.0F;
            return clampColor((value > 0.5F ? value - 0.5F : value + 0.5F) * 255.0F);
        }

        private static int clampColor(double value) {
            return (int) Math.max(0, Math.min(255, Math.round(value)));
        }
    }

    private static final class TypewriterTrack {
        long changedSince;
        long lastAccess;
        int index;

        TypewriterTrack(long now) {
            changedSince = now;
            lastAccess = now;
        }
    }
}
