package neofontrender.addons.vendor.tabbychat;

import java.util.Calendar;
import java.util.Date;

import com.google.gson.annotations.Expose;

import neofontrender.addons.chat.ChatHeadResolver;
import neofontrender.addons.vendor.tabbychat.api.Message;
import neofontrender.addons.vendor.tabbychat.settings.GeneralSettings;
import neofontrender.addons.vendor.tabbychat.util.TimeStamps;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

public class ChatMessage implements Message {

    @Expose
    private IChatComponent message;
    @Expose
    private int id;
    private transient int counter;
    // 1.7.10 resolves player heads by account name instead of UUID.
    private transient String nfrUi$senderName;
    private transient boolean nfrUi$senderResolved;
    private transient boolean nfrUi$firstFragment = true;
    @Expose
    private Date date;

    public ChatMessage(int updatedCounter, IChatComponent chat, int id, boolean isNew) {
        this(updatedCounter, chat, id, isNew, null, true, false);
    }

    public ChatMessage(int updatedCounter, IChatComponent chat, int id, boolean isNew,
                       String senderName, boolean firstFragment) {
        this(updatedCounter, chat, id, isNew, senderName, firstFragment, true);
    }

    private ChatMessage(int updatedCounter, IChatComponent chat, int id, boolean isNew,
                        String senderName, boolean firstFragment, boolean senderResolved) {
        // super(updatedCounter, chat, id);
        this.message = chat;
        this.id = id;
        this.counter = updatedCounter;
        this.nfrUi$senderName = senderName;
        this.nfrUi$senderResolved = senderResolved;
        this.nfrUi$firstFragment = firstFragment;
        if (isNew) {
            this.date = Calendar.getInstance().getTime();
        }
    }

    public ChatMessage(ChatLine chatline) {
        this(chatline.getUpdatedCounter(), chatline.func_151461_a(), chatline.getChatLineID(), true);
    }

    static ChatMessage restored(int updatedCounter, IChatComponent chat, int id, Date date) {
        ChatMessage message = new ChatMessage(updatedCounter, chat, id, false);
        message.date = date;
        return message;
    }

    @Override
    public IChatComponent getMessage() {
        return this.message;
    }

    @Override
    public IChatComponent getMessageWithOptionalTimestamp() {
        IChatComponent chat;
        GeneralSettings settings = TabbyChat.getInstance().settings.general;
        if (date != null && settings.timestampChat.get()) {
            chat = new ChatComponentText("");

            TimeStamps stamp = settings.timestampStyle.get();
            EnumChatFormatting format = settings.timestampColor.get();
            chat = new ChatComponentTranslation("%s %s", format + stamp.format(date), getMessage());
        } else {
            chat = getMessage();
        }
        return chat;
    }

    @Override
    public int getCounter() {
        return this.counter;
    }

    @Override
    public int getID() {
        return this.id;
    }

    @Override
    public Date getDate() {
        return this.date;
    }

    public String nfrUi$getSenderName() {
        if (!nfrUi$senderResolved) {
            nfrUi$senderName = ChatHeadResolver.detect(getMessage());
            nfrUi$senderResolved = true;
        }
        return nfrUi$senderName;
    }

    public boolean nfrUi$isFirstFragment() {
        return nfrUi$firstFragment;
    }

}
