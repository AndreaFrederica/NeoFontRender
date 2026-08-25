package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiCreateWorld;
import neofontrender.addons.worldcreation.CreateWorldConfig;
import neofontrender.addons.worldcreation.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opens the component-based create-world view after vanilla has initialized its state host. */
@Mixin(GuiCreateWorld.class)
public abstract class MixinGuiCreateWorldModularBridge {
    @Unique private boolean nfrUi$openingModularView;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void nfrUi$openModularCreateWorld(CallbackInfo ci) {
        if (nfrUi$openingModularView || !CreateWorldConfig.usesTabbedLayout()
                || ((Object) this).getClass() != GuiCreateWorld.class) return;
        nfrUi$openingModularView = true;
        CreateWorldScreen.open((GuiCreateWorld) (Object) this);
        nfrUi$openingModularView = false;
    }
}
