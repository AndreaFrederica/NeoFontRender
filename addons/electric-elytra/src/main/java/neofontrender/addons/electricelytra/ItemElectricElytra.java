package neofontrender.addons.electricelytra;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.electricelytra.compat.EntityEquipmentSlot;
import neofontrender.addons.electricelytra.compat.IEnergyStorage;

import java.util.List;

public class ItemElectricElytra extends Item {
    static final String ENGINE_KEY = "ElectricElytraEngine";
    static final String POWER_KEY = "ElectricElytraPower";
    static final String THROTTLE_KEY = "ElectricElytraThrottle";
    static final String SAS_ENABLED_KEY = "ElectricElytraSasEnabled";
    static final String SAS_X_KEY = "ElectricElytraSasQx";
    static final String SAS_Y_KEY = "ElectricElytraSasQy";
    static final String SAS_Z_KEY = "ElectricElytraSasQz";
    static final String SAS_W_KEY = "ElectricElytraSasQw";
    static final String FLAP_KEY = "ElectricElytraFlap";
    private final boolean sasCapable;
    private final boolean flapCapable;
    private final boolean infiniteEnergy;
    private final boolean vanillaFlightModel;

    ItemElectricElytra(String name, boolean sasCapable, boolean flapCapable) {
        this(name, sasCapable, flapCapable, false, false);
    }

    ItemElectricElytra(String name, boolean sasCapable, boolean flapCapable,
                       boolean infiniteEnergy) {
        this(name, sasCapable, flapCapable, infiniteEnergy, false);
    }

    ItemElectricElytra(String name, boolean sasCapable, boolean flapCapable,
                       boolean infiniteEnergy, boolean vanillaFlightModel) {
        this.sasCapable = sasCapable;
        this.flapCapable = flapCapable;
        this.infiniteEnergy = infiniteEnergy;
        this.vanillaFlightModel = vanillaFlightModel;
        setUnlocalizedName(ElectricElytraMod.MOD_ID + "." + name);
        setTextureName(ElectricElytraMod.MOD_ID + ":" + name);
        setCreativeTab(CreativeTabs.tabTransport);
        setMaxStackSize(1);
        setFull3D();
    }

    public static boolean isElectricElytra(ItemStack stack) {
        return stack != null && stack.stackSize > 0 && stack.getItem() instanceof ItemElectricElytra;
    }

    public static boolean isSasCapable(ItemStack stack) {
        return isElectricElytra(stack) && ((ItemElectricElytra) stack.getItem()).sasCapable;
    }

    /** True for the reference item that retains Minecraft's complete vanilla Elytra solver. */
    public static boolean usesVanillaFlightModel(ItemStack stack) {
        return isElectricElytra(stack)
                && ((ItemElectricElytra) stack.getItem()).vanillaFlightModel;
    }

    public static boolean usesAerodynamicFlightModel(ItemStack stack) {
        return isElectricElytra(stack) && !usesVanillaFlightModel(stack);
    }

    public static boolean hasInfiniteEnergy(ItemStack stack) {
        return isElectricElytra(stack)
                && ((ItemElectricElytra) stack.getItem()).infiniteEnergy;
    }

    public static boolean isSasEnabled(ItemStack stack) {
        return isSasCapable(stack) && stack.hasTagCompound()
                && stack.getTagCompound().getBoolean(SAS_ENABLED_KEY);
    }

    public static void setSas(ItemStack stack, boolean enabled, FlightAttitude attitude) {
        if (!isSasCapable(stack)) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }
        tag.setBoolean(SAS_ENABLED_KEY, enabled);
        if (!enabled) return;
        if (attitude == null) attitude = FlightAttitude.IDENTITY;
        tag.setDouble(SAS_X_KEY, attitude.x);
        tag.setDouble(SAS_Y_KEY, attitude.y);
        tag.setDouble(SAS_Z_KEY, attitude.z);
        tag.setDouble(SAS_W_KEY, attitude.w);
    }

    public static FlightAttitude getSasTarget(ItemStack stack) {
        if (!isSasEnabled(stack)) return null;
        NBTTagCompound tag = stack.getTagCompound();
        return new FlightAttitude(tag.getDouble(SAS_X_KEY), tag.getDouble(SAS_Y_KEY),
                tag.getDouble(SAS_Z_KEY), tag.getDouble(SAS_W_KEY));
    }

    public static boolean isFlapCapable(ItemStack stack) {
        return isElectricElytra(stack) && ((ItemElectricElytra) stack.getItem()).flapCapable;
    }

    public static int getFlapSetting(ItemStack stack) {
        if (!isFlapCapable(stack) || !stack.hasTagCompound()) return 0;
        return Math.max(0, Math.min(2, stack.getTagCompound().getInteger(FLAP_KEY)));
    }

    public static void setFlapSetting(ItemStack stack, int setting) {
        if (!isFlapCapable(stack)) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }
        tag.setInteger(FLAP_KEY, Math.max(0, Math.min(2, setting)));
    }

    public static boolean isEngineEnabled(ItemStack stack) {
        return isElectricElytra(stack) && stack.hasTagCompound()
                && stack.getTagCompound().getBoolean(ENGINE_KEY);
    }

    public static void setEngineEnabled(ItemStack stack, boolean enabled) {
        if (!isElectricElytra(stack)) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }
        tag.setBoolean(ENGINE_KEY, enabled);
        if (!enabled) tag.setInteger(POWER_KEY, 0);
    }

    public static int getEnginePower(ItemStack stack) {
        return stack.hasTagCompound() ? Math.max(0, Math.min(100,
                stack.getTagCompound().getInteger(POWER_KEY))) : 0;
    }

    public static void setEnginePower(ItemStack stack, int power) {
        if (!isElectricElytra(stack)) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }
        tag.setInteger(POWER_KEY, Math.max(0, Math.min(100, power)));
    }

    public static int getThrottle(ItemStack stack) {
        if (!isElectricElytra(stack)) return 0;
        if (!stack.hasTagCompound() || !stack.getTagCompound().hasKey(THROTTLE_KEY, 3)) return 100;
        return Math.max(0, Math.min(100, stack.getTagCompound().getInteger(THROTTLE_KEY)));
    }

    public static void setThrottle(ItemStack stack, int throttle) {
        if (!isElectricElytra(stack)) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) { tag = new NBTTagCompound(); stack.setTagCompound(tag); }
        tag.setInteger(THROTTLE_KEY, Math.max(0, Math.min(100, throttle)));
    }

    public static IEnergyStorage getEnergy(ItemStack stack) {
        return isElectricElytra(stack) ? new ElectricElytraEnergy(stack) : null;
    }

    @Override public ItemStack onItemRightClick(ItemStack held, World world, EntityPlayer player) {
        ItemStack chest = EntityEquipmentSlot.getChest(player);
        if (chest != null && chest.stackSize > 0) return held;
        EntityEquipmentSlot.setChest(player, held.copy());
        held.stackSize = 0;
        return held;
    }

    @Override public EnumRarity getRarity(ItemStack stack) {
        return hasInfiniteEnergy(stack) ? EnumRarity.epic : EnumRarity.rare;
    }

    @Override public void addInformation(ItemStack stack, EntityPlayer player,
                                         List<String> tooltip, boolean advanced) {
        IEnergyStorage energy = getEnergy(stack);
        int stored = energy == null ? 0 : energy.getEnergyStored();
        int capacity = energy == null ? ElectricElytraConfig.energyCapacity : energy.getMaxEnergyStored();
        tooltip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocal(
                hasInfiniteEnergy(stack) ? "tooltip.neofontrender_electric_elytra.energy_infinite"
                        : "tooltip.neofontrender_electric_elytra.energy"));
        if (!hasInfiniteEnergy(stack)) {
            tooltip.set(tooltip.size() - 1, tooltip.get(tooltip.size() - 1)
                    .replace("%d %d", stored + " " + capacity));
        }
        tooltip.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal(
                vanillaFlightModel ? "tooltip.neofontrender_electric_elytra.vanilla_controls"
                        : "tooltip.neofontrender_electric_elytra.controls"));
        if (vanillaFlightModel) {
            tooltip.add(EnumChatFormatting.YELLOW + StatCollector.translateToLocal(
                    "tooltip.neofontrender_electric_elytra.vanilla_flight"));
        } else {
            tooltip.add(EnumChatFormatting.YELLOW + StatCollector.translateToLocal(
                    "tooltip.neofontrender_electric_elytra.aerodynamic_flight"));
        }
        if (sasCapable) {
            tooltip.add(EnumChatFormatting.GREEN + StatCollector.translateToLocal(
                    isSasEnabled(stack) ? "tooltip.neofontrender_electric_elytra.sas_on"
                            : "tooltip.neofontrender_electric_elytra.sas_off"));
        }
        if (flapCapable) {
            tooltip.add(EnumChatFormatting.AQUA + StatCollector.translateToLocal(
                    "tooltip.neofontrender_electric_elytra.flap") + " "
                    + (getFlapSetting(stack) == 0 ? "UP" : getFlapSetting(stack) == 1 ? "TO" : "LDG"));
        }
    }
}
