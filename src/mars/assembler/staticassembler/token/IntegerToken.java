/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.assembler.staticassembler.token;

import mars.MIPSprogram;

/**
 * The integer token type
 *
 * @author Project2100
 */
public class IntegerToken extends Token<Integer> {

    public IntegerToken(Integer value, MIPSprogram sourceMIPSprogram, int line, int start) {
        super(value, sourceMIPSprogram, line, start);
    }

    @Override
    public String display() {
        return display(true);
    }
    
    public String display(boolean hexMode) {
        return hexMode ? "0x" + Integer.toHexString(value) : Integer.toString(value);
    }
    
}
