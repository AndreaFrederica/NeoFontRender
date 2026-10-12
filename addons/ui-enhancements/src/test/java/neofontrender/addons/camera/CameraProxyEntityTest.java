package neofontrender.addons.camera;

import net.minecraft.entity.EntityLivingBase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CameraProxyEntityTest {
    @Test
    void poseSynchronizesEveryVanillaInterpolationPosition() {
        CameraProxyEntity proxy = new CameraProxyEntity(null);

        assertInstanceOf(EntityLivingBase.class, proxy);
        assertEquals(proxy.getMaxHealth(), proxy.getHealth(), 0.0F);
        proxy.setCameraPose(128.5D, 72.25D, -31.75D, 42.0F, -18.0F);

        assertEquals(proxy.posX, proxy.prevPosX, 0.0D);
        assertEquals(proxy.posY, proxy.prevPosY, 0.0D);
        assertEquals(proxy.posZ, proxy.prevPosZ, 0.0D);
        assertEquals(proxy.posX, proxy.lastTickPosX, 0.0D);
        assertEquals(proxy.posY, proxy.lastTickPosY, 0.0D);
        assertEquals(proxy.posZ, proxy.lastTickPosZ, 0.0D);
        assertEquals(proxy.rotationYaw, proxy.prevRotationYaw, 0.0F);
        assertEquals(proxy.rotationPitch, proxy.prevRotationPitch, 0.0F);
        // 1.7.10 exposes the entity bounds as a public field instead of getEntityBoundingBox().
        assertEquals(proxy.posX, (proxy.boundingBox.minX
                + proxy.boundingBox.maxX) * 0.5D, 1.0E-9D);
        assertEquals(proxy.posZ, (proxy.boundingBox.minZ
                + proxy.boundingBox.maxZ) * 0.5D, 1.0E-9D);
    }
}
