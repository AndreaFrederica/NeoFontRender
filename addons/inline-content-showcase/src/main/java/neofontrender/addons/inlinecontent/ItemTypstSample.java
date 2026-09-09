package neofontrender.addons.inlinecontent;

import neofontrender.addons.inlinecontent.client.InlineContentShowcaseScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** Static demonstration samples: no crafting recipes or chemical simulation. */
public final class ItemTypstSample extends Item {
    @Nullable private final TypstChemicalCatalog.Chemical chemical;

    ItemTypstSample(@Nullable TypstChemicalCatalog.Chemical chemical) {
        this.chemical = chemical;
        String id = chemical == null ? "periodic_table" : chemical.id();
        setRegistryName(InlineContentShowcaseMod.MOD_ID, id);
        setTranslationKey(InlineContentShowcaseMod.MOD_ID + "." + id);
        setCreativeTab(ShowcaseItems.TAB);
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        if (chemical == null) {
            tooltip.add(TextFormatting.WHITE + TypstChemicalCatalog.PERIODIC_TABLE_TOKEN);
        } else {
            tooltip.add(TextFormatting.WHITE + chemical.formulaToken());
            tooltip.add(TextFormatting.GRAY + I18n.format("showcase.typst.notation"));
            tooltip.add(TextFormatting.WHITE + chemical.notationToken());
            if (chemical.structureToken() != null) tooltip.add(TextFormatting.WHITE + chemical.structureToken());
        }
        tooltip.add(TextFormatting.DARK_GRAY + I18n.format("showcase.typst.open"));
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        if (world.isRemote) openGallery();
        return new ActionResult<>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @SideOnly(Side.CLIENT)
    private void openGallery() {
        InlineContentShowcaseScreen.openChemical(chemical);
    }
}
