package neofontrender.addons.inlinecontent;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public final class ItemCrownEtherSolution extends Item {
    ItemCrownEtherSolution(String name) {
        setRegistryName(InlineContentShowcaseMod.MOD_ID, name);
        setTranslationKey(InlineContentShowcaseMod.MOD_ID + "." + name);
        setCreativeTab(CreativeTabs.MISC);
        setMaxStackSize(1);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String key = getTranslationKey(stack) + ".name";
        String translated = I18n.format(key);
        return key.equals(translated) ? "4'-Aminobenzo-18-crown-6 Solution" : translated;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world,
                               List<String> tooltip, ITooltipFlag flag) {
        CrownEtherSample.Detail detail = GuiScreen.isCtrlKeyDown()
                ? CrownEtherSample.Detail.DETAILED
                : GuiScreen.isShiftKeyDown() ? CrownEtherSample.Detail.EXPANDED
                : CrownEtherSample.Detail.COMPACT;
        for (String line : CrownEtherSample.lines(detail)) tooltip.add(color(line));
    }

    private static String color(String line) {
        if (line.equals(CrownEtherSample.STRUCTURE_TOKEN)) return TextFormatting.WHITE + line;
        if (line.startsWith("Hold ")) return TextFormatting.DARK_GRAY + line;
        if (line.equals("SMILES") || line.equals("ContentTweaker")) return TextFormatting.DARK_GRAY + line;
        if (line.equals(CrownEtherSample.SMILES)) return TextFormatting.GRAY + line;
        if (line.endsWith("mB")) return TextFormatting.AQUA + line;
        return TextFormatting.WHITE + line;
    }
}
