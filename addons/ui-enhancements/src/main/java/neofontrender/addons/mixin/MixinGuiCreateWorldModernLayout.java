package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.WorldType;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.worldcreation.CreateWorldConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(GuiCreateWorld.class)
public abstract class MixinGuiCreateWorldModernLayout extends GuiScreen {
    @Unique private static final int NFR_GAME_TAB = 28640;
    @Unique private static final int NFR_WORLD_TAB = 28641;

    @Shadow private GuiTextField worldNameField;
    @Shadow private GuiTextField worldSeedField;
    @Shadow private String saveDirName;
    @Shadow private boolean inMoreWorldOptionsDisplay;
    @Shadow private GuiButton btnGameMode;
    @Shadow private GuiButton btnMoreOptions;
    @Shadow private GuiButton btnMapFeatures;
    @Shadow private GuiButton btnBonusItems;
    @Shadow private GuiButton btnMapType;
    @Shadow private GuiButton btnAllowCommands;
    @Shadow private GuiButton btnCustomizeType;
    @Shadow private String gameModeDesc1;
    @Shadow private String gameModeDesc2;
    @Shadow private int selectedIndex;

    @Unique private GuiButton nfrUi$gameTab;
    @Unique private GuiButton nfrUi$worldTab;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void nfrUi$modernizeCreateWorld(CallbackInfo ci) {
        if (!nfrUi$active()) return;
        nfrUi$gameTab = addButton(new GuiButton(NFR_GAME_TAB, 0, 0, 110, 20,
                AddonI18n.tr("neofontrender_ui_enhancements.create_world.game")));
        nfrUi$worldTab = addButton(new GuiButton(NFR_WORLD_TAB, 0, 0, 110, 20,
                AddonI18n.tr("neofontrender_ui_enhancements.create_world.world")));
        btnMoreOptions.visible = false;
        nfrUi$layoutControls();
        nfrUi$syncTabs();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void nfrUi$selectCreateWorldTab(GuiButton button, CallbackInfo ci) throws IOException {
        if (!nfrUi$active() || (button.id != NFR_GAME_TAB && button.id != NFR_WORLD_TAB)) return;
        boolean wantsWorld = button.id == NFR_WORLD_TAB;
        if (wantsWorld != inMoreWorldOptionsDisplay) {
            ((GuiCreateWorldAccessor) this).nfrUi$toggleMoreWorldOptions();
            btnMoreOptions.visible = false;
            nfrUi$layoutControls();
        }
        nfrUi$syncTabs();
        ci.cancel();
    }

    @Inject(method = "showMoreWorldOptions", at = @At("RETURN"))
    private void nfrUi$keepModernLayout(boolean showWorldOptions, CallbackInfo ci) {
        if (!nfrUi$active() || nfrUi$gameTab == null) return;
        btnMoreOptions.visible = false;
        nfrUi$layoutControls();
        nfrUi$syncTabs();
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void nfrUi$drawModernCreateWorld(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!nfrUi$active()) return;
        nfrUi$layoutControls();
        drawDefaultBackground();

        boolean modernStyle = CreateWorldConfig.usesModernStyle();
        if (modernStyle) {
            drawRect(0, 26, width, height - 34, 0x9814171B);
            drawRect(0, height - 34, width, height, 0xC0101317);
            drawHorizontalLine(0, width, 26, 0x6652E875);
            drawHorizontalLine(0, width, height - 34, 0x55343B42);
        }
        drawCenteredString(fontRenderer, I18n.format("selectWorld.create"), width / 2, 10, 0xFFFFFFFF);

        int labelColor = modernStyle ? 0xFFD0D4DA : 0xFFA0A0A0;
        int secondaryColor = modernStyle ? 0xFF8D949E : 0xFF808080;
        int descriptionColor = modernStyle ? 0xFFB5BBC3 : 0xFFA0A0A0;

        if (inMoreWorldOptionsDisplay) {
            drawString(fontRenderer, I18n.format("selectWorld.enterSeed"), worldSeedField.x, worldSeedField.y - 12, labelColor);
            drawString(fontRenderer, I18n.format("selectWorld.seedInfo"), worldSeedField.x, worldSeedField.y + 23, secondaryColor);
            worldSeedField.drawTextBox();
            WorldType type = WorldType.WORLD_TYPES[selectedIndex];
            if (type != null && type.hasInfoNotice() && !btnCustomizeType.visible) {
                fontRenderer.drawSplitString(I18n.format(type.getInfoTranslationKey()),
                        btnMapType.x + 2, btnMapType.y + 23, btnMapType.width - 4, secondaryColor);
            }
        } else {
            drawString(fontRenderer, I18n.format("selectWorld.enterName"), worldNameField.x, worldNameField.y - 12, labelColor);
            drawString(fontRenderer, I18n.format("selectWorld.resultFolder") + " " + saveDirName,
                    worldNameField.x, worldNameField.y + 23, secondaryColor);
            worldNameField.drawTextBox();
            drawString(fontRenderer, gameModeDesc1, btnGameMode.x, btnGameMode.y + 23, descriptionColor);
            drawString(fontRenderer, gameModeDesc2, btnGameMode.x, btnGameMode.y + 35, secondaryColor);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        ci.cancel();
    }

    @Unique
    private void nfrUi$layoutControls() {
        int contentWidth = Math.min(300, Math.max(220, width - 40));
        int left = width / 2 - contentWidth / 2;
        int half = (contentWidth - 10) / 2;
        int footerY = height - 28;

        nfrUi$gameTab.x = width / 2 - 112;
        nfrUi$gameTab.y = 32;
        nfrUi$worldTab.x = width / 2 + 2;
        nfrUi$worldTab.y = 32;

        worldNameField.x = left;
        worldNameField.y = 70;
        worldNameField.width = contentWidth;
        worldSeedField.x = left;
        worldSeedField.y = 70;
        worldSeedField.width = contentWidth;

        btnGameMode.x = left;
        btnGameMode.y = 120;
        btnGameMode.width = contentWidth;
        btnMapType.x = left;
        btnMapType.y = 114;
        btnMapType.width = contentWidth;
        btnCustomizeType.x = left;
        btnCustomizeType.y = 144;
        btnCustomizeType.width = contentWidth;
        btnMapFeatures.x = left;
        btnMapFeatures.y = 172;
        btnMapFeatures.width = half;
        btnBonusItems.x = left + half + 10;
        btnBonusItems.y = 172;
        btnBonusItems.width = half;
        btnAllowCommands.x = left;
        btnAllowCommands.y = Math.min(202, footerY - 24);
        btnAllowCommands.width = contentWidth;

        for (GuiButton button : buttonList) {
            if (button.id == 0) {
                button.x = left;
                button.y = footerY;
                button.width = half;
            } else if (button.id == 1) {
                button.x = left + half + 10;
                button.y = footerY;
                button.width = half;
            }
        }
    }

    @Unique
    private void nfrUi$syncTabs() {
        nfrUi$gameTab.enabled = inMoreWorldOptionsDisplay;
        nfrUi$worldTab.enabled = !inMoreWorldOptionsDisplay;
        boolean modernStyle = CreateWorldConfig.usesModernStyle();
        nfrUi$gameTab.packedFGColour = modernStyle && !inMoreWorldOptionsDisplay ? 0x52E875 : 0;
        nfrUi$worldTab.packedFGColour = modernStyle && inMoreWorldOptionsDisplay ? 0x52E875 : 0;
    }

    @Unique
    private boolean nfrUi$active() {
        return CreateWorldConfig.usesTabbedLayout()
                && ((Object) this).getClass() == GuiCreateWorld.class;
    }
}
