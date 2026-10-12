package neofontrender.addons.controller;

import neofontrender.addons.controller.sdl.ControllerSnapshot;
import neofontrender.addons.controller.sdl.SdlDeviceManager;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.List;

/** One diagnostics sample shared by all widgets during an NFR page draw. */
final class ControllerWorkbenchModel {
    private ControllerSnapshot snapshot = ControllerSnapshot.disconnected();
    private List<ControllerSnapshot> devices = Collections.emptyList();

    void refresh() {
        SdlDeviceManager manager = ControllerAddonMod.deviceManager();
        if (manager == null) {
            snapshot = ControllerSnapshot.disconnected();
            devices = Collections.emptyList();
        } else {
            snapshot = manager.pollSnapshot();
            devices = manager.connectedSnapshots();
        }
    }

    ControllerSnapshot snapshot() { return snapshot; }
    List<ControllerSnapshot> devices() { return devices; }

    void select(ResourceLocation deviceId) {
        SdlDeviceManager manager = ControllerAddonMod.deviceManager();
        if (manager == null) return;
        manager.selectDevice(deviceId);
        snapshot = manager.latestSnapshot();
        devices = manager.connectedSnapshots();
    }
}
