package neofontrender.addons.inlinecontent;

import neofontrender.text.InlineContent;
import neofontrender.text.InlineRaster;
import neofontrender.text.pipeline.StructuredTextPipeline;
import neofontrender.text.syntax.TextSyntaxEngine;
import neofontrender.typst.pipeline.TypstPipelinePlugin;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Opt-in JNI + package integration test; artifacts make typography reviewable. */
public final class TypstShowcaseSmoke {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        Path library = Path.of(args[1]);
        Files.createDirectories(output);
        Map<String, String> samples = new LinkedHashMap<>();
        for (var chemical : TypstChemicalCatalog.ALL) {
            samples.put(chemical.id() + "-formula", chemical.formulaToken());
            samples.put(chemical.id() + "-notation", chemical.notationToken());
            if (chemical.structureToken() != null) samples.put(chemical.id() + "-structure", chemical.structureToken());
        }
        samples.put("meta-cresol-structure", TypstChemistrySample.STRUCTURE_TOKEN);
        samples.put("meta-cresol-formula", TypstChemistrySample.FORMULA_TOKEN);
        samples.put("periodic-table", TypstChemicalCatalog.PERIODIC_TABLE_TOKEN);
        BufferedImage sheet = new BufferedImage(1000, ((samples.size() + 1) / 2) * 128, BufferedImage.TYPE_INT_ARGB);
        var graphics = sheet.createGraphics();
        graphics.setColor(new Color(0x182330));
        graphics.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        try (TypstPipelinePlugin plugin = new TypstPipelinePlugin(new TypstPipelinePlugin.Config(
                () -> library, () -> true, () -> 2, () -> 4096, () -> {}))) {
            var pipeline = new StructuredTextPipeline(TextSyntaxEngine.builder().build(), plugin.structuredMiddlewares());
            int index = 0;
            for (var sample : samples.entrySet()) {
                InlineContent content = render(pipeline, sample.getValue());
                InlineRaster raster = content.raster();
                BufferedImage image = image(raster);
                ImageIO.write(image, "png", output.resolve(sample.getKey() + ".png").toFile());
                int x = index % 2 * 500 + 12;
                int y = index / 2 * 128;
                graphics.setColor(new Color(0xA6C3DD));
                graphics.drawString(sample.getKey(), x, y + 18);
                float scale = Math.min(1, Math.min(476f / image.getWidth(), 96f / image.getHeight()));
                graphics.drawImage(image, x, y + 25, Math.round(image.getWidth() * scale),
                        Math.round(image.getHeight() * scale), null);
                System.out.println(sample.getKey() + ": " + raster.width() + "x" + raster.height());
                index++;
            }
        } finally {
            graphics.dispose();
        }
        ImageIO.write(sheet, "png", output.resolve("chemistry-contact-sheet.png").toFile());
        if (TypstElementCatalog.ALL.size() != 118
                || TypstElementCatalog.ALL.stream().map(TypstElementCatalog.Element::symbol).distinct().count() != 118
                || TypstElementCatalog.ALL.stream().map(TypstElementCatalog.Element::id).distinct().count() != 118) {
            throw new AssertionError("Expected 118 distinct element items");
        }
        var elementSheet = new BufferedImage(1200, 15 * 176, BufferedImage.TYPE_INT_ARGB);
        var elementGraphics = elementSheet.createGraphics();
        elementGraphics.setColor(new Color(0x182330)); elementGraphics.fillRect(0, 0, 1200, elementSheet.getHeight());
        elementGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        try (TypstPipelinePlugin plugin = new TypstPipelinePlugin(new TypstPipelinePlugin.Config(
                () -> library, () -> true, () -> 2, () -> 4096, () -> {}))) {
            var pipeline = new StructuredTextPipeline(TextSyntaxEngine.builder().build(), plugin.structuredMiddlewares());
            for (int i = 0; i < TypstElementCatalog.ALL.size(); i++) {
                var element = TypstElementCatalog.ALL.get(i);
                if (element.atomicNumber() != i + 1) throw new AssertionError("Elements must be in atomic-number order");
                BufferedImage rendered = image(render(pipeline, element.token()).raster());
                ImageIO.write(rendered, "png", output.resolve(element.id() + ".png").toFile());
                float factor = Math.min(140f / rendered.getWidth(), 166f / rendered.getHeight());
                elementGraphics.drawImage(rendered, i % 8 * 150 + 5, i / 8 * 176 + 5,
                        Math.round(rendered.getWidth() * factor), Math.round(rendered.getHeight() * factor), null);
            }
        } finally {
            elementGraphics.dispose();
        }
        ImageIO.write(elementSheet, "png", output.resolve("element-contact-sheet.png").toFile());
        // Exercise GUI scaling on the tallest / widest examples as well.
        try (TypstPipelinePlugin plugin = new TypstPipelinePlugin(new TypstPipelinePlugin.Config(
                () -> library, () -> true, () -> 2, () -> 4096, () -> {}, () -> 2))) {
            var pipeline = new StructuredTextPipeline(TextSyntaxEngine.builder().build(), plugin.structuredMiddlewares());
            for (String id : new String[]{"periodic-table", "potassium_permanganate-notation", "carbon_14-notation"}) {
                InlineContent content = render(pipeline, samples.get(id));
                if (!"4.0".equals(content.attributes().get("supersample"))) throw new AssertionError("GUI scale ignored");
                ImageIO.write(image(content.raster()), "png", output.resolve(id + "-gui2.png").toFile());
            }
        }
        java.util.ArrayList<String> ids = new java.util.ArrayList<>(java.util.List.of(
                "aminobenzo_18_crown_6_solution", "typst_chemistry_demonstrator", "periodic_table"));
        TypstChemicalCatalog.ALL.forEach(chemical -> ids.add(chemical.id()));
        TypstElementCatalog.ALL.forEach(element -> ids.add(element.id()));
        var iconSheet = new BufferedImage(1000, ((ids.size() + 9) / 10) * 100, BufferedImage.TYPE_INT_ARGB);
        var icons = iconSheet.createGraphics();
        icons.setColor(new Color(0x182330)); icons.fillRect(0, 0, iconSheet.getWidth(), iconSheet.getHeight());
        icons.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 9));
        for (String locale : new String[]{"en_us", "zh_cn"}) {
            try (var input = TypstShowcaseSmoke.class.getResourceAsStream(
                    "/assets/neofontrender_inline_content_showcase/lang/" + locale + ".lang")) {
                var translations = new java.util.Properties();
                translations.load(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8));
                for (var element : TypstElementCatalog.ALL) {
                    if (!translations.containsKey(element.translationKey())) {
                        throw new AssertionError("Missing " + locale + " name for " + element.id());
                    }
                }
            }
        }
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            String asset = "/assets/neofontrender_inline_content_showcase/";
            try (var input = TypstShowcaseSmoke.class.getResourceAsStream(asset + "textures/items/" + id + ".png")) {
                if (input == null) throw new AssertionError("Missing texture " + id);
                BufferedImage icon = ImageIO.read(input);
                if (icon.getWidth() != 32 || icon.getHeight() != 32) throw new AssertionError("Nonstandard icon " + id);
                icons.drawImage(icon, i % 10 * 100 + 18, i / 10 * 100 + 4, 64, 64, null);
            }
            if (TypstShowcaseSmoke.class.getResource(asset + "models/item/" + id + ".json") == null) {
                throw new AssertionError("Missing item model " + id);
            }
            icons.setColor(Color.WHITE);
            String label = id.length() > 16 ? id.substring(0, 15) + "…" : id;
            icons.drawString(label, i % 10 * 100 + 1, i / 10 * 100 + 80);
        }
        icons.dispose();
        ImageIO.write(iconSheet, "png", output.resolve("item-icons.png").toFile());
        System.out.println("Rendered " + samples.size() + " chemistry/table tokens + 118 element tokens + 3 GUI-scale cases; "
                + ids.size() + " item icons.");
    }

    private static InlineContent render(StructuredTextPipeline pipeline, String token) throws Exception {
        long deadline = System.nanoTime() + 90_000_000_000L;
        while (System.nanoTime() < deadline) {
            var parsed = pipeline.parse(token);
            if (parsed.inlineSpans().size() != 1) throw new AssertionError("Token was not parsed: " + token);
            var content = parsed.inlineSpans().getFirst().content();
            if ("failed".equals(content.attributes().get("status"))) throw new AssertionError(content.attributes());
            if (content.resolved()) {
                InlineRaster raster = content.raster();
                int visible = 0;
                for (int pixel : raster.argb()) if ((pixel >>> 24) != 0) visible++;
                if (visible < 16) throw new AssertionError("Empty raster");
                return content;
            }
            Thread.sleep(15);
        }
        throw new AssertionError("Timed out rendering: " + token);
    }

    private static BufferedImage image(InlineRaster raster) {
        BufferedImage image = new BufferedImage(raster.width(), raster.height(), BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, raster.width(), raster.height(), raster.argb(), 0, raster.width());
        return image;
    }
}
