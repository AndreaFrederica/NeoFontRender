package neofontrender.addons.electricelytra;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import neofontrender.addons.electricelytra.compat.IEnergyStorage;

final class ElectricElytraEnergy implements IEnergyStorage {
    private static final String ENERGY_KEY = "ElectricElytraEnergy";
    private final ItemStack stack;

    ElectricElytraEnergy(ItemStack stack) { this.stack = stack; }

    private boolean infinite() { return ItemElectricElytra.hasInfiniteEnergy(stack); }

    @Override public int receiveEnergy(int maxReceive, boolean simulate) {
        if (infinite()) return 0;
        int received = Math.min(Math.max(0, maxReceive), getMaxEnergyStored() - getEnergyStored());
        if (!simulate && received > 0) setEnergy(getEnergyStored() + received);
        return received;
    }

    @Override public int extractEnergy(int maxExtract, boolean simulate) {
        if (infinite()) return Math.max(0, maxExtract);
        int extracted = Math.min(Math.max(0, maxExtract), getEnergyStored());
        if (!simulate && extracted > 0) setEnergy(getEnergyStored() - extracted);
        return extracted;
    }

    @Override public int getEnergyStored() {
        if (infinite()) return Integer.MAX_VALUE;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(ENERGY_KEY, 3)) return getMaxEnergyStored();
        return Math.max(0, Math.min(getMaxEnergyStored(), tag.getInteger(ENERGY_KEY)));
    }

    @Override public int getMaxEnergyStored() {
        return infinite() ? Integer.MAX_VALUE : ElectricElytraConfig.energyCapacity;
    }

    @Override public boolean canExtract() { return false; }
    @Override public boolean canReceive() { return true; }

    private void setEnergy(int energy) {
        if (infinite()) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            stack.setTagCompound(tag);
        }
        tag.setInteger(ENERGY_KEY, Math.max(0, Math.min(getMaxEnergyStored(), energy)));
    }
}
