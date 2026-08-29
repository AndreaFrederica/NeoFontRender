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
import neofontrender.core.font.support.ShadowColorRemapRules;
import neofontrender.core.font.support.ShadowRenderSpec;

/** Large draft-only preview for the Shadow settings route. */
public final class NfrShadowPreview extends Widget<NfrShadowPreview> {
    private static final float FONT_SIZE = 42.0F;
    private static final int[] TEXT_COLORS = {
            0xFFFFFFFF, 0xFFE02D5F, 0xFF008FA8, 0xFFFFAA00
    };
    private final NfrSettingsDraft draft;

    public NfrShadowPreview(NfrSettingsDraft draft) {
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
                I18n.format("neofontrender.gui.preview.shadow_title"), 10, 11, 0xFFB8C8D8);

        drawNativePreview(right - 4, stageTop, stageBottom);

        Platform.setupDrawFont();
        String details = I18n.format("neofontrender.gui.preview.shadow_values",
                draft.shadowOffsetX, draft.shadowOffsetY,
                draft.modernShadow ? draft.shadowBlurRadius : 0.0F,
                Math.round(draft.shadowOpacity * 100.0F));
        minecraft.fontRenderer.drawString(
                minecraft.fontRenderer.trimStringToWidth(details, Math.max(1, width - 20)),
                10, Math.max(10, height - minecraft.fontRenderer.FONT_HEIGHT - 10), 0xFF8292A5);
    }

    private void drawNativePreview(int width, int stageTop, int stageBottom) {
        String[] sample = previewParts();
        int[] colors = TEXT_COLORS;
        ModernTextLayout[] layouts = new ModernTextLayout[sample.length];
        float totalWidth = 0.0F;
        ShadowRenderSpec spec = ShadowRenderSpec.of(
                draft.shadowOffsetX, draft.shadowOffsetY,
                draft.modernShadow ? draft.shadowBlurRadius : 0.0F,
                draft.shadowColor, draft.shadowColorMode, draft.shadowColoredRatio,
                draft.shadowColoredFunction,
                ShadowColorRemapRules.parse(draft.shadowColorOverrides), draft.shadowOpacity);
        boolean useShadow = draft.modernShadow && shadowEnabled();
        for (int index = 0; index < sample.length; index++) {
            int color = colors[index % colors.length];
            layouts[index] = useShadow
                    ? ModernTextApi.layoutFormattedWithShadow(sample[index], FONT_SIZE, color, spec)
                    : ModernTextApi.layoutFormatted(sample[index], FONT_SIZE, color, false);
            totalWidth += layouts[index].advance();
        }
        float cursor = Math.max(10.0F, (width - totalWidth) * 0.5F);
        float top = stageTop + Math.max(4.0F, (stageBottom - stageTop - FONT_SIZE) * 0.5F);
        for (ModernTextLayout layout : layouts) {
            layout.draw(cursor, top);
            cursor += layout.advance();
        }
        Platform.setupDrawFont();
    }

    private static String[] previewParts() {
        return new String[] {
                I18n.format("neofontrender.gui.preview.shadow_part_1"),
                I18n.format("neofontrender.gui.preview.shadow_part_2"),
                I18n.format("neofontrender.gui.preview.shadow_part_3"),
                I18n.format("neofontrender.gui.preview.shadow_part_4")
        };
    }

    private boolean shadowEnabled() {
        return draft.shadowOpacity > 0.0F && !"none".equals(draft.shadowMode);
    }

}
