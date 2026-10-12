package neofontrender.addons.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiContainer.class)
public interface AccessorGuiContainer {
    @Accessor("inventorySlots")
    Container nfrUi$getInventorySlots();

    // 1.7.10 keeps the GUI origin in protected fields and exposes no getters for them.
    @Accessor("guiLeft")
    int nfrUi$getGuiLeft();

    @Accessor("guiTop")
    int nfrUi$getGuiTop();

    // 1.7.10 takes the click type as a raw int; the ClickType enum only exists from 1.12.2 on.
    @Invoker("handleMouseClick")
    void nfrUi$invokeHandleMouseClick(Slot slot, int slotId, int mouseButton, int clickType);
}
