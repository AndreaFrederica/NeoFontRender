package neofontrender.addons.vendor.tabbychat.gui.settings;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import neofontrender.addons.vendor.tabbychat.ChatManager;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.foundation.Color;
import neofontrender.addons.vendor.tabbychat.foundation.ILocation;
import neofontrender.addons.vendor.tabbychat.foundation.Location;
import neofontrender.addons.vendor.tabbychat.foundation.config.SettingsFile;
import neofontrender.addons.vendor.tabbychat.foundation.gui.*;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.SettingPanel;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.ActionPerformedEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class GuiSettingsScreen extends ComponentScreen {

    private static Map<Class<? extends SettingPanel<?>>, Supplier<? extends SettingPanel<?>>> settings = Maps.newLinkedHashMap();

    static {
        registerSetting(GuiSettingsGeneral.class, GuiSettingsGeneral::new);
        registerSetting(GuiSettingsServer.class, GuiSettingsServer::new);
        registerSetting(GuiSettingFilters.class, GuiSettingFilters::new);
        registerSetting(GuiSettingsChannel.class, GuiSettingsChannel::new);
        registerSetting(GuiAdvancedSettings.class, GuiAdvancedSettings::new);
    }

    private List<SettingPanel<?>> panels = Lists.newArrayList();

    private GuiPanel panel;

    private GuiPanel settingsList;
    private SettingPanel<?> selectedSetting;

    public GuiSettingsScreen(SettingPanel<?> setting) {
        this.selectedSetting = setting;

        for (Map.Entry<Class<? extends SettingPanel<?>>, Supplier<? extends SettingPanel<?>>> sett : settings.entrySet()) {
            try {
                if (setting != null && setting.getClass() == sett.getKey()) {
                    panels.add(setting);
                } else {
                    panels.add(sett.getValue().get());
                }
            } catch (Exception e) {
                TabbyChat.getLogger().error("Unable to add " + sett.getKey().getName() + " as a setting.", e);
            }
        }
    }

    @Override
    public void initGui() {


        getPanel().addComponent(panel = new GuiPanel(new BorderLayout()));

        int x = this.width / 2 - 300 / 2;
        int y = this.height / 2 - 200 / 2;
        panel.setLocation(new Location(x, y, 300, 200));

        GuiPanel panel = new GuiPanel(new BorderLayout());
        this.panel.addComponent(panel, BorderLayout.Position.WEST);
        panel.addComponent(settingsList = new GuiPanel(new VerticalLayout()), BorderLayout.Position.WEST);

        GuiButton close = new GuiButton(I18n.format("gui.done"));
        close.setLocation(new Location(0, 0, 40, 10));
        close.setSecondaryColor(Color.of(0, 255, 0, 127));
        close.getBus().register(new Object() {
            @Subscribe
            public void closeTheScreen(ActionPerformedEvent event) {
                Minecraft.getMinecraft().displayGuiScreen(null);
            }
        });
        panel.addComponent(close, BorderLayout.Position.SOUTH);

        {
            // Populate the settings
            for (SettingPanel<?> sett : panels) {
                SettingsButton button = new SettingsButton(sett);
                button.getBus().register(new Object() {
                    @Subscribe
                    public void switchToThisPanel(ActionPerformedEvent event) {
                        selectSetting(((SettingsButton) event.getComponent()).getSettings());
                    }
                });
                settingsList.addComponent(button);
                sett.initGUI();
            }
        }
        SettingPanel<?> panelClass;
        if (selectedSetting == null) {
            panelClass = panels.get(0);
        } else {
            panelClass = selectedSetting;
        }
        selectSetting(panelClass);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        for (SettingPanel<?> settingPanel : panels) {
            SettingsFile config = settingPanel.getSettings();
            config.saveConfig();
            //LiteLoader.getInstance().writeConfig(config);
        }
        ((ChatManager) TabbyChat.getInstance().getChat()).getChatBox().getChatArea().markDirty();
    }

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        this.panels.forEach(GuiPanel::clearComponents);
        super.setWorldAndResolution(mc, width, height);
    }

    private void deactivateAll() {
        for (GuiComponent comp : settingsList) {
            if (comp instanceof SettingsButton) {
                ((SettingsButton) comp).setActive(false);
            }
        }
    }

    private <T extends SettingPanel<?>> void activate(Class<T> settingClass) {
        for (GuiComponent comp : settingsList) {
            if (comp instanceof SettingsButton
                    && ((SettingsButton) comp).getSettings().getClass().equals(settingClass)) {
                ((SettingsButton) comp).setActive(true);
                break;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float tick) {
        // drawDefaultBackground();
        ILocation rect = panel.getLocation();
        Gui.drawRect(rect.getXPos(), rect.getYPos(), rect.getXWidth(), rect.getYHeight(), Integer.MIN_VALUE);
        super.drawScreen(mouseX, mouseY, tick);
    }

    private void selectSetting(SettingPanel<?> setting) {
//        setting.clearComponents();
        deactivateAll();
        panel.removeComponent(selectedSetting);
        selectedSetting = setting;
        activate(setting.getClass());
        this.panel.addComponent(this.selectedSetting, BorderLayout.Position.CENTER);
    }

    private static <T extends SettingPanel<?>> void registerSetting(Class<T> settings, Supplier<T> constructor) {
        if (!GuiSettingsScreen.settings.containsKey(settings)) {
            GuiSettingsScreen.settings.put(settings, constructor);
        }
    }

}
