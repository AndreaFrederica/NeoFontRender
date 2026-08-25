package neofontrender.addons.mixin.compat;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.tooltips.ThaumcraftTooltipCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Bridges Thaumcraft's private tooltip renderer into Forge's modern tooltip event path. */
@Pseudo
@Mixin(targets = "thaumcraft.client.lib.UtilsFX", remap = false)
public abstract class MixinThaumcraftCustomTooltip {
    private static final String TOOLTIP = "drawCustomTooltip(Lnet/minecraft/client/gui/GuiScreen;"
            + "Lnet/minecraft/client/gui/FontRenderer;Ljava/util/List;III)V";
    private static final String TOOLTIP_WITH_FLAG =
            "drawCustomTooltip(Lnet/minecraft/client/gui/GuiScreen;"
                    + "Lnet/minecraft/client/gui/FontRenderer;Ljava/util/List;IIIZ)V";

    @Inject(method = TOOLTIP, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void nfrUi$renderModernTooltip(GuiScreen screen, FontRenderer font,
                                                   List<String> lines, int x, int y, int color,
                                                   CallbackInfo ci) {
        if (!ThaumcraftTooltipCompat.isEnabled()) return;
        ThaumcraftTooltipCompat.draw(screen, font, lines, x, y, color);
        ci.cancel();
    }

    @Inject(method = TOOLTIP_WITH_FLAG, at = @At("HEAD"), cancellable = true,
            require = 0, remap = false)
    private static void nfrUi$renderModernTooltipWithFlag(GuiScreen screen, FontRenderer font,
                                                           List<String> lines, int x, int y,
                                                           int color, boolean right,
                                                           CallbackInfo ci) {
        if (!ThaumcraftTooltipCompat.isEnabled()) return;
        ThaumcraftTooltipCompat.draw(screen, font, lines, x, y, color);
        ci.cancel();
    }
}
