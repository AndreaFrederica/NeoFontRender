package neofontrender.lab;

import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.StructuredText;
import neofontrender.text.StyledSpan;
import neofontrender.text.InlineSpan;
import neofontrender.text.layout.CjkLineBreakProvider;
import neofontrender.text.pipeline.StructuredTextMiddleware;
import neofontrender.text.pipeline.StructuredTextPipeline;
import neofontrender.text.pipeline.TextPipelinePlugin;
import neofontrender.text.pipeline.LineBreakOpportunityProvider;
import neofontrender.text.syntax.StandardSyntaxEngines;
import neofontrender.text.syntax.TextSyntaxEngine;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Standalone structured-text parser, CJK layout, and AWT/Cosmic raster laboratory. */
public final class TextRenderLab {
    private final TextSyntaxEngine syntax = StandardSyntaxEngines.minecraftWithBrilliantDefaults();
    private final TextPluginLoader pluginLoader = new TextPluginLoader();
    private final Map<String, TextPipelinePlugin> plugins = new LinkedHashMap<>();
    private StructuredTextPipeline pipeline;
    private final CjkLineBreakProvider cjk = new CjkLineBreakProvider();
    private final JTextArea source = new JTextArea();
    private final JTextArea structure = inspector();
    private final JTextArea effects = inspector();
    private final JTextArea breaks = inspector();
    private final JTextArea trace = inspector();
    private final Preview preview = new Preview();
    private final JLabel frameLabel = new JLabel("Frame 0");
    private final JSlider fontSize = new JSlider(8, 72, 28);
    private final JSlider paragraphWidth = new JSlider(160, 1000, 620);
    private final JCheckBox animate = new JCheckBox("Animate section-sign k", true);
    private final JCheckBox shadow = new JCheckBox("Shadow", true);
    private final JComboBox<String> backend = new JComboBox<>(
            new String[]{"AWT (Java2D)", "Cosmic (native)"});
    private final JLabel backendStatus = new JLabel("AWT ready");
    private final JLabel pluginStatus = new JLabel("UIE plugin: loading");
    private StructuredText parsed;
    private long frame;

    private TextRenderLab() {
        for (TextPipelinePlugin plugin : pluginLoader.builtIn()) plugins.put(plugin.id(), plugin);
        rebuildPipeline();
        source.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        source.setLineWrap(true);
        source.setWrapStyleWord(false);
        source.setText("\u00A7nMinecraft Formatting + UIE\u00A7r\n"
                + "\u00A70Black \u00A7cRed \u00A7lBold \u00A7oItalic \u00A7mStrike\u00A7r\n"
                + "Markdown: **bold** _italic_ ~~strike~~ [link](https://example.com)\n"
                + "LaTeX: $\\frac{a+b}{c}$[rows=1.4,supersample=4]\n"
                + "Typst: <typst:$ sum_(i=1)^n i = (n(n+1))/2 $>"
                + "[rows=1.4,supersample=4]\n"
                + "Deferred UIE image: <img:https://example.com/image.png>\n"
                + "\u00A7kObfuscated\u00A7r normal\n"
                + "\u00A7g\u00A7lBrilliant style\u00A7r and \u4e2d\u6587\uff0c\u6392\u7248\u6d4b\u8bd5\u3002\n"
                + "Cosmic shaping: العربية 😀❤️");
        source.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { refresh(); }
            @Override public void removeUpdate(DocumentEvent event) { refresh(); }
            @Override public void changedUpdate(DocumentEvent event) { refresh(); }
        });
        fontSize.addChangeListener(event -> refreshPreview());
        paragraphWidth.addChangeListener(event -> refreshPreview());
        shadow.addActionListener(event -> {
            preview.invalidateSnapshot();
            refreshPreview();
        });
        backend.addActionListener(event -> {
            preview.invalidateSnapshot();
            refreshPreview();
        });
        new Timer(80, event -> {
            if (hasLoadingInline(parsed)) refresh();
            if (animate.isSelected() && parsed != null && parsed.animated()) {
                frame++;
                frameLabel.setText("Frame " + frame);
                preview.repaint();
            }
        }).start();
        refresh();
    }

    public static void main(String[] args) {
        List<Path> pluginPaths = new ArrayList<>();
        boolean selfTest = false;
        for (int index = 0; index < args.length; index++) {
            if ("--self-test".equals(args[index])) selfTest = true;
            else if ("--plugin".equals(args[index]) && index + 1 < args.length) {
                pluginPaths.add(Path.of(args[++index]));
            }
        }
        if (selfTest) {
            System.setProperty("java.awt.headless", "true");
            runSelfTest(pluginPaths);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            TextRenderLab lab = new TextRenderLab();
            for (Path path : pluginPaths) lab.loadPlugin(path);
            lab.show();
        });
    }

    private static void runSelfTest(List<Path> pluginPaths) {
        TextSyntaxEngine syntax = StandardSyntaxEngines.minecraftWithBrilliantDefaults();
        Map<String, TextPipelinePlugin> plugins = new LinkedHashMap<>();
        try (TextPluginLoader loader = new TextPluginLoader()) {
            for (TextPipelinePlugin plugin : loader.builtIn()) plugins.put(plugin.id(), plugin);
            for (Path path : pluginPaths) {
                for (TextPipelinePlugin plugin : loader.load(path)) plugins.put(plugin.id(), plugin);
            }
            List<StructuredTextMiddleware> middleware = new ArrayList<>();
            List<LineBreakOpportunityProvider> lineBreakProviders = new ArrayList<>();
            for (TextPipelinePlugin plugin : plugins.values()) {
                middleware.addAll(plugin.structuredMiddlewares());
                lineBreakProviders.addAll(plugin.lineBreakProviders());
            }
            StructuredTextPipeline pipeline = new StructuredTextPipeline(syntax, middleware,
                    lineBreakProviders);
            runSelfTestPipeline(pipeline, pluginPaths);
        } catch (IOException error) {
            throw new IllegalStateException("Could not load test plugin", error);
        }
    }

    private static void runSelfTestPipeline(StructuredTextPipeline pipeline,
                                            List<Path> pluginPaths) {
        String source = "\u00A7g\u00A7lGold\u00A7r **UIE** "
                + "$\\frac{a}{b}$ <img:https://example.com/image.png> \u4e2d\u6587 العربية "
                + "<typst:$ integral_0^1 x^2 dif x $> "
                + "\u00A7nunderline \u00A7mstrike \uD83D\uDE00\u2764\uFE0F";
        StructuredText sample = awaitInlineRasters(pipeline, source);
        if (sample.inlineSpans().size() < 2 || sample.appliedMiddlewareIds().isEmpty()) {
            throw new IllegalStateException("UIE structured middleware did not run");
        }
        if (pipeline.breakOpportunities(sample).isEmpty()) {
            throw new IllegalStateException("UIE CJK line-break provider did not run");
        }
        StandaloneAwtRenderer.Settings settings = StandaloneAwtRenderer.Settings.defaults();
        StandaloneAwtRenderer.Result awt = new StandaloneAwtRenderer().render(sample, settings);
        requireVisible("AWT", awt.image, settings.background.getRGB());
        try (StandaloneCosmicRenderer cosmic = new StandaloneCosmicRenderer()) {
            StandaloneCosmicRenderer.Result result = cosmic.render(sample, settings);
            if (!result.available) throw new IllegalStateException(result.status);
            requireVisible("Cosmic", result.image, settings.background.getRGB());

            StructuredText animated = pipeline.parse("\u00A7kMinecraft");
            StandaloneAwtRenderer.Settings next = new StandaloneAwtRenderer.Settings(
                    settings.width, settings.height, settings.fontSize, settings.padding,
                    settings.lineGap, 23L, settings.fontFamily, settings.background,
                    settings.foreground, settings.shadow);
            long first = pixelHash(cosmic.render(animated, settings).image);
            long second = pixelHash(cosmic.render(animated, next).image);
            if (first == second) throw new IllegalStateException("Cosmic animation did not advance");
            System.out.println("Text Render Lab self-test passed: AWT, Cosmic, UIE middleware, "
                    + "inline LaTeX/image/Typst, formatting, CJK, shaping, emoji, Brilliant preview, "
                    + "shadow, and animation");
            System.out.println(result.status);
            if (!pluginPaths.isEmpty()) System.out.println("External plugins loaded: " + pluginPaths);
        }
    }

    private static StructuredText awaitInlineRasters(StructuredTextPipeline pipeline, String source) {
        long deadline = System.nanoTime() + 15_000_000_000L;
        StructuredText parsed;
        do {
            parsed = pipeline.parse(source);
            if (!hasLoadingInline(parsed)) return parsed;
            try {
                Thread.sleep(20L);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for inline rasters", error);
            }
        } while (System.nanoTime() < deadline);
        throw new IllegalStateException("Structured inline rasters did not resolve");
    }

    private static boolean hasLoadingInline(StructuredText text) {
        if (text == null) return false;
        for (InlineSpan span : text.inlineSpans()) {
            if ("loading".equals(span.content().attributes().get("status"))) return true;
        }
        return false;
    }

    private static void requireVisible(String backend, BufferedImage image, int background) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != background) return;
            }
        }
        throw new IllegalStateException(backend + " produced no visible pixels");
    }

    private static long pixelHash(BufferedImage image) {
        long hash = 1125899906842597L;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) hash = 31L * hash + image.getRGB(x, y);
        }
        return hash;
    }

    private void show() {
        JFrame window = new JFrame("NeoFontRender Text Rendering Laboratory");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setMinimumSize(new Dimension(1080, 720));
        window.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) {
                preview.close();
                pluginLoader.close();
            }
        });

        JSplitPane upper = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                titled("Source", new JScrollPane(source)), titled("Rendered output", preview));
        upper.setResizeWeight(0.35);
        upper.setDividerLocation(380);

        JTabbedPane inspectors = new JTabbedPane();
        inspectors.addTab("Structured spans", new JScrollPane(structure));
        inspectors.addTab("Effects", new JScrollPane(effects));
        inspectors.addTab("CJK breaks", new JScrollPane(breaks));
        inspectors.addTab("Pipeline trace", new JScrollPane(trace));

        JSplitPane main = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upper, inspectors);
        main.setResizeWeight(0.65);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        controls.add(new JLabel("Backend"));
        controls.add(Box.createHorizontalStrut(5));
        controls.add(backend);
        controls.add(Box.createHorizontalStrut(6));
        controls.add(backendStatus);
        controls.add(Box.createHorizontalStrut(10));
        controls.add(pluginStatus);
        controls.add(Box.createHorizontalStrut(16));
        controls.add(new JLabel("Font size"));
        controls.add(fontSize);
        controls.add(Box.createHorizontalStrut(16));
        controls.add(new JLabel("Paragraph width"));
        controls.add(paragraphWidth);
        controls.add(Box.createHorizontalStrut(16));
        controls.add(shadow);
        controls.add(Box.createHorizontalStrut(8));
        controls.add(animate);
        controls.add(Box.createHorizontalStrut(8));
        controls.add(frameLabel);
        JButton step = new JButton("Step");
        step.addActionListener(event -> { frame++; frameLabel.setText("Frame " + frame); preview.repaint(); });
        controls.add(Box.createHorizontalStrut(8));
        controls.add(step);
        JButton exportPng = new JButton("Export PNG");
        exportPng.addActionListener(event -> exportPng(window));
        controls.add(Box.createHorizontalStrut(8));
        controls.add(exportPng);
        JButton exportTrace = new JButton("Export trace");
        exportTrace.addActionListener(event -> exportTrace(window));
        controls.add(Box.createHorizontalStrut(4));
        controls.add(exportTrace);
        JButton loadPlugin = new JButton("Load UIE/plugin JAR");
        loadPlugin.addActionListener(event -> loadPlugin(window));
        controls.add(Box.createHorizontalStrut(4));
        controls.add(loadPlugin);

        window.add(controls, BorderLayout.NORTH);
        window.add(main, BorderLayout.CENTER);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }

    private void loadPlugin(JFrame owner) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        try {
            loadPlugin(chooser.getSelectedFile().toPath());
        } catch (Exception error) {
            pluginStatus.setText("Plugin load failed");
            pluginStatus.setToolTipText(error.toString());
        }
    }

    private void loadPlugin(Path path) {
        try {
            List<TextPipelinePlugin> loaded = pluginLoader.load(path);
            for (TextPipelinePlugin plugin : loaded) plugins.put(plugin.id(), plugin);
            rebuildPipeline();
            refresh();
        } catch (Exception error) {
            pluginStatus.setText("Plugin load failed");
            pluginStatus.setToolTipText(error.toString());
        }
    }

    private void rebuildPipeline() {
        List<StructuredTextMiddleware> middleware = new ArrayList<>();
        List<LineBreakOpportunityProvider> lineBreakProviders = new ArrayList<>();
        for (TextPipelinePlugin plugin : plugins.values()) {
            middleware.addAll(plugin.structuredMiddlewares());
            lineBreakProviders.addAll(plugin.lineBreakProviders());
        }
        pipeline = new StructuredTextPipeline(syntax, middleware, lineBreakProviders);
        pluginStatus.setText("Plugins " + plugins.size() + " / components "
                + (middleware.size() + lineBreakProviders.size()));
        pluginStatus.setToolTipText(String.join(", ", plugins.keySet()));
    }

    private void exportPng(JFrame owner) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("text-render-lab.png"));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        try {
            Snapshot result = preview.snapshot();
            if (result != null) ImageIO.write(result.image, "png", chooser.getSelectedFile());
        } catch (IOException error) {
            trace.setText("PNG export failed: " + error.getMessage());
        }
    }

    private void exportTrace(JFrame owner) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("text-render-trace.json"));
        if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) return;
        try {
            Snapshot result = preview.snapshot();
            List<String> lines = result == null ? java.util.Collections.emptyList() : result.trace;
            StringBuilder json = new StringBuilder("{\n  \"trace\": [\n");
            for (int index = 0; index < lines.size(); index++) {
                if (index > 0) json.append(",\n");
                json.append("    \"").append(jsonEscape(lines.get(index))).append('"');
            }
            json.append("\n  ]\n}\n");
            Files.write(chooser.getSelectedFile().toPath(),
                    json.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) {
            trace.setText("Trace export failed: " + error.getMessage());
        }
    }

    private static String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private void refresh() {
        parsed = pipeline.parse(source.getText());
        StringBuilder styled = new StringBuilder();
        styled.append("Plain text:\n").append(parsed.plainText()).append("\n\nSpans:\n");
        for (StyledSpan span : parsed.styles()) {
            styled.append(span).append("  text=\"")
                    .append(parsed.plainText(), span.start(), span.end()).append("\"\n");
        }
        styled.append("\nSource boundaries:\n");
        for (int index = 0; index <= parsed.plainText().length(); index++) {
            styled.append(index).append(" -> [")
                    .append(parsed.sourceMap().sourceStart(index)).append(',')
                    .append(parsed.sourceMap().sourceEnd(index)).append("]\n");
        }
        structure.setText(styled.toString());

        StringBuilder effectText = new StringBuilder();
        for (StructuredEffectSpan effect : parsed.effects()) {
            effectText.append(effect).append("\n  parameters=")
                    .append(effect.parameters()).append('\n');
        }
        if (!parsed.inlineSpans().isEmpty()) {
            effectText.append("\nInline content:\n");
            for (InlineSpan inline : parsed.inlineSpans()) effectText.append(inline).append('\n');
        }
        if (parsed.effects().isEmpty()) effectText.append("No active effects\n");
        if (!parsed.unresolvedSyntax().isEmpty()) {
            effectText.append("\nUnresolved syntax:\n");
            parsed.unresolvedSyntax().forEach(value -> effectText.append(value).append('\n'));
        }
        effects.setText(effectText.toString());

        List<Integer> opportunities = pipeline.breakOpportunities(parsed);
        if (opportunities.isEmpty()) opportunities = cjk.opportunities(parsed);
        StringBuilder breakText = new StringBuilder("Plain boundaries: ");
        breakText.append(opportunities).append("\nSource boundaries: ");
        for (int value : opportunities) breakText.append(parsed.sourceMap().sourceEnd(value)).append(' ');
        breaks.setText(breakText.toString());

        trace.setText("input:string\n"
                + "  -> syntax:" + String.join(",", parsed.appliedSyntaxProviderIds()) + "\n"
                + "  -> middleware:" + String.join(",", parsed.appliedMiddlewareIds()) + "\n"
                + "  -> structured-text: " + parsed.styles().size() + " style spans, "
                + parsed.effects().size() + " effect spans, " + parsed.inlineSpans().size()
                + " inline spans\n"
                + "  -> line-break:" + String.join(",", pipeline.lineBreakProviderHits(parsed)) + "\n"
                + "  -> cjk-break-provider: " + opportunities.size() + " opportunities\n"
                + (parsed.animated() ? "  -> animated-glyph-resolver\n" : "")
                + "  -> raster:" + selectedBackendId() + "\n"
                + "  -> framebuffer:swing-preview\n");
        refreshPreview();
    }

    private String selectedBackendId() {
        return backend.getSelectedIndex() == 1 ? "cosmic-native" : "awt-java2d";
    }

    private void refreshPreview() {
        preview.setPreferredSize(new Dimension(paragraphWidth.getValue() + 80, 430));
        preview.revalidate();
        preview.repaint();
    }

    private static JPanel titled(String title, java.awt.Component component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private static JTextArea inspector() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        return area;
    }

    private final class Preview extends JPanel {
        private final StandaloneAwtRenderer awt = new StandaloneAwtRenderer();
        private final StandaloneCosmicRenderer cosmic = new StandaloneCosmicRenderer();
        private Snapshot lastResult;
        private int lastBackend = -1;

        Preview() {
            setOpaque(true);
            setBackground(new Color(28, 30, 34));
            setForeground(new Color(235, 238, 242));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (parsed == null) return;
            lastResult = render(Math.max(1, Math.min(getWidth(), paragraphWidth.getValue() + 52)),
                    Math.max(1, getHeight()));
            graphics.drawImage(lastResult.image, 0, 0, null);
            publishStatus(lastResult);
        }

        private Snapshot snapshot() {
            if (parsed == null) return null;
            if (lastResult == null || lastBackend != backend.getSelectedIndex())
                lastResult = render(Math.max(1, paragraphWidth.getValue() + 52), 430);
            return lastResult;
        }

        private Snapshot render(int width, int height) {
            StandaloneAwtRenderer.Settings settings = new StandaloneAwtRenderer.Settings(
                    width, height, fontSize.getValue(), 26, 12, frame,
                    Font.SANS_SERIF, getBackground(), getForeground(), shadow.isSelected());
            lastBackend = backend.getSelectedIndex();
            if (lastBackend == 1) {
                StandaloneCosmicRenderer.Result result = cosmic.render(parsed, settings);
                return new Snapshot(result.image, result.trace, result.available, result.status);
            }
            StandaloneAwtRenderer.Result result = awt.render(parsed, settings);
            return new Snapshot(result.image, result.trace, true, "AWT Java2D ready");
        }

        private void publishStatus(Snapshot snapshot) {
            String label = snapshot.available
                    ? (backend.getSelectedIndex() == 1 ? "Cosmic ready" : "AWT ready")
                    : "Cosmic unavailable";
            if (!label.equals(backendStatus.getText())) backendStatus.setText(label);
            backendStatus.setToolTipText(snapshot.status);
            String renderedTrace = String.join("\n", snapshot.trace) + '\n';
            if (!renderedTrace.equals(trace.getText())) trace.setText(renderedTrace);
        }

        private void invalidateSnapshot() { lastResult = null; }

        private void close() { cosmic.close(); }
    }

    private static final class Snapshot {
        final BufferedImage image;
        final List<String> trace;
        final boolean available;
        final String status;

        Snapshot(BufferedImage image, List<String> trace, boolean available, String status) {
            this.image = image;
            this.trace = trace;
            this.available = available;
            this.status = status;
        }
    }
}
