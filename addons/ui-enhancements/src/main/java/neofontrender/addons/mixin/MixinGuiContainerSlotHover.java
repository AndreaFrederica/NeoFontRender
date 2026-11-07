package neofontrender.addons.mixin;

import com.cleanroommc.modularui.api.IMuiScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Slot;
import neofontrender.addons.hover.HoverAnimationState;
import neofontrender.addons.hover.HoverEffectsConfigAccess;
import neofontrender.addons.hover.HoverEffectsRenderer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainerSlotHover {
    @Shadow private Slot hoveredSlot;

    @Unique private final IdentityHashMap<Slot, HoverAnimationState> nfrUi$slotAnimations =
            new IdentityHashMap<>();

    @ModifyArgs(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGradientRect(IIIIII)V"))
    private void nfrUi$hideVanillaSlotHighlight(Args args) {
        if (nfrUi$animateVanillaSlots()) {
            args.set(4, 0);
            args.set(5, 0);
        }
    }

    @Inject(method = "drawScreen", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGuiContainerForegroundLayer(II)V"))
    private void nfrUi$drawAnimatedSlotHighlights(int mouseX, int mouseY, float partialTicks,
                                                  CallbackInfo ci) {
        if (!nfrUi$animateVanillaSlots()) {
            nfrUi$slotAnimations.clear();
            return;
        }

        Slot current = hoveredSlot;
        if (current != null) nfrUi$slotAnimations.computeIfAbsent(current, ignored -> new HoverAnimationState());
        if (nfrUi$slotAnimations.isEmpty()) return;

        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        FloatBuffer currentColor = BufferUtils.createFloatBuffer(4);
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, currentColor);
        IntBuffer currentColorMask = BufferUtils.createIntBuffer(4);
        GL11.glGetInteger(GL11.GL_COLOR_WRITEMASK, currentColorMask);

        try {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.colorMask(true, true, true, false);

            Iterator<Map.Entry<Slot, HoverAnimationState>> iterator = nfrUi$slotAnimations.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Slot, HoverAnimationState> entry = iterator.next();
                Slot slot = entry.getKey();
                HoverAnimationState animation = entry.getValue();
                boolean active = slot == current;
                animation.update(active, HoverEffectsConfigAccess.slotEnterMillis(),
                        HoverEffectsConfigAccess.slotExitMillis());
                if (!animation.isVisible() && !active) {
                    iterator.remove();
                    continue;
                }
                nfrUi$drawSlotHighlight(slot, animation.easedProgress());
            }
        } finally {
            boolean maskRed = currentColorMask.get(0) != 0;
            boolean maskGreen = currentColorMask.get(1) != 0;
            boolean maskBlue = currentColorMask.get(2) != 0;
            boolean maskAlpha = currentColorMask.get(3) != 0;
            GlStateManager.colorMask(maskRed, maskGreen, maskBlue, maskAlpha);
            GL11.glColorMask(maskRed, maskGreen, maskBlue, maskAlpha);
            GlStateManager.color(currentColor.get(0), currentColor.get(1),
                    currentColor.get(2), currentColor.get(3));
            GL11.glColor4f(currentColor.get(0), currentColor.get(1),
                    currentColor.get(2), currentColor.get(3));
            nfrUi$restoreToggle(lighting, GL11.GL_LIGHTING,
                    GlStateManager::enableLighting, GlStateManager::disableLighting);
            nfrUi$restoreToggle(depth, GL11.GL_DEPTH_TEST,
                    GlStateManager::enableDepth, GlStateManager::disableDepth);
            nfrUi$restoreToggle(blend, GL11.GL_BLEND,
                    GlStateManager::enableBlend, GlStateManager::disableBlend);
            nfrUi$restoreToggle(texture, GL11.GL_TEXTURE_2D,
                    GlStateManager::enableTexture2D, GlStateManager::disableTexture2D);
        }
    }

    @Unique
    private static void nfrUi$restoreToggle(boolean enabled, int capability,
                                            Runnable enable, Runnable disable) {
        if (enabled) enable.run(); else disable.run();
        // Keep raw GL state and GlStateManager's cache in agreement for following renderers.
        if (enabled) GL11.glEnable(capability); else GL11.glDisable(capability);
    }

    @Unique
    private boolean nfrUi$animateVanillaSlots() {
        return HoverEffectsConfigAccess.slotsEnabled() && !((Object) this instanceof IMuiScreen);
    }

    @Unique
    private void nfrUi$drawSlotHighlight(Slot slot, float progress) {
        int color = HoverEffectsRenderer.multiplyAlpha(HoverEffectsConfigAccess.slotColor(), progress);
        Gui.drawRect(slot.xPos, slot.yPos, slot.xPos + 16, slot.yPos + 16, color);
    }
}
