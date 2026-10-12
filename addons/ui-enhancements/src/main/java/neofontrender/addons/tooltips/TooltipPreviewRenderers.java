package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import com.mojang.authlib.GameProfile;

/**
 * Built-in preview backends. The implementation owns all Minecraft render state.
 *
 * <p>1.7.10 differences from the 1.12.2 branch:</p>
 * <ul>
 *   <li>There is no {@code EntityArmorStand}. Both {@link NfrTooltipApi.ArmorPreviewModel}
 *       values render the humanoid player model through {@link EntityOtherPlayerMP}, the only
 *       registered living-model render path on this platform, so the stand-only knobs (base
 *       plate, gravity, name tag) have no counterpart.</li>
 *   <li>Equipment uses {@link NfrTooltipApi.ArmorSlot} indices through
 *       {@code getEquipmentInSlot(int)} and {@code setCurrentItemOrArmor(int, ItemStack)}; an
 *       absent stack is {@code null}.</li>
 *   <li>Item previews are drawn with {@link RenderItem}'s GUI path inside the preview matrix
 *       because 1.7.10 has neither baked-model camera transforms nor
 *       {@code ForgeHooksClient#multiplyCurrentGlMatrix}.</li>
 *   <li>There is no {@code GlStateManager}; GL state is manipulated with LWJGL directly.</li>
 * </ul>
 */
final class TooltipPreviewRenderers {
    private static final long ANIMATION_SESSION_GAP_NANOS = 500_000_000L;
    private static final long ROTATION_WINDOW_NANOS = 3_600_000_000_000L;
    /** Vertical offset that moves the render-only entity outside the 64-block name tag range. */
    private static final double LABEL_FREE_DISTANCE = 512.0D;
    private static final NfrTooltipApi.ArmorSlot[] ARMOR_SLOTS = {
            NfrTooltipApi.ArmorSlot.HEAD, NfrTooltipApi.ArmorSlot.CHEST,
            NfrTooltipApi.ArmorSlot.LEGS, NfrTooltipApi.ArmorSlot.FEET
    };
    private static final Map<String, Long> ANIMATION_STARTS = new HashMap<>();
    private static final Map<String, Long> ANIMATION_LAST_SEEN = new HashMap<>();
    private static final Map<String, Long> LAST_SOUND = new HashMap<>();
    private static final ItemZoomAnimation ZOOM_ANIMATION = new ItemZoomAnimation();
    /** Preview entities are render-only and are reused between frames to preserve model state. */
    private static World previewWorld;
    private static EntityOtherPlayerMP previewEntity;
    private static java.util.UUID previewId;
    private static boolean initialized;

    private TooltipPreviewRenderers() {}

    static synchronized void initialize() {
        if (initialized) return;
        PreviewEffects.registerBuiltins();
        register(new ItemRenderer());
        register(new ArmorRenderer());
        initialized = true;
    }

    static void register(NfrTooltipApi.PreviewRenderer renderer) {
        NfrTooltipApi.PreviewRegistry.register(renderer);
    }

    static void releaseWorld(World world) {
        PreviewBoundsMeasurement.clear();
        ZOOM_ANIMATION.reset();
        if (previewWorld == world) {
            previewEntity = null;
            previewWorld = null;
            previewId = null;
        }
    }

    static NfrTooltipApi.PreviewRenderer find(NfrTooltipApi.PreviewRequest request) {
        initialize();
        return NfrTooltipApi.PreviewRegistry.find(request);
    }

    static void render(NfrTooltipApi.PreviewNode node, int x, int y, FontRenderer font) {
        if (node == null || node.request() == null) return;
        NfrTooltipApi.PreviewRenderer renderer = find(node.request());
        if (renderer == null) return;
        try {
            NfrTooltipApi.PreviewSize measured = renderer.measure(node.request(), font);
            int width = node.width(font) > 0 ? node.width(font) : measured.width();
            int height = node.height(font) > 0 ? node.height(font) : measured.height();
            NfrTooltipApi.PreviewSize size = new NfrTooltipApi.PreviewSize(width, height);
            renderer.render(node.request(), x, y, size, font);
        } catch (RuntimeException | LinkageError ignored) {
            // Optional model renderers must never break the surrounding tooltip.
        }
    }

    private static float animationProgress(NfrTooltipApi.PreviewRequest request, ItemStack stack) {
        if (stack == null) return 1.0F;
        String key = animationKey(request, stack);
        return animationProgress(key, TooltipConfig.previewAnimationEnabled,
                TooltipConfig.previewAnimationMillis, request == null ? null : request.sound(), true);
    }

    static float headerAnimationProgress(ItemStack stack) {
        if (stack == null) return 1.0F;
        StringBuilder key = new StringBuilder("header_icon");
        appendStackIdentity(key, stack);
        return animationProgress(key.toString(), TooltipConfig.headerIconAnimationEnabled,
                TooltipConfig.headerIconAnimationMillis, null, false);
    }

    /** Appearance timeline used by the standalone item zoom overlay. */
    static float itemZoomAnimationProgress(ItemStack stack) {
        if (stack == null) return 1.0F;
        StringBuilder key = new StringBuilder("item_zoom");
        appendStackIdentity(key, stack);
        return ZOOM_ANIMATION.progress(key.toString(), System.nanoTime(), TooltipConfig.zoomOverlayAnimation,
                TooltipConfig.zoomOverlayAnimationMillis, TooltipConfig.zoomOverlayAnimationSwitch);
    }

    static void resetItemZoomAnimation() {
        ZOOM_ANIMATION.reset();
    }

    static void itemZoomRendered() {
        ZOOM_ANIMATION.rendered(System.nanoTime());
    }

    private static float animationProgress(String key, boolean enabled, int durationMillis,
                                           NfrTooltipApi.PreviewSound sound,
                                           boolean playSound) {
        long now = System.nanoTime();
        boolean newSession = false;
        synchronized (ANIMATION_STARTS) {
            Long start = ANIMATION_STARTS.get(key);
            Long lastSeen = ANIMATION_LAST_SEEN.get(key);
            if (start == null || lastSeen == null
                    || now - lastSeen > ANIMATION_SESSION_GAP_NANOS) {
                if (ANIMATION_STARTS.size() > 256) {
                    ANIMATION_STARTS.clear();
                    ANIMATION_LAST_SEEN.clear();
                }
                ANIMATION_STARTS.put(key, now);
                start = now;
                newSession = true;
            }
            ANIMATION_LAST_SEEN.put(key, now);
            if (newSession && playSound) playPreviewSound(key, sound);
            if (!enabled) return 1.0F;
            long duration = Math.max(1L, durationMillis) * 1_000_000L;
            float t = Math.max(0.0F, Math.min(1.0F, (now - start) / (float) duration));
            return t * t * (3.0F - 2.0F * t);
        }
    }

    static float rotationAngle(long timeNanos, float degreesPerSecond) {
        if (!Float.isFinite(degreesPerSecond) || degreesPerSecond == 0.0F) return 0.0F;
        long cycleTime = Math.floorMod(timeNanos, ROTATION_WINDOW_NANOS);
        double degrees = cycleTime / 1_000_000_000.0D * degreesPerSecond;
        return (float) (degrees % 360.0D);
    }

    static void preparePreviewDepthLayer() {
        // Container and ingredient renderers leave depth values behind even after disabling the
        // depth test. A model preview needs a fresh buffer so those earlier items cannot punch
        // holes through it, while retaining normal self-occlusion within the preview itself.
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glClearDepth(1.0D);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    static String animationKey(NfrTooltipApi.PreviewRequest request, ItemStack stack) {
        StringBuilder key = new StringBuilder(128);
        if (request != null) {
            key.append(request.rendererId()).append('|').append(request.previewKind());
        }
        appendStackIdentity(key, stack);
        if (request instanceof NfrTooltipApi.ArmorPreviewRequest) {
            NfrTooltipApi.ArmorPreviewRequest armor =
                    (NfrTooltipApi.ArmorPreviewRequest) request;
            key.append('|').append(armor.model()).append('|').append(armor.mode())
                    .append('|').append(armor.slot());
            for (ItemStack equipment : armor.equipment()) appendStackIdentity(key, equipment);
        }
        return key.toString();
    }

    private static void appendStackIdentity(StringBuilder key, ItemStack stack) {
        key.append('|');
        if (stack == null) {
            key.append("empty");
            return;
        }
        try {
            NBTTagCompound identity = stack.writeToNBT(new NBTTagCompound());
            identity.removeTag("Count");
            key.append(identity);
        } catch (RuntimeException | LinkageError ignored) {
            try {
                // 1.7.10 registers items in Item.itemRegistry; Item itself has no registry name.
                key.append(Item.itemRegistry.getNameForObject(stack.getItem())).append(':')
                        .append(stack.getItemDamage());
            } catch (RuntimeException | LinkageError fallback) {
                key.append(stack.getItem().getClass().getName());
            }
        }
    }

    private static void playPreviewSound(String key, NfrTooltipApi.PreviewSound override) {
        boolean enabled = override == null ? TooltipConfig.previewSoundEnabled : override.enabled();
        if (!enabled) return;
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            long now = System.nanoTime();
            int cooldown = override == null ? TooltipConfig.previewSoundCooldownMillis
                    : override.cooldownMillis();
            synchronized (LAST_SOUND) {
                Long last = LAST_SOUND.get(key);
                if (last != null && now - last < cooldown * 1_000_000L) return;
                if (LAST_SOUND.size() > 256) LAST_SOUND.clear();
                LAST_SOUND.put(key, now);
            }
            if (minecraft.getSoundHandler() == null) return;
            String eventId = override == null ? TooltipConfig.previewSoundEvent : override.eventId();
            if (eventId.isEmpty()) return;
            // 1.7.10 has no SoundEvent registry, so the configured id is the sound resource
            // itself (for example "gui.button.press") and the vanilla UI factory owns the
            // volume and the NONE attenuation.
            ResourceLocation id = new ResourceLocation(eventId);
            minecraft.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(id,
                    override == null ? TooltipConfig.previewSoundPitch : override.pitch()));
        } catch (RuntimeException ignored) {
            // Sound is cosmetic and must never affect tooltip rendering.
        }
    }

    private static final class ItemRenderer implements NfrTooltipApi.PreviewRenderer {
        @Override public NfrTooltipApi.PreviewKind previewKind() {
            return NfrTooltipApi.PreviewKind.ITEM_STACK;
        }

        @Override public NfrTooltipApi.PreviewSize measure(NfrTooltipApi.PreviewRequest request,
                                                            FontRenderer font) {
            if (request instanceof NfrTooltipApi.ItemPreviewRequest) {
                NfrTooltipApi.ItemPreviewRequest item = (NfrTooltipApi.ItemPreviewRequest) request;
                PreviewModelBounds bounds = TooltipConfig.previewMeasureBounds
                        ? PreviewBoundsMeasurement.measure(item) : null;
                if (bounds != null) return new NfrTooltipApi.PreviewSize(bounds.width(), bounds.height());
                return new NfrTooltipApi.PreviewSize(item.width(), item.height());
            }
            return new NfrTooltipApi.PreviewSize(30, 64);
        }

        @Override public void render(NfrTooltipApi.PreviewRequest value, int x, int y,
                                     NfrTooltipApi.PreviewSize size, FontRenderer font) {
            if (!(value instanceof NfrTooltipApi.ItemPreviewRequest)) return;
            NfrTooltipApi.ItemPreviewRequest request = (NfrTooltipApi.ItemPreviewRequest) value;
            renderPreview(request, x, y, size);
        }

        private static void renderModel(NfrTooltipApi.ItemPreviewRequest request, int x, int y,
                                        NfrTooltipApi.PreviewSize size, float animation, float spin) {
            ItemStack stack = request.stack();
            if (stack == null) return;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            try {
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                preparePreviewDepthLayer();
                GL11.glColor4f(1.0F, 1.0F, 1.0F, animation);
                RenderHelper.enableGUIStandardItemLighting();
                float centerX = x + size.width() * 0.46F;
                float centerY = y + size.height() * 0.52F;
                GL11.glTranslatef(centerX, centerY, 500.0F);
                GL11.glScalef(animation, animation, animation);
                // The 1.12.2 branch multiplies a baked-model camera transform here; 1.7.10
                // owns no such matrix, so the same pitch/spin/roll is applied to the GUI item
                // model around the preview centre.
                GL11.glRotatef(request.pitch(), 1.0F, 0.0F, 0.0F);
                GL11.glRotatef(spin, 0.0F, 1.0F, 0.0F);
                GL11.glRotatef(request.roll(), 0.0F, 0.0F, 1.0F);
                GL11.glScalef(request.scale(), request.scale(), request.scale());
                renderItemModel(stack);
            } finally {
                RenderHelper.disableStandardItemLighting();
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
        }
    }

    private static void renderPreview(NfrTooltipApi.PreviewRequest request, int x, int y,
                                      NfrTooltipApi.PreviewSize size) {
        ItemStack stack = request instanceof NfrTooltipApi.ItemPreviewRequest
                ? ((NfrTooltipApi.ItemPreviewRequest) request).stack()
                : ((NfrTooltipApi.ArmorPreviewRequest) request).stack();
        float animation = animationProgress(request, stack);
        float spin = rotationAngle(System.nanoTime(), PreviewBoundsMeasurement.rotationSpeed(request));
        float swing = currentSwing();
        if (!TooltipConfig.previewMeasureBounds
                || !PreviewBoundsMeasurement.renderTooltip(request, x, y, size, animation, spin, swing)) {
            renderRaw(request, x, y, size, animation, spin, swing);
        }
        PreviewEffects.render(request, x, y, size, animation);
    }

    static float currentSwing() {
        return "swing".equalsIgnoreCase(TooltipConfig.armorPlayerPose)
                ? 0.5F + 0.5F * (float) Math.sin(System.nanoTime() / 300_000_000.0D) : 0;
    }

    /** No appearance clock, sounds, effects or measuring: also used by offscreen probes. */
    static void renderRaw(NfrTooltipApi.PreviewRequest request, int x, int y,
                          NfrTooltipApi.PreviewSize size, float animation, float spin, float swing) {
        if (request instanceof NfrTooltipApi.ItemPreviewRequest) {
            ItemRenderer.renderModel((NfrTooltipApi.ItemPreviewRequest) request, x, y, size, animation, spin);
        } else if (request instanceof NfrTooltipApi.ArmorPreviewRequest) {
            ArmorRenderer.renderModel((NfrTooltipApi.ArmorPreviewRequest) request, x, y, size, animation, spin, swing);
        }
    }

    /**
     * 1.7.10's widest generated item model is the 16x16 GUI sprite, and the shield whose
     * oversize clamp the 1.12.2 branch applies does not exist here, so the requested roll is
     * kept unchanged. The entry point is retained because the item zoom overlay shares it.
     */
    static float itemModelRoll(ItemStack stack, float requestedRoll) {
        return requestedRoll;
    }

    /** Draws the item at the current matrix origin, centred like a baked model would be. */
    static void renderItemModel(ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        RenderItem renderer = RenderItem.getInstance();
        if (renderer == null) return;
        // RenderItem anchors a 16x16 sprite at its top-left corner.
        GL11.glTranslatef(-8.0F, -8.0F, 0.0F);
        renderer.renderItemIntoGUI(minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0);
    }

    /** Reuses the equipment backend without the tooltip's appearance timeline or sound. */
    static void renderZoomArmor(NfrTooltipApi.ArmorPreviewRequest request, int size) {
        if (request == null) return;
        PreviewModelBounds bounds = TooltipConfig.zoomOverlayMeasureBounds
                ? PreviewBoundsMeasurement.measure(request) : null;
        GL11.glPushMatrix();
        try {
            if (bounds != null) PreviewBoundsMeasurement.applyFit(bounds, size, size, Math.max(2, size / 25));
            ArmorRenderer.renderModel(request, 0, 0, new NfrTooltipApi.PreviewSize(size, size), 1,
                    rotationAngle(System.nanoTime(), request.rotationSpeed()), currentSwing());
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static final class ArmorRenderer implements NfrTooltipApi.PreviewRenderer {
        @Override public NfrTooltipApi.PreviewKind previewKind() {
            return NfrTooltipApi.PreviewKind.ARMOR;
        }

        @Override public NfrTooltipApi.PreviewSize measure(NfrTooltipApi.PreviewRequest request,
                                                            FontRenderer font) {
            if (request instanceof NfrTooltipApi.ArmorPreviewRequest) {
                NfrTooltipApi.ArmorPreviewRequest armor = (NfrTooltipApi.ArmorPreviewRequest) request;
                PreviewModelBounds bounds = TooltipConfig.previewMeasureBounds
                        ? PreviewBoundsMeasurement.measure(armor) : null;
                if (bounds != null) return new NfrTooltipApi.PreviewSize(bounds.width(), bounds.height());
                return new NfrTooltipApi.PreviewSize(armor.width(), armor.height());
            }
            return new NfrTooltipApi.PreviewSize(40, 64);
        }

        @Override public void render(NfrTooltipApi.PreviewRequest value, int x, int y,
                                     NfrTooltipApi.PreviewSize size, FontRenderer font) {
            if (!(value instanceof NfrTooltipApi.ArmorPreviewRequest)) return;
            NfrTooltipApi.ArmorPreviewRequest request = (NfrTooltipApi.ArmorPreviewRequest) value;
            renderPreview(request, x, y, size);
        }

        private static void renderModel(NfrTooltipApi.ArmorPreviewRequest request, int x, int y,
                                        NfrTooltipApi.PreviewSize size, float animation, float spin, float swing) {
            Minecraft minecraft = Minecraft.getMinecraft();
            World world = minecraft.theWorld;
            EntityPlayer source = minecraft.thePlayer;
            if (world == null || source == null || !hasEquipment(request)) return;
            EntityOtherPlayerMP entity = getPreviewEntity(world, source.getGameProfile());
            applyArmorEquipment(entity, request);
            // The 1.12.2 branch renders an armor stand for ArmorPreviewModel.ARMOR_STAND; 1.7.10
            // has no such entity, so both model choices use the humanoid player model, which
            // wears the same four armor layers through the same equipment slots.
            entity.setInvisible(false);
            entity.setSneaking(TooltipConfig.armorPlayerSneaking);
            // 1.7.10 renders a living entity's name tag (and scoreboard label) whenever the
            // entity is within 64 blocks of the viewer, and it has no "always render name tag"
            // flag to clear. The model is drawn at explicit coordinates, so parking the preview
            // entity high above the player only removes those labels.
            entity.setPositionAndRotation(source.posX, source.posY + LABEL_FREE_DISTANCE,
                    source.posZ, source.rotationYaw, 0);
            entity.prevRotationPitch = 0;
            applyHeldItem(entity, request, source, swing);
            renderLiving(entity, request, x, y, size, animation, spin);
        }

        private static boolean hasEquipment(NfrTooltipApi.ArmorPreviewRequest request) {
            for (ItemStack stack : request.equipment()) {
                if (stack != null) return true;
            }
            return false;
        }

        private static EntityOtherPlayerMP getPreviewEntity(World world, GameProfile profile) {
            java.util.UUID id = profile == null ? null : profile.getId();
            if (previewEntity == null || previewWorld != world
                    || (id != null && !id.equals(previewId))) {
                previewWorld = world;
                previewId = id;
                previewEntity = new EntityOtherPlayerMP(world, profile);
            }
            return previewEntity;
        }

        /**
         * The single hand slot is slot 0. 1.7.10 has no offhand, and the stand model never
         * carries the viewer's item, which is what the 1.12.2 armor-stand path implies.
         */
        private static void applyHeldItem(EntityOtherPlayerMP entity,
                                          NfrTooltipApi.ArmorPreviewRequest request,
                                          EntityPlayer source, float swing) {
            boolean copyHands = request.model() == NfrTooltipApi.ArmorPreviewModel.PLAYER
                    && TooltipConfig.armorPlayerCopyHands;
            ItemStack held = copyHands ? source.getHeldItem() : null;
            entity.setCurrentItemOrArmor(0, held == null ? null : held.copy());
            if (copyHands && "swing".equalsIgnoreCase(TooltipConfig.armorPlayerPose)) {
                entity.prevSwingProgress = swing;
                entity.swingProgress = swing;
                entity.isSwingInProgress = true;
            } else {
                entity.prevSwingProgress = 0.0F;
                entity.swingProgress = 0.0F;
                entity.isSwingInProgress = false;
            }
        }

        private static void applyArmorEquipment(EntityLivingBase entity,
                                                NfrTooltipApi.ArmorPreviewRequest request) {
            for (NfrTooltipApi.ArmorSlot slot : ARMOR_SLOTS) {
                ItemStack desired = null;
                if (request.mode() == NfrTooltipApi.ArmorPreviewMode.FULL_SET) {
                    for (ItemStack stack : request.equipment()) {
                        if (slot == PreviewEquipment.armorSlot(stack)) desired = stack;
                    }
                } else if (slot == request.slot()) {
                    desired = request.stack();
                }
                int index = slot.equipmentIndex();
                ItemStack current = entity.getEquipmentInSlot(index);
                if (!ItemStack.areItemStacksEqual(current, desired)) {
                    entity.setCurrentItemOrArmor(index, desired == null ? null : desired.copy());
                }
            }
        }

        private static void setPreviewYaw(EntityLivingBase entity, float yaw) {
            entity.rotationYaw = yaw;
            entity.prevRotationYaw = yaw;
            entity.rotationYawHead = yaw;
            entity.prevRotationYawHead = yaw;
            entity.renderYawOffset = yaw;
            entity.prevRenderYawOffset = yaw;
        }

        private static void renderLiving(EntityOtherPlayerMP entity,
                                         NfrTooltipApi.ArmorPreviewRequest request,
                                         int x, int y, NfrTooltipApi.PreviewSize size,
                                         float animation, float spin) {
            RenderManager dispatcher = RenderManager.instance;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushMatrix();
            boolean oldFancyGraphics = dispatcher.options.fancyGraphics;
            float oldPlayerViewY = dispatcher.playerViewY;
            try {
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                preparePreviewDepthLayer();
                GL11.glColor4f(1.0F, 1.0F, 1.0F, animation);
                RenderHelper.enableStandardItemLighting();
                setPreviewYaw(entity, 180.0F + spin);
                // Match the armor-stand anchor: the player entity origin is at its feet,
                // so fitting must see the whole model before it is centered in the cell.
                GL11.glTranslatef(x + size.width() * 0.46F, y + size.height(), 500.0F);
                GL11.glScalef(-request.scale() * animation, -request.scale() * animation,
                        request.scale() * animation);
                GL11.glRotatef(request.pitch(), 1.0F, 0.0F, 0.0F);
                // EntityRenderer.drawNameplate orients labels with these fields; the shadow is
                // part of doRenderShadowAndFire below.
                dispatcher.playerViewY = 180.0F;
                dispatcher.options.fancyGraphics = false;
                dispatcher.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, entity.rotationYaw, 1.0F);
            } finally {
                dispatcher.options.fancyGraphics = oldFancyGraphics;
                dispatcher.playerViewY = oldPlayerViewY;
                RenderHelper.disableStandardItemLighting();
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
                GL11.glPopMatrix();
                GL11.glPopAttrib();
            }
        }
    }
}
