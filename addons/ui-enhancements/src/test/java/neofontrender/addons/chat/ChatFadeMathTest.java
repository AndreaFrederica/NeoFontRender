package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatFadeMathTest {
    @Test
    void followsVanillaFadeWindowAndCurve() {
        assertEquals(1.0F, ChatFadeMath.lineFade(100, 100, 200), 0.0001F);
        assertEquals(1.0F, ChatFadeMath.lineFade(280, 100, 200), 0.0001F);
        assertEquals(0.25F, ChatFadeMath.lineFade(290, 100, 200), 0.0001F);
        assertEquals(0.0F, ChatFadeMath.lineFade(300, 100, 200), 0.0001F);
    }

    @Test
    void handlesInvalidDurationAndFutureCounters() {
        assertEquals(0.0F, ChatFadeMath.lineFade(100, 100, 0), 0.0001F);
        assertEquals(1.0F, ChatFadeMath.lineFade(90, 100, 200), 0.0001F);
    }

    @Test
    void combinesMinecraftOpacityWithPerLineFade() {
        assertEquals(64, ChatFadeMath.lineOpacity(0.5F, 0.5F));
        assertEquals(255, ChatFadeMath.lineOpacity(2.0F, 2.0F));
        assertEquals(0, ChatFadeMath.lineOpacity(-1.0F, 1.0F));
    }
}
