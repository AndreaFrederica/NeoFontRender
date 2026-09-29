package neofontrender.addons.muixml.client;

import com.cleanroommc.modularui.api.component.MuiComponentDescriptor;
import com.cleanroommc.modularui.api.component.MuiComponentRegistry;
import com.cleanroommc.modularui.api.dom.MuiElement;
import com.cleanroommc.modularui.api.markup.MuiApplicationDescriptor;
import com.cleanroommc.modularui.api.state.MuiStore;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.markup.MuiApplicationDescriptorParser;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.style.MuiStylesheetParser;
import neofontrender.addons.muixml.MuiXmlShowcaseMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ShowcaseScreen extends ModularScreen {
    private static final Logger LOGGER = LogManager.getLogger(MuiXmlShowcaseMod.MOD_NAME);
    private final MuiStore store;
    private ShowcaseController controller;

    private ShowcaseScreen(ModularPanel panel, MuiStore store) {
        super(MuiXmlShowcaseMod.MOD_ID, panel);
        this.store = store;
        pausesGame(false);
    }

    public static void open() {
        ClientGUI.open(create());
    }

    static ShowcaseScreen create() {
        try {
            ShowcaseResourceResolver resolver = new ShowcaseResourceResolver();
            MuiApplicationDescriptor application;
            try (InputStream stream = resolver.require("mui-app.json")) {
                application = MuiApplicationDescriptorParser.parse(stream);
            }
            MuiComponentRegistry components = new MuiComponentRegistry();
            application.getComponents().forEach((name, resource) -> components.register(
                    MuiComponentDescriptor.builder(name, resource).build()));

            MuiStore store = new MuiStore(initialState());
            ModularPanel panel = new ModularPanel("mui_xml_showcase").relativeToScreen().full();
            panel.disableThemeBackground(true).disableHoverThemeBackground(true);
            ShowcaseScreen screen = new ShowcaseScreen(panel, store);
            ShowcaseNativeElements.register(screen.getElementRegistry(), store);

            if (!application.getStylesheets().isEmpty()) {
                String stylesheet = application.getStylesheets().get(0);
                try (InputStream stream = resolver.require(stylesheet)) {
                    screen.setStylesheet(MuiStylesheetParser.parseCss(
                            application.getOwner(), stream, resolver));
                }
            }
            String document;
            try (InputStream stream = resolver.require("screens/showcase.xml")) {
                document = readUtf8(stream);
            }
            MuiElement root = screen.installCompiledDocument(
                    application.getOwner(), document, components, resolver);
            screen.controller = ShowcaseController.attach(screen, root, store);
            return screen;
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Unable to create the MUI XML showcase", exception);
            throw new IllegalStateException("Unable to create the MUI XML showcase", exception);
        }
    }

    MuiStore getStore() {
        return this.store;
    }

    @Override
    public void onClose() {
        if (this.controller != null) this.controller.close();
        this.store.close();
    }

    private static Map<String, Object> initialState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("status", "XML document mounted; Java controller attached");
        state.put("page", "overview");
        state.put("fontEnabled", true);
        state.put("shadowEnabled", false);
        state.put("mode", "Balanced");
        state.put("scale", 1.0D);
        state.put("sampleText", "The quick brown fox");
        state.put("runtimeRows", 0);
        state.put("events", 0);
        return state;
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
