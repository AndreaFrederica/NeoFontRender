package neofontrender.addons.mixin;
import net.minecraft.client.audio.*;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(SoundManager.class)
public interface AccessorAudioChannels {
    @Accessor("invPlayingSounds") Map<ISound, String> uie$channels();
}
