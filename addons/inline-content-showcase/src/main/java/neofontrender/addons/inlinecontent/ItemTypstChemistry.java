package neofontrender.addons.inlinecontent;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/** Showcase item for the optional Typst and CeTZ structured-text provider. */
public final class ItemTypstChemistry extends Item {
    ItemTypstChemistry(String name) {
        setRegistryName(InlineContentShowcaseMod.MOD_ID, name);
        setTranslationKey(InlineContentShowcaseMod.MOD_ID + "." + name);
        setCreativeTab(ShowcaseItems.TAB);
        setMaxStackSize(1);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String key = getTranslationKey(stack) + ".name";
        String translated = I18n.format(key);
        return key.equals(translated) ? "Typst Chemistry Demonstrator" : translated;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GRAY + I18n.format("showcase.typst.structure"));
        tooltip.add(TextFormatting.WHITE + TypstChemistrySample.STRUCTURE_TOKEN);
        tooltip.add(TextFormatting.DARK_GRAY + I18n.format("showcase.typst.formula"));
        tooltip.add(TextFormatting.WHITE + TypstChemistrySample.FORMULA_TOKEN);
        if (GuiScreen.isShiftKeyDown()) {
            tooltip.add(TextFormatting.AQUA + "@preview/cetz:0.5.2");
            tooltip.add(TextFormatting.AQUA + "@preview/chemformula:0.1.3");
        } else {
            tooltip.add(TextFormatting.DARK_GRAY + I18n.format("showcase.typst.packages"));
        }
    }
}
