package neofontrender.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiScreenEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import neofontrender.Tags;
import neofontrender.client.gui.NeofontrenderConfigScreen;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import java.util.Collections;

/**
 * Adds a textured Neo Font Render shortcut beside the vanilla language button.
 */
public final class NeofontrenderOptionsButtonHandler {

    private static final int BUTTON_ID = 9200;
    private static final int BUTTON_WIDTH = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final ResourceLocation BUTTON_TEXTURE =
            new ResourceLocation(Tags.MOD_ID, "textures/gui/button.png");
    private final TooltipRenderer tooltipRenderer = new TooltipRenderer();
    private TexturedOptionsButton optionsButton;

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiOptions)) {
            return;
        }
        GuiOptions gui = (GuiOptions) event.gui;

        this.optionsButton = new TexturedOptionsButton(
                BUTTON_ID,
                gui.width / 2 - 180,
                gui.height / 6 + 120 - 6,
                I18n.format("neofontrender.options.button")
        );
        event.buttonList.add(this.optionsButton);
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiOptions)) {
            return;
        }
        if (this.optionsButton != null && this.optionsButton.func_146115_a()) {
            this.tooltipRenderer.drawTooltip(
                    event.gui,
                    this.optionsButton.getTooltip(),
                    event.mouseX,
                    event.mouseY);
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!(event.gui instanceof GuiOptions)) {
            return;
        }
        GuiButton button = event.button;
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
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!this.visible) {
                return;
            }

            // Draw the vanilla button material first, then place the transparent icon over it.
            super.drawButton(mc, mouseX, mouseY);
            boolean blendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
            ContextCapabilities capabilities = GLContext.getCapabilities();
            boolean separateBlendSupported = capabilities.OpenGL14 || capabilities.GL_EXT_blend_func_separate;
            int blendSourceRgb = GL11.glGetInteger(
                    separateBlendSupported ? GL14.GL_BLEND_SRC_RGB : GL11.GL_BLEND_SRC);
            int blendDestinationRgb = GL11.glGetInteger(
                    separateBlendSupported ? GL14.GL_BLEND_DST_RGB : GL11.GL_BLEND_DST);
            int blendSourceAlpha = separateBlendSupported
                    ? GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA)
                    : blendSourceRgb;
            int blendDestinationAlpha = separateBlendSupported
                    ? GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA)
                    : blendDestinationRgb;

            mc.getTextureManager().bindTexture(BUTTON_TEXTURE);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, this.enabled ? 1.0F : 0.5F);
            try {
                GL11.glEnable(GL11.GL_BLEND);
                OpenGlHelper.glBlendFunc(
                        GL11.GL_SRC_ALPHA,
                        GL11.GL_ONE_MINUS_SRC_ALPHA,
                        GL11.GL_ONE,
                        GL11.GL_ZERO);
                Gui.func_146110_a(
                        this.xPosition,
                        this.yPosition,
                        0.0F,
                        this.func_146115_a() ? BUTTON_HEIGHT : 0.0F,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT,
                        BUTTON_WIDTH,
                        BUTTON_HEIGHT * 2.0F);
            } finally {
                OpenGlHelper.glBlendFunc(
                        blendSourceRgb,
                        blendDestinationRgb,
                        blendSourceAlpha,
                        blendDestinationAlpha);
                if (blendEnabled) {
                    GL11.glEnable(GL11.GL_BLEND);
                } else {
                    GL11.glDisable(GL11.GL_BLEND);
                }
            }
        }
    }

    /** Exposes vanilla's protected tooltip renderer without changing Minecraft access levels. */
    private static final class TooltipRenderer extends GuiScreen {

        private void drawTooltip(GuiScreen screen, String tooltip, int mouseX, int mouseY) {
            this.width = screen.width;
            this.height = screen.height;
            this.fontRendererObj = Minecraft.getMinecraft().fontRenderer;
            this.func_146283_a(Collections.singletonList(tooltip), mouseX, mouseY);
        }
    }
}
