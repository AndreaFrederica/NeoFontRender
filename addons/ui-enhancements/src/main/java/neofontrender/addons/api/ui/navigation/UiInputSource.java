package neofontrender.addons.api.ui.navigation;

import net.minecraft.util.ResourceLocation;

import java.util.Objects;

public final class UiInputSource {
    private final ResourceLocation owner;
    private final UiInputModality modality;

    public UiInputSource(ResourceLocation owner, UiInputModality modality) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.modality = Objects.requireNonNull(modality, "modality");
    }

    public ResourceLocation owner() { return owner; }
    public UiInputModality modality() { return modality; }

    @Override public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof UiInputSource)) return false;
        UiInputSource other = (UiInputSource) object;
        return owner.equals(other.owner) && modality == other.modality;
    }

    @Override public int hashCode() { return 31 * owner.hashCode() + modality.hashCode(); }
    @Override public String toString() { return owner + "[" + modality + "]"; }
}
