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
    // its 32-38 (CPA : CoProcessor Available) TODO explain?!? 
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

        //CP0.15.[0..5] : Config# (TODO)
        // ALL CONFIGS!!
        //
        //
        //CP0.16.0 : LLAddr (TODO)
        // @IMPLNOTE Holds physical address of last LL,
        // The two LSBs will be always zero as by word alignemnt, thus the last bit is used to check validity of atomic RMW
        regs[16] = new Register("LLAddr", 0);

        //CP0.30.0 : ErrorEPC
        regs[30] = new Register("ErrorEPC", 0);

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
