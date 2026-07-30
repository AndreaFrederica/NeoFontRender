package neofontrender.addons.hover;

import java.awt.Rectangle;

/** Duck interface applied only to JEI/HEI ingredient renderers by optional late mixins. */
public interface IngredientGridHoverTarget {
    Rectangle nfrUi$hoverArea();
    void nfrUi$drawOriginalHighlight();
}
