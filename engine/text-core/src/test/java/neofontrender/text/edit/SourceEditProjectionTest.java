package neofontrender.text.edit;

import neofontrender.text.SourceMap;
import neofontrender.text.StructuredText;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SourceEditProjectionTest {
    @Test
    void projectsRecognizedEffectToOriginalSource() {
        StructuredText text = new StructuredText("§vhello", "hello", Collections.emptyList(),
                Collections.singletonList(new neofontrender.text.StructuredEffectSpan(
                        0, 5, "brilliant_text:v", Collections.emptyMap(), false)),
                Collections.emptyList(), new SourceMap(7,
                        new int[]{2, 3, 4, 5, 6, 7}, new int[]{2, 3, 4, 5, 6, 7}));
        SourceEditProjection projection = SourceEditProjection.of(text);
        assertEquals(1, projection.spans().size());
        assertEquals(SourcePreviewMode.RAW,
                projection.mode(SourceEditState.of(text.sourceText(), 4)));
    }
}
