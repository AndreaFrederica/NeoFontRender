package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiSlot.class)
public interface AccessorGuiSlotNavigation {
    @Invoker("getSize") int nfrUi$invokeNavigationSize();
    @Invoker("elementClicked") void nfrUi$invokeElementClicked(
            int index, boolean doubleClick, int mouseX, int mouseY);
    @Invoker("scrollBy") void nfrUi$invokeScrollBy(int amount);
    @Accessor("left") int nfrUi$getLeft();
    @Accessor("right") int nfrUi$getRight();
    @Accessor("top") int nfrUi$getTop();
    @Accessor("bottom") int nfrUi$getBottom();
    @Accessor("slotHeight") int nfrUi$getSlotHeight();
    @Accessor("amountScrolled") float nfrUi$getAmountScrolled();
}
