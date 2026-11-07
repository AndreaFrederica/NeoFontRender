package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemShield;
import net.minecraft.world.World;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.EnumHand;
import neofontrender.api.client.tooltip.NfrTooltipApi;
import neofontrender.addons.mixin.InvokerEntityArmorStandPreview;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;
import com.mojang.authlib.GameProfile;

/** Built-in preview backends. The implementation owns all Minecraft render state. */
final class TooltipPreviewRenderers {
    private static final long ANIMATION_SESSION_GAP_NANOS = 500_000_000L;
    private static final long ROTATION_WINDOW_NANOS = 3_600_000_000_000L;
    private static final EntityEquipmentSlot[] ARMOR_SLOTS = {
            EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
            EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET
    };
    private static final Map<String, Long> ANIMATION_STARTS = new HashMap<>();
    private static final Map<String, Long> ANIMATION_LAST_SEEN = new HashMap<>();
    private static final Map<String, Long> LAST_SOUND = new HashMap<>();
    private static final ItemZoomAnimation ZOOM_ANIMATION = new ItemZoomAnimation();
    /** Preview entities are render-only and are reused between frames to preserve model state. */
    private static World armorStandWorld;
    private static EntityArmorStand armorStandPreview;
    private static World playerPreviewWorld;
    private static EntityOtherPlayerMP playerPreview;
    private static java.util.UUID playerPreviewId;
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
        if (armorStandWorld == world) {
            armorStandPreview = null;
            armorStandWorld = null;
        }
        if (playerPreviewWorld == world) {
            playerPreview = null;
            playerPreviewWorld = null;
            playerPreviewId = null;
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
        if (stack == null || stack.isEmpty()) return 1.0F;
        String key = animationKey(request, stack);
        return animationProgress(key, TooltipConfig.previewAnimationEnabled,
                TooltipConfig.previewAnimationMillis, request == null ? null : request.sound(), true);
    }

    static float headerAnimationProgress(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 1.0F;
        StringBuilder key = new StringBuilder("header_icon");
        appendStackIdentity(key, stack);
        return animationProgress(key.toString(), TooltipConfig.headerIconAnimationEnabled,
                TooltipConfig.headerIconAnimationMillis, null, false);
    }

    /** Appearance timeline used by the standalone item zoom overlay. */
    static float itemZoomAnimationProgress(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 1.0F;
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
        GlStateManager.depthMask(true);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glClearDepth(1.0D);
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.enableDepth();
    }

    /**
     * The vanilla GUI light is tuned for a 16px item. At Item Zoom sizes the same light
     * produces very dark faces, so use the 1.12 Item Zoom approach and scale the diffuse
     * component with the model size. The light setup is local to the caller's GL state.
     */
    static void enableZoomItemLighting(float modelScale) {
        final net.minecraft.util.math.Vec3d light0 =
                new net.minecraft.util.math.Vec3d(0.2D, 1.0D, -0.7D).normalize();
        final net.minecraft.util.math.Vec3d light1 =
                new net.minecraft.util.math.Vec3d(-0.2D, 1.0D, 0.7D).normalize();
        float strength = Math.max(0.3F, Math.min(3.0F, 0.3F * Math.abs(modelScale)));
        GlStateManager.pushMatrix();
        try {
            GlStateManager.rotate(-30.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(165.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.enableLighting();
            GlStateManager.enableLight(0);
            GlStateManager.enableLight(1);
            GlStateManager.enableColorMaterial();
            GlStateManager.colorMaterial(1032, 5634);
            GlStateManager.glLight(16384, 4611,
                    RenderHelper.setColorBuffer((float) light0.x, (float) light0.y,
                            (float) light0.z, 0.0F));
            GlStateManager.glLight(16384, 4609,
                    RenderHelper.setColorBuffer(strength, strength, strength, 1.0F));
            GlStateManager.glLight(16384, 4608,
                    RenderHelper.setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
            GlStateManager.glLight(16384, 4610,
                    RenderHelper.setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
            GlStateManager.glLight(16385, 4611,
                    RenderHelper.setColorBuffer((float) light1.x, (float) light1.y,
                            (float) light1.z, 0.0F));
            GlStateManager.glLight(16385, 4609,
                    RenderHelper.setColorBuffer(strength, strength, strength, 1.0F));
            GlStateManager.glLight(16385, 4608,
                    RenderHelper.setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
            GlStateManager.glLight(16385, 4610,
                    RenderHelper.setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
            GlStateManager.shadeModel(7424);
            GlStateManager.glLightModel(2899,
                    RenderHelper.setColorBuffer(0.4F, 0.4F, 0.4F, 1.0F));
        } finally {
            GlStateManager.popMatrix();
        }
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
        if (stack == null || stack.isEmpty()) {
            key.append("empty");
            return;
        }
        try {
            net.minecraft.nbt.NBTTagCompound identity = stack.serializeNBT();
            identity.removeTag("Count");
            key.append(identity);
        } catch (RuntimeException | LinkageError ignored) {
            try {
                key.append(stack.getItem().getRegistryName()).append(':').append(stack.getMetadata());
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
            ResourceLocation id = new ResourceLocation(eventId);
            SoundEvent sound = SoundEvent.REGISTRY.getObject(id);
            if (sound == null) return;
            minecraft.getSoundHandler().playSound(PositionedSoundRecord.getRecord(
                    sound, override == null ? TooltipConfig.previewSoundVolume : override.volume(),
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
            if (stack == null || stack.isEmpty()) return;
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                preparePreviewDepthLayer();
                GlStateManager.color(1.0F, 1.0F, 1.0F, animation);
                RenderHelper.enableGUIStandardItemLighting();
                float centerX = x + size.width()
                        * (stack.getItem() instanceof ItemShield ? 0.50F : 0.46F);
                float centerY = y + size.height() * 0.52F;
                GlStateManager.translate(centerX, centerY, 500.0F);
                GlStateManager.scale(animation, animation, animation);
                net.minecraftforge.client.ForgeHooksClient.multiplyCurrentGlMatrix(itemModelTransform(request, spin));
                renderItemModel(stack);
            } finally {
                RenderHelper.disableStandardItemLighting();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
                GL11.glPopAttrib();
            }
        }
    }

    /** The measurement and actual item drawing use exactly the same model-space transform. */
    static javax.vecmath.Matrix4f itemModelTransform(NfrTooltipApi.ItemPreviewRequest request, float spin) {
        javax.vecmath.Matrix4f matrix = new javax.vecmath.Matrix4f();
        matrix.set(itemModelScale(request.stack(), request.scale()));
        javax.vecmath.Matrix4f rotation = new javax.vecmath.Matrix4f();
        rotation.rotX((float) Math.toRadians(request.pitch())); matrix.mul(rotation);
        rotation.rotY((float) Math.toRadians(spin)); matrix.mul(rotation);
        rotation.rotZ((float) Math.toRadians(itemModelRoll(request.stack(), request.roll()))); matrix.mul(rotation);
        rotation.setIdentity();
        rotation.m00 = 16; rotation.m11 = -16; rotation.m22 = 16;
        matrix.mul(rotation);
        return matrix;
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

    /** Keeps broad item models inside the preview cell while leaving normal item styles intact. */
    static float itemModelScale(ItemStack stack, float requestedScale) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemShield) {
            return Math.min(requestedScale, 1.35F);
        }
        return requestedScale;
    }

    static float itemModelRoll(ItemStack stack, float requestedRoll) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemShield) {
            return Math.copySign(Math.min(Math.abs(requestedRoll), 22.0F), requestedRoll);
        }
        return requestedRoll;
    }

    /** Resolve world/player overrides and retain vanilla's built-in special-item renderer. */
    static void renderItemModel(ItemStack stack) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null) {
            minecraft.getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.NONE);
        } else {
            minecraft.getRenderItem().renderItem(stack, minecraft.player,
                    ItemCameraTransforms.TransformType.NONE, false);
        }
    }

    /** Reuses the equipment backend without the tooltip's appearance timeline or sound. */
    static void renderZoomArmor(NfrTooltipApi.ArmorPreviewRequest request, int size) {
        if (request == null) return;
        PreviewModelBounds bounds = TooltipConfig.zoomOverlayMeasureBounds
                ? PreviewBoundsMeasurement.measure(request) : null;
        GlStateManager.pushMatrix();
        try {
            if (bounds != null) PreviewBoundsMeasurement.applyFit(bounds, size, size, Math.max(2, size / 25));
            ArmorRenderer.renderModel(request, 0, 0, new NfrTooltipApi.PreviewSize(size, size), 1,
                    rotationAngle(System.nanoTime(), request.rotationSpeed()), currentSwing());
        } finally {
            GlStateManager.popMatrix();
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
            World world = Minecraft.getMinecraft().world;
            if (world == null || !hasEquipment(request)) return;
            if (request.model() == NfrTooltipApi.ArmorPreviewModel.PLAYER) {
                try {
                    renderPlayer(request, x, y, size, animation, spin, swing);
                } catch (RuntimeException | LinkageError ignored) {
                    renderArmorStand(request, x, y, size, animation, world, spin);
                }
                return;
            }

            renderArmorStand(request, x, y, size, animation, world, spin);
        }

        private static void renderArmorStand(NfrTooltipApi.ArmorPreviewRequest request,
                                             int x, int y, NfrTooltipApi.PreviewSize size,
                                             float animation, World world, float spin) {
            EntityArmorStand stand = getArmorStandPreview(world);
            applyArmorEquipment(stand, request);
            stand.setNoGravity(true);
            stand.setInvisible(false);
            stand.setAlwaysRenderNameTag(false);
            ((InvokerEntityArmorStandPreview) (Object) stand)
                    .nfrUi$setNoBasePlate(!TooltipConfig.armorStandBasePlate);
            // Keep the vanilla renderer's entity yaw stable. The preview matrix owns the
            // complete turn so the stand base plate follows the body and armor in exactly the
            // same transform instead of relying on RenderArmorStand's interpolated yaw path.
            setPreviewYaw(stand, 180.0F);

            RenderManager dispatcher = Minecraft.getMinecraft().getRenderManager();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GlStateManager.pushMatrix();
            boolean oldShadow = dispatcher.isRenderShadow();
            float oldPlayerViewY = dispatcher.playerViewY;
            try {
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                preparePreviewDepthLayer();
                GlStateManager.color(1.0F, 1.0F, 1.0F, animation);
                RenderHelper.enableStandardItemLighting();
                float centerX = x + size.width() * 0.46F;
                // Armor stand coordinates use the feet as the entity origin.  Keep that
                // origin on the bottom edge of the logical preview cell; the bounds pass
                // then moves/scales the complete rendered silhouette (including oversized
                // helmet geometry) into the cell.  The old -7 offset put the head outside
                // the cell before fitting and clipped the top of large helmets.
                GlStateManager.translate(centerX, y + size.height(), 500.0F);
                GlStateManager.scale(-request.scale() * animation, -request.scale() * animation,
                        request.scale() * animation);
                GlStateManager.rotate(request.pitch(), 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(spin, 0.0F, 1.0F, 0.0F);
                dispatcher.setRenderShadow(false);
                dispatcher.setPlayerViewY(180.0F);
                dispatcher.renderEntity(stand, 0.0D, 0.0D, 0.0D, stand.rotationYaw, 1.0F, false);
            } finally {
                dispatcher.setRenderShadow(oldShadow);
                dispatcher.setPlayerViewY(oldPlayerViewY);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
                GL11.glPopAttrib();
            }
        }

        private static boolean hasEquipment(NfrTooltipApi.ArmorPreviewRequest request) {
            for (ItemStack stack : request.equipment()) {
                if (stack != null && !stack.isEmpty()) return true;
            }
            return false;
        }

        private static void renderPlayer(NfrTooltipApi.ArmorPreviewRequest request,
                                         int x, int y, NfrTooltipApi.PreviewSize size,
                                         float animation, float spin, float swing) {
            Minecraft minecraft = Minecraft.getMinecraft();
            EntityPlayer source = minecraft.player;
            World world = minecraft.world;
            if (source == null || world == null) throw new IllegalStateException("Player model unavailable");
            GameProfile profile = source.getGameProfile();
            EntityOtherPlayerMP player = getPlayerPreview(world, profile);
            applyArmorEquipment(player, request);
            player.setPositionAndRotation(source.posX, source.posY, source.posZ,
                    source.rotationYaw, 0);
            player.prevRotationPitch = 0;
            player.setPrimaryHand(source.getPrimaryHand());
            player.setSneaking(TooltipConfig.armorPlayerSneaking);
            if (TooltipConfig.armorPlayerCopyHands) {
                player.setHeldItem(EnumHand.MAIN_HAND, source.getHeldItemMainhand().copy());
                player.setHeldItem(EnumHand.OFF_HAND, source.getHeldItemOffhand().copy());
            } else {
                player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                player.setHeldItem(EnumHand.OFF_HAND, ItemStack.EMPTY);
            }
            if ("swing".equalsIgnoreCase(TooltipConfig.armorPlayerPose)) {
                player.prevSwingProgress = swing;
                player.swingProgress = swing;
                player.isSwingInProgress = true;
                player.swingingHand = EnumHand.MAIN_HAND;
            } else {
                player.prevSwingProgress = 0.0F;
                player.swingProgress = 0.0F;
                player.isSwingInProgress = false;
            }
            renderLiving(player, request, x, y, size, animation, spin);
        }

        private static EntityArmorStand getArmorStandPreview(World world) {
            if (armorStandPreview == null || armorStandWorld != world) {
                armorStandWorld = world;
                armorStandPreview = new EntityArmorStand(world);
            }
            return armorStandPreview;
        }

        private static EntityOtherPlayerMP getPlayerPreview(World world, GameProfile profile) {
            java.util.UUID id = profile == null ? null : profile.getId();
            if (playerPreview == null || playerPreviewWorld != world
                    || (id != null && !id.equals(playerPreviewId))) {
                playerPreviewWorld = world;
                playerPreviewId = id;
                playerPreview = new EntityOtherPlayerMP(world, profile);
            }
            return playerPreview;
        }

        private static void applyArmorEquipment(EntityLivingBase entity,
                                                NfrTooltipApi.ArmorPreviewRequest request) {
            for (EntityEquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack desired = ItemStack.EMPTY;
                if (request.mode() == NfrTooltipApi.ArmorPreviewMode.FULL_SET) {
                    for (ItemStack stack : request.equipment()) {
                        if (slot == PreviewEquipment.armorSlot(stack)) desired = stack;
                    }
                } else if (slot == request.slot()) {
                    desired = request.stack();
                }
                if (desired == null) desired = ItemStack.EMPTY;
                if (!ItemStack.areItemStacksEqual(entity.getItemStackFromSlot(slot), desired)) {
                    entity.setItemStackToSlot(slot, desired.isEmpty() ? ItemStack.EMPTY : desired.copy());
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

        private static void renderLiving(EntityPlayer entity,
                                         NfrTooltipApi.ArmorPreviewRequest request,
                                         int x, int y, NfrTooltipApi.PreviewSize size,
                                         float animation, float spin) {
            RenderManager dispatcher = Minecraft.getMinecraft().getRenderManager();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GlStateManager.pushMatrix();
            boolean oldShadow = dispatcher.isRenderShadow();
            float oldPlayerViewY = dispatcher.playerViewY;
            try {
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                preparePreviewDepthLayer();
                GlStateManager.color(1.0F, 1.0F, 1.0F, animation);
                RenderHelper.enableStandardItemLighting();
                setPreviewYaw(entity, 180.0F + spin);
                // Match the armor-stand anchor: the player entity origin is at its feet,
                // so fitting must see the whole model before it is centered in the cell.
                GlStateManager.translate(x + size.width() * 0.46F, y + size.height(), 500.0F);
                GlStateManager.scale(-request.scale() * animation, -request.scale() * animation,
                        request.scale() * animation);
                GlStateManager.rotate(request.pitch(), 1.0F, 0.0F, 0.0F);
                dispatcher.setRenderShadow(false);
                dispatcher.setPlayerViewY(180.0F);
                dispatcher.renderEntity(entity, 0.0D, 0.0D, 0.0D, entity.rotationYaw, 1.0F, false);
            } finally {
                dispatcher.setRenderShadow(oldShadow);
                dispatcher.setPlayerViewY(oldPlayerViewY);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
                GL11.glPopAttrib();
            }
        }
    }
}
