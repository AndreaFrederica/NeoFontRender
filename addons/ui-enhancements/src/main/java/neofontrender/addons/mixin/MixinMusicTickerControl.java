package neofontrender.addons.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MusicTicker;
import neofontrender.addons.audio.UieAudio;
import neofontrender.addons.audio.VanillaMusic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MusicTicker.class)
public abstract class MixinMusicTickerControl implements VanillaMusic.Ticker {
    // 1.7.10 leaves the ticker's schedule state unmapped (SRG field_147678_c / field_147676_d).
    @Shadow private ISound field_147678_c;
    @Shadow private int field_147676_d;

    public ISound uie$current() { return field_147678_c; }
    public void uie$current(ISound value) { field_147678_c = value; }
    public void uie$delay(int value) { field_147676_d = value; }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void uie$tick(CallbackInfo ci) { if (UieAudio.vanillaMusic().beforeTick(this)) ci.cancel(); }

    // 1.7.10 has no playMusic entry point: update() picks and starts the track inline, so the
    // intercept sits on the record it would start. The scene is read back from Minecraft, which
    // is the same value update() itself resolved before this point in the same tick.
    @Inject(method = "update", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/audio/PositionedSoundRecord;func_147673_a(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/client/audio/PositionedSoundRecord;"),
            cancellable = true)
    private void uie$play(CallbackInfo ci) {
        if (UieAudio.vanillaMusic().play(this, Minecraft.getMinecraft().func_147109_W())) ci.cancel();
    }
}
