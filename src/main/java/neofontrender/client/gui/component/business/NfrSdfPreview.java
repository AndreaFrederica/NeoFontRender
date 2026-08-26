package neofontrender.client.gui.component.business;

import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.Platform;
import com.cleanroommc.modularui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import neofontrender.api.text.ModernTextApi;
import neofontrender.api.text.ModernTextLayout;
import neofontrender.client.gui.model.NfrSettingsDraft;

/** Large draft-only SDF preview. The preview consumes draft values without changing live config. */
public final class NfrSdfPreview extends Widget<NfrSdfPreview> {
    private static final float FONT_SIZE = 34.0F;
    private final NfrSettingsDraft draft;

    public NfrSdfPreview(NfrSettingsDraft draft) {
        this.draft = draft;
    }

    public int preferredHeight() {
        return 174;
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.draw(context, widgetTheme);
        int width = getArea().w();
        int height = getArea().h();
        int right = Math.max(4, width - 4);
        int middle = Math.max(4, width / 2);
        int stageTop = 34;
        int stageBottom = Math.max(stageTop, height - 29);
        Gui.drawRect(4, 4, right, stageTop, 0xFF17222E);
        Gui.drawRect(4, stageTop, middle, stageBottom, 0xFFDCE4EC);
        Gui.drawRect(middle, stageTop, right, stageBottom, 0xFF46576A);
        Gui.drawRect(4, stageBottom, right, Math.max(stageBottom, height - 4), 0xFF17222E);
        Gui.drawRect(4, 4, right, 5, 0xFF00AEB8);
        Gui.drawRect(middle, stageTop, middle + 1, stageBottom, 0x668FA5BA);

        Platform.setupDrawFont();
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.fontRenderer.drawString(
                I18n.format("neofontrender.gui.preview.sdf_title"), 10, 11, 0xFFB8C8D8);
        drawSample(width, stageTop, stageBottom);
        String details = I18n.format("neofontrender.gui.preview.sdf_values",
                draft.sdfDistanceRange, draft.sdfEdgeSoftness,
                draft.sdfEnabled ? I18n.format("neofontrender.gui.on") : I18n.format("neofontrender.gui.off"));
        Platform.setupDrawFont();
        minecraft.fontRenderer.drawString(
                minecraft.fontRenderer.trimStringToWidth(details, Math.max(1, width - 20)),
                10, Math.max(10, height - minecraft.fontRenderer.FONT_HEIGHT - 10), 0xFF8292A5);
    }

    private void drawSample(int width, int stageTop, int stageBottom) {
        Minecraft minecraft = Minecraft.getMinecraft();
        String sample = I18n.format("neofontrender.gui.preview.sdf_sample");
        if (!ModernTextApi.isAvailable()) {
            Platform.setupDrawFont();
            minecraft.fontRenderer.drawString(sample, 12, stageTop + 30, 0xFFFFFFFF);
            return;
        }
        ModernTextLayout layout = ModernTextApi.layout(sample, FONT_SIZE, 0xFFFFFFFF);
        float x = Math.max(10.0F, (width - layout.advance()) * 0.5F);
        float y = stageTop + Math.max(8.0F, (stageBottom - stageTop - FONT_SIZE) * 0.5F);
        float softness = parseSoftness();
        if (softness > 0.55F) {
            float spread = Math.min(1.25F, (softness - 0.5F) * 0.75F);
            float layerOpacity = Math.min(0.22F, (softness - 0.5F) * 0.16F);
            layout.draw(x - spread, y, layerOpacity);
            layout.draw(x + spread, y, layerOpacity);
            layout.draw(x, y - spread, layerOpacity);
            layout.draw(x, y + spread, layerOpacity);
        }
        layout.draw(x, y);
    }

    private float parseSoftness() {
        try {
            float value = Float.parseFloat(draft.sdfEdgeSoftness);
            return Float.isFinite(value) ? Math.max(0.5F, Math.min(2.0F, value)) : 1.0F;
        } catch (RuntimeException ignored) {
            return 1.0F;
        }
    }
}
