package neofontrender.addons.flight.network;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlightRollNetworkProtocolTest {
    @Test
    void packetDiscriminatorsAreUniqueAcrossBothDirections() {
        Set<Integer> ids = new HashSet<>();
        ids.add(FlightRollNetwork.HANDSHAKE_REQUEST_ID);
        ids.add(FlightRollNetwork.ROLL_UPDATE_ID);
        ids.add(FlightRollNetwork.HANDSHAKE_RESPONSE_ID);
        ids.add(FlightRollNetwork.REMOTE_ROLL_ID);

        assertEquals(4, ids.size(),
                "SimpleNetworkWrapper shares one discriminator table across both directions");
        assertEquals(2, FlightRollNetwork.PROTOCOL_VERSION);
    }
}
