package neofontrender.addons.inlinecontent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Test data for the compact, expanded and detailed Tooltip presentations. */
public final class CrownEtherSample {
    public static final String NAME = "4'-Aminobenzo-18-crown-6 solution";
    public static final String FORMULA_TOKEN = "$\\mathrm{C_{16}H_{25}NO_{6}}$";
    public static final String MOLAR_MASS_TOKEN = "$327.37\\,\\mathrm{g\\,mol^{-1}}$";
    public static final String SMILES = "Nc1ccc2OCCOCCOCCOCCOCCOc2c1";
    public static final String STRUCTURE_TOKEN =
            "<svg:resource:neofontrender_inline_content_showcase:structures/aminobenzo_18_crown_6.svg>"
                    + "[height=48]";

    private CrownEtherSample() {}

    public static List<String> lines(Detail detail) {
        switch (detail) {
            case COMPACT:
                return immutable("Formula  " + FORMULA_TOKEN, "666 mB", "Hold Shift for structure");
            case EXPANDED:
                return immutable("Formula  " + FORMULA_TOKEN, "Molar mass  " + MOLAR_MASS_TOKEN,
                        STRUCTURE_TOKEN, "666 mB", "Hold Ctrl for full details");
            case DETAILED:
                return immutable(NAME, "Formula  " + FORMULA_TOKEN,
                        "Molar mass  " + MOLAR_MASS_TOKEN, "SMILES", SMILES,
                        STRUCTURE_TOKEN, "666 mB", "ContentTweaker");
            default:
                throw new IllegalArgumentException("Unknown Tooltip detail " + detail);
        }
    }

    private static List<String> immutable(String... lines) {
        return Collections.unmodifiableList(Arrays.asList(lines));
    }

    public enum Detail { COMPACT, EXPANDED, DETAILED }
}
