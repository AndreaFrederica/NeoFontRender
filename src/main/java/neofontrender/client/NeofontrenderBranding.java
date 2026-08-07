package neofontrender.client;

import net.minecraft.client.resources.I18n;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;

import java.time.LocalDate;

/** Resolves the client-facing brand from the date and installed companion mods. */
public final class NeofontrenderBranding {
    private static final String UI_ENHANCEMENTS_MOD_ID = "neofontrender_ui_enhancements";
    private static final String NOVA_ENGINE_CORE_MOD_ID = "novaeng_core";
    private static final String RESOURCES_MOD_ID = "neofontrender_resources";

    private NeofontrenderBranding() {}

    public static String displayName() {
        return I18n.format("neofontrender.brand." + suffix());
    }

    public static String suffix() {
        LocalDate date = LocalDate.now();
        String brand;
        if (date.getMonthValue() == 4 && date.getDayOfMonth() == 1) {
            brand = "modern";
        } else if (Loader.isModLoaded(NOVA_ENGINE_CORE_MOD_ID)) {
            brand = "nova";
        } else {
            brand = "revo";
        }
        return brand + (Loader.isModLoaded(UI_ENHANCEMENTS_MOD_ID) ? ".ui" : ".font");
    }

    public static void applyModMetadata() {
        setModName("neofontrender", displayName());
        setModName(RESOURCES_MOD_ID, displayName() + " Resources");
    }

    private static void setModName(String modId, String name) {
        ModContainer container = Loader.instance().getIndexedModList().get(modId);
        if (container != null && container.getMetadata() != null) {
            container.getMetadata().name = name;
        }
    }
}
