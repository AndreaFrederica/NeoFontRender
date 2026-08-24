package neofontrender.addons.camera;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CameraProxyEntityTest {
    @Test
    void poseSynchronizesEveryVanillaInterpolationPosition() {
        net.minecraft.init.Bootstrap.register();
        CameraProxyEntity proxy = new CameraProxyEntity(null);

        proxy.setCameraPose(128.5D, 72.25D, -31.75D, 42.0F, -18.0F);

        assertEquals(proxy.posX, proxy.prevPosX, 0.0D);
        assertEquals(proxy.posY, proxy.prevPosY, 0.0D);
        assertEquals(proxy.posZ, proxy.prevPosZ, 0.0D);
        assertEquals(proxy.posX, proxy.lastTickPosX, 0.0D);
        assertEquals(proxy.posY, proxy.lastTickPosY, 0.0D);
        assertEquals(proxy.posZ, proxy.lastTickPosZ, 0.0D);
        assertEquals(proxy.rotationYaw, proxy.prevRotationYaw, 0.0F);
        assertEquals(proxy.rotationPitch, proxy.prevRotationPitch, 0.0F);
        assertEquals(proxy.posX, (proxy.getEntityBoundingBox().minX
                + proxy.getEntityBoundingBox().maxX) * 0.5D, 1.0E-9D);
        assertEquals(proxy.posZ, (proxy.getEntityBoundingBox().minZ
                + proxy.getEntityBoundingBox().maxZ) * 0.5D, 1.0E-9D);
    }
}
