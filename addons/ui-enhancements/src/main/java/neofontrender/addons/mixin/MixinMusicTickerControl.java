package neofontrender.addons.mixin;
import net.minecraft.client.audio.*;
import neofontrender.addons.audio.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MusicTicker.class)
public abstract class MixinMusicTickerControl implements VanillaMusic.Ticker {
    @Shadow private ISound currentMusic;
    @Shadow private int timeUntilNextMusic;
    public ISound uie$current() { return currentMusic; }
    public void uie$current(ISound value) { currentMusic = value; }
    public void uie$delay(int value) { timeUntilNextMusic = value; }
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void uie$tick(CallbackInfo ci) { if (UieAudio.vanillaMusic().beforeTick(this)) ci.cancel(); }
    @Inject(method = "playMusic", at = @At("HEAD"), cancellable = true)
    private void uie$play(MusicTicker.MusicType type, CallbackInfo ci) {
        if (UieAudio.vanillaMusic().play(this, type)) ci.cancel();
    }
}
