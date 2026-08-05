package neofontrender.addons.flight;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import neofontrender.addons.api.flight.FlightHudCrosshairMode;
import neofontrender.addons.api.flight.FlightHudPitchMode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightHudMathTest {
    @Test
    void convertsMinecraftMotionToAviationAndMetricUnits() {
        assertEquals(36.0D, FlightHudMath.speed(10.0D, "KPH"), 1.0E-6D);
        assertEquals(19.438444924D, FlightHudMath.speed(10.0D, "KNOTS"), 1.0E-6D);
        assertEquals(328.0839895D, FlightHudMath.altitude(100.0D, "FEET"), 1.0E-6D);
        assertEquals(10.0D, FlightHudMath.verticalRate(10.0D, "MPS"), 1.0E-6D);
        assertEquals(1968.503937D, FlightHudMath.verticalRate(10.0D, "FPM"), 1.0E-5D);
    }

    @Test
    void lowerSpeedReferenceUsesVanillaElytraCoefficients() {
        double base = FlightHudMath.vanillaElytraLowerSpeed(0.0F, -15.0F, 1.0F);
        double margin = FlightHudMath.vanillaElytraLowerSpeed(0.0F, -15.0F, 1.2F);

        assertTrue(Double.isFinite(base));
        assertTrue(base > 10.0D && base < 20.0D);
        assertEquals(base * 1.2D, margin, 1.0E-4D);
    }

    @Test
    void rollingDrumAdvancesContinuouslyAndWrapsAtCarryBoundaries() {
        FlightHudMath.RollingDrum speed = FlightHudMath.rollingDrum(39.5D, 1, 10);
        assertEquals(3, speed.prefix);
        assertEquals(8, speed.previous);
        assertEquals(9, speed.current);
        assertEquals(0, speed.next);
        assertEquals(0.5D, speed.progress, 1.0E-9D);

        FlightHudMath.RollingDrum altitude = FlightHudMath.rollingDrum(1285.0D, 20, 100);
        assertEquals(12, altitude.prefix);
        assertEquals(60, altitude.previous);
        assertEquals(80, altitude.current);
        assertEquals(0, altitude.next);
        assertEquals(0.25D, altitude.progress, 1.0E-9D);
    }

    @Test
    void fullSpherePitchLadderFindsTheNearestMarkerAcrossWrapBoundaries() {
        assertEquals(0.0D, FlightHudMath.pitchLadderDelta(-80.0D, 80.0D, true), 1.0E-9D);
        assertEquals(1.0D, FlightHudMath.pitchLadderDelta(-180.0D, -179.0D, true), 1.0E-9D);
        assertEquals(-359.0D, FlightHudMath.pitchLadderDelta(-180.0D, -179.0D, false), 1.0E-9D);
    }

    @Test
    void builtInThemesUseTheValidatedSchema() throws Exception {
        Gson gson = new Gson();
        String[][] cases = {
                {"airbus-a319", "AIRBUS_A319"},
                {"airbus-a350", "AIRBUS_A350"},
                {"boeing-737", "BOEING_737"},
                {"msfs-external", "MSFS_EXTERNAL"},
                {"fpv-racing", "FPV_RACING"},
                {"fpv-freestyle", "FPV_FREESTYLE"},
                {"fpv-long-range", "FPV_LONG_RANGE"},
                {"stereotype-tactical", "TACTICAL_STEREOTYPE"}
        };
        for (String[] testCase : cases) {
            String name = testCase[0];
            String path = "assets/neofontrender_ui_enhancements/flight_hud/" + name + ".json";
            try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(stream, path);
                FlightHudTheme theme = gson.fromJson(new InputStreamReader(stream,
                        StandardCharsets.UTF_8), FlightHudTheme.class);
                theme.validate(name);
                assertEquals(name, theme.id);
                assertEquals(testCase[1], theme.style);
                assertEquals(3, theme.schema);
                assertNotNull(theme.first("STATUS"));
                if (name.startsWith("airbus")) {
                    assertNotNull(theme.first("AIRSPEED_TAPE"));
                    assertNotNull(theme.first("ALTITUDE_TAPE"));
                    assertNotNull(theme.first("INPUT_STICK"));
                    assertNotNull(theme.first("HEADING_RIBBON"));
                    assertTrue(theme.first("AIRSPEED_TAPE").variant.startsWith("AIRBUS_"));
                    assertTrue(theme.first("ALTITUDE_TAPE").variant.startsWith("AIRBUS_"));
                    assertTrue(theme.first("STATUS").variant.startsWith(theme.style));
                    assertTrue(theme.first("AIRSPEED_TAPE").boxWidth >= 40.0F);
                    FlightHudTheme.Element reference = theme.first("FLIGHT_REFERENCE");
                    FlightHudTheme.Element heading = theme.first("HEADING_RIBBON");
                    float bankScaleTop = reference.y - reference.width * 0.5F * 0.72F;
                    float headingBottom = heading.y + 16.0F;
                    assertTrue(bankScaleTop - headingBottom >= 6.0F,
                            name + ": heading ribbon collides with bank scale");
                } else if (name.startsWith("boeing")) {
                    assertNotNull(theme.first("AIRSPEED_TAPE"));
                    assertNotNull(theme.first("ALTITUDE_TAPE"));
                    assertNotNull(theme.first("INPUT_STICK"));
                    assertNotNull(theme.first("HEADING_ARC"));
                    assertEquals("BOEING_DRUM", theme.first("AIRSPEED_TAPE").variant);
                } else if (name.startsWith("msfs")) {
                    assertNotNull(theme.first("AIRSPEED_TAPE"));
                    assertNotNull(theme.first("ALTITUDE_TAPE"));
                    assertNotNull(theme.first("INPUT_STICK"));
                    assertNotNull(theme.first("HEADING_DIAL"));
                    assertNotNull(theme.first("AOA_GAUGE"));
                    assertNotNull(theme.first("ENERGY_GAUGE"));
                    assertEquals(FlightHudCrosshairMode.KEEP, theme.crosshairMode,
                            "MSFS has no replacement reticle and must retain vanilla crosshair");
                    FlightHudTheme.Element speed = theme.first("AIRSPEED_TAPE");
                    FlightHudTheme.Element groundSpeed = theme.first("GROUND_SPEED");
                    float speedPanelBottom = speed.y + speed.height + 16.0F;
                    assertTrue(groundSpeed.y + 8.0F <= speedPanelBottom,
                            "MSFS ground-speed text escapes its airspeed panel");
                    FlightHudTheme.Element altitude = theme.first("ALTITUDE_TAPE");
                    float altitudePanelLeft = altitude.x
                            - Math.max(36.0F, altitude.boxWidth);
                    assertTrue(altitudePanelLeft <= altitude.x - 30.0F,
                            "MSFS altitude panel does not contain its left labels");
                } else if (name.startsWith("fpv")) {
                    assertEquals(FlightHudCrosshairMode.HIDE_VANILLA, theme.crosshairMode);
                    assertNotNull(theme.first("FLIGHT_REFERENCE"));
                    assertEquals(FlightHudPitchMode.WRAP_360,
                            theme.first("FLIGHT_REFERENCE").pitchMode);
                    assertNotNull(theme.first("HEADING_RIBBON"));
                    if (!name.endsWith("freestyle")) {
                        assertNotNull(theme.first("AIRSPEED_TAPE"));
                        assertNotNull(theme.first("ALTITUDE_TAPE"));
                    }
                } else {
                    assertEquals(FlightHudCrosshairMode.HIDE_VANILLA, theme.crosshairMode);
                    assertNotNull(theme.first("HEADING_ARC"));
                    assertNotNull(theme.first("FLIGHT_REFERENCE"));
                }
                for (String color : new String[] {
                        "primary", "flightPath", "selected", "warning", "halo"
                }) assertTrue(theme.colors.containsKey(color), name + ": " + color);
            }
        }
    }

    @Test
    void expandedAirlinerThemesPatchTheBaseWithFullSpherePitchCoverage() throws Exception {
        String[][] cases = {
                {"airbus-a319", "airbus-a319-360"},
                {"airbus-a350", "airbus-a350-360"},
                {"boeing-737", "boeing-737-360"}
        };
        Gson gson = new Gson();
        for (String[] testCase : cases) {
            JsonObject parent = resourceObject(gson, testCase[0]);
            JsonObject child = resourceObject(gson, testCase[1]);
            FlightHudThemeManager.merge(parent, child);
            parent.remove("extends");
            FlightHudTheme theme = gson.fromJson(parent, FlightHudTheme.class);
            theme.validate(testCase[1]);
            assertEquals(testCase[1], theme.id);
            assertEquals(FlightHudPitchMode.WRAP_360,
                    theme.first("FLIGHT_REFERENCE").pitchMode);
            assertTrue(theme.first("FLIGHT_REFERENCE").pitchRange >= 30);
        }
    }

    private JsonObject resourceObject(Gson gson, String name) throws Exception {
        String path = "assets/neofontrender_ui_enhancements/flight_hud/" + name + ".json";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return gson.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8),
                    JsonObject.class);
        }
    }

    @Test
    void schemaAcceptsNamespacedExtensionElementsAndPreservesData() {
        String json = "{\"schema\":3,\"id\":\"test:theme\",\"elements\":["
                + "{\"id\":\"radar\",\"type\":\"test:radar\",\"x\":12,"
                + "\"data\":{\"range\":128}}]}";
        FlightHudTheme theme = new Gson().fromJson(json, FlightHudTheme.class);
        theme.validate("fallback");

        FlightHudTheme.Element element = theme.first("test:radar");
        assertNotNull(element);
        assertEquals("test:theme", theme.id);
        assertEquals(128, theme.publicElement(element).getData().get("range").getAsInt());
    }
}
