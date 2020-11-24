/*
 * MIT License
 * 
 * Copyright (c) 2003-2013,  Pete Sanderson and Kenneth Vollmar
 * Developed by Pete Sanderson (psanderson@otterbein.edu)
 * and Kenneth Vollmar (kenvollmar@missouristate.edu)
 * 
 * Copyright (c) 2020 Andrea Proietto [substantial edits]
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
import mars.mips.instructions.ExtendedInstruction;
import mars.mips.instructions.Instruction;

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

    // AP20105 - DANGER: Traps must have a different encoding function!
	// ###### fffff sssss ccccc ccccc ######
    // Format: OPERATOR rs, rt
    //
    // mult
    // multu
    // div
    // divu
    //
    // madd
    // maddu
    // msub
    // msubu
    //
    // tge
    // tgeu
    // tlt
    // tltu
    // teq
    // tne
    //
    // NOTE: For traps, the fields rd and shamt compose into the code field, which is filled by the system! (?!?)
    // Otherwise, they are zero-filled
	static final int genZrdshamtRinstr(int[] operands, int function) {
		// Extract register numbers
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rd, rs and rt in order
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
    // Format: OPERATOR rd, rs, rt
    //
    //
    // mul
    //
    // add
    // addu
    // sub
    // subu
    // and
    // or
    // xor
    // nor
    //
    // slt
    // sltu
    //
    // movz
    // movn
    // NOTE: the operands for the shift instructions are swapped beforehand in the assembler
	static final int genZshamtRinstr(int[] operands, int function) {
		// Extract register numbers
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rd, rs and rt in order
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
    
	// ###### ttttt sssss fffff 00000 ######
    // Format: OPERATOR rd, rt, rs
    //
    // sllv
    // srlv
    // srav
    //
    // NOTE: Basically swap the last two operands
	static final int genZshamtReversedRinstr(int[] operands, int function) {
        
        // Swap
        operands[1] ^= operands[2];
        operands[2] ^= operands[1];
        operands[1] ^= operands[2];
        
        return genZshamtRinstr(operands, function);
	}
    
	// ###### 00000 sssss fffff ttttt ######
    // Format: OPERATOR rd, rt, sa
    // 
    // sll
    // srl
    // sra
	static final int genZrsRinstr(int[] operands, int function) {
		// Extract register numbers
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rd, rt and shamt in order
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
    // Format: OPERATOR rt, rs, immediate[16]
    //
    // addi
    // addiu
    // andi
    // ori
    // xori
    //
    // slti
    // sltiu
	static final int genArithmIinstr(int[] operands, int opcode) {
		// TODO Pass a SHORT as imm
		// Extract operands
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rs, rt and imm in order
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
    // Format: OPERATOR rt, offset(base) [FIELD ORDER: base, rt, offset(16)]
    // 
    // lb
    // lh
    // lwl
    // lw
    // lbu
    // lhu
    // lwr
    // 
    // sb
    // sh
    // swl
    // sw
    // swr
    // 
    // ll # PRERELEASE 6
    // sc # PRERELEASE 6
    //
    // lwc1
    // ldc1
    // 
    // swc1
    // sdc1
	static final int genLoadStoreIinstr(int[] operands, int opcode) {
		// TODO Pass a SHORT as imm
		// Extract operands
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rs, rt and imm in order
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
    // Format: OPERATOR rs, offset[16]/immediate[16]
    //
    // bgez
    // bgezal
    // bgtz
    // blez
    // bltz
    // bltzal
    // 
    // tgei
    // tgeiu
    // tlti
    // tltiu
    // teqi
    // tnei
    // 
    // Note: BGTZ and BLEZ have each their own opcode, and explicitly specify their second field to be zero,
    //      the other branches and all traps are under the REGIMM opcode and use said field to hold the function code
    //
    // Note: Branches denote the 16 bit field as "offset", whereas traps specify it as "immediate".
    //      There may be semantic differences.
	static final int genBranchFuncIinstr(int[] operands, int opfunc) {
		// TODO Pass a SHORT as imm
		// Extract operands
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rs, rt and imm in order
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
    // Format: OPERATOR rs, rt, offset
    //
    // beq
    // bne
	static final int genBranchIinstr(int[] operands, int opcode) {
		// TODO Pass a SHORT as imm
		// Extract operands
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rs, rt and imm in order
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
    // Format: OPERATOR rd [FROM]
    // Format: OPERATOR rs [TO]
    //
    // mfhi
    // mflo
    // mthi
    // mtlo
    //
    // Note: Last bit (marked with x) decides if instruction moves "from" or "to"
	static final int genHiLoRinstr(int[] operands, int function) {

		assert (operands[0] < 32);

		// Last bit (marked with x) decides if instruction moves "from" or "to"
		return function | (operands[0] << ((function & 1) == 0 ? 11 : 21));
	}

	// ###### ffffffffffffffffffffffffff
    // Format: OPERATOR address[26]
    //
    // j
    // jal
    //
    // Note: Excerpt from MIPS spec shared between J and JAL instructions:
    //      "This is a PC-region branch (not PC-relative); the effective target address is in the “current” 256 MB-aligned region. The low 28 bits of the target address is the instr_index field shifted left 2bits. The remaining upper bits are the corresponding bits of the address of the instruction in the delay slot (not the branch itself)."
    //      
	static final int genJinstr(int[] operands, int opcode) {
        
		assert (operands[0] < 0x04000000);
		return opcode | operands[0];
	}

	// NOTE: rs field repeated in rt as per spec, basically a zshamt...
	// ###### sssss fffff fffff 00000 ######
    // Format: OPERATOR rd, rs
    //
    // clo
    // clz
    //
    // Note: Excerpt from spec shared by CLZ and CLO:
    //      "Pre-Release 6: To be compliant with the MIPS32 Architecture, software must place the same GPR number in both the rt and rd fields of the instruction. The operation of the instruction is UNPREDICTABLE if the rt and rd fields of the instruction contain different values. Release 6’s new instruction encoding does not contain an rt field."
    //
	static final int genCountBitsRinstr(int[] operands, int opfunc) {
		// Extract register numbers
		// NOTE ProgramStatement.getOperands return an array of
		// register references, namely rd, rs and rt in order
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
    
    // 000000 sssss ttt 0# fffff 00000 000001
    // Format: OPERATOR rd, rs, cc[3]
    //
    // movf
    // movt
    //
    // Note: '#' bit distinguishes MOVF from MOVT
	static final int genFPUMOVCIRinstr(int[] operands, int tfmask) {
        return tfmask | (operands[0] << 11) | (operands[1] << 21) | (operands[2] << 18);
	}
    
    // 010000 00#00 fffff sssss 00000000 ttt
    // Format: OPERATOR rt, rd, sel[3]
    // 
    // mtc0
    // mfc0
    //
    // Note: '#' bit is handled by the MF/MT mask argument, opcode and shamt(8) fields are fixed
    //
    static final int genCop0Rinstr(int[] operands, int opmft) {

        return opmft | (operands[0] << 16) | operands[1] << 11 | operands[2];
    }
    
    // 010001 ##### ttttt sssss fffff ######
    // Format: OPERATOR fd, fs, ft/rt
    //
    // add.fmt
    // sub.fmt
    // mul.fmt
    // div.fmt
    // movz.fmt
    // movn.fmt
    static final int genFPURinstr(int[] operands, int opfmtfunc) {
        return opfmtfunc | (operands[0] << 6) | (operands[1] << 11) | (operands[2] << 16);
    }
    
    // 010001 ##### 00000 sssss fffff ######
    // Format: OPERATOR fd, fs
    //
    // sqrt.fmt
    // abs.fmt
    // mov.fmt
    // neg.fmt
    // round.fmt
    // trunc.fmt
    // floor.fmt
    // ceil.fmt
    // cvt.fmt.fmt
    static final int genFPUzeroftRinstr(int[] operands, int opfmtfunc) {
        return opfmtfunc | (operands[0] << 6) | (operands[1] << 11);
    }
    
    // 010001 ##### ttt 0# sssss fffff ######
    // Format: OPERATOR fd, fs, cc
    //
    // movf.fmt
    // movt.fmt
    static final int genFPUMOVCFRinstr(int[] operands, int opfmtfunc) {
        return opfmtfunc | (operands[0] << 6) | (operands[1] << 11) | (operands[2] << 18);
    }
    
    // 010001 ##### ttttt sssss fff 00 ######
    // Format: OPERATOR cc, fs, ft
    //
    // c.cond.fmt
    static final int genFPUCinstr(int[] operands, int opfmtfunc) {
        return opfmtfunc | (operands[0] << 8) | (operands[1] << 11) | (operands[2] << 16);
    }

    // 010001 ##### fffff sssss 00000 000000
    // Format: OPERATOR rt, fs
    //
    // mfc1
    // mtc1
    static final int genFPUMOVCCRinstr(int[] operands, int opfmt) {
        return opfmt | (operands[0] << 16) | (operands[1] << 11);
    }
    
    // 010001 01000 fff 0# ssssssssssssssss
    // Format: OPERATOR cc, offset
    //
    // bc1f
    // bc1t
    static final int genFPUBranchIinstr(int[] operands, int opfmt) {
        return opfmt | (operands[0] << 18) | operands[1] ;
    }
    

    // AP170314: Counting ~90 implemented instructions
	public static final Map<String, ToIntFunction<int[]>> BasicInstructionEncodings = new HashMap<>(100);
	static final List<ExtendedInstruction> pseudoInstructions = new ArrayList<>(400);
    
    static {
        populate();
    }

	/**
	 * Creates a new InstructionSet object.
	 */
	public InstructionSetArchitecture() {
		// Counting 378 pseudos as of 170314 - AP
		//pseudoInstructions = new ArrayList<>(400);
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
	public static void populate() {
		// The parade begins...

		BasicInstructionEncodings.put("nop", (statement) -> 0);
		// ...hey, I like this so far!

		// SHIFT OPERATIONS ----------------------------------------------------
		BasicInstructionEncodings.put("sll", (statement) -> genZrsRinstr(statement, 0x00000000));
		BasicInstructionEncodings.put("srl", (statement) -> genZrsRinstr(statement, 0x00000002));
		BasicInstructionEncodings.put("sra", (statement) -> genZrsRinstr(statement, 0x00000003));
		BasicInstructionEncodings.put("sllv", (statement) -> genZshamtReversedRinstr(statement, 0x00000004));
		BasicInstructionEncodings.put("srlv", (statement) -> genZshamtReversedRinstr(statement, 0x00000006));
		BasicInstructionEncodings.put("srav", (statement) -> genZshamtReversedRinstr(statement, 0x00000007));

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
		BasicInstructionEncodings.put("lui", (operands -> {
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
		BasicInstructionEncodings.put("jr", (operands) -> {
			assert (operands[0] < 32);
			return 0x00000008 | (operands[0] << 21);
		});
		// ###### sssss 00000 fffff 00000 ######
		// ###### fffff 00000 11111 00000 ######
		BasicInstructionEncodings.put("jalr", (ops) -> {
			//TODO
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
		// 000000 oooooooooooooooooooo 001100
		BasicInstructionEncodings.put("syscall", (ops) -> {
			// Translates both the valued and unvalued variants, the boilerplate checks should disappear...
			// Expected assert is missing
			return 0x0000000C | ((ops != null && ops.length != 0) ? ops[0] : 0);
		});
		// 000000 oooooooooooooooooooo 001101
		BasicInstructionEncodings.put("break", (ops) -> {
			// Translates both the valued and unvalued variants, the boilerplate checks should disappear...
			// Expected assert is missing
			return 0x0000000D | ((ops != null && ops.length != 0) ? ops[0] : 0);
		});

		// CP0 INSTRUCTIONS ----------------------------------------------------
        
        // SPECIAL
		BasicInstructionEncodings.put("eret", (statement) -> 0x42000018);
        
		// R-TYOE SELECTOR
		BasicInstructionEncodings.put("mfc0", (statement) -> genCop0Rinstr(statement, 0x40000000));
		BasicInstructionEncodings.put("mtc0", (statement) -> genCop0Rinstr(statement, 0x40800000));

		// CP1 INSTRUCTIONS ----------------------------------------------------
        
		// R-TYOE FLAG
		BasicInstructionEncodings.put("movf", (statement) -> genFPUMOVCIRinstr(statement, 0x00000001));
		BasicInstructionEncodings.put("movt", (statement) -> genFPUMOVCIRinstr(statement, 0x00010001));

        
        
        
		// ---------------------------------------------------------------------
		// ------------- Floating Point Instructions Start Here ----------------
		// ---------------------------------------------------------------------
        
        BasicInstructionEncodings.put("add.s", (statement) -> genFPURinstr(statement, 0x46000000));
        BasicInstructionEncodings.put("sub.s", (statement) -> genFPURinstr(statement, 0x46000001));
        BasicInstructionEncodings.put("mul.s", (statement) -> genFPURinstr(statement, 0x46000002));
        BasicInstructionEncodings.put("div.s", (statement) -> genFPURinstr(statement, 0x46000003));
        
        BasicInstructionEncodings.put("sqrt.s", (statement) -> genFPUzeroftRinstr(statement, 0x46000004));
        BasicInstructionEncodings.put("abs.s", (statement) -> genFPUzeroftRinstr(statement, 0x46000005));
        BasicInstructionEncodings.put("mov.s", (statement) -> genFPUzeroftRinstr(statement, 0x46000006));
        BasicInstructionEncodings.put("neg.s", (statement) -> genFPUzeroftRinstr(statement, 0x46000007));
        
        BasicInstructionEncodings.put("round.s", (statement) -> genFPUzeroftRinstr(statement, 0x4600000C));
        BasicInstructionEncodings.put("trunc.s", (statement) -> genFPUzeroftRinstr(statement, 0x4600000D));
        BasicInstructionEncodings.put("ceil.s", (statement) -> genFPUzeroftRinstr(statement, 0x4600000E));
        BasicInstructionEncodings.put("floor.s", (statement) -> genFPUzeroftRinstr(statement, 0x4600000F));
        
        BasicInstructionEncodings.put("movf.s", (statement) -> genFPUMOVCFRinstr(statement, 0x46000011));
        BasicInstructionEncodings.put("movt.s", (statement) -> genFPUMOVCFRinstr(statement, 0x46010011));
        BasicInstructionEncodings.put("movz.s", (statement) -> genFPURinstr(statement, 0x46000012));
        BasicInstructionEncodings.put("movn.s", (statement) -> genFPURinstr(statement, 0x46000013));
        
        BasicInstructionEncodings.put("c.eq.s", (statement) -> genFPUCinstr(statement, 0x46000032));
        BasicInstructionEncodings.put("c.lt.s", (statement) -> genFPUCinstr(statement, 0x4600003C));
        BasicInstructionEncodings.put("c.le.s", (statement) -> genFPUCinstr(statement, 0x4600003E));
        
                
        BasicInstructionEncodings.put("add.d", (statement) -> genFPURinstr(statement, 0x46200000));
        BasicInstructionEncodings.put("sub.d", (statement) -> genFPURinstr(statement, 0x46200001));
        BasicInstructionEncodings.put("mul.d", (statement) -> genFPURinstr(statement, 0x46200002));
        BasicInstructionEncodings.put("div.d", (statement) -> genFPURinstr(statement, 0x46200003));
        
        BasicInstructionEncodings.put("sqrt.d", (statement) -> genFPUzeroftRinstr(statement, 0x46200004));
        BasicInstructionEncodings.put("abs.d", (statement) -> genFPUzeroftRinstr(statement, 0x46200005));
        BasicInstructionEncodings.put("mov.d", (statement) -> genFPUzeroftRinstr(statement, 0x46200006));
        BasicInstructionEncodings.put("neg.d", (statement) -> genFPUzeroftRinstr(statement, 0x46200007));
        
        BasicInstructionEncodings.put("round.d", (statement) -> genFPUzeroftRinstr(statement, 0x4620000C));
        BasicInstructionEncodings.put("trunc.d", (statement) -> genFPUzeroftRinstr(statement, 0x4620000D));
        BasicInstructionEncodings.put("ceil.d", (statement) -> genFPUzeroftRinstr(statement, 0x4620000E));
        BasicInstructionEncodings.put("floor.d", (statement) -> genFPUzeroftRinstr(statement, 0x4620000F));
        
        BasicInstructionEncodings.put("movf.d", (statement) -> genFPUMOVCFRinstr(statement, 0x46200011));
        BasicInstructionEncodings.put("movt.d", (statement) -> genFPUMOVCFRinstr(statement, 0x46210011));
        BasicInstructionEncodings.put("movz.d", (statement) -> genFPURinstr(statement, 0x46200012));
        BasicInstructionEncodings.put("movn.d", (statement) -> genFPURinstr(statement, 0x46200013));
        
        BasicInstructionEncodings.put("c.eq.d", (statement) -> genFPUCinstr(statement, 0x46200032));
        BasicInstructionEncodings.put("c.lt.d", (statement) -> genFPUCinstr(statement, 0x4620003C));
        BasicInstructionEncodings.put("c.le.d", (statement) -> genFPUCinstr(statement, 0x4620003E));
        
        
        BasicInstructionEncodings.put("cvt.d.s", (statement) -> genFPUzeroftRinstr(statement, 0x46000021));
        BasicInstructionEncodings.put("cvt.w.s", (statement) -> genFPUzeroftRinstr(statement, 0x46000024));
        BasicInstructionEncodings.put("cvt.s.d", (statement) -> genFPUzeroftRinstr(statement, 0x46200020));
        BasicInstructionEncodings.put("cvt.w.d", (statement) -> genFPUzeroftRinstr(statement, 0x46200024));
        BasicInstructionEncodings.put("cvt.d.w", (statement) -> genFPUzeroftRinstr(statement, 0x46800020));
        BasicInstructionEncodings.put("cvt.s.w", (statement) -> genFPUzeroftRinstr(statement, 0x46800021));

        
        BasicInstructionEncodings.put("mfc1", (statement) -> genFPUMOVCCRinstr(statement, 0x44000000));
        BasicInstructionEncodings.put("mtc1", (statement) -> genFPUMOVCCRinstr(statement, 0x44800000));
        
        BasicInstructionEncodings.put("bc1f", (statement) -> genFPUBranchIinstr(statement, 0x45000000));
        BasicInstructionEncodings.put("bc1t", (statement) -> genFPUBranchIinstr(statement, 0x45010000));

        BasicInstructionEncodings.put("lwc1", (statement) -> genLoadStoreIinstr(statement, 0xC4000000));
        BasicInstructionEncodings.put("ldc1", (statement) -> genLoadStoreIinstr(statement, 0xD4000000));
        BasicInstructionEncodings.put("swc1", (statement) -> genLoadStoreIinstr(statement, 0xE4000000));
        BasicInstructionEncodings.put("sdc1", (statement) -> genLoadStoreIinstr(statement, 0xF4000000));

        
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
