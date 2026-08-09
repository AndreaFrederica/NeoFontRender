package neofontrender.addons.electricelytra;

import neofontrender.addons.electricelytra.compat.ElectricElytraCompat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import neofontrender.addons.electricelytra.compat.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Vec3;
import net.minecraft.util.ChatComponentTranslation;
import neofontrender.addons.electricelytra.compat.IEnergyStorage;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import neofontrender.addons.electricelytra.network.ServerInputState;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ElectricFlightController {
    private final Set<UUID> activeFlights = new HashSet<>();

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.worldObj.isRemote) return;
        EntityPlayer player = event.player;
        UUID id = player.getUniqueID();
        ItemStack elytra = EntityEquipmentSlot.getChest(player);
        if (!ItemElectricElytra.isElectricElytra(elytra)) {
            activeFlights.remove(id);
            ElectricBodyAxis.reset(player);
            return;
        }

        boolean jump = ServerInputState.isJumpHeld(player);
        boolean vanillaFlightModel = ItemElectricElytra.usesVanillaFlightModel(elytra);
        if (vanillaFlightModel) {
            ElectricBodyAxis.reset(player);
        } else {
            ElectricBodyAxis.setManeuverCommand(player, ServerInputState.getPitchCommand(player),
                    ServerInputState.getRollCommand(player), ServerInputState.getYawCommand(player));
        }
        boolean engine = ItemElectricElytra.isEngineEnabled(elytra);
        int throttle = ItemElectricElytra.getThrottle(elytra);
        double throttleFraction = throttle / 100.0D;
        IEnergyStorage energy = ItemElectricElytra.getEnergy(elytra);
        if (engine && (energy == null || energy.getEnergyStored() <= 0)) {
            engine = false;
            ItemElectricElytra.setEngineEnabled(elytra, false);
            notify(player, "message.neofontrender_electric_elytra.empty");
        }

        // Creative/spectator flight owns movement and must never be converted into Elytra flight
        // merely because the shared jump key is held while descending.
        if (player.capabilities.isFlying) {
            activeFlights.remove(id);
            if (ElectricElytraCompat.isElytraFlying(player)) ElectricElytraCompat.setElytraFlying(player, false);
            ElectricBodyAxis.reset(player);
            ItemElectricElytra.setEnginePower(elytra, 0);
            return;
        }

        if (player.onGround) {
            activeFlights.remove(id);
            if (ElectricElytraCompat.isElytraFlying(player)) ElectricElytraCompat.setElytraFlying(player, false);
            ElectricBodyAxis.reset(player);
        }
        // Adopt a legitimate already-active glide (for example after reconnecting or enabling
        // the engine in flight), but never let engine-on alone create the flying state.
        if (!player.onGround && ElectricElytraCompat.isElytraFlying(player)) activeFlights.add(id);
        boolean canStart = shouldStartFlight(player.capabilities.isFlying, player.onGround,
                player.isInWater(), player.isRiding(), vanillaFlightModel ? false : engine,
                jump, player.motionY);
        if (canStart) activeFlights.add(id);

        boolean launching = !vanillaFlightModel && engine && throttle > 0
                && jump && player.onGround;
        if (launching) {
            activeFlights.add(id);
            double takeoffScale = 0.35D + throttleFraction * 0.65D;
            player.motionY = Math.max(player.motionY,
                    ElectricElytraConfig.takeoffVelocity * takeoffScale);
            Vec3 horizontal = player.getLookVec();
            player.motionX += horizontal.xCoord * 0.08D * throttleFraction;
            player.motionZ += horizontal.zCoord * 0.08D * throttleFraction;
            player.velocityChanged = true;
        }

        boolean flying = activeFlights.contains(id) && !player.onGround && !player.isRiding();
        if (flying) ElectricElytraCompat.setElytraFlying(player, true);

        int power = engine ? (flying
                ? ElectricFlightMath.indicatedPower(throttle) : 12) : 0;
        ItemElectricElytra.setEnginePower(elytra, power);
        if (!engine) return;

        int fullDrain = Math.max(ElectricElytraConfig.idleEnergyPerTick,
                launching ? ElectricElytraConfig.liftEnergyPerTick
                        : ElectricElytraConfig.cruiseEnergyPerTick);
        int drain = flying || launching ? ElectricElytraConfig.idleEnergyPerTick
                + (int) Math.round((fullDrain - ElectricElytraConfig.idleEnergyPerTick)
                        * throttleFraction)
                : ElectricElytraConfig.idleEnergyPerTick;
        if (energy.extractEnergy(drain, false) < drain) {
            ItemElectricElytra.setEngineEnabled(elytra, false);
            ItemElectricElytra.setEnginePower(elytra, 0);
            notify(player, "message.neofontrender_electric_elytra.empty");
            return;
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        activeFlights.remove(event.player.getUniqueID());
        ElectricBodyAxis.reset(event.player);
        ServerInputState.remove(event.player);
    }

    private static void notify(EntityPlayer player, String key) {
        if (player instanceof EntityPlayerMP) {
            player.addChatComponentMessage(new ChatComponentTranslation(key));
        }
    }

    static boolean shouldStartFlight(boolean creativeFlying, boolean onGround,
                                     boolean inWater, boolean riding, boolean engine,
                                     boolean jump, double verticalMotion) {
        return !creativeFlying && !onGround && !inWater && !riding && jump
                && (engine || verticalMotion < 0.0D);
    }
}
