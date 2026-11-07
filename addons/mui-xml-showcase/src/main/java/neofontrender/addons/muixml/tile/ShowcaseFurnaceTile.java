package neofontrender.addons.muixml.tile;

import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ITickable;

public final class ShowcaseFurnaceTile extends ShowcaseInventoryTile implements ITickable {

    static final int INPUT_SLOT_INDEX = 0;
    static final int FUEL_SLOT_INDEX = 1;
    static final int OUTPUT_SLOT_INDEX = 2;
    private static final int COOK_TOTAL = 200;

    private int burnTime;
    private int burnTotal;
    private int cookTime;

    public ShowcaseFurnaceTile() {
        super(ShowcaseProtocolTemplates.FURNACE);
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        if (slot == OUTPUT_SLOT_INDEX) return false;
        if (slot == FUEL_SLOT_INDEX) return TileEntityFurnace.isItemFuel(stack);
        return !FurnaceRecipes.instance().getSmeltingResult(stack).isEmpty();
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return createInventoryPanel(syncManager);
    }

    double getCookProgress() {
        return Math.min(1.0D, this.cookTime / (double) COOK_TOTAL);
    }

    double getBurnProgress() {
        return this.burnTotal <= 0 ? 0.0D : Math.min(1.0D, this.burnTime / (double) this.burnTotal);
    }

    @Override
    public void update() {
        if (this.world == null || this.world.isRemote) return;
        tickServer();
    }

    void tickServer() {
        boolean dirty = false;
        boolean burning = this.burnTime > 0;
        if (burning) {
            this.burnTime--;
            dirty = true;
        }

        if (canSmelt()) {
            if (this.burnTime <= 0) {
                ItemStack fuel = getItems().getStackInSlot(FUEL_SLOT_INDEX);
                int duration = TileEntityFurnace.getItemBurnTime(fuel);
                if (duration > 0) {
                    this.burnTime = duration;
                    this.burnTotal = duration;
                    consumeFuel(fuel);
                    dirty = true;
                }
            }
            if (this.burnTime > 0) {
                if (++this.cookTime >= COOK_TOTAL) {
                    this.cookTime = 0;
                    smeltOne();
                }
                dirty = true;
            }
        } else if (this.cookTime != 0) {
            this.cookTime = 0;
            dirty = true;
        }

        if (dirty) markDirty();
    }

    private boolean canSmelt() {
        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(getItems().getStackInSlot(INPUT_SLOT_INDEX));
        if (result.isEmpty()) return false;
        ItemStack output = getItems().getStackInSlot(OUTPUT_SLOT_INDEX);
        if (output.isEmpty()) return true;
        return ItemStack.areItemsEqual(output, result) && ItemStack.areItemStackTagsEqual(output, result)
                && output.getCount() + result.getCount() <= Math.min(output.getMaxStackSize(),
                getItems().getSlotLimit(OUTPUT_SLOT_INDEX));
    }

    private void smeltOne() {
        ItemStack input = getItems().getStackInSlot(INPUT_SLOT_INDEX);
        ItemStack result = FurnaceRecipes.instance().getSmeltingResult(input).copy();
        ItemStack output = getItems().getStackInSlot(OUTPUT_SLOT_INDEX);
        if (output.isEmpty()) getItems().setStackInSlot(OUTPUT_SLOT_INDEX, result);
        else {
            output.grow(result.getCount());
            getItems().setStackInSlot(OUTPUT_SLOT_INDEX, output);
        }
        input.shrink(1);
        getItems().setStackInSlot(INPUT_SLOT_INDEX, input);
    }

    private void consumeFuel(ItemStack fuel) {
        Item item = fuel.getItem();
        fuel.shrink(1);
        if (fuel.isEmpty() && item.hasContainerItem()) fuel = new ItemStack(item.getContainerItem());
        getItems().setStackInSlot(FUEL_SLOT_INDEX, fuel);
    }

    @Override
    protected void writeMachineNbt(NBTTagCompound compound) {
        compound.setInteger("BurnTime", this.burnTime);
        compound.setInteger("BurnTotal", this.burnTotal);
        compound.setInteger("CookTime", this.cookTime);
    }

    @Override
    protected void readMachineNbt(NBTTagCompound compound) {
        this.burnTime = compound.getInteger("BurnTime");
        this.burnTotal = compound.getInteger("BurnTotal");
        this.cookTime = compound.getInteger("CookTime");
    }

    @Override
    public String getScreenName() {
        return "mui_xml_furnace";
    }

    @Override
    public String getXmlResource() {
        return "screens/furnace.xml";
    }

    @Override
    public String getProtocolResource() {
        return "protocols/furnace.xml";
    }
}
