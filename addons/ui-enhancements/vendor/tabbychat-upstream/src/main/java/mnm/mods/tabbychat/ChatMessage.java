package mnm.mods.tabbychat;

import java.util.Calendar;
import java.util.Date;

import com.google.gson.annotations.Expose;

import mnm.mods.tabbychat.api.Message;
import mnm.mods.tabbychat.settings.GeneralSettings;
import mnm.mods.tabbychat.util.TimeStamps;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import neofontrender.addons.chat.ChatHeadResolver;

import java.util.UUID;

public class ChatMessage implements Message {

    @Expose
    private ITextComponent message;
    @Expose
    private int id;
    private transient int counter;
    private transient UUID nfrUi$senderId;
    private transient boolean nfrUi$senderResolved;
    private transient boolean nfrUi$firstFragment = true;
    @Expose
    private Date date;

    public ChatMessage(int updatedCounter, ITextComponent chat, int id, boolean isNew) {
        this(updatedCounter, chat, id, isNew, null, true, false);
    }

    public ChatMessage(int updatedCounter, ITextComponent chat, int id, boolean isNew,
                       UUID senderId, boolean firstFragment) {
        this(updatedCounter, chat, id, isNew, senderId, firstFragment, true);
    }

    private ChatMessage(int updatedCounter, ITextComponent chat, int id, boolean isNew,
                        UUID senderId, boolean firstFragment, boolean senderResolved) {
        // super(updatedCounter, chat, id);
        this.message = chat;
        this.id = id;
        this.counter = updatedCounter;
        this.nfrUi$senderId = senderId;
        this.nfrUi$senderResolved = senderResolved;
        this.nfrUi$firstFragment = firstFragment;
        if (isNew) {
            this.date = Calendar.getInstance().getTime();
        }
    }

    public ChatMessage(ChatLine chatline) {
        this(chatline.getUpdatedCounter(), chatline.getChatComponent(), chatline.getChatLineID(), true);
    }

    @Override
    public ITextComponent getMessage() {
        return this.message;
    }

    @Override
    public ITextComponent getMessageWithOptionalTimestamp() {
        ITextComponent chat;
        GeneralSettings settings = TabbyChat.getInstance().settings.general;
        if (date != null && settings.timestampChat.get()) {
            chat = new TextComponentString("");

            TimeStamps stamp = settings.timestampStyle.get();
            TextFormatting format = settings.timestampColor.get();
            chat = new TextComponentTranslation("%s %s", format + stamp.format(date), getMessage());
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

    public UUID nfrUi$getSenderId() {
        if (!nfrUi$senderResolved) {
            nfrUi$senderId = ChatHeadResolver.detect(getMessage());
            nfrUi$senderResolved = true;
        }
        return nfrUi$senderId;
    }

    public boolean nfrUi$isFirstFragment() {
        return nfrUi$firstFragment;
    }

}
