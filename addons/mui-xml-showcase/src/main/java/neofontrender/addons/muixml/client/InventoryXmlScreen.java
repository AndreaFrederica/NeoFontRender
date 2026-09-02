package neofontrender.addons.muixml.client;

import com.cleanroommc.modularui.api.component.MuiComponentDescriptor;
import com.cleanroommc.modularui.api.component.MuiComponentRegistry;
import com.cleanroommc.modularui.api.dom.MuiElement;
import com.cleanroommc.modularui.api.event.MuiActionRegistration;
import com.cleanroommc.modularui.api.markup.MuiApplicationDescriptor;
import com.cleanroommc.modularui.api.sync.MuiProtocolInstallation;
import com.cleanroommc.modularui.api.sync.MuiProtocolPlan;
import com.cleanroommc.modularui.markup.MuiApplicationDescriptorParser;
import com.cleanroommc.modularui.markup.MuiProtocolXmlParser;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.style.MuiStylesheetParser;
import neofontrender.addons.muixml.MuiXmlShowcaseMod;
import neofontrender.addons.muixml.tile.ShowcaseInventoryTile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Client projection layer for real server-backed inventories described by XML/CSS. */
public final class InventoryXmlScreen extends ModularScreen {

    private static final Logger LOGGER = LogManager.getLogger(MuiXmlShowcaseMod.MOD_NAME);
    private final ShowcaseInventoryTile tile;
    private MuiActionRegistration closeAction;
    private MuiElement compiledRoot;

    private InventoryXmlScreen(ShowcaseInventoryTile tile, ModularPanel panel) {
        super(MuiXmlShowcaseMod.MOD_ID, panel);
        this.tile = tile;
        pausesGame(false);
    }

    public static InventoryXmlScreen create(ShowcaseInventoryTile tile, ModularPanel panel) {
        return new InventoryXmlScreen(tile, panel);
    }

    @Override
    public void onContainerReady(ModularContainer container) {
        try {
            ShowcaseResourceResolver resolver = new ShowcaseResourceResolver();
            MuiApplicationDescriptor application;
            String applicationSource;
            try (InputStream stream = resolver.require("mui-app.json")) {
                applicationSource = readUtf8(stream);
                application = MuiApplicationDescriptorParser.parse(applicationSource);
            }
            MuiComponentRegistry components = new MuiComponentRegistry();
            application.getComponents().forEach((name, resource) -> components.register(
                    MuiComponentDescriptor.builder(name, resource).build()));

            String stylesheetSource;
            try (InputStream stream = resolver.require("styles/devices.css")) {
                stylesheetSource = readUtf8(stream);
                setStylesheet(MuiStylesheetParser.parseCss(application.getOwner(), stylesheetSource, resolver));
            }
            String xml;
            try (InputStream stream = resolver.require(this.tile.getXmlResource())) {
                xml = readUtf8(stream);
            }
            String protocolSource;
            try (InputStream stream = resolver.require(this.tile.getProtocolResource())) {
                protocolSource = readUtf8(stream);
            }
            MuiProtocolPlan sourceProtocol = MuiProtocolXmlParser.parse(
                    application.getOwner(), protocolSource, resolver);
            MuiProtocolInstallation installedProtocol = getContext().getUISettings().getProtocolInstallation();
            if (installedProtocol == null || !installedProtocol.getPlan().matches(sourceProtocol)) {
                throw new IllegalStateException("Displayed protocol source does not match the installed container contract");
            }

            this.compiledRoot = installCompiledDocument(
                    application.getOwner(), xml, components, resolver);
            com.cleanroommc.modularui.screen.dom.MuiDevToolsSession devTools = openDevTools();
            devTools.setStylesheetContext(application.getOwner(), resolver);
            for (Map.Entry<String, String> source : resolver.getOpenedSources().entrySet()) {
                String resourceId = source.getKey();
                if (resourceId.equals(this.tile.getXmlResource())) {
                    devTools.registerSource(resourceId, source.getValue(), edited ->
                            this.compiledRoot = replaceCompiledDocument(this.compiledRoot,
                                    application.getOwner(), edited, components, resolver));
                } else if ("styles/devices.css".equals(resourceId)) {
                    devTools.registerSource(resourceId, source.getValue());
                } else {
                    devTools.registerReadOnlySource(resourceId, source.getValue());
                }
            }
            this.closeAction = getDocument().getActions().register(
                    "device:close", invocation -> {
                        invocation.getEvent().preventDefault();
                        // Finish the current DOM event dispatch before destroying the screen.
                        // Closing synchronously can invalidate the widget tree while the
                        // pointer event is still walking its propagation path.
                        Minecraft.getMinecraft().addScheduledTask((Runnable) this::close);
                    });
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Unable to initialize XML inventory screen for {}", this.tile.getScreenName(), exception);
            throw new IllegalStateException("Unable to initialize XML inventory screen", exception);
        }
    }

    @Override
    public void onClose() {
        if (this.closeAction != null) this.closeAction.close();
    }

    private static String readUtf8(InputStream stream) throws IOException {
        Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[4096];
        int count;
        while ((count = reader.read(buffer)) >= 0) result.append(buffer, 0, count);
        return result.toString();
    }
}
