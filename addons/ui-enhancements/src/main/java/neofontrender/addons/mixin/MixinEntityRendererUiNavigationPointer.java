package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import neofontrender.addons.navigation.UiNavigationRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererUiNavigationPointer {
    @ModifyArgs(
            method = "updateCameraAndRender",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/client/ForgeHooksClient;drawScreen(Lnet/minecraft/client/gui/GuiScreen;IIF)V",
                    remap = false),
            require = 0
    )
    private void uie$replaceForgeScreenPointer(Args args) {
        replace(args, false);
    }

    @ModifyArgs(
            method = "updateCameraAndRender",
            at = @At(value = "INVOKE",
                    target = "Lnet/optifine/reflect/Reflector;callVoid(Lnet/optifine/reflect/ReflectorMethod;[Ljava/lang/Object;)V",
                    remap = false),
            require = 0
    )
    private void uie$replaceOptifineScreenPointer(Args args) {
        Object[] parameters = args.get(1);
        if (parameters == null || parameters.length < 3
                || !(parameters[1] instanceof Integer) || !(parameters[2] instanceof Integer)) return;
        UiNavigationRuntime runtime = UiNavigationRuntime.instance();
        if (!runtime.isSyntheticPointerActive()) return;
        float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
        parameters[1] = runtime.renderPointerX(partialTicks);
        parameters[2] = runtime.renderPointerY(partialTicks);
    }

    private static void replace(Args args, boolean unused) {
        UiNavigationRuntime runtime = UiNavigationRuntime.instance();
        if (!runtime.isSyntheticPointerActive()) return;
        float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
        args.set(1, runtime.renderPointerX(partialTicks));
        args.set(2, runtime.renderPointerY(partialTicks));
    }
}
