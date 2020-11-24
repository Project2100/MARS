/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.assembler.staticassembler.token;

import mars.MIPSprogram;
import mars.mips.newhardware.Coprocessor1;

/**
 * The FPR type token for double precision formats
 *
 * @author Project2100
 */
public class DPRToken extends FPRToken {

    public DPRToken(Coprocessor1.Descriptor value, MIPSprogram sourceMIPSprogram, int line, int start) {
        super(value, sourceMIPSprogram, line, start);
    }

}
