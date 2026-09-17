package neofontrender.addons.tooltips;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TooltipLayoutEngineTest {
    @Test
    void rowCentersDifferentHeightChildren() {
        TooltipLayoutEngine.Leaf icon = new TooltipLayoutEngine.Leaf(18, 18);
        TooltipLayoutEngine.Leaf text = new TooltipLayoutEngine.Leaf(80, 10);
        TooltipLayoutEngine.Flow row = new TooltipLayoutEngine.Flow(
                TooltipLayoutEngine.Direction.ROW, TooltipLayoutEngine.Alignment.CENTER, 4,
                TooltipLayoutEngine.Insets.none(), Arrays.asList(icon, text));
        TooltipLayoutEngine.Measurement measured = row.measure(new TooltipLayoutEngine.Constraints(200, 40));
        row.place(new TooltipLayoutEngine.Rect(10, 20, measured.width, measured.height));
        assertEquals(18, measured.height);
        assertEquals(24, text.bounds().y);
        assertEquals(32, text.bounds().x);
    }

    @Test
    void columnHonorsPaddingAndGap() {
        TooltipLayoutEngine.Leaf title = new TooltipLayoutEngine.Leaf(60, 10);
        TooltipLayoutEngine.Leaf rarity = new TooltipLayoutEngine.Leaf(40, 9);
        TooltipLayoutEngine.Flow column = new TooltipLayoutEngine.Flow(
                TooltipLayoutEngine.Direction.COLUMN, TooltipLayoutEngine.Alignment.START, 3,
                new TooltipLayoutEngine.Insets(2, 4, 2, 5), Arrays.asList(title, rarity));
        TooltipLayoutEngine.Measurement measured = column.measure(TooltipLayoutEngine.Constraints.unbounded());
        column.place(new TooltipLayoutEngine.Rect(0, 0, measured.width, measured.height));
        assertEquals(31, measured.height);
        assertEquals(4, title.bounds().y);
        assertEquals(17, rarity.bounds().y);
    }
}
