package neofontrender.addons.hud;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.FoodStats;
import net.minecraftforge.common.ForgeHooks;
import neofontrender.addons.hud.api.HudBarElement;
import neofontrender.addons.hud.api.HudBarProvider;
import neofontrender.addons.hud.api.HudBarRegistry;
import neofontrender.addons.hud.api.HudBarSide;
import neofontrender.addons.hud.api.HudBarValue;
import neofontrender.api.arc3d.Arc3DApi;

/** Registers the status sources corresponding to Forge's vanilla HUD elements. */
final class VanillaHudBarProviders {
    private VanillaHudBarProviders() {}

    static void register(FoodStatsAccess foodStatsAccess) {
        if (foodStatsAccess == null) throw new IllegalArgumentException("food stats access must not be null");
        HudBarRegistry.register(provider("health", HudBarElement.HEALTH, HudBarSide.LEFT, 100, true,
                player -> HudBarsConfig.health,
                (player, partialTicks) -> health(player)));
        HudBarRegistry.register(provider("absorption", HudBarElement.HEALTH, HudBarSide.LEFT, 110, false,
                player -> HudBarsConfig.absorption && player.getAbsorptionAmount() > 0.0F,
                (player, partialTicks) -> value(player.getAbsorptionAmount(),
                        Math.max(player.getMaxHealth(), player.getAbsorptionAmount()),
                        HudBarsConfig.absorptionColor)));
        HudBarRegistry.register(provider("armor", HudBarElement.ARMOR, HudBarSide.LEFT, 100, true,
                player -> HudBarsConfig.armor && armor(player) > 0.0F,
                (player, partialTicks) -> {
                    float amount = armor(player);
                    float maximum = Math.max(20.0F, (float) Math.ceil(amount / 20.0F) * 20.0F);
                    return value(amount, maximum, HudBarsConfig.armorColor);
                }));
        HudBarRegistry.register(provider("food", HudBarElement.FOOD, HudBarSide.RIGHT, 100, true,
                player -> HudBarsConfig.food,
                (player, partialTicks) -> food(player, foodStatsAccess)));
        HudBarRegistry.register(provider("air", HudBarElement.AIR, HudBarSide.RIGHT, 100, true,
                player -> HudBarsConfig.air && player.getAir() < 300,
                (player, partialTicks) -> value(player.getAir(), 300.0F, HudBarsConfig.airColor)));
        HudBarRegistry.register(provider("mount_health", HudBarElement.MOUNT_HEALTH, HudBarSide.RIGHT, 100, true,
                player -> HudBarsConfig.mountHealth && player.ridingEntity instanceof EntityLivingBase,
                (player, partialTicks) -> {
                    EntityLivingBase mount = (EntityLivingBase) player.ridingEntity;
                    return value(mount.getHealth(), mount.getMaxHealth(), HudBarsConfig.mountColor);
                }));
    }

    private static HudBarValue health(EntityPlayer player) {
        float maximum = player.getMaxHealth();
        float ratio = maximum <= 0.0F ? 0.0F : player.getHealth() / maximum;
        int color = Arc3DApi.lerpArgb(HudBarsConfig.healthColor, HudBarsConfig.healthyColor, ratio);
        if (player.isPotionActive(Potion.poison)) color = 0xFF4A9C38;
        else if (player.isPotionActive(Potion.wither)) color = 0xFF5A5A5A;
        return value(player.getHealth(), maximum, color);
    }

    private static HudBarValue food(EntityPlayer player, FoodStatsAccess foodStatsAccess) {
        FoodStats stats = player.getFoodStats();
        float food = stats.getFoodLevel();
        int primary = player.isPotionActive(Potion.hunger) ? 0xFF579A42 : HudBarsConfig.foodColor;
        return FoodBarValues.create(food, stats.getSaturationLevel(), player.getHeldItem(), player, foodStatsAccess,
                primary, HudBarsConfig.saturationColor, withAlpha(primary, 150), 0x90FFFFFF,
                text(food, 20.0F));
    }

    private static float armor(EntityPlayer player) {
        return ForgeHooks.getTotalArmorValue(player);
    }

    private static HudBarValue value(float current, float maximum, int color) {
        return new HudBarValue(current, maximum, color, text(current, maximum));
    }

    private static String text(float current, float maximum) {
        if (!HudBarsConfig.showNumbers) return "";
        return Math.round(current) + "/" + Math.round(maximum);
    }

    private static int withAlpha(int color, int alpha) {
        return color & 0x00FFFFFF | Math.max(0, Math.min(255, alpha)) << 24;
    }

    /** Evaluates whether a provider should reserve a bar for a player. */
    private interface Visibility {
        boolean test(EntityPlayer player);
    }

    /** Produces one immutable frame sample for a provider. */
    private interface Sampler {
        HudBarValue sample(EntityPlayer player, float partialTicks);
    }

    private static HudBarProvider provider(String name, HudBarElement element, HudBarSide side, int order,
                                           boolean replaces, Visibility visible, Sampler sampler) {
        return new HudBarProvider() {
            @Override public String id() { return "neofontrender_ui_enhancements:" + name; }
            @Override public HudBarElement element() { return element; }
            @Override public HudBarSide side() { return side; }
            @Override public int order() { return order; }
            @Override public boolean replacesVanilla() { return replaces; }
            @Override public boolean shouldRender(EntityPlayer player) { return visible.test(player); }
            @Override public HudBarValue sample(EntityPlayer player, float partialTicks) {
                return sampler.sample(player, partialTicks);
            }
        };
    }
}
