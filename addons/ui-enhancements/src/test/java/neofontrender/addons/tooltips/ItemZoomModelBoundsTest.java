package neofontrender.addons.tooltips;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemZoomModelBoundsTest {
    @Test
    void cubeFitIncludesFaceQuadsAndKeepsRoomForEveryRotation() {
        int stride = DefaultVertexFormats.ITEM.getIntegerSize();
        int[] vertices = new int[stride * 4];
        for (int i = 0; i < 4; i++) {
            vertices[i * stride] = Float.floatToIntBits(i % 2);
            vertices[i * stride + 1] = Float.floatToIntBits(i / 2);
            vertices[i * stride + 2] = Float.floatToIntBits(1);
        }
        BakedQuad quad = new BakedQuad(vertices, -1, EnumFacing.SOUTH, null,
                true, DefaultVertexFormats.ITEM);
        assertEquals((float) Math.sqrt(3) / 2, ItemZoomRenderer.radius(model(quad, null)), 0.0001F);
        PreviewModelBounds bounds = PreviewModelBounds.baked(model(quad, null),
                ItemZoomModelBoundsTest::yaw, true);
        assertEquals(-(float) Math.sqrt(0.5), bounds.left, 0.0001F);
        assertEquals((float) Math.sqrt(0.5), bounds.right, 0.0001F);
        assertEquals(-0.5F, bounds.top, 0.0001F);
        assertEquals(0.5F, bounds.bottom, 0.0001F);
    }

    @Test
    void fitIncludesNonePerspectiveScaleAndTranslation() {
        int stride = DefaultVertexFormats.ITEM.getIntegerSize();
        int[] vertices = new int[stride * 4];
        for (int i = 0; i < 4; i++) {
            vertices[i * stride] = Float.floatToIntBits(1);
            vertices[i * stride + 1] = Float.floatToIntBits(0.5F);
            vertices[i * stride + 2] = Float.floatToIntBits(0.5F);
        }
        Matrix4f transform = new Matrix4f();
        transform.setIdentity();
        transform.m00 = 2;
        transform.m03 = 0.5F;
        BakedQuad quad = new BakedQuad(vertices, -1, EnumFacing.SOUTH, null,
                true, DefaultVertexFormats.ITEM);
        assertEquals(1.5F, ItemZoomRenderer.radius(model(quad, transform)), 0.0001F);
        PreviewModelBounds bounds = PreviewModelBounds.baked(model(quad, transform),
                ItemZoomModelBoundsTest::yaw, true);
        assertEquals(-1.5F, bounds.left, 0.0001F);
        assertEquals(1.5F, bounds.right, 0.0001F);
        assertEquals(0, bounds.top, 0.0001F);
    }

    private static Matrix4f yaw(float degrees) {
        Matrix4f matrix = new Matrix4f();
        matrix.rotY((float) Math.toRadians(degrees));
        return matrix;
    }

    private static IBakedModel model(BakedQuad quad, Matrix4f transform) {
        return new IBakedModel() {
            @Override public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long random) {
                return side == EnumFacing.SOUTH ? Collections.singletonList(quad) : Collections.emptyList();
            }
            @Override public boolean isAmbientOcclusion() { return true; }
            @Override public boolean isGui3d() { return true; }
            @Override public boolean isBuiltInRenderer() { return false; }
            @Override public TextureAtlasSprite getParticleTexture() { return null; }
            @Override public ItemOverrideList getOverrides() { return ItemOverrideList.NONE; }
            @Override public Pair<? extends IBakedModel, Matrix4f> handlePerspective(
                    ItemCameraTransforms.TransformType type) { return Pair.of(this, transform); }
        };
    }
}
