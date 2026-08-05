package neofontrender.addons.api.input;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Posted on the Forge event bus after Minecraft polls relative mouse movement and before it
 * applies the sensitivity curve or calls {@code EntityPlayerSP.turn(float, float)}.
 *
 * <p>Handlers may replace either raw axis. Cancelling the event consumes both axes for the
 * current rendered frame. The event is client-side and is posted only while the game owns the
 * mouse cursor.</p>
 */
@Cancelable
public final class CameraMouseInputEvent extends Event {
    private final EntityPlayerSP player;
    private final float partialTicks;
    private final int originalDeltaX;
    private final int originalDeltaY;
    private int deltaX;
    private int deltaY;

    public CameraMouseInputEvent(EntityPlayerSP player, float partialTicks, int deltaX, int deltaY) {
        this.player = player;
        this.partialTicks = partialTicks;
        this.originalDeltaX = deltaX;
        this.originalDeltaY = deltaY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public EntityPlayerSP getPlayer() { return player; }
    public float getPartialTicks() { return partialTicks; }
    public int getOriginalDeltaX() { return originalDeltaX; }
    public int getOriginalDeltaY() { return originalDeltaY; }
    public int getDeltaX() { return deltaX; }
    public int getDeltaY() { return deltaY; }
    public void setDeltaX(int deltaX) { this.deltaX = deltaX; }
    public void setDeltaY(int deltaY) { this.deltaY = deltaY; }
    public void consumeHorizontal() { this.deltaX = 0; }
    public void consumeVertical() { this.deltaY = 0; }
}
