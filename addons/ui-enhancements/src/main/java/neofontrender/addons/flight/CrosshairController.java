package neofontrender.addons.flight;

import icyllis.arc3d.core.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.zoom.ZoomModule;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Lowest-priority CCM-compatible renderer; vanilla suppression remains a narrow mixin. */
public final class CrosshairController {
    static final CrosshairController INSTANCE = new CrosshairController();
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final RenderItem RENDER_ITEM = new RenderItem();
    private RenderGameOverlayEvent.Pre claimedLayer;

    private CrosshairController() {}

    /** Claims the crosshair layer before item mods such as TiC draw and cancel their own crosshair. */
    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public void claimCrosshairLayer(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        // Shoulder Surfing intentionally cancels both ordinary third-person perspectives. The
        // explicit UIE visibility option is allowed to override that cancellation.
        if (event.isCanceled() && overridesOrdinaryThirdPersonVisibility()) {
            claimedLayer = event;
            return;
        }
        if (!event.isCanceled() && CrosshairConfig.customEnabled
                && !CrosshairConfig.preferModCrosshair) {
            claimedLayer = event;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void crosshair(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        boolean claimed = claimedLayer == event;
        claimedLayer = null;
        if (ShoulderSurfingMatrixFix.dualCursorMode()
                && CrosshairConfig.customEnabled && isVisible()) {
            drawBlockInteractionCursor(event.resolution);
        }
        if (event.isCanceled() && !claimed) return;
        if (!CrosshairConfig.customEnabled || FlightRollController.suppressVanillaCrosshair()) return;
        // In mod-priority mode an item mod gets the first chance to cancel the layer. Reaching
        // LOWEST means nobody took it, so UIE becomes the renderer and suppresses vanilla.
        if (!event.isCanceled()) event.setCanceled(true);
        if (!isVisible()) return;

        ScaledResolution resolution = event.resolution;
        float centerX = resolution.getScaledWidth() * 0.5F;
        float centerY = resolution.getScaledHeight() * 0.5F;
        float[] shoulderOffset = ShoulderSurfingCompat.crosshairOffset();
        if (shoulderOffset != null) {
            centerX += shoulderOffset[0];
            centerY += shoulderOffset[1];
        }
        draw(centerX, centerY, event.partialTicks);
    }

    /** Orange bracket marker for the camera-origin block/entity interaction ray in dual mode. */
    private static void drawBlockInteractionCursor(ScaledResolution resolution) {
        float cx = resolution.getScaledWidth() * 0.5F;
        float cy = resolution.getScaledHeight() * 0.5F;
        GlSnapshot state = new GlSnapshot();
        try {
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(cx, cy, 0.0F);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                drawBlockInteractionGlyph(0xD0000000, 3.0F);
                drawBlockInteractionGlyph(0xFFFFA52F, 1.25F);
            } finally {
                GL11.glPopMatrix();
            }
        } finally {
            state.restore();
        }
    }

    private static void drawBlockInteractionGlyph(int color, float thickness) {
        float inner = 3.0F;
        float outer = 8.0F;
        line(-outer, -outer, -inner, -outer, color, thickness);
        line(-outer, -outer, -outer, -inner, color, thickness);
        line(inner, -outer, outer, -outer, color, thickness);
        line(outer, -outer, outer, -inner, color, thickness);
        line(-outer, inner, -outer, outer, color, thickness);
        line(-outer, outer, -inner, outer, color, thickness);
        line(outer, inner, outer, outer, color, thickness);
        line(inner, outer, outer, outer, color, thickness);
        quad(-1.25F, -1.25F, 1.25F, 1.25F, color);
    }

    /** Used by the Forge-GUI mixin; deliberately does not cancel the CROSSHAIRS event. */
    public static boolean suppressVanillaCrosshair() {
        return CrosshairConfig.customEnabled || FlightRollController.suppressVanillaCrosshair();
    }

    public static boolean preferModCrosshairs() {
        return CrosshairConfig.customEnabled && CrosshairConfig.preferModCrosshair;
    }

    private static boolean overridesOrdinaryThirdPersonVisibility() {
        return CrosshairConfig.customEnabled && CrosshairConfig.visibleInThirdPerson
                && MC.gameSettings.thirdPersonView != 0 && !ShoulderSurfingCompat.isActive();
    }

    private static boolean isVisible() {
        EntityPlayer player = MC.thePlayer;
        if (player == null || !CrosshairConfig.visibleByDefault) return false;
        if (!CrosshairConfig.visibleWithHiddenGui && MC.gameSettings.hideGUI) return false;
        if (!CrosshairConfig.visibleInDebug && MC.gameSettings.showDebugInfo) return false;
        if (!CrosshairConfig.visibleInThirdPerson && MC.gameSettings.thirdPersonView != 0
                && !ShoulderSurfingCompat.isActive()) return false;
        if (!CrosshairConfig.visibleHoldingRanged && holdsRanged(player)) return false;
        if (!CrosshairConfig.visibleHoldingThrowable && holdsThrowable(player)) return false;
        return CrosshairConfig.visibleUsingSpyglass || !usingSpyglass(player);
    }

    private static boolean holdsThrowable(EntityPlayer player) {
        ItemStack stack = player.getCurrentEquippedItem();
        if (stack == null) return false;
        Item item = stack.getItem();
        return item == Items.ender_pearl || item == Items.ender_eye || item == Items.snowball
                || item == Items.egg || item == Items.experience_bottle
                || item == Items.potionitem
                || BackportCrosshairCompat.isTrident(stack);
    }

    private static boolean holdsRanged(EntityPlayer player) {
        return BackportCrosshairCompat.isRangedWeapon(player.getCurrentEquippedItem());
    }

    private static boolean usingSpyglass(EntityPlayer player) {
        return ZoomModule.isZoomActive() || (player.isUsingItem()
                && BackportCrosshairCompat.isSpyglass(player.getItemInUse()));
    }

    private static void draw(float centerX, float centerY, float partialTicks) {
        float cx = centerX + CrosshairConfig.offsetX;
        float cy = centerY + CrosshairConfig.offsetY;
        int gap = computedGap(partialTicks);
        Entity target = targetEntity(partialTicks);
        int color = computedColor(target);

        GlSnapshot state = new GlSnapshot();
        try {
            drawIndicators(cx, cy);
            GL11.glPushMatrix();
            try {
                GL11.glTranslatef(cx, cy, 0.0F);
                GL11.glRotatef(CrosshairConfig.rotation, 0.0F, 0.0F, 1.0F);
                float scale = CrosshairConfig.scalePercent / 100.0F;
                GL11.glScalef(scale, scale, 1.0F);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_BLEND);

                GL11.glDisable(GL11.GL_TEXTURE_2D);

                if (CrosshairConfig.dotEnabled && !"vanilla".equals(CrosshairConfig.style)) {
                    setBlend(false);
                    filledCircle(0, 0, Math.max(1.0F, CrosshairConfig.thickness), CrosshairConfig.dotColor);
                }

                if (CrosshairConfig.outlineEnabled && supportsOutline(CrosshairConfig.style)) {
                    setBlend(false);
                    drawStyle(CrosshairConfig.style, gap, CrosshairConfig.outlineColor,
                            CrosshairConfig.thickness + 2.0F, 1.0F);
                }
                setBlend(CrosshairConfig.adaptiveColor);
                drawStyle(CrosshairConfig.style, gap, color, CrosshairConfig.thickness, 0.0F);
            } finally {
                GL11.glPopMatrix();
            }
        } finally {
            state.restore();
        }
    }

    private static boolean supportsOutline(String style) {
        return !"vanilla".equals(style) && !"vanilla_plus".equals(style)
                && !"dot".equals(style) && !"drawn".equals(style);
    }

    private static int computedGap(float partialTicks) {
        int base = CrosshairConfig.gap;
        EntityPlayer player = MC.thePlayer;
        if (player == null) return base;
        if (CrosshairConfig.dynamicBow && player.isUsingItem()
                && BackportCrosshairCompat.isChargeWeapon(player.getItemInUse())) {
            ItemStack active = player.getItemInUse();
            int used = active.getMaxItemUseDuration() - player.getItemInUseDuration();
            float progress = BackportCrosshairCompat.chargeProgress(active, player, used);
            return base + Math.round((1.0F - progress) * 40.0F);
        }
        return base;
    }

    private static int computedColor(Entity target) {
        if (target instanceof EntityLivingBase && !canAttack((EntityLivingBase) target)) {
            return CrosshairConfig.color;
        }
        if (target instanceof EntityPlayer && CrosshairConfig.highlightPlayers) return CrosshairConfig.playerColor;
        if (target instanceof IMob && CrosshairConfig.highlightHostiles) return CrosshairConfig.hostileColor;
        if (target instanceof EntityLiving && CrosshairConfig.highlightPassives) return CrosshairConfig.passiveColor;
        if (!CrosshairConfig.rainbowEnabled) return CrosshairConfig.color;
        float speed = Math.max(1, CrosshairConfig.rainbowSpeed);
        float hue = (System.currentTimeMillis() % Math.max(1L, Math.round(2000000.0F / speed)))
                / (Math.max(1.0F, 2000000.0F / speed));
        int rgb = java.awt.Color.HSBtoRGB(hue, 1.0F, 1.0F);
        return (CrosshairConfig.color & 0xFF000000) | (rgb & 0x00FFFFFF);
    }

    private static boolean canAttack(EntityLivingBase target) {
        EntityPlayer player = MC.thePlayer;
        if (player == null || target == player || !target.canAttackWithItem()) return false;
        if (target instanceof EntityPlayer) return player.canAttackPlayer((EntityPlayer) target);
        return !player.isOnSameTeam(target);
    }

    private static Entity targetEntity(float partialTicks) {
        MovingObjectPosition hit = MC.objectMouseOver;
        Entity fallback = hit == null ? null : hit.entityHit;
        return ShoulderSurfingCompat.crosshairTarget(partialTicks, fallback);
    }

    private static void drawStyle(String style, int gap, int color, float thickness, float expansion) {
        float w = CrosshairConfig.width + expansion;
        float h = CrosshairConfig.height + expansion;
        float g = Math.max(0.0F, gap - expansion * 0.5F);
        switch (style) {
            case "vanilla":
            case "debug":
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                MC.getTextureManager().bindTexture(Gui.icons);
                setBlend(true);
                Gui.func_146110_a(-7, -7, 0, 0, 15, 15, 256, 256);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                break;
            case "vanilla_plus":
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                MC.getTextureManager().bindTexture(Gui.icons);
                setBlend(true);
                Gui.func_146110_a(-7, -7, 0, 0, 15, 15, 256, 256);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                drawVanillaPlusCharge(color, thickness);
                break;
            case "dot": filledCircle(0, 0, Math.max(1.0F, thickness), color); break;
            case "circle": circle(0, 0, g + Math.max(w, h), color, thickness); break;
            case "square":
                line(-g - w, -g - h, g + w, -g - h, color, thickness);
                line(g + w, -g - h, g + w, g + h, color, thickness);
                line(g + w, g + h, -g - w, g + h, color, thickness);
                line(-g - w, g + h, -g - w, -g - h, color, thickness);
                break;
            case "triangle":
                line(0, -g - h, g + w, g + h, color, thickness);
                line(g + w, g + h, -g - w, g + h, color, thickness);
                line(-g - w, g + h, 0, -g - h, color, thickness);
                break;
            case "arrow": case "chevron":
                line(-g - w, g + h, 0, 0, color, thickness);
                line(0, 0, g + w, g + h, color, thickness);
                break;
            case "drawn": drawPattern(color); break;
            default:
                quad(-thickness * 0.5F, -g - h, thickness * 0.5F, -g, color);
                quad(-thickness * 0.5F, g, thickness * 0.5F, g + h, color);
                quad(-g - w, -thickness * 0.5F, -g, thickness * 0.5F, color);
                quad(g, -thickness * 0.5F, g + w, thickness * 0.5F, color);
                break;
        }
    }

    /** TiC-like contracting four-way cue layered around the untouched vanilla crosshair. */
    private static void drawVanillaPlusCharge(int color, float thickness) {
        EntityPlayer player = MC.thePlayer;
        if (player == null || !player.isUsingItem()) return;
        ItemStack active = player.getItemInUse();
        if (!BackportCrosshairCompat.isChargeWeapon(active)) return;
        int used = active.getMaxItemUseDuration() - player.getItemInUseDuration();
        float progress = BackportCrosshairCompat.chargeProgress(active, player, used);
        float radius = 8.0F + (1.0F - progress) * 18.0F;
        float arm = 3.0F;
        setBlend(CrosshairConfig.adaptiveColor);
        line(-arm, -radius, arm, -radius, color, thickness);
        line(-arm, radius, arm, radius, color, thickness);
        line(-radius, -arm, -radius, arm, color, thickness);
        line(radius, -arm, radius, arm, color, thickness);
    }

    private static void drawPattern(int color) {
        boolean[][] pixels = CrosshairPattern.parse(CrosshairConfig.drawnPixels, CrosshairConfig.drawnSize);
        float offset = (CrosshairConfig.drawnSize - 1) * 0.5F;
        for (int x = 0; x < pixels.length; x++) for (int y = 0; y < pixels[x].length; y++) {
            if (pixels[x][y]) quad(x - offset, y - offset, x - offset + 1, y - offset + 1, color);
        }
    }

    private static void drawIndicators(float cx, float cy) {
        if (MC.thePlayer == null) return;
        ItemStack tool = MC.thePlayer.getCurrentEquippedItem();
        int x = Math.round(cx) + CrosshairConfig.gap + 6;
        int y = Math.round(cy) + CrosshairConfig.gap + 6;
        if (CrosshairConfig.toolDamageEnabled && tool != null && tool.isItemStackDamageable()) {
            int remaining = tool.getMaxDamage() - tool.getItemDamage();
            if (remaining <= 10) { drawIndicator(tool, Integer.toString(remaining), x, y); x += 15; }
        }
        if (CrosshairConfig.projectileIndicatorEnabled
                && tool != null && tool.getItem() instanceof ItemBow) {
            ItemStack ammo = findVanillaArrow();
            int count = BackportCrosshairCompat.projectileCount(ammo);
            if (ammo != null) drawIndicator(ammo,
                    MC.thePlayer.capabilities.isCreativeMode ? "" : Integer.toString(count), x, y);
        }
    }

    private static ItemStack findVanillaArrow() {
        ItemStack main = MC.thePlayer.getCurrentEquippedItem();
        if (main != null && main.getItem() == Items.arrow) return main;
        for (int i = 0; i < MC.thePlayer.inventory.getSizeInventory(); i++) {
            ItemStack stack = MC.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.arrow) return stack;
        }
        return null;
    }

    private static void drawIndicator(ItemStack stack, String text, int x, int y) {
        GL11.glPushMatrix();
        try {
            GL11.glScalef(0.5F, 0.5F, 1.0F);
            RENDER_ITEM.renderItemAndEffectIntoGUI(MC.fontRenderer, MC.getTextureManager(),
                    stack, x * 2 - 8, y * 2 - 8);
        } finally { GL11.glPopMatrix(); }
        if (text != null && !text.isEmpty()) MC.fontRenderer.drawStringWithShadow(text, x + 5, y, 0xFFFFFFFF);
    }

    private static void setBlend(boolean adaptive) {
        GL11.glEnable(GL11.GL_BLEND);
        if (adaptive) GL14.glBlendFuncSeparate(GL11.GL_ONE_MINUS_DST_COLOR,
                GL11.GL_ONE_MINUS_SRC_COLOR, GL11.GL_ONE, GL11.GL_ZERO);
        else GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    private static void circle(float cx, float cy, float radius, int color, float width) {
        partialCircle(cx, cy, radius, 0, 360, color, width);
    }

    private static void partialCircle(float cx, float cy, float radius, int start, int end, int color, float width) {
        GL11.glLineWidth(Math.max(1.0F, width));
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINE_STRIP);
        for (int i = start; i <= end; i += 3) {
            double angle = Math.PI * 2.0D * i / 360.0D - Math.PI / 2.0D;
            vertex(tessellator, cx + (float) Math.cos(angle) * radius,
                    cy + (float) Math.sin(angle) * radius, color);
        }
        tessellator.draw();
    }

    private static void filledCircle(float cx, float cy, float radius, int color) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_TRIANGLE_FAN);
        vertex(tessellator, cx, cy, color);
        for (int i = 0; i <= 32; i++) {
            double angle = Math.PI * 2.0D * i / 32.0D;
            vertex(tessellator, cx + (float) Math.cos(angle) * radius,
                    cy + (float) Math.sin(angle) * radius, color);
        }
        tessellator.draw();
    }

    private static void line(float x1, float y1, float x2, float y2, int color, float width) {
        GL11.glLineWidth(Math.max(1.0F, width));
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_LINES);
        vertex(tessellator, x1, y1, color); vertex(tessellator, x2, y2, color);
        tessellator.draw();
    }

    private static void quad(float left, float top, float right, float bottom, int color) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawing(GL11.GL_QUADS);
        vertex(tessellator, left, top, color); vertex(tessellator, left, bottom, color);
        vertex(tessellator, right, bottom, color); vertex(tessellator, right, top, color);
        tessellator.draw();
    }

    private static void vertex(Tessellator tessellator, float x, float y, int color) {
        tessellator.setColorRGBA(Color.red(color), Color.green(color),
                Color.blue(color), Color.alpha(color));
        tessellator.addVertex(x, y, 0.0D);
    }

    private static final class GlSnapshot {
        private final boolean texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        private final boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final float width = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        private final int sourceRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        private final int destinationRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        private final int sourceAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        private final int destinationAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        private final int textureBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);

        private void restore() {
            GL11.glLineWidth(width);
            GL14.glBlendFuncSeparate(sourceRgb, destinationRgb, sourceAlpha, destinationAlpha);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureBinding);
            if (texture) GL11.glEnable(GL11.GL_TEXTURE_2D); else GL11.glDisable(GL11.GL_TEXTURE_2D);
            if (depth) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
            if (blend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
        }
    }
}
