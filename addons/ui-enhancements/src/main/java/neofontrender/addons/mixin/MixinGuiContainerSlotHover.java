package neofontrender.addons.mixin;

import com.cleanroommc.modularui.api.IMuiScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import neofontrender.addons.hover.HoverAnimationState;
import neofontrender.addons.hover.HoverEffectsConfigAccess;
import neofontrender.addons.hover.HoverEffectsRenderer;
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

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainerSlotHover {
    // 1.7.10 names GuiContainer#hoveredSlot as theSlot (SRG field_147006_u).
    @Shadow private Slot theSlot;

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

        Slot current = theSlot;
        if (current != null) nfrUi$slotAnimations.computeIfAbsent(current, ignored -> new HoverAnimationState());
        if (nfrUi$slotAnimations.isEmpty()) return;

        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColorMask(true, true, true, false);

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

        GL11.glColorMask(true, true, true, true);
        if (lighting) GL11.glEnable(GL11.GL_LIGHTING);
        if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    @Unique
    private boolean nfrUi$animateVanillaSlots() {
        return HoverEffectsConfigAccess.slotsEnabled() && !((Object) this instanceof IMuiScreen);
    }

    @Unique
    private void nfrUi$drawSlotHighlight(Slot slot, float progress) {
        int color = HoverEffectsRenderer.multiplyAlpha(HoverEffectsConfigAccess.slotColor(), progress);
        // 1.7.10 names Slot#xPos/yPos as xDisplayPosition/yDisplayPosition.
        Gui.drawRect(slot.xDisplayPosition, slot.yDisplayPosition,
                slot.xDisplayPosition + 16, slot.yDisplayPosition + 16, color);
    }
}
