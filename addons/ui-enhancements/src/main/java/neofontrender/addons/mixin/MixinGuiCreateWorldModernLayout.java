package neofontrender.addons.mixin;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.world.WorldType;
import neofontrender.addons.tooltips.AddonI18n;
import neofontrender.addons.worldcreation.CreateWorldConfig;
import neofontrender.addons.worldcreation.CreateWorldGameRulesState;
import neofontrender.addons.worldcreation.CreateWorldTabIds;
import neofontrender.addons.worldcreation.GuiGameRulesList;
import neofontrender.addons.worldcreation.NfrColumnLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rearranges the vanilla create-world screen into Game / World / Game Rules tabs (the
 * latter mirrors newer Minecraft versions). All rows are positioned by
 * {@link NfrColumnLayout}, which compresses gaps instead of overlapping widgets when the
 * window gets short, so the layout survives window resizes. World creation itself stays
 * vanilla; chosen game rules ride along via {@link CreateWorldGameRulesState}.
 */
@Mixin(GuiCreateWorld.class)
public abstract class MixinGuiCreateWorldModernLayout extends GuiScreen {
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
    @Unique private GuiButton nfrUi$rulesTab;
    @Unique private boolean nfrUi$rulesTabActive;
    @Unique private GuiGameRulesList nfrUi$rulesList;
    @Unique private final Map<String, String> nfrUi$ruleValues = new LinkedHashMap<>();

    @Inject(method = "initGui", at = @At("TAIL"))
    private void nfrUi$modernizeCreateWorld(CallbackInfo ci) {
        if (!nfrUi$active()) return;
        nfrUi$gameTab = addButton(new GuiButton(CreateWorldTabIds.GAME, 0, 0, 100, 20,
                AddonI18n.tr("neofontrender_ui_enhancements.create_world.game")));
        nfrUi$worldTab = addButton(new GuiButton(CreateWorldTabIds.WORLD, 0, 0, 100, 20,
                AddonI18n.tr("neofontrender_ui_enhancements.create_world.world")));
        nfrUi$rulesTab = addButton(new GuiButton(CreateWorldTabIds.RULES, 0, 0, 100, 20,
                AddonI18n.tr("neofontrender_ui_enhancements.create_world.rules")));
        btnMoreOptions.visible = false;
        nfrUi$rulesList = new GuiGameRulesList(mc, width, height, 60, height - 36, nfrUi$ruleValues);
        if (nfrUi$rulesTabActive) nfrUi$hideVanillaForRules();
        nfrUi$layoutControls();
        nfrUi$syncTabs();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void nfrUi$selectCreateWorldTab(GuiButton button, CallbackInfo ci) {
        if (!nfrUi$active()) return;
        if (CreateWorldTabIds.isTab(button.id)) {
            nfrUi$switchToTab(button.id);
            ci.cancel();
            return;
        }
        if (button.id == 0) {
            // "Create New World" pressed: stash rule overrides for the WorldInfo hook.
            CreateWorldGameRulesState.setPending(GuiGameRulesList.collectOverrides(nfrUi$ruleValues));
        }
    }

    @Inject(method = "showMoreWorldOptions", at = @At("RETURN"))
    private void nfrUi$keepModernLayout(boolean showWorldOptions, CallbackInfo ci) {
        if (!nfrUi$active() || nfrUi$gameTab == null) return;
        btnMoreOptions.visible = false;
        if (nfrUi$rulesTabActive) nfrUi$hideVanillaForRules();
        nfrUi$layoutControls();
        nfrUi$syncTabs();
    }

    @Inject(method = "keyTyped", at = @At("HEAD"), cancellable = true)
    private void nfrUi$rulesTabKeyTyped(char typedChar, int keyCode, CallbackInfo ci) {
        if (!nfrUi$active() || !nfrUi$rulesTabActive) return;
        // Keep keystrokes away from the hidden name/seed fields and from the
        // vanilla Enter-creates-world shortcut; only the rules list receives them.
        worldNameField.setFocused(false);
        worldSeedField.setFocused(false);
        if (nfrUi$rulesList != null) nfrUi$rulesList.forwardKeyTyped(typedChar, keyCode);
        ci.cancel();
    }

    @Inject(method = "updateScreen", at = @At("TAIL"))
    private void nfrUi$tickRulesTab(CallbackInfo ci) {
        if (nfrUi$active() && nfrUi$rulesTabActive && nfrUi$rulesList != null) {
            nfrUi$rulesList.tick();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (nfrUi$active() && nfrUi$rulesTabActive && nfrUi$rulesList != null) {
            nfrUi$rulesList.handleMouseInput();
        }
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    private void nfrUi$drawModernCreateWorld(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!nfrUi$active()) return;
        Map<String, Integer> rows = nfrUi$layoutControls();
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

        if (nfrUi$rulesTabActive) {
            if (nfrUi$rulesList != null) nfrUi$rulesList.drawScreen(mouseX, mouseY, partialTicks);
        } else if (inMoreWorldOptionsDisplay) {
            drawString(fontRenderer, I18n.format("selectWorld.enterSeed"),
                    worldSeedField.x, worldSeedField.y - 12, labelColor);
            worldSeedField.drawTextBox();
            drawString(fontRenderer, I18n.format("selectWorld.seedInfo"),
                    worldSeedField.x, rows.get("seedInfo"), secondaryColor);
            Integer noticeY = rows.get("notice");
            if (noticeY != null) {
                WorldType type = WorldType.WORLD_TYPES[selectedIndex];
                fontRenderer.drawSplitString(I18n.format(type.getInfoTranslationKey()),
                        btnMapType.x + 2, noticeY, btnMapType.width - 4, secondaryColor);
            }
        } else {
            drawString(fontRenderer, I18n.format("selectWorld.enterName"),
                    worldNameField.x, worldNameField.y - 12, labelColor);
            worldNameField.drawTextBox();
            drawString(fontRenderer, I18n.format("selectWorld.resultFolder") + " " + saveDirName,
                    worldNameField.x, rows.get("folderInfo"), secondaryColor);
            drawString(fontRenderer, gameModeDesc1, btnGameMode.x, rows.get("desc1"), descriptionColor);
            drawString(fontRenderer, gameModeDesc2, btnGameMode.x, rows.get("desc2"), secondaryColor);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        ci.cancel();
    }

    /**
     * Positions every control for the current tab and returns the y positions of the
     * text rows for {@link #nfrUi$drawModernCreateWorld}. Footer buttons stay pinned to
     * the bottom; the column layouter handles everything above them.
     */
    @Unique
    private Map<String, Integer> nfrUi$layoutControls() {
        int contentWidth = Math.min(300, Math.max(220, width - 40));
        int left = Math.max(10, width / 2 - contentWidth / 2);
        int half = (contentWidth - 10) / 2;
        int footerY = height - 28;
        int top = 60;
        int bottom = footerY - 8;

        // Tabs span the content column.
        int tabWidth = (contentWidth - 8) / 3;
        nfrUi$gameTab.x = left;
        nfrUi$gameTab.y = 34;
        nfrUi$gameTab.width = tabWidth;
        nfrUi$worldTab.x = left + tabWidth + 4;
        nfrUi$worldTab.y = 34;
        nfrUi$worldTab.width = tabWidth;
        nfrUi$rulesTab.x = left + 2 * (tabWidth + 4);
        nfrUi$rulesTab.y = 34;
        nfrUi$rulesTab.width = contentWidth - 2 * (tabWidth + 4);

        Map<String, Integer> rows;
        if (nfrUi$rulesTabActive) {
            // Park the hidden text fields off-screen so vanilla click forwarding
            // can never refocus them over the rules list.
            worldNameField.x = -1000;
            worldNameField.y = -1000;
            worldSeedField.x = -1000;
            worldSeedField.y = -1000;
            rows = Collections.emptyMap();
            if (nfrUi$rulesList != null) {
                nfrUi$rulesList.setDimensions(width, height, top, bottom);
            }
        } else if (inMoreWorldOptionsDisplay) {
            NfrColumnLayout column = new NfrColumnLayout()
                    .gap(14, 8)
                    .item("seed", 20)
                    .gap(4, 2).item("seedInfo", 9)
                    .gap(10, 4).item("mapType", 20);
            WorldType type = WorldType.WORLD_TYPES[selectedIndex];
            if (type != null && type.hasInfoNotice()) {
                int lines = fontRenderer.listFormattedStringToWidth(
                        I18n.format(type.getInfoTranslationKey()), contentWidth - 4).size();
                column.gap(3, 2).item("notice", Math.max(1, lines) * (fontRenderer.FONT_HEIGHT + 1));
            }
            if (btnCustomizeType.visible) {
                column.gap(6, 3).item("customize", 20);
            }
            column.gap(10, 4).item("pair", 20)
                    .gap(6, 3).item("cheats", 20);
            rows = column.layout(top, bottom);

            worldSeedField.x = left;
            worldSeedField.y = rows.get("seed");
            worldSeedField.width = contentWidth;
            btnMapType.x = left;
            btnMapType.y = rows.get("mapType");
            btnMapType.width = contentWidth;
            if (btnCustomizeType.visible) {
                btnCustomizeType.x = left;
                btnCustomizeType.y = rows.get("customize");
                btnCustomizeType.width = contentWidth;
            }
            // Structures and bonus chest share a row; when structures are hidden
            // (CUSTOMIZED world type) the bonus chest gets the full row instead of
            // sitting orphaned on one half.
            boolean pairFeatures = btnMapFeatures.visible;
            btnMapFeatures.x = left;
            btnMapFeatures.y = rows.get("pair");
            btnMapFeatures.width = half;
            btnBonusItems.x = pairFeatures ? left + half + 10 : left;
            btnBonusItems.y = rows.get("pair");
            btnBonusItems.width = pairFeatures ? half : contentWidth;
            btnAllowCommands.x = left;
            btnAllowCommands.y = rows.get("cheats");
            btnAllowCommands.width = contentWidth;
        } else {
            NfrColumnLayout column = new NfrColumnLayout()
                    .gap(14, 8)
                    .item("name", 20)
                    .gap(4, 2).item("folderInfo", 9)
                    .gap(12, 5).item("gameMode", 20)
                    .gap(3, 2).item("desc1", 9)
                    .gap(2, 1).item("desc2", 9);
            rows = column.layout(top, bottom);

            worldNameField.x = left;
            worldNameField.y = rows.get("name");
            worldNameField.width = contentWidth;
            btnGameMode.x = left;
            btnGameMode.y = rows.get("gameMode");
            btnGameMode.width = contentWidth;
        }

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
        return rows;
    }

    @Unique
    private void nfrUi$switchToTab(int tabId) {
        GuiCreateWorldAccessor accessor = (GuiCreateWorldAccessor) this;
        if (tabId == CreateWorldTabIds.RULES) {
            nfrUi$rulesTabActive = true;
            nfrUi$hideVanillaForRules();
        } else {
            boolean wantsWorld = tabId == CreateWorldTabIds.WORLD;
            boolean wasRules = nfrUi$rulesTabActive;
            nfrUi$rulesTabActive = false;
            if (wantsWorld != inMoreWorldOptionsDisplay) {
                accessor.nfrUi$toggleMoreWorldOptions();
            } else if (wasRules) {
                accessor.nfrUi$showMoreWorldOptions(inMoreWorldOptionsDisplay);
            }
            btnMoreOptions.visible = false;
        }
        nfrUi$layoutControls();
        nfrUi$syncTabs();
    }

    @Unique
    private void nfrUi$hideVanillaForRules() {
        btnGameMode.visible = false;
        btnMapFeatures.visible = false;
        btnBonusItems.visible = false;
        btnMapType.visible = false;
        btnAllowCommands.visible = false;
        btnCustomizeType.visible = false;
        btnMoreOptions.visible = false;
        worldNameField.setFocused(false);
        worldSeedField.setFocused(false);
    }

    @Unique
    private void nfrUi$syncTabs() {
        nfrUi$gameTab.enabled = nfrUi$rulesTabActive || inMoreWorldOptionsDisplay;
        nfrUi$worldTab.enabled = nfrUi$rulesTabActive || !inMoreWorldOptionsDisplay;
        nfrUi$rulesTab.enabled = !nfrUi$rulesTabActive;
        boolean modernStyle = CreateWorldConfig.usesModernStyle();
        nfrUi$gameTab.packedFGColour = modernStyle && !nfrUi$gameTab.enabled ? 0x52E875 : 0;
        nfrUi$worldTab.packedFGColour = modernStyle && !nfrUi$worldTab.enabled ? 0x52E875 : 0;
        nfrUi$rulesTab.packedFGColour = modernStyle && !nfrUi$rulesTab.enabled ? 0x52E875 : 0;
    }

    @Unique
    private boolean nfrUi$active() {
        return CreateWorldConfig.usesTabbedLayout()
                && ((Object) this).getClass() == GuiCreateWorld.class;
    }
}
