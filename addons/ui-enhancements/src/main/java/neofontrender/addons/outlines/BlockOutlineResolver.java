package neofontrender.addons.outlines;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import neofontrender.addons.ui.NfrUiEnhancements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Runtime rule resolver adapted from FancyOutlines by Invadermonky (WTFPL v2). */
public final class BlockOutlineResolver {
    private static final Pattern RULE = Pattern.compile(
            "^([^:\\s=]+:[^:\\s=]+)(?::(\\d+))?=([0-9]+(?:\\.[0-9]+)?);(#[0-9a-fA-F]{8})$");
    private static volatile List<BlockOutlineStyle> normalRules = Collections.emptyList();
    private static volatile List<BlockOutlineStyle> noHarvestRules = Collections.emptyList();

    private BlockOutlineResolver() {}

    static void reload() {
        normalRules = parse(BlockOutlineConfig.blockOverrides, "normal");
        noHarvestRules = parse(BlockOutlineConfig.noHarvestOverrides, "no-harvest");
    }

    public static ResolvedOutline resolve(EntityPlayer player, IBlockState state, BlockPos position) {
        if (!BlockOutlineConfig.enabled || player == null || state == null || position == null) return null;
        boolean noHarvest = BlockOutlineConfig.noHarvestEnabled
                && !ForgeHooks.canHarvestBlock(state.getBlock(), player, player.world, position);
        List<BlockOutlineStyle> rules = noHarvest ? noHarvestRules : normalRules;
        for (BlockOutlineStyle rule : rules) {
            if (rule.matches(state)) return new ResolvedOutline(rule.lineWidth(), rule.color());
        }
        return noHarvest
                ? new ResolvedOutline(BlockOutlineConfig.noHarvestLineWidth, BlockOutlineConfig.noHarvestColor)
                : new ResolvedOutline(BlockOutlineConfig.globalLineWidth, BlockOutlineConfig.globalColor);
    }

    private static List<BlockOutlineStyle> parse(List<String> values, String group) {
        List<BlockOutlineStyle> result = new ArrayList<>();
        for (String value : values) {
            try {
                Matcher matcher = RULE.matcher(value.trim());
                if (!matcher.matches()) throw new IllegalArgumentException("invalid syntax");
                ResourceLocation id = new ResourceLocation(matcher.group(1));
                Block block = ForgeRegistries.BLOCKS.getValue(id);
                if (block == null || block == Blocks.AIR) throw new IllegalArgumentException("unknown block " + id);
                int metadata = matcher.group(2) == null ? -1 : Integer.parseInt(matcher.group(2));
                if (metadata > 15) throw new IllegalArgumentException("metadata must be 0-15");
                float width = Math.max(0.0F, Math.min(1000.0F, Float.parseFloat(matcher.group(3))));
                int color = (int) Long.parseLong(matcher.group(4).substring(1), 16);
                result.add(new BlockOutlineStyle(block, metadata, width, color));
            } catch (RuntimeException exception) {
                NfrUiEnhancements.LOGGER.warn("Ignoring invalid {} block-outline rule '{}': {}",
                        group, value, exception.getMessage());
            }
        }
        return Collections.unmodifiableList(result);
    }

    public static final class ResolvedOutline {
        public final float lineWidth;
        public final float red;
        public final float green;
        public final float blue;
        public final float alpha;

        private ResolvedOutline(float lineWidth, int argb) {
            this.lineWidth = lineWidth;
            this.alpha = (argb >>> 24 & 0xFF) / 255.0F;
            this.red = (argb >>> 16 & 0xFF) / 255.0F;
            this.green = (argb >>> 8 & 0xFF) / 255.0F;
            this.blue = (argb & 0xFF) / 255.0F;
        }
    }
}
