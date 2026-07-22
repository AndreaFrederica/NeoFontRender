package neofontrender.addons.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatAnimationControllerTest {
    @Test
    void easingFunctionsKeepStableEndpoints() {
        for (String easing : new String[]{"linear", "sine", "quad", "cubic", "back"}) {
            assertEquals(0.0F, ChatAnimationController.ease(0.0F, easing), 0.0001F, easing);
            assertEquals(1.0F, ChatAnimationController.ease(1.0F, easing), 0.0001F, easing);
        }
    }

    @Test
    void backEasingProvidesAControlledOvershoot() {
        assertTrue(ChatAnimationController.ease(0.7F, "back") > 1.0F);
    }

    @Test
    void unknownEasingFallsBackToSine() {
        assertEquals(ChatAnimationController.ease(0.5F, "sine"),
                ChatAnimationController.ease(0.5F, "unknown"), 0.0001F);
    }
}
