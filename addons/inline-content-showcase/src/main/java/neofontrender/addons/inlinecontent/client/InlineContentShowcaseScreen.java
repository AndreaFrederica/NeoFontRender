package neofontrender.addons.inlinecontent.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import neofontrender.addons.inlinecontent.ShowcaseSamples;
import neofontrender.addons.inlinecontent.ShowcaseItems;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

public final class InlineContentShowcaseScreen extends GuiScreen {
    private static final int MATHEMATICS = 0;
    private static final int CHEMISTRY = 1;
    private static final int MARKDOWN = 2;
    private static final int ROW_HEIGHT = 54;
    private int page = MATHEMATICS;
    private int firstVisible;

    static void open() {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> minecraft.displayGuiScreen(new InlineContentShowcaseScreen()));
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int center = width / 2;
        buttonList.add(new GuiButton(0, center - 204, height - 28, 96, 20, "Mathematics"));
        buttonList.add(new GuiButton(1, center - 104, height - 28, 104, 20, "Chemistry SVG"));
        buttonList.add(new GuiButton(2, center + 4, height - 28, 92, 20, "Markdown"));
        buttonList.add(new GuiButton(3, center + 100, height - 28, 104, 20, "Give test item"));
        buttonList.add(new GuiButton(4, center + 208, height - 28, 116, 20, "Give Typst item"));
        updateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == MATHEMATICS || button.id == CHEMISTRY || button.id == MARKDOWN) {
            page = button.id;
            firstVisible = 0;
            updateButtons();
        } else if (button.id == 3 && mc.player != null) {
            ItemStack stack = new ItemStack(ShowcaseItems.AMINOBENZO_CROWN_ETHER_SOLUTION);
            if (!mc.player.inventory.addItemStackToInventory(stack)) mc.player.dropItem(stack, false);
        } else if (button.id == 4 && mc.player != null) {
            ItemStack stack = new ItemStack(ShowcaseItems.TYPST_CHEMISTRY_DEMONSTRATOR);
            if (!mc.player.inventory.addItemStackToInventory(stack)) mc.player.dropItem(stack, false);
        }
    }

    private void updateButtons() {
        for (GuiButton button : buttonList) {
            if (button.id == MATHEMATICS || button.id == CHEMISTRY || button.id == MARKDOWN) {
                button.enabled = button.id != page;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        String title = page == MATHEMATICS
                ? "NFR inline compatibility: LaTeX mathematics"
                : page == CHEMISTRY
                ? "NFR inline compatibility: organic chemistry SVG"
                : "NFR text pipeline: experimental Markdown";
        drawCenteredString(fontRenderer, title, width / 2, 12, 0xFFFFFFFF);
        drawCenteredString(fontRenderer,
                "Enable the matching Revo UI laboratory options; unresolved tokens remain visible.",
                width / 2, 25, 0xFFA0A0A0);

        List<ShowcaseSamples.Sample> samples = samples();
        int visibleRows = visibleRows();
        int last = Math.min(samples.size(), firstVisible + visibleRows);
        int y = 43;
        for (int index = firstVisible; index < last; index++) {
            drawSample(samples.get(index), y);
            y += ROW_HEIGHT;
        }
        if (samples.size() > visibleRows) {
            drawString(fontRenderer, (firstVisible + 1) + "-" + last + " / " + samples.size(),
                    width - 52, 12, 0xFF909090);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawSample(ShowcaseSamples.Sample sample, int y) {
        int left = Math.max(12, width / 2 - 250);
        int maximumWidth = Math.max(40, width - left - 12);
        drawString(fontRenderer, sample.name(), left, y, 0xFFE0E0E0);
        drawString(fontRenderer, ellipsize("Source: " + sample.visibleSource(), maximumWidth),
                left, y + 11, 0xFF808080);
        drawString(fontRenderer, sample.rendered(), left, y + 25, 0xFFFFFFFF);
        drawHorizontalLine(left, left + maximumWidth, y + ROW_HEIGHT - 4, 0x403F3F3F);
    }

    private String ellipsize(String value, int maximumWidth) {
        if (fontRenderer.getStringWidth(value) <= maximumWidth) return value;
        return fontRenderer.trimStringToWidth(value, Math.max(0,
                maximumWidth - fontRenderer.getStringWidth("..."))) + "...";
    }

    private int visibleRows() {
        return Math.max(1, (height - 43 - 36) / ROW_HEIGHT);
    }

    private void scroll(int direction) {
        int size = samples().size();
        int maximum = Math.max(0, size - visibleRows());
        firstVisible = Math.max(0, Math.min(maximum, firstVisible + direction));
    }

    private List<ShowcaseSamples.Sample> samples() {
        if (page == MATHEMATICS) return ShowcaseSamples.MATHEMATICS;
        if (page == CHEMISTRY) return ShowcaseSamples.CHEMISTRY;
        return ShowcaseSamples.MARKDOWN;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) scroll(wheel < 0 ? 1 : -1);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            mc.displayGuiScreen(null);
            return;
        }
        if (keyCode == 200) scroll(-1);
        if (keyCode == 208) scroll(1);
        super.keyTyped(typedChar, keyCode);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
