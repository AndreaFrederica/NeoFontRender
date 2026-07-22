package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import neofontrender.Tags;
import neofontrender.client.gui.NeofontrenderConfigScreen;

/**
 * Adds a textured Neo Font Render shortcut beside the vanilla language button.
 */
public final class NeofontrenderOptionsButtonHandler {

    private static final int BUTTON_ID = 9200;
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final ResourceLocation BUTTON_TEXTURE =
            new ResourceLocation(Tags.MOD_ID, "textures/gui/button.png");
    private TexturedOptionsButton optionsButton;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.getGui() instanceof GuiOptions)) {
            return;
        }
        GuiOptions gui = (GuiOptions) event.getGui();

        this.optionsButton = new TexturedOptionsButton(
                BUTTON_ID,
                gui.width / 2 - 180,
                gui.height / 6 + 96 - 6,
                I18n.format("neofontrender.options.button")
        );
        event.getButtonList().add(this.optionsButton);
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.getGui() instanceof GuiOptions)) {
            return;
        }
        if (this.optionsButton != null && this.optionsButton.isMouseOver()) {
            event.getGui().drawHoveringText(
                    this.optionsButton.getTooltip(),
                    event.getMouseX(),
                    event.getMouseY());
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.getGui() instanceof GuiOptions)) {
            return;
        }
        GuiButton button = event.getButton();
        if (button.enabled && button == this.optionsButton) {
            NeofontrenderConfigScreen.open(Minecraft.getMinecraft().currentScreen);
            event.setCanceled(true);
        }
    }

    /** Uses the upper texture row normally and the lower row while hovered. */
    private static final class TexturedOptionsButton extends GuiButton {

        private final String tooltip;

        private TexturedOptionsButton(int id, int x, int y, String tooltip) {
            super(id, x, y, BUTTON_WIDTH, BUTTON_HEIGHT, "");
            this.tooltip = tooltip;
        }

        private String getTooltip() {
            return this.tooltip;
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) {
                return;
            }

            // Draw the vanilla button material first, then place the transparent icon over it.
            super.drawButton(mc, mouseX, mouseY, partialTicks);
            mc.getTextureManager().bindTexture(BUTTON_TEXTURE);
            GlStateManager.color(1.0F, 1.0F, 1.0F, this.enabled ? 1.0F : 0.5F);
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ZERO);
            Gui.drawModalRectWithCustomSizedTexture(
                    this.x,
                    this.y,
                    0.0F,
                    this.hovered ? BUTTON_HEIGHT : 0.0F,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT,
                    BUTTON_WIDTH,
                    BUTTON_HEIGHT * 2.0F);
        }
    }
}
