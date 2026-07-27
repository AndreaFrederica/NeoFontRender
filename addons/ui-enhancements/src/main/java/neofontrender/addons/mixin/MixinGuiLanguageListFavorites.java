package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.resources.Language;
import neofontrender.addons.language.LanguageListSearchAccess;
import neofontrender.addons.language.LanguageSearchIndex;
import neofontrender.addons.language.LanguageSelectionConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(targets = "net.minecraft.client.gui.GuiLanguage$List")
public abstract class MixinGuiLanguageListFavorites implements LanguageListSearchAccess {
    @Shadow @Final private List<String> langCodeList;
    @Shadow @Final private Map<String, Language> languageMap;

    @Unique private List<String> nfrUi$allLanguages;
    @Unique private String nfrUi$query = "";

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nfrUi$captureLanguages(CallbackInfo ci) {
        nfrUi$allLanguages = new ArrayList<>(langCodeList);
        nfrUi$rebuildLanguages();
    }

    @Override
    public void nfrUi$setLanguageSearch(String query) {
        nfrUi$query = query == null ? "" : query;
        nfrUi$rebuildLanguages();
    }

    @Override
    public boolean nfrUi$toggleFavorite(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= langCodeList.size()) return false;
        LanguageSelectionConfig.toggleFavorite(langCodeList.get(slotIndex));
        return true;
    }

    @Inject(method = "drawSlot", at = @At("RETURN"))
    private void nfrUi$drawFavorite(int slotIndex, int xPos, int yPos, int heightIn,
                                    int mouseXIn, int mouseYIn, float partialTicks, CallbackInfo ci) {
        if (slotIndex < 0 || slotIndex >= langCodeList.size()) return;
        GuiSlot self = (GuiSlot) (Object) this;
        boolean favorite = LanguageSelectionConfig.isFavorite(langCodeList.get(slotIndex));
        String icon = favorite ? "\u2605" : "\u2606";
        int center = self.width / 2;
        boolean hovered = mouseXIn >= center + 58 && mouseXIn <= center + 110
                && mouseYIn >= yPos - 2 && mouseYIn <= yPos + heightIn + 1;
        Gui.drawRect(center + 70, yPos - 2, center + 110, yPos + heightIn + 1,
                hovered ? 0x70434B53 : 0x24282E34);
        int color = favorite ? 0xFFFFC857 : hovered ? 0xFFD8DCE2 : 0xFF8A9098;
        int iconX = center + 90 - Minecraft.getMinecraft().fontRenderer.getStringWidth(icon) / 2;
        Minecraft.getMinecraft().fontRenderer.drawString(icon, iconX, yPos + 1, color);
    }

    @Unique
    private void nfrUi$rebuildLanguages() {
        if (nfrUi$allLanguages == null) return;
        langCodeList.clear();
        langCodeList.addAll(LanguageSearchIndex.filter(nfrUi$allLanguages, languageMap,
                LanguageSelectionConfig.favorites(), nfrUi$query));
    }
}
