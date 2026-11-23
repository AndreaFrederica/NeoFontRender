package neofontrender.addons.muixml.client;

import com.cleanroommc.modularui.api.markup.MuiResourceResolver;
import neofontrender.addons.muixml.MuiXmlShowcaseMod;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ShowcaseResourceResolver implements MuiResourceResolver {
    public static final String OWNER = MuiXmlShowcaseMod.MOD_ID;
    public static final String ROOT = "assets/" + OWNER + "/mui/";
    private final Map<String, String> openedSources = new LinkedHashMap<>();

    @Override
    public InputStream open(String owner, String resourceId) throws IOException {
        if (!OWNER.equals(owner)) return null;
        String normalized = normalize(resourceId);
        InputStream stream = ShowcaseResourceResolver.class.getClassLoader().getResourceAsStream(ROOT + normalized);
        if (stream == null) return null;
        byte[] bytes;
        try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) >= 0) output.write(buffer, 0, count);
            bytes = output.toByteArray();
        }
        this.openedSources.putIfAbsent(normalized, new String(bytes, StandardCharsets.UTF_8));
        return new ByteArrayInputStream(bytes);
    }

    public InputStream require(String resourceId) throws IOException {
        InputStream stream = open(OWNER, resourceId);
        if (stream == null) throw new IOException("Missing showcase resource: " + resourceId);
        return stream;
    }

    public Map<String, String> getOpenedSources() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.openedSources));
    }

    private static String normalize(String resourceId) throws IOException {
        if (resourceId == null) throw new IOException("Resource id is null");
        String value = resourceId.trim().replace('\\', '/');
        if (value.isEmpty() || value.startsWith("/") || value.contains("../") || value.contains("://")) {
            throw new IOException("Unsafe showcase resource id: " + resourceId);
        }
        return value;
    }
}
