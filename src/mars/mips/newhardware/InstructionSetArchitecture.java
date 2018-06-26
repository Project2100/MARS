/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.mips.newhardware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import mars.Main;
import mars.ProcessingException;
import mars.ProgramStatement;
import mars.mips.instructions.ExtendedInstruction;
import mars.mips.instructions.Instruction;
import mars.simulator.Exceptions;

/*
 Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar

 Developed by Pete Sanderson (psanderson@otterbein.edu)
 and Kenneth Vollmar (kenvollmar@missouristate.edu)

 Permission is hereby granted, free of charge, to any person obtaining 
 a copy of this software and associated documentation files (the 
 "Software"), to deal in the Software without restriction, including 
 without limitation the rights to use, copy, modify, merge, publish, 
 distribute, sublicense, and/or sell copies of the Software, and to 
 permit persons to whom the Software is furnished to do so, subject 
 to the following conditions:

 The above copyright notice and this permission notice shall be 
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 (MIT license, http://www.opensource.org/licenses/mit-license.html)
 */
/**
 * The list of Instruction objects, each of which represents a MIPS instruction.
 * The instruction may either be basic (translates into binary machine code) or
 * extended (translates into sequence of one or more basic instructions).
 *
 * @author Pete Sanderson and Ken Vollmar
 * @version August 2003-5
 */
public class InstructionSetArchitecture {

	/**
	 * Length in bytes of a machine instruction. MIPS is a RISC architecture so
	 * all instructions are the same length. Currently set to 4.
	 */
	public static final int INSTRUCTION_BYTES = 4;

	// ###### fffff sssss 00000 00000 ######
	static final int genZrdshamtRinstr(ProgramStatement s, int function) {
		// Extract register numbers
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rd, rs and rt in order
		int[] operands = s.getOperands();
		int rs = operands[0];
		int rt = operands[1];

		// TODO must assure operands are less than 32!!!
		assert (rs < 32 && rt < 32);

		// Align regnums
		rs = rs << 21;
		rt = rt << 16;

		// Compose binary instruction - asserting shamt = 0
		return function | rs | rt;
	}

	// ###### sssss ttttt fffff 00000 ######
	static final int genZshamtRinstr(ProgramStatement s, int function) {
		// Extract register numbers
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rd, rs and rt in order
		int[] operands = s.getOperands();
		int rd = operands[0];
		int rs = operands[1];
		int rt = operands[2];

		// TODO must assure operands are less than 32!!!
		assert (rs < 32 && rt < 32 && rd < 32);

		// Align regnums
		rs = rs << 21;
		rt = rt << 16;
		rd = rd << 11;

		// Compose binary instruction - asserting shamt = 0
		return function | rs | rt | rd;
	}

	// ###### 00000 sssss fffff ttttt ######
	static final int genZrsRinstr(ProgramStatement s, int function) {
		// Extract register numbers
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rd, rt and shamt in order
		int[] operands = s.getOperands();
		int rd = operands[0];
		int rt = operands[1];
		int shamt = operands[2];

		// TODO must assure operands are less than 32!!!
		assert (rt < 32 && shamt < 32 && rd < 32);

		// Align regnums
		rt = rt << 16;
		rd = rd << 11;
		shamt = shamt << 6;

		// Compose binary instruction - asserting rs = 0
		return function | rt | rd | shamt;
	}

	// ###### sssss fffff tttttttttttttttt
	static final int genArithmIinstr(ProgramStatement s, int opcode) {
		// TODO Pass a SHORT as imm
		// Extract operands
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rs, rt and imm in order
		int[] operands = s.getOperands();
		int rt = operands[0];
		int rs = operands[1];
		int imm = operands[2];

		// TODO must assure operands are less than 32 | 0xFFFF!!!
		assert (rs < 32 && rt < 32 && imm <= 0xFFFF);

		// Align regnums
		rs = rs << 21;
		rt = rt << 16;

		// Compose binary instruction
		return opcode | rs | rt | imm;
	}

	// ###### ttttt fffff ssssssssssssssss
	static final int genLoadStoreIinstr(ProgramStatement s, int opcode) {
		// TODO Pass a SHORT as imm
		// Extract operands
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rs, rt and imm in order
		int[] operands = s.getOperands();
		int rt = operands[0];
		int imm = operands[1];
		int rs = operands[2];

		// TODO must assure operands are less than 32 | 0xFFFF!!!
		assert (rs < 32 && rt < 32 && imm <= 0xFFFF);

		// Align regnums
		rs = rs << 21;
		rt = rt << 16;

		// Compose binary instruction
		return opcode | rs | rt | imm;
	}

	// ###### fffff ##### ssssssssssssssss
	static final int genBranchFuncIinstr(ProgramStatement s, int opfunc) {
		// TODO Pass a SHORT as imm
		// Extract operands
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rs, rt and imm in order
		int[] operands = s.getOperands();
		int imm = operands[1];
		int rs = operands[0];

		// TODO must assure operands are less than 32 | 0xFFFF!!!
		assert (rs < 32 && imm <= 0xFFFF);

		// Align regnums
		rs = rs << 21;

		// Compose binary instruction
		return opfunc | rs | imm;
	}

	// ###### fffff sssss tttttttttttttttt
	static final int genBranchIinstr(ProgramStatement s, int opcode) {
		// TODO Pass a SHORT as imm
		// Extract operands
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rs, rt and imm in order
		int[] operands = s.getOperands();
		int rs = operands[0];
		int rt = operands[1];
		int imm = operands[2];

		// TODO must assure operands are less than 32 | 0xFFFF!!!
		assert (rs < 32 && rt < 32 && imm <= 0xFFFF);

		// Align regnums
		rs = rs << 21;
		rt = rt << 16;

		// Compose binary instruction
		return opcode | rs | rt | imm;
	}

	// isFrom =  true -> 000000 00000 00000 fffff 00000 #####x
	//          false -> 000000 fffff 00000 00000 00000 #####x
	static final int genHiLoRinstr(ProgramStatement s, int function) {
		int reg = s.getOperands()[0];

		assert (reg < 32);

		// Last bit (marked with x) decides if instruction moves "from" or "to"
		return function | (reg << ((function & 1) == 0 ? 11 : 21));
	}

	// ###### ffffffffffffffffffffffffff
	static final int genJinstr(ProgramStatement s, int opcode) {
		int reg = s.getOperands()[0];
		assert (reg < 0x04000000);
		return opcode | reg;
	}

	// NOTE: rs field repeated in rt as per spec, basically a zshamt...
	// ###### sssss fffff fffff 00000 ######
	static final int genCountBitsRinstr(ProgramStatement s, int opfunc) {
		// Extract register numbers
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rd, rs and rt in order
		int[] operands = s.getOperands();
		int rd = operands[0];
		int rs = operands[1];
		int rt = operands[1];

		// TODO must assure operands are less than 32!!!
		assert (rs < 32 && rt < 32 && rd < 32);

		// Align regnums
		rs = rs << 21;
		rt = rt << 16;
		rd = rd << 11;

		// Compose binary instruction - asserting shamt = 0
		return opfunc | rs | rt | rd;
	}

	final Map<String, ToIntFunction<ProgramStatement>> BasicInstructionEncodings;
	final List<ExtendedInstruction> pseudoInstructions;

	/**
	 * Creates a new InstructionSet object.
	 */
	public InstructionSetArchitecture() {
		// Counting ~90 implemented instructions as of 170314 - AP
		BasicInstructionEncodings = new HashMap<>(100);
		// Counting 378 pseudos as of 170314 - AP
		pseudoInstructions = new ArrayList<>(400);
	}

	/**
	 * Adds all instructions to the set. A given extended instruction may have
	 * more than one Instruction object, depending on how many formats it can
	 * have.
	 *
	 * @see Instruction
	 * @see BasicInstruction
	 * @see ExtendedInstruction
	 */
	public void populate() {
		// The parade begins...

		BasicInstructionEncodings.put("nop", (statement) -> 0);
		// ...hey, I like this so far!

		// SHIFT OPERATIONS ----------------------------------------------------
		BasicInstructionEncodings.put("sll", (statement) -> genZrsRinstr(statement, 0x00000000));
		BasicInstructionEncodings.put("srl", (statement) -> genZrsRinstr(statement, 0x00000002));
		BasicInstructionEncodings.put("sra", (statement) -> genZrsRinstr(statement, 0x00000003));
		BasicInstructionEncodings.put("sllv", (statement) -> genZshamtRinstr(statement, 0x00000004));
		BasicInstructionEncodings.put("srlv", (statement) -> genZshamtRinstr(statement, 0x00000006));
		BasicInstructionEncodings.put("srav", (statement) -> genZshamtRinstr(statement, 0x00000007));

		// ARITHMETIC-LOGIC INSTRUCTIONS ---------------------------------------
		// R-TYPE
		BasicInstructionEncodings.put("mult", (statement) -> genZrdshamtRinstr(statement, 0x00000018));
		BasicInstructionEncodings.put("multu", (statement) -> genZrdshamtRinstr(statement, 0x00000019));
		BasicInstructionEncodings.put("div", (statement) -> genZrdshamtRinstr(statement, 0x0000001A));
		BasicInstructionEncodings.put("divu", (statement) -> genZrdshamtRinstr(statement, 0x0000001B));

		BasicInstructionEncodings.put("madd", (statement) -> genZrdshamtRinstr(statement, 0x70000000));
		BasicInstructionEncodings.put("maddu", (statement) -> genZrdshamtRinstr(statement, 0x70000001));
		BasicInstructionEncodings.put("mul", (statement) -> genZshamtRinstr(statement, 0x70000002));
		BasicInstructionEncodings.put("msub", (statement) -> genZrdshamtRinstr(statement, 0x70000004));
		BasicInstructionEncodings.put("msubu", (statement) -> genZrdshamtRinstr(statement, 0x70000005));

		BasicInstructionEncodings.put("add", (statement) -> genZshamtRinstr(statement, 0x00000020));
		BasicInstructionEncodings.put("addu", (statement) -> genZshamtRinstr(statement, 0x00000021));
		BasicInstructionEncodings.put("sub", (statement) -> genZshamtRinstr(statement, 0x00000022));
		BasicInstructionEncodings.put("subu", (statement) -> genZshamtRinstr(statement, 0x00000023));
		BasicInstructionEncodings.put("and", (statement) -> genZshamtRinstr(statement, 0x00000024));
		BasicInstructionEncodings.put("or", (statement) -> genZshamtRinstr(statement, 0x00000025));
		BasicInstructionEncodings.put("xor", (statement) -> genZshamtRinstr(statement, 0x00000026));
		BasicInstructionEncodings.put("nor", (statement) -> genZshamtRinstr(statement, 0x00000027));

		BasicInstructionEncodings.put("clz", (statement) -> genCountBitsRinstr(statement, 0x70000020));
		BasicInstructionEncodings.put("clo", (statement) -> genCountBitsRinstr(statement, 0x70000021));

		// I-TYPE
		BasicInstructionEncodings.put("addi", (statement) -> genArithmIinstr(statement, 0x20000000));
		BasicInstructionEncodings.put("addiu", (statement) -> genArithmIinstr(statement, 0x24000000));
		BasicInstructionEncodings.put("andi", (statement) -> genArithmIinstr(statement, 0x30000000));
		BasicInstructionEncodings.put("ori", (statement) -> genArithmIinstr(statement, 0x34000000));
		BasicInstructionEncodings.put("xori", (statement) -> genArithmIinstr(statement, 0x38000000));

		//###### 00000 fffff ssssssssssssssss
		BasicInstructionEncodings.put("lui", (statement -> {
			int[] operands = statement.getOperands();
			return 0x3C000000 | (operands[0] << 16) | operands[1];
		}));

		// SET INSTRUCTIONS ----------------------------------------------------
		// R-TYPE
		BasicInstructionEncodings.put("slt", (statement) -> genZshamtRinstr(statement, 0x0000002A));
		BasicInstructionEncodings.put("sltu", (statement) -> genZshamtRinstr(statement, 0x0000002B));

		// I-TYPE
		BasicInstructionEncodings.put("slti", (statement) -> genArithmIinstr(statement, 0x28000000));
		BasicInstructionEncodings.put("sltiu", (statement) -> genArithmIinstr(statement, 0x2C000000));

		// MOVE INSTRUCTIONS ---------------------------------------------------
		// R-TYPE
		BasicInstructionEncodings.put("movz", (statement) -> genZshamtRinstr(statement, 0x0000000A));
		BasicInstructionEncodings.put("movn", (statement) -> genZshamtRinstr(statement, 0x0000000B));
		BasicInstructionEncodings.put("mfhi", (statement) -> genHiLoRinstr(statement, 0x00000010));
		BasicInstructionEncodings.put("mflo", (statement) -> genHiLoRinstr(statement, 0x00000012));
		BasicInstructionEncodings.put("mthi", (statement) -> genHiLoRinstr(statement, 0x00000011));
		BasicInstructionEncodings.put("mtlo", (statement) -> genHiLoRinstr(statement, 0x00000013));

		// MEMORY INSTRUCTIONS | I-TYPE ----------------------------------------
		BasicInstructionEncodings.put("lb", (statement) -> genLoadStoreIinstr(statement, 0x80000000));
		BasicInstructionEncodings.put("lh", (statement) -> genLoadStoreIinstr(statement, 0x84000000));
		BasicInstructionEncodings.put("lwl", (statement) -> genLoadStoreIinstr(statement, 0x88000000));
		BasicInstructionEncodings.put("lw", (statement) -> genLoadStoreIinstr(statement, 0x8C000000));
		BasicInstructionEncodings.put("lbu", (statement) -> genLoadStoreIinstr(statement, 0x90000000));
		BasicInstructionEncodings.put("lhu", (statement) -> genLoadStoreIinstr(statement, 0x94000000));
		BasicInstructionEncodings.put("lwr", (statement) -> genLoadStoreIinstr(statement, 0x98000000));

		BasicInstructionEncodings.put("sb", (statement) -> genLoadStoreIinstr(statement, 0xA0000000));
		BasicInstructionEncodings.put("sh", (statement) -> genLoadStoreIinstr(statement, 0xA4000000));
		BasicInstructionEncodings.put("swl", (statement) -> genLoadStoreIinstr(statement, 0xA8000000));
		BasicInstructionEncodings.put("sw", (statement) -> genLoadStoreIinstr(statement, 0xAC000000));
		BasicInstructionEncodings.put("swr", (statement) -> genLoadStoreIinstr(statement, 0xB8000000));

		BasicInstructionEncodings.put("ll", (statement) -> genLoadStoreIinstr(statement, 0xC0000000));
		BasicInstructionEncodings.put("sc", (statement) -> genLoadStoreIinstr(statement, 0xE0000000));

		// BRANCH INSTRUCIONS --------------------------------------------------
		// I-TYPE
		BasicInstructionEncodings.put("beq", (statement) -> genBranchIinstr(statement, 0x10000000));
		BasicInstructionEncodings.put("bne", (statement) -> genBranchIinstr(statement, 0x14000000));
		BasicInstructionEncodings.put("bgez", (statement) -> genBranchFuncIinstr(statement, 0x04010000));
		BasicInstructionEncodings.put("bgezal", (statement) -> genBranchFuncIinstr(statement, 0x04110000));
		BasicInstructionEncodings.put("bgtz", (statement) -> genBranchFuncIinstr(statement, 0x1C000000));
		BasicInstructionEncodings.put("blez", (statement) -> genBranchFuncIinstr(statement, 0x18000000));
		BasicInstructionEncodings.put("bltz", (statement) -> genBranchFuncIinstr(statement, 0x04000000));
		BasicInstructionEncodings.put("bltzal", (statement) -> genBranchFuncIinstr(statement, 0x04100000));

		// JUMP INSTRUCIONS ----------------------------------------------------
		// J-TYPE
		BasicInstructionEncodings.put("j", (statement) -> genJinstr(statement, 0x08000000));
		BasicInstructionEncodings.put("jal", (statement) -> genJinstr(statement, 0x0C000000));

		// R-TYPE
		// ###### fffff 00000 00000 00000 ######
		BasicInstructionEncodings.put("jr", (statement) -> {
			int reg = statement.getOperands()[0];
			assert (reg < 32);
			return 0x00000008 | (reg << 21);
		});
		// ###### sssss 00000 fffff 00000 ######
		// ###### fffff 00000 11111 00000 ######
		BasicInstructionEncodings.put("jalr", (statement) -> {
			//TODO
			int[] ops = statement.getOperands();
			int rs = ops.length == 1 ? ops[0] : ops[1];
			int rd = ops.length == 1 ? 0b11111 : ops[0];
			assert (rs < 32 && rd < 32);
			return 0x00000009 | rs << 21 | rd << 11;
		});

		// EXCEPTION INSTRUCTIONS ----------------------------------------------
		// R-TYPE
		BasicInstructionEncodings.put("tge", (statement) -> genZrdshamtRinstr(statement, 0x00000030));
		BasicInstructionEncodings.put("tgeu", (statement) -> genZrdshamtRinstr(statement, 0x00000031));
		BasicInstructionEncodings.put("tlt", (statement) -> genZrdshamtRinstr(statement, 0x00000032));
		BasicInstructionEncodings.put("tltu", (statement) -> genZrdshamtRinstr(statement, 0x00000033));
		BasicInstructionEncodings.put("teq", (statement) -> genZrdshamtRinstr(statement, 0x00000034));
		BasicInstructionEncodings.put("tne", (statement) -> genZrdshamtRinstr(statement, 0x00000036));

		// I-TYPE
		BasicInstructionEncodings.put("tgei", (statement) -> genBranchFuncIinstr(statement, 0x40080000));
		BasicInstructionEncodings.put("tgeiu", (statement) -> genBranchFuncIinstr(statement, 0x40090000));
		BasicInstructionEncodings.put("tlti", (statement) -> genBranchFuncIinstr(statement, 0x400A0000));
		BasicInstructionEncodings.put("tltiu", (statement) -> genBranchFuncIinstr(statement, 0x400B0000));
		BasicInstructionEncodings.put("teqi", (statement) -> genBranchFuncIinstr(statement, 0x400C0000));
		BasicInstructionEncodings.put("tnei", (statement) -> genBranchFuncIinstr(statement, 0x400E0000));

		// R-TYPE SPECIAL
		BasicInstructionEncodings.put("syscall", (statement) -> 0x0000000C);
		// 000000 oooooooooooooooooooo 001101
		BasicInstructionEncodings.put("break", (statement) -> {
			int[] ops = statement.getOperands();
			// Translates both the valued and unvalued variants, the boilerplate checks should disappear...
			// Expected assert is missing
			return 0x0000000D | ((ops != null && ops.length != 0) ? ops[0] : 0);
		});

		// CP0 INSTRUCTIONS ----------------------------------------------------
		BasicInstructionEncodings.put("eret", (statement) -> 0x42000018);

		// OTHERS
		// 000000 sssss ttt 00 fffff 00000 000001
		BasicInstructionEncodings.put("movf", (statement) -> {
			int i = 0;

			// GEtting operands
			int[] operands = statement.getOperands();

			i |= (operands[0] << 11);
			i |= (operands[1] << 21);
			i |= (operands[2] << 18);

			// Setting MOVCI in func field
			return i | 0x00000001;
		});

		// 000000 sssss ttt 01 fffff 00000 000001
		BasicInstructionEncodings.put("movt", (statement) -> {
			int i = 0;

			// GEtting operands
			int[] operands = statement.getOperands();

			i |= (operands[0] << 11);
			i |= (operands[1] << 21);
			i |= (operands[2] << 18);

			// Setting MOVCI in func field
			return i | 0x00010001;
		});

		////////////////////////////////////////////////////////////////////////
//        instructionSet.add(
//                new BasicInstruction("mfc0 $t1,$8",
//                        "Move from Coprocessor 0 : Set $t1 to the value stored in Coprocessor 0 register $8",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010000 00000 fffff sssss 00000 000000",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        RegisterFile.updateRegister(operands[0],
//                                Coprocessor0.getValue(operands[1]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("mtc0 $t1,$8",
//                        "Move to Coprocessor 0 : Set Coprocessor 0 register $8 to value stored in $t1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010000 00100 fffff sssss 00000 000000",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        Coprocessor0.updateRegister(operands[1],
//                                RegisterFile.getValue(operands[0]));
//                    }
//                }));
		/////////////////////// Floating Point Instructions Start Here ////////////////
//        instructionSet.add(
//                new BasicInstruction("add.s $f0,$f1,$f3",
//                        "Floating point addition single precision : Set $f0 to single-precision floating point value of $f1 plus $f3",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 ttttt sssss fffff 000000",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float add1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        float add2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[2]));
//                        float sum = add1 + add2;
//                        // overflow detected when sum is positive or negative infinity.
//                        /*
//                                 if (sum == Float.NEGATIVE_INFINITY || sum == Float.POSITIVE_INFINITY) {
//                                 throw new ProcessingException(statement,"arithmetic overflow");
//                                 }
//                         */
//                        Coprocessor1.updateRegister(operands[0], Float.floatToIntBits(sum));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("sub.s $f0,$f1,$f3",
//                        "Floating point subtraction single precision : Set $f0 to single-precision floating point value of $f1  minus $f3",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 ttttt sssss fffff 000001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float sub1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        float sub2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[2]));
//                        float diff = sub1 - sub2;
//                        Coprocessor1.updateRegister(operands[0], Float.floatToIntBits(diff));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("mul.s $f0,$f1,$f3",
//                        "Floating point multiplication single precision : Set $f0 to single-precision floating point value of $f1 times $f3",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 ttttt sssss fffff 000010",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float mul1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        float mul2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[2]));
//                        float prod = mul1 * mul2;
//                        Coprocessor1.updateRegister(operands[0], Float.floatToIntBits(prod));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("div.s $f0,$f1,$f3",
//                        "Floating point division single precision : Set $f0 to single-precision floating point value of $f1 divided by $f3",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 ttttt sssss fffff 000011",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float div1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        float div2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[2]));
//                        float quot = div1 / div2;
//                        Coprocessor1.updateRegister(operands[0], Float.floatToIntBits(quot));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("sqrt.s $f0,$f1",
//                        "Square root single precision : Set $f0 to single-precision floating point square root of $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 00000 sssss fffff 000100",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float value = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        int floatSqrt = 0;
//                        if (value < 0.0f)
//                            // This is subject to refinement later.  Release 4.0 defines floor, ceil, trunc, round
//                            // to act silently rather than raise Invalid Operation exception, so sqrt should do the
//                            // same.  An intermediate step would be to define a setting for FCSR Invalid Operation
//                            // flag, but the best solution is to simulate the FCSR register itself.
//                            // FCSR = Floating point unit Control and Status Register.  DPS 10-Aug-2010
//                            floatSqrt = Float.floatToIntBits(Float.NaN); //throw new ProcessingException(statement, "Invalid Operation: sqrt of negative number");
//                        else
//                            floatSqrt = Float.floatToIntBits((float) Math.sqrt(value));
//                        Coprocessor1.updateRegister(operands[0], floatSqrt);
//                    }
//                }));
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
//        instructionSet.add(
//                new BasicInstruction("add.d $f2,$f4,$f6",
//                        "Floating point addition double precision : Set $f2 to double-precision floating point value of $f4 plus $f6",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 ttttt sssss fffff 000000",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1 || operands[2] % 2 == 1)
//                            throw new ProcessingException(statement, "all registers must be even-numbered");
//                        double add1 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        double add2 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[2] + 1), Coprocessor1.getValue(operands[2])));
//                        double sum = add1 + add2;
//                        long longSum = Double.doubleToLongBits(sum);
//                        Coprocessor1.updateRegister(operands[0] + 1, Binary.highOrderLongToInt(longSum));
//                        Coprocessor1.updateRegister(operands[0], Binary.lowOrderLongToInt(longSum));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("sub.d $f2,$f4,$f6",
//                        "Floating point subtraction double precision : Set $f2 to double-precision floating point value of $f4 minus $f6",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 ttttt sssss fffff 000001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1 || operands[2] % 2 == 1)
//                            throw new ProcessingException(statement, "all registers must be even-numbered");
//                        double sub1 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        double sub2 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[2] + 1), Coprocessor1.getValue(operands[2])));
//                        double diff = sub1 - sub2;
//                        long longDiff = Double.doubleToLongBits(diff);
//                        Coprocessor1.updateRegister(operands[0] + 1, Binary.highOrderLongToInt(longDiff));
//                        Coprocessor1.updateRegister(operands[0], Binary.lowOrderLongToInt(longDiff));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("mul.d $f2,$f4,$f6",
//                        "Floating point multiplication double precision : Set $f2 to double-precision floating point value of $f4 times $f6",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 ttttt sssss fffff 000010",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1 || operands[2] % 2 == 1)
//                            throw new ProcessingException(statement, "all registers must be even-numbered");
//                        double mul1 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        double mul2 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[2] + 1), Coprocessor1.getValue(operands[2])));
//                        double prod = mul1 * mul2;
//                        long longProd = Double.doubleToLongBits(prod);
//                        Coprocessor1.updateRegister(operands[0] + 1, Binary.highOrderLongToInt(longProd));
//                        Coprocessor1.updateRegister(operands[0], Binary.lowOrderLongToInt(longProd));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("div.d $f2,$f4,$f6",
//                        "Floating point division double precision : Set $f2 to double-precision floating point value of $f4 divided by $f6",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 ttttt sssss fffff 000011",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1 || operands[2] % 2 == 1)
//                            throw new ProcessingException(statement, "all registers must be even-numbered");
//                        double div1 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        double div2 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[2] + 1), Coprocessor1.getValue(operands[2])));
//                        double quot = div1 / div2;
//                        long longQuot = Double.doubleToLongBits(quot);
//                        Coprocessor1.updateRegister(operands[0] + 1, Binary.highOrderLongToInt(longQuot));
//                        Coprocessor1.updateRegister(operands[0], Binary.lowOrderLongToInt(longQuot));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("sqrt.d $f2,$f4",
//                        "Square root double precision : Set $f2 to double-precision floating point square root of $f4",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 00000 sssss fffff 000100",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1 || operands[2] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        double value = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        long longSqrt = 0;
//                        if (value < 0.0)
//                            // This is subject to refinement later.  Release 4.0 defines floor, ceil, trunc, round
//                            // to act silently rather than raise Invalid Operation exception, so sqrt should do the
//                            // same.  An intermediate step would be to define a setting for FCSR Invalid Operation
//                            // flag, but the best solution is to simulate the FCSR register itself.
//                            // FCSR = Floating point unit Control and Status Register.  DPS 10-Aug-2010
//                            longSqrt = Double.doubleToLongBits(Double.NaN); //throw new ProcessingException(statement, "Invalid Operation: sqrt of negative number");
//                        else
//                            longSqrt = Double.doubleToLongBits(Math.sqrt(value));
//                        Coprocessor1.updateRegister(operands[0] + 1, Binary.highOrderLongToInt(longSqrt));
//                        Coprocessor1.updateRegister(operands[0], Binary.lowOrderLongToInt(longSqrt));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("floor.w.d $f1,$f2",
//                        "Floor double precision to word : Set $f1 to 32-bit integer floor of double-precision float in $f2",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 00000 sssss fffff 001111",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "second register must be even-numbered");
//                        double doubleValue = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        // DPS 27-July-2010: Since MARS does not simulate the FSCR, I will take the default
//                        // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
//                        int floor = (int) Math.floor(doubleValue);
//                        if (Double.isNaN(doubleValue)
//                                || Double.isInfinite(doubleValue)
//                                || doubleValue < (double) Integer.MIN_VALUE
//                                || doubleValue > (double) Integer.MAX_VALUE)
//                            floor = Integer.MAX_VALUE;
//                        Coprocessor1.updateRegister(operands[0], floor);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("ceil.w.d $f1,$f2",
//                        "Ceiling double precision to word : Set $f1 to 32-bit integer ceiling of double-precision float in $f2",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 00000 sssss fffff 001110",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "second register must be even-numbered");
//                        double doubleValue = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        // DPS 27-July-2010: Since MARS does not simulate the FSCR, I will take the default
//                        // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
//                        int ceiling = (int) Math.ceil(doubleValue);
//                        if (Double.isNaN(doubleValue)
//                                || Double.isInfinite(doubleValue)
//                                || doubleValue < (double) Integer.MIN_VALUE
//                                || doubleValue > (double) Integer.MAX_VALUE)
//                            ceiling = Integer.MAX_VALUE;
//                        Coprocessor1.updateRegister(operands[0], ceiling);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("round.w.d $f1,$f2",
//                        "Round double precision to word : Set $f1 to 32-bit integer round of double-precision float in $f2",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 00000 sssss fffff 001100",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException { // See comments in round.w.s above, concerning MIPS and IEEE 754 standard. 
//                        // Until MARS 3.5, I used Math.round, which rounds to nearest but when both are
//                        // equal it rounds toward positive infinity.  With Release 3.5, I painstakingly
//                        // carry out the MIPS and IEEE 754 standard (round to nearest/even).
//                        int[] operands = statement.getOperands();
//                        if (operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "second register must be even-numbered");
//                        double doubleValue = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        int below = 0, above = 0;
//                        int round = (int) Math.round(doubleValue);
//                        // See comments in round.w.s above concerning FSCR...  
//                        if (Double.isNaN(doubleValue)
//                                || Double.isInfinite(doubleValue)
//                                || doubleValue < (double) Integer.MIN_VALUE
//                                || doubleValue > (double) Integer.MAX_VALUE)
//                            round = Integer.MAX_VALUE;
//                        else {
//                            Double doubleObj = new Double(doubleValue);
//                            // If we are EXACTLY in the middle, then round to even!  To determine this,
//                            // find next higher integer and next lower integer, then see if distances 
//                            // are exactly equal.
//                            if (doubleValue < 0.0) {
//                                above = doubleObj.intValue(); // truncates
//                                below = above - 1;
//                            }
//                            else {
//                                below = doubleObj.intValue(); // truncates
//                                above = below + 1;
//                            }
//                            if (doubleValue - below == above - doubleValue) // exactly in the middle?
//                                round = (above % 2 == 0) ? above : below;
//                        }
//                        Coprocessor1.updateRegister(operands[0], round);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("trunc.w.d $f1,$f2",
//                        "Truncate double precision to word : Set $f1 to 32-bit integer truncation of double-precision float in $f2",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 00000 sssss fffff 001101",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "second register must be even-numbered");
//                        double doubleValue = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        // DPS 27-July-2010: Since MARS does not simulate the FSCR, I will take the default
//                        // action of setting the result to 2^31-1, if the value is outside the 32 bit range.
//                        int truncate = (int) doubleValue; // Typecasting will round toward zero, the correct action.
//                        if (Double.isNaN(doubleValue)
//                                || Double.isInfinite(doubleValue)
//                                || doubleValue < (double) Integer.MIN_VALUE
//                                || doubleValue > (double) Integer.MAX_VALUE)
//                            truncate = Integer.MAX_VALUE;
//                        Coprocessor1.updateRegister(operands[0], truncate);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("bc1t label",
//                        "Branch if FP condition flag 0 true (BC1T, not BCLT) : If Coprocessor 1 condition flag 0 is true (one) then branch to statement at label's address",
//                        BasicInstructionFormat.I_BRANCH_FORMAT,
//                        "010001 01000 00001 ffffffffffffffff",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (Coprocessor1.getConditionFlag(0) == 1)
//                            processBranch(operands[0]);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("bc1t 1,label",
//                        "Branch if specified FP condition flag true (BC1T, not BCLT) : If Coprocessor 1 condition flag specified by immediate is true (one) then branch to statement at label's address",
//                        BasicInstructionFormat.I_BRANCH_FORMAT,
//                        "010001 01000 fff 01 ssssssssssssssss",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (Coprocessor1.getConditionFlag(operands[0]) == 1)
//                            processBranch(operands[1]);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("bc1f label",
//                        "Branch if FP condition flag 0 false (BC1F, not BCLF) : If Coprocessor 1 condition flag 0 is false (zero) then branch to statement at label's address",
//                        BasicInstructionFormat.I_BRANCH_FORMAT,
//                        "010001 01000 00000 ffffffffffffffff",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (Coprocessor1.getConditionFlag(0) == 0)
//                            processBranch(operands[0]);
//
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("bc1f 1,label",
//                        "Branch if specified FP condition flag false (BC1F, not BCLF) : If Coprocessor 1 condition flag specified by immediate is false (zero) then branch to statement at label's address",
//                        BasicInstructionFormat.I_BRANCH_FORMAT,
//                        "010001 01000 fff 00 ssssssssssssssss",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (Coprocessor1.getConditionFlag(operands[0]) == 0)
//                            processBranch(operands[1]);
//
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.eq.s $f0,$f1",
//                        "Compare equal single precision : If $f0 is equal to $f1, set Coprocessor 1 condition flag 0 true else set it false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 sssss fffff 00000 110010",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float op1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[0]));
//                        float op2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        if (op1 == op2)
//                            Coprocessor1.setConditionFlag(0);
//                        else
//                            Coprocessor1.clearConditionFlag(0);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.eq.s 1,$f0,$f1",
//                        "Compare equal single precision : If $f0 is equal to $f1, set Coprocessor 1 condition flag specied by immediate to true else set it to false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 ttttt sssss fff 00 11 0010",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float op1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        float op2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[2]));
//                        if (op1 == op2)
//                            Coprocessor1.setConditionFlag(operands[0]);
//                        else
//                            Coprocessor1.clearConditionFlag(operands[0]);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.le.s $f0,$f1",
//                        "Compare less or equal single precision : If $f0 is less than or equal to $f1, set Coprocessor 1 condition flag 0 true else set it false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 sssss fffff 00000 111110",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float op1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[0]));
//                        float op2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        if (op1 <= op2)
//                            Coprocessor1.setConditionFlag(0);
//                        else
//                            Coprocessor1.clearConditionFlag(0);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.le.s 1,$f0,$f1",
//                        "Compare less or equal single precision : If $f0 is less than or equal to $f1, set Coprocessor 1 condition flag specified by immediate to true else set it to false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 ttttt sssss fff 00 111110",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float op1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        float op2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[2]));
//                        if (op1 <= op2)
//                            Coprocessor1.setConditionFlag(operands[0]);
//                        else
//                            Coprocessor1.clearConditionFlag(operands[0]);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.lt.s $f0,$f1",
//                        "Compare less than single precision : If $f0 is less than $f1, set Coprocessor 1 condition flag 0 true else set it false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 sssss fffff 00000 111100",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float op1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[0]));
//                        float op2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        if (op1 < op2)
//                            Coprocessor1.setConditionFlag(0);
//                        else
//                            Coprocessor1.clearConditionFlag(0);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.lt.s 1,$f0,$f1",
//                        "Compare less than single precision : If $f0 is less than $f1, set Coprocessor 1 condition flag specified by immediate to true else set it to false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 ttttt sssss fff 00 111100",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        float op1 = Float.intBitsToFloat(Coprocessor1.getValue(operands[1]));
//                        float op2 = Float.intBitsToFloat(Coprocessor1.getValue(operands[2]));
//                        if (op1 < op2)
//                            Coprocessor1.setConditionFlag(operands[0]);
//                        else
//                            Coprocessor1.clearConditionFlag(operands[0]);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.eq.d $f2,$f4",
//                        "Compare equal double precision : If $f2 is equal to $f4 (double-precision), set Coprocessor 1 condition flag 0 true else set it false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 sssss fffff 00000 110010",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        double op1 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[0] + 1), Coprocessor1.getValue(operands[0])));
//                        double op2 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        if (op1 == op2)
//                            Coprocessor1.setConditionFlag(0);
//                        else
//                            Coprocessor1.clearConditionFlag(0);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.eq.d 1,$f2,$f4",
//                        "Compare equal double precision : If $f2 is equal to $f4 (double-precision), set Coprocessor 1 condition flag specified by immediate to true else set it to false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 ttttt sssss fff 00 110010",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[1] % 2 == 1 || operands[2] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        double op1 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        double op2 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[2] + 1), Coprocessor1.getValue(operands[2])));
//                        if (op1 == op2)
//                            Coprocessor1.setConditionFlag(operands[0]);
//                        else
//                            Coprocessor1.clearConditionFlag(operands[0]);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.le.d $f2,$f4",
//                        "Compare less or equal double precision : If $f2 is less than or equal to $f4 (double-precision), set Coprocessor 1 condition flag 0 true else set it false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 sssss fffff 00000 111110",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        double op1 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[0] + 1), Coprocessor1.getValue(operands[0])));
//                        double op2 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        if (op1 <= op2)
//                            Coprocessor1.setConditionFlag(0);
//                        else
//                            Coprocessor1.clearConditionFlag(0);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.le.d 1,$f2,$f4",
//                        "Compare less or equal double precision : If $f2 is less than or equal to $f4 (double-precision), set Coprocessor 1 condition flag specfied by immediate true else set it false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 ttttt sssss fff 00 111110",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[1] % 2 == 1 || operands[2] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        double op1 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        double op2 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[2] + 1), Coprocessor1.getValue(operands[2])));
//                        if (op1 <= op2)
//                            Coprocessor1.setConditionFlag(operands[0]);
//                        else
//                            Coprocessor1.clearConditionFlag(operands[0]);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.lt.d $f2,$f4",
//                        "Compare less than double precision : If $f2 is less than $f4 (double-precision), set Coprocessor 1 condition flag 0 true else set it false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 sssss fffff 00000 111100",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        double op1 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[0] + 1), Coprocessor1.getValue(operands[0])));
//                        double op2 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        if (op1 < op2)
//                            Coprocessor1.setConditionFlag(0);
//                        else
//                            Coprocessor1.clearConditionFlag(0);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("c.lt.d 1,$f2,$f4",
//                        "Compare less than double precision : If $f2 is less than $f4 (double-precision), set Coprocessor 1 condition flag specified by immediate to true else set it to false",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 ttttt sssss fff 00 111100",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[1] % 2 == 1 || operands[2] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        double op1 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        double op2 = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[2] + 1), Coprocessor1.getValue(operands[2])));
//                        if (op1 < op2)
//                            Coprocessor1.setConditionFlag(operands[0]);
//                        else
//                            Coprocessor1.clearConditionFlag(operands[0]);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("abs.s $f0,$f1",
//                        "Floating point absolute value single precision : Set $f0 to absolute value of $f1, single precision",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 00000 sssss fffff 000101",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        // I need only clear the high order bit!
//                        Coprocessor1.updateRegister(operands[0],
//                                Coprocessor1.getValue(operands[1]) & Integer.MAX_VALUE);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("abs.d $f2,$f4",
//                        "Floating point absolute value double precision : Set $f2 to absolute value of $f4, double precision",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 00000 sssss fffff 000101",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        // I need only clear the high order bit of high word register!
//                        Coprocessor1.updateRegister(operands[0] + 1,
//                                Coprocessor1.getValue(operands[1] + 1) & Integer.MAX_VALUE);
//                        Coprocessor1.updateRegister(operands[0],
//                                Coprocessor1.getValue(operands[1]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("cvt.d.s $f2,$f1",
//                        "Convert from single precision to double precision : Set $f2 to double precision equivalent of single precision value in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 00000 sssss fffff 100001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1)
//                            throw new ProcessingException(statement, "first register must be even-numbered");
//                        // convert single precision in $f1 to double stored in $f2
//                        long result = Double.doubleToLongBits(
//                                (double) Float.intBitsToFloat(Coprocessor1.getValue(operands[1])));
//                        Coprocessor1.updateRegister(operands[0] + 1, Binary.highOrderLongToInt(result));
//                        Coprocessor1.updateRegister(operands[0], Binary.lowOrderLongToInt(result));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("cvt.d.w $f2,$f1",
//                        "Convert from word to double precision : Set $f2 to double precision equivalent of 32-bit integer value in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10100 00000 sssss fffff 100001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1)
//                            throw new ProcessingException(statement, "first register must be even-numbered");
//                        // convert integer to double (interpret $f1 value as int?)
//                        long result = Double.doubleToLongBits(
//                                (double) Coprocessor1.getValue(operands[1]));
//                        Coprocessor1.updateRegister(operands[0] + 1, Binary.highOrderLongToInt(result));
//                        Coprocessor1.updateRegister(operands[0], Binary.lowOrderLongToInt(result));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("cvt.s.d $f1,$f2",
//                        "Convert from double precision to single precision : Set $f1 to single precision equivalent of double precision value in $f2",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 00000 sssss fffff 100000",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        // convert double precision in $f2 to single stored in $f1
//                        if (operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "second register must be even-numbered");
//                        double val = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        Coprocessor1.updateRegister(operands[0], Float.floatToIntBits((float) val));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("cvt.s.w $f0,$f1",
//                        "Convert from word to single precision : Set $f0 to single precision equivalent of 32-bit integer value in $f2",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10100 00000 sssss fffff 100000",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        // convert integer to single (interpret $f1 value as int?)
//                        Coprocessor1.updateRegister(operands[0],
//                                Float.floatToIntBits((float) Coprocessor1.getValue(operands[1])));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("cvt.w.d $f1,$f2",
//                        "Convert from double precision to word : Set $f1 to 32-bit integer equivalent of double precision value in $f2",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 00000 sssss fffff 100100",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        // convert double precision in $f2 to integer stored in $f1
//                        if (operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "second register must be even-numbered");
//                        double val = Double.longBitsToDouble(Binary.twoIntsToLong(
//                                Coprocessor1.getValue(operands[1] + 1), Coprocessor1.getValue(operands[1])));
//                        Coprocessor1.updateRegister(operands[0], (int) val);
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("cvt.w.s $f0,$f1",
//                        "Convert from single precision to word : Set $f0 to 32-bit integer equivalent of single precision value in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 00000 sssss fffff 100100",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        // convert single precision in $f1 to integer stored in $f0
//                        Coprocessor1.updateRegister(operands[0],
//                                (int) Float.intBitsToFloat(Coprocessor1.getValue(operands[1])));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("mov.d $f2,$f4",
//                        "Move floating point double precision : Set double precision $f2 to double precision value in $f4",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 00000 sssss fffff 000110",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                        Coprocessor1.updateRegister(operands[0] + 1, Coprocessor1.getValue(operands[1] + 1));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movf.d $f2,$f4",
//                        "Move floating point double precision : If condition flag 0 false, set double precision $f2 to double precision value in $f4",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 000 00 sssss fffff 010001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        if (Coprocessor1.getConditionFlag(0) == 0) {
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                            Coprocessor1.updateRegister(operands[0] + 1, Coprocessor1.getValue(operands[1] + 1));
//                        }
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movf.d $f2,$f4,1",
//                        "Move floating point double precision : If condition flag specified by immediate is false, set double precision $f2 to double precision value in $f4",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 ttt 00 sssss fffff 010001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        if (Coprocessor1.getConditionFlag(operands[2]) == 0) {
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                            Coprocessor1.updateRegister(operands[0] + 1, Coprocessor1.getValue(operands[1] + 1));
//                        }
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movt.d $f2,$f4",
//                        "Move floating point double precision : If condition flag 0 true, set double precision $f2 to double precision value in $f4",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 000 01 sssss fffff 010001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        if (Coprocessor1.getConditionFlag(0) == 1) {
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                            Coprocessor1.updateRegister(operands[0] + 1, Coprocessor1.getValue(operands[1] + 1));
//                        }
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movt.d $f2,$f4,1",
//                        "Move floating point double precision : If condition flag specified by immediate is true, set double precision $f2 to double precision value in $f4e",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 ttt 01 sssss fffff 010001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        if (Coprocessor1.getConditionFlag(operands[2]) == 1) {
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                            Coprocessor1.updateRegister(operands[0] + 1, Coprocessor1.getValue(operands[1] + 1));
//                        }
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movn.d $f2,$f4,$t3",
//                        "Move floating point double precision : If $t3 is not zero, set double precision $f2 to double precision value in $f4",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 ttttt sssss fffff 010011",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        if (RegisterFile.getValue(operands[2]) != 0) {
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                            Coprocessor1.updateRegister(operands[0] + 1, Coprocessor1.getValue(operands[1] + 1));
//                        }
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movz.d $f2,$f4,$t3",
//                        "Move floating point double precision : If $t3 is zero, set double precision $f2 to double precision value in $f4",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 ttttt sssss fffff 010010",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        if (RegisterFile.getValue(operands[2]) == 0) {
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                            Coprocessor1.updateRegister(operands[0] + 1, Coprocessor1.getValue(operands[1] + 1));
//                        }
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("mov.s $f0,$f1",
//                        "Move floating point single precision : Set single precision $f0 to single precision value in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 00000 sssss fffff 000110",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movf.s $f0,$f1",
//                        "Move floating point single precision : If condition flag 0 is false, set single precision $f0 to single precision value in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 000 00 sssss fffff 010001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (Coprocessor1.getConditionFlag(0) == 0)
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movf.s $f0,$f1,1",
//                        "Move floating point single precision : If condition flag specified by immediate is false, set single precision $f0 to single precision value in $f1e",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 ttt 00 sssss fffff 010001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (Coprocessor1.getConditionFlag(operands[2]) == 0)
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movt.s $f0,$f1",
//                        "Move floating point single precision : If condition flag 0 is true, set single precision $f0 to single precision value in $f1e",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 000 01 sssss fffff 010001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (Coprocessor1.getConditionFlag(0) == 1)
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movt.s $f0,$f1,1",
//                        "Move floating point single precision : If condition flag specified by immediate is true, set single precision $f0 to single precision value in $f1e",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 ttt 01 sssss fffff 010001",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (Coprocessor1.getConditionFlag(operands[2]) == 1)
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movn.s $f0,$f1,$t3",
//                        "Move floating point single precision : If $t3 is not zero, set single precision $f0 to single precision value in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 ttttt sssss fffff 010011",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (RegisterFile.getValue(operands[2]) != 0)
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("movz.s $f0,$f1,$t3",
//                        "Move floating point single precision : If $t3 is zero, set single precision $f0 to single precision value in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 ttttt sssss fffff 010010",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (RegisterFile.getValue(operands[2]) == 0)
//                            Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("mfc1 $t1,$f1",
//                        "Move from Coprocessor 1 (FPU) : Set $t1 to value in Coprocessor 1 register $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 00000 fffff sssss 00000 000000",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        RegisterFile.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("mtc1 $t1,$f1",
//                        "Move to Coprocessor 1 (FPU) : Set Coprocessor 1 register $f1 to value in $t1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 00100 fffff sssss 00000 000000",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        Coprocessor1.updateRegister(operands[1], RegisterFile.getValue(operands[0]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("neg.d $f2,$f4",
//                        "Floating point negate double precision : Set double precision $f2 to negation of double precision value in $f4",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10001 00000 sssss fffff 000111",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1 || operands[1] % 2 == 1)
//                            throw new ProcessingException(statement, "both registers must be even-numbered");
//                        // flip the sign bit of the second register (high order word) of the pair
//                        int value = Coprocessor1.getValue(operands[1] + 1);
//                        Coprocessor1.updateRegister(operands[0] + 1,
//                                ((value < 0) ? (value & Integer.MAX_VALUE) : (value | Integer.MIN_VALUE)));
//                        Coprocessor1.updateRegister(operands[0], Coprocessor1.getValue(operands[1]));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("neg.s $f0,$f1",
//                        "Floating point negate single precision : Set single precision $f0 to negation of single precision value in $f1",
//                        BasicInstructionFormat.R_FORMAT,
//                        "010001 10000 00000 sssss fffff 000111",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        int value = Coprocessor1.getValue(operands[1]);
//                        // flip the sign bit
//                        Coprocessor1.updateRegister(operands[0],
//                                ((value < 0) ? (value & Integer.MAX_VALUE) : (value | Integer.MIN_VALUE)));
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("lwc1 $f1,-100($t2)",
//                        "Load word into Coprocessor 1 (FPU) : Set $f1 to 32-bit value from effective memory word address",
//                        BasicInstructionFormat.I_FORMAT,
//                        "110001 ttttt fffff ssssssssssssssss",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        try {
//                            Coprocessor1.updateRegister(operands[0],
//                                    Main.memory.getWord(
//                                            RegisterFile.getValue(operands[2]) + operands[1]));
//                        }
//                        catch (mars.mips.hardware.AddressErrorException e) {
//                            throw new ProcessingException(statement, e);
//                        }
//                    }
//                }));
//        instructionSet.add(// no printed reference, got opcode from SPIM
//                new BasicInstruction("ldc1 $f2,-100($t2)",
//                        "Load double word Coprocessor 1 (FPU)) : Set $f2 to 64-bit value from effective memory doubleword address",
//                        BasicInstructionFormat.I_FORMAT,
//                        "110101 ttttt fffff ssssssssssssssss",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1)
//                            throw new ProcessingException(statement, "first register must be even-numbered");
//                        // IF statement added by DPS 13-July-2011.
//                        if (!Main.memory.doublewordAligned(RegisterFile.getValue(operands[2]) + operands[1]))
//                            throw new ProcessingException(statement,
//                                    new mars.mips.hardware.AddressErrorException("address not aligned on doubleword boundary ",
//                                            Exceptions.ADDRESS_EXCEPTION_LOAD, RegisterFile.getValue(operands[2]) + operands[1]));
//
//                        try {
//                            Coprocessor1.updateRegister(operands[0],
//                                    Main.memory.getWord(
//                                            RegisterFile.getValue(operands[2]) + operands[1]));
//                            Coprocessor1.updateRegister(operands[0] + 1,
//                                    Main.memory.getWord(
//                                            RegisterFile.getValue(operands[2]) + operands[1] + 4));
//                        }
//                        catch (mars.mips.hardware.AddressErrorException e) {
//                            throw new ProcessingException(statement, e);
//                        }
//                    }
//                }));
//        instructionSet.add(
//                new BasicInstruction("swc1 $f1,-100($t2)",
//                        "Store word from Coprocesor 1 (FPU) : Store 32 bit value in $f1 to effective memory word address",
//                        BasicInstructionFormat.I_FORMAT,
//                        "111001 ttttt fffff ssssssssssssssss",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        try {
//                            Main.memory.setWord(
//                                    RegisterFile.getValue(operands[2]) + operands[1],
//                                    Coprocessor1.getValue(operands[0]));
//                        }
//                        catch (mars.mips.hardware.AddressErrorException e) {
//                            throw new ProcessingException(statement, e);
//                        }
//                    }
//                }));
//        instructionSet.add( // no printed reference, got opcode from SPIM
//                new BasicInstruction("sdc1 $f2,-100($t2)",
//                        "Store double word from Coprocessor 1 (FPU)) : Store 64 bit value in $f2 to effective memory doubleword address",
//                        BasicInstructionFormat.I_FORMAT,
//                        "111101 ttttt fffff ssssssssssssssss",
//                        new SimulationCode() {
//                    public void simulate(ProgramStatement statement) throws ProcessingException {
//                        int[] operands = statement.getOperands();
//                        if (operands[0] % 2 == 1)
//                            throw new ProcessingException(statement, "first register must be even-numbered");
//                        // IF statement added by DPS 13-July-2011.
//                        if (!Main.memory.doublewordAligned(RegisterFile.getValue(operands[2]) + operands[1]))
//                            throw new ProcessingException(statement,
//                                    new mars.mips.hardware.AddressErrorException("address not aligned on doubleword boundary ",
//                                            Exceptions.ADDRESS_EXCEPTION_STORE, RegisterFile.getValue(operands[2]) + operands[1]));
//                        try {
//                            Main.memory.setWord(
//                                    RegisterFile.getValue(operands[2]) + operands[1],
//                                    Coprocessor1.getValue(operands[0]));
//                            Main.memory.setWord(
//                                    RegisterFile.getValue(operands[2]) + operands[1] + 4,
//                                    Coprocessor1.getValue(operands[0] + 1));
//                        }
//                        catch (mars.mips.hardware.AddressErrorException e) {
//                            throw new ProcessingException(statement, e);
//                        }
//                    }
//                }));
		////////////// READ PSEUDO-INSTRUCTION SPECS FROM DATA FILE AND ADD //////////////////////
		//
		//
		//
		//
		//
		// leading "/" prevents package name being prepended to filepath.
		try (BufferedReader in = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/PseudoOps.txt")))) {
			String line, pseudoOp, template, firstTemplate, token;
			String description;
			StringTokenizer tokenizer;
			while ((line = in.readLine()) != null)
				// skip over: comment lines, empty lines, lines starting with blank.
				if (!line.startsWith("#") && !line.startsWith(" ")
						&& line.length() > 0) {
					description = "";
					tokenizer = new StringTokenizer(line, "\t");
					pseudoOp = tokenizer.nextToken();
					template = "";
					firstTemplate = null;
					while (tokenizer.hasMoreTokens()) {
						token = tokenizer.nextToken();
						if (token.startsWith("#")) {
							// Optional description must be last token in the line.
							description = token.substring(1);
							break;
						}
						if (token.startsWith("COMPACT")) {
							// has second template for Compact (16-bit) memory config -- added DPS 3 Aug 2009
							firstTemplate = template;
							template = "";
							continue;
						}
						template = template + token;
						if (tokenizer.hasMoreTokens())
							template = template + "\n";
					}
					ExtendedInstruction inst = (firstTemplate == null)
							? new ExtendedInstruction(pseudoOp, template, description)
							: new ExtendedInstruction(pseudoOp, firstTemplate, template, description);
					pseudoInstructions.add(inst);
					//if (firstTemplate != null) System.out.println("\npseudoOp: "+pseudoOp+"\ndefault template:\n"+firstTemplate+"\ncompact template:\n"+template);
				}
		}
		// TODO Rewritten, but behaves similarly to original, must remove umbrella catch
		catch (IOException ex) {
			Main.logger.log(Level.SEVERE, "Internal error: Could not load MIPS pseudo-instructions", ex);
			System.exit(0);
		}

		////////////// GET AND CREATE LIST OF SYSCALL FUNCTION OBJECTS ////////////////////
//		syscallLoader = new SyscallLoader();
//		syscallLoader.loadSyscalls();
//		// Initialization step.  Create token list for each instruction example.  This is
//		// used by parser to determine user program correct syntax.
//		for (Instruction inst : instructionSet)
//			inst.createExampleTokenList();
	}

//	/**
//	 * Given an operator mnemonic, will return the corresponding Instruction
//	 * object(s) from the instruction set. Uses straight linear search
//	 * technique.
//	 *
//	 * @param name operator mnemonic (e.g. addi, sw,...)
//	 * @return list of corresponding Instruction object(s), or null if not
//	 * found.
//	 */
//	public ArrayList<Instruction> matchOperator(String name) {
//		ArrayList<Instruction> matchingInstructions = null;
//		// Linear search for now....
//		for (Instruction instruction : instructionSet)
//			if (instruction.getName().equalsIgnoreCase(name)) {
//				if (matchingInstructions == null)
//					matchingInstructions = new ArrayList<>();
//				matchingInstructions.add(instruction);
//			}
//		return matchingInstructions;
//	}
//
//	/**
//	 * Given a string, will return the Instruction object(s) from the
//	 * instruction set whose operator mnemonic prefix matches it.
//	 * Case-insensitive. For example "s" will match "sw", "sh", "sb", etc. Uses
//	 * straight linear search technique.
//	 *
//	 * @param name a string
//	 * @return list of matching Instruction object(s), or null if none match.
//	 */
//	public ArrayList<Instruction> prefixMatchOperator(String name) {
//		ArrayList<Instruction> matchingInstructions = null;
//		// Linear search for now....
//		if (name != null)
//			for (Instruction instruction : instructionSet)
//				if (instruction.getName().toLowerCase().startsWith(name.toLowerCase())) {
//					if (matchingInstructions == null)
//						matchingInstructions = new ArrayList<>();
//					matchingInstructions.add(instruction);
//				}
//		return matchingInstructions;
//	}
//
//	/*
//     * Method to find and invoke a syscall given its service number.  Each syscall
//     * function is represented by an object in an array list.  Each object is of
//     * a class that implements Syscall or extends AbstractSyscall.
//	 */
//	private void findAndSimulateSyscall(int number, ProgramStatement statement)
//			throws ProcessingException {
//		Syscall service = syscallLoader.findSyscall(number);
//		if (service != null) {
//			service.simulate(statement);
//			return;
//		}
//		throw new ProcessingException(statement,
//				"invalid or unimplemented syscall service: "
//				+ number + " ", Exceptions.SYSCALL_EXCEPTION);
//	}
}
