package neofontrender.addons.inlinecontent.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import neofontrender.addons.inlinecontent.ShowcaseSamples;
import neofontrender.addons.inlinecontent.TypstChemicalCatalog;
import neofontrender.addons.inlinecontent.TypstElementCatalog;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

public final class InlineContentShowcaseScreen extends GuiScreen {
    private static final int MATHEMATICS = 0;
    private static final int CHEMISTRY = 1;
    private static final int MARKDOWN = 2;
    private static final int TYPST = 3;
    private static final int ELEMENTS = 4;
    private static final int ELEMENT_ITEMS = 5;
    private static final String[] PAGE_KEYS = {"math", "svg", "markdown", "typst", "elements", "element_items"};
    private static final int ROW_HEIGHT = 54;
    private int page = MATHEMATICS;
    private int firstVisible;
    private int chemicalIndex;
    private int elementIndex;
    private boolean copied;

    static void open() {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> minecraft.displayGuiScreen(new InlineContentShowcaseScreen()));
    }

    public static void openChemical(TypstChemicalCatalog.Chemical chemical) {
        InlineContentShowcaseScreen screen = new InlineContentShowcaseScreen();
        screen.page = chemical == null ? ELEMENTS : TYPST;
        screen.chemicalIndex = chemical == null ? 0 : TypstChemicalCatalog.ALL.indexOf(chemical);
        Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().displayGuiScreen(screen));
    }

    public static void openElement(TypstElementCatalog.Element element) {
        InlineContentShowcaseScreen screen = new InlineContentShowcaseScreen();
        screen.page = ELEMENT_ITEMS;
        screen.elementIndex = element.atomicNumber() - 1;
        Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().displayGuiScreen(screen));
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int tabWidth = Math.min(108, (width - 16) / PAGE_KEYS.length);
        int left = (width - tabWidth * PAGE_KEYS.length) / 2;
        for (int i = 0; i < PAGE_KEYS.length; i++) {
            buttonList.add(new GuiButton(i, left + i * tabWidth, 39, tabWidth - 2, 20,
                    ellipsize(I18n.format("showcase.page." + PAGE_KEYS[i]), tabWidth - 8)));
        }
        int controlWidth = Math.min(104, (width - 20) / 3);
        left = (width - controlWidth * 3) / 2;
        buttonList.add(new GuiButton(10, left, height - 28, controlWidth - 2, 20, I18n.format("showcase.previous")));
        buttonList.add(new GuiButton(11, left + controlWidth, height - 28, controlWidth - 2, 20, I18n.format("showcase.next")));
        buttonList.add(new GuiButton(12, left + controlWidth * 2, height - 28, controlWidth - 2, 20, I18n.format("showcase.copy")));
        updateButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id >= MATHEMATICS && button.id <= ELEMENT_ITEMS) {
            page = button.id;
            firstVisible = 0;
            copied = false;
            updateButtons();
        } else if (button.id == 10) {
            scroll(-1);
        } else if (button.id == 11) {
            scroll(1);
        } else if (button.id == 12) {
            String token = page == ELEMENTS ? TypstChemicalCatalog.PERIODIC_TABLE_TOKEN
                    : page == ELEMENT_ITEMS ? element().token()
                    : chemical().formulaToken() + " " + chemical().notationToken()
                    + (chemical().structureToken() == null ? "" : " " + chemical().structureToken());
            // Export the complete game token for editors; the periodic table exceeds vanilla chat limits.
            setClipboardString(token.replace('\r', ' ').replace('\n', ' '));
            copied = true;
            updateButtons();
        }
    }

    private void updateButtons() {
        for (GuiButton button : buttonList) {
            if (button.id >= MATHEMATICS && button.id <= ELEMENT_ITEMS) {
                button.enabled = button.id != page;
            } else if (button.id == 10 || button.id == 11) {
                button.visible = page != ELEMENTS;
                int current = page == ELEMENT_ITEMS ? elementIndex : page == TYPST ? chemicalIndex : firstVisible;
                int maximum = page == ELEMENT_ITEMS ? TypstElementCatalog.ALL.size() - 1
                        : page == TYPST ? TypstChemicalCatalog.ALL.size() - 1
                        : Math.max(0, samples().size() - visibleRows());
                button.enabled = button.id == 10 ? current > 0 : current < maximum;
            } else if (button.id == 12) {
                button.visible = page >= TYPST;
                button.displayString = I18n.format(copied ? "showcase.copied" : "showcase.copy");
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format("showcase.title"), width / 2, 12, 0xFFFFFFFF);
        drawCenteredString(fontRenderer, I18n.format("showcase.hint"),
                width / 2, 25, 0xFFA0A0A0);

        if (page >= TYPST) {
            drawTypst();
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        List<ShowcaseSamples.Sample> samples = samples();
        int visibleRows = visibleRows();
        int last = Math.min(samples.size(), firstVisible + visibleRows);
        int y = 68;
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

    private TypstChemicalCatalog.Chemical chemical() {
        return TypstChemicalCatalog.ALL.get(chemicalIndex);
    }

    private TypstElementCatalog.Element element() {
        return TypstElementCatalog.ALL.get(elementIndex);
    }

    private void drawTypst() {
        int left = Math.max(12, width / 2 - 250);
        if (page == ELEMENT_ITEMS) {
            var element = element();
            drawCenteredString(fontRenderer, element.atomicNumber() + " / 118   "
                    + I18n.format(element.translationKey()), width / 2, 70, 0xFFE0EFFF);
            float em = Math.max(1, fontRenderer.FONT_HEIGHT);
            drawCenteredString(fontRenderer, TypstChemicalCatalog.token(element.source(),
                    "rows=" + Math.max(2, Math.min(14, (height - 151) / em))
                            + ",max-width=" + (width - 28) / em + "em,align=top"), width / 2, 91, 0xFFFFFFFF);
            drawCenteredString(fontRenderer, I18n.format("showcase.element.category." + element.category()),
                    width / 2, height - 57, 0xFFA0A0A0);
        } else if (page == ELEMENTS) {
            drawCenteredString(fontRenderer, I18n.format("showcase.typst.native"), width / 2, 68, 0xFFB0C8DD);
            float em = Math.max(1, fontRenderer.FONT_HEIGHT);
            String token = TypstChemicalCatalog.token(TypstChemicalCatalog.PERIODIC_TABLE_SOURCE,
                    "rows=" + Math.max(1, (height - 135) / em) + ",max-width=" + (width - 28) / em
                            + "em,align=top");
            drawCenteredString(fontRenderer, token, width / 2, 86, 0xFFFFFFFF);
        } else {
            var chemical = chemical();
            drawString(fontRenderer, (chemicalIndex + 1) + " / " + TypstChemicalCatalog.ALL.size()
                    + "   " + I18n.format(chemical.translationKey()), left, 70, 0xFFE0EFFF);
            drawString(fontRenderer, chemical.formulaToken(), left, 91, 0xFFFFFFFF);
            drawString(fontRenderer, I18n.format("showcase.typst.notation"), left, 116, 0xFFA0A0A0);
            float em = Math.max(1, fontRenderer.FONT_HEIGHT);
            drawString(fontRenderer, TypstChemicalCatalog.token(TypstChemicalCatalog.formulaSource(chemical.notation()),
                    "rows=2,max-width=" + (width - left - 16) / em + "em,align=top"), left, 133, 0xFFFFFFFF);
            if (chemical.structureSource() != null && height > 220) {
                drawString(fontRenderer, TypstChemicalCatalog.token(chemical.structureSource(),
                        "rows=" + Math.min(8, (height - 216) / em) + ",max-width=18em,align=top"), left, 164, 0xFFFFFFFF);
            }
        }
        drawCenteredString(fontRenderer, I18n.format("showcase.creative"), width / 2, height - 43, 0xFF909090);
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
        return Math.max(1, (height - 68 - 36) / ROW_HEIGHT);
    }

    private void scroll(int direction) {
        copied = false;
        if (page == ELEMENTS) return;
        if (page == ELEMENT_ITEMS) {
            elementIndex = Math.max(0, Math.min(TypstElementCatalog.ALL.size() - 1, elementIndex + direction));
            updateButtons();
            return;
        }
        if (page == TYPST) {
            chemicalIndex = Math.max(0, Math.min(TypstChemicalCatalog.ALL.size() - 1, chemicalIndex + direction));
            updateButtons();
            return;
        }
        int size = samples().size();
        int maximum = Math.max(0, size - visibleRows());
        firstVisible = Math.max(0, Math.min(maximum, firstVisible + direction));
        updateButtons();
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
        if (keyCode == 203) scroll(-1);
        if (keyCode == 205) scroll(1);
        super.keyTyped(typedChar, keyCode);
    }

    @Override public boolean doesGuiPauseGame() { return false; }
}
