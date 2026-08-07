package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiPlayerInfo;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import neofontrender.addons.mixin.AccessorGuiChatFeatures;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ChatPlayerActions {
    private ChatPlayerActions() {}

    static String findPlayer(String selectedText) {
        Minecraft minecraft = Minecraft.getMinecraft();
        NetHandlerPlayClient connection = minecraft.getNetHandler();
        if (connection == null) return null;
        Map<String, String> names = new LinkedHashMap<>();
        for (GuiPlayerInfo player : connection.playerInfoList) {
            String name = player.name;
            names.put(name, name);
        }
        if (minecraft.theWorld != null) {
            for (Object entry : minecraft.theWorld.playerEntities) {
                EntityPlayer player = (EntityPlayer) entry;
                String accountName = player.getCommandSenderName();
                names.put(accountName, accountName);
                String displayName = player.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) names.put(displayName, accountName);
            }
        }
        String clean = EnumChatFormatting.getTextWithoutFormattingCodes(selectedText);
        return ChatPlayerNameMatcher.find(clean == null ? "" : clean, names);
    }

    public static void startPrivateMessage(String player) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!(minecraft.currentScreen instanceof GuiChat)) return;
        GuiTextField input = ((AccessorGuiChatFeatures) minecraft.currentScreen).nfrUi$getInputField();
        if (input == null) return;
        String template = EnhancedChatConfig.privateMessageCommand == null
                ? "/msg {player}" : EnhancedChatConfig.privateMessageCommand.trim();
        String command = template.contains("{player}") ? template.replace("{player}", player)
                : template + " " + player;
        input.setText(command + " ");
        input.setCursorPositionEnd();
        input.setFocused(true);
    }

    static void mention(String player) {
        GuiTextField input = input();
        if (input == null) return;
        input.writeText("@" + player + " ");
        input.setFocused(true);
    }

    static void copyName(String player) {
        GuiScreen.setClipboardString(player);
    }

    static void mute(String player) {
        EnhancedChatConfig.mutedPlayers = ChatRuleMatcher.addName(EnhancedChatConfig.mutedPlayers, player);
        EnhancedChatConfig.save();
    }

    private static GuiTextField input() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (!(minecraft.currentScreen instanceof GuiChat)) return null;
        return ((AccessorGuiChatFeatures) minecraft.currentScreen).nfrUi$getInputField();
    }
}
