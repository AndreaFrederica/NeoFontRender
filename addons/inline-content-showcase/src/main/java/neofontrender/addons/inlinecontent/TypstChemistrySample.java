package neofontrender.addons.inlinecontent;

/** A meta-cresol structure and matching formula rendered by the optional Typst provider. */
public final class TypstChemistrySample {
    public static final String STRUCTURE_TOKEN =
            "<typst:#set page(width:auto,height:auto);"
                    + "#import \"@preview/cetz:0.5.2\";"
                    + "#cetz.canvas({import cetz.draw:*;"
                    + "let ring=((0,1.25),(1.08,0.625),(1.08,-0.625),(0,-1.25),(-1.08,-0.625),(-1.08,0.625));"
                    + "line(..ring,close:true,stroke:(paint:white,thickness:1.2pt));"
                    + "circle((0,0),radius:0.72,stroke:(paint:white,thickness:0.9pt));"
                    + "line((0,1.25),(0,2.0),stroke:(paint:white,thickness:1.2pt));content((0,2.2),$\"OH\"$);"
                    + "line((1.08,-0.625),(1.85,-1.05),stroke:(paint:white,thickness:1.2pt));content((1.95,-1.1),$\"CH\"_3$,anchor:\"west\")})"
                    + "</typst>[rows=6,max-width=12em,align=top]";

    public static final String FORMULA_TOKEN =
            // chemformula subscripts digits automatically; '_' changes how the rest is parsed.
            "<typst:#import \"@preview/chemformula:0.1.3\":ch;#ch(\"CH3C6H4OH\")</typst>"
                    + "[rows=1.35,align=center]";

    private TypstChemistrySample() {}
}
