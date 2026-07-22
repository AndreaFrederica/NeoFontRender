package neofontrender.addons.vendor.tabbychat.foundation.sound;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import neofontrender.addons.vendor.tabbychat.TabbyChat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Enumerates 1.7.10 sound events directly from resource-pack sounds.json files. */
public final class SoundCatalog {

    private SoundCatalog() {}

    public static List<String> listSounds() {
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
        List<String> sounds = Lists.newArrayList();
        for (String domain : manager.getResourceDomains()) {
            collectDomain(manager, domain, sounds);
        }
        Collections.sort(sounds);
        return sounds;
    }

    private static void collectDomain(IResourceManager manager, String domain, List<String> sounds) {
        try {
            for (IResource resource : manager.getAllResources(new ResourceLocation(domain, "sounds.json"))) {
                Reader reader = new InputStreamReader(resource.getInputStream(), "UTF-8");
                try {
                    JsonElement parsed = new JsonParser().parse(reader);
                    if (!parsed.isJsonObject()) {
                        throw new IllegalStateException("sounds.json is not an object for domain " + domain);
                    }
                    JsonObject object = parsed.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                        sounds.add(domain + ":" + entry.getKey());
                    }
                } finally {
                    reader.close();
                }
            }
        } catch (IOException e) {
            TabbyChat.getLogger().warn("Unable to enumerate sounds for domain " + domain, e);
        }
    }
}
