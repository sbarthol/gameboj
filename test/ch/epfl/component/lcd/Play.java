package ch.epfl.component.lcd;

import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Joypad.Key;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.lcd.LcdImage;

public class Play {

    private static final int[] COLOR_MAP = new int[] { 0xebffd4, 0x97b77c, 0x4b6572,
            0x091b26 };
    private static GameBoy gb;
    private static int SCALE = 3;

    public static void main(String[] args)
            throws IOException, InterruptedException {

        File romFile = new File("flappyboy.gb");
        long cycles = Long.MAX_VALUE;
        gb = new GameBoy(Cartridge.ofFile(romFile));
        JFrame frame = new JFrame();
        frame.addKeyListener(new Listener(gb.joypad()));
        frame.setVisible(true);
        frame.getContentPane().setLayout(new FlowLayout());

        int i = 0;
        while (gb.cycles() < cycles) {
            
            i++;
            gb.runUntil(Math.min(cycles, gb.cycles() + 17556));
            if(i > 150)Thread.sleep(3);
            if(i%5!=0)continue;

            show(gb.lcdController().currentImage(), frame);
            frame.pack();
            
        }
    }

    private static void show(LcdImage li, JFrame frame) {

        BufferedImage i = new BufferedImage(li.width() * SCALE,
                li.height() * SCALE, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < li.height(); ++y)
            for (int x = 0; x < li.width(); ++x) {
                
                int color = li.get(x, y);

                for (int r = 0; r < SCALE; r++) {
                    for (int c = 0; c < SCALE; c++) {
                        i.setRGB(SCALE * x + r, SCALE * y + c,
                                COLOR_MAP[color]);
                    }
                }
            }

        frame.setContentPane(new JLabel(new ImageIcon(i)));
    }
}

class Listener implements KeyListener {

    private Joypad joypad;

    Listener(Joypad joypad) {
        this.joypad = joypad;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub

    }

    @Override
    public void keyPressed(KeyEvent e) {


        switch (e.getKeyCode()) {
        case KeyEvent.VK_A: {
            joypad.keyPressed(Key.A);
        }
            break;
        case KeyEvent.VK_B: {
            joypad.keyPressed(Key.B);
        }
            break;
        case KeyEvent.VK_SPACE: {
            joypad.keyPressed(Key.SELECT);
        }
            break;
        case KeyEvent.VK_ENTER: {
            joypad.keyPressed(Key.START);
        }
            break;

        case KeyEvent.VK_LEFT: {
            joypad.keyPressed(Key.LEFT);
        }
            break;
        case KeyEvent.VK_RIGHT: {
            joypad.keyPressed(Key.RIGHT);
        }
            break;
        case KeyEvent.VK_DOWN: {
            joypad.keyPressed(Key.DOWN);
        }
            break;
        case KeyEvent.VK_UP: {
            joypad.keyPressed(Key.UP);
        }
            break;
        }

    }

    @Override
    public void keyReleased(KeyEvent e) {

        switch (e.getKeyCode()) {
        case KeyEvent.VK_A: {
            joypad.keyReleased(Key.A);
        }
            break;
        case KeyEvent.VK_B: {
            joypad.keyReleased(Key.B);
        }
            break;
        case KeyEvent.VK_SPACE: {
            joypad.keyReleased(Key.SELECT);
        }
            break;
        case KeyEvent.VK_ENTER: {
            joypad.keyReleased(Key.START);
        }
            break;

        case KeyEvent.VK_LEFT: {
            joypad.keyReleased(Key.LEFT);
        }
            break;
        case KeyEvent.VK_RIGHT: {
            joypad.keyReleased(Key.RIGHT);
        }
            break;
        case KeyEvent.VK_DOWN: {
            joypad.keyReleased(Key.DOWN);
        }
            break;
        case KeyEvent.VK_UP: {
            joypad.keyReleased(Key.UP);
        }
            break;
        }
    }
}
