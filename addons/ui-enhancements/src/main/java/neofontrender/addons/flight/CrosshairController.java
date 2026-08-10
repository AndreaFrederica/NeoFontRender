package neofontrender.addons.flight;

import icyllis.arc3d.core.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.addons.mixin.InvokerGuiIngameCrosshair;
import neofontrender.addons.zoom.ZoomModule;
import neofontrender.addons.camera.CameraRuntime;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Lowest-priority CCM-compatible renderer; vanilla suppression remains a narrow mixin. */
public final class CrosshairController {
    static final CrosshairController INSTANCE = new CrosshairController();
    private static final Minecraft MC = Minecraft.getMinecraft();
    private RenderGameOverlayEvent.Pre claimedLayer;

    private CrosshairController() {}

    /** Claims the crosshair layer before item mods such as TiC draw and cancel their own crosshair. */
    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public void claimCrosshairLayer(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
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
        if (event.getType() != RenderGameOverlayEvent.ElementType.CROSSHAIRS) return;
        boolean claimed = claimedLayer == event;
        claimedLayer = null;
        if ((ShoulderSurfingMatrixFix.dualCursorMode() || CameraRuntime.shoulderCrosshairDual())
                && isVisible()) {
            drawBlockInteractionCursor(event.getResolution(),
                    CameraRuntime.shoulderSecondaryCrosshairOffset(event.getPartialTicks()));
        }
        if (CameraRuntime.isDroneActive()) {
            drawDroneCameraCursor(event.getResolution());
        }
        if (event.isCanceled() && !claimed) return;
        boolean flightSuppressesCrosshair = FlightRollController.suppressVanillaCrosshair();
        if (ModCrosshairRouting.shouldRenderFlightAim(flightSuppressesCrosshair,
                holdsPlayerAimItem(MC.player), isVisible(true))) {
            if (!event.isCanceled()) event.setCanceled(true);
            drawFlightAimCrosshair(event.getResolution(), event.getPartialTicks());
            return;
        }
        if (!CrosshairConfig.customEnabled || flightSuppressesCrosshair) return;
        // In mod-priority mode an item mod gets the first chance to cancel the layer. Reaching
        // LOWEST means nobody took it, so UIE becomes the renderer and suppresses vanilla.
        if (!event.isCanceled()) event.setCanceled(true);
        if (!isVisible() || !cameraCrosshairVisible(event.getPartialTicks())) return;

        ScaledResolution resolution = event.getResolution();
        float centerX = resolution.getScaledWidth() * 0.5F;
        float centerY = resolution.getScaledHeight() * 0.5F;
        float[] shoulderOffset = cameraCrosshairOffset(event.getPartialTicks());
        if (shoulderOffset != null) {
            centerX += shoulderOffset[0];
            centerY += shoulderOffset[1];
        }
        drawSelectedCrosshair(resolution, centerX, centerY, event.getPartialTicks());
    }

    private static void drawFlightAimCrosshair(ScaledResolution resolution, float partialTicks) {
        float centerX = resolution.getScaledWidth() * 0.5F;
        float centerY = resolution.getScaledHeight() * 0.5F;
        float[] projected = flightAimCrosshairOffset(partialTicks);
        if (projected == null) {
            if (CameraRuntime.isFreeLookActive() || CameraRuntime.isShoulderActive()
                    || ShoulderSurfingCompat.isActive()) return;
        } else {
            centerX += projected[0];
            centerY += projected[1];
        }
        if (CrosshairConfig.customEnabled) {
            drawSelectedCrosshair(resolution, centerX, centerY, partialTicks);
        } else {
            drawVanillaCrosshair(resolution, centerX, centerY, partialTicks, false);
        }
    }

    private static void drawSelectedCrosshair(ScaledResolution resolution,
                                              float centerX, float centerY,
                                              float partialTicks) {
        boolean debug = "debug".equals(CrosshairConfig.style)
                || (CrosshairConfig.keepDebugCrosshair && MC.gameSettings.showDebugInfo);
        if (debug) {
            drawVanillaCrosshair(resolution, centerX, centerY, partialTicks, true);
            return;
        }
        draw(centerX, centerY, partialTicks);
    }

    private static void drawVanillaCrosshair(ScaledResolution resolution,
                                             float centerX, float centerY,
                                             float partialTicks, boolean forceDebug) {
        boolean previousDebug = MC.gameSettings.showDebugInfo;
        int previousPerspective = MC.gameSettings.thirdPersonView;
        if (forceDebug) MC.gameSettings.showDebugInfo = true;
        MC.gameSettings.thirdPersonView = 0;
        try {
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(centerX - resolution.getScaledWidth() * 0.5F,
                        centerY - resolution.getScaledHeight() * 0.5F, 0.0F);
                ((InvokerGuiIngameCrosshair) MC.ingameGUI)
                        .nfrUi$renderVanillaCrosshair(partialTicks, resolution);
            } finally {
                GlStateManager.popMatrix();
            }
        } finally {
            MC.gameSettings.thirdPersonView = previousPerspective;
            MC.gameSettings.showDebugInfo = previousDebug;
        }
    }

    /** Orange bracket marker for the camera-origin block/entity interaction ray in dual mode. */
    private static void drawBlockInteractionCursor(ScaledResolution resolution, float[] offset) {
        float cx = resolution.getScaledWidth() * 0.5F;
        float cy = resolution.getScaledHeight() * 0.5F;
        if (offset != null) { cx += offset[0]; cy += offset[1]; }
        GlSnapshot state = new GlSnapshot();
        try {
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(cx, cy, 0.0F);
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();
                GlStateManager.disableTexture2D();
                drawBlockInteractionGlyph(0xD0000000, 3.0F);
                drawBlockInteractionGlyph(0xFFFFA52F, 1.25F);
            } finally {
                GlStateManager.popMatrix();
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

    /** Additive camera-origin reticle shown even when the normal UIE crosshair is disabled. */
    private static void drawDroneCameraCursor(ScaledResolution resolution) {
        float cx = resolution.getScaledWidth() * 0.5F;
        float cy = resolution.getScaledHeight() * 0.5F;
        GlSnapshot state = new GlSnapshot();
        try {
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(cx, cy, 0.0F);
                GlStateManager.disableDepth();
                GlStateManager.disableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
                int color = 0xB040E9FF;
                float outer = 13.0F;
                float inner = 7.0F;
                line(-outer, -outer, -inner, -outer, color, 1.25F);
                line(-outer, -outer, -outer, -inner, color, 1.25F);
                line(inner, -outer, outer, -outer, color, 1.25F);
                line(outer, -outer, outer, -inner, color, 1.25F);
                line(-outer, inner, -outer, outer, color, 1.25F);
                line(-outer, outer, -inner, outer, color, 1.25F);
                line(outer, inner, outer, outer, color, 1.25F);
                line(inner, outer, outer, outer, color, 1.25F);
                quad(-1.0F, -1.0F, 1.0F, 1.0F, color);
            } finally {
                GlStateManager.popMatrix();
            }
        } finally {
            state.restore();
        }
    }

    /** Used by the Forge-GUI mixin; deliberately does not cancel the CROSSHAIRS event. */
    public static boolean suppressVanillaCrosshair() {
        return CrosshairConfig.customEnabled || FlightRollController.suppressVanillaCrosshair();
    }

    public static boolean preferModCrosshairs() {
        return CrosshairConfig.customEnabled && CrosshairConfig.preferModCrosshair;
    }

    /** Unified offset consumed by UIE, vanilla and supported mod crosshair renderers. */
    public static float[] cameraCrosshairOffset(float partialTicks) {
        float[] offset = ShoulderSurfingCompat.crosshairOffset();
        return offset != null ? offset : CameraRuntime.shoulderCrosshairOffset(partialTicks);
    }

    private static float[] flightAimCrosshairOffset(float partialTicks) {
        float[] offset = ShoulderSurfingCompat.crosshairOffset();
        return offset != null ? offset : CameraRuntime.playerAimCrosshairOffset(partialTicks);
    }

    /** Unified visibility decision for the active built-in camera mode. */
    public static boolean cameraCrosshairVisible(float partialTicks) {
        return CameraRuntime.shoulderCrosshairVisible(partialTicks);
    }

    /** Offset for optional item-mod renderers, sourced from either legacy or built-in Shoulder. */
    public static float[] preferredModCrosshairOffset(float partialTicks) {
        if (!ModCrosshairRouting.shouldOffset(CrosshairConfig.customEnabled,
                CrosshairConfig.preferModCrosshair)) return null;
        return cameraCrosshairOffset(partialTicks);
    }

    private static boolean overridesOrdinaryThirdPersonVisibility() {
        return CrosshairConfig.customEnabled && CrosshairConfig.visibleInThirdPerson
                && MC.gameSettings.thirdPersonView != 0 && !ShoulderSurfingCompat.isActive();
    }

    private static boolean isVisible() { return isVisible(false); }

    private static boolean isVisible(boolean ignoreThirdPerson) {
        EntityPlayer player = MC.player;
        if (player == null || !CrosshairConfig.visibleByDefault) return false;
        if (!CrosshairConfig.visibleWithHiddenGui && MC.gameSettings.hideGUI) return false;
        if (!CrosshairConfig.visibleInDebug && MC.gameSettings.showDebugInfo) return false;
        if (!CrosshairConfig.visibleAsSpectator && player.isSpectator()) return false;
        if (!ignoreThirdPerson && !CrosshairConfig.visibleInThirdPerson
                && MC.gameSettings.thirdPersonView != 0
                && !ShoulderSurfingCompat.isActive()) return false;
        if (!CrosshairConfig.visibleHoldingRanged && holdsRanged(player)) return false;
        if (!CrosshairConfig.visibleHoldingThrowable && holdsThrowable(player)) return false;
        return CrosshairConfig.visibleUsingSpyglass || !usingSpyglass(player);
    }

    private static boolean holdsPlayerAimItem(EntityPlayer player) {
        if (player == null) return false;
        if (player.isHandActive()
                && BackportCrosshairCompat.usesPlayerAim(player.getActiveItemStack())) return true;
        ItemStack main = player.getHeldItemMainhand();
        if (BackportCrosshairCompat.usesPlayerAim(main)) return true;
        return main.isEmpty()
                && BackportCrosshairCompat.usesPlayerAim(player.getHeldItemOffhand());
    }

    private static boolean holdsThrowable(EntityPlayer player) {
        ItemStack main = player.getHeldItemMainhand();
        ItemStack stack = main.isEmpty() ? player.getHeldItemOffhand() : main;
        Item item = stack.getItem();
        return item == Items.ENDER_PEARL || item == Items.ENDER_EYE || item == Items.SNOWBALL
                || item == Items.EGG || item == Items.EXPERIENCE_BOTTLE
                || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION
                || BackportCrosshairCompat.isTrident(stack);
    }

    private static boolean holdsRanged(EntityPlayer player) {
        ItemStack main = player.getHeldItemMainhand();
        ItemStack off = player.getHeldItemOffhand();
        return BackportCrosshairCompat.isRangedWeapon(main)
                || (main.isEmpty() && BackportCrosshairCompat.isRangedWeapon(off));
    }

    private static boolean usingSpyglass(EntityPlayer player) {
        return ZoomModule.isZoomActive() || (player.isHandActive()
                && BackportCrosshairCompat.isSpyglass(player.getActiveItemStack()));
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
            GlStateManager.pushMatrix();
            try {
                GlStateManager.translate(cx, cy, 0.0F);
                GlStateManager.rotate(CrosshairConfig.rotation, 0.0F, 0.0F, 1.0F);
                float scale = CrosshairConfig.scalePercent / 100.0F;
                GlStateManager.scale(scale, scale, 1.0F);
                GlStateManager.disableDepth();
                GlStateManager.enableBlend();

                drawAttackIndicator(partialTicks, target);
                GlStateManager.disableTexture2D();

                if (CrosshairConfig.itemCooldownEnabled) drawCooldownRings(gap, partialTicks);
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
                GlStateManager.popMatrix();
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
        EntityPlayer player = MC.player;
        if (player == null || player.isSpectator()) return base;
        if (CrosshairConfig.dynamicBow && player.isHandActive()
                && BackportCrosshairCompat.isChargeWeapon(player.getActiveItemStack())) {
            int used = player.getItemInUseMaxCount();
            float progress = BackportCrosshairCompat.chargeProgress(player.getActiveItemStack(), player, used);
            return base + Math.round((1.0F - progress) * 40.0F);
        }
        if (CrosshairConfig.dynamicAttack && player.getCooldownPeriod() > 5.0F) {
            float progress = player.getCooledAttackStrength(partialTicks);
            if (progress < 1.0F) return base + Math.round((1.0F - progress) * 40.0F);
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

    /** Minecraft 1.12.2 equivalent of CCM's Player#canAttack(LivingEntity) guard. */
    private static boolean canAttack(EntityLivingBase target) {
        EntityPlayer player = MC.player;
        if (player == null || target == player || !target.canBeAttackedWithItem()) return false;
        if (target instanceof EntityPlayer) return player.canAttackPlayer((EntityPlayer) target);
        return !player.isOnSameTeam(target);
    }

    private static Entity targetEntity(float partialTicks) {
        RayTraceResult hit = MC.objectMouseOver;
        Entity fallback = hit == null ? null : hit.entityHit;
        return ShoulderSurfingCompat.crosshairTarget(partialTicks, fallback);
    }

    private static void drawStyle(String style, int gap, int color, float thickness, float expansion) {
        float w = CrosshairConfig.width + expansion;
        float h = CrosshairConfig.height + expansion;
        float g = Math.max(0.0F, gap - expansion * 0.5F);
        switch (style) {
            case "vanilla":
                GlStateManager.enableTexture2D();
                MC.getTextureManager().bindTexture(Gui.ICONS);
                setBlend(true);
                Gui.drawModalRectWithCustomSizedTexture(-7, -7, 0, 0, 15, 15, 256, 256);
                GlStateManager.disableTexture2D();
                break;
            case "vanilla_plus":
                GlStateManager.enableTexture2D();
                MC.getTextureManager().bindTexture(Gui.ICONS);
                setBlend(true);
                Gui.drawModalRectWithCustomSizedTexture(-7, -7, 0, 0, 15, 15, 256, 256);
                GlStateManager.disableTexture2D();
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
        EntityPlayer player = MC.player;
        if (player == null || !player.isHandActive()) return;
        ItemStack active = player.getActiveItemStack();
        if (!BackportCrosshairCompat.isChargeWeapon(active)) return;
        int used = player.getItemInUseMaxCount();
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

    private static void drawAttackIndicator(float partialTicks, Entity target) {
        if (MC.gameSettings.attackIndicator != 1 || MC.player == null) return;
        float cooldown = MC.player.getCooledAttackStrength(partialTicks);
        boolean readyTarget = target instanceof EntityLivingBase && cooldown >= 1.0F
                && MC.player.getCooldownPeriod() > 5.0F && target.isEntityAlive();
        GlStateManager.enableTexture2D();
        MC.getTextureManager().bindTexture(Gui.ICONS);
        setBlend(true);
        if (readyTarget) {
            Gui.drawModalRectWithCustomSizedTexture(-8, 9, 68, 94, 16, 16, 256, 256);
        } else if (cooldown < 1.0F) {
            int progress = (int) (cooldown * 17.0F);
            Gui.drawModalRectWithCustomSizedTexture(-8, 9, 36, 94, 16, 4, 256, 256);
            Gui.drawModalRectWithCustomSizedTexture(-8, 9, 52, 94, progress, 4, 256, 256);
        }
    }

    private static void drawCooldownRings(int gap, float partialTicks) {
        int radius = gap + Math.max(CrosshairConfig.width, CrosshairConfig.height) + 3;
        for (Item item : new Item[]{Items.ENDER_PEARL, Items.CHORUS_FRUIT}) {
            float cooldown = MC.player.getCooldownTracker().getCooldown(item, partialTicks);
            if (cooldown > 0.0F) {
                partialCircle(0, 0, radius, 0, Math.round(360.0F * (1.0F - cooldown)),
                        CrosshairConfig.itemCooldownColor, 2.0F);
                radius += 3;
            }
        }
    }

    private static void drawIndicators(float cx, float cy) {
        if (MC.player == null) return;
        ItemStack tool = MC.player.getHeldItemMainhand();
        int x = Math.round(cx) + CrosshairConfig.gap + 6;
        int y = Math.round(cy) + CrosshairConfig.gap + 6;
        if (CrosshairConfig.toolDamageEnabled && !tool.isEmpty() && tool.isItemStackDamageable()) {
            int remaining = tool.getMaxDamage() - tool.getItemDamage();
            if (remaining <= 10) { drawIndicator(tool, Integer.toString(remaining), x, y); x += 15; }
        }
        if (CrosshairConfig.projectileIndicatorEnabled
                && (tool.getItem() instanceof ItemBow || BackportCrosshairCompat.isCrossbow(tool))) {
            ItemStack ammo = tool.getItem() instanceof ItemBow ? findVanillaArrow()
                    : BackportCrosshairCompat.findProjectile(tool, MC.player);
            int count = BackportCrosshairCompat.projectileCount(ammo);
            if (!ammo.isEmpty()) drawIndicator(ammo,
                    MC.player.capabilities.isCreativeMode ? "" : Integer.toString(count), x, y);
        }
    }

    private static ItemStack findVanillaArrow() {
        ItemStack off = MC.player.getHeldItem(EnumHand.OFF_HAND);
        if (off.getItem() instanceof ItemArrow) return off;
        ItemStack main = MC.player.getHeldItem(EnumHand.MAIN_HAND);
        if (main.getItem() instanceof ItemArrow) return main;
        for (int i = 0; i < MC.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = MC.player.inventory.getStackInSlot(i);
            if (stack.getItem() instanceof ItemArrow) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static void drawIndicator(ItemStack stack, String text, int x, int y) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.scale(0.5F, 0.5F, 1.0F);
            MC.getRenderItem().renderItemAndEffectIntoGUI(stack, x * 2 - 8, y * 2 - 8);
        } finally { GlStateManager.popMatrix(); }
        if (!text.isEmpty()) MC.fontRenderer.drawStringWithShadow(text, x + 5, y, 0xFFFFFFFF);
    }

    private static void setBlend(boolean adaptive) {
        GlStateManager.enableBlend();
        if (adaptive) GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        else GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
    }

    private static void circle(float cx, float cy, float radius, int color, float width) {
        partialCircle(cx, cy, radius, 0, 360, color, width);
    }

    private static void partialCircle(float cx, float cy, float radius, int start, int end, int color, float width) {
        GL11.glLineWidth(Math.max(1.0F, width));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = start; i <= end; i += 3) {
            double angle = Math.PI * 2.0D * i / 360.0D - Math.PI / 2.0D;
            vertex(buffer, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, color);
        }
        tessellator.draw();
    }

    private static void filledCircle(float cx, float cy, float radius, int color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, cx, cy, color);
        for (int i = 0; i <= 32; i++) {
            double angle = Math.PI * 2.0D * i / 32.0D;
            vertex(buffer, cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, color);
        }
        tessellator.draw();
    }

    private static void line(float x1, float y1, float x2, float y2, int color, float width) {
        GL11.glLineWidth(Math.max(1.0F, width));
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, x1, y1, color); vertex(buffer, x2, y2, color);
        tessellator.draw();
    }

    private static void quad(float left, float top, float right, float bottom, int color) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        vertex(buffer, left, top, color); vertex(buffer, left, bottom, color);
        vertex(buffer, right, bottom, color); vertex(buffer, right, top, color);
        tessellator.draw();
    }

    private static void vertex(BufferBuilder buffer, float x, float y, int color) {
        buffer.pos(x, y, 0.0D).color(Color.red(color), Color.green(color), Color.blue(color), Color.alpha(color)).endVertex();
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
            GlStateManager.bindTexture(textureBinding);
            if (texture) GlStateManager.enableTexture2D(); else GlStateManager.disableTexture2D();
            if (depth) GlStateManager.enableDepth(); else GlStateManager.disableDepth();
            if (blend) GlStateManager.enableBlend(); else GlStateManager.disableBlend();
        }
    }
}
