package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.GuiScreen;
import neofontrender.addons.language.LanguageListSearchAccess;
import neofontrender.addons.tooltips.AddonI18n;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(GuiLanguage.class)
public abstract class MixinGuiLanguageSearch extends GuiScreen {
    @Unique private static Field nfrUi$listField;
    @Unique private GuiTextField nfrUi$searchField;
    @Unique private String nfrUi$query = "";

    @Inject(method = "initGui", at = @At("TAIL"))
    private void nfrUi$addLanguageSearch(CallbackInfo ci) {
        nfrUi$searchField = new GuiTextField(fontRendererObj, width / 2 - 100, 30, 200, 20);
        nfrUi$searchField.setMaxStringLength(128);
        nfrUi$searchField.setText(nfrUi$query);
        GuiSlot list = nfrUi$getList();
        if (list != null) {
            list.func_148122_a(width, height, 54, height - 65 + 4);
            ((LanguageListSearchAccess) list).nfrUi$setLanguageSearch(nfrUi$query);
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void nfrUi$drawLanguageSearch(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (nfrUi$searchField == null) return;
        nfrUi$searchField.drawTextBox();
        if (nfrUi$searchField.getText().isEmpty() && !nfrUi$searchField.isFocused()) {
            fontRendererObj.drawString(AddonI18n.tr("neofontrender_ui_enhancements.language.search"),
                    nfrUi$searchField.xPosition + 4, nfrUi$searchField.yPosition + 6, 0xFF808080);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (nfrUi$searchField != null) nfrUi$searchField.updateCursorCounter();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (nfrUi$searchField != null && nfrUi$searchField.isFocused() && keyCode != Keyboard.KEY_ESCAPE) {
            String before = nfrUi$searchField.getText();
            if (nfrUi$searchField.textboxKeyTyped(typedChar, keyCode)) {
                nfrUi$query = nfrUi$searchField.getText();
                if (!before.equals(nfrUi$query)) nfrUi$applySearch();
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (nfrUi$searchField != null) nfrUi$searchField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Unique
    private void nfrUi$applySearch() {
        GuiSlot list = nfrUi$getList();
        if (list != null) {
            ((LanguageListSearchAccess) list).nfrUi$setLanguageSearch(nfrUi$query);
            list.scrollBy(-list.getAmountScrolled());
        }
    }

    /**
     * GuiLanguage$List is package-private, so the list field cannot be shadowed with its real
     * type. Scanning for the single GuiSlot-typed field stays independent of mapping names.
     */
    @Unique
    private GuiSlot nfrUi$getList() {
        try {
            if (nfrUi$listField == null) {
                for (Field field : GuiLanguage.class.getDeclaredFields()) {
                    if (GuiSlot.class.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        nfrUi$listField = field;
                        break;
                    }
                }
            }
            return nfrUi$listField == null ? null : (GuiSlot) nfrUi$listField.get(this);
        } catch (IllegalAccessException exception) {
            return null;
        }
    }
}
