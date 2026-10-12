package neofontrender.addons.audio;

import net.minecraft.client.audio.ISound;
import net.minecraft.util.ResourceLocation;

/**
 * Minimal positioned sound carrying per-call volume, pitch, repeat and attenuation.
 *
 * <p>1.7.10 resolves sound events by name and only exposes the 10-argument
 * {@code PositionedSoundRecord} constructor as private, so UIE supplies its own {@link ISound}
 * whenever a caller needs values the vanilla record does not take (volume, pitch, looping).
 */
final class UiePositionedSound implements ISound {
    private final ResourceLocation location;
    private final float volume;
    private final float pitch;
    private final boolean repeat;
    private final ISound.AttenuationType attenuation;
    private final float x, y, z;

    UiePositionedSound(ResourceLocation location, float volume, float pitch, boolean repeat,
            float x, float y, float z, boolean positional) {
        this.location = location;
        this.volume = volume;
        this.pitch = pitch;
        this.repeat = repeat;
        this.attenuation = positional ? ISound.AttenuationType.LINEAR : ISound.AttenuationType.NONE;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public ResourceLocation getPositionedSoundLocation() { return location; }
    public boolean canRepeat() { return repeat; }
    public int getRepeatDelay() { return 0; }
    public float getVolume() { return volume; }
    public float getPitch() { return pitch; }
    public float getXPosF() { return x; }
    public float getYPosF() { return y; }
    public float getZPosF() { return z; }
    public ISound.AttenuationType getAttenuationType() { return attenuation; }
}
