package neofontrender.addons.muixml.block;

import com.cleanroommc.modularui.factory.GuiFactories;
import neofontrender.addons.muixml.MuiXmlShowcaseMod;
import neofontrender.addons.muixml.tile.ShowcaseChestTile;
import neofontrender.addons.muixml.tile.ShowcaseFurnaceTile;
import neofontrender.addons.muixml.tile.ShowcaseInventoryTile;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class ShowcaseMachineBlock extends Block implements ITileEntityProvider {

    public enum Kind { CHEST, FURNACE }

    private final Kind kind;

    public ShowcaseMachineBlock(String name, Kind kind) {
        super(kind == Kind.CHEST ? Material.WOOD : Material.ROCK);
        this.kind = kind;
        setRegistryName(MuiXmlShowcaseMod.MOD_ID, name);
        setTranslationKey(MuiXmlShowcaseMod.MOD_ID + "." + name);
        setCreativeTab(CreativeTabs.REDSTONE);
        setHardness(kind == Kind.CHEST ? 2.5F : 3.5F);
    }

    @Override
    public @Nullable TileEntity createNewTileEntity(World world, int metadata) {
        return this.kind == Kind.CHEST ? new ShowcaseChestTile() : new ShowcaseFurnaceTile();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote && world.getTileEntity(pos) instanceof ShowcaseInventoryTile) {
            GuiFactories.tileEntity().open(player, pos);
        }
        return true;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof ShowcaseInventoryTile) {
            ShowcaseInventoryTile inventory = (ShowcaseInventoryTile) tile;
            for (int i = 0; i < inventory.getItems().getSlots(); i++) {
                ItemStack stack = inventory.getItems().getStackInSlot(i);
                if (!stack.isEmpty()) InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), stack);
            }
        }
        super.breakBlock(world, pos, state);
    }
}
