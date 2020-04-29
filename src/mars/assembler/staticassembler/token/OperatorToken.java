/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.assembler.staticassembler.token;

import mars.MIPSprogram;

/**
 * The instruction operator token type
 *
 * @author Project2100
 */
public class OperatorToken extends Token<String> {

    public OperatorToken(String value, MIPSprogram sourceMIPSprogram, int line, int start) {
        super(value, sourceMIPSprogram, line, start);
    }

}
