package neofontrender.addons.outlines;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

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

    boolean matches(IBlockState state) {
        return block == state.getBlock()
                && (metadata < 0 || metadata == state.getBlock().getMetaFromState(state));
    }
}
