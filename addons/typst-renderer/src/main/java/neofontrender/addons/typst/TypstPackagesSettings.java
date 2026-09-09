package neofontrender.addons.typst;

import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.api.client.settings.*;
import neofontrender.client.gui.component.base.*;
import neofontrender.client.gui.views.NfrContentView;

import java.util.List;

import static neofontrender.addons.typst.TypstStatus.tr;

/** NFR settings card for the optional addon's package cache and status overlay. */
final class TypstPackagesSettings implements NfrSettingsPage {
    @Override public String id() { return TypstRendererMod.MOD_ID + ":packages"; }
    @Override public String titleKey() { return "neofontrender_typst_renderer.gui.packages"; }
    @Override public String title() { return TypstI18n.tr(titleKey()); }
    @Override public int order() { return 1100; }
    @Override public NfrSettingsPageSession createSession() { return new Session(); }

    private static final class Session implements NfrSettingsPageSession {
        private final boolean originalHud = TypstConfig.downloadHud();
        private final TypstStatus status = TypstStatus.INSTANCE;
        private String input = "";
        private String selected = "";
        private String deleteConfirmation = "";

        Session() { status.refresh(); }

        @Override public IWidget createView(NfrSettingsPageContext context) {
            var controls = context.controls();
            var options = controls.grid()
                    .add(controls.toggleText(() -> tr("gui.hud"), () -> "", TypstConfig::downloadHud,
                            TypstConfig::setDownloadHud, () -> {}))
                    .add(controls.action(() -> tr("gui.refresh"), 260, 24, status::refresh))
                    .add(controls.action(() -> tr("gui.retry"), 260, 24, status::retry))
                    .add(controls.action(() -> tr("gui.dismiss"), 260, 24, status::dismiss));
            TextFieldWidget editor = new TextFieldWidget().setMaxLength(160)
                    .value(new NfrStringValue(() -> input, value -> input = value));
            NfrLabeledTextField field = new NfrLabeledTextField(tr("gui.package_spec"), editor);
            var install = controls.grid().add(controls.action(
                    () -> tr(status.busy() ? "gui.busy" : "gui.install"), 260, 24,
                    () -> status.manage(input, false)));
            var inventory = controls.grid();
            List<String> specs = status.packages().stream().map(entry -> entry.spec()).toList();
            if (!specs.contains(selected)) selected = specs.isEmpty() ? "" : specs.get(0);
            if (!specs.isEmpty()) {
                inventory.add(controls.dropdownText("typst_package", () -> tr("gui.cached"),
                        () -> selected, value -> { selected = value; deleteConfirmation = ""; }, specs,
                        value -> value).size(260, 24));
                inventory.add(controls.action(() -> tr(selected.equals(deleteConfirmation)
                                ? "gui.confirm_delete" : "gui.delete"), 260, 24, () -> {
                    if (status.busy()) return;
                    if (selected.equals(deleteConfirmation)) {
                        status.manage(selected, true);
                        deleteConfirmation = "";
                    } else deleteConfirmation = selected;
                }));
            }
            inventory.add(controls.action(() -> tr("gui.copy_error"), 260, 24,
                    () -> GuiScreen.setClipboardString(status.lastError())));
            return new View(context, options, field, install, inventory, new StatusPanel());
        }

        @Override public void apply() { TypstConfig.save(); }
        @Override public void cancel() { TypstConfig.setDownloadHud(originalHud); }
    }

    private static final class View extends NfrContentView<View> {
        private final NfrSettingsPageContext context;
        private long revision = TypstStatus.INSTANCE.revision();

        View(NfrSettingsPageContext context, NfrOptionsGrid options, NfrLabeledTextField field,
             NfrOptionsGrid install, NfrOptionsGrid inventory, StatusPanel status) {
            super(section(options, options::preferredHeight), section(field, width -> field.preferredHeight()),
                    section(install, install::preferredHeight), section(inventory, inventory::preferredHeight),
                    section(status, width -> status.heightFor(width)));
            this.context = context;
        }

        @Override public void onUpdate() {
            super.onUpdate();
            if (revision != TypstStatus.INSTANCE.revision()) {
                revision = TypstStatus.INSTANCE.revision();
                context.refresh();
            }
        }
    }

    private static final class StatusPanel extends Widget<StatusPanel> {
        int heightFor(int width) {
            var font = Minecraft.getMinecraft().fontRenderer;
            return Math.max(160, lines(width).size() * (font.FONT_HEIGHT + 3));
        }

        @Override public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
            super.draw(context, theme);
            com.cleanroommc.modularui.utils.Platform.setupDrawFont();
            var font = Minecraft.getMinecraft().fontRenderer;
            int y = 0;
            for (String line : lines(getArea().w())) {
                if (y + font.FONT_HEIGHT > getArea().h()) return;
                font.drawString(line, 2, y, 0xFFD4DBDF);
                y += font.FONT_HEIGHT + 3;
            }
        }

        private List<String> lines(int width) {
            var font = Minecraft.getMinecraft().fontRenderer;
            var status = TypstStatus.INSTANCE;
            long bytes = status.packages().stream().mapToLong(entry -> entry.bytes()).sum();
            StringBuilder text = new StringBuilder(tr("gui.cache_summary", status.packages().size(), TypstStatus.bytes(bytes)));
            if (status.packages().isEmpty()) text.append('\n').append(tr("gui.empty"));
            if (!status.lastError().isEmpty()) text.append('\n').append(status.lastError());
            for (TypstStatus.Notice notice : status.notices()) {
                text.append('\n').append(TypstStatus.describe(notice.event()));
            }
            // Error text can contain the failed source; never feed it back into the Typst parser.
            String plain = text.toString().replaceAll("(?i)<typst:", "[typst:").replace('\u00a7', ' ');
            var result = new java.util.ArrayList<String>();
            for (String paragraph : plain.split("\\R")) {
                result.addAll(font.listFormattedStringToWidth(paragraph, Math.max(1, width - 8)));
            }
            return result;
        }
    }
}
