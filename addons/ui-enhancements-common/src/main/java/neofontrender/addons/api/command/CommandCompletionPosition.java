package neofontrender.addons.api.command;

/** Version-neutral copy of the command completion target position. */
public final class CommandCompletionPosition {
    private final int x;
    private final int y;
    private final int z;

    public CommandCompletionPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
}
