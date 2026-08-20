package neofontrender.addons.controller.sdl;

import dev.isxander.sdl.Sdl;
import dev.isxander.sdl.SdlEvent;
import dev.isxander.sdl.SdlJoystickId;
import neofontrender.addons.api.input.InputDeviceSample;
import neofontrender.addons.controller.ControllerConfig;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static dev.isxander.sdl.SdlEvents.SDL_EVENT_JOYSTICK_ADDED;
import static dev.isxander.sdl.SdlEvents.SDL_EVENT_JOYSTICK_REMOVED;
import static dev.isxander.sdl.SdlInit.SDL_INIT_EVENTS;
import static dev.isxander.sdl.SdlInit.SDL_INIT_GAMEPAD;
import static dev.isxander.sdl.SdlInit.SDL_INIT_JOYSTICK;

/**
 * Owns SDL initialization, game-controller discovery, hot-plug events, and open handles.
 *
 * <p>All SDL calls except one-time {@code SDL_Init} run on a dedicated daemon poll thread.
 * Windows periodically re-enumerates HID devices while SDL pumps its event queue, which can
 * stall the calling thread for seconds; keeping that work off the Minecraft render thread
 * prevents the window from being ghosted by the DWM ("未响应"/白屏/任务栏图标闪烁).
 * Main-thread accessors only read {@code volatile} published snapshots.</p>
 */
public final class SdlDeviceManager implements AutoCloseable {
    private static final String GAMEPAD_DB =
            "/assets/neofontrender_ui_enhancements_controller/controllers/gamecontrollerdb-sdl3.txt";
    private static final long POLL_INTERVAL_MILLIS = 8L;
    private static final long CLOSE_JOIN_TIMEOUT_MILLIS = 2_000L;

    private final Sdl sdl;
    private final Logger logger;
    private final Thread pollThread;
    private final AtomicReference<ResourceLocation> pendingSelection = new AtomicReference<>();
    /** Community mapping db indexed by GUID; only applied to devices SDL does not recognize. */
    private final Map<String, String> gamepadMappings;

    /** SDL-poll-thread-only state below; never touched from the Minecraft thread. */
    private final Map<Integer, SdlControllerDevice> devices = new TreeMap<>();
    private final SdlEvent event = new SdlEvent();
    private Integer selectedDevice;
    private String preferredDeviceKey;

    private volatile ControllerSnapshot latestSnapshot = ControllerSnapshot.disconnected();
    private volatile List<ControllerSnapshot> connectedSnapshots = Collections.emptyList();
    private volatile boolean closed;

    private SdlDeviceManager(Sdl sdl, Logger logger) {
        this.sdl = sdl;
        this.logger = logger;
        this.preferredDeviceKey = ControllerConfig.selectedDeviceKey();
        this.gamepadMappings = loadGamepadMappings(logger);
        this.pollThread = new Thread(this::pollLoop, "Revo UI SDL controller poll");
        this.pollThread.setDaemon(true);
    }

    /** Opens SDL once for this addon. Returns {@code null} when the optional native runtime is absent. */
    public static SdlDeviceManager open(Logger logger) {
        Sdl sdl = SdlRuntime.open(logger);
        if (sdl == null) return null;
        SdlDeviceManager manager = new SdlDeviceManager(sdl, logger);
        manager.pollThread.start();
        return manager;
    }

    /** Latest snapshot as an NFR device sample; pure memory read, no SDL calls. */
    public InputDeviceSample sample() {
        ControllerSnapshot snapshot = latestSnapshot();
        return snapshot.isConnected() ? snapshot.toDeviceSample() : emptySample();
    }

    /** Latest snapshot published by the SDL poll thread; never blocks on SDL. */
    public ControllerSnapshot pollSnapshot() {
        return latestSnapshot();
    }

    /** Latest raw linear sample, retained for the embedded NFR diagnostics components. */
    public ControllerSnapshot latestSnapshot() { return latestSnapshot; }

    public List<ControllerSnapshot> connectedSnapshots() {
        return connectedSnapshots;
    }

    /** Requests a device selection; applied by the poll thread within one poll cycle. */
    public void selectDevice(ResourceLocation deviceId) {
        if (deviceId == null) return;
        pendingSelection.set(deviceId);
    }

    public int connectedDeviceCount() {
        return connectedSnapshots.size();
    }

    // ------------------------------------------------------------------
    // Everything below runs exclusively on the SDL poll thread.
    // ------------------------------------------------------------------

    private void pollLoop() {
        try {
            discover();
            while (!closed) {
                try {
                    pollCycle();
                } catch (RuntimeException error) {
                    logger.warn("SDL controller poll cycle failed; retrying", error);
                }
                try {
                    Thread.sleep(POLL_INTERVAL_MILLIS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } finally {
            for (SdlControllerDevice device : devices.values()) device.close();
            devices.clear();
            latestSnapshot = ControllerSnapshot.disconnected();
            connectedSnapshots = Collections.emptyList();
        }
    }

    private void pollCycle() {
        ResourceLocation selection = pendingSelection.getAndSet(null);
        pollEvents();
        // SDL_PollEvent pumps the native event queue and refreshes joystick/gamepad state.
        // Calling both explicit update functions as well caused redundant HID scans on Windows,
        // which can briefly stall the calling thread when multiple HID/XInput devices exist.

        Map<Integer, ControllerSnapshot> updated = new LinkedHashMap<>();
        Iterator<Map.Entry<Integer, SdlControllerDevice>> iterator = devices.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, SdlControllerDevice> entry = iterator.next();
            SdlControllerDevice device = entry.getValue();
            ControllerSnapshot snapshot = device.snapshot();
            if (snapshot != null) {
                updated.put(entry.getKey(), snapshot);
                continue;
            }
            device.close();
            iterator.remove();
        }
        if (selection != null) applySelection(selection);
        selectedDevice = selectDevice(updated);
        connectedSnapshots = Collections.unmodifiableList(new ArrayList<>(updated.values()));
        latestSnapshot = selectedDevice == null ? ControllerSnapshot.disconnected()
                : updated.get(selectedDevice);
    }

    private void applySelection(ResourceLocation deviceId) {
        for (Map.Entry<Integer, SdlControllerDevice> entry : devices.entrySet()) {
            SdlControllerDevice device = entry.getValue();
            if (deviceId.equals(device.deviceId())) {
                selectedDevice = entry.getKey();
                preferredDeviceKey = device.persistentId();
                try {
                    ControllerConfig.setSelectedDeviceKey(preferredDeviceKey);
                    ControllerConfig.save();
                } catch (RuntimeException error) {
                    logger.warn("Unable to persist selected SDL controller", error);
                }
                return;
            }
        }
    }

    private Integer selectDevice(Map<Integer, ControllerSnapshot> available) {
        Map<Integer, String> identities = new LinkedHashMap<>();
        for (Integer id : available.keySet()) {
            SdlControllerDevice device = devices.get(id);
            if (device != null) identities.put(id, device.persistentId());
        }
        return SdlDevicePreference.choose(identities, selectedDevice, preferredDeviceKey);
    }

    private void discover() {
        SdlJoystickId[] discovered = sdl.joystick().SDL_GetJoysticks();
        if (discovered == null) return;
        for (SdlJoystickId id : discovered) connect(id);
    }

    private void pollEvents() {
        while (sdl.events().SDL_PollEvent(event)) {
            if (event.type() == SDL_EVENT_JOYSTICK_ADDED && event.data() instanceof SdlEvent.JoyDevice) {
                connect(((SdlEvent.JoyDevice) event.data()).which());
            } else if (event.type() == SDL_EVENT_JOYSTICK_REMOVED
                    && event.data() instanceof SdlEvent.JoyDevice) {
                disconnect(((SdlEvent.JoyDevice) event.data()).which());
            }
        }
    }

    private void connect(SdlJoystickId id) {
        if (id == null || devices.containsKey(id.value())) return;
        try {
            applyBundledMapping(id);
            SdlControllerDevice device = SdlControllerDevice.open(sdl, id);
            devices.put(device.sdlId(), device);
            logger.info("Connected SDL {} {}", sdl.gamepad().SDL_IsGamepad(id) ? "gamepad" : "joystick",
                    id.value());
            if (device.isGamepad()) logger.info("SDL gamepad mapping {}: {}", id.value(),
                    device.mapping());
        } catch (RuntimeException error) {
            logger.warn("Unable to open SDL controller {}", id.value(), error);
        }
    }

    private void disconnect(SdlJoystickId id) {
        if (id == null) return;
        SdlControllerDevice removed = devices.remove(id.value());
        if (selectedDevice != null && selectedDevice == id.value()) selectedDevice = null;
        if (removed != null) {
            removed.close();
            logger.info("Disconnected SDL controller {}", id.value());
        }
    }

    /**
     * Feeds the bundled community mapping db only to devices SDL does not already recognize,
     * so SDL's built-in/native mapping always wins. Eagerly registering all db entries used to
     * override SDL's own mapping for known controllers, which scrambled stick-click buttons on
     * some Xbox controllers.
     */
    private void applyBundledMapping(SdlJoystickId id) {
        if (sdl.gamepad().SDL_IsGamepad(id)) return;
        String guid = String.valueOf(sdl.joystick().SDL_GetJoystickGUIDForID(id))
                .toLowerCase(Locale.ROOT);
        String mapping = gamepadMappings.get(guid);
        if (mapping == null) return;
        if (sdl.gamepad().SDL_AddGamepadMapping(mapping) >= 0) {
            logger.info("Applied bundled gamepad mapping for unrecognized controller {}", guid);
        }
    }

    private static Map<String, String> loadGamepadMappings(Logger logger) {
        Map<String, String> mappings = new LinkedHashMap<>();
        try (InputStream stream = SdlDeviceManager.class.getResourceAsStream(GAMEPAD_DB)) {
            if (stream == null) {
                logger.warn("Bundled SDL gamepad mapping database is missing");
                return mappings;
            }
            for (String line : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                String mapping = line.trim();
                int comma = mapping.indexOf(',');
                if (comma > 0 && !mapping.startsWith("#")) {
                    mappings.put(mapping.substring(0, comma).toLowerCase(Locale.ROOT), mapping);
                }
            }
            logger.info("Indexed {} bundled SDL gamepad mappings", mappings.size());
        } catch (IOException | RuntimeException error) {
            logger.warn("Unable to load bundled SDL gamepad mappings", error);
        }
        return mappings;
    }

    private static InputDeviceSample emptySample() {
        return InputDeviceSample.builder(ControllerControls.DISCONNECTED_DEVICE).build();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        pollThread.interrupt();
        try {
            pollThread.join(CLOSE_JOIN_TIMEOUT_MILLIS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        sdl.init().SDL_QuitSubSystem(SDL_INIT_JOYSTICK | SDL_INIT_GAMEPAD | SDL_INIT_EVENTS);
    }
}
