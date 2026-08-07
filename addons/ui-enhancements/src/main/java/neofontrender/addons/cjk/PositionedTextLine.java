package neofontrender.addons.cjk;

import net.minecraft.util.IChatComponent;
import neofontrender.api.text.CjkParagraphLayoutProvider;

import java.util.List;

/** Transient Tiqian geometry attached to a component line owned by UIE. */
public interface PositionedTextLine {
    List<CjkParagraphLayoutProvider.Run> nfrUi$runs();
    float nfrUi$width();
    int nfrUi$visibleOffsetAt(float x);
    float nfrUi$xAtVisibleOffset(int offset);
    IChatComponent nfrUi$componentAt(float x);
    float nfrUi$componentLeft(IChatComponent component);
    float nfrUi$componentRight(IChatComponent component);
}
