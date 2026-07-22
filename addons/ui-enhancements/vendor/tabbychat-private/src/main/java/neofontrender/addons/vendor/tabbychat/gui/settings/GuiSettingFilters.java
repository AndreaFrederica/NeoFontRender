package neofontrender.addons.vendor.tabbychat.gui.settings;

import com.google.common.eventbus.Subscribe;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import neofontrender.addons.vendor.tabbychat.extra.filters.GuiFilterEditor;
import neofontrender.addons.vendor.tabbychat.extra.filters.UserFilter;
import neofontrender.addons.vendor.tabbychat.settings.GeneralServerSettings;
import neofontrender.addons.vendor.tabbychat.settings.ServerSettings;
import neofontrender.addons.vendor.tabbychat.foundation.Color;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiButton;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiGridLayout;
import neofontrender.addons.vendor.tabbychat.foundation.gui.GuiLabel;
import neofontrender.addons.vendor.tabbychat.foundation.gui.config.SettingPanel;
import neofontrender.addons.vendor.tabbychat.foundation.gui.events.ActionPerformedEvent;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ChatComponentText;

import static neofontrender.addons.vendor.tabbychat.util.Translation.*;

public class GuiSettingFilters extends SettingPanel<ServerSettings> {
    private int index = 0;
    private GuiButton prev;
    private GuiButton edit;
    private GuiButton next;
    private GuiButton delete;
    private GuiLabel lblFilter;
    private GuiLabel lblPattern;

    GuiSettingFilters(){
        this.setLayout(new GuiGridLayout(10, 20));
        this.setDisplayString(I18n.format(SETTINGS_FILTERS));
        this.setSecondaryColor(Color.of(0, 215, 0, 64));
    }

    @Override
    public void initGUI() {
        GeneralServerSettings sett = getSettings().general;
        index = getSettings().filters.get().size() - 1;
        // Filters
        int pos = 1;
        prev = new GuiButton("<");
        prev.getBus().register(new Object() {
            @Subscribe
            public void goBackwards(ActionPerformedEvent event) {
                select(index - 1);
            }
        });
        this.addComponent(prev, new int[]{0, pos, 1, 2});

        edit = new GuiButton(I18n.format("selectServer.edit"));
        edit.getBus().register(new Object() {
            @Subscribe
            public void goEditwords(ActionPerformedEvent event) {
                edit(index);
            }
        });
        this.addComponent(edit, new int[]{1, pos, 2, 2});

        next = new GuiButton(">");
        next.getBus().register(new Object() {
            @Subscribe
            public void goForwards(ActionPerformedEvent event) {
                select(index + 1);
            }
        });
        this.addComponent(next, new int[]{3, pos, 1, 2});

        this.addComponent(lblFilter = new GuiLabel(), new int[]{5, pos+1});
        this.addComponent(lblPattern = new GuiLabel(), new int[]{5, pos + 2});
        GuiButton _new = new GuiButton(I18n.format(FILTERS_NEW));
        _new.getBus().register(new Object() {
            @Subscribe
            public void goAddwords(ActionPerformedEvent event) {
                add();
            }
        });

        pos += 2;
        this.addComponent(_new, new int[]{0, pos, 2, 2});
        delete = new GuiButton(I18n.format("selectServer.delete"));
        delete.getBus().register(new Object() {
            @Subscribe
            public void goDelwords(ActionPerformedEvent event) {
                delete(index);
            }
        });
        this.addComponent(delete, new int[]{2, pos, 2, 2});
        prev.setEnabled(false);
        if (index == -1) {
            delete.setEnabled(false);
            edit.setEnabled(false);
            next.setEnabled(false);
        }

        update();
    }

    @Override
    public ServerSettings getSettings() {
        return TabbyChat.getInstance().serverSettings;
    }

    private void select(int i) {
        this.index = i;
        update();
    }

    private void delete(int i) {
        // deletes a filter
        getSettings().filters.remove(i);
        update();
    }

    private void add() {
        // creates a new filter, adds it to the list, and selects it.
        getSettings().filters.add(new UserFilter());
        select(getSettings().filters.get().size() - 1);
        update();
    }

    private void update() {
        this.next.setEnabled(true);
        this.prev.setEnabled(true);
        this.edit.setEnabled(true);
        this.delete.setEnabled(true);

        int size = getSettings().filters.get().size();

        if (index >= size - 1) {
            this.next.setEnabled(false);
            index = size - 1;
        }
        if (index < 1) {
            this.prev.setEnabled(false);
            index = 0;
        }
        if (size < 1) {
            this.edit.setEnabled(false);
            this.delete.setEnabled(false);
            this.index = 0;
        } else {
            UserFilter filter = getSettings().filters.get(index);
            this.lblFilter.setText(new ChatComponentText(filter.getName()));
            this.lblPattern.setText(new ChatComponentText(filter.getRawPattern()));
        }
    }

    private void edit(int i) {
        UserFilter filter = getSettings().filters.get(i);
        setOverlay(new GuiFilterEditor(filter, f -> {
            this.lblFilter.setText(new ChatComponentText(f.getName()));
            this.lblPattern.setText(new ChatComponentText(f.getRawPattern()));
        }));

    }
}
