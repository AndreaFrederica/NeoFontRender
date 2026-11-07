package neofontrender.client.integration;

import com.cleanroommc.modularui.api.text.MuiTextBackend;
import neofontrender.api.text.ModernTextApi;
import neofontrender.core.config.NeofontrenderConfig;

/** Supplies MUI with the same fractional advances used by NFR's draw path. */
public final class NfrMuiTextBackend implements MuiTextBackend {

    @Override
    public boolean isAvailable() {
        return ModernTextApi.isAvailable();
    }

    @Override
    public float measure(String text, float scale, int color, boolean shadow) {
        return ModernTextApi.measureFormatted(text, NeofontrenderConfig.fontSize(), color, shadow) * scale;
    }
}
