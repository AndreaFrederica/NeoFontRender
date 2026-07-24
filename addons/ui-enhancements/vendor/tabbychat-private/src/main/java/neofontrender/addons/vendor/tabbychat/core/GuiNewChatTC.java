package neofontrender.addons.vendor.tabbychat.core;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Runnables;
import neofontrender.addons.chat.ChatAnimationController;
import neofontrender.addons.vendor.tabbychat.ChatChannel;
import neofontrender.addons.vendor.tabbychat.ChatManager;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.api.Channel;
import neofontrender.addons.vendor.tabbychat.api.ChannelStatus;
import neofontrender.addons.vendor.tabbychat.api.events.ChatMessageEvent.ChatReceivedEvent;
import neofontrender.addons.vendor.tabbychat.api.gui.ChatScreen;
import neofontrender.addons.vendor.tabbychat.gui.ChatBox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import neofontrender.addons.vendor.tabbychat.foundation.render.GlState;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjglx.input.Mouse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class GuiNewChatTC extends GuiNewChat implements ChatScreen {

    private static GuiNewChatTC instance;

    private Minecraft mc;

    private TabbyChat tc = TabbyChat.getInstance();
    private ChatManager chat;
    private EventBus bus;

    private int prevScreenWidth;
    private int prevScreenHeight;

    public GuiNewChatTC(Minecraft minecraft, ChatManager manager) {
        super(minecraft);
        mc = minecraft;
        chat = manager;
        bus = new EventBus();
        instance = this;

        this.prevScreenHeight = mc.displayHeight;
    }

    public static GuiNewChatTC getInstance() {
        return instance;
    }

    @Override
    public void refreshChat() {
        chat.getChatBox().updateComponent();
    }

    @Override
    public void clearChatMessages() {
        clearChatMessages(false);
    }

    public void clearChatMessages(boolean sent) {
        checkThread(() -> {
            chat.clearMessages();
            if (sent) {
                this.getSentMessages().clear();
            }
        }).run();
    }

    @Override
    public void drawChat(int i) {
        if (prevScreenHeight != mc.displayHeight || prevScreenWidth != mc.displayWidth) {

            chat.getChatBox().onScreenHeightResize(prevScreenWidth, prevScreenHeight, mc.displayWidth, mc.displayHeight);

            prevScreenWidth = mc.displayWidth;
            prevScreenHeight = mc.displayHeight;
        }

//        if (getChatOpen())
//            return;

        ChatBox chatbox = chat.getChatBox();
        float scale = chatbox.getScale();

        GlState.popMatrix(); // ignore what GuiIngame did.
        GlState.pushMatrix();

        // Scale it accordingly
        GlState.scale(scale, scale, 1.0F);

        // Make the upper left corner of the panel (0,0).
        GlState.translate(chatbox.getBounds().x, chatbox.getBounds().y, 0.0F);

        int mouseX = Mouse.getEventX();
        int mouseY = -Mouse.getEventY() - 1;
        chatbox.drawComponent(mouseX, mouseY);

        GlState.popMatrix();
        GlState.pushMatrix(); // push to avoid gl errors
    }

    @Override
    public void printChatMessageWithOptionalDeletion(IChatComponent ichat, int id) {
        checkThread(() -> this.addMessage(ichat, id)).run();
    }

    public void addMessage(IChatComponent ichat, int id) {
        // chat listeners
        ChatReceivedEvent chatevent = new ChatReceivedEvent(ichat, id);
        chatevent.channels.add(ChatChannel.DEFAULT_CHANNEL);
        MinecraftForge.EVENT_BUS.post(chatevent);
        // chat filters
        ichat = chatevent.text;
        id = chatevent.id;
        if (ichat != null && !ichat.getUnformattedText().isEmpty()) {
            ChatAnimationController.messageAdded();
            if (id != 0) {
                // send removable msg to current channel
                chatevent.channels.clear();
                chatevent.channels.add(this.chat.getActiveChannel());
            }
            if (chatevent.channels.contains(ChatChannel.DEFAULT_CHANNEL) && chatevent.channels.size() > 1
                    && !tc.serverSettings.general.useDefaultTab.get()) {
                chatevent.channels.remove(ChatChannel.DEFAULT_CHANNEL);
            }
            boolean msg = !chatevent.channels.contains(this.chat.getActiveChannel());
            final Set<String> ignored = Sets.newHashSet(this.tc.serverSettings.general.ignoredChannels.get());
            Set<Channel> channels = chatevent.channels.stream()
                    .filter(it -> !ignored.contains(it.getName()))
                    .collect(Collectors.toSet());
            for (Channel channel : channels) {
                channel.addMessage(ichat, id);
                if (msg) {
                    channel.setStatus(ChannelStatus.UNREAD);
                }
            }
            TabbyChat.getLogger().info("[CHAT] " + ichat.getUnformattedText());
            this.chat.getChatBox().updateComponent();
        }
    }

    @Override
    public void deleteChatLine(int id) {
        checkThread(() -> chat.removeMessages(id)).run();
    }

    private Runnable checkThread(Runnable runnable) {
        if (!mc.func_152345_ab()) {
            mc.func_152344_a(runnable);
            TabbyChat.getLogger().warn("Tried to modify chat from thread {}. To prevent a crash, it has been scheduled on the main thread.", Thread.currentThread().getName(), new Exception());
            return Runnables.doNothing();
        }
        return runnable;
    }

    @Override
    public void resetScroll() {
        chat.getChatBox().getChatArea().resetScroll();
        super.resetScroll();
    }

    @Nonnull
    @Override
    public List<String> getSentMessages() {
        return super.getSentMessages();
    }

    public ChatManager getChatManager() {
        return chat;
    }

    public IChatComponent getChatComponent(int clickX, int clickY) {
        return chat.getChatBox().getChatArea().getChatComponent(clickX, clickY);
    }

    public int getChatHeight() {
        return chat.getChatBox().getChatArea().getBounds().height;
    }

    public int getChatWidth() {
        return chat.getChatBox().getChatArea().getBounds().width;
    }

    public float getChatScale() {
        return func_146244_h();
    }

    public int getLineCount() {
        return Math.max(1, getChatHeight() / mc.fontRenderer.FONT_HEIGHT);
    }

    @Override
    public EventBus getBus() {
        return bus;
    }

}
