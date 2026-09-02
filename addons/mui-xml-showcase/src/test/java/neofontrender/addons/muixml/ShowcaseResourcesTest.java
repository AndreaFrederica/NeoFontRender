package neofontrender.addons.muixml;

import com.cleanroommc.modularui.api.component.MuiComponentDescriptor;
import com.cleanroommc.modularui.api.component.MuiComponentRegistry;
import com.cleanroommc.modularui.api.dom.MuiDocument;
import com.cleanroommc.modularui.api.dom.MuiElement;
import com.cleanroommc.modularui.api.markup.MuiApplicationDescriptor;
import com.cleanroommc.modularui.markup.MuiApplicationDescriptorParser;
import com.cleanroommc.modularui.markup.MuiDocumentCompiler;
import com.cleanroommc.modularui.markup.MuiProtocolXmlParser;
import com.cleanroommc.modularui.style.MuiCascade;
import com.cleanroommc.modularui.style.MuiStylesheetParser;
import neofontrender.addons.muixml.client.ShowcaseResourceResolver;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowcaseResourcesTest {
    @Test
    void manifestComponentsScreenAndImportedCssCompileTogether() throws Exception {
        ShowcaseResourceResolver resolver = new ShowcaseResourceResolver();
        MuiApplicationDescriptor application;
        try (InputStream stream = resolver.require("mui-app.json")) {
            application = MuiApplicationDescriptorParser.parse(stream);
        }
        assertEquals(7, application.getComponents().size());
        assertEquals(1, application.getStylesheets().size());

        MuiComponentRegistry components = new MuiComponentRegistry();
        application.getComponents().forEach((name, resource) -> components.register(
                MuiComponentDescriptor.builder(name, resource).build()));
        String screen;
        try (InputStream stream = resolver.require("screens/showcase.xml");
             Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
            screen = scanner.hasNext() ? scanner.next() : "";
        }
        MuiDocument document = new MuiDocument();
        MuiElement root = new MuiDocumentCompiler(components, resolver).compile(
                application.getOwner(), screen, document, null);
        assertTrue(root.isConnected());
        assertNotNull(document.querySelector("#toggle-font"));
        assertNotNull(document.querySelector("#runtime-list"));
        assertNotNull(document.querySelector("#settings-card"));
        assertNotNull(document.querySelector("#preview-card"));
        assertNotNull(document.querySelector("#runtime-card"));
        assertNotNull(document.querySelector("#event-card"));
        assertNotNull(document.querySelector("nfr:slider"));
        assertNotNull(document.querySelector("nfr:text-field"));
        assertNull(document.querySelector("nfr:app-shell"));
        assertNull(document.querySelector("nfr:card"));
        assertNull(document.querySelector("nfr:nav-item"));

        MuiCascade cascade;
        String stylesheet = application.getStylesheets().get(0);
        try (InputStream stream = resolver.require(stylesheet)) {
            cascade = MuiStylesheetParser.parseCss(application.getOwner(), stream, resolver);
        }
        assertNotNull(cascade.compute(document.querySelector("#toggle-font")));
    }

    @Test
    void chestAndFurnaceXmlDeclareBoundWidgetsAndFixedProtocols() throws Exception {
        ShowcaseResourceResolver resolver = new ShowcaseResourceResolver();
        MuiApplicationDescriptor application;
        try (InputStream stream = resolver.require("mui-app.json")) {
            application = MuiApplicationDescriptorParser.parse(stream);
        }
        MuiComponentRegistry components = new MuiComponentRegistry();
        application.getComponents().forEach((name, resource) -> components.register(
                MuiComponentDescriptor.builder(name, resource).build()));

        MuiElement chest = compile(application, components, resolver, "screens/chest.xml");
        assertNotNull(chest.querySelector("#machine-slots"));
        assertEquals("9", chest.querySelector("#machine-slots").getAttribute("columns"));
        assertEquals("device:close", chest.querySelector("#device-close").getAttribute("onclick"));
        assertNotNull(chest.querySelector("#player-main"));
        assertNotNull(chest.querySelector("#player-hotbar"));
        assertEquals(63, chest.querySelectorAll("mui:item-slot").size());
        assertEquals(27, chest.querySelectorAll(".machine-slot").size());
        assertEquals(36, chest.querySelectorAll(".player-slot").size());
        assertTrue(chest.querySelectorAll("mui:item-slot").stream()
                .allMatch(slot -> slot.getAttribute("bind-id") == null));

        MuiElement furnace = compile(application, components, resolver, "screens/furnace.xml");
        assertNotNull(furnace.querySelector("#input-slot"));
        assertNotNull(furnace.querySelector("#fuel-slot"));
        assertNotNull(furnace.querySelector("#output-slot"));
        assertNotNull(furnace.querySelector("#cook-progress-host"));
        assertNotNull(furnace.querySelector("#burn-progress-host"));
        assertEquals(39, furnace.querySelectorAll("mui:item-slot").size());
        assertEquals(3, furnace.querySelectorAll(".machine-slot").size());
        assertEquals(36, furnace.querySelectorAll(".player-slot").size());
        assertEquals("machine.input", furnace.querySelector("#machine-input-slot").getAttribute("bind"));
        assertEquals("machine.fuel", furnace.querySelector("#machine-fuel-slot").getAttribute("bind"));
        assertEquals("machine.output", furnace.querySelector("#machine-output-slot").getAttribute("bind"));
        assertTrue(furnace.querySelectorAll("mui:item-slot").stream()
                .allMatch(slot -> slot.getAttribute("bind-id") == null));
        assertEquals(2, furnace.querySelectorAll("mui:progress").size());
        assertEquals("cook_progress", furnace.querySelector("mui:progress").getAttribute("bind"));

        try (InputStream stream = resolver.require("protocols/chest.xml")) {
            com.cleanroommc.modularui.api.sync.MuiProtocolPlan protocol = MuiProtocolXmlParser.parse(
                    application.getOwner(), read(stream), resolver);
            assertEquals(2, protocol.getSchemaVersion());
            assertEquals(63, protocol.getEntries().size());
        }
        try (InputStream stream = resolver.require("protocols/furnace.xml")) {
            com.cleanroommc.modularui.api.sync.MuiProtocolPlan protocol = MuiProtocolXmlParser.parse(
                    application.getOwner(), read(stream), resolver);
            assertEquals(2, protocol.getSchemaVersion());
            assertEquals(41, protocol.getEntries().size());
        }
        assertTrue(resolver.getOpenedSources().containsKey("protocols/chest.xml"));
        assertTrue(resolver.getOpenedSources().containsKey("protocols/furnace.xml"));
        assertTrue(resolver.getOpenedSources().containsKey("protocol-components/player-inventory.xml"));

        try (InputStream stream = resolver.require("styles/devices.css")) {
            MuiCascade devices = MuiStylesheetParser.parseCss(application.getOwner(), stream, resolver);
            assertNotNull(devices.compute(chest));
            assertNotNull(devices.compute(furnace));
        }
    }

    private static MuiElement compile(MuiApplicationDescriptor application, MuiComponentRegistry components,
                                      ShowcaseResourceResolver resolver, String resource) throws Exception {
        String xml;
        try (InputStream stream = resolver.require(resource);
             Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
            xml = scanner.hasNext() ? scanner.next() : "";
        }
        return new MuiDocumentCompiler(components, resolver).compile(
                application.getOwner(), xml, new MuiDocument(), null);
    }

    private static String read(InputStream stream) {
        try (Scanner scanner = new Scanner(stream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }
}
