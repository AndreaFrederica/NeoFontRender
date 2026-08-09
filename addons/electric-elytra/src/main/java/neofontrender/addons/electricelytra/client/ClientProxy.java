package neofontrender.addons.electricelytra.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import neofontrender.addons.electricelytra.compat.EntityEquipmentSlot;
import neofontrender.addons.electricelytra.compat.ElectricElytraCompat;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import neofontrender.addons.api.flight.FlightApi;
import neofontrender.addons.api.flight.FlightBodyPose;
import neofontrender.addons.api.flight.FlightCameraTracking;
import neofontrender.addons.api.flight.FlightAttitude;
import neofontrender.addons.api.flight.FlightCapability;
import neofontrender.addons.api.flight.FlightDecision;
import neofontrender.addons.api.flight.FlightHudAttitude;
import neofontrender.addons.electricelytra.ElectricBodyAxis;
import neofontrender.addons.electricelytra.ElectricFlightDebug;
import neofontrender.addons.electricelytra.CommonProxy;
import neofontrender.addons.electricelytra.ElectricElytraItems;
import neofontrender.addons.electricelytra.ElectricElytraConfig;
import neofontrender.addons.electricelytra.ElectricElytraMod;
import neofontrender.addons.electricelytra.ItemElectricElytra;
import neofontrender.addons.electricelytra.network.ElectricElytraNetwork;
import org.lwjgl.input.Keyboard;

public final class ClientProxy extends CommonProxy {
    private static final KeyBinding TOGGLE_ENGINE = new KeyBinding(
            "key.neofontrender_electric_elytra.toggle_engine", Keyboard.KEY_G,
            "key.categories.neofontrender_electric_elytra");
    private static final KeyBinding INCREASE_THROTTLE = new KeyBinding(
            "key.neofontrender_electric_elytra.increase_throttle", Keyboard.KEY_EQUALS,
            "key.categories.neofontrender_electric_elytra");
    private static final KeyBinding DECREASE_THROTTLE = new KeyBinding(
            "key.neofontrender_electric_elytra.decrease_throttle", Keyboard.KEY_MINUS,
            "key.categories.neofontrender_electric_elytra");
    private static final KeyBinding TOGGLE_SAS = new KeyBinding(
            "key.neofontrender_electric_elytra.toggle_sas", Keyboard.KEY_H,
            "key.categories.neofontrender_electric_elytra");
    private static final KeyBinding FLAP_UP = new KeyBinding(
            "key.neofontrender_electric_elytra.flap_up", Keyboard.KEY_LBRACKET,
            "key.categories.neofontrender_electric_elytra");
    private static final KeyBinding FLAP_DOWN = new KeyBinding(
            "key.neofontrender_electric_elytra.flap_down", Keyboard.KEY_RBRACKET,
            "key.categories.neofontrender_electric_elytra");
    private static final KeyBinding EDIT_HUD = new KeyBinding(
            "key.neofontrender_electric_elytra.edit_hud", Keyboard.KEY_J,
            "key.categories.neofontrender_electric_elytra");

    private boolean lastJump;
    private int heartbeat;
    private float maneuverPitch;
    private float maneuverYaw;
    private float maneuverRoll;
    private float lastSentPitch = Float.NaN;
    private float lastSentYaw = Float.NaN;
    private float lastSentRoll = Float.NaN;

    @Override public void preInit() {
        ElectricElytraNetwork.initializeClient();
    }

    @Override public void init() {
        ClientRegistry.registerKeyBinding(TOGGLE_ENGINE);
        ClientRegistry.registerKeyBinding(INCREASE_THROTTLE);
        ClientRegistry.registerKeyBinding(DECREASE_THROTTLE);
        ClientRegistry.registerKeyBinding(TOGGLE_SAS);
        ClientRegistry.registerKeyBinding(FLAP_UP);
        ClientRegistry.registerKeyBinding(FLAP_DOWN);
        ClientRegistry.registerKeyBinding(EDIT_HUD);
        FMLCommonHandler.instance().bus().register(this);
        MinecraftForge.EVENT_BUS.register(ElectricElytraHud.INSTANCE);
        FlightApi.registerCapabilityProvider(new ResourceLocation(ElectricElytraMod.MOD_ID,
                "electric_elytra"), 200, (player, capability, builtIn) -> {
            ItemStack stack = EntityEquipmentSlot.getChest(player);
            if (!ItemElectricElytra.isElectricElytra(stack)) return FlightDecision.PASS;
            boolean editingHud = player == Minecraft.getMinecraft().thePlayer
                    && Minecraft.getMinecraft().currentScreen instanceof ElectricElytraHudEditor;
            if (ItemElectricElytra.usesVanillaFlightModel(stack)) {
                if (capability == FlightCapability.HUD) {
                    return ElectricElytraCompat.isElytraFlying(player) || ItemElectricElytra.isEngineEnabled(stack)
                            || editingHud ? FlightDecision.ALLOW : FlightDecision.PASS;
                }
                // Electric Elytra is the vanilla-flight clone. Yield every flight-control
                // capability so UIE treats it exactly like a normal Elytra, including UIE's
                // optional A/D yaw. Only the separate aerodynamic wings take those controls over.
                return FlightDecision.PASS;
            }
            boolean aerodynamicFlight = ItemElectricElytra.usesAerodynamicFlightModel(stack)
                    && ElectricElytraCompat.isElytraFlying(player) && !player.onGround;
            // Engine-on at rest needs the instrument panel, not UIE's flight controller,
            // camera capture or rolled player renderer.
            if (!aerodynamicFlight) {
                return capability == FlightCapability.HUD
                        && (ItemElectricElytra.isEngineEnabled(stack) || editingHud)
                        ? FlightDecision.ALLOW : FlightDecision.PASS;
            }
            return aerodynamicFlight && (capability == FlightCapability.KEYBOARD_YAW
                    || capability == FlightCapability.CAMERA_ROTATION)
                    ? FlightDecision.DENY : FlightDecision.ALLOW;
        });
        FlightApi.registerBodyPoseProvider(new ResourceLocation(ElectricElytraMod.MOD_ID,
                "electric_elytra_body_axis"), 200, (player, partialTicks) -> {
            ItemStack stack = EntityEquipmentSlot.getChest(player);
            if (player.onGround || !ElectricElytraCompat.isElytraFlying(player)
                    || !ItemElectricElytra.usesAerodynamicFlightModel(stack)
                    || !ItemElectricElytra.isElectricElytra(stack)) return null;
            FlightAttitude attitude = Minecraft.getMinecraft().thePlayer == player
                    ? ElectricBodyAxis.sampleAttitude(player, partialTicks)
                    : ElectricRemoteAttitudes.sample(player.getEntityId(), partialTicks);
            if (attitude == null) {
                attitude = FlightAttitude.fromMinecraftDegrees(
                        player.rotationPitch, player.rotationYaw, 0.0D);
            }
            return new FlightBodyPose(attitude);
        });
        FlightApi.registerHudAttitudeProvider(new ResourceLocation(ElectricElytraMod.MOD_ID,
                "electric_elytra_hud_attitude"), 200, (player, partialTicks) -> {
            ItemStack stack = EntityEquipmentSlot.getChest(player);
            if (player.onGround || !ElectricElytraCompat.isElytraFlying(player)
                    || !ItemElectricElytra.usesAerodynamicFlightModel(stack)
                    || !ItemElectricElytra.isElectricElytra(stack)) return null;
            return new FlightHudAttitude(ElectricBodyAxis.sampleAttitude(player, partialTicks));
        });
        FlightApi.registerManeuverHandler(new ResourceLocation(ElectricElytraMod.MOD_ID,
                "electric_elytra_virtual_stick"), 200, input -> {
            ItemStack stack = EntityEquipmentSlot.getChest(input.getPlayer());
            if (input.getPlayer().onGround || !ElectricElytraCompat.isElytraFlying(input.getPlayer())
                    || !ItemElectricElytra.usesAerodynamicFlightModel(stack)
                    || !ItemElectricElytra.isElectricElytra(stack)) return false;
            maneuverPitch = input.getPitch();
            maneuverYaw = input.getYaw();
            maneuverRoll = input.getRoll();
            ElectricBodyAxis.setManeuverCommand(input.getPlayer(), maneuverPitch,
                    maneuverRoll, maneuverYaw);
            return true;
        });
        FlightApi.registerCameraTrackingProvider(new ResourceLocation(ElectricElytraMod.MOD_ID,
                "electric_elytra_camera"), 200, (player, partialTicks) -> {
            ItemStack stack = EntityEquipmentSlot.getChest(player);
            if (player.onGround || !ElectricElytraCompat.isElytraFlying(player)
                    || !ItemElectricElytra.usesAerodynamicFlightModel(stack)
                    || !ItemElectricElytra.isElectricElytra(stack)) return null;
            return FlightCameraTracking.rigid(
                    ElectricBodyAxis.sampleAttitude(player, partialTicks));
        });
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.thePlayer == null || minecraft.theWorld == null) {
            lastJump = false;
            heartbeat = 0;
            maneuverPitch = maneuverYaw = maneuverRoll = 0.0F;
            lastSentPitch = Float.NaN;
            lastSentYaw = Float.NaN;
            lastSentRoll = Float.NaN;
            ElectricRemoteAttitudes.clear();
            return;
        }
        while (TOGGLE_ENGINE.isPressed()) ElectricElytraNetwork.toggleEngine();

        ItemStack chest = EntityEquipmentSlot.getChest(minecraft.thePlayer);
        boolean equipped = ItemElectricElytra.isElectricElytra(chest);
        if (!equipped || minecraft.thePlayer.onGround || !ElectricElytraCompat.isElytraFlying(minecraft.thePlayer)) {
            ElectricBodyAxis.reset(minecraft.thePlayer);
            ElectricFlightDebug.clear(minecraft.thePlayer);
            maneuverPitch = maneuverYaw = maneuverRoll = 0.0F;
            if (minecraft.thePlayer.onGround) FlightApi.setRoll(0.0F);
        }
        if (equipped && ItemElectricElytra.usesAerodynamicFlightModel(chest)
                && !minecraft.thePlayer.onGround && ElectricElytraCompat.isElytraFlying(minecraft.thePlayer)) {
            ElectricBodyAxis.setManeuverCommand(minecraft.thePlayer, maneuverPitch,
                    maneuverRoll, maneuverYaw);
        }
        if (equipped && minecraft.currentScreen == null) {
            while (EDIT_HUD.isPressed()) {
                minecraft.displayGuiScreen(new ElectricElytraHudEditor());
            }
            while (INCREASE_THROTTLE.isPressed()) adjustThrottle(chest,
                    ElectricElytraConfig.throttleStepPercent);
            while (DECREASE_THROTTLE.isPressed()) adjustThrottle(chest,
                    -ElectricElytraConfig.throttleStepPercent);
            while (TOGGLE_SAS.isPressed() && ItemElectricElytra.isSasCapable(chest)) {
                FlightAttitude attitude = ElectricElytraCompat.isElytraFlying(minecraft.thePlayer)
                        ? ElectricBodyAxis.sampleAttitude(minecraft.thePlayer, 1.0F)
                        : FlightAttitude.fromMinecraftDegrees(minecraft.thePlayer.rotationPitch,
                        minecraft.thePlayer.rotationYaw, 0.0D);
                boolean enabled = !ItemElectricElytra.isSasEnabled(chest);
                ItemElectricElytra.setSas(chest, enabled, attitude);
                ElectricElytraNetwork.toggleSas(attitude);
            }
            while (FLAP_UP.isPressed()) adjustFlap(chest, -1);
            while (FLAP_DOWN.isPressed()) adjustFlap(chest, 1);
        }
        boolean jump = equipped && minecraft.currentScreen == null
                && minecraft.gameSettings.keyBindJump.getIsKeyPressed();
        boolean controlsChanged = !Float.isFinite(lastSentPitch)
                || Math.abs(maneuverPitch - lastSentPitch) >= 0.01F
                || !Float.isFinite(lastSentYaw)
                || Math.abs(maneuverYaw - lastSentYaw) >= 0.01F
                || !Float.isFinite(lastSentRoll)
                || Math.abs(maneuverRoll - lastSentRoll) >= 0.01F;
        if (jump != lastJump || controlsChanged || ++heartbeat >= 3) {
            FlightAttitude networkAttitude = equipped
                    && ItemElectricElytra.usesAerodynamicFlightModel(chest)
                    && !minecraft.thePlayer.onGround && ElectricElytraCompat.isElytraFlying(minecraft.thePlayer)
                    ? ElectricBodyAxis.sampleAttitude(minecraft.thePlayer, 1.0F)
                    : FlightAttitude.fromMinecraftDegrees(minecraft.thePlayer.rotationPitch,
                    minecraft.thePlayer.rotationYaw, 0.0D);
            ElectricElytraNetwork.sendInput(jump, maneuverPitch, maneuverYaw, maneuverRoll,
                    networkAttitude);
            lastJump = jump;
            lastSentPitch = maneuverPitch;
            lastSentYaw = maneuverYaw;
            lastSentRoll = maneuverRoll;
            heartbeat = 0;
        }
    }

    private static void adjustThrottle(ItemStack stack, int delta) {
        int throttle = Math.max(0, Math.min(100,
                ItemElectricElytra.getThrottle(stack) + delta));
        ItemElectricElytra.setThrottle(stack, throttle);
        ElectricElytraNetwork.setThrottle(throttle);
    }

    private static void adjustFlap(ItemStack stack, int delta) {
        if (!ItemElectricElytra.isFlapCapable(stack)) return;
        int current = ItemElectricElytra.getFlapSetting(stack);
        int setting = Math.max(0, Math.min(2, current + delta));
        if (setting == current) return;
        ItemElectricElytra.setFlapSetting(stack, setting);
        ElectricElytraNetwork.setFlap(setting);
    }
}
