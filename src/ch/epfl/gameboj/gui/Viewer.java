package ch.epfl.gameboj.gui;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.lcd.LcdImage;

public interface Viewer {

    /**
     * Permet d'obtenir l'image courante de la simulation du viewer en question
     * (tuiles, arrière-plan, fenêtre ou sprites)
     */
    public LcdImage currentImage();

    /**
     * Permet d'obtenir une image des tuiles affichées cote à cote
     * 
     * @param gb
     * @return
     */
    public static LcdImage tiles(GameBoy gb) {

        return new TileViewer(gb).currentImage();
    }

    /**
     * Permet d'obtenir une image 256 x 256 de l'ensemble de la fenêtre
     * 
     * @param gb
     * @return
     */
    public static LcdImage window(GameBoy gb) {
        return new WindowViewer(gb).currentImage();
    }
    
    /**
     * Permet d'obtenir une image 256 x 256 de l'ensemble de l'arrière-plan
     * 
     * @param gb
     * @return
     */
    public static LcdImage background(GameBoy gb) {
        return new BackgroundViewer(gb).currentImage();
    }
    
    /**
     * Permet d'obtenir une image 32 x 160 des sprites
     * 
     * @param gb
     * @return
     */
    public static LcdImage sprites(GameBoy gb) {
        return new SpriteViewer(gb).currentImage();
    }

}
