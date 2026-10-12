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
 * Renders cached player skin heads and server avatars beside chat lines. Minecraft 1.7.10 resolves
 * skins per player entity instead of GuiPlayerInfo, so lines carry the sender's account name.
 */
public final class ChatHeadRenderer {
    public static final int HEAD_SIZE = 8;
    public static final int TEXT_OFFSET = 11;

    private static final ResourceLocation SERVER_AVATAR = avatar("server_avatar");
    private static final ResourceLocation SERVER_AVATAR_TOWER = avatar("server_avatar_tower");
    private static final ResourceLocation SERVER_AVATAR_STORAGE = avatar("server_avatar_storage");
    private static final ResourceLocation SERVER_AVATAR_NARROW = avatar("server_avatar_narrow");

    private ChatHeadRenderer() {}

    private static ResourceLocation avatar(String name) {
        return new ResourceLocation("neofontrender_ui_enhancements", "textures/chat/" + name + ".png");
    }

    private static ResourceLocation serverAvatar() {
        switch (EnhancedChatConfig.serverAvatar) {
            case "tower": return SERVER_AVATAR_TOWER;
            case "storage": return SERVER_AVATAR_STORAGE;
            case "narrow": return SERVER_AVATAR_NARROW;
            default: return SERVER_AVATAR;
        }
    }

    public static boolean headsEnabled() {
        return EnhancedChatConfig.enabled && (EnhancedChatConfig.playerHeads || EnhancedChatConfig.serverHeads);
    }

    public static boolean isServer(String senderName, ChatMessageMetadata metadata) {
        return metadata != null ? metadata.source == ChatSource.SERVER : senderName == null;
    }

    public static boolean showMessageHead(String senderName, ChatMessageMetadata metadata, boolean first,
                                          String olderSenderName, ChatMessageMetadata olderMetadata,
                                          boolean hasOlder) {
        if (!first) return false;
        boolean server = isServer(senderName, metadata);
        boolean grouped = server ? EnhancedChatConfig.serverHeadGroupFirst
                : EnhancedChatConfig.playerHeadGroupFirst;
        return !grouped || !hasOlder
                || !sameSender(senderName, metadata, olderSenderName, olderMetadata);
    }

    /**
     * Adjacency rule of ChatHeadGrouping for the name-based sender identity of 1.7.10.
     * Account names are compared case-insensitively because the tab list and chat components
     * disagree on casing for the same player.
     */
    private static boolean sameSender(String senderName, ChatMessageMetadata metadata,
                                      String olderSenderName, ChatMessageMetadata olderMetadata) {
        boolean server = metadata != null ? metadata.source == ChatSource.SERVER : senderName == null;
        boolean olderServer = olderMetadata != null
                ? olderMetadata.source == ChatSource.SERVER : olderSenderName == null;
        if (server || olderServer) return server && olderServer;
        if (metadata != null && olderMetadata != null && metadata.source != olderMetadata.source) return false;
        if (senderName != null || olderSenderName != null) {
            return senderName != null && senderName.equalsIgnoreCase(olderSenderName);
        }
        return metadata != null && olderMetadata != null && !metadata.playerName.isEmpty()
                && metadata.playerName.equalsIgnoreCase(olderMetadata.playerName);
    }

    public static void renderMessage(String senderName, ChatMessageMetadata metadata, int x, int y, float opacity) {
        if (!EnhancedChatConfig.enabled || opacity <= 0.0F) return;
        if (isServer(senderName, metadata)) {
            if (EnhancedChatConfig.serverHeads) renderTexture(serverAvatar(), x, y, opacity);
        } else if (senderName != null) {
            render(senderName, x, y, opacity);
        }
    }

    public static int textOffset() {
        return headsEnabled() ? TEXT_OFFSET : 0;
    }

    public static void render(String senderName, int x, int y, float opacity) {
        if (!EnhancedChatFeatures.playerHeads() || senderName == null || opacity <= 0.0F) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.theWorld == null) return;
        renderTexture(skinFor(minecraft, senderName), x, y, opacity);
    }

    /** Draws a completion-list face independently of the message-head visibility setting. */
    public static void renderCandidate(String candidate, int x, int y, float opacity) {
        if (candidate == null || candidate.length() < 2 || candidate.charAt(0) != '@') return;
        render(candidate.substring(1), x, y, opacity);
    }

    private static void renderTexture(ResourceLocation texture, int x, int y, float opacity) {
        Minecraft minecraft = Minecraft.getMinecraft();

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        minecraft.getTextureManager().bindTexture(texture);
        if (EnhancedChatFeatures.headShadow()) {
            GL11.glColor4f(0.25F, 0.25F, 0.25F, opacity);
            drawAvatar(texture, x + 1, y + 1);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, opacity);
        drawAvatar(texture, x, y);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawAvatar(ResourceLocation texture, int x, int y) {
        if (isServerAvatar(texture)) {
            drawFullTexture(x, y);
        } else {
            drawFace(x, y);
        }
    }

    private static boolean isServerAvatar(ResourceLocation texture) {
        return SERVER_AVATAR.equals(texture) || SERVER_AVATAR_TOWER.equals(texture)
                || SERVER_AVATAR_STORAGE.equals(texture) || SERVER_AVATAR_NARROW.equals(texture);
    }

    /** Server avatars are standalone images, unlike the two-plane face layout of player skins. */
    private static void drawFullTexture(int x, int y) {
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + HEAD_SIZE, 0.0D, 0.0D, 1.0D);
        tessellator.addVertexWithUV(x + HEAD_SIZE, y + HEAD_SIZE, 0.0D, 1.0D, 1.0D);
        tessellator.addVertexWithUV(x + HEAD_SIZE, y, 0.0D, 1.0D, 0.0D);
        tessellator.addVertexWithUV(x, y, 0.0D, 0.0D, 0.0D);
        tessellator.draw();
    }

    public static void renderVanilla(List<ChatLine> lines, int scrollPos, int updateCounter,
                                     int lineCount, float chatScale) {
        if (!headsEnabled() || lines.isEmpty()) return;
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
            int olderIndex = row + scrollPos + 1;
            ChatHeadLineMetadata older =
                    olderIndex < lines.size() && lines.get(olderIndex) instanceof ChatHeadLineMetadata
                            ? (ChatHeadLineMetadata) lines.get(olderIndex) : null;
            if (!showMessageHead(metadata.nfrUi$getSenderName(), metadata.nfrUi$getMessageMetadata(),
                    metadata.nfrUi$isFirstFragment(), older == null ? null : older.nfrUi$getSenderName(),
                    older == null ? null : older.nfrUi$getMessageMetadata(), older != null)) continue;
            int age = updateCounter - line.getUpdatedCounter();
            if (age >= 200 && !open) continue;
            double fade = (1.0D - age / 200.0D) * 10.0D;
            if (fade < 0.0D) fade = 0.0D;
            if (fade > 1.0D) fade = 1.0D;
            int alpha = open ? 255 : (int) (255.0D * fade * fade);
            alpha = (int) (alpha * configuredOpacity);
            if (alpha <= 3) continue;
            int y = ChatInlineLayout.bottomAlignedY(lines, scrollPos, row, HEAD_SIZE,
                    minecraft.fontRenderer);
            renderMessage(metadata.nfrUi$getSenderName(), metadata.nfrUi$getMessageMetadata(),
                    0, y, alpha / 255.0F);
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
