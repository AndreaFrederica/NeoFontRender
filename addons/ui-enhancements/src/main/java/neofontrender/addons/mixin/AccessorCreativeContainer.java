package neofontrender.addons.mixin;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/** Accesses the package-private creative inventory container without naming its class in host code. */
@Mixin(targets = "net.minecraft.client.gui.inventory.GuiContainerCreative$ContainerCreative")
public interface AccessorCreativeContainer {
    @Accessor("itemList")
    List<ItemStack> nfrUi$getItemList();

    @Invoker("scrollTo")
    void nfrUi$scrollTo(float position);
}
