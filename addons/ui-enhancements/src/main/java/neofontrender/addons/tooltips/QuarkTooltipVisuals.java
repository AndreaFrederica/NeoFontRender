package neofontrender.addons.tooltips;

import com.google.common.collect.Multimap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTippedArrow;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import org.lwjgl.opengl.GL11;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Full modern-tooltip replacement for Quark's three whitespace-driven visual features. */
public final class QuarkTooltipVisuals {
    private static final int ICON_SIZE = 9;
    private static final int ROW_HEIGHT = 10;
    private static final ResourceLocation QUARK_ICONS =
            new ResourceLocation("quark", "textures/misc/general_icons.png");
    private static final String[] ATTRIBUTES = {
            "generic.attackDamage", "generic.attackSpeed", "generic.reachDistance",
            "generic.armor", "generic.armorToughness", "generic.knockbackResistance",
            "generic.maxHealth", "generic.movementSpeed", "generic.luck"
    };
    private static final List<String> PERCENT_ATTRIBUTES = Arrays.asList(
            "generic.knockbackResistance", "generic.luck");
    private static final List<String> MULTIPLIER_ATTRIBUTES =
            Collections.singletonList("generic.movementSpeed");
    private static final List<String> POTION_MULTIPLIER_ATTRIBUTES =
            Collections.singletonList("generic.attackSpeed");
    private static final ThreadLocal<Capture> CAPTURE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> MODERN_POST_TEXT_DEPTH = new ThreadLocal<>();
    private static volatile QuarkMethods methods;
    private static volatile boolean methodsResolved;
    private static volatile Field foodPotionEffectField;
    private static volatile boolean foodPotionEffectFieldResolved;

    private QuarkTooltipVisuals() {}

    /** Quark's food handler contributes only transport whitespace, so modern mode owns it fully. */
    public static boolean replaceFoodTooltip(ItemTooltipEvent event) {
        if (!ownsModernTooltip() || event == null || event.getItemStack() == null
                || event.getItemStack().isEmpty()
                || !(event.getItemStack().getItem() instanceof ItemFood)) return false;
        capture(event.getItemStack()).food = true;
        return true;
    }

    /** Quark's book handler contributes only transport whitespace, so modern mode owns it fully. */
    public static boolean replaceEnchantedBookTooltip(ItemTooltipEvent event) {
        if (!ownsModernTooltip() || event == null || !isEnchantedBook(event.getItemStack())
                || Minecraft.getMinecraft().player == null) return false;
        capture(event.getItemStack()).enchantedBook = true;
        return true;
    }

    /** Called at VisualStatDisplay's final List.add after Quark has removed vanilla attributes. */
    public static boolean replaceVisualStatPlaceholder(ItemTooltipEvent event) {
        if (!ownsModernTooltip() || event == null || event.getItemStack() == null
                || event.getItemStack().isEmpty()) return false;
        capture(event.getItemStack()).visualStats = true;
        return true;
    }

    public static void beginModernPostText() {
        Integer depth = MODERN_POST_TEXT_DEPTH.get();
        MODERN_POST_TEXT_DEPTH.set(depth == null ? 1 : depth + 1);
    }

    public static void endModernPostText() {
        Integer depth = MODERN_POST_TEXT_DEPTH.get();
        if (depth == null || depth <= 1) MODERN_POST_TEXT_DEPTH.remove();
        else MODERN_POST_TEXT_DEPTH.set(depth - 1);
    }

    public static boolean replaceOriginalDrawing() {
        Integer depth = MODERN_POST_TEXT_DEPTH.get();
        return depth != null && depth > 0;
    }

    static void populate(TooltipVisualPlan plan, ItemStack stack, List<String> lines,
                         FontRenderer font, int maxWidth) {
        Capture captured = CAPTURE.get();
        CAPTURE.remove();
        if (captured == null || stack == null || stack.isEmpty()
                || !ItemStack.areItemStacksEqual(captured.stack, stack)) return;

        int available = Math.max(1, maxWidth);
        if (captured.visualStats) {
            EquipmentBlock block = EquipmentBlock.create(stack, font, available);
            if (block != null) plan.addAfter(0, block);
        }
        if (captured.food) {
            FoodBlock block = FoodBlock.create(stack, available);
            if (block != null) plan.addAfter(0, block);
        }
        if (captured.enchantedBook) addEnchantedBookBlocks(plan, stack, lines, available);
    }

    private static boolean ownsModernTooltip() {
        return TooltipConfig.enabled && Arc3DRuntimeSupport.isAvailable()
                && !HeiTooltipCompat.isCustomTooltipActive();
    }

    private static Capture capture(ItemStack stack) {
        Capture captured = CAPTURE.get();
        if (captured == null || !ItemStack.areItemStacksEqual(captured.stack, stack)) {
            captured = new Capture(stack);
            CAPTURE.set(captured);
        }
        return captured;
    }

    private static void addEnchantedBookBlocks(TooltipVisualPlan plan, ItemStack stack,
                                                List<String> lines, int maxWidth) {
        QuarkMethods resolved = methods();
        if (resolved == null) return;
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        boolean[] used = new boolean[lines.size()];
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey();
            if (enchantment == null) continue;
            List<ItemStack> items = resolved.itemsFor(enchantment);
            if (items.isEmpty()) continue;
            String expected = stripped(enchantment.getTranslatedName(entry.getValue()));
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                if (!used[lineIndex] && expected.equals(stripped(lines.get(lineIndex)))) {
                    used[lineIndex] = true;
                    plan.addAfter(lineIndex, new EnchantedItemsBlock(items, maxWidth));
                    break;
                }
            }
        }
    }

    private static String stripped(String text) {
        String stripped = TextFormatting.getTextWithoutFormattingCodes(text);
        return stripped == null ? "" : stripped;
    }

    private static boolean isEnchantedBook(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() == Items.ENCHANTED_BOOK) return true;
        ResourceLocation id = Item.REGISTRY.getNameForObject(stack.getItem());
        return id != null && "quark".equals(id.getNamespace())
                && "ancient_tome".equals(id.getPath());
    }

    private static int divisor() {
        QuarkMethods resolved = methods();
        return resolved == null ? 2 : resolved.divisor();
    }

    static int[] foodDimensionsFor(int iconCount, int maxWidth) {
        if (iconCount <= 0) return new int[]{0, 0};
        int columns = Math.max(1, Math.min(iconCount,
                Math.max(1, Math.max(1, maxWidth) / ICON_SIZE)));
        return new int[]{columns * ICON_SIZE,
                ((iconCount + columns - 1) / columns) * ROW_HEIGHT};
    }

    static int[] enchantedItemDimensionsFor(int itemCount, int maxWidth) {
        if (itemCount <= 0) return new int[]{0, 0};
        int columns = Math.max(1, Math.min(itemCount,
                Math.max(1, (Math.max(1, maxWidth) - 8) / 9 + 1)));
        return new int[]{(columns - 1) * 9 + 8,
                ((itemCount + columns - 1) / columns) * 9};
    }

    private static boolean isPoisonous(ItemFood food) {
        Field field = foodPotionEffectField();
        if (field == null) return false;
        try {
            Object value = field.get(food);
            return value instanceof PotionEffect
                    && ((PotionEffect) value).getPotion().isBadEffect();
        } catch (IllegalAccessException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static Field foodPotionEffectField() {
        if (foodPotionEffectFieldResolved) return foodPotionEffectField;
        synchronized (QuarkTooltipVisuals.class) {
            if (foodPotionEffectFieldResolved) return foodPotionEffectField;
            for (Field field : ItemFood.class.getDeclaredFields()) {
                if (PotionEffect.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    foodPotionEffectField = field;
                    break;
                }
            }
            foodPotionEffectFieldResolved = true;
            return foodPotionEffectField;
        }
    }

    private static QuarkMethods methods() {
        if (methodsResolved) return methods;
        synchronized (QuarkTooltipVisuals.class) {
            if (methodsResolved) return methods;
            try {
                ClassLoader loader = QuarkTooltipVisuals.class.getClassLoader();
                Class<?> visualStats = Class.forName(
                        "vazkii.quark.client.feature.VisualStatDisplay", false, loader);
                Class<?> enchantedBooks = Class.forName(
                        "vazkii.quark.client.feature.EnchantedBooksShowItems", false, loader);
                Class<?> foodTooltip = Class.forName(
                        "vazkii.quark.client.feature.FoodTooltip", false, loader);
                methods = new QuarkMethods(
                        visualStats.getMethod("getModifiers", ItemStack.class,
                                EntityEquipmentSlot.class),
                        enchantedBooks.getMethod("getItemsForEnchantment", Enchantment.class),
                        foodTooltip.getField("divisor"));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                methods = null;
            }
            methodsResolved = true;
            return methods;
        }
    }

    private static double attributeValue(EntityPlayer player, EntityEquipmentSlot slot,
                                         ItemStack stack,
                                         Multimap<String, AttributeModifier> modifiers,
                                         String key) {
        if (player == null) return 0.0D;
        Collection<AttributeModifier> values = modifiers.get(key);
        if (values.isEmpty()) return 0.0D;

        double value = 0.0D;
        if (!PERCENT_ATTRIBUTES.contains(key)
                && (slot != null || !"generic.attackDamage".equals(key))) {
            IAttributeInstance attribute = player.getAttributeMap().getAttributeInstanceByName(key);
            if (attribute != null) value = attribute.getBaseValue();
        }
        for (AttributeModifier modifier : values) {
            if (modifier.getOperation() == 0) value += modifier.getAmount();
        }
        double rawValue = value;
        for (AttributeModifier modifier : values) {
            if (modifier.getOperation() == 1) value += rawValue * modifier.getAmount();
        }
        for (AttributeModifier modifier : values) {
            if (modifier.getOperation() == 2) value += value * modifier.getAmount();
        }
        if ("generic.attackDamage".equals(key) && slot == EntityEquipmentSlot.MAINHAND) {
            value += EnchantmentHelper.getModifierForCreature(stack, EnumCreatureAttribute.UNDEFINED);
        }
        return value;
    }

    private static String formatAttribute(String key, double value, EntityEquipmentSlot slot) {
        DecimalFormat format = ItemStack.DECIMALFORMAT;
        if (PERCENT_ATTRIBUTES.contains(key)) {
            return (value > 0.0D ? "+" : "") + format.format(value * 100.0D) + "%";
        }
        if (MULTIPLIER_ATTRIBUTES.contains(key)
                || (slot == null && POTION_MULTIPLIER_ATTRIBUTES.contains(key))) {
            return format.format(value / baseValue(key)) + "x";
        }
        return format.format(value);
    }

    private static double baseValue(String key) {
        if ("generic.movementSpeed".equals(key)) return 0.1D;
        if ("generic.attackSpeed".equals(key)) return 4.0D;
        return 1.0D;
    }

    private static int attributeTextureU(String key) {
        if ("generic.attackDamage".equals(key)) return 238;
        if ("generic.attackSpeed".equals(key)) return 247;
        if ("generic.reachDistance".equals(key)) return 193;
        if ("generic.armor".equals(key)) return 229;
        if ("generic.armorToughness".equals(key)) return 220;
        if ("generic.knockbackResistance".equals(key)) return 175;
        if ("generic.maxHealth".equals(key)) return 211;
        if ("generic.movementSpeed".equals(key)) return 184;
        if ("generic.luck".equals(key)) return 202;
        return 211;
    }

    private static final class Capture {
        final ItemStack stack;
        boolean visualStats;
        boolean enchantedBook;
        boolean food;

        Capture(ItemStack stack) {
            this.stack = stack;
        }
    }

    private static final class FoodBlock implements TooltipVisualBlock {
        final int pips;
        final int count;
        final int columns;
        final boolean poisonous;

        private FoodBlock(int pips, int count, int columns, boolean poisonous) {
            this.pips = pips;
            this.count = count;
            this.columns = columns;
            this.poisonous = poisonous;
        }

        static FoodBlock create(ItemStack stack, int maxWidth) {
            if (!(stack.getItem() instanceof ItemFood)) return null;
            ItemFood food = (ItemFood) stack.getItem();
            int pips = food.getHealAmount(stack);
            int count = Math.max(0, (pips + divisor() - 1) / divisor());
            if (count == 0) return null;
            int columns = foodDimensionsFor(count, maxWidth)[0] / ICON_SIZE;
            return new FoodBlock(pips, count, columns, isPoisonous(food));
        }

        @Override public int width() {
            return Math.min(count, columns) * ICON_SIZE;
        }

        @Override public int height() {
            return ((count + columns - 1) / columns) * ROW_HEIGHT;
        }

        @Override public String debugLabel() {
            return "quark-food";
        }

        @Override public void draw(int x, int y, FontRenderer font) {
            Minecraft minecraft = Minecraft.getMinecraft();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.enableTexture2D();
                GlStateManager.enableBlend();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                minecraft.getTextureManager().bindTexture(GuiIngameForge.ICONS);
                for (int index = 0; index < count; index++) {
                    int iconX = x + index % columns * ICON_SIZE;
                    int iconY = y + index / columns * ROW_HEIGHT;
                    int backgroundU = poisonous ? 133 : 16;
                    Gui.drawModalRectWithCustomSizedTexture(
                            iconX, iconY, backgroundU, 27, ICON_SIZE, ICON_SIZE, 256, 256);
                    int fillU = 52;
                    if ((pips & 1) != 0 && index == 0) fillU += 9;
                    if (poisonous) fillU += 36;
                    Gui.drawModalRectWithCustomSizedTexture(
                            iconX, iconY, fillU, 27, ICON_SIZE, ICON_SIZE, 256, 256);
                }
            } finally {
                GlStateManager.popMatrix();
                GL11.glPopAttrib();
            }
        }
    }

    private static final class EnchantedItemsBlock implements TooltipVisualBlock {
        private static final int ITEM_SIZE = 8;
        private static final int ITEM_STEP = 9;
        final List<ItemStack> items;
        final int columns;

        EnchantedItemsBlock(List<ItemStack> items, int maxWidth) {
            this.items = Collections.unmodifiableList(new ArrayList<>(items));
            this.columns = (enchantedItemDimensionsFor(items.size(), maxWidth)[0]
                    - ITEM_SIZE) / ITEM_STEP + 1;
        }

        @Override public int width() {
            return (Math.min(items.size(), columns) - 1) * ITEM_STEP + ITEM_SIZE;
        }

        @Override public int height() {
            return ((items.size() + columns - 1) / columns) * ITEM_STEP;
        }

        @Override public String debugLabel() {
            return "quark-enchant-items";
        }

        @Override public void draw(int x, int y, FontRenderer font) {
            Minecraft minecraft = Minecraft.getMinecraft();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GlStateManager.pushMatrix();
            try {
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.translate(x, y, minecraft.getRenderItem().zLevel);
                GlStateManager.scale(0.5F, 0.5F, 1.0F);
                for (int index = 0; index < items.size(); index++) {
                    minecraft.getRenderItem().renderItemIntoGUI(items.get(index),
                            index % columns * 18, index / columns * 18);
                }
            } finally {
                GlStateManager.popMatrix();
                GL11.glPopAttrib();
            }
        }
    }

    private static final class EquipmentBlock implements TooltipVisualBlock {
        final List<DrawLine> lines;
        final int width;

        private EquipmentBlock(List<DrawLine> lines, int width) {
            this.lines = lines;
            this.width = width;
        }

        static EquipmentBlock create(ItemStack stack, FontRenderer font, int maxWidth) {
            QuarkMethods resolved = methods();
            Minecraft minecraft = Minecraft.getMinecraft();
            if (resolved == null || minecraft == null || minecraft.player == null
                    || GuiScreen.isShiftKeyDown()) return null;

            EntityEquipmentSlot[] slots = EntityEquipmentSlot.values();
            slots = Arrays.copyOf(slots, slots.length + 1);
            List<Multimap<String, AttributeModifier>> modifiers = new ArrayList<>(slots.length);
            int attributeHash = 0;
            boolean allSame = true;
            boolean onlyInvalid = true;
            boolean showSlots = false;
            EntityEquipmentSlot primary = stack.getItem() instanceof ItemPotion
                    || stack.getItem() instanceof ItemTippedArrow
                    ? null : EntityLiving.getSlotForItemStack(stack);

            for (EntityEquipmentSlot slot : slots) {
                Multimap<String, AttributeModifier> slotModifiers = resolved.modifiers(stack, slot);
                if (slotModifiers == null) return null;
                modifiers.add(slotModifiers);
                if (slot == EntityEquipmentSlot.MAINHAND) attributeHash = slotModifiers.hashCode();
                else if (allSame && attributeHash != slotModifiers.hashCode()) allSame = false;
                for (String key : slotModifiers.keys()) {
                    if (isValidAttribute(key)) {
                        onlyInvalid = false;
                        if (slot != primary) showSlots = true;
                    }
                }
            }
            if (allSame) showSlots = false;
            else if (onlyInvalid) showSlots = true;

            List<List<Cell>> sourceRows = new ArrayList<>();
            for (int slotIndex = 0; slotIndex < slots.length; slotIndex++) {
                EntityEquipmentSlot slot = slots[slotIndex];
                Multimap<String, AttributeModifier> slotModifiers = modifiers.get(slotIndex);
                boolean any = false;
                for (String key : slotModifiers.keys()) {
                    if (attributeValue(minecraft.player, slot, stack, slotModifiers, key) != 0.0D) {
                        any = true;
                        break;
                    }
                }
                if (!any) continue;

                List<Cell> cells = new ArrayList<>();
                if (showSlots) cells.add(Cell.icon(
                        202 + (slot == null ? -1 : slot.ordinal()) * ICON_SIZE, 35));
                for (String key : ATTRIBUTES) {
                    double value = attributeValue(minecraft.player, slot, stack, slotModifiers, key);
                    if (value == 0.0D) continue;
                    String text = formatAttribute(key, value, slot);
                    int color = value < 0.0D
                            || (text.endsWith("x") && value / baseValue(key) < 1.0D)
                            ? 0xFF5555 : 0xFFFFFF;
                    cells.add(Cell.attribute(attributeTextureU(key), text, color, font));
                }
                for (String key : slotModifiers.keys()) {
                    if (!isValidAttribute(key)) {
                        cells.add(Cell.invalid(font));
                        break;
                    }
                }
                if (!cells.isEmpty()) sourceRows.add(cells);
                if (allSame) break;
            }
            if (sourceRows.isEmpty()) return null;
            return pack(sourceRows, Math.max(1, maxWidth));
        }

        private static EquipmentBlock pack(List<List<Cell>> rows, int maxWidth) {
            List<DrawLine> packed = new ArrayList<>();
            int blockWidth = 0;
            for (List<Cell> row : rows) {
                DrawLine line = new DrawLine();
                for (Cell cell : row) {
                    if (!line.cells.isEmpty() && line.cursor + cell.visualWidth > maxWidth) {
                        blockWidth = Math.max(blockWidth, line.right);
                        packed.add(line);
                        line = new DrawLine();
                    }
                    line.add(cell);
                }
                if (!line.cells.isEmpty()) {
                    blockWidth = Math.max(blockWidth, line.right);
                    packed.add(line);
                }
            }
            return new EquipmentBlock(Collections.unmodifiableList(packed), blockWidth);
        }

        @Override public int width() {
            return width;
        }

        @Override public int height() {
            return lines.size() * ROW_HEIGHT;
        }

        @Override public String debugLabel() {
            return "quark-equipment";
        }

        @Override public void draw(int x, int y, FontRenderer font) {
            Minecraft minecraft = Minecraft.getMinecraft();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GlStateManager.pushMatrix();
            try {
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.enableTexture2D();
                GlStateManager.enableBlend();
                minecraft.getTextureManager().bindTexture(QUARK_ICONS);
                for (int row = 0; row < lines.size(); row++) {
                    int rowY = y + row * ROW_HEIGHT;
                    for (PlacedCell placed : lines.get(row).cells) {
                        Cell cell = placed.cell;
                        int cellX = x + placed.x;
                        if (cell.textureU >= 0) {
                            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                            minecraft.getTextureManager().bindTexture(QUARK_ICONS);
                            Gui.drawModalRectWithCustomSizedTexture(cellX, rowY,
                                    cell.textureU, cell.textureV,
                                    ICON_SIZE, ICON_SIZE, 256, 256);
                        }
                        if (cell.text != null) {
                            font.drawStringWithShadow(cell.text,
                                    cellX + cell.textOffset, rowY + 1, cell.color);
                        }
                    }
                }
            } finally {
                GlStateManager.popMatrix();
                GL11.glPopAttrib();
            }
        }
    }

    private static boolean isValidAttribute(String key) {
        for (String attribute : ATTRIBUTES) {
            if (attribute.equals(key)) return true;
        }
        return false;
    }

    private static final class Cell {
        final int textureU;
        final int textureV;
        final String text;
        final int color;
        final int textOffset;
        final int visualWidth;
        final int advance;

        private Cell(int textureU, int textureV, String text, int color, int textOffset,
                     int visualWidth, int advance) {
            this.textureU = textureU;
            this.textureV = textureV;
            this.text = text;
            this.color = color;
            this.textOffset = textOffset;
            this.visualWidth = visualWidth;
            this.advance = advance;
        }

        static Cell icon(int u, int v) {
            return new Cell(u, v, null, 0, 0, ICON_SIZE, 20);
        }

        static Cell attribute(int u, String text, int color, FontRenderer font) {
            int textWidth = font.getStringWidth(text);
            return new Cell(u, 0, text, color, 12, 12 + textWidth, textWidth + 20);
        }

        static Cell invalid(FontRenderer font) {
            String text = "[+]";
            return new Cell(-1, 0, text, 0xFFFF55, 1,
                    1 + font.getStringWidth(text), 1 + font.getStringWidth(text));
        }
    }

    private static final class DrawLine {
        final List<PlacedCell> cells = new ArrayList<>();
        int cursor;
        int right;

        void add(Cell cell) {
            cells.add(new PlacedCell(cell, cursor));
            right = Math.max(right, cursor + cell.visualWidth);
            cursor += cell.advance;
        }
    }

    private static final class PlacedCell {
        final Cell cell;
        final int x;

        PlacedCell(Cell cell, int x) {
            this.cell = cell;
            this.x = x;
        }
    }

    private static final class QuarkMethods {
        final Method getModifiers;
        final Method getItems;
        final Field divisor;

        QuarkMethods(Method getModifiers, Method getItems, Field divisor) {
            this.getModifiers = getModifiers;
            this.getItems = getItems;
            this.divisor = divisor;
        }

        @SuppressWarnings("unchecked")
        Multimap<String, AttributeModifier> modifiers(ItemStack stack, EntityEquipmentSlot slot) {
            try {
                Object value = getModifiers.invoke(null, stack, slot);
                return value instanceof Multimap
                        ? (Multimap<String, AttributeModifier>) value : null;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        List<ItemStack> itemsFor(Enchantment enchantment) {
            try {
                Object value = getItems.invoke(null, enchantment);
                return value instanceof List ? (List<ItemStack>) value : Collections.emptyList();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                return Collections.emptyList();
            }
        }

        int divisor() {
            try {
                return Math.max(1, divisor.getInt(null));
            } catch (IllegalAccessException | RuntimeException | LinkageError ignored) {
                return 2;
            }
        }
    }
}
