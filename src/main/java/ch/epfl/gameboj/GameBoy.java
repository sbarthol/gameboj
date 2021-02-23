package ch.epfl.gameboj;

import java.util.Objects;

import ch.epfl.gameboj.component.Joypad;
import ch.epfl.gameboj.component.Timer;
import ch.epfl.gameboj.component.cartridge.Cartridge;
import ch.epfl.gameboj.component.cpu.Cpu;
import ch.epfl.gameboj.component.lcd.LcdController;
import ch.epfl.gameboj.component.memory.BootRomController;
import ch.epfl.gameboj.component.memory.Ram;
import ch.epfl.gameboj.component.memory.RamController;

public final class GameBoy {

    public static final long CYCLES_PER_SECOND = 1 << 20;
    public static final double CYCLES_PER_NANOSECOND = CYCLES_PER_SECOND
            / 1000000000.0;

    private final Bus bus;
    private final Cpu cpu;
    private final Timer timer;
    private final LcdController lcdController;
    private final Joypad joypad;
    long currentCycle;

    /**
     * Construit une Game Boy en créant les composants nécessaires et en les
     * attachant à un bus commun
     * 
     * @param cartridge
     */
    public GameBoy(Cartridge cartridge) {

        bus = new Bus();
        Ram ram = new Ram(AddressMap.WORK_RAM_SIZE);

        RamController controller = new RamController(ram,
                AddressMap.WORK_RAM_START);
        controller.attachTo(bus);

        RamController controllerCopy = new RamController(ram,
                AddressMap.ECHO_RAM_START, AddressMap.ECHO_RAM_END);
        controllerCopy.attachTo(bus);

        cpu = new Cpu();
        cpu.attachTo(bus);

        BootRomController bootRomController = new BootRomController(
                Objects.requireNonNull(cartridge));
        bootRomController.attachTo(bus);

        timer = new Timer(cpu);
        timer.attachTo(bus);

        lcdController = new LcdController(cpu);
        lcdController.attachTo(bus);

        joypad = new Joypad(cpu);
        joypad.attachTo(bus);

        currentCycle = 0;
    }

    /**
     * Retourne le bus de la GameBoy
     * 
     * @return bus
     * 
     */
    public Bus bus() {
        return bus;
    }

    /**
     * Retourne le processeur de la GameBoy
     * 
     * @return cpu
     */
    public Cpu cpu() {
        return cpu;
    }

    /**
     * Retourne le contrôleur LCD
     * 
     * @return lcdController
     */
    public LcdController lcdController() {
        return lcdController;
    }

    /**
     * Retourne le minuteur de la GameBoy
     * 
     * @return timer
     */
    public Timer timer() {
        return timer;
    }

    /**
     * Retourne le joypad de la GameBoy
     * 
     * @return joypad
     */
    public Joypad joypad() {
        return joypad;
    }

    /**
     * Simule le fonctionnement du GameBoy jusqu'au cycle donné moins 1, ou lève
     * l'exception IllegalArgumentException si un nombre (strictement) supérieur
     * de cycles a déjà été simulé
     * 
     * @param cycle
     */
    public void runUntil(long cycle) {

        Preconditions.checkArgument(currentCycle <= cycle);

        while (currentCycle < cycle) {
            timer.cycle(currentCycle);
            lcdController.cycle(currentCycle);
            cpu.cycle(currentCycle);
            currentCycle++;
        }
    }

    /**
     * Retourne le nombre de cycles déjà simulés
     * 
     * @return currentCyle
     */
    public long cycles() {
        return currentCycle;
    }

}
