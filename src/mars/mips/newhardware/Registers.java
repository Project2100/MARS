/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.mips.newhardware;

import java.util.Observer;
import java.util.logging.Level;
import mars.Main;
import mars.mips.hardware.AccessNotice;

/**
 * This class serves as the main access point to the MIPS Machine registers.
 *
 * @implNote All registers have the same class, though some should have special
 * behaviour, e.g. $zero
 *
 * @author Project2100
 */
public class Registers {

    private final Register[] userRegisters;
    private int defaultGP, defaultSP;

    Registers() {
        this(MIPSMachine.defaultConfig);
    }

    Registers(MIPSMachine.Configuration config) {
        //TODO Optimize!
        userRegisters = new Register[Descriptor.values().length];
        userRegisters[Descriptor.$gp.ordinal()] = new Register(
                Descriptor.$gp.name(),
                config.getAddress(Memory.Descriptor.GLOBAL_POINTER));
        userRegisters[Descriptor.$sp.ordinal()] = new Register(
                Descriptor.$sp.name(),
                config.getAddress(Memory.Descriptor.STACK_POINTER));

        // Zero register is hardwired to do nothing on write
        userRegisters[0] = new Register(Descriptor.values()[0].name(), 0) {
            @Override
            public synchronized int set(int value) {
                // Do not write
                if (countObservers() > 0) {
                    setChanged();
                    notifyObservers(new RegisterAccessNotice(AccessNotice.WRITE, name));
                }
                return 0;
            }};

        for (int i = 1; i < userRegisters.length; i++)
            if (userRegisters[i] == null)
                userRegisters[i] = new Register(Descriptor.values()[i].name(), 0);
    }

    void configure(MIPSMachine.Configuration config) {
        defaultGP = config.getAddress(Memory.Descriptor.GLOBAL_POINTER);
        defaultSP = config.getAddress(Memory.Descriptor.STACK_POINTER);
        resetRegisters();
    }

    /**
     * Method to reinitialize the values of the registers.
     * <b>NOTE:</b> Should <i>not</i> be called from command-mode MARS because
     * this this method uses global settings from the registry. Command-mode
     * must operate using only the command switches, not registry settings. It
     * can be called from tools running stand-alone, and this is done in
     * <code>AbstractMarsToolAndApplication</code>. PENDING - NOT SYNCHRONIZED!
     */
    public void resetRegisters() {
        for (Register reg : userRegisters)
            reg.value = 0;
        userRegisters[Descriptor.$gp.ordinal()].value = defaultGP;
        userRegisters[Descriptor.$sp.ordinal()].value = defaultSP;
    }

    /**
     * Method for displaying the register values for debugging.
     */
    //PENDING - Can be abused as register read!
    public void showRegisters() {
        final String message = "Name: {0} - Number: {1} - Value: {2}";

        for (int idx = 0; idx < userRegisters.length; idx++)
            Main.logger.log(Level.INFO, message, new Object[] {
                userRegisters[idx].name,
                idx,
                userRegisters[idx].value});
    }

    /**
     * This method updates the register value whose number is num.
     *
     * @param reg Register to set the value of.
     * @param val The desired value for the register.
     * @return the register's previous value if backstepping disabled,
     * {@code val} otherwise
     */
    public int setUserRegister(Descriptor reg, int val) {

        int i = reg.ordinal();

        // Denying reentrant synchronization
        // PENDING test
        if (Thread.holdsLock(userRegisters[i]))
            throw new IllegalStateException("Reentrant call on register " + userRegisters[i].name);

        return Main.isBackSteppingEnabled()
                ? Main.program.getBackStepper().addRegisterFileRestore(i, userRegisters[i].set(val))
                : userRegisters[i].set(val);
    }


    /**
     * Sets the register identified by the specified number to the given value.
     *
     * @param regNumber the number of the register to set
     * @param value the value to set the register to
     * @return the register's old value
     */
    int set(int regNumber, int value) {
        return setUserRegister(Descriptor.values()[regNumber], value);
    }

    /**
     * Reads the register identified by the specified number.
     *
     * @param regNumber the number of the register to read
     * @return the register's value
     */
    int read(int regNumber) {
        return userRegisters[regNumber].get();
    }

    /**
     * Each individual register is a separate object and Observable. This handy
     * method will add the given Observer to each one. Currently does not apply
     * to Program Counter.
     *
     * @param observer
     */
    public void addRegistersObserver(Observer observer) {
        for (Register r : userRegisters) r.addObserver(observer);
    }

    /**
     * Each individual register is a separate object and Observable. This handy
     * method will delete the given Observer from each one. Currently does not
     * apply to Program Counter.
     *
     * @param observer
     */
    public void deleteRegistersObserver(Observer observer) {
        for (Register r : userRegisters) r.deleteObserver(observer);
    }

    public static enum Descriptor {
        $zero, $at,
        $v0, $v1,
        $a0, $a1, $a2, $a3,
        $t0, $t1, $t2, $t3, $t4, $t5, $t6, $t7,
        $s0, $s1, $s2, $s3, $s4, $s5, $s6, $s7,
        $t8, $t9,
        $k0, $k1,
        $gp, $sp, $fp, $ra
    }
}
