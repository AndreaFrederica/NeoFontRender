package neofontrender.client.render.sign;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.client.MinecraftForgeClient;
import neofontrender.NeoFontRender;
import neofontrender.core.config.NeofontrenderConfig;
import neofontrender.core.font.FontManager;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.skia.SkijaTextRenderer;
import neofontrender.core.font.support.FontRenderTuning;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects distant vanilla signs inside Forge's TESR batch window. Models are emitted into one
 * lightmapped buffer before text is replayed, preserving the original board-before-text depth order.
 */
public final class SignBatchRenderer {
    private static final ResourceLocation SIGN_TEXTURE = new ResourceLocation("textures/entity/sign.png");
    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean collecting;
    private static boolean flushing;
    private static boolean failed;
    private static int collectedLastFrame;
    private static int modelBatchesLastFrame;
    private static int textDrawsLastFrame;
    private static String lastFallback = "";

    private SignBatchRenderer() {
    }

    public static void begin() {
        if (!ENTRIES.isEmpty()) {
            // A missing drawBatch hook must never leak stale instances into the next frame/pass.
            ENTRIES.clear();
            lastFallback = "unpaired batch reset";
        }
        collecting = NeofontrenderConfig.signCrossTileBatching()
                && !failed
                && MinecraftForgeClient.getRenderPass() == 0
                && FontManager.INSTANCE.isSkiaActive()
                && FontManager.INSTANCE.getSkijaTextRenderer() != null;
        flushing = false;
    }

    public static boolean collect(TileEntitySign sign, double x, double y, double z,
                                  int destroyStage) {
        if (!collecting || flushing || sign == null || destroyStage >= 0) {
            return false;
        }
        float minDistance = NeofontrenderConfig.signModelLodDistance();
        if (x * x + y * y + z * z < minDistance * minDistance) {
            return false;
        }
        if (ENTRIES.size() >= NeofontrenderConfig.signBatchMaxEntries()) {
            lastFallback = "entry limit";
            return false;
        }

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc == null ? null : mc.fontRenderer;
        if (font == null) {
            return false;
        }
        String[] lines = new String[4];
        for (int i = 0; i < lines.length; i++) {
            ITextComponent component = i < sign.signText.length ? sign.signText[i] : null;
            if (component == null) {
                lines[i] = "";
                continue;
            }
            List<ITextComponent> wrapped = GuiUtilRenderComponents.splitText(component, 90, font, false, true);
            String text = wrapped == null || wrapped.isEmpty() ? "" : wrapped.get(0).getFormattedText();
            lines[i] = i == sign.lineBeingEdited ? "> " + text + " <" : text;
        }
        int light = sign.getWorld() == null ? 0x00F000F0
                : sign.getWorld().getCombinedLight(sign.getPos(), 0);
        ENTRIES.add(new Entry(x, y, z, sign.getBlockType(), sign.getBlockMetadata(), light, lines));
        return true;
    }

    public static void flush(int pass) {
        collecting = false;
        if (flushing || pass != 0 || ENTRIES.isEmpty()) {
            ENTRIES.clear();
            return;
        }
        flushing = true;
        ArrayList<Entry> entries = new ArrayList<>(ENTRIES);
        ENTRIES.clear();
        collectedLastFrame = entries.size();
        modelBatchesLastFrame = 0;
        textDrawsLastFrame = 0;
        try {
            drawModels(entries);
            drawText(entries);
            lastFallback = "";
        } catch (Throwable t) {
            lastFallback = t.getClass().getSimpleName();
            // Once entries have replaced the immediate calls they cannot be replayed at the old
            // traversal positions. Disable future collection so the next frame is fully vanilla.
            failed = true;
            NeoFontRender.LOGGER.error("Failed to flush cross-tile sign batch; using immediate rendering until restart", t);
        } finally {
            // These are the states Forge's drawBatch expects to own immediately after this hook.
            GlStateManager.depthMask(true);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            RenderHelper.enableStandardItemLighting();
            flushing = false;
        }
    }

    public static String debugLine() {
        String suffix = lastFallback.isEmpty() ? "" : " fallback=" + lastFallback;
        return "NFR signs: collected=" + collectedLastFrame + " model_batches="
                + modelBatchesLastFrame + " text_draws=" + textDrawsLastFrame + suffix;
    }

    private static void drawModels(List<Entry> entries) {
        Minecraft mc = Minecraft.getMinecraft();
        TextureManager textures = mc.getTextureManager();
        textures.bindTexture(SIGN_TEXTURE);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableCull();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        for (Entry entry : entries) {
            appendModel(buffer, entry);
        }
        tessellator.draw();
        modelBatchesLastFrame = 1;
    }

    private static void drawText(List<Entry> entries) {
        SkijaTextRenderer renderer = FontManager.INSTANCE.getSkijaTextRenderer();
        if (renderer == null || !FontManager.INSTANCE.isSkiaActive()) {
            lastFallback = "Skia unavailable during flush";
            return;
        }
        for (Entry entry : entries) {
            GlStateManager.pushMatrix();
            try {
                applySignTransform(entry);
                GlStateManager.translate(0.0F, 0.33333334F, 0.046666667F);
                GlStateManager.scale(0.010416667F, -0.010416667F, 0.010416667F);
                GlStateManager.glNormal3f(0.0F, 0.0F, -0.010416667F);
                GlStateManager.depthMask(false);
                FontRenderTuning.updateFromCurrentGlState(false);
                if (NeofontrenderConfig.signTextLodCulling()
                        && !FontRenderTuning.isCurrentTextQuadVisible(-45.0F, -20.0F, 90.0F, 40.0F,
                        NeofontrenderConfig.signTextMinPixelHeight())) {
                    continue;
                }
                TextRenderResult result = renderer.renderSign(entry.lines);
                if (result != null && result != TextRenderResult.EMPTY && result.advance() > 0.0F) {
                    result.draw(-45.0F, -20.0F, 1.0F);
                    textDrawsLastFrame++;
                }
            } finally {
                GlStateManager.depthMask(true);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
            }
        }
    }

    private static void applySignTransform(Entry entry) {
        GlStateManager.translate((float) entry.x + 0.5F, (float) entry.y + 0.5F, (float) entry.z + 0.5F);
        GlStateManager.rotate(-entry.rotationDegrees(), 0.0F, 1.0F, 0.0F);
        if (!entry.standing()) {
            GlStateManager.translate(0.0F, -0.3125F, -0.4375F);
        }
    }

    private static void appendModel(BufferBuilder buffer, Entry entry) {
        // ModelSign UV regions are stable normalized coordinates in the currently bound sign texture.
        appendQuad(buffer, entry, -0.75F, -0.875F, -0.0625F, 0.75F, -0.125F,
                2.0F / 64.0F, 2.0F / 32.0F, 26.0F / 64.0F, 14.0F / 32.0F);
        appendQuad(buffer, entry, 0.75F, -0.875F, 0.0625F, -0.75F, -0.125F,
                28.0F / 64.0F, 2.0F / 32.0F, 52.0F / 64.0F, 14.0F / 32.0F);
        if (entry.standing()) {
            appendQuad(buffer, entry, -0.0625F, -0.125F, -0.0625F, 0.0625F, 0.75F,
                    2.0F / 64.0F, 16.0F / 32.0F, 4.0F / 64.0F, 30.0F / 32.0F);
            appendQuad(buffer, entry, 0.0625F, -0.125F, 0.0625F, -0.0625F, 0.75F,
                    6.0F / 64.0F, 16.0F / 32.0F, 8.0F / 64.0F, 30.0F / 32.0F);
        }
    }

    private static void appendQuad(BufferBuilder buffer, Entry entry,
                                   float left, float top, float z, float right, float bottom,
                                   float u0, float v0, float u1, float v1) {
        vertex(buffer, entry, left, top, z, u0, v0);
        vertex(buffer, entry, left, bottom, z, u0, v1);
        vertex(buffer, entry, right, bottom, z, u1, v1);
        vertex(buffer, entry, right, top, z, u1, v0);
    }

    private static void vertex(BufferBuilder buffer, Entry entry, float x, float y, float z,
                               float u, float v) {
        // ModelSign first scales its 1/16 model coordinates by (2/3,-2/3,-2/3).
        double px = x * 0.6666667D;
        double py = y * -0.6666667D;
        double pz = z * -0.6666667D;
        if (!entry.standing()) {
            py -= 0.3125D;
            pz -= 0.4375D;
        }
        double radians = Math.toRadians(-entry.rotationDegrees());
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        double rx = cos * px + sin * pz;
        double rz = -sin * px + cos * pz;
        int blockLight = entry.light & 0xFFFF;
        int skyLight = entry.light >>> 16 & 0xFFFF;
        buffer.pos(entry.x + 0.5D + rx, entry.y + 0.5D + py, entry.z + 0.5D + rz)
                .color(255, 255, 255, 255)
                .tex(u, v)
                .lightmap(blockLight, skyLight)
                .endVertex();
    }

    private static final class Entry {
        private final double x;
        private final double y;
        private final double z;
        private final Block block;
        private final int metadata;
        private final int light;
        private final String[] lines;

        private Entry(double x, double y, double z, Block block, int metadata, int light, String[] lines) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.metadata = metadata;
            this.light = light;
            this.lines = lines;
        }

        private boolean standing() {
            return block == Blocks.STANDING_SIGN;
        }

        private float rotationDegrees() {
            if (standing()) {
                return metadata * 360.0F / 16.0F;
            }
            if (metadata == 2) {
                return 180.0F;
            }
            if (metadata == 4) {
                return 90.0F;
            }
            if (metadata == 5) {
                return -90.0F;
            }
            return 0.0F;
        }
    }
}
