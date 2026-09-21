package neofontrender.addons.tooltips;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import neofontrender.addons.ui.NfrUiEnhancements;
import neofontrender.addons.build.UiBuildFeatures;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import ae2.api.storage.cells.IStackTooltipDataProvider;
import ae2.items.storage.StorageCellTooltipComponent;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.GenericStack;
import net.minecraftforge.fluids.FluidUtil;

/** Public source-level bridge for AE2 Supergiant's native tooltip path. */
public final class NfrAe2TooltipApi {
    /** Producer id written into anchor markers folded from AE2's reserved rows. */
    private static final String AE2_MOD_ID = "ae2";
    private static final neofontrender.api.client.tooltip.NfrTooltipApi.DocumentProvider DOCUMENT_PROVIDER =
            NfrAe2TooltipApi::createDocument;
    private static final neofontrender.api.client.tooltip.NfrTooltipApi.DocumentFinalizer DOCUMENT_FINALIZER =
            NfrAe2TooltipApi::normalizeDocument;

    private NfrAe2TooltipApi() {}

    public static void register() {
        neofontrender.api.client.tooltip.NfrTooltipApi.registerDocumentProvider(DOCUMENT_PROVIDER);
        neofontrender.api.client.tooltip.NfrTooltipApi.registerDocumentFinalizer(DOCUMENT_FINALIZER);
        if (UiBuildFeatures.DIAGNOSTIC_LOGS) {
            NfrUiEnhancements.LOGGER.info("Registered AE2 tooltip document source adapter");
        }
    }

    /** Builds the same coordinate-free document for ordinary Forge tooltips. */
    public static Optional<neofontrender.api.client.tooltip.NfrTooltipApi.TooltipDocument>
    createDocument(ItemStack stack, List<String> lines) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof IStackTooltipDataProvider provider)) {
            return Optional.empty();
        }
        Optional<StorageCellTooltipComponent> image = provider.getTooltipImage(stack);
        if (!image.isPresent()) return Optional.empty();
        neofontrender.api.client.tooltip.NfrTooltipApi.TooltipDocument.Builder builder =
                neofontrender.api.client.tooltip.NfrTooltipApi.TooltipDocument.builder(stack, lines);
        List<neofontrender.api.client.tooltip.NfrTooltipApi.ItemNode> items = new ArrayList<>();
        for (GenericStack entry : image.get().content()) {
            addNode(items, entry, image.get().showAmounts());
        }
        if (!items.isEmpty()) {
            builder.add(new neofontrender.api.client.tooltip.NfrTooltipApi.ItemRowNode(items,
                    image.get().hasMoreContent()));
        }
        // Upgrades are part of the same AE2 visual component and must be included
        // in the document so the common layout owns their height as well.
        List<neofontrender.api.client.tooltip.NfrTooltipApi.ItemNode> upgrades = new ArrayList<>();
        for (ItemStack upgrade : image.get().upgrades()) {
            if (upgrade != null && !upgrade.isEmpty()) {
                upgrades.add(new neofontrender.api.client.tooltip.NfrTooltipApi.ItemNode(upgrade, 1, false));
            }
        }
        if (!upgrades.isEmpty()) {
            builder.add(new neofontrender.api.client.tooltip.NfrTooltipApi.SpacerNode(2));
            builder.add(new neofontrender.api.client.tooltip.NfrTooltipApi.ItemRowNode(upgrades, false));
        }
        if (items.isEmpty() && upgrades.isEmpty()) return Optional.empty();
        return Optional.of(builder.build());
    }

    private static void addNode(List<neofontrender.api.client.tooltip.NfrTooltipApi.ItemNode> target,
                                GenericStack entry, boolean showAmount) {
        if (entry == null || entry.what() == null) return;
        ItemStack display = ItemStack.EMPTY;
        if (entry.what() instanceof AEItemKey key) {
            display = key.toStack();
        } else if (entry.what() instanceof AEFluidKey key) {
            // UIE's visual node is item based; use the canonical filled bucket
            // representation for fluids so the generic renderer can still own
            // measurement and drawing without a fluid-specific backend.
            try {
                display = FluidUtil.getFilledBucket(key.toStack(1000));
            } catch (RuntimeException ignored) {
                display = ItemStack.EMPTY;
            }
        }
        if (display != null && !display.isEmpty()) {
            target.add(new neofontrender.api.client.tooltip.NfrTooltipApi.ItemNode(
                    display, entry.amount(), showAmount));
        }
    }

    /**
     * Turns AE2's reserved placeholder rows into an explicit anchor marker and applies the
     * configured ownership placement. AE2 appends its reserved image rows right after the
     * ownership line and its ingredient-action lines after those rows, so "ownership last"
     * means the ownership line moves past everything else while the stored-content rows
     * keep the position AE2 reserved for them. Documents built by AE2 never pass through
     * ModernTooltipHandler's reorder, so this is applied here for every document render.
     */
    private static neofontrender.api.client.tooltip.NfrTooltipApi.TooltipDocument normalizeDocument(
            neofontrender.api.client.tooltip.NfrTooltipApi.TooltipDocument document) {
        if (document == null) return null;
        List<String> lines = NfrTooltipAnchor.foldReservedBlocks(document.lines,
                ae2.client.gui.StackTooltipRenderer::isReservedTooltipLine,
                NfrTooltipAnchor.FAMILY_ITEMS, AE2_MOD_ID);
        if (lines != document.lines) {
            document = new neofontrender.api.client.tooltip.NfrTooltipApi.TooltipDocument(
                    document.stack, lines, document.nodes);
        }
        return document;
    }

    public static boolean render(ItemStack stack, List<String> lines, int mouseX, int mouseY,
                                 FontRenderer font) {
        return neofontrender.api.client.tooltip.NfrTooltipApi.render(
                stack, lines, mouseX, mouseY, font);
    }
}
