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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import mars.Main;
import mars.assembler.SymbolTable;
import mars.settings.BooleanSettings;

/**
 * @implSpec text & data segments and memory limit must be multiples of 8 to
 * ensure doubleword alignment consistency (and avoid segfaults) PENDING -
 * Restrictions on MMIO and uData subsegments?
 * <p>
 * </p>
 * Little-endian only.
 *
 * @author Project2100
 */
public class MIPSMachine {

	//<editor-fold defaultstate="collapsed" desc="Config statics">
	// Starting with MARS 3.7, the configuration can be changed.
	private static final HashSet<Configuration> configurations = new HashSet<>(3);
	// The default configuration is based on SPIM.
	public static final Configuration defaultConfig;

	static {
		configurations.add(defaultConfig = new Configuration("Default", "Default", Configuration.defaultConfigValues));
		configurations.add(new Configuration("CompactDataAtZero", "Compact, Data at Address 0", Configuration.dataCompactConfigValues));
		configurations.add(new Configuration("CompactTextAtZero", "Compact, Text at Address 0", Configuration.textCompactConfigValues));
	}

	public static ArrayList<Configuration> getConfigurations() {
		return new ArrayList<>(configurations);
	}

	public static Configuration getConfigByName(String name) {
		return configurations.stream()
				.filter(config -> name.equals(config.getID()))
				.findFirst().orElse(null);
	}
	//</editor-fold>

	// Machine components
	final Register pc, hi, lo;
	Registers gpRegisters;
	Memory memory;
	Coprocessor0 coprocessor0;
	Coprocessor1 coprocessor1;

	// Delayed branching fields - surrogate of the pipelining side-effect
	int delayArgument;
	DelayState branchState;

	// (Not-only)Memory configuration
	private Configuration currentConfig;

	public MIPSMachine() {
		this(defaultConfig);
	}

	public MIPSMachine(Configuration config) {
		memory = new Memory(config);
		gpRegisters = new Registers(config);

		coprocessor0 = new Coprocessor0();
		coprocessor1 = new Coprocessor1();

		pc = new Register("pc", config.getAddress(Memory.Descriptor.TEXT_BASE_ADDRESS));
		hi = new Register("hi", 0);
		lo = new Register("lo", 0);

		configure(config);

		delayArgument = 0;
		branchState = DelayState.IDLE;
	}

    /**
     * Gives a reference to this machine's memory
     * 
     * @return 
     */
    public Memory getMemory() {
        return memory;
    }
    
    public Registers getGPRegisters() {
        return gpRegisters;
    }

	/**
	 * For returning the program counter's initial (reset) value. PENDING: what
	 * should I actually return?
	 *
	 * @return The program counter's initial value
	 */
	public int getInitialProgramCounter() {
		return currentConfig.getAddress(Memory.Descriptor.TEXT_BASE_ADDRESS);
	}

	/**
	 * For returning the program counters value.
	 *
	 * @return The program counters value as an int.
	 */
	// PENDING notify?
	public int getProgramCounter() {
		return pc.get();
	}

	/**
	 * Method to increment the Program counter in the general case (not a jump
	 * or branch).
	 */
	//PENDING notify?
	public void incrementPC() {
		pc.set(pc.value + InstructionSetArchitecture.INSTRUCTION_BYTES);
	}

	/**
	 * For setting the Program Counter. Note that ordinary PC update should be
	 * done using incrementPC() method. Use this only when processing jumps and
	 * branches.
	 *
	 * @param value The value to set the Program Counter to.
	 * @return previous PC value
	 */
	// TODO BACKSTEPPER!!!
	public int setProgramCounter(int value) {
		return Main.isBackSteppingEnabled()
				? Main.program.getBackStepper().addPCRestore(pc.set(value))
				: pc.set(value);
	}

	/**
	 * Will initialize the Program Counter to either the default reset value, or
	 * the address associated with source program global label "main", if it
	 * exists as a text segment label and the global setting is set.
	 *
	 * @param startAtMain If true, will set program counter to address of
	 * statement labeled 'main' (or other defined start label) if defined. If
	 * not defined, or if parameter false, will set program counter to default
	 * reset value.
	 */
	public void initializeProgramCounter(boolean startAtMain) {
		int mainAddr = Main.symbolTable.getAddress(SymbolTable.getStartLabel());
		pc.set(startAtMain && mainAddr != SymbolTable.NOT_FOUND
				&& (memory.inTextSegment(mainAddr) || memory.inKernelTextSegment(mainAddr))
				? mainAddr
				: currentConfig.getAddress(Memory.Descriptor.TEXT_BASE_ADDRESS));
	}

	public Configuration configuration() {
		return currentConfig;
	}

	/**
	 * Sets current memory configuration for simulated MIPS. Configuration is
	 * collection of memory segment addresses. e.g. text segment starting at
	 * address 0x00400000. Configuration can be modified starting with MARS 3.7.
	 *
	 * Changing memory configuration will clear it
	 *
	 * @param config
	 * @return {@code true} if configuration has been changed, {@code false}
	 * otherwise
	 */
	public final boolean configure(Configuration config) {
		if (config == null || config.equals(currentConfig))
			return false;
		Main.logger.log(Level.CONFIG, "Changing machine configuration from {0} to {1}",
				new Object[] {currentConfig == null ? "NULL" : currentConfig.getName(), config.getName()});

		memory = new Memory(config);
		gpRegisters.configure(config);

		currentConfig = config;

		initializeProgramCounter(BooleanSettings.START_AT_MAIN.isSet());
		hi.value = lo.value = 0;
		//TODO Reset coprocessors
		//TODO call gc?

		return true;
	}


	//<editor-fold defaultstate="collapsed" desc="Helpers">
	// PERSONAL NOTES
	// --------------------
	// 16bit Sign extension of int: val<<16>>16 | (int)(short)int
	// 0-extension of int: val&0xFFFF
	static final int opcode(int i) {
		return i >> 26 & 0b111111;
	}

	static final int rs(int i) {
		return i >> 21 & 0b11111;
	}

	static final int rt(int i) {
		return i >> 16 & 0b11111;
	}

	static final int rd(int i) {
		return i >> 11 & 0b11111;
	}

	static final int shamt(int i) {
		return i >> 6 & 0b11111;
	}

	static final int seimm(int i) {
		return i << 16 >> 16;
	}

	static final int zeimm(int i) {
		return i & 0xFFFF;
	}
	//</editor-fold>

	/**
	 *
	 * @implnote <ul>
	 * <li>Missing instructions: BGEZL, BLTZL</li>
	 * <li>M(F|H)(HI|LO) instructions behave as in MIPS IV, previous versions
	 * have restrictions, refer to documentation volume II</li>
	 * <li> BREAK instruction is not specified to have an argument, it is
	 * implemented here by having a single integer value as parameter which will
	 * be loaded into code field; omitting it sets code to 0</li>
	 * <li> Atomic RMWs are implemented at word-precision, no caching mechanism
	 * implemented
	 * <li> MOV(F|T) instructions with FP condition omitted (implied to be 0)
	 * are removed, they are not described in the official spec; just put a 0 in
	 * the instruction...</li>
	 * <li> Code field in trap instructions is ignored (a.k.a. UNDEFINED)</li>
	 * </ul>
	 *
	 *
	 * @author Project2100
	 */
// TODO Reimplement FPC-less MOV(F|T) as pseudoinstructions?
// TODO Unimplemented restrictions on mtfhilo instructions!
// TODO test casting correctness of MULT and MULTU
// TODO check sign behaviour over DIV and DIVU
// TODO Division by zero is treated as NOP, should be unpredictable...
// TODO Implement syscall system!
// TODO Write something in trap instructions' code field?
	final void executeInstruction(int instruction) throws MIPSException {

		//Extract common fields
		final int rs = rs(instruction);
		final int rt = rt(instruction);

		// Execute instruction
		// TODO test branches thoroughly on delayed/expedite
		switch (opcode(instruction)) {

			//<editor-fold defaultstate="collapsed" desc="SPECIAL OPCODE">
			case 0: // SPECIAL opcode - instruction is implicitly R-typed
			{
				//Extract R fields
				final int rd = rd(instruction);
				final int shamt = shamt(instruction);

				switch (instruction & 0b111111) { // read FUNCTION field

					case 0b000000: // SLL (NOP, SSNOP, PAUSE, ?????)
						// check if rs is empty
						if (rs != 0)
							throw new MIPSException("Invalid instruction: nonzero RS in SLL");
						// Execute shift
						//TODO: does not contemplate SSNOP PAUSE and ??????
						gpRegisters.set(rd, gpRegisters.read(rt) << shamt);
						break;


					case 0b000001: // MOVF MOVT
						// check if rd is empty
						if (rd == 0)
							throw new MIPSException("Invalid instruction: nonzero RD in MOVF");
						// check 18th bit
						if ((instruction & 0x00020000) != 0)
							throw new MIPSException("Invalid instruction: 18th bit in MOVF is set to 1");
						// Execute - see tf bit to discern MOVF from MOVT
						if ((instruction & 0x00010000) == 0) {
							if (coprocessor1.getFCC((instruction & 0x001B0000) >> 18) == false)
								gpRegisters.set(rd, gpRegisters.read(rs));
						}
						else if (coprocessor1.getFCC((instruction & 0x001B0000) >> 18) == true)
							gpRegisters.set(rd, gpRegisters.read(rs));


					case 0b000010: // SRL
						// check if rs is empty
						if (rs != 0)
							throw new MIPSException("Invalid instruction: nonzero RS in SRL");
						// Execute shift
						gpRegisters.set(rd, gpRegisters.read(rt) >>> shamt);
						break;

					case 0b000011: // SRA
						// check if rs is empty
						if (rs != 0)
							throw new MIPSException("Invalid instruction: nonzero RS in SRA");
						// Execute shift
						gpRegisters.set(rd, gpRegisters.read(rt) >> shamt);
						break;

					case 0b000100: // SLLV
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in SLLV");
						// Execute shift
						gpRegisters.set(rd, gpRegisters.read(rs) << (gpRegisters.read(rt) & 0b11111));
						break;

					case 0b000110: // SRLV
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in SRLV");
						// Execute shift
						gpRegisters.set(rd, gpRegisters.read(rs) >>> (gpRegisters.read(rt) & 0b11111));
						break;

					case 0b000111: // SRAV
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in SRAV");
						// Execute shift
						gpRegisters.set(rd, gpRegisters.read(rs) >> (gpRegisters.read(rt) & 0b11111));
						break;

					case 0b001000: // JR
						doJump(gpRegisters.read(rs));
						break;

					case 0b001001: // JALR
						doJump(gpRegisters.read(rs));
						andLink(rt);
						break;

					case 0b001010: // MOVZ
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in MOVZ");
						if (gpRegisters.read(rt) == 0)
							gpRegisters.set(rd, gpRegisters.read(rs));
						break;

					case 0b001011: // MOVN
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in MOVN");
						if (gpRegisters.read(rt) != 0)
							gpRegisters.set(rd, gpRegisters.read(rs));
						break;

					case 0b001100: // SYSCALL
						//Basically checking if every single bit is at it should be...
						if (instruction != 12)
							throw new MIPSException("Invalid instruction: malformed SYSCALL");
						throw new MIPSException("Syscall!");

					case 0b001101: // BREAK
						throw new MIPSException("Breakpoint reached, code: " + (instruction >> 6));

					case 0b010000: // MFHI
						if (shamt != 0 || rs != 0 || rt != 0)
							throw new MIPSException("Invalid instruction: nonzero field in MFHI");
						gpRegisters.set(rd, hi.get());
						break;

					case 0b010001: // MTHI
						if (shamt != 0 || rd != 0 || rt != 0)
							throw new MIPSException("Invalid instruction: nonzero field in MTHI");
						hi.set(gpRegisters.read(rs));
						break;

					case 0b010010: // MFLO
						if (shamt != 0 || rs != 0 || rt != 0)
							throw new MIPSException("Invalid instruction: nonzero field in MFLO");
						gpRegisters.set(rd, lo.get());
						break;

					case 0b010011: // MTLO
						if (shamt != 0 || rd != 0 || rt != 0)
							throw new MIPSException("Invalid instruction: nonzero field in MTLO");
						lo.set(gpRegisters.read(rd));
						break;

					case 0b011000: // MULT
					{
						if (shamt != 0 || rd != 0)
							throw new MIPSException("Invalid instruction: nonzero field in MULT");
						long product = (long) rs * (long) rt;
						hi.set((int) (product >> 32));
						lo.set((int) product);
						break;
					}

					case 0b011001: // MULTU
					{
						if (shamt != 0 || rd != 0)
							throw new MIPSException("Invalid instruction: nonzero field in MULTU");
						long product = ((long) rs & 0x00000000_FFFFFFFF) * ((long) rt & 0x00000000_FFFFFFFF);
						hi.set((int) (product >> 32));
						lo.set((int) product);
					}

					case 0b011010: // DIV
					{
						if (shamt != 0 || rd != 0)
							throw new MIPSException("Invalid instruction: nonzero field in DIV");
						if (rt != 0) {
							hi.set(rs % rt);
							lo.set(rs / rt);
						}
					}

					case 0b011011: // DIVU
					{
						if (shamt != 0 || rd != 0)
							throw new MIPSException("Invalid instruction: nonzero field in DIVU");
						if (rt != 0) {
							hi.set(Integer.remainderUnsigned(rs, rt));
							lo.set(Integer.divideUnsigned(rs, rt));
						}
					}

					case 0b100000: // ADD
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in ADD");
						// Sum, check overflow and write to destination
						try {
							gpRegisters.set(rd, Math.addExact(gpRegisters.read(rs), gpRegisters.read(rt)));
						}
						catch (ArithmeticException ex) {
							throw new MIPSException("Integer overflow");
						}
						break;

					case 0b100001: //ADDU
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in ADDU");
						// Sum and write to destination
						gpRegisters.set(rd, gpRegisters.read(rs) + gpRegisters.read(rt));
						break;

					case 0b100010: //SUB
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in SUB");
						// Subtract, check overflow and write to destination
						try {
							gpRegisters.set(rd, Math.subtractExact(gpRegisters.read(rs), gpRegisters.read(rt)));
						}
						catch (ArithmeticException ex) {
							throw new MIPSException("Integer overflow");
						}
						break;

					case 0b100011: //SUBU
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in SUBU");
						// Subtract and write to destination
						gpRegisters.set(rd, gpRegisters.read(rs) - gpRegisters.read(rt));
						break;

					case 0b100100: //AND
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in AND");
						// Compute and write to destination
						gpRegisters.set(rd, gpRegisters.read(rs) & gpRegisters.read(rt));
						break;

					case 0b100101: //OR
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in OR");
						// Compute and write to destination
						gpRegisters.set(rd, gpRegisters.read(rs) | gpRegisters.read(rt));
						break;

					case 0b100110: //XOR
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in XOR");
						// Compute and write to destination
						gpRegisters.set(rd, gpRegisters.read(rs) ^ gpRegisters.read(rt));
						break;

					case 0b100111: //NOR
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in NOR");
						// Compute and write to destination
						gpRegisters.set(rd, ~(gpRegisters.read(rs) | gpRegisters.read(rt)));
						break;

					case 0b101010: // SLT
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in SLT");
						gpRegisters.set(rd, (gpRegisters.read(rs) < gpRegisters.read(rt)) ? 1 : 0);
						break;

					case 0b101011: // SLTU
						// check if shamt is empty
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero SHAMT in SLTU");
						gpRegisters.set(rd, Integer.compareUnsigned(gpRegisters.read(rs), gpRegisters.read(rt)) < 0 ? 1 : 0);
						break;

					case 0b110000: // TGE
						if (gpRegisters.read(rs) >= gpRegisters.read(rt))
							throw new MIPSException("It's a trap!");
						break;

					case 0b110001: // TGEU
						if (Integer.compareUnsigned(gpRegisters.read(rs), gpRegisters.read(rt)) >= 0)
							throw new MIPSException("It's a trap!");
						break;

					case 0b110010: // TLT
						if (gpRegisters.read(rs) < gpRegisters.read(rt))
							throw new MIPSException("It's a trap!");
						break;

					case 0b110011: // TLTU
						if (Integer.compareUnsigned(gpRegisters.read(rs), gpRegisters.read(rt)) < 0)
							throw new MIPSException("It's a trap!");
						break;

					case 0b110100: // TEQ
						if (Integer.compareUnsigned(gpRegisters.read(rs), gpRegisters.read(rt)) == 0)
							throw new MIPSException("It's a trap!");
						break;

					case 0b110110: // TNE
						if (Integer.compareUnsigned(gpRegisters.read(rs), gpRegisters.read(rt)) != 0)
							throw new MIPSException("It's a trap!");
						break;

					default:
						throw new MIPSException(
								"Invalid FUNCTION field on SPECIAL opcode" + (instruction & 0b111111));

				}
				break;
			}
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="REGIMM OPCODE">
			case 1: // REGIMM opcode, instruction is implicitly I-typed

				switch (rt) {
					case 0b00000: // BLTZ
						if (gpRegisters.read(rs) < gpRegisters.read(0))
							doBranch(seimm(instruction));
						break;

					case 0b00001: // BGEZ
						if (gpRegisters.read(rs) >= gpRegisters.read(0))
							doBranch(seimm(instruction));
						break;

					case 0b00010: // BLTZL
						throw new MIPSException("Unsupported instruction: BLTZL");

					case 0b00011: // BGEZL
						throw new MIPSException("Unsupported instruction: BGEZL");

					case 0b01000: // TGEI
						if (gpRegisters.read(rs) >= seimm(instruction))
							throw new MIPSException("It's a trap!");
						break;

					case 0b01001: // TGEIU
						if (Integer.compareUnsigned(gpRegisters.read(rs), seimm(instruction)) >= 0)
							throw new MIPSException("It's a trap!");
						break;

					case 0b01010: // TLTI
						if (gpRegisters.read(rs) < seimm(instruction))
							throw new MIPSException("It's a trap!");
						break;

					case 0b01011: // TLTIU
						if (Integer.compareUnsigned(gpRegisters.read(rs), seimm(instruction)) < 0)
							throw new MIPSException("It's a trap!");
						break;

					case 0b01100: // TEQ
						if (gpRegisters.read(rs) == seimm(instruction))
							throw new MIPSException("It's a trap!");
						break;

					case 0b01110: // TNE
						if (gpRegisters.read(rs) != seimm(instruction))
							throw new MIPSException("It's a trap!");
						break;

					case 0b10000: // BLTZAL
						if (gpRegisters.read(rs) < gpRegisters.read(0)) {
							doBranch(seimm(instruction));
							andLink(31);
						}
						break;

					case 0b10001: // BGEZAL
						if (gpRegisters.read(rs) >= gpRegisters.read(0)) {
							doBranch(seimm(instruction));
							andLink(31);
						}
						break;
				}
				break;
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="J-TYPE INSTRUCTIONS">
			case 2: // J
				doJump((instruction & 0x0FFFFFFF) << 2);

			case 3: // JAL
				doJump((instruction & 0x0FFFFFFF) << 2);
				andLink(31);
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="BRANCH IMMEDIATE INSTRUCTIONS">
			case 0b000100: // BEQ
				if (gpRegisters.read(rs) == gpRegisters.read(rt))
					doBranch(seimm(instruction));
				break;

			case 0b000101: // BNE
				if (gpRegisters.read(rs) != gpRegisters.read(rt))
					doBranch(seimm(instruction));
				break;

			case 0b000110: // BLEZ
				if (rt != 0)
					throw new MIPSException("Invalid instruction: nonzero RT in BLEZ");

				if (gpRegisters.read(rs) <= gpRegisters.read(0))
					doBranch(seimm(instruction));
				break;

			case 0b000111: // BGTZ
				if (rt != 0)
					throw new MIPSException("Invalid instruction: nonzero RT in BGTZ");

				if (gpRegisters.read(rs) > gpRegisters.read(0))
					doBranch(seimm(instruction));
				break;
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="ARITHMETIC-LOGIC IMMEDIATE INSTRUCTIONS">
			case 0b001000: // ADDI
				try {
					gpRegisters.set(rt, Math.addExact(gpRegisters.read(rs), instruction << 16 >> 16));
				}
				catch (ArithmeticException ex) {
					throw new MIPSException("Integer overflow");
				}
				break;

			case 0b001001: // ADDIU
				gpRegisters.set(rt, gpRegisters.read(rs) + seimm(instruction));
				break;

			case 0b001010: // SLTI
				gpRegisters.set(rt, gpRegisters.read(rs) < seimm(instruction) ? 1 : 0);
				break;

			case 0b001011: // SLTIU
				gpRegisters.set(rt, Integer.compareUnsigned(gpRegisters.read(rs), seimm(instruction)) < 0 ? 1 : 0);
				break;

			case 0b001100: // ANDI
				gpRegisters.set(rt, gpRegisters.read(rs) & zeimm(instruction));
				break;

			case 0b001101: // ORI
				gpRegisters.set(rt, gpRegisters.read(rs) | zeimm(instruction));
				break;

			case 0b001110: // XORI
				gpRegisters.set(rt, gpRegisters.read(rs) ^ zeimm(instruction));
				break;

			case 0b001111: // LUI
				// Direct instruction shift, no sign correction required
				gpRegisters.set(rt, instruction << 16);
				break;
			//</editor-fold>

			case 0b010000: // COP0 - instruction format partially compatible with R-type
				//TODO first decode RS, then FUNC!!!!
				switch (instruction & 0b111111) {
					case 0b011000: // ERET
						// IMPORTANT: Implemented as specified in Volume II-A of spec, page 167
						// Counter natural increment
						// PENDING TODO: Original impl does not correct! Check correctness
						setProgramCounter(coprocessor0.exceptionReturn() - 4);
						break;
				}
				break;

			//<editor-fold defaultstate="collapsed" desc="SPECIAL2 OPCODE">
			case 0b011100: // SPECIAL2 opcode - instruction is implicitly R-typed
			{
				//Extract R fields
				final int rd = rd(instruction);
				final int shamt = shamt(instruction);

				switch (instruction & 0b111111) { // read FUNCTION field

					case 0b000000: // MADD
					{
						if (shamt != 0 || rd != 0)
							throw new MIPSException("Invalid instruction: nonzero field in MADD");
						long product = (long) rs * (long) rt
								+ ((long) lo.get() & 0xFFFFFFFFL) | ((long) hi.get() << 32);
						hi.set((int) (product >> 32));
						lo.set((int) product);
						break;
					}

					case 0b000001: // MADDU
					{
						if (shamt != 0 || rd != 0)
							throw new MIPSException("Invalid instruction: nonzero field in MADDU");
						long product = ((long) rs & 0x00000000_FFFFFFFF) * ((long) rt & 0x00000000_FFFFFFFF)
								+ ((long) lo.get() & 0xFFFFFFFFL) | ((long) hi.get() << 32);
						hi.set((int) (product >> 32));
						lo.set((int) product);
						break;
					}

					case 0b000010: // MUL
					{
						if (shamt != 0)
							throw new MIPSException("Invalid instruction: nonzero field in MUL");
						long product = (long) rs * (long) rt;
						hi.set((int) (product >> 32));
						lo.set((int) product);
						gpRegisters.set(rd, (int) product);
						break;
					}

					case 0b000100: // MSUB
					{
						if (shamt != 0 || rd != 0)
							throw new MIPSException("Invalid instruction: nonzero field in MADD");
						long product = (((long) lo.get() & 0xFFFFFFFFL) | ((long) hi.get() << 32))
								- (long) rs * (long) rt;
						hi.set((int) (product >> 32));
						lo.set((int) product);
						break;
					}

					case 0b000101: // MSUBU
					{
						if (shamt != 0 || rd != 0)
							throw new MIPSException("Invalid instruction: nonzero field in MADDU");
						long product = (((long) lo.get() & 0xFFFFFFFFL) | ((long) hi.get() << 32))
								- ((long) rs & 0x00000000_FFFFFFFF) * ((long) rt & 0x00000000_FFFFFFFF);
						hi.set((int) (product >> 32));
						lo.set((int) product);
						break;
					}

					case 0b100000: // CLZ
					{
						if (shamt != 0 || rd != rt)
							throw new MIPSException("Invalid instruction: malformed CLZ");
						gpRegisters.set(rd, Integer.numberOfLeadingZeros(gpRegisters.read(rs)));
						break;
					}

					case 0b100001: // CLO
					{
						if (shamt != 0 || rd != rt)
							throw new MIPSException("Invalid instruction: malformed CLO");
						// Apparently Java doesn't have a corresponding function
						// for counting ones... not a problem, though ;)
						gpRegisters.set(rd, Integer.numberOfLeadingZeros(~gpRegisters.read(rs)));
						break;
					}
				}
				break;
			}
			//</editor-fold>

			//<editor-fold defaultstate="collapsed" desc="MEMORY INSTRUCTIONS">
			//TODO KMODE!
			case 0b100000: // LB
				try {
					// Do sign extend
					gpRegisters.set(rt, memory.read(gpRegisters.read(rs) + seimm(instruction), Memory.Boundary.BYTE, true) << 24 >> 24);
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
				break;

			case 0b100001: // LH
				try {
					// Do sign extend
					gpRegisters.set(rt, memory.read(gpRegisters.read(rs) + seimm(instruction), Memory.Boundary.HALF_WORD, true) << 16 >> 16);
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
				break;

			case 0b100010: // LWL
			{
				int address = gpRegisters.read(rs) + seimm(instruction);
				int word;
				try {
					word = memory.read(address & 0xFFFFFFFC, Memory.Boundary.WORD, false);
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}

				int bitOffset = ((address & 0b11) ^ 0b11) << 3;
				gpRegisters.set(rt, (word << bitOffset) | (gpRegisters.read(rt) & ((-1 << bitOffset) ^ -1)));
			}
			break;

			case 0b100011: // LW
				try {
					gpRegisters.set(rt, memory.read(gpRegisters.read(rs) + seimm(instruction), Memory.Boundary.WORD, false));
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
				break;

			case 0b100100: // LBU
				try {
					gpRegisters.set(rt, memory.read(gpRegisters.read(rs) + seimm(instruction), Memory.Boundary.BYTE, false));
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
				break;

			case 0b100101: // LHU
				try {
					gpRegisters.set(rt, memory.read(gpRegisters.read(rs) + seimm(instruction), Memory.Boundary.HALF_WORD, false));
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
				break;

			case 0b100110: // LWR
			{
				int address = gpRegisters.read(rs) + seimm(instruction);
				int word;
				try {
					word = memory.read(address & 0xFFFFFFFC, Memory.Boundary.WORD, false);
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}

				int bitOffset = (address & 0b11) << 3;
				gpRegisters.set(rt, (word >>> bitOffset) | (gpRegisters.read(rt) & ((-1 >>> bitOffset) ^ -1)));
			}
			break;

			case 0b101000: // SB
				try {
					memory.write(this, gpRegisters.read(rs) + seimm(instruction), gpRegisters.read(rt), Memory.Boundary.BYTE);
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
				break;

			case 0b101001: // SH
				try {
					memory.write(this, gpRegisters.read(rs) + seimm(instruction), gpRegisters.read(rt), Memory.Boundary.HALF_WORD);
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
				break;

			case 0b101010: // SWL
			{
				int address = gpRegisters.read(rs) + seimm(instruction);
				try {
					int word = memory.read(address & 0xFFFFFFFC, Memory.Boundary.WORD, false);
					int bitOffset = ((address & 0b11) ^ 0b11) << 3;
					memory.write(this, address & 0xFFFFFFFC,
							(gpRegisters.read(rt) >>> bitOffset) | (word & ((-1 >>> bitOffset) ^ -1)),
							Memory.Boundary.WORD);
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
			}
			break;

			case 0b101011: // SW
				try {
					memory.write(this, gpRegisters.read(rs) + seimm(instruction), gpRegisters.read(rt), Memory.Boundary.WORD);
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
				break;

			case 0b101110: // SWR
			{
				int address = gpRegisters.read(rs) + seimm(instruction);
				try {
					int word = memory.read(address & 0xFFFFFFFC, Memory.Boundary.WORD, false);
					int bitOffset = (address & 0b11) << 3;
					memory.write(this, address & 0xFFFFFFFC,
							(gpRegisters.read(rt) << bitOffset) | (word & ((-1 << bitOffset) ^ -1)),
							Memory.Boundary.WORD);
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
			}
			break;

			case 0b110000: // LL
			{
				try {
					int address = gpRegisters.read(rs) + seimm(instruction);
					gpRegisters.set(rt, memory.read(address, Memory.Boundary.WORD, false));
					coprocessor0.beginRMW(address);
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
			}

			case 0b111000: // SC
			{
				int address = gpRegisters.read(rs) + seimm(instruction);
				//(TODO)IMPLNOTE: address is corrected beforehand to avoid exceptions,
				// the alignment check is inside Memory.write(...)
				// Must rethink the step sequence...
				if (coprocessor0.isSynchronized(address & -2)) try {
					memory.write(this, address, gpRegisters.read(rt), Memory.Boundary.WORD);
					gpRegisters.set(rt, 1);
				}
				catch (AddressErrorException ex) {
					throw new MIPSException("Invalid memory address", ex);
				}
				else gpRegisters.set(rt, 0);
				break;
			}
			//</editor-fold>

			default:
				throw new MIPSException("Invalid opcode:" + opcode(instruction));
		}

		// TODO Test Handle branching status
		// DOC: MIPS specification pre-release6 defines a CTI in
		// a delay slot as UNPREDICTABLE behaviour, thus I will leave it
		// as-is for now; do note that MIPSRel6 does trigger a
		// ReservedInstructionException
		// ---------------------------------------------------------------------
		// (AP, 20170219)
		switch (branchState) {
			case BRANCH:
				branchState = MIPSMachine.DelayState.IDLE;
				setProgramCounter(getProgramCounter() + delayArgument);
				break;
			case JUMP:
				branchState = MIPSMachine.DelayState.IDLE;
				setProgramCounter((getProgramCounter() & 0xF0000000) | delayArgument);
				break;
			case DELAYED_B:
				branchState = MIPSMachine.DelayState.BRANCH;
				incrementPC();
				break;
			case DELAYED_J:
				branchState = MIPSMachine.DelayState.JUMP;
				incrementPC();
				break;
			case IDLE:
				incrementPC();
				break;
		}
	}

	// TODO test impl
	// offset correction is done here, all branches involve an immediate field to be shifted
	final void doBranch(int offset) {
		branchState = BooleanSettings.DELAYED_BRANCHING.isSet()
				? MIPSMachine.DelayState.DELAYED_B
				: MIPSMachine.DelayState.BRANCH;
		delayArgument = offset << 2;
	}

	// TODO test impl
	// corrections must be done beforehand
	final void doJump(int instrAddress) {
		branchState = BooleanSettings.DELAYED_BRANCHING.isSet()
				? MIPSMachine.DelayState.DELAYED_J
				: MIPSMachine.DelayState.JUMP;
		delayArgument = instrAddress;
	}

	/**
	 * Handler of the "andLink" instruction variants, sets the given register
	 * (usually $ra) to program counter of next instruction; if delayed
	 * branching is enabled the value is corrected by 4.
	 *
	 * @param m
	 */
	final void andLink(int regNumber) {
		gpRegisters.set(regNumber, getProgramCounter()
				+ (BooleanSettings.DELAYED_BRANCHING.isSet() ? 8 : 4));
	}


	/**
	 * Models the memory configuration for the simulated MIPS machine.
	 * "configuration" refers to the starting memory addresses for the various
	 * memory segments. The default configuration is based on SPIM. Starting
	 * with MARS 3.7, the configuration can be changed.
	 *
	 * @implSpec
	 *
	 * Base addresses listed here must be aligned on word boundary (i.e
	 * multiples of 4), whereas limit addresses align on a word's last byte,
	 * except the stack limit which is word aligned.
	 *
	 * @author Pete Sanderson
	 * @version August 2009
	 */
	public static class Configuration {

		// Default configuration comes from SPIM
		private static final int[] defaultConfigValues = {
			0x00400000, // .text Base Address
			0x10000000, // Data Segment base address
			0x10000000, // .extern Base Address
			0x10008000, // Global Pointer $gp)
			0x10010000, // .data base Address
			0x10040000, // heap base address
			0x7fffeffc, // stack pointer $sp (from SPIM not MIPS)
			0x7ffffffc, // stack base address
			0x7fffffff, // highest address in user space
			0x80000000, // lowest address in kernel space
			0x80000000, // .ktext base address
			0x80000180, // exception handler address
			0x90000000, // .kdata base address
			0xffff0000, // MMIO base address
			0xffffffff, // highest address in kernel (and memory)
			0x7fffffff, // data segment limit address
			0x0ffffffc, // text limit address
			0xfffeffff, // kernel data segment limit address
			0x8ffffffc, // kernel text limit address
			0x10040000, // stack limit address
			0xffffffff // memory map limit address
		};

		// Compact allows 16 bit addressing, data segment starts at 0
		private static final int[] dataCompactConfigValues = {
			0x00003000, // .text Base Address
			0x00000000, // Data Segment base address
			0x00001000, // .extern Base Address
			0x00001800, // Global Pointer $gp)
			0x00000000, // .data base Address
			0x00002000, // heap base address
			0x00002ffc, // stack pointer $sp 
			0x00002ffc, // stack base address
			0x00003fff, // highest address in user space
			0x00004000, // lowest address in kernel space
			0x00004000, // .ktext base address
			0x00004180, // exception handler address
			0x00005000, // .kdata base address
			0x00007f00, // MMIO base address
			0x00007fff, // highest address in kernel (and memory)
			0x00002fff, // data segment limit address
			0x00003ffc, // text limit address
			0x00007eff, // kernel data segment limit address
			0x00004ffc, // kernel text limit address
			0x00002000, // stack limit address
			0x00007fff // memory map limit address
		};

		// Compact allows 16 bit addressing, text segment starts at 0
		private static final int[] textCompactConfigValues = {
			0x00000000, // .text Base Address
			0x00001000, // Data Segment base address
			0x00001000, // .extern Base Address
			0x00001800, // Global Pointer $gp)
			0x00002000, // .data base Address
			0x00003000, // heap base address
			0x00003ffc, // stack pointer $sp 
			0x00003ffc, // stack base address
			0x00003fff, // highest address in user space
			0x00004000, // lowest address in kernel space
			0x00004000, // .ktext base address
			0x00004180, // exception handler address
			0x00005000, // .kdata base address
			0x00007f00, // MMIO base address
			0x00007fff, // highest address in kernel (and memory)
			0x00003fff, // data segment limit address
			0x00000ffc, // text limit address
			0x00007eff, // kernel data segment limit address
			0x00004ffc, // kernel text limit address
			0x00003000, // stack limit address
			0x00007fff // memory map limit address
		};

		// Identifier is used for saving setting; name is used for display
		private final String identifier;
		private final String name;
		private final int[] addresses;

		private Configuration(String id, String name, int[] values) {
			super();
			identifier = id;
			this.name = name;
			addresses = values;
		}

		public String getID() {
			return identifier;
		}

		public String getName() {
			return name;
		}

		public int[] getAddresses() {
			return Arrays.copyOf(addresses, addresses.length);
		}

		public int getAddress(Memory.Descriptor d) {
			return addresses[d.ordinal()];
		}
	}

	static enum DelayState {
		IDLE,
		DELAYED_B,
		BRANCH,
		DELAYED_J,
		JUMP
	}


	////////////////////////////////////////////////////////////////////////////
	// TEST SECTION ------------------------------------------------------------
	public static void main(String[] args) throws AddressErrorException {
		testSWR();
	}

	static final void testSWR() throws AddressErrorException {
		Main.symbolTable = new SymbolTable("global");
		MIPSMachine m = new MIPSMachine();
		int userSpace = m.configuration().getAddress(Memory.Descriptor.DATA_BASE_ADDRESS);
		
		m.gpRegisters.setUserRegister(Registers.Descriptor.$t0, userSpace);
		m.memory.write(m, userSpace, 0x55120873, Memory.Boundary.WORD);
		int readWord = m.memory.read(userSpace, Memory.Boundary.WORD, false);
		System.out.println(Integer.toHexString(readWord));

		m.gpRegisters.setUserRegister(Registers.Descriptor.$t1, 0xFD327593);
		System.out.println(Integer.toHexString(m.gpRegisters.read(Registers.Descriptor.$t1.ordinal())));

		
		Main.initialize();
		int[] s = new int[]{Registers.Descriptor.$t1.ordinal(), 1, Registers.Descriptor.$t0.ordinal()};
		int instruction = InstructionSetArchitecture.BasicInstructionEncodings.get("swr").applyAsInt(s);
		System.out.println(Integer.toBinaryString(instruction));

		m.executeInstruction(instruction);

		System.out.println(Integer.toHexString(m.memory.read(userSpace, Memory.Boundary.WORD, false)));
	}

//	static final int genLoadStoreIinstr(int[] s, int opcode) {
//		// TODO Pass a SHORT as imm
//		// Extract operands
//		// NOTE ProgramStatement.getOperands return an array of
//		// register references, namely rs, rt and imm in order
//		int[] operands = s;
//		int rt = operands[0];
//		int imm = operands[1];
//		int rs = operands[2];
//
//		// TODO must assure operands are less than 32 | 0xFFFF!!!
//		assert (rs < 32 && rt < 32 && imm <= 0xFFFF);
//
//		// Align regnums
//		rs = rs << 21;
//		rt = rt << 16;
//
//		// Compose binary instruction
//		return opcode | rs | rt | imm;
//	}
}
