/*
 * MIT License
 * 
 * Copyright (c) 2020 Andrea Proietto
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
