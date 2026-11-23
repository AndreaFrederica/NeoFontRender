package ae2.items.storage;

import ae2.api.stacks.GenericStack;
import net.minecraft.item.ItemStack;
import java.util.List;

/** Compile-only signature; never packaged. */
public record StorageCellTooltipComponent(List<ItemStack> upgrades, List<GenericStack> content,
                                         boolean hasMoreContent, boolean showAmounts) {}
