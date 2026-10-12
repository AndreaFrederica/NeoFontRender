package neofontrender.addons.hud;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.util.FoodStats;
import net.minecraftforge.common.ForgeHooks;
import neofontrender.addons.hud.api.HudBarElement;
import neofontrender.addons.hud.api.HudBarProvider;
import neofontrender.addons.hud.api.HudBarRegistry;
import neofontrender.addons.hud.api.HudBarSide;
import neofontrender.addons.hud.api.HudBarValue;
import neofontrender.api.arc3d.Arc3DApi;

final class VanillaHudBarProviders {
    private VanillaHudBarProviders() {}

    static void register() {
        HudBarRegistry.register(provider("health", HudBarElement.HEALTH, HudBarSide.LEFT, 100, true,
                player -> HudBarsConfig.health,
                (player, partialTicks) -> {
                    float max = player.getMaxHealth();
                    float ratio = max <= 0 ? 0 : player.getHealth() / max;
                    int color = lerp(HudBarsConfig.healthColor, HudBarsConfig.healthyColor, ratio);
                    if (player.isPotionActive(Potion.poison)) color = 0xFF4A9C38;
                    else if (player.isPotionActive(Potion.wither)) color = 0xFF5A5A5A;
                    return value(player.getHealth(), max, color);
                }));
        HudBarRegistry.register(provider("absorption", HudBarElement.HEALTH, HudBarSide.LEFT, 110, false,
                player -> HudBarsConfig.absorption && player.getAbsorptionAmount() > 0.0F,
                (player, partialTicks) -> value(player.getAbsorptionAmount(),
                        Math.max(player.getMaxHealth(), player.getAbsorptionAmount()), HudBarsConfig.absorptionColor)));
        HudBarRegistry.register(provider("armor", HudBarElement.ARMOR, HudBarSide.LEFT, 100, true,
                player -> HudBarsConfig.armor && armor(player) > 0.0F,
                (player, partialTicks) -> {
                    float amount = armor(player);
                    float maximum = Math.max(20.0F, (float) Math.ceil(amount / 20.0F) * 20.0F);
                    return value(amount, maximum, HudBarsConfig.armorColor);
                }));
        HudBarRegistry.register(provider("food", HudBarElement.FOOD, HudBarSide.RIGHT, 100, true,
                player -> HudBarsConfig.food,
                (player, partialTicks) -> food(player)));
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

    private static HudBarValue food(EntityPlayer player) {
        FoodStats stats = player.getFoodStats();
        float food = stats.getFoodLevel();
        float maximum = AppleCoreCompat.maximumHunger(player);
        float saturation = stats.getSaturationLevel();
        FoodBarPreview preview = foodPreview(player.getHeldItem(), player, food, maximum, saturation);
        // FoodStats exposes no public exhaustion accessor before 1.12.2 and the SRG field name
        // cannot be resolved from this workspace, so vanilla exhaustion stays at zero; AppleCore
        // supplies the real value when it is installed.
        float exhaustion = AppleCoreCompat.exhaustion(player, 0.0F);
        float maximumExhaustion = AppleCoreCompat.maximumExhaustion(player);
        int primary = player.isPotionActive(Potion.hunger) ? 0xFF579A42 : HudBarsConfig.foodColor;
        float depletion = Math.min(maximumExhaustion, exhaustion) / maximumExhaustion * maximum;
        float hungerPreview = preview == null ? 0.0F : preview.hunger;
        float saturationPreview = preview == null ? 0.0F : preview.saturation;
        return new HudBarValue(food, maximum, saturation, hungerPreview, saturationPreview, depletion,
                primary, HudBarsConfig.saturationColor, primary, 0x90FFFFFF,
                text(food, maximum), preview != null);
    }

    private static FoodBarPreview foodPreview(ItemStack stack, EntityPlayer player, float food,
                                              float maximum, float saturation) {
        AppleCoreCompat.FoodValue appleCore = AppleCoreCompat.foodValue(stack, player);
        if (appleCore != null)
            return FoodBarPreview.calculate(food, maximum, saturation,
                    appleCore.hunger, appleCore.saturationModifier);
        if (stack != null && stack.getItem() instanceof ItemFood) {
            ItemFood item = (ItemFood) stack.getItem();
            return FoodBarPreview.calculate(food, maximum, saturation,
                    item.func_150905_g(stack), item.func_150906_h(stack));
        }
        return null;
    }

    private static float armor(EntityPlayer player) {
        // Forge's hook already folds ISpecialArmor contributions into the displayed value.
        return (float) ForgeHooks.getTotalArmorValue(player);
    }

    private static HudBarValue value(float current, float maximum, int color) {
        return new HudBarValue(current, maximum, color, text(current, maximum));
    }

    private static String text(float current, float maximum) {
        if (!HudBarsConfig.showNumbers) return "";
        return Math.round(current) + "/" + Math.round(maximum);
    }

    private static int lerp(int from, int to, float amount) {
        return Arc3DApi.lerpArgb(from, to, amount);
    }

    private interface Visibility { boolean test(EntityPlayer player); }
    private interface Sampler { HudBarValue sample(EntityPlayer player, float partialTicks); }

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
