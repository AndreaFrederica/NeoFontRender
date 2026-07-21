package neofontrender.client.gui.component.base;

import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.cleanroommc.modularui.utils.Alignment;
import net.minecraft.client.Minecraft;

/** A settings-form field with a consistently styled label above the editor. */
public final class NfrLabeledTextField extends ParentWidget<NfrLabeledTextField> implements ILayoutWidget {
    private final TextWidget label;
    private final TextFieldWidget field;

    public NfrLabeledTextField(String text, TextFieldWidget field) {
        this.label = new TextWidget(IKey.str(text)).alignment(Alignment.CenterLeft).color(0xA9B5C5);
        this.field = field;
        child(label);
        child(field);
    }

    @Override
    public boolean layoutWidgets() {
        int width = getArea().w();
        int labelHeight = Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT + 3;
        NfrLayout.place(label, 0, 0, width, labelHeight);
        NfrLayout.place(field, 0, labelHeight + 4, width, Math.max(18, getArea().h() - labelHeight - 4));
        return true;
    }
}
