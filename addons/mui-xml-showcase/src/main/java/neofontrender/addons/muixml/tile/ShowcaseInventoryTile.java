package neofontrender.addons.muixml.tile;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.sync.MuiProtocolEntry;
import com.cleanroommc.modularui.api.sync.MuiProtocolTemplate;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.widgets.slot.PlayerSlotGroup;
import neofontrender.addons.muixml.client.InventoryXmlScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class ShowcaseInventoryTile extends TileEntity implements IGuiHolder<PosGuiData> {

    private final MuiProtocolTemplate protocolTemplate;
    private final ItemStackHandler items;

    protected ShowcaseInventoryTile(MuiProtocolTemplate protocolTemplate) {
        this.protocolTemplate = Objects.requireNonNull(protocolTemplate, "protocolTemplate");
        int machineSlots = Math.toIntExact(protocolTemplate.getPlan().getEntries().stream()
                .filter(entry -> entry.getKind() == MuiProtocolEntry.Kind.SLOT)
                .filter(entry -> "showcase:machine-slot".equals(entry.getType()))
                .count());
        if (machineSlots <= 0) throw new IllegalArgumentException("Inventory protocol declares no machine slots");
        this.items = new ItemStackHandler(machineSlots) {
            @Override
            protected void onContentsChanged(int slot) {
                ShowcaseInventoryTile.this.markDirty();
            }

            @Override
            public boolean isItemValid(int slot, net.minecraft.item.ItemStack stack) {
                return ShowcaseInventoryTile.this.isItemValid(slot, stack);
            }
        };
    }

    @Override
    public final MuiProtocolTemplate getProtocolTemplate(PosGuiData data) {
        return this.protocolTemplate;
    }

    public final ItemStackHandler getItems() {
        return this.items;
    }

    protected boolean isItemValid(int slot, net.minecraft.item.ItemStack stack) {
        return true;
    }

    protected final ModularPanel createInventoryPanel(PanelSyncManager syncManager) {
        syncManager.registerSlotGroup("machine", this.items.getSlots());
        syncManager.registerSlotGroup(new PlayerSlotGroup(PlayerSlotGroup.NAME));
        ModularPanel panel = new ModularPanel(getScreenName()).size(194, 222);
        panel.disableThemeBackground(true).disableHoverThemeBackground(true);
        return panel;
    }

    public abstract String getScreenName();

    public abstract String getXmlResource();

    public abstract String getProtocolResource();

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PosGuiData data, ModularPanel mainPanel) {
        return InventoryXmlScreen.create(this, mainPanel);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Items", this.items.serializeNBT());
        writeMachineNbt(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("Items")) this.items.deserializeNBT(compound.getCompoundTag("Items"));
        readMachineNbt(compound);
    }

    protected void writeMachineNbt(NBTTagCompound compound) {}

    protected void readMachineNbt(NBTTagCompound compound) {}

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return (T) this.items;
        return super.getCapability(capability, facing);
    }

    public boolean canUse(EntityPlayer player) {
        return !isInvalid() && player.getDistanceSq(getPos()) <= 64.0D;
    }
}
