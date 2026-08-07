package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * Renders cached player skin heads beside chat lines. Minecraft 1.7.10 resolves skins
 * per player entity instead of GuiPlayerInfo, so lines carry the sender's name.
 */
public final class ChatHeadRenderer {
    public static final int HEAD_SIZE = 8;
    public static final int TEXT_OFFSET = 11;

    private ChatHeadRenderer() {}

    public static int textOffset() {
        return EnhancedChatFeatures.playerHeads() ? TEXT_OFFSET : 0;
    }

    public static void render(String senderName, int x, int y, float opacity) {
        if (!EnhancedChatFeatures.playerHeads() || senderName == null || opacity <= 0.0F) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null) return;
        ResourceLocation skin = skinFor(minecraft, senderName);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        minecraft.getTextureManager().bindTexture(skin);
        if (EnhancedChatFeatures.headShadow()) {
            GL11.glColor4f(0.25F, 0.25F, 0.25F, opacity);
            drawFace(x + 1, y + 1);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, opacity);
        drawFace(x, y);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void renderCandidate(String candidate, int x, int y, float opacity) {
        if (candidate == null || candidate.length() < 2 || candidate.charAt(0) != '@') return;
        render(candidate.substring(1), x, y, opacity);
    }

    public static void renderVanilla(List<ChatLine> lines, int scrollPos, int updateCounter,
                                     int lineCount, float chatScale) {
        if (!EnhancedChatFeatures.playerHeads() || lines.isEmpty()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings.chatVisibility == EntityPlayer.EnumChatVisibility.HIDDEN) return;
        boolean open = minecraft.ingameGUI.getChatGUI().getChatOpen();
        float configuredOpacity = minecraft.gameSettings.chatOpacity * 0.9F + 0.1F;

        GL11.glPushMatrix();
        GL11.glTranslatef(2.0F, 20.0F, 0.0F);
        GL11.glScalef(chatScale, chatScale, 1.0F);
        for (int row = 0; row + scrollPos < lines.size() && row < lineCount; row++) {
            ChatLine line = lines.get(row + scrollPos);
            if (!(line instanceof ChatHeadLineMetadata)) continue;
            ChatHeadLineMetadata metadata = (ChatHeadLineMetadata) line;
            if (!metadata.nfrUi$isFirstFragment()) continue;
            int age = updateCounter - line.getUpdatedCounter();
            if (age >= 200 && !open) continue;
            double fade = (1.0D - age / 200.0D) * 10.0D;
            if (fade < 0.0D) fade = 0.0D;
            if (fade > 1.0D) fade = 1.0D;
            int alpha = open ? 255 : (int) (255.0D * fade * fade);
            alpha = (int) (alpha * configuredOpacity);
            if (alpha <= 3) continue;
            int y = -row * minecraft.fontRenderer.FONT_HEIGHT - 8;
            render(metadata.nfrUi$getSenderName(), 0, y, alpha / 255.0F);
        }
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static ResourceLocation skinFor(Minecraft minecraft, String senderName) {
        EntityPlayer player = minecraft.theWorld.getPlayerEntityByName(senderName);
        if (player instanceof AbstractClientPlayer) {
            return ((AbstractClientPlayer) player).getLocationSkin();
        }
        // The player list knows the name but no entity is loaded (e.g. another dimension);
        // fall back to the shared download cache so the head appears once fetched.
        ResourceLocation skin = AbstractClientPlayer.getLocationSkin(senderName);
        AbstractClientPlayer.getDownloadImageSkin(skin, senderName);
        return skin;
    }

    private static void drawFace(int x, int y) {
        drawSkinPart(x, y, 8.0F, 8.0F);
        drawSkinPart(x, y, 40.0F, 8.0F);
    }

    private static void drawSkinPart(int x, int y, float u, float v) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + HEAD_SIZE, 0.0D, u / 64.0D, (v + 8.0F) / 64.0D);
        tessellator.addVertexWithUV(x + HEAD_SIZE, y + HEAD_SIZE, 0.0D,
                (u + 8.0F) / 64.0D, (v + 8.0F) / 64.0D);
        tessellator.addVertexWithUV(x + HEAD_SIZE, y, 0.0D, (u + 8.0F) / 64.0D, v / 64.0D);
        tessellator.addVertexWithUV(x, y, 0.0D, u / 64.0D, v / 64.0D);
        tessellator.draw();
    }
}
