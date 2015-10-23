package mars.assembler;

import java.util.ArrayList;

/*
 Copyright (c) 2003-2012,  Pete Sanderson and Kenneth Vollmar

 Developed by Pete Sanderson (psanderson@otterbein.edu)
 and Kenneth Vollmar (kenvollmar@missouristate.edu)

 Permission is hereby granted, free of charge, to any person obtaining 
 a copy of this software and associated documentation files (the 
 "Software"), to deal in the Software without restriction, including 
 without limitation the rights to use, copy, modify, merge, publish, 
 distribute, sublicense, and/or sell copies of the Software, and to 
 permit persons to whom the Software is furnished to do so, subject 
 to the following conditions:

 The above copyright notice and this permission notice shall be 
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 (MIT license, http://www.opensource.org/licenses/mit-license.html)
 */
/**
 * Class representing MIPS assembler directives. Each directive is represented
 * by a unique object. The directive name is indicative of the directive it
 * represents. For example, DATA represents the MIPS .data directive.
 *
 * @author Pete Sanderson
 * @version August 2003
 */
public enum Directive {

    
    DATA(".data", "Subsequent items stored in Data segment at next available address"),
    TEXT(".text", "Subsequent items (instructions) stored in Text segment at next available address"),
    WORD(".word", "Store the listed value(s) as 32 bit words on word boundary"),
    ASCII(".ascii", "Store the string in the Data segment but do not add null terminator"),
    ASCIIZ(".asciiz", "Store the string in the Data segment and add null terminator"),
    BYTE(".byte", "Store the listed value(s) as 8 bit bytes"),
    ALIGN(".align", "Align next data item on specified byte boundary (0=byte, 1=half, 2=word, 3=double)"),
    HALF(".half", "Store the listed value(s) as 16 bit halfwords on halfword boundary"),
    SPACE(".space", "Reserve the next specified number of bytes in Data segment"),
    DOUBLE(".double", "Store the listed value(s) as double precision floating point"),
    FLOAT(".float", "Store the listed value(s) as single precision floating point"),
    EXTERN(".extern", "Declare the listed label and byte length to be a global data field"),
    KDATA(".kdata", "Subsequent items stored in Kernel Data segment at next available address"),
    KTEXT(".ktext", "Subsequent items (instructions) stored in Kernel Text segment at next available address"),
    GLOBL(".globl", "Declare the listed label(s) as global to enable referencing from other files"),
    SET(".set", "Set assembler variables.  Currently ignored but included for SPIM compatability"),
    /*  EQV added by DPS 11 July 2012 */
    EQV(".eqv", "Substitute second operand for first. First operand is symbol, second operand is expression (like #define)"),
    /* MACRO and END_MACRO added by Mohammad Sekhavat Oct 2012 */
    MACRO(".macro", "Begin macro definition.  See .end_macro"),
    END_MACRO(".end_macro", "End macro definition.  See .macro"),
    /*  INCLUDE added by DPS 11 Jan 2013 */
    INCLUDE(".include", "Insert the contents of the specified file.  Put filename in quotes.");

    public final String descriptor;
    public final String description;

    private Directive(String name, String description) {
        this.descriptor = name;
        this.description = description;
    }

    /**
     * Finds the {@code Directive} object given its descriptor.
     *
     * @param str a {@code String} containing candidate directive name (e.g.
     * ".ascii")
     * @return the matching directive, or {@code null} if none found.
     */
    public static Directive matchDirective(String str) {
        for (Directive d : Directive.values())
            if (str.equalsIgnoreCase(d.descriptor))
                return d;
        return null;
    }

    /**
     * Finds Directive objects, if any, which contain the given string as a
     * prefix. For example, ".a" will match ".ascii", ".asciiz" and ".align"
     *
     * @param str a potential directive prefix
     * @return the matching directives as an {@link ArrayList} of
     * {@code Directive}s, or {@code null} if none found.
     */
    public static ArrayList<Directive> prefixMatchDirectives(String str) {
        ArrayList<Directive> matches = null;
        for (Directive value : Directive.values())
            if (value.descriptor.toLowerCase().startsWith(str.toLowerCase())) {
                if (matches == null)
                    matches = new ArrayList<>();
                matches.add(value);

            }
        return matches;
    }

    /**
     * Lets you know whether given directive is for integer (WORD,HALF,BYTE).
     *
     * @return true if given directive is WORD, HALF or BYTE; false otherwise
     */
    public boolean isIntegerDirective() {
        return this == Directive.WORD || this == Directive.HALF || this == Directive.BYTE;
    }

    /**
     * Lets you know whether given directive is for floating number
     * (FLOAT,DOUBLE).
     *
     * @return true if given directive is FLOAT or DOUBLE, false otherwise.
     */
    public boolean isFloatingDirective() {
        return this == Directive.FLOAT || this == Directive.DOUBLE;
    }
}
