package neofontrender.addons.chat;

import neofontrender.addons.chat.network.ChatCharacterNetwork;
import neofontrender.addons.chat.network.ChatCharacterPolicy;
import neofontrender.addons.chat.network.ChatPolicySession;
import neofontrender.addons.mixin.AccessorGuiChatFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.text.TextComponentTranslation;

public final class ClientChatPolicy {
    private static final ChatPolicySession SESSION = new ChatPolicySession();
    private static long lastWarning;

    private ClientChatPolicy() {}

    public static void initialize() {
        ChatCharacterNetwork.initialize();
        ChatCharacterNetwork.setClientListener((handler, request, version, allowed) ->
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    NetHandlerPlayClient current = Minecraft.getMinecraft().getConnection();
                    if (current != null && current == handler) SESSION.accept(current.getNetworkManager(), request, version, allowed);
                }));
    }

    public static void connected(NetworkManager manager) {
        long request = SESSION.begin(manager);
        Minecraft.getMinecraft().addScheduledTask(() -> {
            NetHandlerPlayClient current = Minecraft.getMinecraft().getConnection();
            if (current != null && current.getNetworkManager() == manager
                    && manager.isChannelOpen() && SESSION.matches(manager, request)) {
                ChatCharacterNetwork.probe(request);
            }
        });
    }

    public static void disconnected(NetworkManager manager) { SESSION.disconnect(manager); }

    public static boolean allowsSectionSign(NetworkManager manager) {
        return manager != null && manager.isChannelOpen()
                && EnhancedChatConfigAccess.allowSectionSignInput() && SESSION.allowsSectionSign(manager);
    }

    public static boolean allowsSectionSignInput(GuiTextField field) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.currentScreen instanceof GuiChat)
                || ((AccessorGuiChatFeatures) mc.currentScreen).nfrUi$getInputField() != field) return false;
        NetHandlerPlayClient current = mc.getConnection();
        return current != null && allowsSectionSign(current.getNetworkManager());
    }

    public static boolean canSend(NetworkManager manager, String message) {
        if (ChatCharacterPolicy.isValid(message, allowsSectionSign(manager))) return true;
        Minecraft.getMinecraft().addScheduledTask(() -> {
            Minecraft mc = Minecraft.getMinecraft();
            long now = System.nanoTime();
            if (mc.player != null && (lastWarning == 0 || now - lastWarning >= 1_000_000_000L)) {
                lastWarning = now;
                mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation(
                        ChatCharacterPolicy.containsSectionSign(message)
                                ? "neofontrender_ui_enhancements.chat.input.section_sign_blocked"
                                : "neofontrender_ui_enhancements.chat.input.illegal_characters"));
            }
        });
        return false;
    }
}
