package neofontrender.addons.mixin;

import net.minecraft.client.resources.IResource;
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
import java.util.List;

/**
 * The old resource loader may skip this addon's lang domain, leaving vanilla-rendered
 * strings (keybinding names and categories in the controls screen) untranslated.
 * Feed the bundled translations through Locale's resource parser on every reload.
 * This preserves resource-domain tracking used by localization compatibility mods.
 */
@Mixin(Locale.class)
public abstract class MixinLocaleAddonTranslations {
    @Invoker("loadLocaleData")
    protected abstract void nfrUi$loadLocaleData(List<IResource> resources) throws IOException;

    @Inject(method = "loadLocaleDataFiles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/Locale;checkUnicode()V"))
    private void nfrUi$mergeAddonTranslations(IResourceManager resourceManager,
                                               List<String> languages, CallbackInfo ci) {
        try {
            nfrUi$loadLocaleData(AddonI18n.bundledLanguageResources(languages));
        } catch (IOException exception) {
            LogManager.getLogger("Revo UI").warn("Failed to load bundled UI translations", exception);
        }
    }
}
