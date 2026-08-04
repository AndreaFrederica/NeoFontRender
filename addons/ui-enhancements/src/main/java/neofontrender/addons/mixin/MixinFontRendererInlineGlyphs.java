package neofontrender.addons.mixin;

import net.minecraft.client.gui.FontRenderer;
import neofontrender.addons.api.inline.InlineTextEngine;
import neofontrender.addons.api.inline.InlineTextLayout;
import neofontrender.addons.api.inline.InlineTextWrapping;
import neofontrender.addons.chat.EnhancedChatFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Thin global adapter for UIE's middleware. Priority 2000 lets it split image runs first; nested
 * ordinary text calls then fall through to NFR's normal FontRenderer mixin under the guard.
 */
@Mixin(value = FontRenderer.class, priority = 2000)
public abstract class MixinFontRendererInlineGlyphs {
    private static final ThreadLocal<Boolean> NFR_UI$ACTIVE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "drawString(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), cancellable = true)
    private void nfrUi$drawInlineGlyphs(String text, float x, float y, int color, boolean shadow,
                                        CallbackInfoReturnable<Integer> cir) {
        if (!EnhancedChatFeatures.inlineGlyphs() || Boolean.TRUE.equals(NFR_UI$ACTIVE.get())
                || text == null || text.isEmpty()) return;
        NFR_UI$ACTIVE.set(Boolean.TRUE);
        try {
            InlineTextLayout layout = InlineTextEngine.layout((FontRenderer) (Object) this, text);
            if (layout.hasGlyphs()) {
                cir.setReturnValue(layout.draw((FontRenderer) (Object) this, x, y, color, shadow));
            }
        } finally {
            NFR_UI$ACTIVE.set(Boolean.FALSE);
        }
    }

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true)
    private void nfrUi$measureInlineGlyphs(String text, CallbackInfoReturnable<Integer> cir) {
        if (!EnhancedChatFeatures.inlineGlyphs() || Boolean.TRUE.equals(NFR_UI$ACTIVE.get())
                || text == null || text.isEmpty()) return;
        NFR_UI$ACTIVE.set(Boolean.TRUE);
        try {
            InlineTextLayout layout = InlineTextEngine.layout((FontRenderer) (Object) this, text);
            if (layout.hasGlyphs()) cir.setReturnValue(layout.width());
        } finally {
            NFR_UI$ACTIVE.set(Boolean.FALSE);
        }
    }

    @Inject(method = "trimStringToWidth(Ljava/lang/String;IZ)Ljava/lang/String;",
            at = @At("HEAD"), cancellable = true)
    private void nfrUi$trimInlineGlyphs(String text, int width, boolean reverse,
                                        CallbackInfoReturnable<String> cir) {
        if (!EnhancedChatFeatures.inlineGlyphs() || Boolean.TRUE.equals(NFR_UI$ACTIVE.get())
                || text == null || text.isEmpty()) return;
        NFR_UI$ACTIVE.set(Boolean.TRUE);
        try {
            InlineTextLayout layout = InlineTextEngine.layout((FontRenderer) (Object) this, text);
            if (!layout.hasGlyphs()) return;
            if (!reverse) {
                cir.setReturnValue(text.substring(0,
                        layout.sourceIndexFitting((FontRenderer) (Object) this, width)));
                return;
            }
            int start = text.length();
            while (start > 0) {
                int previous = Character.offsetByCodePoints(text, start, -1);
                InlineTextLayout suffix = InlineTextEngine.layout((FontRenderer) (Object) this,
                        text.substring(previous));
                if (suffix.width() > width) break;
                start = previous;
            }
            cir.setReturnValue(text.substring(start));
        } finally {
            NFR_UI$ACTIVE.set(Boolean.FALSE);
        }
    }

    @Inject(method = "sizeStringToWidth", at = @At("HEAD"), cancellable = true)
    private void nfrUi$wrapInlineGlyphs(String text, int width,
                                        CallbackInfoReturnable<Integer> cir) {
        if (!EnhancedChatFeatures.inlineGlyphs() || Boolean.TRUE.equals(NFR_UI$ACTIVE.get())
                || text == null || text.isEmpty()) return;
        NFR_UI$ACTIVE.set(Boolean.TRUE);
        try {
            InlineTextLayout layout = InlineTextEngine.layout((FontRenderer) (Object) this, text);
            if (!layout.hasGlyphs()) return;
            List<String> lines = InlineTextWrapping.wrap((FontRenderer) (Object) this, text, width);
            cir.setReturnValue(lines.isEmpty() ? 0 : lines.get(0).length());
        } finally {
            NFR_UI$ACTIVE.set(Boolean.FALSE);
        }
    }
}
