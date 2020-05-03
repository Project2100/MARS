/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.assembler.staticassembler.token;

import mars.MIPSprogram;
import mars.mips.newhardware.Register;

/**
 * The FPR token type
 *
 * @author Project2100
 */
public class FRegisterToken extends Token<Register> {

    public FRegisterToken(Register value, MIPSprogram sourceMIPSprogram, int line, int start) {
        super(value, sourceMIPSprogram, line, start);
    }

    @Override
    public String display() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
