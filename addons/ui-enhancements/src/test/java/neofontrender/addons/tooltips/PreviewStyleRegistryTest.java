package neofontrender.addons.tooltips;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewStyleRegistryTest {
    @Test
    void resolvesInheritanceAndDeepMergesSoundSettings() {
        PreviewStyleRegistry.Definition base = definition("""
                {"id":"base","scale":3.25,"effects":["shimmer"],
                 "sound":{"enabled":true,"event":"ui.button.click","volume":0.2}}
                """);
        PreviewStyleRegistry.Definition child = definition("""
                {"id":"child","extends":"base","items":["test:blade"],
                 "width":48,"sound":{"pitch":1.4}}
                """);
        List<String> errors = new ArrayList<>();

        List<PreviewStyleRegistry.Style> styles = PreviewStyleRegistry.resolveStyles(
                Arrays.asList(base, child), errors);
        PreviewStyleRegistry.Style resolved = styles.stream()
                .filter(style -> "child".equals(style.id)).findFirst().orElseThrow();

        assertTrue(errors.isEmpty());
        assertEquals(3.25F, resolved.scale(1.0F));
        assertEquals(48, resolved.width(20));
        assertEquals(Arrays.asList("shimmer"), resolved.effects);
        assertEquals("ui.button.click", resolved.sound().eventId());
        assertEquals(0.2F, resolved.sound().volume());
        assertEquals(1.4F, resolved.sound().pitch());
    }

    @Test
    void matchesNestedAndDottedNbtPaths() {
        JsonObject expected = parse("""
                {"display.Name":"Example Blade",
                  "stats":{"level":3},
                  "runes":[4,7]
                }
                """);
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "Example Blade");
        root.setTag("display", display);
        NBTTagCompound stats = new NBTTagCompound();
        stats.setInteger("level", 3);
        root.setTag("stats", stats);
        NBTTagList runes = new NBTTagList();
        runes.appendTag(new NBTTagInt(4));
        runes.appendTag(new NBTTagInt(7));
        root.setTag("runes", runes);

        assertTrue(PreviewStyleRegistry.Style.matchesNbt(root, expected));
    }

    @Test
    void rejectsEveryStyleParticipatingInAnInheritanceCycle() {
        List<String> errors = new ArrayList<>();

        List<PreviewStyleRegistry.Style> styles = PreviewStyleRegistry.resolveStyles(Arrays.asList(
                definition("{\"id\":\"first\",\"extends\":\"second\",\"items\":[\"test:first\"]}"),
                definition("{\"id\":\"second\",\"extends\":\"first\",\"items\":[\"test:second\"]}")),
                errors);

        assertTrue(styles.isEmpty());
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.contains("inheritance cycle")));
    }

    @Test
    void reportsUnsupportedArmorPresentationValues() {
        List<String> errors = new ArrayList<>();

        List<PreviewStyleRegistry.Style> styles = PreviewStyleRegistry.resolveStyles(
                Arrays.asList(definition(
                        "{\"id\":\"bad\",\"items\":[\"test:item\"],\"armorModel\":\"zombie\"}")),
                errors);

        assertTrue(styles.isEmpty());
        assertTrue(errors.stream().anyMatch(error -> error.contains("armorModel")));
    }

    private static PreviewStyleRegistry.Definition definition(String json) {
        JsonObject object = parse(json);
        return new PreviewStyleRegistry.Definition(object.get("id").getAsString(), object, "test");
    }

    private static JsonObject parse(String json) {
        return new JsonParser().parse(json).getAsJsonObject();
    }
}
