package neofontrender.addons.controller.sdl;

import dev.isxander.sdl.Sdl;
import dev.isxander.sdl.SdlEvent;
import dev.isxander.sdl.SdlGamepadHandle;
import dev.isxander.sdl.SdlJoystickId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.isxander.sdl.SdlEvents.SDL_EVENT_JOYSTICK_ADDED;
import static dev.isxander.sdl.SdlEvents.SDL_EVENT_JOYSTICK_REMOVED;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_AXIS_LEFTX;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_AXIS_LEFTY;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_AXIS_LEFT_TRIGGER;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_AXIS_RIGHTX;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_AXIS_RIGHTY;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_AXIS_RIGHT_TRIGGER;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_BACK;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_DPAD_DOWN;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_DPAD_LEFT;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_DPAD_RIGHT;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_DPAD_UP;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_EAST;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_GUIDE;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_LEFT_SHOULDER;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_LEFT_STICK;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_MISC1;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_NORTH;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_RIGHT_STICK;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_SOUTH;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_START;
import static dev.isxander.sdl.SdlGamepad.SDL_GAMEPAD_BUTTON_WEST;

/**
 * Manual out-of-game probe with a Swing UI: connect an Xbox (or any SDL gamepad) controller,
 * press every button / move every stick, and the panel lights up while a coverage checklist
 * tracks which controls SDL recognized. Close the window (or hold View+Menu) to print the
 * final coverage report. Run via {@code gradlew :addons:ui-enhancements-controller:probeGamepad}.
 */
public final class SdlGamepadProbe {
    private static final Logger LOGGER = LogManager.getLogger("Revo UI SDL gamepad probe");
    private static final long POLL_INTERVAL_MILLIS = 30L;
    private static final float AXIS_COVERAGE_THRESHOLD = 0.5F;

    private record ButtonSpec(int sdlButton, String label) {}
    private record AxisSpec(int sdlAxis, String label, boolean trigger) {}
    private record DeviceItem(SdlJoystickId id, boolean gamepad) {
        @Override public String toString() {
            return (gamepad ? "Gamepad" : "Joystick") + " #" + id.value();
        }
    }

    private static final ButtonSpec[] BUTTONS = {
            new ButtonSpec(SDL_GAMEPAD_BUTTON_SOUTH, "A"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_EAST, "B"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_WEST, "X"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_NORTH, "Y"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_LEFT_SHOULDER, "LB"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER, "RB"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_BACK, "View"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_START, "Menu"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_GUIDE, "Xbox"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_LEFT_STICK, "LS"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_RIGHT_STICK, "RS"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_MISC1, "分享"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_DPAD_UP, "十字↑"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_DPAD_DOWN, "十字↓"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_DPAD_LEFT, "十字←"),
            new ButtonSpec(SDL_GAMEPAD_BUTTON_DPAD_RIGHT, "十字→"),
    };
    private static final AxisSpec[] AXES = {
            new AxisSpec(SDL_GAMEPAD_AXIS_LEFTX, "左摇杆 X", false),
            new AxisSpec(SDL_GAMEPAD_AXIS_LEFTY, "左摇杆 Y", false),
            new AxisSpec(SDL_GAMEPAD_AXIS_RIGHTX, "右摇杆 X", false),
            new AxisSpec(SDL_GAMEPAD_AXIS_RIGHTY, "右摇杆 Y", false),
            new AxisSpec(SDL_GAMEPAD_AXIS_LEFT_TRIGGER, "LT 扳机", true),
            new AxisSpec(SDL_GAMEPAD_AXIS_RIGHT_TRIGGER, "RT 扳机", true),
    };

    private SdlGamepadProbe() {}

    public static void main(String[] args) {
        Sdl sdl = SdlRuntime.open(LOGGER);
        if (sdl == null) {
            System.err.println("Bundled SDL3 native failed to initialize; see log above.");
            return;
        }
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default look and feel.
        }
        SwingUtilities.invokeLater(() -> new ProbeFrame(sdl).setVisible(true));
    }

    /** Colored button indicator that latches a green "recognized" border on first activation. */
    private static final class ButtonIndicator extends JPanel {
        private static final Color IDLE = new Color(60, 63, 65);
        private static final Color DOWN = new Color(46, 160, 67);
        private final String text;
        private final JLabel label;
        private boolean covered;

        ButtonIndicator(String text) {
            this.text = text;
            setLayout(new BorderLayout());
            setBackground(IDLE);
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
            label = new JLabel(text, SwingConstants.CENTER);
            label.setForeground(Color.WHITE);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
            add(label, BorderLayout.CENTER);
            setPreferredSize(new Dimension(86, 44));
        }

        void setDown(boolean down) {
            setBackground(down ? DOWN : IDLE);
            if (down && !covered) {
                covered = true;
                setBorder(BorderFactory.createLineBorder(new Color(0, 200, 90), 3));
                label.setText(text + " ✓");
            }
        }

        boolean covered() { return covered; }
    }

    /** Axis bar: sticks sweep -1..1, triggers sweep 0..1; latches coverage past the threshold. */
    private static final class AxisBar extends JPanel {
        private final JProgressBar bar;
        private final boolean trigger;
        private boolean covered;

        AxisBar(String text, boolean trigger) {
            this.trigger = trigger;
            setLayout(new BorderLayout(8, 0));
            JLabel label = new JLabel(text);
            label.setPreferredSize(new Dimension(70, 24));
            add(label, BorderLayout.WEST);
            bar = new JProgressBar(0, 200);
            bar.setStringPainted(true);
            bar.setString("—");
            add(bar, BorderLayout.CENTER);
        }

        void setValue(float normalized) {
            bar.setValue(trigger
                    ? Math.round(normalized * 200.0F)
                    : Math.round((normalized + 1.0F) * 100.0F));
            bar.setString(String.format("%+.2f", normalized));
            if (!covered && Math.abs(normalized) >= AXIS_COVERAGE_THRESHOLD) {
                covered = true;
                bar.setForeground(new Color(0, 200, 90));
            }
        }

        boolean covered() { return covered; }
    }

    private static final class ProbeFrame extends JFrame {
        private final Sdl sdl;
        private final JComboBox<DeviceItem> deviceCombo = new JComboBox<>();
        private final JLabel statusLabel = new JLabel("正在枚举设备…");
        private final JLabel coverageLabel = new JLabel();
        private final Map<Integer, ButtonIndicator> buttonIndicators = new LinkedHashMap<>();
        private final Map<Integer, AxisBar> axisBars = new LinkedHashMap<>();
        private final Thread pollThread;
        private final java.util.concurrent.atomic.AtomicReference<SdlJoystickId> pendingOpen =
                new java.util.concurrent.atomic.AtomicReference<>();

        /** Poll-thread-only device state below. */
        private final List<DeviceItem> devices = new ArrayList<>();
        private final SdlEvent event = new SdlEvent();
        private SdlGamepadHandle gamepad;
        private int openDeviceId = -1;
        private volatile boolean running = true;

        ProbeFrame(Sdl sdl) {
            super("SDL Gamepad 探针 — 按下所有按键测试识别");
            this.sdl = sdl;
            buildUi();
            pollThread = new Thread(this::pollLoop, "SDL gamepad probe poll");
            pollThread.setDaemon(true);
            pollThread.start();
            addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { shutdown(); }
            });
        }

        private void buildUi() {
            for (ButtonSpec spec : BUTTONS) buttonIndicators.put(spec.sdlButton(), new ButtonIndicator(spec.label()));
            for (AxisSpec spec : AXES) axisBars.put(spec.sdlAxis(), new AxisBar(spec.label(), spec.trigger()));

            JPanel top = new JPanel(new BorderLayout(8, 0));
            deviceCombo.addActionListener(e -> {
                DeviceItem item = (DeviceItem) deviceCombo.getSelectedItem();
                if (item != null && item.gamepad() && item.id().value() != openDeviceId) {
                    pendingOpen.set(item.id());
                }
            });
            top.add(deviceCombo, BorderLayout.CENTER);
            top.add(statusLabel, BorderLayout.EAST);

            JPanel buttons = new JPanel(new GridLayout(4, 4, 6, 6));
            buttons.setBorder(BorderFactory.createTitledBorder("按键（亮起 = 当前按下，绿框✓ = 已识别过）"));
            for (ButtonIndicator indicator : buttonIndicators.values()) buttons.add(indicator);

            JPanel axes = new JPanel(new GridLayout(AXES.length, 1, 4, 4));
            axes.setBorder(BorderFactory.createTitledBorder("摇杆 / 扳机（绿色 = 已识别过）"));
            for (AxisBar axisBar : axisBars.values()) axes.add(axisBar);

            JPanel center = new JPanel(new BorderLayout(8, 8));
            center.add(buttons, BorderLayout.CENTER);
            center.add(axes, BorderLayout.SOUTH);

            coverageLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            updateCoverageLabel();

            JPanel root = new JPanel(new BorderLayout(8, 8));
            root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            root.add(top, BorderLayout.NORTH);
            root.add(center, BorderLayout.CENTER);
            root.add(coverageLabel, BorderLayout.SOUTH);
            setContentPane(root);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            pack();
            setMinimumSize(getSize());
            setLocationRelativeTo(null);
        }

        // ---------------- UI-thread helpers ----------------

        private void setStatus(String text) {
            SwingUtilities.invokeLater(() -> statusLabel.setText(text));
        }

        private void refreshDeviceCombo() {
            SwingUtilities.invokeLater(() -> {
                int wanted = openDeviceId;
                deviceCombo.removeAllItems();
                for (DeviceItem item : devices) {
                    deviceCombo.addItem(item);
                    if (item.id().value() == wanted) deviceCombo.setSelectedItem(item);
                }
                if (deviceCombo.getSelectedItem() == null && deviceCombo.getItemCount() > 0) {
                    deviceCombo.setSelectedIndex(0);
                }
            });
        }

        private void updateCoverageLabel() {
            long covered = buttonIndicators.values().stream().filter(ButtonIndicator::covered).count()
                    + axisBars.values().stream().filter(AxisBar::covered).count();
            int total = BUTTONS.length + AXES.length;
            coverageLabel.setText("识别覆盖: " + covered + " / " + total
                    + "（关闭窗口或按住 View+Menu 退出并输出报告）");
        }

        // ---------------- Poll thread (owns every SDL call) ----------------

        private void pollLoop() {
            try {
                discover();
                while (running) {
                    try {
                        SdlJoystickId requested = pendingOpen.getAndSet(null);
                        if (requested != null) openGamepad(requested);
                        drainEvents();
                        sampleGamepad();
                    } catch (RuntimeException error) {
                        LOGGER.warn("Probe poll failed; retrying", error);
                    }
                    try {
                        Thread.sleep(POLL_INTERVAL_MILLIS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            } finally {
                closeGamepad();
            }
        }

        private void openGamepad(SdlJoystickId id) {
            closeGamepad();
            gamepad = sdl.gamepad().SDL_OpenGamepad(id);
            if (gamepad == null) {
                System.err.println("无法打开设备 #" + id.value() + ": " + sdl.error().SDL_GetError());
                return;
            }
            openDeviceId = id.value();
            String name = sdl.gamepad().SDL_GetGamepadName(gamepad);
            String mapping = sdl.gamepad().SDL_GetGamepadMapping(gamepad);
            setStatus(name == null ? "已连接 #" + id.value() : name);
            System.out.println("已打开: " + name);
            System.out.println("映射: " + mapping);
        }

        private void closeGamepad() {
            if (gamepad == null) return;
            sdl.gamepad().SDL_CloseGamepad(gamepad);
            gamepad = null;
            openDeviceId = -1;
        }

        private void discover() {
            devices.clear();
            SdlJoystickId[] found = sdl.joystick().SDL_GetJoysticks();
            if (found != null) {
                for (SdlJoystickId id : found) {
                    devices.add(new DeviceItem(id, sdl.gamepad().SDL_IsGamepad(id)));
                }
            }
            refreshDeviceCombo();
            if (gamepad == null && pendingOpen.get() == null) {
                for (DeviceItem item : devices) {
                    if (item.gamepad()) {
                        pendingOpen.set(item.id());
                        break;
                    }
                }
                setStatus(devices.isEmpty() ? "未检测到手柄，请连接 Xbox 手柄…" : "请选择设备");
            }
        }

        private void drainEvents() {
            while (sdl.events().SDL_PollEvent(event)) {
                if (!(event.data() instanceof SdlEvent.JoyDevice joy)) continue;
                if (event.type() == SDL_EVENT_JOYSTICK_ADDED) {
                    System.out.println("设备插入 #" + joy.which().value());
                    discover();
                } else if (event.type() == SDL_EVENT_JOYSTICK_REMOVED) {
                    System.out.println("设备拔出 #" + joy.which().value());
                    if (joy.which().value() == openDeviceId) {
                        closeGamepad();
                        setStatus("手柄已拔出");
                    }
                    discover();
                }
            }
        }

        private void sampleGamepad() {
            if (gamepad == null) return;
            Map<Integer, Boolean> buttonStates = new LinkedHashMap<>();
            for (ButtonSpec spec : BUTTONS) {
                buttonStates.put(spec.sdlButton(),
                        sdl.gamepad().SDL_GamepadHasButton(gamepad, spec.sdlButton())
                                && sdl.gamepad().SDL_GetGamepadButton(gamepad, spec.sdlButton()));
            }
            Map<Integer, Float> axisValues = new LinkedHashMap<>();
            for (AxisSpec spec : AXES) {
                axisValues.put(spec.sdlAxis(),
                        sdl.gamepad().SDL_GamepadHasAxis(gamepad, spec.sdlAxis())
                                ? AxisNormalizer.normalize(
                                        sdl.gamepad().SDL_GetGamepadAxis(gamepad, spec.sdlAxis()), 0.0F)
                                : 0.0F);
            }
            SwingUtilities.invokeLater(() -> {
                boolean changed = false;
                for (Map.Entry<Integer, Boolean> entry : buttonStates.entrySet()) {
                    ButtonIndicator indicator = buttonIndicators.get(entry.getKey());
                    boolean wasCovered = indicator.covered();
                    indicator.setDown(entry.getValue());
                    changed |= indicator.covered() != wasCovered;
                }
                for (Map.Entry<Integer, Float> entry : axisValues.entrySet()) {
                    AxisBar axisBar = axisBars.get(entry.getKey());
                    boolean wasCovered = axisBar.covered();
                    axisBar.setValue(entry.getValue());
                    changed |= axisBar.covered() != wasCovered;
                }
                if (changed) updateCoverageLabel();
            });
            if (Boolean.TRUE.equals(buttonStates.get(SDL_GAMEPAD_BUTTON_BACK))
                    && Boolean.TRUE.equals(buttonStates.get(SDL_GAMEPAD_BUTTON_START))) {
                System.out.println("检测到 View+Menu 组合键，退出探针");
                SwingUtilities.invokeLater(this::dispose);
            }
        }

        // ---------------- Shutdown (UI thread) ----------------

        private void shutdown() {
            running = false;
            pollThread.interrupt();
            try {
                pollThread.join(1_000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
            printCoverageReport();
            sdl.init().SDL_Quit();
        }

        private void printCoverageReport() {
            System.out.println();
            System.out.println("================ 按键识别覆盖报告 ================");
            List<String> missed = new ArrayList<>();
            for (ButtonSpec spec : BUTTONS) {
                boolean covered = buttonIndicators.get(spec.sdlButton()).covered();
                if (!covered) missed.add(spec.label());
                System.out.printf("%-8s %s%n", spec.label(), covered ? "已识别 ✓" : "未触发 ✗");
            }
            for (AxisSpec spec : AXES) {
                boolean covered = axisBars.get(spec.sdlAxis()).covered();
                if (!covered) missed.add(spec.label());
                System.out.printf("%-8s %s%n", spec.label(), covered ? "已识别 ✓" : "未触发 ✗");
            }
            System.out.println("================================================");
            System.out.println(missed.isEmpty()
                    ? "全部控件识别正常！"
                    : "未识别的控件: " + String.join(", ", missed)
                            + "（若测试时未按下请忽略；按下仍不亮则说明识别异常）");
        }
    }
}
