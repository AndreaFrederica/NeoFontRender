package neofontrender.addons.muixml.tile;

import com.cleanroommc.modularui.api.sync.MuiProtocolPlan;
import com.cleanroommc.modularui.api.sync.MuiProtocolTemplate;
import com.cleanroommc.modularui.api.sync.MuiProtocolTypeRegistry;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.markup.MuiProtocolXmlParser;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.ItemSlotSH;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.cleanroommc.modularui.widgets.slot.PlayerSlotGroup;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

final class ShowcaseProtocolTemplates {

    private static final String OWNER = "neofontrender_mui_xml_showcase";
    private static final String RESOURCE_ROOT = "assets/" + OWNER + "/mui/";
    static final MuiProtocolTemplate CHEST = load("protocols/chest.xml");
    static final MuiProtocolTemplate FURNACE = load("protocols/furnace.xml");

    private ShowcaseProtocolTemplates() {}

    private static MuiProtocolTemplate load(String resource) {
        MuiProtocolPlan plan = MuiProtocolXmlParser.parse(OWNER, read(resource), ShowcaseProtocolTemplates::open);
        MuiProtocolTypeRegistry types = new MuiProtocolTypeRegistry()
                .registerSlot("showcase:machine-slot", 1, (context, entry) -> {
                    ShowcaseInventoryTile tile = tile(context.getGuiData(PosGuiData.class));
                    ModularSlot slot = new ModularSlot(tile.getItems(), machineSlotIndex(tile, entry.getKey()))
                            .slotGroup("machine");
                    if ("machine.output".equals(entry.getKey())) slot.canPut(false);
                    return new ItemSlotSH(slot);
                })
                .registerSlot("showcase:player-slot", 1, (context, entry) -> {
                    PosGuiData data = context.getGuiData(PosGuiData.class);
                    ModularSlot slot = new ModularSlot(new PlayerMainInvWrapper(data.getPlayer().inventory),
                            playerSlotIndex(entry.getKey())).slotGroup(PlayerSlotGroup.NAME);
                    return new ItemSlotSH(slot);
                })
                .registerHandler("showcase:furnace-progress", 1, (context, entry) -> {
                    ShowcaseFurnaceTile furnace = (ShowcaseFurnaceTile) tile(
                            context.getGuiData(PosGuiData.class));
                    if ("cook_progress".equals(entry.getKey())) {
                        return new DoubleSyncValue(furnace::getCookProgress);
                    }
                    if ("burn_progress".equals(entry.getKey())) {
                        return new DoubleSyncValue(furnace::getBurnProgress);
                    }
                    throw new IllegalArgumentException("Unknown furnace progress binding: " + entry.getKey());
                });
        return new MuiProtocolTemplate(plan, types);
    }

    private static ShowcaseInventoryTile tile(PosGuiData data) {
        if (!(data.getTileEntity() instanceof ShowcaseInventoryTile tile)) {
            throw new IllegalArgumentException("Showcase protocol requires a showcase inventory tile");
        }
        return tile;
    }

    private static int machineSlotIndex(ShowcaseInventoryTile tile, String key) {
        if (tile instanceof ShowcaseFurnaceTile) {
            if ("machine.input".equals(key)) return ShowcaseFurnaceTile.INPUT_SLOT_INDEX;
            if ("machine.fuel".equals(key)) return ShowcaseFurnaceTile.FUEL_SLOT_INDEX;
            if ("machine.output".equals(key)) return ShowcaseFurnaceTile.OUTPUT_SLOT_INDEX;
        } else if (key.startsWith("machine.storage.")) {
            return indexSuffix(key, "machine.storage.", 27);
        }
        throw new IllegalArgumentException("Unknown machine slot binding: " + key);
    }

    private static int playerSlotIndex(String key) {
        if (key.startsWith("player.hotbar.")) return indexSuffix(key, "player.hotbar.", 9);
        if (key.startsWith("player.main.")) return 9 + indexSuffix(key, "player.main.", 27);
        throw new IllegalArgumentException("Unknown player slot binding: " + key);
    }

    private static int indexSuffix(String key, String prefix, int limit) {
        try {
            int index = Integer.parseInt(key.substring(prefix.length()));
            if (index < 0 || index >= limit) throw new NumberFormatException();
            return index;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid indexed slot binding: " + key, exception);
        }
    }

    private static String read(String resource) {
        try (InputStream stream = open(OWNER, resource)) {
            if (stream == null) throw new IOException("Missing showcase protocol resource: " + resource);
            Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            StringBuilder result = new StringBuilder();
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) >= 0) result.append(buffer, 0, count);
            return result.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load showcase protocol " + resource, exception);
        }
    }

    private static InputStream open(String owner, String resource) throws IOException {
        if (!OWNER.equals(owner)) return null;
        String normalized = resource == null ? "" : resource.trim().replace('\\', '/');
        if (normalized.isEmpty() || normalized.startsWith("/") || normalized.contains("../")
                || normalized.contains("://")) {
            throw new IOException("Unsafe showcase protocol resource: " + resource);
        }
        return ShowcaseProtocolTemplates.class.getClassLoader().getResourceAsStream(RESOURCE_ROOT + normalized);
    }
}
