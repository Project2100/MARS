/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.assembler.staticassembler.token;

import mars.MIPSprogram;

/**
 * The GPR type token
 *
 * @author Project2100
 */
public class RegNumberToken extends Token<Integer> {

    public RegNumberToken(Integer value, MIPSprogram sourceMIPSprogram, int line, int start) {
        super(value, sourceMIPSprogram, line, start);
    }
    
    @Override
    public String display() {
        return "$" + value;
    }

}
