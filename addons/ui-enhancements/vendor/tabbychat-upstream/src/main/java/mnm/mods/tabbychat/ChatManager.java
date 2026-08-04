package mnm.mods.tabbychat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import mnm.mods.tabbychat.api.Channel;
import mnm.mods.tabbychat.api.ChannelStatus;
import mnm.mods.tabbychat.api.Chat;
import mnm.mods.tabbychat.api.Message;
import mnm.mods.tabbychat.gui.ChatBox;
import mnm.mods.tabbychat.gui.TextBox;
import mnm.mods.tabbychat.settings.AdvancedSettings;
import mnm.mods.tabbychat.settings.GeneralServerSettings;
import mnm.mods.util.Location;
import mnm.mods.util.config.ValueMap;
import mnm.mods.util.text.TextBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumTypeAdapterFactory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import neofontrender.addons.chat.ChatSourceChannels;
import neofontrender.addons.chat.EnhancedChatConfigAccess;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.TypeUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ChatManager implements Chat {

    public static final int MAX_CHAT_LENGTH = 256;

    private Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .registerTypeHierarchyAdapter(ITextComponent.class, new ITextComponent.Serializer())
            .registerTypeAdapter(Style.class, new Style.Serializer())
            .registerTypeAdapterFactory(new EnumTypeAdapterFactory())
            .create();

    private ChatBox chatbox;

    private Map<String, Channel> allChannels = Maps.newHashMap();
    private Map<String, Channel> allPms = Maps.newHashMap();
    private List<Channel> channels = Lists.newLinkedList();
    private Channel active = ChatChannel.DEFAULT_CHANNEL;

    private Map<Channel, List<Message>> messages = Maps.newHashMap();
    private Map<String, String> drafts = Maps.newHashMap();
    private Map<String, Boolean> privateCommandBlocks = Maps.newHashMap();

    public ChatManager(TabbyChat tc) {
        AdvancedSettings settings = tc.settings.advanced;
        int x = settings.chatX.get();
        int y = settings.chatY.get();
        int width = settings.chatW.get();
        int height = settings.chatH.get();

        this.chatbox = new ChatBox(new Location(x, y, width, height));

        if (!this.channels.contains(ChatChannel.DEFAULT_CHANNEL)) {
            this.channels.add(ChatChannel.DEFAULT_CHANNEL);
            chatbox.getTray().addChannel(ChatChannel.DEFAULT_CHANNEL);
        }
    }

    @Override
    public Channel getChannel(String name) {
        return getChannel(name, false);
    }

    @Override
    public Channel getChannel(String name, boolean pm) {
        return pm ? getPmChannel(name) : getChatChannel(name);
    }

    private Channel getChatChannel(String name) {
        return getChannel(name, false, this.allChannels, TabbyChat.getInstance().serverSettings.channels);
    }

    private Channel getPmChannel(String name) {
        Channel channel = getChannel(name, true, this.allPms, TabbyChat.getInstance().serverSettings.pms);
        if (channel.getPrefix().isEmpty()) {
            channel.setPrefix(EnhancedChatConfigAccess.privateCommandPrefix(name));
        }
        syncPrivateCommandMode(channel);
        return channel;
    }

    /** PM-style channel for a chat group, sending through /nfrgroup &lt;name&gt;. */
    public Channel getGroupChannel(String groupName) {
        String name = "__uie_group_" + groupName;
        Channel channel = getPmChannel(name);
        channel.setAlias(groupName);
        String expected = "/nfrgroup " + groupName;
        if (!channel.getPrefix().startsWith("/nfrgroup ")) {
            channel.setPrefix(expected);
        }
        return channel;
    }

    private Channel getChannel(String name, boolean pm, Map<String, Channel> from, ValueMap<ChatChannel> setting) {
        if (!from.containsKey(name)) {
            // fetch from settings
            ChatChannel chan = setting.get(name);
            if (chan == null || chan.getName() == null) {
                chan = new ChatChannel(name, pm);
                setting.get().put(chan.getName(), chan);
            }
            from.put(name, chan);
            messages.put(chan, chan.getMessages());
        }
        return from.get(name);
    }

    @Override
    public void addChannel(Channel channel) {
        if (!this.channels.contains(channel)) {
            this.channels.add(channel);
            chatbox.getTray().addChannel(channel);
        }
        save();
    }

    @Override
    public void removeChannel(Channel channel) {
        if (channels.contains(channel) && !channel.equals(ChatChannel.DEFAULT_CHANNEL)) {
            channels.remove(channel);
            chatbox.getTray().removeChannel(channel);
        }
        if (getActiveChannel() == channel) {
            setActiveChannel(ChatChannel.DEFAULT_CHANNEL);
        }
        save();
    }

    @Override
    public List<Channel> getChannels() {
        return ImmutableList.copyOf(channels);
    }

    @Override
    public void removeMessages(int id) {
        for (Channel channel : this.channels) {
            channel.removeMessages(id);
        }
        save();
    }

    @Override
    public void clearMessages() {
        for (Channel channel : channels) {
            channel.clear();
        }

        this.channels.clear();
        this.channels.add(ChatChannel.DEFAULT_CHANNEL);

        chatbox.getTray().clear();
    }

    @Override
    public Channel getActiveChannel() {
        return active;
    }

    @Override
    public void setActiveChannel(Channel channel) {
        TextBox text = chatbox.getChatInput();
        saveDraft(active, text.getText());
        syncPrivateCommandMode(channel);
        if (channel.isPm() && EnhancedChatConfigAccess.privateCommandBlockEnabled()) {
            privateCommandBlocks.put(draftKey(channel), true);
        }
        // set max text length
        boolean hidden = channel.isPrefixHidden();
        int prefLength = hidden ? channel.getPrefix().length() + 1 : 0;

        text.getTextField().getTextField().setMaxStringLength(MAX_CHAT_LENGTH - prefLength);

        // reset scroll
        // TODO per-channel scroll settings?
        if (channel != active) {
            chatbox.getChatArea().resetScroll();
        }
        active.setStatus(null);
        active = channel;
        active.setStatus(ChannelStatus.ACTIVE);
        restoreActiveInput("");

        runActivationCommand(channel);

    }

    public void captureActiveDraft() {
        saveDraft(active, chatbox.getChatInput().getText());
    }

    public void restoreActiveInput(String explicitText) {
        syncPrivateCommandMode(active);
        String value = explicitText == null ? "" : explicitText;
        if (value.isEmpty() && active != null && active.isPm()
                && EnhancedChatConfigAccess.privateCommandBlockEnabled()) {
            privateCommandBlocks.put(draftKey(active), true);
        }
        if (value.isEmpty()) {
            value = drafts.get(draftKey(active));
            if (value == null) value = defaultInput(active);
        }
        chatbox.getChatInput().getTextField().getTextField().setText(value);
    }

    public void clearActiveDraft() {
        clearDraft(active);
    }

    public void clearDraft(Channel channel) {
        if (channel == null) return;
        drafts.remove(draftKey(channel));
        if (channel.isPm()) privateCommandBlocks.put(draftKey(channel), true);
        if (channel == active) {
            chatbox.getChatInput().getTextField().getTextField().setText(defaultInput(channel));
        }
    }

    public boolean hasActivePrivateCommandBlock() {
        syncPrivateCommandMode(active);
        return active != null && active.isPm()
                && EnhancedChatConfigAccess.privateCommandBlockEnabled()
                && privateCommandBlocks.getOrDefault(draftKey(active), true);
    }

    public String getActivePrivateCommandPrefix() {
        return hasActivePrivateCommandBlock() ? active.getPrefix() : "";
    }

    public boolean removeActivePrivateCommandBlock() {
        if (!hasActivePrivateCommandBlock()) return false;
        privateCommandBlocks.put(draftKey(active), false);
        return true;
    }

    public String applyActivePrivateCommand(String message) {
        String body = message == null ? "" : message;
        if (!hasActivePrivateCommandBlock() || body.trim().isEmpty()
                || body.trim().startsWith("/")) return body;
        return active.getPrefix() + " " + body;
    }

    public String activeInputText(String sentMessage) {
        String message = sentMessage == null ? "" : sentMessage;
        String prefix = getActivePrivateCommandPrefix();
        return !prefix.isEmpty() && message.startsWith(prefix + " ")
                ? message.substring(prefix.length() + 1) : message;
    }

    private void saveDraft(Channel channel, String value) {
        if (channel == null) return;
        String text = value == null ? "" : value;
        if (text.equals(defaultInput(channel))) drafts.remove(draftKey(channel));
        else drafts.put(draftKey(channel), text);
    }

    private static String defaultInput(Channel channel) {
        return channel != null && !(channel.isPm()
                && EnhancedChatConfigAccess.privateCommandBlockEnabled())
                && !channel.isPrefixHidden() && !channel.getPrefix().isEmpty()
                ? channel.getPrefix() + " " : "";
    }

    private static void syncPrivateCommandMode(Channel channel) {
        if (channel != null && channel.isPm()) {
            channel.setPrefixHidden(EnhancedChatConfigAccess.privateCommandBlockEnabled());
        }
    }

    private static String draftKey(Channel channel) {
        if (channel == null || channel == ChatChannel.DEFAULT_CHANNEL
                || ChatSourceChannels.isSourceChannel(channel)) return "__uie_public";
        return (channel.isPm() ? "pm:" : "channel:") + channel.getName();
    }

    private void runActivationCommand(Channel channel) {
        if (ChatSourceChannels.isSourceChannel(channel)) return;
        String cmd = channel.getCommand();
        if (cmd.isEmpty()) {

            GeneralServerSettings settings = TabbyChat.getInstance().serverSettings.general;
            String pat = channel.isPm() ? settings.messageCommand.get() : settings.channelCommand.get();

            if (pat.isEmpty()) {
                return;
            }
            String name = channel.getName();
            if (channel == ChatChannel.DEFAULT_CHANNEL) {
                name = TabbyChat.getInstance().serverSettings.general.defaultChannel.get();
            }
            // insert the channel name
            cmd = pat.replace("{}", name);

        }
        if (cmd.startsWith("/")) {
            if (cmd.length() > MAX_CHAT_LENGTH) {
                cmd = cmd.substring(0, MAX_CHAT_LENGTH);
            }
            Minecraft.getMinecraft().player.sendChatMessage(cmd);
        }
    }

    private boolean loading;

    public void loadFrom(File dir) throws IOException {
        loading = true;
        try {
            loadFrom_(dir);
        } finally {
            loading = false;
        }
    }

    private synchronized void loadFrom_(File dir) throws IOException {
        File file = new File(dir, "data.gz");
        clearMessages();
        drafts.clear();
        privateCommandBlocks.clear();
        allChannels.clear();
        allPms.clear();
        if (!file.exists()) {
            return;
        }
        InputStream fin = null;
        InputStream gzin = null;
        String data;
        try {
            fin = new FileInputStream(file);
            gzin = new GzipCompressorInputStream(fin);
            data = IOUtils.toString(gzin, Charsets.UTF_8);
        } finally {
            IOUtils.closeQuietly(fin);
            IOUtils.closeQuietly(gzin);
        }

        JsonObject root = gson.fromJson(data, JsonObject.class);
        Type type = TypeUtils.parameterize(List.class, ChatMessage.class);

        List<Message> def = gson.fromJson(root.get("default"), type);
        ChatChannel.DEFAULT_CHANNEL.getMessages().addAll(def);

        JsonObject chans = root.get("chans").getAsJsonObject();
        readJson(chans, false);
        JsonObject pms = root.get("pms").getAsJsonObject();
        readJson(pms, true);

        // active channels
        JsonObject active = root.get("active").getAsJsonObject();
        JsonArray achans = active.get("chans").getAsJsonArray();
        for (JsonElement e : achans) {
            addChannel(getChannel(e.getAsString(), false));
        }
        JsonArray apms = active.get("pms").getAsJsonArray();
        for (JsonElement e : apms) {
            addChannel(getChannel(e.getAsString(), true));
        }

        String time;
        if (root.has("datetime")) {
            Instant datetime = Instant.ofEpochSecond(root.get("datetime").getAsLong());
            time = datetime.toString();
        } else {
            time = "UNKNOWN";
        }
        ITextComponent chat = new TextBuilder()
                .text(I18n.format("tabbychat.message.loadchatdata", time))
                .format(TextFormatting.GRAY)
                .build();
        for (Channel c : getChannels()) {
            if (!c.getMessages().isEmpty()) {
                c.addMessage(chat, -1);
            }
        }
    }

    private void readJson(JsonObject obj, boolean pm) {
        for (Entry<String, JsonElement> entry : obj.entrySet()) {
            Channel chan = getChannel(entry.getKey(), pm);
            Type type = TypeUtils.parameterize(List.class, ChatMessage.class);
            List<Message> list = gson.fromJson(entry.getValue(), type);
            chan.getMessages().addAll(list);
        }
    }

    public void save() {}

    void saveing(){
        if (loading) {
            return;
        }
        try {
            saveTo(TabbyChat.getInstance().serverSettings.getFile().getParentFile());
        } catch (IOException e) {
            TabbyChat.getLogger().warn("Error while saving chat data", e);
        }
    }

    private synchronized void saveTo(File dir) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("datetime", Instant.now().getEpochSecond());
        root.add("default", gson.toJsonTree(ChatChannel.DEFAULT_CHANNEL.getMessages()));

        JsonObject chans = new JsonObject();
        root.add("chans", chans);
        JsonObject pms = new JsonObject();
        root.add("pms", pms);

        for (Channel c : messages.keySet()) {
            JsonObject obj = c.isPm() ? pms : chans;
            obj.add(c.getName(), gson.toJsonTree(c.getMessages()));
        }

        // active channels
        JsonObject active = new JsonObject();
        root.add("active", active);

        JsonArray apms = new JsonArray();
        JsonArray achans = new JsonArray();
        active.add("chans", achans);
        active.add("pms", apms);

        for (Channel c : channels) {
            if (c == ChatChannel.DEFAULT_CHANNEL) {
                continue;
            }
            JsonArray array = c.isPm() ? apms : achans;
            array.add(new JsonPrimitive(c.getName()));
        }

        OutputStream fout = null;
        GzipCompressorOutputStream gzout = null;
        try {
            File file = new File(dir, "data.gz");
            file.getParentFile().mkdirs();
            fout = new FileOutputStream(file);
            gzout = new GzipCompressorOutputStream(fout);
            String data = gson.toJson(root);
            IOUtils.write(data, gzout, Charsets.UTF_8);
            gzout.finish();
        } finally {
            IOUtils.closeQuietly(fout);
            IOUtils.closeQuietly(gzout);
        }
    }

    public ChatBox getChatBox() {
        return this.chatbox;
    }
}
