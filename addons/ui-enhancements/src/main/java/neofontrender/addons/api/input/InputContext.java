package neofontrender.addons.api.input;

import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Immutable routing rule set for one input mode such as free-look, flight, or drone. */
public final class InputContext {
    private final ResourceLocation id;
    private final int priority;
    private final Set<InputAction> claimed;
    private final Set<InputAction> blocked;

    private InputContext(ResourceLocation id, int priority, Set<InputAction> claimed,
                         Set<InputAction> blocked) {
        this.id = Objects.requireNonNull(id, "id");
        this.priority = priority;
        this.claimed = immutable(claimed);
        this.blocked = immutable(blocked);
        if (!Collections.disjoint(this.claimed, this.blocked)) {
            throw new IllegalArgumentException("An input action cannot be claimed and blocked");
        }
    }

    public ResourceLocation getId() { return id; }
    public int getPriority() { return priority; }
    public Set<InputAction> getClaimed() { return claimed; }
    public Set<InputAction> getBlocked() { return blocked; }

    public InputDisposition disposition(InputAction action) {
        if (blocked.contains(action)) return InputDisposition.BLOCK;
        return claimed.contains(action) ? InputDisposition.CLAIM : InputDisposition.PASS;
    }

    public static Builder builder(ResourceLocation id, int priority) {
        return new Builder(id, priority);
    }

    private static Set<InputAction> immutable(Set<InputAction> values) {
        if (values == null || values.isEmpty()) return Collections.emptySet();
        return Collections.unmodifiableSet(EnumSet.copyOf(values));
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final int priority;
        private final EnumSet<InputAction> claimed = EnumSet.noneOf(InputAction.class);
        private final EnumSet<InputAction> blocked = EnumSet.noneOf(InputAction.class);

        private Builder(ResourceLocation id, int priority) {
            this.id = Objects.requireNonNull(id, "id");
            this.priority = priority;
        }

        public Builder claim(InputAction... actions) {
            add(claimed, actions);
            return this;
        }

        public Builder block(InputAction... actions) {
            add(blocked, actions);
            return this;
        }

        public InputContext build() { return new InputContext(id, priority, claimed, blocked); }

        private static void add(EnumSet<InputAction> target, InputAction[] actions) {
            Objects.requireNonNull(actions, "actions");
            for (InputAction action : actions) target.add(Objects.requireNonNull(action, "action"));
        }
    }
}
