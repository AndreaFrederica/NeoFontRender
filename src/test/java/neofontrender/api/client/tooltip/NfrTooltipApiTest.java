package neofontrender.api.client.tooltip;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NfrTooltipApiTest {
    @Test
    void documentRenderPublishesModernRenderScope() {
        AtomicBoolean observed = new AtomicBoolean();
        NfrTooltipApi.Renderer renderer = (document, mouseX, mouseY, font) -> {
            observed.set(NfrTooltipApi.isModernRender());
            return true;
        };
        NfrTooltipApi.register(renderer);
        try {
            assertFalse(NfrTooltipApi.isModernRender());
            assertTrue(NfrTooltipApi.render(document(), 0, 0, null));
            assertTrue(observed.get());
            assertFalse(NfrTooltipApi.isModernRender());
        } finally {
            NfrTooltipApi.unregister(renderer);
        }
    }

    @Test
    void scopeIsClearedWhenRendererFails() {
        AtomicBoolean observed = new AtomicBoolean();
        NfrTooltipApi.Renderer renderer = (document, mouseX, mouseY, font) -> {
            observed.set(NfrTooltipApi.isModernRender());
            throw new IllegalStateException("renderer failure");
        };
        NfrTooltipApi.register(renderer);
        try {
            assertThrows(IllegalStateException.class,
                    () -> NfrTooltipApi.render(document(), 0, 0, null));
            assertTrue(observed.get());
            assertFalse(NfrTooltipApi.isModernRender());
        } finally {
            NfrTooltipApi.unregister(renderer);
        }
    }

    @Test
    void unregisteredRendererDoesNotPublishScope() {
        assertFalse(NfrTooltipApi.isModernRender());
        assertFalse(NfrTooltipApi.render(document(), 0, 0, null));
        assertFalse(NfrTooltipApi.isModernRender());
    }

    @Test
    void stackRenderComposesRegisteredDocumentProviders() {
        AtomicBoolean observed = new AtomicBoolean();
        NfrTooltipApi.DocumentProvider provider = (stack, lines) -> java.util.Optional.of(
                NfrTooltipApi.TooltipDocument.builder(stack, lines)
                        .add(new NfrTooltipApi.SpacerNode(4))
                        .build());
        NfrTooltipApi.Renderer renderer = (document, mouseX, mouseY, font) -> {
            observed.set(document.nodes.size() == 1
                    && document.nodes.get(0).kind() == NfrTooltipApi.Kind.SPACER);
            return true;
        };
        NfrTooltipApi.registerDocumentProvider(provider);
        NfrTooltipApi.register(renderer);
        try {
            assertTrue(NfrTooltipApi.render(null, Collections.singletonList("line"),
                    0, 0, null));
            assertTrue(observed.get());
        } finally {
            NfrTooltipApi.unregister(renderer);
            NfrTooltipApi.unregisterDocumentProvider(provider);
        }
    }

    @Test
    void nestedLayoutNodesMeasureWithoutCoordinates() {
        NfrTooltipApi.GroupNode group = new NfrTooltipApi.GroupNode(
                java.util.Arrays.asList(new NfrTooltipApi.SpacerNode(6),
                        new NfrTooltipApi.SpacerNode(10)),
                NfrTooltipApi.LayoutDirection.VERTICAL, NfrTooltipApi.LayoutAlignment.START, 2);
        assertEquals(18, group.height(null));
        assertEquals(0, group.width(null));
    }

    @Test
    void previewEffectsCanBeRegisteredAndRemoved() {
        NfrTooltipApi.PreviewEffect effect = new NfrTooltipApi.PreviewEffect() {
            @Override public String id() { return "test_effect"; }
            @Override public void render(NfrTooltipApi.PreviewEffectContext context) { }
        };
        NfrTooltipApi.PreviewRegistry.registerEffect(effect);
        try {
            assertTrue(NfrTooltipApi.PreviewRegistry.findEffect("TEST_EFFECT") == effect);
        } finally {
            NfrTooltipApi.PreviewRegistry.unregisterEffect(effect);
        }
        assertTrue(NfrTooltipApi.PreviewRegistry.findEffect("test_effect") == null);
    }

    @Test
    void customPreviewRenderersUseStableStringIds() {
        NfrTooltipApi.PreviewRequest request = new NfrTooltipApi.PreviewRequest() {
            @Override public NfrTooltipApi.PreviewKind previewKind() {
                return NfrTooltipApi.PreviewKind.CUSTOM;
            }
            @Override public String rendererId() { return "example:energy_orb"; }
        };
        NfrTooltipApi.PreviewRenderer renderer = new NfrTooltipApi.PreviewRenderer() {
            @Override public NfrTooltipApi.PreviewKind previewKind() {
                return NfrTooltipApi.PreviewKind.CUSTOM;
            }
            @Override public String id() { return "example:energy_orb"; }
            @Override public NfrTooltipApi.PreviewSize measure(
                    NfrTooltipApi.PreviewRequest value,
                    net.minecraft.client.gui.FontRenderer font) {
                return new NfrTooltipApi.PreviewSize(24, 24);
            }
            @Override public void render(NfrTooltipApi.PreviewRequest value, int x, int y,
                                         NfrTooltipApi.PreviewSize size,
                                         net.minecraft.client.gui.FontRenderer font) { }
        };
        NfrTooltipApi.PreviewRegistry.register(renderer);
        try {
            assertTrue(NfrTooltipApi.PreviewRegistry.find(request) == renderer);
        } finally {
            NfrTooltipApi.PreviewRegistry.unregister(renderer);
        }
        assertTrue(NfrTooltipApi.PreviewRegistry.find(request) == null);
    }

    private static NfrTooltipApi.TooltipDocument document() {
        return NfrTooltipApi.TooltipDocument.builder(null, Collections.emptyList()).build();
    }
}
