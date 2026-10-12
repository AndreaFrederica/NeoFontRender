package neofontrender.addons.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import neofontrender.addons.navigation.UiNavigationRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererUiNavigationPointer {
    /**
     * 1.7.10 passes the mouse position straight into GuiScreen#drawScreen and has no
     * ForgeHooksClient.drawScreen indirection, so the synthetic pointer replaces args 0 and 1.
     */
    @ModifyArgs(
            method = "updateCameraAndRender",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiScreen;drawScreen(IIF)V"),
            require = 0
    )
    private void uie$replaceScreenPointer(Args args) {
        UiNavigationRuntime runtime = UiNavigationRuntime.instance();
        if (!runtime.isSyntheticPointerActive()) return;
        args.set(0, runtime.renderPointerX(1.0F));
        args.set(1, runtime.renderPointerY(1.0F));
    }
}
