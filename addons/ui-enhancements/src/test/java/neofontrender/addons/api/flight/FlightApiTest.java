package neofontrender.addons.api.flight;

import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightApiTest {
    @Test
    void capabilityPriorityAndRegistrationLifetimeAreDeterministic() {
        FlightRegistration low = FlightApi.registerCapabilityProvider(
                new ResourceLocation("test", "low"), 0,
                (player, capability, builtIn) -> FlightDecision.ALLOW);
        FlightRegistration high = FlightApi.registerCapabilityProvider(
                new ResourceLocation("test", "high"), 100,
                (player, capability, builtIn) -> FlightDecision.DENY);
        try {
            assertEquals(FlightDecision.DENY, FlightApi.queryCapability(
                    null, FlightCapability.CAMERA_ROTATION, false));
            high.close();
            assertEquals(FlightDecision.ALLOW, FlightApi.queryCapability(
                    null, FlightCapability.CAMERA_ROTATION, false));
        } finally {
            high.close();
            low.close();
        }
        assertEquals(FlightDecision.PASS, FlightApi.queryCapability(
                null, FlightCapability.CAMERA_ROTATION, false));
    }

    @Test
    void controllerContributionsAccumulateAndClamp() {
        FlightRegistration first = FlightApi.registerControlProvider(
                new ResourceLocation("test", "first"), 10, input -> input.addRoll(0.75F));
        FlightRegistration second = FlightApi.registerControlProvider(
                new ResourceLocation("test", "second"), 0, input -> input.addRoll(0.75F));
        try {
            FlightControlInput input = new FlightControlInput(null, 0.5F, 1.0D / 60.0D);
            FlightApi.collectControlInput(input);
            assertEquals(1.0F, input.getRoll());
        } finally {
            first.close();
            second.close();
        }
    }

    @Test
    void namespacedThemeRegistrationIsReversible() {
        String json = "{\"schema\":3,\"name\":\"Test\",\"elements\":["
                + "{\"id\":\"radar\",\"type\":\"test:radar\"}]}";
        FlightRegistration registration = FlightApi.registerHudTheme(
                new ResourceLocation("test", "theme"), json);
        assertTrue(FlightApi.registeredHudThemes().containsKey("test:theme"));
        registration.close();
        assertFalse(FlightApi.registeredHudThemes().containsKey("test:theme"));
    }

    @Test
    void apiV2ComponentRegistrationIsReversibleAndDiscoverable() {
        assertEquals(2, FlightApi.getApiVersion());
        ResourceLocation type = new ResourceLocation("test", "radar");
        FlightRegistration registration = FlightApi.registerHudComponent(type,
                (context, element) -> {});
        assertTrue(FlightApi.hasHudComponent("test:radar"));
        assertTrue(FlightApi.registeredHudComponentTypes().contains("test:radar"));
        registration.close();
        assertFalse(FlightApi.hasHudComponent("test:radar"));
        assertFalse(FlightApi.registeredHudComponentTypes().contains("test:radar"));
    }
}
