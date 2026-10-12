package neofontrender.api.text.postprocess;

import neofontrender.core.font.backend.TextRenderBackend;
import neofontrender.core.font.backend.TextRenderResult;
import neofontrender.core.font.support.ShadowRenderSpec;
import neofontrender.text.StructuredEffectSpan;
import neofontrender.text.StructuredText;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Immutable structured-text request data available to modern post-processors. */
public final class TextPostProcessContext {
    private final StructuredText structuredText;
    private final int baseArgb;
    private final float fontSize;
    private final boolean shadowRequested;
    private final TextRenderBackend backend;
    private final ShadowRenderSpec shadowSpec;
    private final Supplier<TextRenderResult> shadowSourceFactory;
    private TextRenderResult shadowSource;
    private boolean shadowSourceResolved;

    public TextPostProcessContext(StructuredText structuredText, int baseArgb,
                                  float fontSize, boolean shadowRequested,
                                  TextRenderBackend backend, ShadowRenderSpec shadowSpec,
                                  Supplier<TextRenderResult> shadowSourceFactory) {
        this.structuredText = Objects.requireNonNull(structuredText, "structuredText");
        this.baseArgb = baseArgb;
        this.fontSize = Float.isFinite(fontSize) ? Math.max(1.0F, fontSize) : 8.0F;
        this.shadowRequested = shadowRequested;
        this.backend = backend;
        this.shadowSpec = shadowSpec == null ? ShadowRenderSpec.fromConfig() : shadowSpec;
        this.shadowSourceFactory = shadowSourceFactory;
    }

    public String formattedText() { return structuredText.plainText(); }
    public int baseArgb() { return baseArgb; }
    public float fontSize() { return fontSize; }
    public boolean shadowRequested() { return shadowRequested; }
    public TextRenderBackend backend() { return backend; }
    public ShadowRenderSpec shadowSpec() { return shadowSpec; }
    public StructuredText structuredText() { return structuredText; }
    public List<StructuredEffectSpan> effects() { return structuredText.effects(); }

    /** Creates and memoizes the backend's shadow source at most once per draw. */
    public synchronized TextRenderResult shadowSource() {
        if (!shadowSourceResolved) {
            shadowSourceResolved = true;
            shadowSource = shadowSourceFactory == null ? TextRenderResult.EMPTY
                    : shadowSourceFactory.get();
            if (shadowSource == null) shadowSource = TextRenderResult.EMPTY;
        }
        return shadowSource;
    }
}
