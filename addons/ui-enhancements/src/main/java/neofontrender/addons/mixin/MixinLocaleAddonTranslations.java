package neofontrender.addons.mixin;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.Locale;
import neofontrender.addons.tooltips.AddonI18n;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static java.util.Locale.ROOT;

/**
 * The 1.7.10 resource loader may skip this addon's lang domain, leaving vanilla-rendered
 * strings (keybinding names and categories in the controls screen) untranslated.
 * Feed the bundled translations through Locale's own parser on every reload, so plain
 * {@code I18n.format} lookups resolve them exactly like a pack-provided language file.
 */
@Mixin(Locale.class)
public abstract class MixinLocaleAddonTranslations {
    private static final String LANGUAGE_ROOT = "/assets/" + AddonI18n.DOMAIN + "/lang/";

    /**
     * 1.7.10 keeps both {@code loadLocaleData} overloads private; the stream overload is the one
     * that accepts resources that do not come from the active resource packs.
     */
    @Invoker("loadLocaleData")
    protected abstract void nfrUi$loadLocaleData(InputStream stream) throws IOException;

    @Inject(method = "loadLocaleDataFiles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/Locale;checkUnicode()V"))
    private void nfrUi$mergeAddonTranslations(IResourceManager resourceManager,
                                              List<String> languages, CallbackInfo ci) {
        for (String language : languages) {
            InputStream stream = nfrUi$openBundledLanguage(language);
            if (stream == null) continue;
            try (InputStream closing = stream) {
                nfrUi$loadLocaleData(closing);
            } catch (IOException exception) {
                LogManager.getLogger("Revo UI")
                        .warn("Failed to load bundled UI translations for {}", language, exception);
            }
        }
    }

    /**
     * 1.7.10 language codes keep their region capitalization ({@code en_US}), which the resource
     * loader turns into {@code lang/en_US.lang}; accept the lowercased spelling as well because the
     * bundled files are also shipped in that form.
     */
    private static InputStream nfrUi$openBundledLanguage(String language) {
        InputStream stream = nfrUi$resource(language);
        if (stream != null) return stream;
        String lowercased = language.toLowerCase(ROOT);
        return lowercased.equals(language) ? null : nfrUi$resource(lowercased);
    }

    private static InputStream nfrUi$resource(String language) {
        return AddonI18n.class.getResourceAsStream(LANGUAGE_ROOT + language + ".lang");
    }
}
