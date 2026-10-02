package neofontrender.addons.camera;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHandSide;
import net.minecraft.world.World;

import java.util.Collections;

/**
 * Client-only render-view anchor for a detached camera. It is never spawned, ticked, or synced;
 * Minecraft only reads its interpolated position and yaw/pitch while rendering the world.
 */
final class CameraProxyEntity extends EntityLivingBase {
    CameraProxyEntity(World world) {
        super(world);
        noClip = true;
    }

    void setCameraPose(double x, double y, double z, float yaw, float pitch) {
        setPosition(x, y, z);
        lastTickPosX = prevPosX = posX = x;
        lastTickPosY = prevPosY = posY = y;
        lastTickPosZ = prevPosZ = posZ = z;
        prevRotationYaw = rotationYaw = yaw;
        prevRotationPitch = rotationPitch = pitch;
    }

    @Override public Iterable<ItemStack> getArmorInventoryList() {
        return Collections.emptyList();
    }

    @Override public ItemStack getItemStackFromSlot(EntityEquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override public void setItemStackToSlot(EntityEquipmentSlot slot, ItemStack stack) {
    }

    @Override public EnumHandSide getPrimaryHand() {
        return EnumHandSide.RIGHT;
    }

    @Override public float getEyeHeight() { return 0.0F; }
}
