package neofontrender.mixin;

import com.cleanroommc.modularui.drawable.text.TextRenderer;
import net.minecraft.client.gui.FontRenderer;
import neofontrender.core.font.support.TooltipBoundsCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Uses shaped-text pixel bounds for ModularUI tooltip lines without changing normal widgets. */
@Mixin(value = TextRenderer.class, remap = false)
public abstract class MixinModularTextRenderer {
    @Shadow protected float scale;

    @Inject(method = "line(Ljava/lang/String;)Lcom/cleanroommc/modularui/drawable/text/TextRenderer$Line;",
            at = @At("HEAD"), cancellable = true)
    private void sfr$measureRichTooltipLine(String text,
                                            CallbackInfoReturnable<TextRenderer.Line> cir) {
        if (!TooltipBoundsCompat.isRichTooltipLayout()) return;
        FontRenderer font = TextRenderer.getFontRenderer();
        cir.setReturnValue(new TextRenderer.Line(text,
                TooltipBoundsCompat.measuredWidth(font, text) * this.scale));
    }
}
