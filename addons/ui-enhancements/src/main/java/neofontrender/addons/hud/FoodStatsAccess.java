package neofontrender.addons.hud;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

/** Abstracts optional food-system data so HUD calculations do not link directly to AppleCore. */
interface FoodStatsAccess {
    /** Returns the player's current exhaustion contribution. */
    float exhaustion(EntityPlayer player);

    /** Returns the exhaustion threshold corresponding to a full depletion indicator. */
    float maximumExhaustion(EntityPlayer player);

    /** Returns the held stack's player-specific hunger and saturation restoration. */
    FoodPreview preview(ItemStack stack, EntityPlayer player);
}
