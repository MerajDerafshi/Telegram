package ToolBox;

import java.awt.image.BufferedImage;

public class ImageCropper {

    public static BufferedImage cropToSquare(BufferedImage originalImage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        int size = Math.min(width, height);

        int x = (width - size) / 2;
        int y = (height - size) / 2;

        return originalImage.getSubimage(x, y, size, size);
    }
}
