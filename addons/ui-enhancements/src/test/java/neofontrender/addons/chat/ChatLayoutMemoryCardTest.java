package neofontrender.addons.chat;

import com.cleanroommc.modularui.widget.Widget;
import neofontrender.client.gui.component.base.NfrLayout;
import neofontrender.client.gui.component.base.NfrOptionsGrid;
import neofontrender.client.gui.component.base.NfrSettingsCard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatLayoutMemoryCardTest {
    @Test void cardsRemainFullWidthAndDoNotGrowAcrossRepeatedLayoutPasses() {
        var firstAction = new Widget<>().size(180, 24);
        var secondAction = new Widget<>().size(180, 24);
        var card = new NfrSettingsCard(() -> "1920 × 1080", () -> "Position / size")
                .action(firstAction).action(secondAction);
        var next = new NfrSettingsCard(() -> "Next", () -> "");
        var grid = new NfrOptionsGrid(260, 24, 8, true)
                .add(new Widget<>().size(260, 24)).add(card).add(next);
        for (int width : new int[]{240, 600, 240}) {
            int expectedHeight = grid.preferredHeight(width);
            for (int pass = 0; pass < 5; pass++) {
                NfrLayout.place(grid, 0, 0, width, expectedHeight);
                assertEquals(expectedHeight, grid.preferredHeight(width));
                assertEquals(width, card.getArea().w());
                assertTrue(next.getArea().y() >= card.getArea().y() + card.getArea().h());
                assertTrue(secondAction.getArea().y() + secondAction.getArea().h() <= card.getArea().h());
            }
        }
    }
}
