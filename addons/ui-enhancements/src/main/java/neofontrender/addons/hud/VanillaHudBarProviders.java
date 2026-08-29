package neofontrender.addons.hud;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.MathUtil;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;
import net.minecraftforge.common.ISpecialArmor;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import neofontrender.addons.hud.api.HudBarElement;
import neofontrender.addons.hud.api.HudBarProvider;
import neofontrender.addons.hud.api.HudBarRegistry;
import neofontrender.addons.hud.api.HudBarSide;
import neofontrender.addons.hud.api.HudBarValue;

final class VanillaHudBarProviders {
    private VanillaHudBarProviders() {}

    static void register() {
        HudBarRegistry.register(provider("health", HudBarElement.HEALTH, HudBarSide.LEFT, 100, true,
                player -> HudBarsConfig.health,
                (player, partialTicks) -> {
                    float max = player.getMaxHealth();
                    float ratio = max <= 0 ? 0 : player.getHealth() / max;
                    int color = lerp(HudBarsConfig.healthColor, HudBarsConfig.healthyColor, ratio);
                    if (player.isPotionActive(MobEffects.POISON)) color = 0xFF4A9C38;
                    else if (player.isPotionActive(MobEffects.WITHER)) color = 0xFF5A5A5A;
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
        HudBarRegistry.register(provider("toughness", HudBarElement.ARMOR, HudBarSide.LEFT, 110, false,
                player -> HudBarsConfig.toughness && toughness(player) > 0.0F,
                (player, partialTicks) -> value(toughness(player), Math.max(20.0F, toughness(player)),
                        HudBarsConfig.toughnessColor)));
        HudBarRegistry.register(provider("food", HudBarElement.FOOD, HudBarSide.RIGHT, 100, true,
                player -> HudBarsConfig.food,
                (player, partialTicks) -> food(player)));
        HudBarRegistry.register(provider("air", HudBarElement.AIR, HudBarSide.RIGHT, 100, true,
                player -> HudBarsConfig.air && player.getAir() < 300,
                (player, partialTicks) -> value(player.getAir(), 300.0F, HudBarsConfig.airColor)));
        HudBarRegistry.register(provider("mount_health", HudBarElement.MOUNT_HEALTH, HudBarSide.RIGHT, 100, true,
                player -> HudBarsConfig.mountHealth && player.getRidingEntity() instanceof EntityLivingBase,
                (player, partialTicks) -> {
                    EntityLivingBase mount = (EntityLivingBase) player.getRidingEntity();
                    return value(mount.getHealth(), mount.getMaxHealth(), HudBarsConfig.mountColor);
                }));
    }

    private static HudBarValue food(EntityPlayer player) {
        FoodStats stats = player.getFoodStats();
        float food = stats.getFoodLevel();
        float maximum = AppleCoreCompat.maximumHunger(player);
        float saturation = stats.getSaturationLevel();
        FoodBarPreview preview = foodPreview(player.getHeldItemMainhand(), player, food, maximum, saturation);
        if (preview == null)
            preview = foodPreview(player.getHeldItemOffhand(), player, food, maximum, saturation);
        float exhaustion;
        try {
            exhaustion = ObfuscationReflectionHelper.getPrivateValue(FoodStats.class, stats, "field_75126_c");
        } catch (RuntimeException ignored) {
            exhaustion = 0.0F;
        }
        exhaustion = AppleCoreCompat.exhaustion(player, exhaustion);
        float maximumExhaustion = AppleCoreCompat.maximumExhaustion(player);
        int primary = player.isPotionActive(MobEffects.HUNGER) ? 0xFF579A42 : HudBarsConfig.foodColor;
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
        if (!stack.isEmpty() && stack.getItem() instanceof ItemFood) {
            ItemFood item = (ItemFood) stack.getItem();
            return FoodBarPreview.calculate(food, maximum, saturation,
                    item.getHealAmount(stack), item.getSaturationModifier(stack));
        }
        return null;
    }

    private static float armor(EntityPlayer player) {
        float value = player.getTotalArmorValue();
        for (ItemStack stack : player.getArmorInventoryList()) {
            if (stack.getItem() instanceof ISpecialArmor) {
                value += ((ISpecialArmor) stack.getItem()).getArmorDisplay(player, stack, 0);
            }
        }
        return value;
    }

    private static float toughness(EntityPlayer player) {
        return (float) player.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();
    }

    private static HudBarValue value(float current, float maximum, int color) {
        return new HudBarValue(current, maximum, color, text(current, maximum));
    }

    private static String text(float current, float maximum) {
        if (!HudBarsConfig.showNumbers) return "";
        return Math.round(current) + "/" + Math.round(maximum);
    }

    private static int lerp(int from, int to, float amount) {
        amount = Math.max(0.0F, Math.min(1.0F, amount));
        return (Math.round(MathUtil.lerp(Color.alpha(from), Color.alpha(to), amount)) << 24)
                | (Math.round(MathUtil.lerp(Color.red(from), Color.red(to), amount)) << 16)
                | (Math.round(MathUtil.lerp(Color.green(from), Color.green(to), amount)) << 8)
                | Math.round(MathUtil.lerp(Color.blue(from), Color.blue(to), amount));
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
