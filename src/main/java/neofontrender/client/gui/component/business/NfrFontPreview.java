package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;

import java.util.function.Supplier;

/** Live font preview used by the font settings view. */
public final class NfrFontPreview extends Widget<NfrFontPreview> {
    private final Supplier<String> selectedFont;
    public NfrFontPreview(Supplier<String> selectedFont){this.selectedFont=selectedFont;}
    @Override public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme){
        super.draw(context,theme);int x=8,y=8;
        Gui.drawRect(4,4,Math.max(4,getArea().w()-4),Math.max(4,getArea().h()-4),0x66000000);
        Platform.setupDrawFont();Minecraft mc=Minecraft.getMinecraft();
        mc.fontRenderer.drawString(I18n.format("neofontrender.gui.preview.font",selectedFont.get()),x,y,0xFFFFFF);
        mc.fontRenderer.drawString(I18n.format("neofontrender.gui.preview.sample"),x,y+14,0xD8D8D8);
        mc.fontRenderer.drawString(I18n.format("neofontrender.gui.preview.styles"),x,y+28,0xFFFFFF);
    }
}
