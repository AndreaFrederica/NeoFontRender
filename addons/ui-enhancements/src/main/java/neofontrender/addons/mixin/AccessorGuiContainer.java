package neofontrender.addons.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.inventory.Slot;
import net.minecraft.inventory.ClickType;

@Mixin(GuiContainer.class)
public interface AccessorGuiContainer {
    @Accessor("inventorySlots")
    Container nfrUi$getInventorySlots();

    @Invoker("handleMouseClick")
    void nfrUi$invokeHandleMouseClick(Slot slot, int slotId, int mouseButton, ClickType type);
}
