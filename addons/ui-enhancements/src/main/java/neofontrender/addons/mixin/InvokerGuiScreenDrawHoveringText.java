package neofontrender.addons.mixin;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/** Exposes the protected vanilla hover-text renderer for the main-menu continue button tooltip. */
@Mixin(GuiScreen.class)
public interface InvokerGuiScreenDrawHoveringText {
    @Invoker("drawHoveringText")
    void nfrUi$drawHoveringText(List<String> textLines, int x, int y, FontRenderer font);
}
