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

/**
 * Represents Coprocessor 0.
 *
 * @implnote Only some functionality is implemented, MARS does not emulate
 * everything of a MIPS CPU (such as cache management and instruction pipelines)
 *
 *
 */
// TODO missing memory segmentation
public class Coprocessor0 {

    // Fields : CPA_..._HWI_SWI_..._UM_..._ERL_EXL_IE
    // bits 32-38 (CPA : CoProcessor Available) TODO explain?!? 
    // bits 8-15 (HWI::SWI : mask for interrupt levels) all set,
    // bit 4 (UM : user mode) set,
    // bit 2 (ERL : error level) not set,
    // bit 1 (EXL : exception level) not set,
    // bit 0 (IE : interrupt enable) set.
    final int DEFAULT_STATUS = 0b1000_000000000000_111111_11_000_1_0_0_0_1;

    final Register[] regs;

    public Coprocessor0() {

        regs = new Register[32];

        // TODO Override Register.set()?
        //CP0.8.0 : BadVAddr
        // TODO used on AddressErrors (Not exceptions!!!) and TLB errors...
        regs[8] = new Register("BadVAddr", 0);

        //CP0.8.1 : BadInstr (TODO)
        // Holds a copy of the offending instruction
        //
        //
        //CP0.12.0 : Status
        // Runtime status
        regs[12] = new Register("Status", DEFAULT_STATUS);

        //CP0.13.0 : Cause
        // Describes the cause of the latest exception
        // TODO Too many fields, document...
        regs[13] = new Register("Cause", 0);

        //CP0.14.0 : EPC
        // Return point for exception handling, used by ERET
        regs[14] = new Register("EPC", 0);

        //CP0.16.[0|1] : Config# (TODO)
        // ALL CONFIGS!!
        //
        //
        //CP0.17.0 : LLAddr (TODO)
        // @IMPLNOTE Holds physical address of last LL,
        // The two LSBs will be always zero as by word alignemnt, thus the last bit is used to check validity of atomic RMW - AP200503: Noes, this is on Release 5 :(
        regs[17] = new Register("LLAddr", 0);

        //CP0.30.0 : ErrorEPC
        regs[30] = new Register("ErrorEPC", 0);

    }
    
    
    // SIMTHREAD
    // AP200503 - TODO: Use selector to pick the right register
    /**
     * Reads the register identified by the specified number.
     * 
     * @apinote This method is intended to be invoked only and exclusively in the simulating thread
     *
     * @param regNumber the number of the register to read
     * @param selector 
     * @return the register's value
     */
    // AP200503 - TODO: READABILITY CHECKS!
    int read(int regNumber, int selector) {
        return regs[regNumber].get();
    }
    
    // SIMTHREAD
    /**
     * Writes the given value to the register identified by the specified number.
     * 
     * @apinote This method is intended to be invoked only and exclusively in the simulating thread
     *
     * @param regNumber the number of the register to read
     * @param selector 
     * @param value the value to write in the CP0R
     * @return the register's value
     */
    // AP200503 - TODO: WRITEABILITY CHECKS!
    int write(int regNumber, int selector, int value) {
        return regs[regNumber].set(value);
    }
    

    boolean isInKMode() {
        return (regs[12].value & 16) == 0;
    }

    /**
     * Tells whether the memory location at the given address is synchronized
     *
     * @param address
     * @return {@code true} if the pointed word in memory is unmodified,
     * {@code false} otherwise
     * @throws IllegalArgumentException if the given address is misaligned
     */
    boolean isSynchronized(int address) {
        // Never hurts to put an alignment check here...
        if ((address & 3) != 0)
            throw new IllegalArgumentException("INTERNAL ERROR: Misaligned memory address in a LL: " + address);

        // Check synchronization bit
        return regs[16].value == (address & 1);
    }

    void trialSync(int address) {
        // If address is the same, then clear synchronizaion bit
        if ((address & -2) == (regs[16].value & -2))
            invalidateRMW();
    }

    void invalidateRMW() {
        regs[16].value &= -2;
    }

    void beginRMW(int address) {
        // Never hurts to put an alignment check here...
        if ((address & 3) != 0)
            throw new IllegalArgumentException("INTERNAL ERROR: Misaligned memory address in a LL: " + address);

        // Set validity bit along, preceding LLs are simply overwritten/forgotten as by specification
        regs[16].value = address | 1;
    }

    int exceptionReturn() {
        int targetPC;
        int status = regs[12].get();

        // Decide if error or exception, clear status bits accordingly
        if ((status & 4) != 0) {
            targetPC = regs[30].get();
            regs[12].set(status & 0xFFFFFFFB);
        }
        else {
            targetPC = regs[14].get();
            regs[12].set(status & 0xFFFFFFFD);
        }

        // Invalidate atomic RMW
        invalidateRMW();

        // No hazards to clear since pipeline is not implemented
        return targetPC;
    }
}
