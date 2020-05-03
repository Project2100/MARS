/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.assembler.staticassembler.token;

import mars.MIPSprogram;
import mars.assembler.Directive;

/**
 * The directive token type
 *
 * @author Project2100
 */
public class DirectiveToken extends Token<Directive> {

    public DirectiveToken(Directive value, MIPSprogram sourceMIPSprogram, int line, int start) {
        super(value, sourceMIPSprogram, line, start);
    }

    @Override
    public String display() {
        return value.descriptor;
    }

}
