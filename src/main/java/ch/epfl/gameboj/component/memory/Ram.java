package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;

public final class Ram {

	private final byte[] data;

	/**
	 * construit une nouvelle mémoire vive de taille donnée (en octets) ou lève
	 * IllegalArgumentException si celle-ci est strictement négative
	 * 
	 * @param size
	 * @throws IllegalArgumentException
	 */
	public Ram(int size) {

		Preconditions.checkArgument(size >= 0);
		data = new byte[size];
	}

	/**
	 * retourne la taille, en octets, de la mémoire
	 * 
	 * @return
	 */
	public int size() {
		return data.length;
	}

	/**
	 * retourne l'octet se trouvant à l'index donné, sous la forme d'une valeur
	 * comprise entre 0 et FF16, ou lève l'exception IndexOutOfBoundsException si
	 * l'index est invalide
	 * 
	 * @param index
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	public int read(int index) {

		if (index >= 0 && index < data.length) {
			return Byte.toUnsignedInt(data[index]);
		} else {
			throw new IndexOutOfBoundsException();
		}
	}

	/**
	 * Modifie le contenu de la mémoire à l'index donné pour qu'il soit égal à la
	 * valeur donnée ; lève l'exception IndexOutOfBoundsException si l'index est
	 * invalide, et l'exception IllegalArgumentException si la valeur n'est pas une
	 * valeur 8 bits
	 * 
	 * @param index
	 * @param value
	 * @throws IndexOutOfBoundsException
	 * @throws IllegalArgumentException
	 */
	public void write(int index, int value) {

		Preconditions.checkBits8(value);
		if (index < 0 || index >= data.length) {
			throw new IndexOutOfBoundsException();
		}

		data[index] = (byte) value;
	}
}
