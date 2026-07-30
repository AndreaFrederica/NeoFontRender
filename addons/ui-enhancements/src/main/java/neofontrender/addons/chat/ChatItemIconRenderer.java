package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.event.HoverEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;
import org.lwjgl.opengl.GL11;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Adds and renders an inline icon for standard SHOW_ITEM chat components. */
public final class ChatItemIconRenderer {
    private static final String MARKER_TAG = "nfrUiChatItemIcon";
    private static final String PADDING = "  ";
    private static final float ICON_SCALE = 0.5F;
    private static final int CACHE_LIMIT = 4096;
    // ChatStyle.hashCode() must not be relied on for identity lookups of chat components,
    // so an identity-based map is used instead of an equality-based one.
    private static final Map<IChatComponent, ItemStack> ITEM_CACHE = new IdentityHashMap<>();

    private ChatItemIconRenderer() {}

    public static IChatComponent decorate(IChatComponent component) {
        if (!EnhancedChatFeatures.itemIcons() || component == null || containsMarker(component)) {
            return component;
        }
        return decorateNode(component, null);
    }

    public static void renderLine(IChatComponent component, int x, int y) {
        if (!EnhancedChatFeatures.itemIcons() || component == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        int cursor = x;
        for (Iterator<?> parts = component.iterator(); parts.hasNext();) {
            IChatComponent part = (IChatComponent) parts.next();
            String text = part.getUnformattedTextForChat();
            int width = minecraft.fontRenderer.getStringWidth(text);
            ItemStack stack = markerStack(part);
            if (stack != null) {
                renderStack(stack, cursor + Math.max(0, (width - 8) / 2), y);
            }
            cursor += width;
        }
        restoreChatDrawingState();
    }

    public static void renderVanilla(List<ChatLine> lines, int scrollPos, int updateCounter,
                                     int lineCount, float chatScale) {
        if (!EnhancedChatFeatures.itemIcons() || lines.isEmpty()) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        boolean open = minecraft.ingameGUI.getChatGUI().getChatOpen();

        GL11.glPushMatrix();
        GL11.glTranslatef(2.0F, 20.0F, 0.0F);
        GL11.glScalef(chatScale, chatScale, 1.0F);
        for (int row = 0; row + scrollPos < lines.size() && row < lineCount; row++) {
            ChatLine line = lines.get(row + scrollPos);
            int age = updateCounter - line.getUpdatedCounter();
            if (age >= 200 && !open) continue;
            double fade = (1.0D - age / 200.0D) * 10.0D;
            if (fade < 0.0D) fade = 0.0D;
            if (fade > 1.0D) fade = 1.0D;
            if (!open && fade * fade * minecraft.gameSettings.chatOpacity <= 0.02D) continue;
            int y = -row * minecraft.fontRenderer.FONT_HEIGHT - 8;
            renderLine(line.func_151461_a(), ChatHeadRenderer.textOffset(), y);
        }
        GL11.glPopMatrix();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_BLEND);
    }

    private static IChatComponent decorateNode(IChatComponent source, HoverEvent parentHover) {
        IChatComponent copy = source.createCopy();
        copy.getSiblings().clear();
        HoverEvent hover = source.getChatStyle().getChatHoverEvent();
        for (IChatComponent sibling : source.getSiblings()) {
            copy.appendSibling(decorateNode(sibling, hover));
        }

        if (!sameItemHover(hover, parentHover)) {
            IChatComponent marker = createMarker(hover);
            if (marker != null) {
                IChatComponent wrapper = new ChatComponentText("");
                wrapper.appendSibling(marker);
                wrapper.appendSibling(copy);
                return wrapper;
            }
        }
        return copy;
    }

    private static IChatComponent createMarker(HoverEvent hover) {
        NBTTagCompound tag = showItemTag(hover);
        if (tag == null) return null;
        tag.setBoolean(MARKER_TAG, true);
        IChatComponent marker = new ChatComponentText(PADDING);
        marker.setChatStyle(new ChatStyle().setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                new ChatComponentText(tag.toString()))));
        return marker;
    }

    private static boolean sameItemHover(HoverEvent hover, HoverEvent parentHover) {
        if (!isShowItem(hover)) return true;
        return isShowItem(parentHover) && hover.getValue().getUnformattedText()
                .equals(parentHover.getValue().getUnformattedText());
    }

    private static boolean containsMarker(IChatComponent component) {
        for (Iterator<?> parts = component.iterator(); parts.hasNext();) {
            IChatComponent part = (IChatComponent) parts.next();
            HoverEvent hover = part.getChatStyle().getChatHoverEvent();
            if (isShowItem(hover) && hover.getValue().getUnformattedText().contains(MARKER_TAG)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack markerStack(IChatComponent component) {
        if (ITEM_CACHE.containsKey(component)) return ITEM_CACHE.get(component);
        NBTTagCompound tag = showItemTag(component.getChatStyle().getChatHoverEvent());
        ItemStack stack = null;
        if (tag != null && tag.getBoolean(MARKER_TAG)) {
            tag.removeTag(MARKER_TAG);
            stack = ItemStack.loadItemStackFromNBT(tag);
        }
        if (ITEM_CACHE.size() >= CACHE_LIMIT) ITEM_CACHE.clear();
        ITEM_CACHE.put(component, stack);
        return stack;
    }

    private static NBTTagCompound showItemTag(HoverEvent hover) {
        if (!isShowItem(hover)) return null;
        try {
            NBTBase parsed = JsonToNBT.func_150315_a(hover.getValue().getUnformattedText());
            return parsed instanceof NBTTagCompound ? (NBTTagCompound) parsed : null;
        } catch (NBTException exception) {
            // Malformed item NBT simply renders no icon, mirroring vanilla's hover tooltip.
            return null;
        }
    }

    private static boolean isShowItem(HoverEvent hover) {
        return hover != null && hover.getAction() == HoverEvent.Action.SHOW_ITEM
                && hover.getValue() != null;
    }

    private static void renderStack(ItemStack stack, int x, int y) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(x, y, 0.0F);
            GL11.glScalef(ICON_SCALE, ICON_SCALE, ICON_SCALE);
            RenderHelper.enableGUIStandardItemLighting();
            RenderItem.getInstance().renderItemAndEffectIntoGUI(
                    minecraft.fontRenderer, minecraft.getTextureManager(), stack, 0, 0);
        } finally {
            RenderHelper.disableStandardItemLighting();
            GL11.glPopMatrix();
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void restoreChatDrawingState() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
