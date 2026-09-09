package neofontrender.addons.inlinecontent;

import java.util.List;

/** All 118 elements in atomic-number order; each entry becomes an independent item. */
public final class TypstElementCatalog {
    public static final String[] COLORS = {"ef9b8b", "efcc85", "8ccac8", "afbee8", "c6b2de",
            "9fd49b", "e2d493", "84cce9", "e8acd0", "d0b39a"};
    private static final String[] CATEGORIES = {"Alkali metal", "Alkaline earth", "Transition metal",
            "Post-transition metal", "Metalloid", "Nonmetal", "Halogen", "Noble gas", "Lanthanide", "Actinide"};
    public static final List<Element> ALL = TypstChemicalCatalog.readResource("elements.tsv").lines()
            .filter(line -> !line.isBlank() && !line.startsWith("#"))
            .map(line -> {
                String[] f = line.split("\\|", -1);
                return new Element(Integer.parseInt(f[0]), f[1], "element_" + f[2], f[3], Integer.parseInt(f[5]));
            }).toList();

    private TypstElementCatalog() {}

    public record Element(int atomicNumber, String symbol, String id, String name, int category) {
        public String translationKey() {
            return "item." + InlineContentShowcaseMod.MOD_ID + "." + id + ".name";
        }

        public String source() {
            return "#block(width:108pt,inset:8pt,radius:4pt,stroke:rgb(\"" + COLORS[category] + "\"))["
                    + "#set text(fill:white);#stack(dir:ttb,spacing:5pt,"
                    + "text(size:9pt,\"Atomic number  " + atomicNumber + "\"),"
                    + "text(size:32pt,weight:\"bold\",fill:rgb(\"" + COLORS[category] + "\"),\"" + symbol + "\"),"
                    + "text(size:10pt,\"" + name + "\"),"
                    + "text(size:8pt,\"" + CATEGORIES[category] + "\"),"
                    + "$Z = " + atomicNumber + "$)]";
        }

        public String token() {
            return TypstChemicalCatalog.token(source(), "rows=8,max-width=18em,align=top");
        }
    }
}
