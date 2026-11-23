package neofontrender.lab;

import neofontrender.text.pipeline.TextPipelinePlugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** Isolated SPI loader that also exposes nested dependency JARs from a Forge mod JAR. */
final class TextPluginLoader implements AutoCloseable {
    private final List<URLClassLoader> loaders = new ArrayList<>();
    private final List<Path> temporaryFiles = new ArrayList<>();
    private final List<AutoCloseable> closeables = new ArrayList<>();

    List<TextPipelinePlugin> builtIn() {
        return track(providers(ServiceLoader.load(TextPipelinePlugin.class)));
    }

    List<TextPipelinePlugin> load(Path jar) throws IOException {
        Path source = jar.toAbsolutePath().normalize();
        if (!Files.isRegularFile(source)) throw new IOException("Plugin JAR does not exist: " + source);
        List<URL> urls = new ArrayList<>();
        urls.add(source.toUri().toURL());
        Path tempRoot = Files.createTempDirectory("nfr-text-plugin-");
        temporaryFiles.add(tempRoot);
        try (JarFile archive = new JarFile(source.toFile())) {
            java.util.Enumeration<JarEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".jar")) continue;
                Path nested = tempRoot.resolve(Path.of(entry.getName()).getFileName().toString());
                try (InputStream input = archive.getInputStream(entry)) {
                    Files.copy(input, nested, StandardCopyOption.REPLACE_EXISTING);
                }
                urls.add(nested.toUri().toURL());
            }
        }
        URLClassLoader loader = new PluginClassLoader(urls.toArray(new URL[0]),
                TextPipelinePlugin.class.getClassLoader());
        loaders.add(loader);
        List<TextPipelinePlugin> found = providers(ServiceLoader.load(TextPipelinePlugin.class, loader));
        if (found.isEmpty()) {
            try {
                Object value = Class.forName("neofontrender.uie.text.v3.UiEnhancementsTextPlugin",
                        true, loader).getConstructor().newInstance();
                if (value instanceof TextPipelinePlugin) found = List.of((TextPipelinePlugin) value);
            } catch (ReflectiveOperationException error) {
                throw new IOException("No TextPipelinePlugin provider in " + source, error);
            }
        }
        return track(found);
    }

    /** UIE implementation classes are child-first; the shared SPI remains parent-owned. */
    private static final class PluginClassLoader extends URLClassLoader {
        PluginClassLoader(URL[] urls, ClassLoader parent) { super(urls, parent); }

        @Override protected Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            if (name.startsWith("neofontrender.uie.text.v3.")) {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> loaded = findLoadedClass(name);
                    if (loaded == null) {
                        try { loaded = findClass(name); }
                        catch (ClassNotFoundException ignored) { loaded = super.loadClass(name, false); }
                    }
                    if (resolve) resolveClass(loaded);
                    return loaded;
                }
            }
            return super.loadClass(name, resolve);
        }
    }

    private static List<TextPipelinePlugin> providers(ServiceLoader<TextPipelinePlugin> service) {
        List<TextPipelinePlugin> result = new ArrayList<>();
        for (TextPipelinePlugin plugin : service) result.add(plugin);
        return Collections.unmodifiableList(result);
    }

    private List<TextPipelinePlugin> track(List<TextPipelinePlugin> plugins) {
        for (TextPipelinePlugin plugin : plugins) {
            if (plugin instanceof AutoCloseable) closeables.add((AutoCloseable) plugin);
        }
        return plugins;
    }

    @Override public void close() {
        List<AutoCloseable> reversedCloseables = new ArrayList<>(closeables);
        Collections.reverse(reversedCloseables);
        for (AutoCloseable closeable : reversedCloseables) {
            try { closeable.close(); } catch (Exception ignored) {}
        }
        closeables.clear();
        for (URLClassLoader loader : loaders) try { loader.close(); } catch (IOException ignored) {}
        List<Path> reversed = new ArrayList<>(temporaryFiles);
        Collections.reverse(reversed);
        for (Path root : reversed) {
            try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                });
            } catch (IOException ignored) {}
        }
    }
}
