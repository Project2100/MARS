/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.mips.newhardware;

import static mars.mips.newhardware.MIPSMachine.fd;
import static mars.mips.newhardware.MIPSMachine.fmt;
import static mars.mips.newhardware.MIPSMachine.fs;
import static mars.mips.newhardware.MIPSMachine.ft;



//        instructionSet.add(
//                new BasicInstruction("floor.w.s $f0,$f1",
//                        "Floor single precision to word : Set $f0 to 32-bit integer floor of single-precision float in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 00000 sssss fffff 001111",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float floatValue = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        int floor = (int) Math.floor(floatValue);
//                        // DPS 28-July-2010: Since MARS does not simulate the FSCR, I will take the default
//                        // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
//                        if (Float.isNaN(floatValue)
//                                || Float.isInfinite(floatValue)
//                                || floatValue < (float) Integer.MIN_VALUE
//                                || floatValue > (float) Integer.MAX_VALUE)
//                            floor = Integer.MAX_VALUE;
//                        Coprocessor1.updateRegister(operands[0], floor);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("ceil.w.s $f0,$f1",
//                        "Ceiling single precision to word : Set $f0 to 32-bit integer ceiling of single-precision float in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 00000 sssss fffff 001110",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float floatValue = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        int ceiling = (int) Math.ceil(floatValue);
//                        // DPS 28-July-2010: Since MARS does not simulate the FSCR, I will take the default
//                        // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
//                        if (Float.isNaN(floatValue)
//                                || Float.isInfinite(floatValue)
//                                || floatValue < (float) Integer.MIN_VALUE
//                                || floatValue > (float) Integer.MAX_VALUE)
//                            ceiling = Integer.MAX_VALUE;
//                        Coprocessor1.updateRegister(operands[0], ceiling);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("round.w.s $f0,$f1",
//                        "Round single precision to word : Set $f0 to 32-bit integer round of single-precision float in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 00000 sssss fffff 001100",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException { // MIPS32 documentation (and IEEE 754) states that round rounds to the nearest but when
//                        // both are equally near it rounds to the even one!  SPIM rounds -4.5, -5.5,
//                        // 4.5 and 5.5 to (-4, -5, 5, 6).  Curiously, it rounds -5.1 to -4 and -5.6 to -5. 
//                        // Until MARS 3.5, I used Math.round, which rounds to nearest but when both are
//                        // equal it rounds toward positive infinity.  With Release 3.5, I painstakingly
//                        // carry out the MIPS and IEEE 754 standard.
//                        int[] operands = statement.getOperands();
//                        float floatValue = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        int below = 0, above = 0, round = Math.round(floatValue);
//                        // According to MIPS32 spec, if any of these conditions is true, set
//                        // Invalid Operation in the FCSR (Floating point Control/Status Register) and
//                        // set result to be 2^31-1.  MARS does not implement this register (as of release 3.4.1).
//                        // It also mentions the "Invalid Operation Enable bit" in FCSR, that, if set, results
//                        // in immediate exception instead of default value.  
//                        if (Float.isNaN(floatValue)
//                                || Float.isInfinite(floatValue)
//                                || floatValue < (float) Integer.MIN_VALUE
//                                || floatValue > (float) Integer.MAX_VALUE)
//                            round = Integer.MAX_VALUE;
//                        else {
//                            Float floatObj = new Float(floatValue);
//                            // If we are EXACTLY in the middle, then round to even!  To determine this,
//                            // find next higher integer and next lower integer, then see if distances 
//                            // are exactly equal.
//                            if (floatValue < 0.0F) {
//                                above = floatObj.intValue(); // truncates
//                                below = above - 1;
//                            }
//                            else {
//                                below = floatObj.intValue(); // truncates
//                                above = below + 1;
//                            }
//                            if (floatValue - below == above - floatValue) // exactly in the middle?
//                                round = (above % 2 == 0) ? above : below;
//                        }
//                        Coprocessor1.updateRegister(operands[0], round);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("trunc.w.s $f0,$f1",
//                        "Truncate single precision to word : Set $f0 to 32-bit integer truncation of single-precision float in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 00000 sssss fffff 001101",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float floatValue = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        int truncate = (int) floatValue;// Typecasting will round toward zero, the correct action
//                        // DPS 28-July-2010: Since MARS does not simulate the FSCR, I will take the default
//                        // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
//                        if (Float.isNaN(floatValue)
//                                || Float.isInfinite(floatValue)
//                                || floatValue < (float) Integer.MIN_VALUE
//                                || floatValue > (float) Integer.MAX_VALUE)
//                            truncate = Integer.MAX_VALUE;
//                        Coprocessor1.updateRegister(operands[0], truncate);
//                    }
//                }));


/**
 * 
 * @implnote The standard documents do not specify for double formats on which register go the higher order 32 bits. Assuming even
 * @implnote Not dealing with NaNs for now
 * @implnote Normal operation rounding is stil done by Java, regardless of the mode in FCSR
 *
 * @author Project2100
 */
public class Coprocessor1 {
    
    private final MIPSMachine machine;

	final Register[] fpRegisters;

	// IMPL dependant
	//private final Register fir;
	//
	// FCC 7-1 (31-25) : FS (24) : FCC0 (23) : ...
	private final Register fcsr;

	// Following registers are actual aliases of fcsr
	/*, fexr, fenr,*/
	//
	// 0^(31-8) : FCC (7-0)
	//private final Register fccr;
	//
	/**
	 * @implnote FS initially set to zero
	 *
	 */
	Coprocessor1(MIPSMachine machine) {
        
        this.machine = machine;
        
		//TODO Optimize!
		fpRegisters = new Register[Descriptor.values().length];

		fcsr = new Register("fcsr", 0);

		for (int i = 0; i < fpRegisters.length; i++)
			if (fpRegisters[i] == null)
				fpRegisters[i] = new Register(Descriptor.values()[i].name(), 0);
	}
    
    int roundToInt(double val) {
        
        // Read rounding mode from FCSR
        switch (fcsr.get() & 3) {
            case 0: // Round to nearest
                return (int) Math.rint(val);
            case 1: // Round to zero
                return (int) val;
            case 2: // Round to plus infinity
                return (int) Math.floor(val);
            case 3: // Round to minus infinity
                return (int) Math.ceil(val);
            default:
                throw new RuntimeException("Internal Error: Bad CP1 rounding mode");
        }
    }
    
    /**
     * 
     * @param val the bits of a double value
     * @return 
     */
    float roundToFloat(double val) {
        
        // AP200922: For now, just perform a Java cast
        
        return (float) val;
        
        
        // Read rounding mode from FCSR
//        switch (fcsr.get() & 3) {
//            case 0: // Round to nearest
//                return Math.rint(val);
//            case 1: // Round to zero
//                break;
//            case 2: // Round to plus infinity
//                break;
//            case 3: // Round to minus infinity
//                break;
//            default:
//                throw new RuntimeException("Internal Error: Bad CP1 rounding mode");
//        }
    }
    
    // SIMTHREAD
    /**
     * Main subroutine invoked on opcode COP1 detection
     * 
     */
    void executeCOP1(int instruction) {
        
        // The format field is checked first: a few instructions are actually identified here
        switch (fmt(instruction)) {
            
            case 0x00000: // MF
                if ((instruction & 0x000007FF) != 0)
                    throw new MIPSException("Stray 1-bits in MFC1");
                
                machine.gpRegisters.write(ft(instruction), fpRegisters[fs(instruction)].get());
                break;
                
            case 0x00100: // MT
                if ((instruction & 0x000007FF) != 0)
                    throw new MIPSException("Stray 1-bits in MTC1");
                
                fpRegisters[fs(instruction)].set(machine.gpRegisters.read(ft(instruction)));
                break;
                
            case 0x01000: // BC
                if ((instruction & 0x00020000) != 0)
                    throw new MIPSException("Stray 1-bit in BC");
                
                if (getFCC((instruction & 0x001c0000) >> 18) ^ ((instruction & 0x00010000) == 0)) {
					machine.doBranch(MIPSMachine.seimm(instruction));
                }
                break;
                
            case 0x10000: { // 16: Single precision
                int fdNum = fd(instruction);
                int fsNum = fs(instruction);
                int ftNum = ft(instruction);
                
                switch(instruction & 0x111111) {
                    case 0x000000: // ADD.fmt
                        fpRegisters[fdNum].set(Float.floatToRawIntBits(
                                Float.intBitsToFloat(fpRegisters[ftNum].get()) +
                                Float.intBitsToFloat(fpRegisters[fsNum].get())
                        ));
                        break;
                    case 0x000001: // SUB.fmt
                        fpRegisters[fdNum].set(Float.floatToRawIntBits(
                                Float.intBitsToFloat(fpRegisters[ftNum].get()) -
                                Float.intBitsToFloat(fpRegisters[fsNum].get())
                        ));
                        break;
                    case 0x000010: // MUL.fmt
                        fpRegisters[fdNum].set(Float.floatToRawIntBits(
                                Float.intBitsToFloat(fpRegisters[ftNum].get()) *
                                Float.intBitsToFloat(fpRegisters[fsNum].get())
                        ));
                        break;
                    case 0x000011: // DIV.fmt
                        fpRegisters[fdNum].set(Float.floatToRawIntBits(
                                Float.intBitsToFloat(fpRegisters[ftNum].get()) /
                                Float.intBitsToFloat(fpRegisters[fsNum].get())
                        ));
                        break;
                        
                        
                    case 0x000100: // SQRT.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in sqrt instruction");
                        
                        fpRegisters[fdNum].set(Float.floatToRawIntBits(
                                (float) Math.sqrt(Float.intBitsToFloat(fpRegisters[fsNum].get()))
                        ));
                        break;
                    case 0x000101: // ABS.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in abs instruction");
                        
                        fpRegisters[fdNum].set(Float.floatToRawIntBits(
                                Math.abs(Float.intBitsToFloat(fpRegisters[fsNum].get()))
                        ));
                        break;
                    case 0x000110: // MOV.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in mov instruction");
                        
                        fpRegisters[fdNum].set(fpRegisters[fsNum].get());
                        break;
                    case 0x000111: // NEG.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in neg instruction");
                        
                        fpRegisters[fdNum].set(Float.floatToRawIntBits(
                                - Float.intBitsToFloat(fpRegisters[fsNum].get())
                        ));
                        break;
                        
                        
                    case 0x001100: // ROUND.W.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in round instruction");
                        
                        // AP200921: MIPS spec of rounding conforms to IEE754, whcih is round-ties-to-even
                        fpRegisters[fdNum].set((int) Math.rint(Float.intBitsToFloat(fpRegisters[fsNum].get())));
                        break;
                    case 0x001101: // TRUNC.W.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in trunc instruction");
                        
                        // AP200921: As by Java15 spec, casting a floating-point value to an integer value is done with a round-towards-zero policy, which is truncation
                        fpRegisters[fdNum].set((int) Float.intBitsToFloat(fpRegisters[fsNum].get()));
                        break;
                    case 0x001110: // CEIL.W.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in ceil instruction");
                        
                        fpRegisters[fdNum].set((int) Math.ceil(Float.intBitsToFloat(fpRegisters[fsNum].get())));
                        break;
                    case 0x001111: // FLOOR.W.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in floor instruction");
                        
                        fpRegisters[fdNum].set((int) Math.floor(Float.intBitsToFloat(fpRegisters[fsNum].get())));
                        break;
                        
                        
                    case 0x010001: // MOVCF
                        if ((instruction & 0x00020000) != 0)
                            throw new MIPSException("Stray 1-bit in MOVCF");
                        
                        if (getFCC((instruction & 0x001c0000) >> 18) ^ ((instruction & 0x00010000) == 0)) {
                            fpRegisters[fdNum].set(fpRegisters[fsNum].get());
                        }
                        break;
                    case 0x010010: // MOVZ.fmt
                        if (machine.gpRegisters.read(ftNum) == 0) {
                            fpRegisters[fdNum].set(fpRegisters[fsNum].get());
                        }
                        break;
                    case 0x010011: // MOVN.fmt
                        if (machine.gpRegisters.read(ftNum) != 0) {
                            fpRegisters[fdNum].set(fpRegisters[fsNum].get());
                        }
                        break;
                        
                        
                    case 0x100000: // CVT.S.fmt
                        throw new MIPSException("Reserved Instruction - Same-format conversion");
                    case 0x100001: // CVT.D.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in round instruction");
                        if (fdNum % 1 != 0)
                            throw new MIPSException("Target register cannot contain a double");
                        {
                            // AP200922: This operation is always exact, no roundings occur
                            long result = Double.doubleToLongBits((double) Float.intBitsToFloat(fpRegisters[fsNum].get()));
                            fpRegisters[fdNum].set((int) (result >> 32));
                            fpRegisters[fdNum + 1].set((int) result);
                        }
                        break;
                    case 0x100100: // CVT.W.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in round instruction");
                        
                        fpRegisters[fdNum].set(roundToInt(Float.intBitsToFloat(fpRegisters[fsNum].get())));
                        break;
                        
                    
                        // Comparisons: MARS implements only EQ, LE and LT
                    case 0x110010: // C.EQ.fmt
                        if ((fdNum & 0x11) != 0)
                            throw new MIPSException("Stray 1 bits in comparison instruction");
                        
                        setFCC(fdNum >>> 2, fpRegisters[fsNum].get() == fpRegisters[ftNum].get());
                        break;
                    case 0x111100: // C.LT.fmt
                        if ((fdNum & 0x11) != 0)
                            throw new MIPSException("Stray 1 bits in comparison instruction");
                        
                        setFCC(fdNum >>> 2, fpRegisters[fsNum].get() < fpRegisters[ftNum].get());
                        break;
                    case 0x111110: // C.LE.fmt
                        if ((fdNum & 0x11) != 0)
                            throw new MIPSException("Stray 1 bits in comparison instruction");
                        
                        setFCC(fdNum >>> 2, fpRegisters[fsNum].get() <= fpRegisters[ftNum].get());
                        break;
                
                        
                    default:
                        throw new MIPSException("Invalid function for single format");
                }
                break;
            }
                
            case 0x10001: { // 17: Double precision
                int fdNum = fd(instruction);
                int fsNum = fs(instruction);
                int ftNum = ft(instruction);
                
                switch(instruction & 0x111111) {
                    case 0x000000: // ADD.fmt
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (ftNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        {
                            long result = Double.doubleToRawLongBits(
                                    Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get()) +
                                    Double.longBitsToDouble(((long) fpRegisters[ftNum].get() << 32) | (long) fpRegisters[ftNum + 1].get()));
                            fpRegisters[fdNum].set((int) (result >>> 32));
                            fpRegisters[fdNum + 1].set((int) result);
                        }
                        break;
                    case 0x000001: // SUB.fmt
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (ftNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        {
                            long result = Double.doubleToRawLongBits(
                                    Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get()) -
                                    Double.longBitsToDouble(((long) fpRegisters[ftNum].get() << 32) | (long) fpRegisters[ftNum + 1].get()));
                            fpRegisters[fdNum].set((int) (result >>> 32));
                            fpRegisters[fdNum + 1].set((int) result);
                        }
                        break;
                    case 0x000010: // MUL.fmt
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (ftNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        {
                            long result = Double.doubleToRawLongBits(
                                    Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get()) *
                                    Double.longBitsToDouble(((long) fpRegisters[ftNum].get() << 32) | (long) fpRegisters[ftNum + 1].get()));
                            fpRegisters[fdNum].set((int) (result >>> 32));
                            fpRegisters[fdNum + 1].set((int) result);
                        }
                        break;
                    case 0x000011: // DIV.fmt
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (ftNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        {
                            long result = Double.doubleToRawLongBits(
                                    Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get()) /
                                    Double.longBitsToDouble(((long) fpRegisters[ftNum].get() << 32) | (long) fpRegisters[ftNum + 1].get()));
                            fpRegisters[fdNum].set((int) (result >>> 32));
                            fpRegisters[fdNum + 1].set((int) result);
                        }
                        break;
                        
                        
                    case 0x000100: // SQRT.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in sqrt instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        {
                            long result = Double.doubleToRawLongBits(
                                    Math.sqrt(Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get())));
                            fpRegisters[fdNum].set((int) (result >>> 32));
                            fpRegisters[fdNum + 1].set((int) result);
                        }
                        break;
                    case 0x000101: // ABS.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in abs instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        {
                            long result = Double.doubleToRawLongBits(
                                    Math.abs(Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get())));
                            fpRegisters[fdNum].set((int) (result >>> 32));
                            fpRegisters[fdNum + 1].set((int) result);
                        }
                        break;
                    case 0x000110: // MOV.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in mov instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        {
                            fpRegisters[fdNum].set(fpRegisters[fsNum].get());
                            fpRegisters[fdNum + 1].set(fpRegisters[fsNum + 1].get());
                        }
                        break;
                    case 0x000111: // NEG.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in neg instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        {
                            long result = Double.doubleToRawLongBits(
                                    - Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get()));
                            fpRegisters[fdNum].set((int) (result >>> 32));
                            fpRegisters[fdNum + 1].set((int) result);
                        }
                        break;
                        
                        
                    case 0x001100: // ROUND.W.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in round instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        {
                            
                        }
                        // AP200921: MIPS spec of rounding conforms to IEE754, whcih is round-ties-to-even
                        fpRegisters[fdNum].set((int) Math.rint(Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get())));
                        break;
                    case 0x001101: // TRUNC.W.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in trunc instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        
                        // AP200921: As by Java15 spec, casting a floating-point value to an integer value is done with a round-towards-zero policy, which is truncation
                        fpRegisters[fdNum].set((int) Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get()));
                        break;
                    case 0x001110: // CEIL.W.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in ceil instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        
                        fpRegisters[fdNum].set((int) Math.ceil(Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get())));
                        break;
                    case 0x001111: // FLOOR.W.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in floor instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        
                        fpRegisters[fdNum].set((int) Math.floor(Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get())));
                        break;
                        
                        
                    case 0x010001: // MOVCF
                        if ((instruction & 0x00020000) != 0)
                            throw new MIPSException("Stray 1-bit in MOVCF");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        
                        if (getFCC((instruction & 0x001c0000) >> 18) ^ ((instruction & 0x00010000) == 0)) {
                            fpRegisters[fdNum].set(fpRegisters[fsNum].get());
                            fpRegisters[fdNum + 1].set(fpRegisters[fsNum + 1].get());
                        }
                        break;
                    case 0x010010: // MOVZ.fmt
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        
                        if (machine.gpRegisters.read(ftNum) == 0) {
                            fpRegisters[fdNum].set(fpRegisters[fsNum].get());
                            fpRegisters[fdNum + 1].set(fpRegisters[fsNum + 1].get());
                        }
                        break;
                    case 0x010011: // MOVN.fmt
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (fdNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        
                        if (machine.gpRegisters.read(ftNum) != 0) {
                            fpRegisters[fdNum].set(fpRegisters[fsNum].get());
                            fpRegisters[fdNum + 1].set(fpRegisters[fsNum + 1].get());
                        }
                        break;
                        
                        
                    case 0x100000: // CVT.S.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in cvt instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        
                        fpRegisters[fdNum].set(Float.floatToIntBits(roundToFloat(Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get()))));
                    case 0x100001: // CVT.D.fmt
                        throw new MIPSException("Reserved Instruction - Same-format conversion");
                    case 0x100100: // CVT.W.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in round instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        
                        fpRegisters[fdNum].set(roundToInt(Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get())));
                        break;
                        
                    
                        // Comparisons: MARS implements only EQ, LE and LT
                    case 0x110010: // C.EQ.fmt
                        if ((fdNum & 0x11) != 0)
                            throw new MIPSException("Stray 1 bits in comparison instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (ftNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        
                        setFCC(fdNum >>> 2,
                                Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get()) ==
                                Double.longBitsToDouble(((long) fpRegisters[ftNum].get() << 32) | (long) fpRegisters[ftNum + 1].get()));
                        break;
                    case 0x111100: // C.LT.fmt
                        if ((fdNum & 0x11) != 0)
                            throw new MIPSException("Stray 1 bits in comparison instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (ftNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        
                        setFCC(fdNum >>> 2,
                                Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get()) <
                                Double.longBitsToDouble(((long) fpRegisters[ftNum].get() << 32) | (long) fpRegisters[ftNum + 1].get()));
                        break;
                    case 0x111110: // C.LE.fmt
                        if ((fdNum & 0x11) != 0)
                            throw new MIPSException("Stray 1 bits in comparison instruction");
                        if (fsNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        if (ftNum % 2 != 0)
                            throw new MIPSException("Invalid register for double format");
                        
                        setFCC(fdNum >>> 2,
                                Double.longBitsToDouble(((long) fpRegisters[fsNum].get() << 32) | (long) fpRegisters[fsNum + 1].get()) <=
                                Double.longBitsToDouble(((long) fpRegisters[ftNum].get() << 32) | (long) fpRegisters[ftNum + 1].get()));
                        break;
                
                    
                    default:
                        throw new MIPSException("Invalid function for double format");
                }
                break;
            }
                
            case 0x10100: { // 20: Word
                int fdNum = fd(instruction);
                int fsNum = fs(instruction);
                int ftNum = ft(instruction);
                
                switch(instruction & 0x111111) {
                    
                    case 0x100000: // CVT.S.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in round instruction");
                        
                        fpRegisters[fdNum].set(Float.floatToRawIntBits((float) fpRegisters[fsNum].get()));
                    case 0x100001: // CVT.D.fmt
                        if (ftNum != 0)
                            throw new MIPSException("Nonzero ft in round instruction");
                        if (fdNum % 1 != 0)
                            throw new MIPSException("Target register cannot contain a double");
                        {
                            long result = Double.doubleToLongBits((double) fpRegisters[fsNum].get());
                            fpRegisters[fdNum].set((int) (result >> 32));
                            fpRegisters[fdNum + 1].set((int) result);
                        }
                        break;
                    case 0x100100: // CVT.W.fmt
                        throw new MIPSException("Reserved Instruction - Same-format conversion");
                    
                    
                    default:
                        throw new MIPSException("Invalid function for word format");
                }
                break;
            }
            
            default:
                throw new MIPSException("Unrecognized format/function for COP1 opcode");
        }
        
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
//    int write(int fmt, int regNumber, int value) {
//        
//        // Denying reentrant synchronization
//        // PENDING test
//        if (Thread.holdsLock(fpRegisters[regNumber]))
//            throw new IllegalStateException("Reentrant call on register " + fpRegisters[regNumber].name);
//
//        return Main.isBackSteppingEnabled()
//                ? Main.program.getBackStepper().addRegisterFileRestore(regNumber, fpRegisters[regNumber].set(value))
//                : fpRegisters[regNumber].set(value);
//    }

	boolean getFCC(int code) {
		if (code > 7) throw new MIPSException("Bad code received: " + code);

		return ((code == 0)
				? ((fcsr.value & 0x00800000) >> 23)
				: ((fcsr.value & (1 << (code + 24))) >> (code + 24)))
				== 1;

	}
    
	void setFCC(int code, boolean value) {
		if (code > 7) throw new MIPSException("Bad code received: " + code);
        
        fcsr.value = (value 
                ? (fcsr.value |  (1 << (code + 24)))
                : (fcsr.value & ~(1 << (code + 24)))
        );

	}

	public static enum Descriptor {
		$f0, $f1, $f2, $f3, $f4, $f5, $f6, $f7,
		$f8, $f9, $f10, $f11, $f12, $f13, $f14, $f15,
		$f16, $f17, $f18, $f19, $f20, $f21, $f22, $f23,
		$f24, $f25, $f26, $f27, $f28, $f29, $f30, $f31;
        
	}
    
        
    public static Descriptor findByName(String name) {

        for (Descriptor desc : Descriptor.values()) {
            if (desc.name().equals(name)) return desc;
        }

        return null;
    }

}
