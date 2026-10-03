package neofontrender.core.font.postprocess;

import neofontrender.api.text.ModernTextLayout;
import neofontrender.api.text.gl.TextGlComponent;
import neofontrender.api.text.gl.TextGlComponentApi;
import neofontrender.api.text.postprocess.TextPostProcessContext;
import neofontrender.api.text.postprocess.TextPostProcessor;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.FontRenderTuning;
import neofontrender.text.StructuredEffectSpan;
import neofontrender.api.text.effect.TextEffectDefinition;
import neofontrender.api.text.effect.TextEffectRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Modern draw-time bridge for Brilliant Text effect metadata. */
public final class BrilliantTextPostProcessor implements TextPostProcessor {
    public static final BrilliantTextPostProcessor INSTANCE = new BrilliantTextPostProcessor();
    public static final String ID = "brilliant_text:effects";

    private BrilliantTextPostProcessor() {}

    @Override public String id() { return ID; }
    @Override public int priority() { return 100; }

    @Override
    public boolean supports(TextPostProcessContext context) {
        if (context.effects().isEmpty()) return false;
        boolean eligible = neofontrender.core.config.NeofontrenderConfig.brilliantTextEnabled();
        if (!eligible) {
            for (StructuredEffectSpan effect : context.effects()) {
                if ("textanimator:neon".equals(effect.effectId())) {
                    eligible = true;
                    break;
                }
            }
        }
        if (!eligible) return false;
        FontRenderTuning.DrawContext draw = FontRenderTuning.currentDrawContext();
        return draw.orthographic() && !draw.rotation() && TextGlComponentApi.active() != null;
    }

    @Override
    public ModernTextLayout process(TextPostProcessContext context, ModernTextLayout current) {
        boolean supportedEffect = false;
        for (StructuredEffectSpan effect : context.effects()) {
            if (TextEffectRegistry.get(effect.effectId()) != null) {
                supportedEffect = true;
                break;
            }
        }
        if (!supportedEffect) return current;
        TextGlComponent component = TextGlComponentApi.active();
        if (component == null) return current;
        List<StructuredEffectSpan> effects = enrichEffects(context);
        TextRenderResult result = component.draw(current.resultForPostProcess(), 0.0F, 0.0F,
                current.alphaForPostProcess(), effects);
        return ModernTextLayout.fromPostProcessResult(result, current.alphaForPostProcess());
    }

    private static List<StructuredEffectSpan> enrichEffects(TextPostProcessContext context) {
        String text = context.structuredText().plainText();
        List<StructuredEffectSpan> result = new ArrayList<>();
        for (StructuredEffectSpan effect : context.effects()) {
            if (TextEffectRegistry.get(effect.effectId()) == null) continue;
            Map<String, String> parameters = new HashMap<>(effect.parameters());
            int start = Math.max(0, Math.min(text.length(), effect.start()));
            int end = Math.max(start, Math.min(text.length(), effect.end()));
            float left;
            float right;
            if (context.backend() == null) {
                left = start;
                right = end;
            } else {
                left = context.backend().measureStructuredAtSize(
                        context.structuredText().slice(0, start), context.baseArgb(),
                        false, context.fontSize());
                right = context.backend().measureStructuredAtSize(
                        context.structuredText().slice(0, end), context.baseArgb(),
                        false, context.fontSize());
            }
            parameters.put("left", Float.toString(left));
            parameters.put("right", Float.toString(right));
            parameters.put("top", Float.toString(-1.0F));
            parameters.put("bottom", Float.toString(context.fontSize() + 2.0F));
            int baseArgb = context.baseArgb();
            if ((baseArgb & 0xFC000000) == 0) baseArgb |= 0xFF000000;
            parameters.putIfAbsent("textColor", Integer.toHexString(baseArgb));
            StructuredEffectSpan prepared = new StructuredEffectSpan(effect.start(), effect.end(),
                    effect.effectId(), parameters,
                    effect.lineWide(), effect.animationRenderMode());
            TextEffectDefinition definition = TextEffectRegistry.get(effect.effectId());
            result.add(definition == null ? prepared : definition.prepare(prepared));
        }
        return result;
    }

}
