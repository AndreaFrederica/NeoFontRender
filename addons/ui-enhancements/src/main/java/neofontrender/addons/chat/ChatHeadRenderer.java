package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import java.util.List;
import java.util.UUID;

public final class ChatHeadRenderer {
    public static final int HEAD_SIZE = 8;
    public static final int TEXT_OFFSET = 11;
    private static final ResourceLocation SERVER_AVATAR = new ResourceLocation("neofontrender_ui_enhancements", "textures/chat/server_avatar.png");

    private static final ResourceLocation TOWER = avatar("tower");
    private static final ResourceLocation STORAGE = avatar("storage");
    private static final ResourceLocation NARROW = avatar("narrow");

    private static ResourceLocation avatar(String name) {
        return new ResourceLocation("neofontrender_ui_enhancements", "textures/chat/server_avatar_" + name + ".png");
    }

    private static ResourceLocation serverAvatar() {
        switch (EnhancedChatConfig.serverAvatar) {
            case "tower": return TOWER;
            case "storage": return STORAGE;
            case "narrow": return NARROW;
            default: return SERVER_AVATAR;
        }
    }

    public static boolean headsEnabled() {
        return EnhancedChatConfig.enabled && (EnhancedChatConfig.playerHeads || EnhancedChatConfig.serverHeads);
    }

    public static boolean isServer(UUID sender, ChatMessageMetadata metadata) {
        return metadata != null ? metadata.source == ChatSource.SERVER : sender == null;
    }

    public static boolean showMessageHead(UUID sender, ChatMessageMetadata metadata, boolean first,
                                          UUID olderSender, ChatMessageMetadata olderMetadata, boolean hasOlder) {
        if (!first) return false;
        boolean server = isServer(sender, metadata);
        boolean grouped = server ? EnhancedChatConfig.serverHeadGroupFirst : EnhancedChatConfig.playerHeadGroupFirst;
        return !grouped || !hasOlder || !ChatHeadGrouping.sameSender(sender, metadata, olderSender, olderMetadata);
    }

    public static void renderMessage(UUID sender, ChatMessageMetadata metadata, int x, int y, float opacity) {
        if (!EnhancedChatConfig.enabled || opacity <= 0) return;
        if (isServer(sender, metadata)) {
            if (EnhancedChatConfig.serverHeads) renderTexture(serverAvatar(), x, y, opacity);
        } else if (sender != null) {
            render(sender, x, y, opacity);
        }
    }

    private ChatHeadRenderer() {}

    public static int textOffset() {
        return headsEnabled() ? TEXT_OFFSET : 0;
    }

    public static void render(UUID senderId, int x, int y, float opacity) {
        if (!EnhancedChatFeatures.playerHeads() || opacity <= 0.0F) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (senderId == null) return;
        if (minecraft.getConnection() == null) return;
        NetworkPlayerInfo player = minecraft.getConnection().getPlayerInfo(senderId);
        if (player == null) return;
        render(player, x, y, opacity);
    }

    /** Draws a completion-list face independently of the message-head visibility setting. */
    public static void renderCandidate(String candidate, int x, int y, float opacity) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (candidate == null || minecraft.getConnection() == null || opacity <= 0.0F) return;
        String playerName = candidate.startsWith("@") ? candidate.substring(1) : candidate;
        NetworkPlayerInfo player = minecraft.getConnection().getPlayerInfo(playerName);
        if (player != null) render(player, x, y, opacity);
    }

    private static void render(NetworkPlayerInfo player, int x, int y, float opacity) {
        renderTexture(player.getLocationSkin(), x, y, opacity);
    }

    private static void renderTexture(ResourceLocation skin, int x, int y, float opacity) {
        Minecraft minecraft = Minecraft.getMinecraft();

        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        minecraft.getTextureManager().bindTexture(skin);
        if (EnhancedChatFeatures.headShadow()) {
            GlStateManager.color(0.25F, 0.25F, 0.25F, opacity);
            drawAvatar(skin, x + 1, y + 1);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, opacity);
        drawAvatar(skin, x, y);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawAvatar(ResourceLocation skin, int x, int y) {
        if (SERVER_AVATAR.equals(skin) || TOWER.equals(skin) || STORAGE.equals(skin) || NARROW.equals(skin)) {
            Gui.drawScaledCustomSizeModalRect(x, y, 0.0F, 0.0F, 64, 64,
                    HEAD_SIZE, HEAD_SIZE, 64.0F, 64.0F);
        } else {
            drawFace(x, y);
        }
    }

    public static void renderVanilla(List<ChatLine> lines, int scrollPos, int updateCounter,
                                     int lineCount, float chatScale) {
        if (!headsEnabled() || lines.isEmpty()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.gameSettings.chatVisibility == EntityPlayer.EnumChatVisibility.HIDDEN) return;
        boolean open = minecraft.ingameGUI.getChatGUI().getChatOpen();
        float configuredOpacity = minecraft.gameSettings.chatOpacity * 0.9F + 0.1F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(2.0F, 8.0F, 0.0F);
        GlStateManager.scale(chatScale, chatScale, 1.0F);
        for (int row = 0; row + scrollPos < lines.size() && row < lineCount; row++) {
            ChatLine line = lines.get(row + scrollPos);
            if (!(line instanceof ChatHeadLineMetadata)) continue;
            ChatHeadLineMetadata metadata = (ChatHeadLineMetadata) line;
            int olderIndex = row + scrollPos + 1;
            ChatHeadLineMetadata older = olderIndex < lines.size() && lines.get(olderIndex) instanceof ChatHeadLineMetadata
                    ? (ChatHeadLineMetadata) lines.get(olderIndex) : null;
            if (!showMessageHead(metadata.nfrUi$getSenderId(), metadata.nfrUi$getMessageMetadata(),
                    metadata.nfrUi$isFirstFragment(), older == null ? null : older.nfrUi$getSenderId(),
                    older == null ? null : older.nfrUi$getMessageMetadata(), older != null)) continue;
            int age = updateCounter - line.getUpdatedCounter();
            if (age >= 200 && !open) continue;
            double fade = MathHelper.clamp((1.0D - age / 200.0D) * 10.0D, 0.0D, 1.0D);
            int alpha = open ? 255 : (int) (255.0D * fade * fade);
            alpha = (int) (alpha * configuredOpacity);
            if (alpha <= 3) continue;
            int y = ChatInlineLayout.bottomAlignedY(lines, scrollPos, row, HEAD_SIZE,
                    minecraft.fontRenderer);
            renderMessage(metadata.nfrUi$getSenderId(), metadata.nfrUi$getMessageMetadata(), 0, y, alpha / 255.0F);
        }
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void drawFace(int x, int y) {
        Gui.drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8, 8,
                HEAD_SIZE, HEAD_SIZE, 64.0F, 64.0F);
        Gui.drawScaledCustomSizeModalRect(x, y, 40.0F, 8.0F, 8, 8,
                HEAD_SIZE, HEAD_SIZE, 64.0F, 64.0F);
    }
}
