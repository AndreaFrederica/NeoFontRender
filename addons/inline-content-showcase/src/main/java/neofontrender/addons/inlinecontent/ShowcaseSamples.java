package neofontrender.addons.inlinecontent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Canonical compatibility cases shared by the interactive screen and automated tests. */
public final class ShowcaseSamples {
    public static final List<Sample> MATHEMATICS = Collections.unmodifiableList(Arrays.asList(
            new Sample("Euler identity", "Euler: $e^{i\\pi}+1=0$", "Euler: ＄e^{i\\pi}+1=0＄"),
            new Sample("Fraction and radical", "$\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}$[scale=1.15]",
                    "＄\\frac{-b\\pm\\sqrt{b^2-4ac}}{2a}＄ [scale=1.15]"),
            new Sample("Display integral", "$$\\int_{-\\infty}^{\\infty}e^{-x^2}\\,dx=\\sqrt{\\pi}$$[scale=1.1]",
                    "＄＄\\int_{-∞}^{∞}e^{-x^2} dx=\\sqrt{π}＄＄ [scale=1.1]"),
            new Sample("Matrices and mixed text", "中英文 $A=\\begin{pmatrix}a&b\\\\c&d\\end{pmatrix}$ mixed",
                    "中英文 ＄A=[a b; c d]＄ mixed"),
            new Sample("Formatting boundary", "\u00a7bBlue $\\sum_{k=1}^{n}k=\\frac{n(n+1)}{2}$ \u00a7aresumes green",
                    "Blue ＄sum(k)=n(n+1)/2＄ resumes green"),
            new Sample("Escaped delimiter", "Price: \\$12; formula: $x^2+y^2=r^2$",
                    "Price: \\$12; formula: ＄x^2+y^2=r^2＄")
    ));

    public static final List<Sample> CHEMISTRY = Collections.unmodifiableList(Arrays.asList(
            svg("4'-Aminobenzo-18-crown-6", "aminobenzo_18_crown_6.svg"),
            svg("Benzene / aromatic ring", "benzene.svg"),
            svg("Ethyl acetate / carbonyl and heteroatoms", "ethyl_acetate.svg"),
            svg("Aspirin / full SVG style compatibility", "aspirin_full.svg")
    ));

    public static final List<Sample> MARKDOWN = Collections.unmodifiableList(Arrays.asList(
            new Sample("Emphasis", "**Bold**  *italic*  ~~strikethrough~~",
                    "**Bold**  *italic*  ~~strikethrough~~"),
            new Sample("Code and link", "Use `StructuredTextApi.register` or [open docs](https://example.invalid)",
                    "`code` and [label](url)"),
            new Sample("Markdown with LaTeX", "**Euler identity:** $e^{i\\pi}+1=0$",
                    "**Euler identity:** followed by $...$"),
            new Sample("Escaped marker", "\\*literal asterisks\\* and _rendered italic_",
                    "\\*literal\\* and _italic_"),
            new Sample("CJK mixed text", "**化学式:** $\\mathrm{C}_{16}\\mathrm{H}_{25}\\mathrm{N}\\mathrm{O}_{6}$",
                    "Markdown label + LaTeX molecular formula")
    ));

    private ShowcaseSamples() {}

    private static Sample svg(String name, String file) {
        String token = "<svg:resource:neofontrender_inline_content_showcase:structures/" + file + ">";
        return new Sample(name, name + "  " + token,
                "< svg:resource:neofontrender_inline_content_showcase:structures/" + file + " >");
    }

    public static final class Sample {
        private final String name;
        private final String rendered;
        private final String visibleSource;

        public Sample(String name, String rendered, String visibleSource) {
            this.name = name;
            this.rendered = rendered;
            this.visibleSource = visibleSource;
        }

        public String name() { return name; }
        public String rendered() { return rendered; }
        public String visibleSource() { return visibleSource; }
    }
}
