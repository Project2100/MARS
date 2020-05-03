/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mars.assembler.staticassembler.token;

import mars.MIPSprogram;


/**
 *
 * @author Project2100
 * @param <E>
 */
public abstract class Token<E> {
    
    public final E value;
    public final MIPSprogram program;
    public final int lineNumber;
    public final int position;

    /**
     * Constructor for Token class.
     *
     * @param value The generic value that this token models
     * @param sourceMIPSprogram The MIPSprogram object containing this token
     * @param line The line number in source program in which this token
     * appears.
     * @param start The starting position in that line number of this token's
     * source value.
     * @see TokenType
     */
    public Token(E value, MIPSprogram sourceMIPSprogram, int line, int start) {
        this.value = value;
        this.program = sourceMIPSprogram;
        this.lineNumber = line;
        this.position = start;
    }
    
    /**
     * Returns a displayable form of this token.
     * 
     * @return 
     */
    public abstract String display();

}
