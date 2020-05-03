/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.assembler.staticassembler.token;

import mars.MIPSprogram;

/**
 * The floating-point token type
 *
 * @author Project2100
 */
public class RealToken extends Token<Double> {

    public RealToken(Double value, MIPSprogram sourceMIPSprogram, int line, int start) {
        super(value, sourceMIPSprogram, line, start);
    }

    @Override
    public String display() {
        return Double.toHexString(value);
    }

}
