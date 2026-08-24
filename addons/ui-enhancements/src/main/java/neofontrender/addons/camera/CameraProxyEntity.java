package neofontrender.addons.camera;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Client-only render-view anchor for a detached camera. It is never spawned, ticked, or synced;
 * Minecraft only reads its interpolated position and yaw/pitch while rendering the world.
 */
final class CameraProxyEntity extends Entity {
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

    @Override protected void entityInit() {}
    @Override protected void readEntityFromNBT(NBTTagCompound compound) {}
    @Override protected void writeEntityToNBT(NBTTagCompound compound) {}
    @Override public float getEyeHeight() { return 0.0F; }
}
