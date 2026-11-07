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

    @Test
    void gridSharesColumnWidthsAcrossRows() {
        TooltipLayoutEngine.Leaf first = new TooltipLayoutEngine.Leaf(20, 8);
        TooltipLayoutEngine.Leaf second = new TooltipLayoutEngine.Leaf(30, 10);
        TooltipLayoutEngine.Leaf third = new TooltipLayoutEngine.Leaf(25, 6);
        TooltipLayoutEngine.Grid grid = new TooltipLayoutEngine.Grid(2, 3,
                Arrays.asList(first, second, third));
        TooltipLayoutEngine.Measurement measured = grid.measure(TooltipLayoutEngine.Constraints.unbounded());
        grid.place(new TooltipLayoutEngine.Rect(5, 7, measured.width, measured.height));
        assertEquals(58, measured.width);
        assertEquals(19, measured.height);
        assertEquals(5, first.bounds().x);
        assertEquals(33, second.bounds().x);
        assertEquals(5, third.bounds().x);
        assertEquals(20, third.bounds().y);
    }

    @Test
    void leafKeepsIntrinsicSizeWhenConstraintsAreSmaller() {
        TooltipLayoutEngine.Leaf leaf = new TooltipLayoutEngine.Leaf(24, 12);
        TooltipLayoutEngine.Measurement measured = leaf.measure(new TooltipLayoutEngine.Constraints(4, 3));
        assertEquals(24, measured.width);
        assertEquals(12, measured.height);
    }

    @Test
    void wrapKeepsOversizedItemOnItsOwnRow() {
        TooltipLayoutEngine.Leaf wide = new TooltipLayoutEngine.Leaf(40, 7);
        TooltipLayoutEngine.Leaf small = new TooltipLayoutEngine.Leaf(10, 5);
        TooltipLayoutEngine.Wrap wrap = new TooltipLayoutEngine.Wrap(24, 2, 3,
                TooltipLayoutEngine.Insets.none(), Arrays.asList(wide, small));
        TooltipLayoutEngine.Measurement measured = wrap.measure(new TooltipLayoutEngine.Constraints(24, 100));
        wrap.place(new TooltipLayoutEngine.Rect(0, 0, measured.width, measured.height));
        assertEquals(40, measured.width);
        assertEquals(15, measured.height);
        assertEquals(0, wide.bounds().x);
        assertEquals(10, small.bounds().y);
    }

    @Test
    void gridDropsColumnsWhenNaturalTracksDoNotFit() {
        TooltipLayoutEngine.Leaf first = new TooltipLayoutEngine.Leaf(30, 4);
        TooltipLayoutEngine.Leaf second = new TooltipLayoutEngine.Leaf(30, 5);
        TooltipLayoutEngine.Grid grid = new TooltipLayoutEngine.Grid(2, 2, Arrays.asList(first, second));
        TooltipLayoutEngine.Measurement measured = grid.measure(new TooltipLayoutEngine.Constraints(40, 100));
        grid.place(new TooltipLayoutEngine.Rect(0, 0, measured.width, measured.height));
        assertEquals(30, measured.width);
        assertEquals(11, measured.height);
        assertEquals(0, first.bounds().x);
        assertEquals(6, second.bounds().y);
    }

    @Test
    void zeroConstraintsAllowEmptyContainers() {
        TooltipLayoutEngine.Flow flow = new TooltipLayoutEngine.Flow(
                TooltipLayoutEngine.Direction.COLUMN, TooltipLayoutEngine.Alignment.START, 0,
                TooltipLayoutEngine.Insets.none(), Arrays.asList());
        TooltipLayoutEngine.Measurement measured = flow.measure(new TooltipLayoutEngine.Constraints(0, 0));
        assertEquals(0, measured.width);
        assertEquals(0, measured.height);
    }
}
