package ch.epfl.gameboj.gui;

import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public final class ImageConverter {

    private static final int[] COLOR_MAP = new int[] { 0xFF_FF_FF_FF, 0xFF_D3_D3_D3,
            0xFF_A9_A9_A9, 0xFF_00_00_00 };

    /**
     * Convertit une image de type LcdImage en image de la même taille de type
     * javafx.scene.image.Image
     * 
     * @param image
     * @return
     */
    public static Image convert(LcdImage image) {

        WritableImage writableImage = new WritableImage(image.width(),
                image.height());
        PixelWriter pixelWriter = writableImage.getPixelWriter();

        for (int y = 0; y < image.height(); ++y)
            for (int x = 0; x < image.width(); ++x)
                pixelWriter.setArgb(x, y, COLOR_MAP[image.get(x, y)]);

        return writableImage;

    }

}
