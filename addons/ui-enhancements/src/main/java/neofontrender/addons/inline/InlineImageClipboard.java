package neofontrender.addons.inline;

import neofontrender.addons.ui.NfrUiEnhancements;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;

final class InlineImageClipboard {
    private InlineImageClipboard() {}

    static boolean copy(BufferedImage image) {
        if (image == null) return false;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new ImageTransfer(image), null);
            return true;
        } catch (Throwable failure) {
            NfrUiEnhancements.LOGGER.warn("Could not copy inline image to the system clipboard", failure);
            return false;
        }
    }

    private static final class ImageTransfer implements Transferable {
        private final Image image;

        private ImageTransfer(Image image) { this.image = image; }

        @Override public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }

        @Override public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return image;
        }
    }
}
