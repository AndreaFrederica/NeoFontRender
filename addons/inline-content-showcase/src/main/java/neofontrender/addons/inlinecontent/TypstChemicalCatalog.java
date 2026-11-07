package neofontrender.addons.inlinecontent;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Data shared by the items, gallery, icon generator, and native rendering smoke test. */
public final class TypstChemicalCatalog {
    private static final String ROOT = "/assets/neofontrender_inline_content_showcase/";
    public static final List<Chemical> ALL = readResource("chemistry.tsv").lines()
            .filter(line -> !line.isBlank() && !line.startsWith("#"))
            .map(line -> {
                String[] fields = line.split("\\|", -1);
                return new Chemical(fields[0], fields[1], fields[4], fields[5], fields.length > 6 ? fields[6] : "");
            }).toList();
    // The resource explicitly terminates declarations so the exported game token is single-line.
    public static final String PERIODIC_TABLE_SOURCE = readResource("typst/periodic-table.typ")
            .replace('\r', ' ').replace('\n', ' ').trim();
    public static final String PERIODIC_TABLE_TOKEN = token(PERIODIC_TABLE_SOURCE,
            "rows=15,max-width=32em,flow=block,align=top");

    private TypstChemicalCatalog() {}

    static String readResource(String path) {
        try (InputStream input = TypstChemicalCatalog.class.getResourceAsStream(ROOT + path)) {
            if (input == null) throw new IOException("Missing showcase resource: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    public static String token(String source, String layout) {
        return "<typst:" + source + "</typst>[" + layout + "]";
    }

    public static String formulaSource(String notation) {
        return "#import \"@preview/chemformula:0.1.3\":ch;#ch(\"" + notation + "\")";
    }

    public record Chemical(String id, String name, String formula, String notation, String smiles) {
        public String translationKey() {
            return "item." + InlineContentShowcaseMod.MOD_ID + "." + id + ".name";
        }

        public String formulaToken() {
            return token(formulaSource(formula), "rows=1.6,max-width=24em,align=center");
        }

        public String notationToken() {
            return token(formulaSource(notation), "rows=2,max-width=28em,align=top");
        }

        /** Complex molecules use typed-smiles' WASM layout and CeTZ drawing through Typst. */
        public String structureSource() {
            if (!smiles.isEmpty()) {
                return "#import \"@preview/typed-smiles:0.10.0\":smiles;#smiles(\""
                        + smiles + "\",fg:white,theme:\"dark\")";
            }
            String substituent = switch (id) {
                case "benzene" -> "";
                case "phenol" -> "OH";
                case "aniline" -> "NH2";
                default -> null;
            };
            if (substituent == null) return null;
            return "#import \"@preview/cetz:0.5.2\";"
                    + "#import \"@preview/chemformula:0.1.3\":ch;"
                    + "#cetz.canvas({import cetz.draw:*;"
                    + "set-style(stroke:(paint:white,thickness:0.9pt));"
                    + "line((0,1),(0.866,0.5),(0.866,-0.5),(0,-1),(-0.866,-0.5),(-0.866,0.5),close:true);"
                    + "circle((0,0),radius:0.55);"
                    + (substituent.isEmpty() ? "" : "line((0,1),(0,1.6));content((0,1.9),ch(\""
                    + substituent + "\"));") + "})";
        }

        public String structureToken() {
            String source = structureSource();
            return source == null ? null : token(source, "rows=6,max-width=18em,align=top");
        }
    }
}
