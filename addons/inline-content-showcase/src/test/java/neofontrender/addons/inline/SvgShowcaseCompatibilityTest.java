package neofontrender.addons.inline;

import neofontrender.addons.inlinecontent.CrownEtherSample;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SvgShowcaseCompatibilityTest {
    private static final String ROOT = "/assets/neofontrender_inline_content_showcase/structures/";

    @Test
    void safeChemistryStructuresAreWellFormedAndRenderable() throws Exception {
        verifyStructure("aminobenzo_18_crown_6.svg", false, 7);
        verifyStructure("benzene.svg", false, 0);
        verifyStructure("ethyl_acetate.svg", false, 2);
    }

    @Test
    void crownEtherTooltipRequestsA48PixelAtomicGlyph() {
        SvgTokenParser.Match match = SvgTokenParser.match(CrownEtherSample.STRUCTURE_TOKEN, 0);
        assertNotNull(match);
        assertEquals(48, match.height);
        assertEquals(CrownEtherSample.STRUCTURE_TOKEN.length(), match.end);
    }

    @Test
    void aspirinExercisesFullSvgCompatibilityMode() throws Exception {
        Document document = parse("aspirin_full.svg");
        assertEquals(1, document.getElementsByTagName("style").getLength());
        assertTrue(document.getElementsByTagName("text").getLength() >= 3);
        try (InputStream input = resource("aspirin_full.svg")) {
            assertThrows(IOException.class, () -> SvgRasterizer.rasterize(input, false, 1024));
        }
        verifyStructure("aspirin_full.svg", true, 3);
    }

    private static void verifyStructure(String file, boolean full, int minimumAtomLabels) throws Exception {
        Document document = parse(file);
        assertEquals("svg", document.getDocumentElement().getLocalName());
        assertTrue(document.getElementsByTagName("path").getLength() > 0);
        assertTrue(document.getElementsByTagName("line").getLength() >= 2);
        NodeList doubleBonds = document.getElementsByTagName("g");
        boolean markedDoubleBond = false;
        for (int index = 0; index < doubleBonds.getLength(); index++) {
            Node order = doubleBonds.item(index).getAttributes().getNamedItem("data-bond-order");
            if (order != null && "2".equals(order.getNodeValue())) {
                markedDoubleBond = true;
                break;
            }
        }
        assertTrue(markedDoubleBond, file + " must exercise double-bond rendering");
        assertTrue(document.getElementsByTagName("text").getLength() >= minimumAtomLabels);
        try (InputStream input = resource(file)) {
            BufferedImage image = SvgRasterizer.rasterize(input, full, 1024);
            assertTrue(hasVisiblePixels(image), file + " produced an empty raster");
        }
    }

    private static Document parse(String file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (InputStream input = resource(file)) {
            return factory.newDocumentBuilder().parse(input);
        }
    }

    private static InputStream resource(String file) {
        InputStream input = SvgShowcaseCompatibilityTest.class.getResourceAsStream(ROOT + file);
        assertNotNull(input, "Missing SVG resource " + file);
        return input;
    }

    private static boolean hasVisiblePixels(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) != 0) return true;
            }
        }
        return false;
    }
}
