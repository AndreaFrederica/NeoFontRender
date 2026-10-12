package neofontrender.addons.muixml.tile;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import neofontrender.addons.muixml.ShowcaseBlocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowcaseFurnaceTileTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) Bootstrap.register();
        ShowcaseBlocks.registerTileEntities();
    }

    @Test
    void chest256ConstructsAndPersistsItsLastSlot() {
        ShowcaseChest256Tile chest = new ShowcaseChest256Tile();
        assertEquals(256, chest.getItems().getSlots());
        chest.getItems().setStackInSlot(255, new ItemStack(Items.DIAMOND, 17));
        NBTTagCompound saved = chest.writeToNBT(new NBTTagCompound());
        ShowcaseChest256Tile loaded = new ShowcaseChest256Tile();
        loaded.readFromNBT(saved);
        assertEquals(256, loaded.getItems().getSlots());
        assertEquals(Items.DIAMOND, loaded.getItems().getStackInSlot(255).getItem());
        assertEquals(17, loaded.getItems().getStackInSlot(255).getCount());
    }

    @Test
    void furnaceEnforcesSlotRolesSmeltsAndPersistsInventory() {
        ShowcaseFurnaceTile furnace = new ShowcaseFurnaceTile();
        assertEquals(3, furnace.getItems().getSlots());
        assertEquals(27, new ShowcaseChestTile().getItems().getSlots());
        assertTrue(furnace.getItems().isItemValid(0, new ItemStack(Blocks.IRON_ORE)));
        assertTrue(furnace.getItems().isItemValid(1, new ItemStack(Items.COAL)));
        assertFalse(furnace.getItems().isItemValid(1, new ItemStack(Items.DIAMOND)));
        assertFalse(furnace.getItems().isItemValid(2, new ItemStack(Items.IRON_INGOT)));

        furnace.getItems().setStackInSlot(0, new ItemStack(Blocks.IRON_ORE));
        furnace.getItems().setStackInSlot(1, new ItemStack(Items.COAL));
        for (int i = 0; i < 200; i++) furnace.tickServer();
        assertTrue(furnace.getItems().getStackInSlot(0).isEmpty());
        assertEquals(1, furnace.getItems().getStackInSlot(2).getCount());
        assertEquals(Items.IRON_INGOT, furnace.getItems().getStackInSlot(2).getItem());

        NBTTagCompound saved = furnace.writeToNBT(new NBTTagCompound());
        ShowcaseFurnaceTile loaded = new ShowcaseFurnaceTile();
        loaded.readFromNBT(saved);
        assertEquals(Items.IRON_INGOT, loaded.getItems().getStackInSlot(2).getItem());
        assertEquals(furnace.getItems().getStackInSlot(1).getCount(),
                loaded.getItems().getStackInSlot(1).getCount());
    }
}
