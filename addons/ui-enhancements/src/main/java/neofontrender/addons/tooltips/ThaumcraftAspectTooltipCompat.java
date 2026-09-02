package neofontrender.addons.tooltips;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Replaces TC6's whitespace-based Aspect tooltip component with an NFR-owned visual block. */
public final class ThaumcraftAspectTooltipCompat {
    static final int ICON_SIZE = 16;
    static final int CELL_SIZE = 18;
    private static final int LABEL_MAX_WIDTH = 15;
    private static final ThreadLocal<ChartCache> CACHE = ThreadLocal.withInitial(ChartCache::new);
    private static volatile AspectMethods aspectMethods;
    private static volatile boolean aspectMethodsResolved;

    private ThaumcraftAspectTooltipCompat() {}

    /** Called before TC can append transport whitespace. */
    public static boolean replaceOriginal(ItemTooltipEvent event) {
        if (!ownsModernTooltip() || event == null || event.getItemStack() == null) return false;
        Minecraft minecraft = Minecraft.getMinecraft();
        AspectMethods methods = aspectMethods();
        return minecraft != null && minecraft.currentScreen instanceof GuiContainer
                && !Mouse.isGrabbed() && methods != null
                && entries(methods, event.getItemStack()) != null;
    }

    public static boolean replaceOriginalDrawing(ItemStack stack) {
        AspectMethods methods = aspectMethods();
        return ownsModernTooltip() && aspectDisplayRequested() && methods != null
                && stack != null && entries(methods, stack) != null;
    }

    static Chart chart(ItemStack stack, FontRenderer font, int maxWidth) {
        if (!ownsModernTooltip() || !aspectDisplayRequested() || stack == null || stack.isEmpty()) {
            return null;
        }
        AspectMethods methods = aspectMethods();
        if (methods == null) return null;
        List<Entry> entries = entries(methods, stack);
        if (entries == null || entries.isEmpty()) return null;
        return Chart.arrange(entries, methods.largeTagText(), maxWidth);
    }

    private static boolean ownsModernTooltip() {
        return ThaumcraftTooltipCompat.isEnabled()
                && !HeiTooltipCompat.isCustomTooltipActive()
                && !(TooltipConfig.yieldToLegendaryTooltips
                && Loader.isModLoaded("legendarytooltips"));
    }

    private static boolean aspectDisplayRequested() {
        Minecraft minecraft = Minecraft.getMinecraft();
        AspectMethods methods = aspectMethods();
        if (minecraft == null || !(minecraft.currentScreen instanceof GuiContainer)
                || Mouse.isGrabbed() || methods == null) return false;
        return GuiScreen.isShiftKeyDown() != methods.showTags();
    }

    static void draw(Chart chart, int x, int y, FontRenderer font) {
        if (chart == null || font == null) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft == null || minecraft.getTextureManager() == null) return;

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.pushMatrix();
        try {
            GlStateManager.disableLighting();
            GlStateManager.disableDepth();
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);

            for (int index = 0; index < chart.entries.size(); index++) {
                Entry entry = chart.entries.get(index);
                int iconX = x + index % chart.columns * CELL_SIZE;
                int iconY = y + index / chart.columns * CELL_SIZE + 1;
                minecraft.getTextureManager().bindTexture(entry.image);
                GlStateManager.color((entry.color >> 16 & 0xFF) / 255.0F,
                        (entry.color >> 8 & 0xFF) / 255.0F,
                        (entry.color & 0xFF) / 255.0F, 1.0F);
                Gui.drawModalRectWithCustomSizedTexture(
                        iconX, iconY, 0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            for (int index = 0; index < chart.entries.size(); index++) {
                Entry entry = chart.entries.get(index);
                if (entry.amount <= 0) continue;
                int iconX = x + index % chart.columns * CELL_SIZE;
                int iconY = y + index / chart.columns * CELL_SIZE + 1;
                drawAmount(font, Integer.toString(entry.amount), iconX, iconY,
                        chart.largeTagText);
            }
        } finally {
            GlStateManager.popMatrix();
            GL11.glPopAttrib();
        }
    }

    private static void drawAmount(FontRenderer font, String text, int iconX, int iconY,
                                   boolean largeTagText) {
        int measured = Math.max(1, font.getStringWidth(text));
        float preferredScale = largeTagText ? 1.0F : 0.5F;
        float scale = Math.min(preferredScale, LABEL_MAX_WIDTH / (float) measured);
        float labelWidth = measured * scale;
        float labelHeight = font.FONT_HEIGHT * scale;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(iconX + ICON_SIZE - labelWidth,
                    iconY + ICON_SIZE - labelHeight, 0.0F);
            GlStateManager.scale(scale, scale, 1.0F);
            font.drawStringWithShadow(text, 0.0F, 0.0F, 0xFFFFFFFF);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private static List<Entry> entries(AspectMethods methods, ItemStack stack) {
        ChartCache cached = CACHE.get();
        Item item = stack.getItem();
        int metadata = stack.getMetadata();
        int tagHash = stack.hasTagCompound() ? stack.getTagCompound().hashCode() : 0;
        if (cached.matches(item, metadata, tagHash)) return cached.entries;
        try {
            Object aspectList = methods.getObjectTags.invoke(null, stack);
            if (aspectList == null) {
                cached.update(item, metadata, tagHash, Collections.emptyList());
                return cached.entries;
            }
            Object aspects = methods.getAspectsSortedByAmount.invoke(aspectList);
            if (aspects == null || !aspects.getClass().isArray()) {
                cached.update(item, metadata, tagHash, Collections.emptyList());
                return cached.entries;
            }
            List<Entry> result = new ArrayList<>(Array.getLength(aspects));
            for (int i = 0; i < Array.getLength(aspects); i++) {
                Object aspect = Array.get(aspects, i);
                if (aspect == null) continue;
                Object image = methods.getImage.invoke(aspect);
                Object color = methods.getColor.invoke(aspect);
                Object amount = methods.getAmount.invoke(aspectList, aspect);
                if (image instanceof ResourceLocation && color instanceof Number) {
                    result.add(new Entry((ResourceLocation) image,
                            ((Number) color).intValue(),
                            amount instanceof Number ? ((Number) amount).intValue() : 0));
                }
            }
            result = Collections.unmodifiableList(result);
            cached.update(item, metadata, tagHash, result);
            return result;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static AspectMethods aspectMethods() {
        if (aspectMethodsResolved) return aspectMethods;
        synchronized (ThaumcraftAspectTooltipCompat.class) {
            if (aspectMethodsResolved) return aspectMethods;
            try {
                ClassLoader loader = ThaumcraftAspectTooltipCompat.class.getClassLoader();
                Class<?> manager = Class.forName(
                        "thaumcraft.common.lib.crafting.ThaumcraftCraftingManager", false, loader);
                Class<?> aspectList = Class.forName(
                        "thaumcraft.api.aspects.AspectList", false, loader);
                Class<?> aspect = Class.forName(
                        "thaumcraft.api.aspects.Aspect", false, loader);
                Class<?> graphics = Class.forName(
                        "thaumcraft.common.config.ModConfig$CONFIG_GRAPHICS", false, loader);
                aspectMethods = new AspectMethods(
                        manager.getMethod("getObjectTags", ItemStack.class),
                        aspectList.getMethod("getAspectsSortedByAmount"),
                        aspectList.getMethod("getAmount", aspect),
                        aspect.getMethod("getImage"),
                        aspect.getMethod("getColor"),
                        graphics.getField("showTags"),
                        graphics.getField("largeTagText"));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                aspectMethods = null;
            }
            aspectMethodsResolved = true;
            return aspectMethods;
        }
    }

    static final class Chart implements TooltipVisualBlock {
        final List<Entry> entries;
        final int columns;
        final int width;
        final int height;
        final boolean largeTagText;

        private Chart(List<Entry> entries, int columns, boolean largeTagText) {
            this.entries = entries;
            this.columns = columns;
            this.width = Math.min(entries.size(), columns) * CELL_SIZE;
            this.height = ((entries.size() + columns - 1) / columns) * CELL_SIZE;
            this.largeTagText = largeTagText;
        }

        static Chart arrange(List<Entry> entries, boolean largeTagText, int maxWidth) {
            return new Chart(entries, columnsFor(entries.size(), maxWidth), largeTagText);
        }

        @Override
        public Chart constrain(int maxWidth) {
            int constrained = columnsFor(entries.size(), maxWidth);
            return constrained == columns ? this : new Chart(entries, constrained, largeTagText);
        }

        @Override
        public int width() {
            return width;
        }

        @Override
        public int height() {
            return height;
        }

        @Override
        public void draw(int x, int y, FontRenderer font) {
            ThaumcraftAspectTooltipCompat.draw(this, x, y, font);
        }

        @Override
        public String debugLabel() {
            return "aspect";
        }
    }

    static int[] dimensionsFor(int entryCount, int maxWidth) {
        if (entryCount <= 0) return new int[]{0, 0};
        int columns = columnsFor(entryCount, maxWidth);
        return new int[]{Math.min(entryCount, columns) * CELL_SIZE,
                ((entryCount + columns - 1) / columns) * CELL_SIZE};
    }

    private static int columnsFor(int entryCount, int maxWidth) {
        int availableColumns = Math.max(1, maxWidth / CELL_SIZE);
        return Math.max(1, Math.min(entryCount, availableColumns));
    }

    static final class Entry {
        final ResourceLocation image;
        final int color;
        final int amount;

        Entry(ResourceLocation image, int color, int amount) {
            this.image = image;
            this.color = color;
            this.amount = amount;
        }
    }

    private static final class AspectMethods {
        final Method getObjectTags;
        final Method getAspectsSortedByAmount;
        final Method getAmount;
        final Method getImage;
        final Method getColor;
        final Field showTags;
        final Field largeTagText;

        AspectMethods(Method getObjectTags, Method getAspectsSortedByAmount, Method getAmount,
                      Method getImage, Method getColor, Field showTags, Field largeTagText) {
            this.getObjectTags = getObjectTags;
            this.getAspectsSortedByAmount = getAspectsSortedByAmount;
            this.getAmount = getAmount;
            this.getImage = getImage;
            this.getColor = getColor;
            this.showTags = showTags;
            this.largeTagText = largeTagText;
        }

        boolean showTags() {
            try {
                return showTags.getBoolean(null);
            } catch (IllegalAccessException | LinkageError ignored) {
                return true;
            }
        }

        boolean largeTagText() {
            try {
                return largeTagText.getBoolean(null);
            } catch (IllegalAccessException | LinkageError ignored) {
                return false;
            }
        }
    }

    private static final class ChartCache {
        Item item;
        int metadata;
        int tagHash;
        List<Entry> entries;

        boolean matches(Item item, int metadata, int tagHash) {
            return entries != null && this.item == item && this.metadata == metadata
                    && this.tagHash == tagHash;
        }

        void update(Item item, int metadata, int tagHash, List<Entry> entries) {
            this.item = item;
            this.metadata = metadata;
            this.tagHash = tagHash;
            this.entries = entries;
        }
    }
}
