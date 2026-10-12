package neofontrender.addons.controller;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import neofontrender.addons.controller.sdl.ControllerSnapshot;
import neofontrender.addons.controller.sdl.SdlDeviceManager;
import neofontrender.api.client.input.NfrKeyBindingControllerInput;
import neofontrender.addons.api.input.InputAction;
import neofontrender.addons.api.input.InputApi;
import neofontrender.addons.api.input.InputDisposition;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pulses exact vanilla or Forge/mod KeyBinding instances from the selected SDL controller. */
final class ControllerKeyBindingBridge {
    private final SdlDeviceManager manager;
    private final Map<String, Boolean> previousDown = new HashMap<>();
    private final Map<String, KeyBinding> touched = new HashMap<>();

    ControllerKeyBindingBridge(SdlDeviceManager manager) { this.manager = manager; }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen != null || !minecraft.inGameHasFocus) {
            clear();
            return;
        }
        Map<String, KeyBinding> registered = new LinkedHashMap<>();
        for (KeyBinding binding : ControllerForgeBindings.registered()) {
            registered.put(binding.getKeyDescription(), binding);
        }
        Map<String, ControllerKeyBindingAssignment> assignments = ControllerForgeBindings.all();
        Map<String, Boolean> controllerDown = new HashMap<>();
        addLogical(controllerDown, minecraft.gameSettings.keyBindAttack, InputAction.PLAYER_ATTACK);
        addLogical(controllerDown, minecraft.gameSettings.keyBindUseItem, InputAction.PLAYER_USE);
        addLogical(controllerDown, minecraft.gameSettings.keyBindPickBlock, InputAction.PLAYER_PICK_BLOCK);
        addLogical(controllerDown, minecraft.gameSettings.keyBindDrop, InputAction.PLAYER_DROP);
        addLogical(controllerDown, minecraft.gameSettings.keyBindInventory, InputAction.PLAYER_INVENTORY);
        addLogical(controllerDown, minecraft.gameSettings.keyBindSwapHands, InputAction.PLAYER_SWAP_HANDS);
        addLogical(controllerDown, minecraft.gameSettings.keyBindSprint, InputAction.PLAYER_SPRINT);
        Map<String, KeyBinding> targets = new LinkedHashMap<>();
        for (String description : assignments.keySet()) {
            KeyBinding binding = registered.get(description);
            if (binding != null) targets.put(description, binding);
        }
        for (String description : controllerDown.keySet()) {
            KeyBinding binding = registered.get(description);
            if (binding != null) targets.put(description, binding);
        }
        for (Map.Entry<String, KeyBinding> entry : new HashMap<>(touched).entrySet()) {
            if (!targets.containsKey(entry.getKey()) || targets.get(entry.getKey()) != entry.getValue()) {
                access(entry.getValue()).nfr$clearControllerInput();
                touched.remove(entry.getKey());
                previousDown.remove(entry.getKey());
            }
        }

        ControllerSnapshot snapshot = manager.latestSnapshot();
        for (Map.Entry<String, KeyBinding> entry : targets.entrySet()) {
            ControllerKeyBindingAssignment target = assignments.get(entry.getKey());
            boolean down = target != null && target.isDown(snapshot.get(target.control()));
            down |= controllerDown.getOrDefault(entry.getKey(), false);
            boolean wasDown = previousDown.getOrDefault(entry.getKey(), false);
            access(entry.getValue()).nfr$setControllerInput(down, down && !wasDown);
            previousDown.put(entry.getKey(), down);
            touched.put(entry.getKey(), entry.getValue());
        }
    }

    private void addLogical(Map<String, Boolean> output, KeyBinding binding, InputAction action) {
        if (binding == null) return;
        if (!ControllerInputMode.current().accepts(action)) return;
        if (InputApi.isBlocked(action)) return;
        neofontrender.addons.api.input.InputValue value =
                ControllerBindings.resolve(action, manager.latestSnapshot());
        boolean down = value.isDown() || Math.abs(value.getAxis()) >= 0.55F;
        output.merge(binding.getKeyDescription(), down, ControllerKeyBindingBridge::logicalOr);
    }

    private static boolean logicalOr(boolean left, boolean right) { return left || right; }

    private void clear() {
        for (KeyBinding binding : touched.values()) access(binding).nfr$clearControllerInput();
        touched.clear();
        previousDown.clear();
    }

    private static NfrKeyBindingControllerInput access(KeyBinding binding) {
        return (NfrKeyBindingControllerInput) (Object) binding;
    }
}
