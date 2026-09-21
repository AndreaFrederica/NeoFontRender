package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
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
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
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
            key.append(stack.serializeNBT());
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
                return new NfrTooltipApi.PreviewSize(item.width(), item.height());
            }
            return new NfrTooltipApi.PreviewSize(30, 64);
        }

        @Override public void render(NfrTooltipApi.PreviewRequest value, int x, int y,
                                     NfrTooltipApi.PreviewSize size, FontRenderer font) {
            if (!(value instanceof NfrTooltipApi.ItemPreviewRequest)) return;
            NfrTooltipApi.ItemPreviewRequest request = (NfrTooltipApi.ItemPreviewRequest) value;
            ItemStack stack = request.stack();
            if (stack == null || stack.isEmpty()) return;
            float animation = animationProgress(request, stack);

            Minecraft minecraft = Minecraft.getMinecraft();
            RenderItem itemRenderer = minecraft.getRenderItem();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.enableTexture2D();
                GlStateManager.enableAlpha();
                preparePreviewDepthLayer();
                GlStateManager.color(1.0F, 1.0F, 1.0F, animation);
                RenderHelper.enableGUIStandardItemLighting();
                enablePreviewScissor(x, y, size);
                float centerX = x + size.width()
                        * (stack.getItem() instanceof ItemShield ? 0.50F : 0.46F);
                float centerY = y + size.height() * 0.52F;
                float spin = rotationAngle(System.nanoTime(), request.rotationSpeed());
                GlStateManager.translate(centerX, centerY, 500.0F);
                float modelScale = itemModelScale(stack, request.scale());
                float modelRoll = itemModelRoll(stack, request.roll());
                GlStateManager.scale(modelScale * animation, modelScale * animation,
                        modelScale * animation);
                GlStateManager.rotate(request.pitch(), 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(spin, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(modelRoll, 0.0F, 0.0F, 1.0F);
                GlStateManager.scale(16.0F, -16.0F, 16.0F);
                itemRenderer.renderItem(stack, ItemCameraTransforms.TransformType.NONE);
            } finally {
                disablePreviewScissor();
                RenderHelper.disableStandardItemLighting();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
                GL11.glPopAttrib();
            }
            PreviewEffects.render(request, x, y, size, animation);
        }
    }

    private static boolean previewScissorWasEnabled;
    private static final IntBuffer PREVIEW_SCISSOR = BufferUtils.createIntBuffer(4);

    private static void enablePreviewScissor(int x, int y, NfrTooltipApi.PreviewSize size) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(minecraft);
        int factor = resolution.getScaleFactor();
        int left = Math.max(0, x * factor);
        int bottom = Math.max(0, minecraft.displayHeight - (y + size.height()) * factor);
        int width = Math.max(0, Math.min(minecraft.displayWidth - left, size.width() * factor));
        int height = Math.max(0, Math.min(minecraft.displayHeight - bottom, size.height() * factor));
        previewScissorWasEnabled = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (previewScissorWasEnabled) {
            PREVIEW_SCISSOR.clear();
            GL11.glGetInteger(GL11.GL_SCISSOR_BOX, PREVIEW_SCISSOR);
            int oldLeft = PREVIEW_SCISSOR.get(0);
            int oldBottom = PREVIEW_SCISSOR.get(1);
            int oldRight = oldLeft + PREVIEW_SCISSOR.get(2);
            int oldTop = oldBottom + PREVIEW_SCISSOR.get(3);
            int right = Math.min(left + width, oldRight);
            int top = Math.min(bottom + height, oldTop);
            left = Math.max(left, oldLeft);
            bottom = Math.max(bottom, oldBottom);
            width = Math.max(0, right - left);
            height = Math.max(0, top - bottom);
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left, bottom, width, height);
    }

    private static void disablePreviewScissor() {
        if (previewScissorWasEnabled) {
            GL11.glScissor(PREVIEW_SCISSOR.get(0), PREVIEW_SCISSOR.get(1),
                    PREVIEW_SCISSOR.get(2), PREVIEW_SCISSOR.get(3));
        } else {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }
    }

    /** Keeps broad item models inside the preview cell while leaving normal item styles intact. */
    static float itemModelScale(ItemStack stack, float requestedScale) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemShield) {
            return Math.min(requestedScale, 2.0F);
        }
        return requestedScale;
    }

    static float itemModelRoll(ItemStack stack, float requestedRoll) {
        if (stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemShield) {
            return Math.copySign(Math.min(Math.abs(requestedRoll), 22.0F), requestedRoll);
        }
        return requestedRoll;
    }

    private static final class ArmorRenderer implements NfrTooltipApi.PreviewRenderer {
        @Override public NfrTooltipApi.PreviewKind previewKind() {
            return NfrTooltipApi.PreviewKind.ARMOR;
        }

        @Override public NfrTooltipApi.PreviewSize measure(NfrTooltipApi.PreviewRequest request,
                                                            FontRenderer font) {
            if (request instanceof NfrTooltipApi.ArmorPreviewRequest) {
                NfrTooltipApi.ArmorPreviewRequest armor = (NfrTooltipApi.ArmorPreviewRequest) request;
                return new NfrTooltipApi.PreviewSize(armor.width(), armor.height());
            }
            return new NfrTooltipApi.PreviewSize(40, 64);
        }

        @Override public void render(NfrTooltipApi.PreviewRequest value, int x, int y,
                                     NfrTooltipApi.PreviewSize size, FontRenderer font) {
            if (!(value instanceof NfrTooltipApi.ArmorPreviewRequest)) return;
            NfrTooltipApi.ArmorPreviewRequest request = (NfrTooltipApi.ArmorPreviewRequest) value;
            World world = Minecraft.getMinecraft().world;
            if (world == null || !hasEquipment(request)) return;
            float animation = animationProgress(request, request.stack());

            if (request.model() == NfrTooltipApi.ArmorPreviewModel.PLAYER) {
                try {
                    renderPlayer(request, x, y, size, animation);
                } catch (RuntimeException | LinkageError ignored) {
                    renderArmorStand(request, x, y, size, animation, world);
                }
                return;
            }

            renderArmorStand(request, x, y, size, animation, world);
        }

        private static void renderArmorStand(NfrTooltipApi.ArmorPreviewRequest request,
                                             int x, int y, NfrTooltipApi.PreviewSize size,
                                             float animation, World world) {
            EntityArmorStand stand = getArmorStandPreview(world);
            applyArmorEquipment(stand, request);
            stand.setNoGravity(true);
            stand.setInvisible(false);
            stand.setAlwaysRenderNameTag(false);
            ((InvokerEntityArmorStandPreview) (Object) stand)
                    .nfrUi$setNoBasePlate(!TooltipConfig.armorStandBasePlate);
            float spin = rotationAngle(System.nanoTime(), request.rotationSpeed());
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
                GlStateManager.translate(centerX, y + size.height() - 7.0F, 500.0F);
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
            PreviewEffects.render(request, x, y, size, animation);
        }

        private static boolean hasEquipment(NfrTooltipApi.ArmorPreviewRequest request) {
            for (ItemStack stack : request.equipment()) {
                if (stack != null && !stack.isEmpty()) return true;
            }
            return false;
        }

        private static void renderPlayer(NfrTooltipApi.ArmorPreviewRequest request,
                                         int x, int y, NfrTooltipApi.PreviewSize size,
                                         float animation) {
            Minecraft minecraft = Minecraft.getMinecraft();
            EntityPlayer source = minecraft.player;
            World world = minecraft.world;
            if (source == null || world == null) throw new IllegalStateException("Player model unavailable");
            GameProfile profile = source.getGameProfile();
            EntityOtherPlayerMP player = getPlayerPreview(world, profile);
            applyArmorEquipment(player, request);
            player.setPositionAndRotation(source.posX, source.posY, source.posZ,
                    source.rotationYaw, source.rotationPitch);
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
                float swing = 0.5F + 0.5F * (float) Math.sin(System.nanoTime() / 300_000_000.0D);
                player.prevSwingProgress = swing;
                player.swingProgress = swing;
                player.isSwingInProgress = true;
                player.swingingHand = EnumHand.MAIN_HAND;
            } else {
                player.prevSwingProgress = 0.0F;
                player.swingProgress = 0.0F;
                player.isSwingInProgress = false;
            }
            renderLiving(player, request, x, y, size, animation);
            PreviewEffects.render(request, x, y, size, animation);
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
                                         float animation) {
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
                float spin = rotationAngle(System.nanoTime(), request.rotationSpeed());
                setPreviewYaw(entity, 180.0F + spin);
                GlStateManager.translate(x + size.width() * 0.46F, y + size.height() - 7.0F, 500.0F);
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
