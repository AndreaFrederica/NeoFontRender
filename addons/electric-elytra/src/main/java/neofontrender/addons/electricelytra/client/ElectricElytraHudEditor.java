package neofontrender.addons.electricelytra.client;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import neofontrender.addons.api.flight.FlightHudEditorScreen;

import java.io.IOException;

/** Transparent layout mode that keeps the live flight HUD visible while moving its addon panel. */
final class ElectricElytraHudEditor extends GuiScreen implements FlightHudEditorScreen {
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawCenteredString(fontRenderer,
                I18n.format("gui.neofontrender_electric_elytra.hud_editor"),
                width / 2, 8, 0xFFFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) ElectricElytraHud.INSTANCE.beginDrag(mouseX, mouseY);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton,
                                  long timeSinceLastClick) {
        if (clickedMouseButton == 0) ElectricElytraHud.INSTANCE.dragTo(mouseX, mouseY);
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) ElectricElytraHud.INSTANCE.endDrag();
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void onGuiClosed() {
        ElectricElytraHud.INSTANCE.endDrag();
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
