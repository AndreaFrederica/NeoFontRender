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

/**
 * Reworks the create-world screen into a two-tab layout. All shadowed members keep their
 * 1.7.10 SRG names because MCP never assigned readable names to GuiCreateWorld internals.
 */
@Mixin(GuiCreateWorld.class)
public abstract class MixinGuiCreateWorldModernLayout extends GuiScreen {
    @Unique private static final int NFR_GAME_TAB = 28640;
    @Unique private static final int NFR_WORLD_TAB = 28641;

    @Shadow private GuiTextField field_146333_g; // world name field
    @Shadow private GuiTextField field_146335_h; // world seed field
    @Shadow private String field_146336_i; // save dir name
    @Shadow private boolean field_146344_y; // in more world options display
    @Shadow private GuiButton field_146343_z; // game mode button
    @Shadow private GuiButton field_146324_A; // more options button
    @Shadow private GuiButton field_146325_B; // map features button
    @Shadow private GuiButton field_146326_C; // bonus items button
    @Shadow private GuiButton field_146320_D; // map type button
    @Shadow private GuiButton field_146321_E; // allow commands button
    @Shadow private GuiButton field_146322_F; // customize type button
    @Shadow private String field_146323_G; // game mode description line 1
    @Shadow private String field_146328_H; // game mode description line 2
    @Shadow private int field_146331_K; // selected world type index

    @Unique private GuiButton nfrUi$gameTab;
    @Unique private GuiButton nfrUi$worldTab;

    @Inject(method = "initGui", at = @At("TAIL"))
    private void nfrUi$modernizeCreateWorld(CallbackInfo ci) {
        if (!nfrUi$active()) return;
        nfrUi$gameTab = new GuiButton(NFR_GAME_TAB, 0, 0, 110, 20,
                AddonI18n.tr("neofontrender_ui_enhancements.create_world.game"));
        this.buttonList.add(nfrUi$gameTab);
        nfrUi$worldTab = new GuiButton(NFR_WORLD_TAB, 0, 0, 110, 20,
                AddonI18n.tr("neofontrender_ui_enhancements.create_world.world"));
        this.buttonList.add(nfrUi$worldTab);
        field_146324_A.visible = false;
        nfrUi$layoutControls();
        nfrUi$syncTabs();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    private void nfrUi$selectCreateWorldTab(GuiButton button, CallbackInfo ci) {
        if (!nfrUi$active() || (button.id != NFR_GAME_TAB && button.id != NFR_WORLD_TAB)) return;
        boolean wantsWorld = button.id == NFR_WORLD_TAB;
        if (wantsWorld != field_146344_y) {
            ((GuiCreateWorldAccessor) this).nfrUi$toggleMoreWorldOptions();
            field_146324_A.visible = false;
            nfrUi$layoutControls();
        }
        nfrUi$syncTabs();
        ci.cancel();
    }

    @Inject(method = "func_146316_a", at = @At("RETURN"))
    private void nfrUi$keepModernLayout(boolean showWorldOptions, CallbackInfo ci) {
        if (!nfrUi$active() || nfrUi$gameTab == null) return;
        field_146324_A.visible = false;
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
        drawCenteredString(fontRendererObj, I18n.format("selectWorld.create"), width / 2, 10, 0xFFFFFFFF);

        int labelColor = modernStyle ? 0xFFD0D4DA : 0xFFA0A0A0;
        int secondaryColor = modernStyle ? 0xFF8D949E : 0xFF808080;
        int descriptionColor = modernStyle ? 0xFFB5BBC3 : 0xFFA0A0A0;

        if (field_146344_y) {
            drawString(fontRendererObj, I18n.format("selectWorld.enterSeed"), field_146335_h.xPosition, field_146335_h.yPosition - 12, labelColor);
            drawString(fontRendererObj, I18n.format("selectWorld.seedInfo"), field_146335_h.xPosition, field_146335_h.yPosition + 23, secondaryColor);
            field_146335_h.drawTextBox();
            WorldType type = WorldType.worldTypes[field_146331_K];
            if (type != null && type.showWorldInfoNotice() && !field_146322_F.visible) {
                fontRendererObj.drawSplitString(I18n.format(type.func_151359_c()),
                        field_146320_D.xPosition + 2, field_146320_D.yPosition + 23,
                        field_146320_D.width - 4, secondaryColor);
            }
        } else {
            drawString(fontRendererObj, I18n.format("selectWorld.enterName"), field_146333_g.xPosition, field_146333_g.yPosition - 12, labelColor);
            drawString(fontRendererObj, I18n.format("selectWorld.resultFolder") + " " + field_146336_i,
                    field_146333_g.xPosition, field_146333_g.yPosition + 23, secondaryColor);
            field_146333_g.drawTextBox();
            drawString(fontRendererObj, field_146323_G, field_146343_z.xPosition, field_146343_z.yPosition + 23, descriptionColor);
            drawString(fontRendererObj, field_146328_H, field_146343_z.xPosition, field_146343_z.yPosition + 35, secondaryColor);
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

        nfrUi$gameTab.xPosition = width / 2 - 112;
        nfrUi$gameTab.yPosition = 32;
        nfrUi$worldTab.xPosition = width / 2 + 2;
        nfrUi$worldTab.yPosition = 32;

        field_146333_g.xPosition = left;
        field_146333_g.yPosition = 70;
        field_146333_g.width = contentWidth;
        field_146335_h.xPosition = left;
        field_146335_h.yPosition = 70;
        field_146335_h.width = contentWidth;

        field_146343_z.xPosition = left;
        field_146343_z.yPosition = 120;
        field_146343_z.width = contentWidth;
        field_146320_D.xPosition = left;
        field_146320_D.yPosition = 114;
        field_146320_D.width = contentWidth;
        field_146322_F.xPosition = left;
        field_146322_F.yPosition = 144;
        field_146322_F.width = contentWidth;
        field_146325_B.xPosition = left;
        field_146325_B.yPosition = 172;
        field_146325_B.width = half;
        field_146326_C.xPosition = left + half + 10;
        field_146326_C.yPosition = 172;
        field_146326_C.width = half;
        field_146321_E.xPosition = left;
        field_146321_E.yPosition = Math.min(202, footerY - 24);
        field_146321_E.width = contentWidth;

        for (GuiButton button : buttonList) {
            if (button.id == 0) {
                button.xPosition = left;
                button.yPosition = footerY;
                button.width = half;
            } else if (button.id == 1) {
                button.xPosition = left + half + 10;
                button.yPosition = footerY;
                button.width = half;
            }
        }
    }

    @Unique
    private void nfrUi$syncTabs() {
        nfrUi$gameTab.enabled = field_146344_y;
        nfrUi$worldTab.enabled = !field_146344_y;
        boolean modernStyle = CreateWorldConfig.usesModernStyle();
        nfrUi$gameTab.packedFGColour = modernStyle && !field_146344_y ? 0x52E875 : 0;
        nfrUi$worldTab.packedFGColour = modernStyle && field_146344_y ? 0x52E875 : 0;
    }

    @Unique
    private boolean nfrUi$active() {
        return CreateWorldConfig.usesTabbedLayout()
                && ((Object) this).getClass() == GuiCreateWorld.class;
    }
}
