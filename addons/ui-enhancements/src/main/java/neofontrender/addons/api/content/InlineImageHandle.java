package neofontrender.addons.api.content;

import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.net.URI;

/** Read-only asynchronous image state returned by {@link InlineImages}. */
public interface InlineImageHandle {
    enum State { LOADING, READY, FAILED }

    URI uri();

    State state();

    int pixelWidth();

    int pixelHeight();

    @Nullable ResourceLocation texture();
}
