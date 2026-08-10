package neofontrender.addons.mixin;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.Locale;
import neofontrender.addons.tooltips.AddonI18n;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

/**
 * The old resource loader may skip this addon's lang domain, leaving vanilla-rendered
 * strings (keybinding names and categories in the controls screen) untranslated.
 * Merge the bundled translations into the vanilla locale table on every reload.
 */
@Mixin(Locale.class)
public abstract class MixinLocaleAddonTranslations {
    @Shadow @Final private Map<String, String> properties;

    @Inject(method = "loadLocaleDataFiles", at = @At("RETURN"))
    private void nfrUi$mergeAddonTranslations(IResourceManager resourceManager,
                                              List<String> languages, CallbackInfo ci) {
        AddonI18n.mergeInto(properties, languages);
    }
}
