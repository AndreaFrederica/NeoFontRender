package neofontrender.addons.vendor.tabbychat;

import java.util.Calendar;
import java.util.Date;

import com.google.gson.annotations.Expose;

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
    @Expose
    private Date date;

    public ChatMessage(int updatedCounter, IChatComponent chat, int id, boolean isNew) {
        // super(updatedCounter, chat, id);
        this.message = chat;
        this.id = id;
        this.counter = updatedCounter;
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

}
