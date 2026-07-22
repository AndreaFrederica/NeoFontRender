package neofontrender.addons.vendor.tabbychat.foundation.text;

import net.minecraft.util.IChatComponent;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.ChatComponentText;

import java.util.Iterator;
import java.util.List;

public class FancyTextComponent implements IChatComponent {

    private final IChatComponent text;
    private FancyTextStyle style;

    public FancyTextComponent(IChatComponent parent) {
        if (parent instanceof FancyTextComponent)
            throw new IllegalArgumentException("Parent text cannot be fancy");
        this.text = parent;
    }

    public FancyTextComponent(String string) {
        this(new ChatComponentText(string));
    }

    @Override
    public String getUnformattedTextForChat() {
        return text.getUnformattedTextForChat();
    }

    @Override
    public IChatComponent createCopy() {
        IChatComponent text = this.text.createCopy();
        FancyTextComponent fcc = new FancyTextComponent(text);
        fcc.setFancyStyle(getFancyStyle().createCopy());
        return fcc;
    }

    @Override
    public IChatComponent appendSibling(IChatComponent component) {
        text.appendSibling(component);
        return this;
    }

    @Override
    public IChatComponent appendText(String text) {
        this.text.appendText(text);
        return this;
    }

    @Override
    public ChatStyle getChatStyle() {
        return text.getChatStyle();
    }

    @Override
    public IChatComponent setChatStyle(ChatStyle style) {
        text.setChatStyle(style);
        return this;
    }

    @Override
    public String getFormattedText() {
        return text.getFormattedText();
    }

    @Override
    public List<IChatComponent> getSiblings() {
        return text.getSiblings();
    }

    @Override
    public String getUnformattedText() {
        return text.getUnformattedText();
    }

    @Override
    public Iterator<IChatComponent> iterator() {
        final Iterator<IChatComponent> iterator = text.iterator();
        return new Iterator<IChatComponent>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public IChatComponent next() {
                IChatComponent next = iterator.next();
                return next instanceof FancyTextComponent
                        ? next
                        : new FancyTextComponent(next).setFancyStyle(getFancyStyle());
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    public IChatComponent getText() {
        return text;
    }

    public FancyTextStyle getFancyStyle() {
        if (this.style == null)
            this.style = new FancyTextStyle();
        return this.style;
    }

    public FancyTextComponent setFancyStyle(FancyTextStyle style) {
        this.style = style;
        return this;
    }

    @Override
    public String toString() {
        return String.format("FancyText{text=%s, fancystyle=%s}", text, style);
    }
}
