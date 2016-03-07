/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.mips.newhardware;

import java.util.ConcurrentModificationException;
import java.util.Observer;
import java.util.logging.Level;
import mars.Main;

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
        for (int i = 0; i < userRegisters.length; i++)
            if (userRegisters[i] == null)
                userRegisters[i] = new Register(Descriptor.values()[i].name(), 0);
    }

    public void configure(MIPSMachine.Configuration config) {
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
     * This method updates the register value who's number is num. Also handles
     * the lo and hi registers
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

        if (reg.equals(Descriptor.$zero)) {
//            throw new IllegalArgumentException("The $zero register cannot be set to any value!");
            Main.logger.log(Level.INFO, "Writing on $zero register - ignoring");
            return 0;
        }

        return Main.isBackSteppingEnabled()
                ? Main.program.getBackStepper().addRegisterFileRestore(i, userRegisters[i].set(val))
                : userRegisters[i].set(val);
    }

    /**
     * Returns the value of the register who's number is num.
     *
     * @param num The register number.
     * @return The value of the given register.
     */
    public int readUserRegister(Descriptor num) {
        return userRegisters[num.ordinal()].get();

    }

    //PENDING deleteme
    public int updateRegister(int num, int val) {
        return setUserRegister(Descriptor.values()[num], val);
    }

    //PENDING deleteme
    public void updateRegister(String reg, int val) {
        setUserRegister(Descriptor.valueOf(reg), val);
    }

    //PENDING deleteme
    public int getValue(int num) {
        return readUserRegister(Descriptor.values()[num]);
    }

    //PENDING deleteme
    public static int getNumber(String n) {
        return Descriptor.valueOf(n).ordinal();
    }

    //WARNING DELETEME
    public static Register[] getRegisters() throws IllegalAccessException {
//        return regFile;
        throw new IllegalAccessException("These are private now...");
    }

    //PENDING needed?
    public Register getUserRegister(String Rname) {
        return userRegisters[Descriptor.valueOf(Rname).ordinal()];
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
