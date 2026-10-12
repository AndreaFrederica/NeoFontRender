package neofontrender.addons.outlines;

import net.minecraft.block.Block;

final class BlockOutlineStyle {
    private final Block block;
    private final int metadata;
    private final float lineWidth;
    private final int color;

    BlockOutlineStyle(Block block, int metadata, float lineWidth, int color) {
        this.block = block;
        this.metadata = metadata;
        this.lineWidth = lineWidth;
        this.color = color;
    }

    float lineWidth() { return lineWidth; }
    int color() { return color; }

    boolean matches(Block stateBlock, int stateMetadata) {
        return block == stateBlock && (metadata < 0 || metadata == stateMetadata);
    }
}
