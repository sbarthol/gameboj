package ch.epfl.gameboj.component;

public final class DebugPrintComponent implements Component {

    @Override
    public int read(int address) throws IllegalArgumentException {
        return NO_DATA;
    }

    @Override
    public void write(int address, int data) throws IllegalArgumentException {
        if (address == 0xFF01) {
            System.out.printf("%c",data);
        }
    }
}
