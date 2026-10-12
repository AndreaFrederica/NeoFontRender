package neofontrender.addons.camera;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Client-only render-view anchor for a detached camera. It is never spawned, ticked, or synced;
 * Minecraft only reads its interpolated position and yaw/pitch while rendering the world.
 */
final class CameraProxyEntity extends EntityLivingBase {
    private final ItemStack[] equipment = new ItemStack[5];

    CameraProxyEntity(World world) {
        super(world);
        noClip = true;
        // 1.12.2's orientCamera lifts the camera by getEyeHeight(), which this proxy reports as
        // zero. 1.7.10 instead computes yOffset - 1.62F, so the offset has to be compensated here
        // or the rendered camera would sit 1.62 blocks above the requested origin.
        yOffset = 1.62F;
    }

    void setCameraPose(double x, double y, double z, float yaw, float pitch) {
        setPosition(x, y, z);
        lastTickPosX = prevPosX = posX = x;
        lastTickPosY = prevPosY = posY = y;
        lastTickPosZ = prevPosZ = posZ = z;
        prevRotationYaw = rotationYaw = yaw;
        prevRotationPitch = rotationPitch = pitch;
    }

    @Override public ItemStack getHeldItem() { return equipment[0]; }

    @Override public ItemStack getEquipmentInSlot(int slot) {
        return slot >= 0 && slot < equipment.length ? equipment[slot] : null;
    }

    @Override public void setCurrentItemOrArmor(int slot, ItemStack stack) {
        if (slot >= 0 && slot < equipment.length) equipment[slot] = stack;
    }

    @Override public ItemStack[] getLastActiveItems() { return equipment; }

    @Override public float getEyeHeight() { return 0.0F; }
}
