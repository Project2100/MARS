/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.assembler.staticassembler.token;

import mars.MIPSprogram;

/**
 * The identifier token type
 *
 * @author Project2100
 */
public class MinusToken extends Token<String> {

    public MinusToken(MIPSprogram sourceMIPSprogram, int line, int start) {
        super("-", sourceMIPSprogram, line, start);
    }

    @Override
    public String display() {
        return "-";
    }

}
