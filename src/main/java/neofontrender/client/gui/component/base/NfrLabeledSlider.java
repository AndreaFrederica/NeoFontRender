package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.GuiAxis;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

/** A dynamic label and slider assembled as one consistently sized settings control. */
public final class NfrLabeledSlider extends ParentWidget<NfrLabeledSlider> implements ILayoutWidget {
    private final TextWidget label;
    private final SliderWidget slider;

    public NfrLabeledSlider(Supplier<String> text, SliderWidget slider) {
        this.label = new TextWidget(IKey.dynamic(text)).alignment(Alignment.CenterLeft).color(0xA9B5C5);
        this.slider = slider;
        child(label);
        child(slider);
    }

    public int preferredHeight() {
        return Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 3 + 6 + 20;
    }

    @Override
    public boolean layoutWidgets() {
        int labelHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 3;
        NfrLayout.place(label, 0, 0, getArea().w(), labelHeight);
        NfrLayout.place(slider, 0, labelHeight + 6, getArea().w(), 20);
        if (getArea().h() <= 0) getArea().setSize(GuiAxis.Y, preferredHeight());
        return true;
    }
}
