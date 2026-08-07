package neofontrender.addons.electricelytra;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagInt;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

final class ElectricElytraEnergy implements IEnergyStorage, ICapabilitySerializable<NBTBase> {
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
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(ENERGY_KEY, 3)) {
            return getMaxEnergyStored();
        }
        return Math.max(0, Math.min(getMaxEnergyStored(),
                stack.getTagCompound().getInteger(ENERGY_KEY)));
    }

    @Override public int getMaxEnergyStored() {
        return infinite() ? Integer.MAX_VALUE : ElectricElytraConfig.energyCapacity;
    }
    @Override public boolean canExtract() { return false; }
    @Override public boolean canReceive() { return true; }

    private void setEnergy(int energy) {
        if (infinite()) return;
        if (!stack.hasTagCompound()) stack.setTagCompound(new net.minecraft.nbt.NBTTagCompound());
        stack.getTagCompound().setInteger(ENERGY_KEY,
                Math.max(0, Math.min(getMaxEnergyStored(), energy)));
    }

    @Override public boolean hasCapability(Capability<?> capability, @Nullable net.minecraft.util.EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY;
    }

    @Nullable @Override public <T> T getCapability(Capability<T> capability,
                                                    @Nullable net.minecraft.util.EnumFacing facing) {
        return capability == CapabilityEnergy.ENERGY ? CapabilityEnergy.ENERGY.cast(this) : null;
    }

    @Override public NBTBase serializeNBT() { return new NBTTagInt(getEnergyStored()); }

    @Override public void deserializeNBT(NBTBase nbt) {
        if (nbt instanceof NBTTagInt) setEnergy(((NBTTagInt) nbt).getInt());
    }
}
