package ch.epfl.gameboj.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import ch.epfl.gameboj.GameBoy;
import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.lcd.LcdImage;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public final class Main extends Application {

    private static final float SCALE = 2f;

    private final Map<String, Joypad.Key> textMap = Map.of("a", Joypad.Key.A,
            "b", Joypad.Key.B, "s", Joypad.Key.START, " ", Joypad.Key.SELECT);

    private final Map<KeyCode, Joypad.Key> codeMap = Map.of(KeyCode.LEFT,
            Joypad.Key.LEFT, KeyCode.RIGHT, Joypad.Key.RIGHT, KeyCode.UP,
            Joypad.Key.UP, KeyCode.DOWN, Joypad.Key.DOWN);

    private long origin = System.nanoTime();
    private long lastChangeCycle = 0;
    private float speed = 1f;

    public static void main(String[] args) {
        args = new String[] { "Tetris (JUE) (V1.1) [!].gb" }; // ENLEVER
        Application.launch(args);

    }

    @Override
    public void start(Stage primaryStage) throws Exception {

        if (getParameters().getRaw().size() != 1) {
            System.exit(1);
        }

        File romFile = new File(getParameters().getRaw().get(0));
        Cartridge cartridge = Cartridge.ofFile(romFile);
        GameBoy gameBoy = new GameBoy(cartridge);

        LcdImage image = gameBoy.lcdController().currentImage();
        ImageView imageView = new ImageView(ImageConverter.convert(image));
        imageView.setFitWidth(SCALE * image.width());
        imageView.setFitHeight(SCALE * image.height());

        imageView.setOnKeyPressed((e) -> {

            Joypad.Key key = e.getText().length() == 0
                    ? codeMap.getOrDefault(e.getCode(), null)
                    : textMap.getOrDefault(e.getText(), null);

            if (key != null)
                gameBoy.joypad().keyPressed(key);
        });

        imageView.setOnKeyReleased((e) -> {

            Joypad.Key key = e.getText().length() == 0
                    ? codeMap.getOrDefault(e.getCode(), null)
                    : textMap.getOrDefault(e.getText(), null);

            if (key != null)
                gameBoy.joypad().keyReleased(key);
        });

        AnimationTimer timer = new AnimationTimer() {

            @Override
            public void handle(long time) {

                long elapsed = time - origin;
                long cycle = (long) (elapsed * GameBoy.CYCLES_PER_NANOSECOND
                        * speed);

                gameBoy.runUntil(lastChangeCycle + cycle);
                LcdImage currentImage = gameBoy.lcdController().currentImage();
                imageView.setImage(ImageConverter.convert(currentImage));

            }
        };

        BorderPane pane = new BorderPane(imageView);

        MenuBar bar = new MenuBar();

        Menu file = new Menu("File");
        MenuItem screenshot = new MenuItem("Screenshot");
        screenshot.setOnAction(
                e -> takeScreenshot(gameBoy.lcdController().currentImage()));
        file.getItems().add(screenshot);

        Menu gpu = new Menu("GPU");
        MenuItem tiles = new MenuItem("Show tiles");
        tiles.setOnAction(e -> {

            showImage(Viewer.tiles(gameBoy));

        });
        MenuItem win = new MenuItem("Show window");
        win.setOnAction(e -> {

            showImage(Viewer.window(gameBoy));

        });
        MenuItem bg = new MenuItem("Show background");
        bg.setOnAction(e -> {

            showImage(Viewer.background(gameBoy));

        });
        MenuItem sprites = new MenuItem("Show sprites");
        sprites.setOnAction(e -> {

            showImage(Viewer.sprites(gameBoy));

        });
        gpu.getItems().addAll(tiles, win, bg, sprites);

        Menu speedMenu = new Menu("Speed");
        MenuItem freeze = new MenuItem("Freeze");
        freeze.setOnAction(e -> timer.stop());
        MenuItem x1 = new MenuItem("x1");
        x1.setOnAction(e -> {
            lastChangeCycle = gameBoy.cycles();
            origin = System.nanoTime();
            speed = 1f;
            timer.start();
        });
        MenuItem x2 = new MenuItem("x2");
        x2.setOnAction(e -> {
            lastChangeCycle = gameBoy.cycles();
            origin = System.nanoTime();
            speed = 2f;
            timer.start();
        });
        MenuItem x3 = new MenuItem("x3");
        x3.setOnAction(e -> {
            lastChangeCycle = gameBoy.cycles();
            origin = System.nanoTime();
            speed = 3f;
            timer.start();
        });
        speedMenu.getItems().addAll(freeze, x1, x2, x3);

        bar.getMenus().addAll(file, gpu, speedMenu);
        pane.setTop(bar);

        Scene scene = new Scene(pane);

        primaryStage.setTitle("Gameboj - " + cartridge.name());
        primaryStage.setScene(scene);
        primaryStage.show();
        imageView.requestFocus();
        timer.start();

    }

    private static final int[] COLOR_MAP = new int[] { 0xFF_FF_FF, 0xD3_D3_D3,
            0xA9_A9_A9, 0x00_00_00 };

    private void takeScreenshot(LcdImage li) {

        BufferedImage i = new BufferedImage(li.width(), li.height(),
                BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < li.height(); ++y)
            for (int x = 0; x < li.width(); ++x)
                i.setRGB(x, y, COLOR_MAP[li.get(x, y)]);

        try {
            ImageIO.write(i, "png", new File("screenshot.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showImage(LcdImage image) {

        Stage stage = new Stage();
        ImageView imageView = new ImageView(ImageConverter.convert(image));
        imageView.setFitWidth(SCALE * image.width());
        imageView.setFitHeight(SCALE * image.height());
        BorderPane pane = new BorderPane(imageView);
        Scene scene = new Scene(pane);
        stage.setScene(scene);
        stage.show();
    }

}
