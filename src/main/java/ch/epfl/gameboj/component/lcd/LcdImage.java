package ch.epfl.gameboj.component.lcd;

import ch.epfl.gameboj.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LcdImage {

	private final List<LcdImageLine> list;
	private final int height, width;

	/**
	 * Construit une image avec une liste de lcdImageLines donnés
	 * 
	 * @param width
	 * @param height
	 * @param list
	 */
	public LcdImage(int width, int height, List<LcdImageLine> list) {

		Preconditions.checkArgument(list.size() > 0 && width == list.get(0).size() && height == list.size());

		this.list = Collections.unmodifiableList(new ArrayList<>(list));
		this.height = height;
		this.width = width;
	}
	
	/**
     * Construit une image avec des zeros
     * 
     * @param width
     * @param height
     */
    public LcdImage(int width, int height) {

        Preconditions.checkArgument(width > 0 && height > 0);

        this.list = Collections.nCopies(height, new LcdImageLine(width));
        this.height = height;
        this.width = width;
    }

	/**
	 * @return la largeur de l'image
	 */
	public int width() {
		return width;
	}

	/**
	 * 
	 * @return la hauteur de l'image
	 */
	public int height() {
		return height;
	}

	/**
	 * Permet d'obtenir, sous la forme d'un entier compris entre 0 et 3, la couleur
	 * d'un pixel d'index (x, y) donné (get).
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public int get(int x, int y) {

		boolean lsb = list.get(y).lsb().testBit(x);
		boolean msb = list.get(y).msb().testBit(x);
		return (msb ? 1 : 0) * 2 + (lsb ? 1 : 0);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (!(other instanceof LcdImage))
			return false;

		return list.equals(((LcdImage) other).list);
	}

	@Override
	public int hashCode() {
		return list.hashCode();
	}

	public final static class Builder {

		private final List<LcdImageLine> list;
		private final int height, width;

		/**
		 * Prend en argument la largeur et la hauteur de l'image à bâtir. Initialement,
		 * celle-ci est vide, c-à-d que tous ses pixels ont la couleur 0
		 * 
		 * @param width
		 * @param height
		 */
		public Builder(int width, int height) {

			Preconditions.checkArgument(width > 0 && height > 0);
			list = new ArrayList<>(Collections.<LcdImageLine>nCopies(height, new LcdImageLine(width)));
			this.height = height;
			this.width = width;

		}

		/**
		 * Permet de changer la ligne d'index donné
		 * 
		 * @param line
		 * @param index
		 * @return
		 */
		public Builder setLine(int index, LcdImageLine line) {

			Preconditions.checkArgument(line.size() == width);
			list.set(index, line);
			return this;
		}

		/**
		 * Permet d'obtenir l'image en cours de construction
		 * 
		 * @return
		 */
		public LcdImage build() {

			return new LcdImage(width, height, list);
		}
	}
}
