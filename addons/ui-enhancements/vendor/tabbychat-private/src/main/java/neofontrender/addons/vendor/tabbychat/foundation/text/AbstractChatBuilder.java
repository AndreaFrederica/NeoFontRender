package neofontrender.addons.vendor.tabbychat.foundation.text;

import neofontrender.addons.vendor.tabbychat.foundation.Color;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;

public abstract class AbstractChatBuilder implements ITextBuilder {

    protected IChatComponent current;

    @Override
    public ITextBuilder format(EnumChatFormatting f) {
        checkCreated();
        if (f.isColor()) {
            current.getChatStyle().setColor(f);
        } else if (f.isFancyStyling()) {
            if (f == EnumChatFormatting.BOLD) {
                current.getChatStyle().setBold(true);
            } else if (f == EnumChatFormatting.ITALIC) {
                current.getChatStyle().setItalic(true);
            } else if (f == EnumChatFormatting.UNDERLINE) {
                current.getChatStyle().setUnderlined(true);
            } else if (f == EnumChatFormatting.STRIKETHROUGH) {
                current.getChatStyle().setStrikethrough(true);
            } else if (f == EnumChatFormatting.OBFUSCATED) {
                current.getChatStyle().setObfuscated(true);
            }
        } else if (f == EnumChatFormatting.RESET) {
            current.setChatStyle(new ChatStyle());
        }
        return this;
    }

    @Override
    public ITextBuilder color(Color color) {
        asFancy().getFancyStyle().setColor(color);
        return this;
    }

    @Override
    public ITextBuilder underline(Color color) {
        asFancy().getFancyStyle().setUnderline(color);
        return this;
    }

    @Override
    public ITextBuilder highlight(Color color) {
        asFancy().getFancyStyle().setHighlight(color);
        return this;
    }

    private FancyTextComponent asFancy() {
        if (!(current instanceof FancyTextComponent)) {
            current = new FancyTextComponent(current);
        }
        return (FancyTextComponent) current;
    }

    @Override
    public ITextBuilder click(ClickEvent event) {
        checkCreated();
        current.getChatStyle().setChatClickEvent(event);
        return this;
    }

    @Override
    public ITextBuilder hover(HoverEvent event) {
        checkCreated();
        current.getChatStyle().setChatHoverEvent(event);
        return this;
    }

    @Override
    public ITextBuilder insertion(String insertion) {
        checkCreated();
        asFancy().getFancyStyle().setInsertion(insertion);
        return this;
    }

    private void checkCreated() {
        if (current == null) {
            throw new IllegalStateException("A chat component has not been created yet.");
        }
    }

    @Override
    public ITextBuilder score(String player, String objective) {
        return append(new ChatComponentText(player + ":" + objective));
    }

    @Override
    public ITextBuilder text(String text) {
        return append(new ChatComponentText(text));
    }

    @Override
    public ITextBuilder selector(Selector selector) {
        return append(new ChatComponentText(selector.toString()));
    }

    @Override
    public ITextBuilder translation(String key) {
        return new TranslationBuilder(this, key);
    }

    @Override
    public ITextBuilder quickTranslate(String key) {
        return translation(key).end();
    }

}
