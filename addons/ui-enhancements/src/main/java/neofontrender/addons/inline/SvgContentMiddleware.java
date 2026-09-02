package neofontrender.addons.inline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import neofontrender.api.text.pipeline.InlineContent;
import neofontrender.api.text.pipeline.InlineContentMatch;
import neofontrender.api.text.pipeline.InlineContentMiddleware;
import neofontrender.api.text.pipeline.TextTrigger;
import neofontrender.addons.chat.EnhancedChatFeatures;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Explicit local, resource-pack and allowlisted external SVG token provider. */
final class SvgContentMiddleware implements InlineContentMiddleware {
    @Override public String id() { return "neofontrender_ui_enhancements:svg"; }
    @Override public int priority() { return 90; }
    @Override public TextTrigger trigger() { return TextTrigger.exact('<'); }
    @Override public boolean isEnabled() { return EmbeddedContentConfig.svgEnabled(); }

    @Nullable
    @Override public InlineContentMatch match(CharSequence source, int sourceIndex) {
        SvgTokenParser.Match token = SvgTokenParser.match(source, sourceIndex);
        if (token == null) return null;
        Source svg = resolve(token.reference);
        if (svg == null) return null;
        boolean full = EmbeddedContentConfig.fullSvgEnabled();
        InlineContent glyph = RasterGlyphService.INSTANCE.glyph(
                "svg\u0000" + full + "\u0000" + svg.key,
                svg.description,
                token.height,
                false,
                false,
                () -> SvgRasterizer.rasterize(svg.load(), svg.baseUri, full, 1024));
        return glyph == null ? null : new InlineContentMatch(token.start, token.end, glyph);
    }

    @Nullable private static Source resolve(String reference) {
        if (reference.startsWith("local:")) {
            String alias = reference.substring("local:".length()).trim();
            Path path = LocalImageCatalog.INSTANCE.image(alias);
            if (path == null || !path.getFileName().toString().toLowerCase().endsWith(".svg")) return null;
            return new Source(path.toUri().toString(), "SVG " + alias, path.toUri(), () -> readLocal(path));
        }
        if (reference.startsWith("resource:")) {
            ResourceLocation location = parseResourceLocation(
                    reference.substring("resource:".length()));
            if (location == null) return null;
            return new Source(location.toString(), "SVG " + location, null,
                    () -> readResource(location));
        }
        try {
            URI uri = URI.create(reference);
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !EnhancedChatFeatures.externalImageGlyphs()) return null;
            return new Source(uri.normalize().toString(), "External SVG \u00b7 " + uri.getHost(), uri,
                    () -> InlineImageService.downloadExternal(uri));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * ResourceLocation's one-string constructor has changed validation details between
     * Forge/Cleanroom builds. Parse the explicit namespace:path form ourselves so the
     * middleware remains stable across those runtime versions.
     */
    @Nullable
    static ResourceLocation parseResourceLocation(String raw) {
        if (raw == null) return null;
        String value = raw.trim();
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1
                || separator != value.lastIndexOf(':')) return null;
        String namespace = value.substring(0, separator).toLowerCase(Locale.ROOT);
        String path = value.substring(separator + 1).replace('\\', '/');
        while (path.startsWith("/")) path = path.substring(1);
        if (path.isEmpty() || path.contains("..") || path.startsWith("/")
                || !path.toLowerCase(Locale.ROOT).endsWith(".svg")) return null;
        if (!namespace.matches("[a-z0-9_.-]+") || !path.matches("[a-zA-Z0-9_./-]+")) {
            return null;
        }
        try {
            return new ResourceLocation(namespace, path);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static byte[] readLocal(Path path) throws IOException {
        if (!Files.isRegularFile(path) || Files.size(path) > 8L * 1024L * 1024L) {
            throw new IOException("Local SVG is missing or exceeds 8 MiB");
        }
        return Files.readAllBytes(path);
    }

    private static byte[] readResource(ResourceLocation location) throws IOException {
        try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
             InputStream input = resource.getInputStream()) {
            return readLimited(input);
        }
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > 8 * 1024 * 1024) throw new IOException("SVG exceeds 8 MiB");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    @FunctionalInterface private interface Loader { byte[] load() throws Exception; }

    private static final class Source {
        final String key;
        final String description;
        final URI baseUri;
        final Loader loader;

        Source(String key, String description, URI baseUri, Loader loader) {
            this.key = key;
            this.description = description;
            this.baseUri = baseUri;
            this.loader = loader;
        }

        byte[] load() throws Exception { return loader.load(); }
    }
}
