package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.renderer.Tessellator;
import neofontrender.addons.navigation.vanilla.VanillaWidgetCapture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiListExtended.class)
public abstract class MixinGuiListExtendedNavigationCapture {
    @Shadow public abstract GuiListExtended.IGuiListEntry getListEntry(int index);

    // 1.7.10 drawSlot carries the Tessellator and has no partialTicks parameter.
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void nfrUi$beginNavigationEntry(int index, int entryLeft, int rowTop, int slotHeight,
                                            Tessellator tessellator, int mouseX, int mouseY,
                                            CallbackInfo ci) {
        VanillaWidgetCapture.beginListEntry((GuiListExtended) (Object) this,
                getListEntry(index), index, entryLeft, rowTop);
    }

    @Inject(method = "drawSlot", at = @At("RETURN"))
    private void nfrUi$endNavigationEntry(int index, int entryLeft, int rowTop, int slotHeight,
                                          Tessellator tessellator, int mouseX, int mouseY,
                                          CallbackInfo ci) {
        VanillaWidgetCapture.endListEntry((GuiListExtended) (Object) this,
                getListEntry(index));
    }
}
