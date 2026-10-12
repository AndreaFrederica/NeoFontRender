package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(GuiScreen.class)
public interface AccessorGuiScreenNavigation {
    @Accessor("buttonList") List<GuiButton> nfrUi$getNavigationButtons();
    @Invoker("mouseClicked") void nfrUi$invokeMouseClicked(int mouseX, int mouseY, int button);
    @Invoker("mouseClickMove") void nfrUi$invokeMouseClickMove(
            int mouseX, int mouseY, int button, long timeSinceClick);
    // 1.7.10 releases buttons through mouseMovedOrUp; mouseReleased only exists from 1.8 on.
    @Invoker("mouseMovedOrUp") void nfrUi$invokeMouseReleased(int mouseX, int mouseY, int button);
    @Invoker("keyTyped") void nfrUi$invokeKeyTyped(char typedChar, int keyCode);
}
