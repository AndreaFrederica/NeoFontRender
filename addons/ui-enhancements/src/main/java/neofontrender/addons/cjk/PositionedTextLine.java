package neofontrender.addons.cjk;

import net.minecraft.util.text.ITextComponent;
import neofontrender.api.text.CjkParagraphLayoutProvider;

import java.util.List;

/** Transient Tiqian geometry attached to a component line owned by UIE. */
public interface PositionedTextLine {
    List<CjkParagraphLayoutProvider.Run> nfrUi$runs();
    float nfrUi$width();
    int nfrUi$visibleOffsetAt(float x);
    float nfrUi$xAtVisibleOffset(int offset);
    ITextComponent nfrUi$componentAt(float x);
    float nfrUi$componentLeft(ITextComponent component);
    float nfrUi$componentRight(ITextComponent component);
}
