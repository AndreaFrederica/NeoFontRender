package neofontrender.addons.server;

import net.minecraftforge.common.config.Configuration;
import neofontrender.addons.flight.network.FlightRollNetwork;

import java.io.File;

final class FlightRollServerConfig {
    private FlightRollServerConfig() {}

    static void load(File configDirectory) {
        Configuration config = new Configuration(
                new File(configDirectory, "revo-ui-flight-roll-server.cfg"));
        config.load();
        boolean enabled = config.getBoolean("enabled", "flightRoll", true,
                "Allow UIE flight-roll clients on this server.");
        boolean sync = config.getBoolean("syncRemotePlayers", "flightRoll", true,
                "Relay validated roll values to nearby compatible clients.");
        float maximum = config.getFloat("maximumRollSpeed", "flightRoll", 180.0F,
                30.0F, 720.0F,
                "Maximum momentum-mode roll speed advertised to clients, in degrees per second.");
        FlightRollNetwork.configureServer(enabled, sync, maximum);
        if (config.hasChanged()) config.save();
    }
}
