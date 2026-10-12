package neofontrender.addons.loading;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class WorldLoadingProgressTest {
    @Test
    void waitingPulseStaysSmallWithoutChunks() {
        WorldLoadingProgress progress = new WorldLoadingProgress();
        progress.reset(0L);
        float value = progress.update(0, 8, 0L, 5_000_000_000L);
        assertTrue(value >= 0.02F);
        assertTrue(value <= 0.08F);
    }

    @Test
    void receivedChunkReadinessIsMonotonicAndReservedBelowCompletion() {
        WorldLoadingProgress progress = new WorldLoadingProgress();
        progress.reset(0L);
        float previous = 0.0F;
        float value = 0.0F;
        for (int frame = 1; frame <= 24; frame++) {
            value = progress.update(289, 8, 0L, frame * 250_000_000L);
            assertTrue(value >= previous);
            previous = value;
        }
        assertTrue(value > 0.90F);
        assertTrue(value <= 0.97F);
    }

    @Test
    void integratedServerUsesTheMostAdvancedRealSignal() {
        assertEquals(0.5F, WorldLoadingRenderer.integratedPreparationProgress(312, 50, -1), 0.002F);
        assertEquals(0.8F, WorldLoadingRenderer.integratedPreparationProgress(100, 80, -1), 0.001F);
        assertEquals(1.0F, WorldLoadingRenderer.integratedPreparationProgress(625, 96, -1), 0.001F);
    }

    @Test
    void serverAndClientPhasesReserveCompletionForTheLiveWorld() {
        assertEquals(0.08F, WorldLoadingRenderer.serverPhaseProgress(0.0F), 0.001F);
        assertEquals(0.88F, WorldLoadingRenderer.serverPhaseProgress(1.0F), 0.001F);
        assertEquals(0.92F, WorldLoadingRenderer.clientPhaseProgress(0.0F), 0.001F);
        assertEquals(0.99F, WorldLoadingRenderer.clientPhaseProgress(1.0F), 0.001F);
        assertEquals(0.12F, WorldLoadingRenderer.multiplayerClientPhaseProgress(0.0F), 0.001F);
        assertEquals(0.97F, WorldLoadingRenderer.multiplayerClientPhaseProgress(1.0F), 0.001F);
    }

    @Test
    void rendererPreparationUsesRealRenderChunkCounts() {
        assertEquals(0.88F,
                WorldLoadingRenderer.rendererPreparationProgress(true, 0, 1600), 0.001F);
        assertEquals(0.90F,
                WorldLoadingRenderer.rendererPreparationProgress(true, 800, 1600), 0.001F);
        assertEquals(0.92F,
                WorldLoadingRenderer.rendererPreparationProgress(true, 1600, 1600), 0.001F);
        assertEquals(0.02F,
                WorldLoadingRenderer.rendererPreparationProgress(false, 0, 1600), 0.001F);
        assertEquals(0.12F,
                WorldLoadingRenderer.rendererPreparationProgress(false, 1600, 1600), 0.001F);
    }

    @Test
    void exactSpawnCounterIgnoresStaleLoadingScreenAndServerValues() {
        assertEquals(1.0F / 625.0F,
                WorldLoadingRenderer.authoritativeSpawnProgress(true, 1, 400, 100),
                0.0001F);
        assertEquals(0.5F,
                WorldLoadingRenderer.authoritativeSpawnProgress(false, -1, 312, 50),
                0.002F);
    }

    @Test
    void loadingYieldsItsFramebufferToAnyForeignPromptScreen() {
        assertTrue(WorldLoadingRenderer.shouldPresentLoadingScreen(true, false, true));
        assertTrue(WorldLoadingRenderer.shouldPresentLoadingScreen(false, true, true));
        assertFalse(WorldLoadingRenderer.shouldPresentLoadingScreen(true, false, false));
        assertFalse(WorldLoadingRenderer.shouldPresentLoadingScreen(false, true, false));
    }

    @Test
    void acceptedForgePromptKeepsOnlyItsSyntheticMainMenuInTheLoadingPresentation() {
        assertTrue(WorldLoadingRenderer.shouldOwnAcceptedPromptReturn(true, true, true));
        assertFalse(WorldLoadingRenderer.shouldOwnAcceptedPromptReturn(false, true, true));
        assertFalse(WorldLoadingRenderer.shouldOwnAcceptedPromptReturn(true, false, true));
        assertFalse(WorldLoadingRenderer.shouldOwnAcceptedPromptReturn(true, true, false));
    }
}
