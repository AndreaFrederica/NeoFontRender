package ae2.api.storage.cells;

import ae2.items.storage.StorageCellTooltipComponent;
import net.minecraft.item.ItemStack;
import java.util.Optional;

/** Compile-only signature; never packaged. */
public interface IStackTooltipDataProvider {
    Optional<StorageCellTooltipComponent> getTooltipImage(ItemStack stack);
}
