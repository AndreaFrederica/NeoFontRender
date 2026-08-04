package neofontrender.addons.inline;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import neofontrender.addons.api.inline.ExternalImagePolicy;
import neofontrender.addons.api.inline.InlineGlyph;
import neofontrender.addons.api.inline.InlineImageHandle;
import neofontrender.addons.chat.EnhancedChatFeatures;
import neofontrender.addons.ui.NfrUiEnhancements;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Asynchronous, policy-gated remote image cache shared by emoji and external image providers. */
public enum InlineImageService {
    INSTANCE;

    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int MAX_SOURCE_DIMENSION = 16384;
    private static final long MAX_SOURCE_PIXELS = 64L * 1024L * 1024L;
    private static final int MAX_TEXTURE_DIMENSION = 1024;
    private static final int MAX_TEXTURES = 256;
    private static final String GOSLING_HOSTS = "cdnjs.cloudflare.com,cdn.discordapp.com";

    private final ExecutorService downloads = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "NFR Inline Image Loader");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Key, Handle> handles = new ConcurrentHashMap<>();

    @Nullable
    public InlineGlyph glyph(URI uri, String description, boolean goslingSource) {
        ExternalImagePolicy policy = policy(goslingSource);
        if (!policy.allows(uri)) return null;
        Key key = new Key(uri.normalize(), goslingSource);
        Handle handle = handles.computeIfAbsent(key, ignored -> {
            Handle created = new Handle(key.uri);
            downloads.execute(() -> load(created, goslingSource));
            return created;
        });
        handle.lastAccess = System.nanoTime();
        if (handle.state == InlineImageHandle.State.FAILED) return null;
        return new RemoteImageGlyph(handle, description == null ? uri.toString() : description,
                goslingSource);
    }

    @Nullable
    InlineGlyph localGlyph(Path path, String description) {
        Path normalized = path.toAbsolutePath().normalize();
        URI uri = normalized.toUri();
        Key key = new Key(uri, false);
        Handle handle = handles.computeIfAbsent(key, ignored -> {
            Handle created = new Handle(uri);
            downloads.execute(() -> loadLocal(created, normalized));
            return created;
        });
        handle.lastAccess = System.nanoTime();
        if (handle.state == InlineImageHandle.State.FAILED) return null;
        return new RemoteImageGlyph(handle, description == null ? normalized.getFileName().toString() : description,
                false);
    }

    /** Policy-aware public bridge used by the inline glyph API. */
    @Nullable
    public InlineGlyph localGlyph(String alias, String description) {
        if (!EnhancedChatFeatures.localImageGlyphs()) return null;
        Path path = LocalImageCatalog.INSTANCE.image(alias);
        return path == null ? null : localGlyph(path, description);
    }

    private static ExternalImagePolicy policy(boolean goslingSource) {
        String custom = EnhancedChatFeatures.imageAllowlist();
        String allow = goslingSource ? GOSLING_HOSTS + "," + custom : custom;
        return new ExternalImagePolicy(allow, EnhancedChatFeatures.imageBlocklist());
    }

    private void load(Handle handle, boolean goslingSource) {
        try {
            byte[] bytes = readCache(handle.uri);
            BufferedImage image = null;
            if (bytes != null) {
                try {
                    image = decode(bytes);
                } catch (IOException corruptCache) {
                    deleteCache(handle.uri);
                }
            }
            if (image == null) {
                bytes = download(handle.uri, goslingSource);
                image = decode(bytes);
                // Persist only content that has passed format and dimension validation.
                writeCache(handle.uri, image);
            }
            BufferedImage decoded = image;
            Minecraft.getMinecraft().addScheduledTask(() -> upload(handle, decoded));
        } catch (Throwable failure) {
            handle.state = InlineImageHandle.State.FAILED;
            NfrUiEnhancements.LOGGER.debug("Inline image rejected or unavailable: {}", handle.uri, failure);
        }
    }

    private void loadLocal(Handle handle, Path path) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_BYTES) {
                throw new IOException("Local image is missing or exceeds byte limit");
            }
            BufferedImage image = decode(Files.readAllBytes(path));
            Minecraft.getMinecraft().addScheduledTask(() -> upload(handle, image));
        } catch (Throwable failure) {
            handle.state = InlineImageHandle.State.FAILED;
            NfrUiEnhancements.LOGGER.debug("Local inline image rejected or unavailable: {}", path, failure);
        }
    }

    private static byte[] download(URI initial, boolean goslingSource) throws IOException {
        URI current = initial;
        for (int redirects = 0; redirects <= MAX_REDIRECTS; redirects++) {
            ExternalImagePolicy policy = policy(goslingSource);
            if (!policy.allows(current)) throw new IOException("URL is not allowlisted");
            validateResolvedAddresses(current);
            URLConnection raw = current.toURL().openConnection();
            if (!(raw instanceof HttpURLConnection)) throw new IOException("Unsupported connection");
            HttpURLConnection connection = (HttpURLConnection) raw;
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "image/png,image/gif,image/jpeg,image/webp;q=0.8");
            connection.setRequestProperty("User-Agent", "NFR-UIE/inline-image");
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status < 400) {
                    if (redirects == MAX_REDIRECTS) throw new IOException("Too many redirects");
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Redirect without Location");
                    current = current.resolve(location);
                    continue;
                }
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP " + status);
                }
                String contentType = connection.getContentType();
                if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                    throw new IOException("Response is not an image");
                }
                int declared = connection.getContentLength();
                if (declared > MAX_BYTES) throw new IOException("Image exceeds byte limit");
                try (InputStream input = connection.getInputStream()) {
                    return readLimited(input);
                }
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Redirect loop");
    }

    private static void validateResolvedAddresses(URI uri) throws IOException {
        InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
        if (addresses.length == 0) throw new IOException("Host did not resolve");
        for (InetAddress address : addresses) {
            if (ExternalImagePolicy.unsafeAddress(address)) {
                throw new IOException("Host resolves to a private or local address");
            }
        }
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(32 * 1024);
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > MAX_BYTES) throw new IOException("Image exceeds byte limit");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static BufferedImage decode(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new IOException("No image input stream");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported image format");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_SOURCE_DIMENSION
                        || height > MAX_SOURCE_DIMENSION
                        || (long) width * height > MAX_SOURCE_PIXELS) {
                    throw new IOException("Source image dimensions exceed safety limits");
                }
                int sample = Math.max(1, (Math.max(width, height)
                        + MAX_TEXTURE_DIMENSION - 1) / MAX_TEXTURE_DIMENSION);
                ImageReadParam parameters = reader.getDefaultReadParam();
                if (sample > 1) parameters.setSourceSubsampling(sample, sample, 0, 0);
                BufferedImage image = reader.read(0, parameters);
                if (image == null) throw new IOException("Could not decode image");
                if (image.getWidth() > MAX_TEXTURE_DIMENSION
                        || image.getHeight() > MAX_TEXTURE_DIMENSION) {
                    throw new IOException("Downsampled texture dimensions exceed limits");
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private void upload(Handle handle, BufferedImage image) {
        try {
            DynamicTexture texture = new DynamicTexture(image);
            texture.setBlurMipmap(true, false);
            ResourceLocation location = Minecraft.getMinecraft().getTextureManager()
                    .getDynamicTextureLocation("nfr_inline_image", texture);
            handle.width = image.getWidth();
            handle.height = image.getHeight();
            handle.image = image;
            handle.texture = texture;
            handle.location = location;
            handle.state = InlineImageHandle.State.READY;
            evictOldTextures();
        } catch (Throwable failure) {
            handle.state = InlineImageHandle.State.FAILED;
            NfrUiEnhancements.LOGGER.debug("Could not upload inline image {}", handle.uri, failure);
        }
    }

    private void evictOldTextures() {
        while (handles.size() > MAX_TEXTURES) {
            Map.Entry<Key, Handle> oldest = null;
            for (Map.Entry<Key, Handle> entry : handles.entrySet()) {
                if (entry.getValue().state != InlineImageHandle.State.READY) continue;
                if (oldest == null || entry.getValue().lastAccess < oldest.getValue().lastAccess) {
                    oldest = entry;
                }
            }
            if (oldest == null || !handles.remove(oldest.getKey(), oldest.getValue())) return;
            Handle handle = oldest.getValue();
            if (handle.location != null) {
                Minecraft.getMinecraft().getTextureManager().deleteTexture(handle.location);
            }
            handle.location = null;
            handle.texture = null;
            handle.image = null;
            handle.state = InlineImageHandle.State.FAILED;
        }
    }

    @Nullable
    private static byte[] readCache(URI uri) {
        try {
            Path file = cacheFile(uri);
            if (!Files.isRegularFile(file) || Files.size(file) > MAX_BYTES) return null;
            return Files.readAllBytes(file);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void writeCache(URI uri, BufferedImage image) {
        Path temporary = null;
        try {
            Path target = cacheFile(uri);
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), "inline-", ".png.tmp");
            if (!ImageIO.write(image, "png", temporary.toFile())) {
                throw new IOException("No PNG writer is available");
            }
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (Throwable ignored) {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignoredToo) {}
        }
    }

    private static void deleteCache(URI uri) {
        try {
            Files.deleteIfExists(cacheFile(uri));
        } catch (Throwable ignored) {
        }
    }

    private static Path cacheFile(URI uri) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(uri.toASCIIString().getBytes(StandardCharsets.UTF_8));
        StringBuilder name = new StringBuilder(64);
        for (byte value : hash) name.append(String.format("%02x", value & 0xff));
        return cacheRoot(Minecraft.getMinecraft().gameDir.toPath()).resolve(name + ".png");
    }

    static Path cacheRoot(Path gameDirectory) {
        return gameDirectory.resolve("neofontrender").resolve("image-cache");
    }

    private static final class Key {
        private final URI uri;
        private final boolean gosling;

        private Key(URI uri, boolean gosling) { this.uri = uri; this.gosling = gosling; }
        @Override public boolean equals(Object other) {
            return other instanceof Key && gosling == ((Key) other).gosling
                    && uri.equals(((Key) other).uri);
        }
        @Override public int hashCode() { return 31 * uri.hashCode() + (gosling ? 1 : 0); }
    }

    static final class Handle implements InlineImageHandle {
        private final URI uri;
        private volatile State state = State.LOADING;
        private volatile int width;
        private volatile int height;
        private volatile DynamicTexture texture;
        private volatile ResourceLocation location;
        private volatile BufferedImage image;
        private volatile long lastAccess = System.nanoTime();

        private Handle(URI uri) { this.uri = uri; }
        @Override public URI uri() { return uri; }
        @Override public State state() { return state; }
        @Override public int pixelWidth() { return width; }
        @Override public int pixelHeight() { return height; }
        @Override public ResourceLocation texture() { return location; }
        BufferedImage image() { return image; }
    }
}
