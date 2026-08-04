package neofontrender.addons.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.math.MathHelper;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Adds and renders an inline icon for standard SHOW_ITEM chat components. */
public final class ChatItemIconRenderer {
    private static final String MARKER_TAG = "nfrUiChatItemIcon";
    private static final String PADDING = "  ";
    private static final float ICON_SCALE = 0.5F;
    private static final int CACHE_LIMIT = 4096;
    // Minecraft 1.12 Style.hashCode() throws when optional style fields are null, so chat
    // components must never be used in an equality-based map.
    private static final Map<ITextComponent, ItemStack> ITEM_CACHE = new IdentityHashMap<>();

    private ChatItemIconRenderer() {}

    public static ITextComponent decorate(ITextComponent component) {
        if (!EnhancedChatFeatures.itemIcons() || component == null || containsMarker(component)) {
            return component;
        }
        return decorateNode(component, null);
    }

    public static void renderLine(ITextComponent component, int x, int y) {
        if (!EnhancedChatFeatures.itemIcons() || component == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        int cursor = x;
        for (ITextComponent part : component) {
            String text = GuiUtilRenderComponents.removeTextColorsIfConfigured(
                    part.getUnformattedComponentText(), false);
            int width = minecraft.fontRenderer.getStringWidth(text);
            ItemStack stack = markerStack(part);
            if (!stack.isEmpty()) {
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

        GlStateManager.pushMatrix();
        GlStateManager.translate(2.0F, 8.0F, 0.0F);
        GlStateManager.scale(chatScale, chatScale, 1.0F);
        for (int row = 0; row + scrollPos < lines.size() && row < lineCount; row++) {
            ChatLine line = lines.get(row + scrollPos);
            int age = updateCounter - line.getUpdatedCounter();
            if (age >= 200 && !open) continue;
            double fade = MathHelper.clamp((1.0D - age / 200.0D) * 10.0D, 0.0D, 1.0D);
            if (!open && fade * fade * minecraft.gameSettings.chatOpacity <= 0.02D) continue;
            int y = ChatInlineLayout.bottomAlignedY(lines, scrollPos, row, 8,
                    minecraft.fontRenderer);
            renderLine(line.getChatComponent(), ChatHeadRenderer.textOffset(), y);
        }
        GlStateManager.popMatrix();
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
    }

    private static ITextComponent decorateNode(ITextComponent source, HoverEvent parentHover) {
        ITextComponent copy = source.createCopy();
        copy.getSiblings().clear();
        HoverEvent hover = source.getStyle().getHoverEvent();
        for (ITextComponent sibling : source.getSiblings()) {
            copy.appendSibling(decorateNode(sibling, hover));
        }

        if (!sameItemHover(hover, parentHover)) {
            ITextComponent marker = createMarker(hover);
            if (marker != null) {
                ITextComponent wrapper = new TextComponentString("");
                wrapper.appendSibling(marker);
                wrapper.appendSibling(copy);
                return wrapper;
            }
        }
        return copy;
    }

    private static ITextComponent createMarker(HoverEvent hover) {
        NBTTagCompound tag = showItemTag(hover);
        if (tag == null) return null;
        tag.setBoolean(MARKER_TAG, true);
        ITextComponent marker = new TextComponentString(PADDING);
        marker.setStyle(new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                new TextComponentString(tag.toString()))));
        return marker;
    }

    private static boolean sameItemHover(HoverEvent hover, HoverEvent parentHover) {
        if (!isShowItem(hover)) return true;
        return isShowItem(parentHover) && hover.getValue().getUnformattedText()
                .equals(parentHover.getValue().getUnformattedText());
    }

    private static boolean containsMarker(ITextComponent component) {
        for (ITextComponent part : component) {
            HoverEvent hover = part.getStyle().getHoverEvent();
            if (isShowItem(hover) && hover.getValue().getUnformattedText().contains(MARKER_TAG)) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack markerStack(ITextComponent component) {
        ItemStack cached = ITEM_CACHE.get(component);
        if (cached != null) return cached;
        NBTTagCompound tag = showItemTag(component.getStyle().getHoverEvent());
        ItemStack stack = ItemStack.EMPTY;
        if (tag != null && tag.getBoolean(MARKER_TAG)) {
            tag.removeTag(MARKER_TAG);
            stack = new ItemStack(tag);
            if (stack.isEmpty()) stack = ItemStack.EMPTY;
        }
        if (ITEM_CACHE.size() >= CACHE_LIMIT) ITEM_CACHE.clear();
        ITEM_CACHE.put(component, stack);
        return stack;
    }

    private static NBTTagCompound showItemTag(HoverEvent hover) {
        if (!isShowItem(hover)) return null;
        try {
            return JsonToNBT.getTagFromJson(hover.getValue().getUnformattedText());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isShowItem(HoverEvent hover) {
        return hover != null && hover.getAction() == HoverEvent.Action.SHOW_ITEM
                && hover.getValue() != null;
    }

    private static void renderStack(ItemStack stack, int x, int y) {
        Minecraft minecraft = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(x, y, 0.0F);
            GlStateManager.scale(ICON_SCALE, ICON_SCALE, ICON_SCALE);
            RenderHelper.enableGUIStandardItemLighting();
            minecraft.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void restoreChatDrawingState() {
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
