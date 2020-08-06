/*
 * MIT License
 * 
 * Copyright (c) 2020 Andrea Proietto
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
     * Standard setter for GPRs
     *
     * @param reg Register to set the value of
     * @param val The desired value for the register
     * @return the value previously held by the given GPR
     */
    public int setUserRegister(Descriptor reg, int val) {
        return userRegisters[reg.ordinal()].set(val);
    }

    /**
     * Standard setter for GPRs
     *
     * @param reg Register to get the value from
     * @return the register's current value
     */
    public int get(Descriptor reg) {
        return userRegisters[reg.ordinal()].get();
    }

    // SIMTHREAD
    /**
     * Sets the register identified by the specified number to the given value.
     * 
     * @apinote This method is intended to be invoked only and exclusively in the simulating thread
     *
     * @param regNumber the number of the register to set
     * @param value the value to set the register to
     * @return the register's old value
     */
    int write(int regNumber, int value) {
        
        // Denying reentrant synchronization
        // PENDING test
        if (Thread.holdsLock(userRegisters[regNumber]))
            throw new IllegalStateException("Reentrant call on register " + userRegisters[regNumber].name);

        return Main.isBackSteppingEnabled()
                ? Main.program.getBackStepper().addRegisterFileRestore(regNumber, userRegisters[regNumber].set(value))
                : userRegisters[regNumber].set(value);
    }

    // SIMTHREAD
    /**
     * Reads the register identified by the specified number.
     * 
     * @apinote This method is intended to be invoked only and exclusively in the simulating thread
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
    
    public static Descriptor findByName(String name) {
        
        for (Descriptor desc : Descriptor.values()) {
            if (desc.name().equals(name)) return desc;
        }
        
        return null;
    }
    
    // Was used for the assembling process, may not be needed anymore
    @Deprecated
    public static Descriptor findByNumber(String name) {
        
        for (Descriptor desc : Descriptor.values()) {
            if (("$" + desc.ordinal()).equals(name)) return desc;
        }
        
        return null;
    }
}
