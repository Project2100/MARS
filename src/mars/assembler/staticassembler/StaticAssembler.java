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

package mars.assembler.staticassembler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import mars.ErrorList;
import mars.ErrorMessage;
import mars.MIPSprogram;
import mars.Main;
import mars.ProcessingException;
import mars.assembler.Directive;
import mars.assembler.staticassembler.token.*;
import mars.mips.newhardware.InstructionSetArchitecture;
import mars.mips.newhardware.MIPSMachine;
import mars.mips.newhardware.Memory;
import mars.mips.newhardware.Memory.Boundary;
import mars.mips.newhardware.Registers;

/**
 * MISSING FEATURES:
 *
 * float (reg/val token, ISA, .data) include arith resolution
 *
 * @author Project2100
 */
public class StaticAssembler {
    
    /**
     * Enumeration of all possible MIPS instruction syntaxes; there aren't many,
     * so they're defined here for quick reference in the translation step.
     *
     * @author Andrea Proietto
     * @date 200406
     */
    static enum InstructionSyntax {

        R3(new Class[] {
            RegisterToken.class,
            RegisterToken.class,
            RegisterToken.class}),
        R2(new Class[] {
            RegisterToken.class,
            RegisterToken.class}),
        R2A16(new Class[] {
            RegisterToken.class,
            RegisterToken.class,
            IdentifierToken.class}),
        R2C10(new Class[] {
            RegisterToken.class,
            RegisterToken.class,
            IntegerToken.class}),
        R2I16(new Class[] {
            RegisterToken.class,
            RegisterToken.class,
            IntegerToken.class}),
        R2I5(new Class[] {
            RegisterToken.class,
            RegisterToken.class,
            IntegerToken.class}),
        R2I3(new Class[] {
            RegisterToken.class,
            RegisterToken.class,
            IntegerToken.class}),
        LS(new Class[] {
            RegisterToken.class,
            IntegerToken.class,
            LeftParenToken.class,
            RegisterToken.class,
            RightParenToken.class}),
        R1(new Class[] {
            RegisterToken.class}),
        R1A16(new Class[] {
            RegisterToken.class,
            IdentifierToken.class}),
        R1I16(new Class[] {
            RegisterToken.class,
            IntegerToken.class}),
        E(new Class[] {}),
        A26(new Class[] {
            IdentifierToken.class}),
        C20(new Class[] {
            IntegerToken.class});



        public final Class<Token<?>>[] sequence;

        private InstructionSyntax(Class<Token<?>>[] sequence) {
            this.sequence = sequence;
        }

    }
    
    static class SourceLine {

        public final String filename;
        public final int lineNumber;
        public final String originalLine;
        // Editable because labels
        public List<Token<?>> tokens;

        public SourceLine(String originalLine, List<Token<?>> tokens, String filename, int lineNumber) {
            this.originalLine = originalLine;
            this.tokens = tokens;
            this.filename = filename;
            this.lineNumber = lineNumber;
        }
    }

    static class Macro {

        final String name;
        public List<String> formalParams;
        public List<SourceLine> body;
        private int invocationCount;

        public Macro(String name, List<String> formalParams, List<SourceLine> body) {
            if (name == null || name.isEmpty())
                throw new IllegalArgumentException("Constructing macro signature without specifying a name");
            this.name = name;
            this.formalParams = formalParams;
            this.body = body;
            invocationCount = 1;
        }

        public List<SourceLine> expand(final List<Token<?>> substitutes) {
            
            final String labelPrefix = name + ":" + formalParams.size() + "@" + invocationCount + "_";

            // Craft a clone of the body:
            // - replace all parameters with their respective substitutes
            // - apply a unique prefix to all remaining identifiers, which MUST be labels
            List<SourceLine> expansion = body.stream()
                    .map(line -> new SourceLine(
                            line.filename,
                            line.tokens.stream()
                                    .map(token -> (token instanceof MacroParameterToken) && formalParams.contains(((MacroParameterToken) token).value)
                                            ? substitutes.get(formalParams.indexOf(((MacroParameterToken) token).value))
                                            : (token instanceof IdentifierToken)
                                                    ? new IdentifierToken(labelPrefix + ((IdentifierToken) token).value, token.program, token.lineNumber, token.position)
                                                    : token)
                                    .collect(Collectors.toList()),
                            line.filename,
                            line.lineNumber))
                    .collect(Collectors.toList());

            return expansion;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + Objects.hashCode(this.name);
            hash = 53 * hash + Objects.hashCode(this.formalParams.size());
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Macro other = (Macro) obj;
            if (!Objects.equals(this.name, other.name)) return false;
            return this.formalParams.size() == other.formalParams.size();
        }

    }

    static class Label {

        public int address; // The label address, or 0 if not defined
        public boolean valid; // States if label has been actually declared (i.e. it has an actual address)
        public boolean global; // States if the label has been designated as global

        public Label(int address, boolean valid, boolean global) {
            this.address = address;
            this.valid = valid;
            this.global = global;
        }
    }

    static class DataDir {

        Directive type;
        List<Token<?>> values;

        public DataDir(Directive type, List<Token<?>> values) {
            this.type = type;
            this.values = values;
        }
    }
    
    public static class ExecutableProgram {
        
        
        Map<String, List<Token<?>>> EQVs = new HashMap<>();
        Set<Macro> MACROs = new HashSet<>();
        Map<String, Label> Labels = new HashMap<>(); // AP190730: Consider using hashset, and merging the classes?
        
        
        // 0/2 = text, 2/2 = ktext
        HashMap<Integer, SourceLine> textsegs[] = new HashMap[] {new HashMap<>(), new HashMap<>()};
        // 1/2 = data, 3/2 = kdata - INTEGER DIV
        HashMap<Integer, DataDir> datasegs[] = new HashMap[] {new HashMap<>(), new HashMap<>()};
    }
    
    

    /**
     * The 8 escaped characters are: single quote, double quote, backslash,
     * newline (linefeed), tab, backspace, return, form feed.
     */
    static final Map<Character, Integer> ESCAPE_MAP = Map.of(
            '\'', 39,
            '"', 34,
            '\\', 92,
            'n', 10,
            't', 9, 
            'b', 8,
            'r', 13,
            'f', 12,
            '0', 0);
    
    static final Map<Directive, ToIntFunction<Token<?>>> DATA_INCREMENTS = Map.of(
            Directive.ASCII, t -> ((StringToken) t).value.length(),
            Directive.ASCIIZ, t -> ((StringToken) t).value.length() + 1,
            Directive.BYTE, t -> 1,
            Directive.HALF, t -> 2,
            Directive.WORD, t -> 4,
            Directive.FLOAT, t -> 4,
            Directive.DOUBLE, t -> 8);

    // AP190927 - IMPORTANT: Nested invocations should not be a problem, as bodies are expanded top-bottom
    // AP190812 - TODO: For now, we accept only single token arguments
    static final Set<Class<?>> MACRO_PARAM_CLASSES = Set.of(
            IdentifierToken.class,
            RegisterToken.class,
            FRegisterToken.class,
            IntegerToken.class,
            RealToken.class,
            StringToken.class,
            OperatorToken.class);

    static final Set<Directive> SOURCE_DIRS = Set.of(
            Directive.INCLUDE,
            Directive.EQV,
            Directive.MACRO,
            Directive.END_MACRO);

    // The ASCII encoder might be useful in more places other than here
    static final CharsetEncoder ASCII_ENCODER = Charset.forName(StandardCharsets.ISO_8859_1.name()).newEncoder();

    /**
     * COD2, A-51: "Identifiers are a sequence of alphanumeric characters,
     * underbars (_), and dots (.) that do not begin with a number."
     * <p/>
     * Ideally this would be in a separate Identifier class but I did not see an
     * immediate need beyond this method (refactoring effort would probably
     * identify other uses related to symbol table).
     *
     * @implNote DPS 14-Jul-2008: added '$' as valid symbol. Permits labels to
     * include $. MIPS-target GCC will produce labels that start with $.
     */
    static final Matcher IDENT_MATCHER = Pattern.compile("\\A[a-zA-Z_\\.\\$][\\w\\.$]*\\Z").matcher("");

    

    //<editor-fold defaultstate="collapsed" desc="TOKENIZATION">

    /**
     * Decodes a string into an integer value; accepts signed and unsigned
     * literals, as long as they are within their respective bounds
     *
     * @param literal
     * @return
     */
    static int decodeInteger(String literal) {
        long value = Long.decode(literal);

        if (value < (long) Integer.MIN_VALUE || value > ((long) Integer.MAX_VALUE + Integer.MAX_VALUE + 1)) {
            throw new NumberFormatException("Cannot decode to either signed or unsigned integer");
        }

        return (int) value;
    }

    /**
     * Decides if the given string is a valid identifier name.
     *
     * @param value the string to check
     * @return the decision
     */
    static boolean isValidIdentifier(String value) {
        return IDENT_MATCHER.reset(value).matches();
    }

    /**
     * Builds a valid token from the given string form, along with additional
     * required info.
     *
     * @param value
     * @param program
     * @param line
     * @param pos
     * @param extendedAssemblerEnabled
     * @return
     * @throws ProcessingException
     */
    static Token<?> constructToken(String value, MIPSprogram program, int line, int pos, boolean extendedAssemblerEnabled, ErrorList errors) throws ProcessingException {

        // AP200424: Comments are removed, and quoted strings are processed elsewhere

        // If it starts with single quote ('), it should be a character literal
        //<editor-fold defaultstate="collapsed" desc="Character literal">
        if (value.startsWith("'")) {
            // must start and end with quote and have something in between
            if (value.length() < 3 || !value.endsWith("'")) {
                errors.add(new ErrorMessage(true, program, line, pos, "Malformed char literal"));
                throw new ProcessingException(errors);
            }

            // Remove the quotes
            String stripped = value.substring(1, value.length() - 1);

            // If this isn't an escape sequence, then decoding is simple: if one character is left, return its encoding, otherwise -1
            if (stripped.charAt(0) != '\\') {
                if (stripped.length() != 1) {
                    errors.add(new ErrorMessage(true, program, line, pos, "Malformed char literal"));
                    throw new ProcessingException(errors);
                }

                int cp = Character.codePointAt(stripped, 0);
                if (cp > 255) {
                    // Unmappale character in ISO-8859
                    errors.add(new ErrorMessage(true, program, line, pos, "Unmappable character literal, will be truncated"));
                }
                // AP200424: Value might become negative w.r.t. Java's interpretation, we're only interested in preserving the 8 least significant bits
                return new IntegerToken(cp, program, line, pos);
            }

            // now we know it is an escape sequence, have to decode which of the 8: ',",\,n,t,b,r,f
            if (stripped.length() == 2) {
                try {
                    return new IntegerToken(ESCAPE_MAP.get(stripped.charAt(1)), program, line, pos);
                }
                catch (NullPointerException ex) {
                    errors.add(new ErrorMessage(true, program, line, pos, "Unknown escape sequence"));
                    throw new ProcessingException(errors);
                }
            }

            // last valid possibility is 3 digit octal code 000 through 377
            if (stripped.length() == 4) try {
                int intValue = Integer.parseInt(stripped.substring(1), 8);
                if (intValue < 0 || intValue > 255) {
                    errors.add(new ErrorMessage(true, program, line, pos, "Octal code outside of ISO-8859-1 range"));
                    throw new ProcessingException(errors);
                }
                return new IntegerToken(intValue, program, line, pos);
            }
            catch (NumberFormatException nfe) {
                errors.add(new ErrorMessage(true, program, line, pos, "Malformed char literal"));
                throw new ProcessingException(errors);
            }

            // No hope left...
            errors.add(new ErrorMessage(true, program, line, pos, "Malformed char literal"));
            throw new ProcessingException(errors);
        }
        //</editor-fold>

        // See if it is a macro parameter
        if (value.startsWith("%")) {
            if (!IDENT_MATCHER.reset(value.substring(1)).matches()) {
                errors.add(new ErrorMessage(true, program, line, pos, "Illegal macro parameter identifier"));
                throw new ProcessingException(errors);
            }

            return new MacroParameterToken(value, program, line, pos);
        }


        // See if it is a directive
        Directive d = Directive.matchDirective(value);
        if (d != null)
            return new DirectiveToken(d, program, line, pos);


        // See if it is a register, in case it is not, a dollar-lead token may be a label name
        Registers.Descriptor reg = Registers.findByName(value);
        if (reg != null && extendedAssemblerEnabled) {
            return new RegisterToken(reg, program, line, pos);
        }
        reg = Registers.findByNumber(value);
        if (reg != null) {
            return new RegisterToken(reg, program, line, pos);
        }

        // See if it is a floating point register
//        Register reg = Coprocessor1.getRegister(value);
//        if (reg != null)
//            return TokenType.FP_REGISTER_NAME;

        // See if it is an integer value
        // AP200424: Are there instances of a token other than an integer beginning with a digit? Can be a good fail-fast
        try {
            return new IntegerToken(decodeInteger(value), program, line, pos);
        }
        catch (NumberFormatException e) {
            // NO ACTION -- exception suppressed
        }

        // See if it is a real (fixed or floating point) number.  Note that parseDouble()
        // accepts integer values but if it were an integer literal we wouldn't get this far.
        // AP200424: Discern between floats and doubles?!
        // AP200426: Decide between Float/Double, and between valueOf() and parse<FORMAT>()
        try {
            double val = Double.parseDouble(value);
            return new RealToken(val, program, line, pos);
        }
        catch (NumberFormatException e) {
            // NO ACTION -- exception suppressed
        }

        // See if it is an instruction operator
        if (InstructionSetArchitecture.BasicInstructionEncodings.get(value) != null)
            return new OperatorToken(value, program, line, pos);

        // Test for identifier goes last because I have defined tokens for various
        // MIPS constructs (such as operators and directives) that also could fit
        // the lexical specifications of an identifier, and those need to be
        // recognized first.
        // AP200424: For now, operators are clearly distinct from identifiers, see above
        if (isValidIdentifier(value))
            return new IdentifierToken(value, program, line, pos);

        // Matches no MIPS language token: The token is inherently malformed
        errors.add(new ErrorMessage(true, program, line, pos, "Malformed token"));
        throw new ProcessingException(errors);
    }

    /**
     * Will tokenize one line of source code.If lexical errors are discovered,
     * they are noted in an ErrorMessage object which is added to the provided
     * ErrorList instead of the Tokenizer's error list.
     *
     * @param program MIPSprogram containing this line of source
     * @param lineNum line number from source code (used in error message)
     * @param theLine String containing source code
     * @param extendedAssemblerEnabled
     * @return the generated token list for that line
     */
    static List<Token<?>> tokenize(MIPSprogram program, int lineNum, String theLine, boolean extendedAssemblerEnabled, ErrorList errors) throws ProcessingException {

        List<Token<?>> tokens = new ArrayList<>();
        if (theLine.isEmpty()) return tokens;

        // Line trimming facilitates some tasks
        theLine = theLine.trim();

        // will be faster to work with char arrays instead of strings
        char c;
        char[] lineBuffer = theLine.toCharArray();
        int lineCaret = 0;
        char[] tokenBuffer = new char[lineBuffer.length];
        int tokenCaret = 0;
        int tokenStartIdxInc = 1;
        boolean insideQuotedString = false;
        if (Main.debug)
            System.out.println("source line --->" + theLine + "<---");

        // Iterate over the line's characters
        charloop:
        while (lineCaret < lineBuffer.length) {
            c = lineBuffer[lineCaret];

            // If we are reading a quoted string, keep putting any char in it
            if (insideQuotedString) {
                tokenBuffer[tokenCaret++] = c;
                // If char is a quote, and it is not preceded by a backslash, then the quoted string is closed
                if (c == '"' && tokenBuffer[tokenCaret - 2] != '\\') {

                    // Directly add the quoted string as a token to the tkList
                    // Immediately dispose of the enclosing quotes
                    tokens.add(new StringToken(new String(tokenBuffer, 1, tokenCaret - 1), program, lineNum, tokenStartIdxInc));
                    tokenCaret = 0;
                    insideQuotedString = false;
                }
            }

            // Detect whitespaces
            else if (Character.isWhitespace(lineBuffer[lineCaret])) {

                // AP190720: See if this condition will be effectively always true
                if (tokenCaret > 0) {
                    tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                    tokenCaret = 0;
                }

                // Advance until a non-whitespace char is met
                // Recall that leading and trailing whitespaces are removed, no OOBE checks are necessary
                do lineCaret++;
                while (Character.isWhitespace(lineBuffer[lineCaret]));
                continue;
            }

            // Otherwise, not inside a quoted string, so be sensitive to delimiters - AP190722: Whitespaces will not appear here, only true delimiters
            else switch (lineBuffer[lineCaret]) {

                // '#' denotes a comment that takes the line's remainder
                case '#':
                    // There may not be spaces between the commen and a previous token, mening we could still be in a token read state; if so, compute the previous token here
                    if (tokenCaret > 0) {
                        tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                    }

                    // Create the comment token, will cover the line's remainder
                    // AP190723: NOT PUTTING COMMENTS IN PREPROCESSED CODE
                    //tokens.add(new Token(TokenType.COMMENT, new String(lineBuffer, lineCaret, lineBuffer.length - lineCaret), sourceMIPSprogram, lineNum, lineCaret + 1));

                    // At this point, this source line is effectively exausted
                    //lineCaret = lineBuffer.length;
                    //tokenCaret = 0;
                    break charloop;

                // Commas are seen as delimiters (for now...)
                // AP190720: Consider keeping only the comma as delimiter, and review the assembler accordingly
                // AP190923: MARS treats commas essentially as whitespace, ignore it here too
                case ',':
                    if (tokenCaret > 0) {
                        tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                        tokenCaret = 0;
                    }

                    // Add the comma to the token list - AP190720: LATER
                    //tokens.add(new Token(TokenType.DELIMITER, ",", sourceMIPSprogram, lineNum, lineCaret + 1));
                    break;

                // Since we're not in a quoted string, this occurrence does start one
                case '"':
                    if (tokenCaret > 0) {
                        tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                        tokenCaret = 0;
                    }
                    tokenStartIdxInc = lineCaret + 1;
                    tokenBuffer[tokenCaret++] = c;
                    insideQuotedString = true;
                    break;



                // these are other single-character tokens
                case ':':
                    if (tokenCaret > 0) {
                        tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                    }

                    tokens.add(new ColonToken(program, lineNum, lineCaret + 1));
                    tokenCaret = 0;
                    break;

                case '(':
                    if (tokenCaret > 0) {
                        tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                    }

                    tokens.add(new LeftParenToken(program, lineNum, lineCaret + 1));
                    tokenCaret = 0;
                    break;

                case ')':
                    if (tokenCaret > 0) {
                        tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                    }

                    tokens.add(new RightParenToken(program, lineNum, lineCaret + 1));
                    tokenCaret = 0;
                    break;

                // These two guys are special.  Will be recognized as unary if and only if two conditions hold:
                // 1. Immediately followed by a digit (will use look-ahead for this).
                // 2. Previous token, if any, is _not_ an IDENTIFIER
                // Otherwise considered binary and thus a separate token.  This is a slight hack but reasonable.
                // AP190730 - TODO INSPECT: Are they ever used as (arithmetic) operators?!
                case '+':
                case '-':
                    // Here's the REAL hack: recognizing signed exponent in E-notation floating point!
                    // (e.g. 1.2e-5) Add the + or - to the token and keep going.  DPS 17 Aug 2005
                    // AP190722: If I'm reading a token, and there are at least 1 more char left in the line, which happens to be a digit, and the preceding char is an 'e' for exponent, then I'm in the case of an exponent-form integer literal; therefore, the sign is actually 'part' of the number token, copy it and move along
                    if (tokenCaret > 0 && lineBuffer.length >= lineCaret + 2
                            && Character.isDigit(lineBuffer[lineCaret + 1])
                            && Character.toLowerCase(lineBuffer[lineCaret - 1]) == 'e') {
                        tokenBuffer[tokenCaret++] = c;
                        break;
                    }
                    // End of REAL hack.

                    // Else, close any previous token
                    if (tokenCaret > 0) {
                        tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                        tokenCaret = 0;
                    }

                    // Do read ahead to interpret the symbol as sign or as operator

                    // If the symbol is immediately followed by a digit, if any, and the previous token, if any, is not an identifier (numbers included), then this symbol is to be interpreted as a sign
                    if (lineBuffer.length >= lineCaret + 2
                            && Character.isDigit(lineBuffer[lineCaret + 1])
                            && (tokens.isEmpty() || !(tokens.get(tokens.size() - 1) instanceof IdentifierToken))) {
                        tokenStartIdxInc = lineCaret + 1;
                        tokenBuffer[tokenCaret++] = c;
                    }
                    else {
                        // Otherwise, it is an operator
                        tokens.add(lineBuffer[lineCaret] == '+'
                                ? new PlusToken(program, lineNum, lineCaret + 1)
                                : new MinusToken(program, lineNum, lineCaret + 1));
                    }

                    // AP190722: Trashing old logic, need to check consequences at parsing level
//                    // Set the token start
//                    tokenStartIdxInc = lineCaret + 1;
//                    // Get(READ!) the next character
//                    tokenBuffer[tokenCaret++] = c;
//
//                    // NOT (If either we have at least a preceding token, and it is an identifier, and he symbol is followed by a digit, if any)
//                    if (!((tokens.isEmpty() || tokens.get(tokens.size() - 1).getType() != TokenType.IDENTIFIER)
//                            && lineBuffer.length >= lineCaret + 2 && Character.isDigit(lineBuffer[lineCaret + 1]))) {
//                        // treat it as binary.....
//                        this.processCandidateToken(tokenBuffer, program, lineNum, theLine, tokenCaret, tokenStartIdxInc, tokens);
//                        tokenCaret = 0;
//                    }
                    break;



                case '\'': // start of character constant (single quote).
                    if (tokenCaret > 0) {
                        tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                        tokenCaret = 0;
                    }


                    // Our strategy is to process the whole thing right now...
                    tokenStartIdxInc = lineCaret + 1;
                    tokenBuffer[tokenCaret++] = c; // Put the quote in token[0]
                    int lookaheadChars = lineBuffer.length - lineCaret - 1;
                    // need minimum 2 more characters, 1 for char and 1 for ending quote
                    if (lookaheadChars < 2)
                        break;  // gonna be an error
                    c = lineBuffer[++lineCaret];
                    tokenBuffer[tokenCaret++] = c; // grab second character, put it in token[1]
                    if (c == '\'')
                        break; // gonna be an error: nothing between the quotes
                    c = lineBuffer[++lineCaret];
                    tokenBuffer[tokenCaret++] = c; // grab third character, put it in token[2]
                    // Process if we've either reached second, non-escaped, quote or end of line.
                    if (c == '\'' && tokenBuffer[1] != '\\' || lookaheadChars == 2) {
                        tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                        tokenCaret = 0;
                        tokenStartIdxInc = lineCaret + 1;
                        break;
                    }
                    // At this point, there is at least one more character on this line. If we're
                    // still here after seeing a second quote, it was escaped.  Not done yet;
                    // we either have an escape code, an octal code (also escaped) or invalid.
                    c = lineBuffer[++lineCaret];
                    tokenBuffer[tokenCaret++] = c; // grab fourth character, put it in token[3]
                    // Process, if this is ending quote for escaped character or if at end of line
                    if (c == '\'' || lookaheadChars == 3) {
                        tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                        tokenCaret = 0;
                        tokenStartIdxInc = lineCaret + 1;
                        break;
                    }
                    // At this point, we've handled all legal possibilities except octal, e.g. '\377'
                    // Proceed, if enough characters remain to finish off octal.
                    if (lookaheadChars >= 5) {
                        c = lineBuffer[++lineCaret];
                        tokenBuffer[tokenCaret++] = c;  // grab fifth character, put it in token[4]
                        if (c != '\'') {
                            // still haven't reached end, last chance for validity!
                            c = lineBuffer[++lineCaret];
                            tokenBuffer[tokenCaret++] = c;  // grab sixth character, put it in token[5]
                        }
                    }
                    // process no matter what...we either have a valid character by now or not
                    tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
                    tokenCaret = 0;
                    tokenStartIdxInc = lineCaret + 1;
                    break;

                // Other non-whitespace characters
                default:
                    if (tokenCaret == 0)
                        // We're starting a new token
                        tokenStartIdxInc = lineCaret + 1;
                    tokenBuffer[tokenCaret++] = c;
                    break;
            }

            // Read next character
            lineCaret++;
        }

        // Close the last token on the line, if left opened
        if (tokenCaret > 0) {
            tokens.add(constructToken(new String(tokenBuffer, 0, tokenCaret), program, lineNum, tokenStartIdxInc, extendedAssemblerEnabled, errors));
        }


        return tokens;
    }

    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="PREPROCESSING">

    /**
     * Extracts label definitions from the given source line, and registers them
     * into the given map
     *
     * @param Labels
     * @param program
     * @param line
     * @param currentAddress
     * @return
     * @throws ProcessingException
     */
    static List<Token<?>> extractLabelDefinitions(Map<String, Label> Labels, MIPSprogram program, SourceLine line, int currentAddress, Map<String, List<Token<?>>> EQVs, ErrorList errors) throws ProcessingException {

        List<Token<?>> tokenLine = line.tokens;

        // If line begins with a label, record it
        if (tokenLine.size() >= 2 && tokenLine.get(0) instanceof IdentifierToken && tokenLine.get(1) instanceof ColonToken) {

            String name = ((IdentifierToken) tokenLine.get(0)).value;

            // Check for a clash with an EQV
            if (EQVs.containsKey(name)) {
                errors.add(new ErrorMessage(program, line.lineNumber, 0, "Label name clashes with previous EQV"));
                throw new ProcessingException(errors);
            }

            // There's a label def: look for redefs
            if (Labels.containsKey(name)) {

                Label l = Labels.get(name);
                // Label has already been declared, look if it is already defined properly
                if (l.valid) {
                    // Dupe!!

                    errors.add(new ErrorMessage(program, line.lineNumber, 0, "Label redefinition"));
                    throw new ProcessingException(errors);
                }
                else {
                    // Declared only, complete definition
                    l.valid = true;
                    l.address = currentAddress;
                }
            }
            else {
                // Label is fresh, define standardly
                Label l = new Label(currentAddress, true, false);
                Labels.put(name, l);
            }

            // Elide the label from source line
            // AP200426 - CAUTION: Forcing the edit onto the source line object
            line.tokens = tokenLine.subList(2, tokenLine.size());
        }

        return line.tokens;
    }
    
    // AP200429 - CAUTION: Needs to accept integers for float directives
    /**
     * Decides whether the given (data) directive accepts this token as input
     *
     * @param token
     * @param dir
     * @return {@code true} if {@code dir} is a strict data directive and it
     * accepts {@code token}, {@code false} otherwise
     */
    static boolean isLiteralForDirective(Token<?> token, Directive dir) {
        switch (dir) {

            case ASCII:
            case ASCIIZ:
                return token instanceof StringToken;
            case BYTE:
            case HALF:
            case WORD:
                return token instanceof IntegerToken || token instanceof IdentifierToken;
            case FLOAT:
            case DOUBLE:
                return token instanceof RealToken;
            default:
                return false;
        }
    }

    /**
     * Utility method for generating memory spaces in the form of ASCII null
     * strings
     *
     * @param count the required string length
     * @return a zero-filled string
     */
    static String generateQuotedZeroString(int count) {
        char[] a = new char[count];
        Arrays.fill(a, '\0');
        return new String(a);
    }

    /**
     * Apply EQV substitutions to the given tokenized source line
     *
     * @param EQVs
     * @param tokenizedLine
     */
    static void applyEQVs(Map<String, List<Token<?>>> EQVs, List<Token<?>> tokenizedLine) {

        for (int j = 0; j < tokenizedLine.size(); j++) {
            Token<?> token = tokenizedLine.get(j);

            // Explicitly check the token type to avoid possible unintended behaviour with other token types, such as quoted strings
            if (token instanceof IdentifierToken && EQVs.containsKey(((IdentifierToken) token).value)) {
                List<Token<?>> exp = EQVs.get(((IdentifierToken) token).value);
                tokenizedLine.remove(j);
                tokenizedLine.addAll(j, exp);
                // EQVs are eagerly evaluated, we can safely skip the substitution
            }
        }
    }

    /**
     * Decides whether the given tokenized line is a macro invocation, and if
     * true, performs a macro expansion
     *
     * @param tokenLine
     * @param program
     * @param tokenProgram
     * @param lineIdx
     * @return
     * @throws mars.ProcessingException
     */
    static boolean attemptMACROInvocation(List<Token<?>> tokenLine, MIPSprogram program, List<SourceLine> tokenProgram, int lineIdx, Set<Macro> MACROs, ErrorList errors) throws ProcessingException {

        // First token must be an identifier
        if (!(tokenLine.get(0) instanceof IdentifierToken)) return false;

        // There must be parentheses
        // AP190812: Not considering 0-arity parentheses-less invocations, yet - TODO
        if (tokenLine.size() < 3 || !(tokenLine.get(1) instanceof LeftParenToken) || !(tokenLine.get(tokenLine.size() - 1) instanceof RightParenToken))
            return false;

        // Looks much like a macro invocation at this point, parse the arguments
        String macroName = ((IdentifierToken) tokenLine.get(0)).value;
        
        
        
        List<Token<?>> macroArgs = tokenLine.subList(2, tokenLine.size() - 2);
        
        Optional<Token<?>> invalid = macroArgs.stream().filter(t -> MACRO_PARAM_CLASSES.contains(t.getClass())).findFirst();
        if (invalid.isPresent()) {
            errors.add(new ErrorMessage(program, lineIdx + 1, invalid.get().position, "Illegal token as macro argument"));
            throw new ProcessingException(errors);
        }
        
        
        Optional<Macro> mac = MACROs.stream().filter((m) -> m.name.equals(macroName) && m.formalParams.size() == macroArgs.size()).findFirst();
        if (mac.isPresent()) {

            // There's a match! Do the expansion
            tokenProgram.remove(lineIdx);
            tokenProgram.addAll(lineIdx, mac.get().expand(macroArgs));
            return true;
        }
        else {
            errors.add(new ErrorMessage(program, lineIdx + 1, 0, "Unknown macro signature"));
            throw new ProcessingException(errors);
        }

    }

    /**
     * BE CAREFUL OF MACRO PARAMETER INJECTIONS!
     *
     * possible tests:
     *
     * EQV: unauthorized token as identifier (OK), unauthorized tokens in body
     * (REDACTED), eqv redefinition (OK?!), nested eqvs (OK?!), MACRO clash,
     * macro invocation in eqv body (!), arithmetic expressions
     *
     * MACRO (def): directives inside macro, label handling, end-of-macro check,
     * macro redefinition and overloading, EQV clash, nested EQV/MACRO
     * invocations
     *
     * MACRO (inv): invocation syntax, EQV expansion, (invalid) MACRO expansion,
     * unauthorized tokens as MACRO arguments, arithmetic expressions
     *
     * @param program
     * @param tokenProgram
     * @param machine
     * @throws ProcessingException
     */
    static ExecutableProgram preprocessSourceUnit(MIPSprogram program, List<SourceLine> tokenProgram, MIPSMachine machine, Map<String, List<Token<?>>> EQVs, Set<Macro> MACROs, Map<String, Label> Labels, ErrorList errors) throws ProcessingException {
        
        ExecutableProgram s = new ExecutableProgram();
        HashMap<Integer, SourceLine>[] textsegs = s.textsegs;
        HashMap<Integer, DataDir>[] datasegs = s.datasegs;
        
        ////////////////////////////////////////////////////////////////////////
        // SEGMENT TRACKING
        int textAddress = machine.configuration().getAddress(Memory.Descriptor.TEXT_BASE_ADDRESS);
        int dataAddress = machine.configuration().getAddress(Memory.Descriptor.DATA_BASE_ADDRESS);
        int ktextAddress = machine.configuration().getAddress(Memory.Descriptor.KTEXT_BASE_ADDRESS);
        int kdataAddress = machine.configuration().getAddress(Memory.Descriptor.KDATA_BASE_ADDRESS);
        int externAddress = machine.configuration().getAddress(Memory.Descriptor.EXTERN_BASE_ADDRESS);

        /**
         * 0: text 1: data 2: ktext 3: kdata
         */
        int segment = 0;
        int currentAddress = textAddress;
        ////////////////////////////////////////////////////////////////////////

        // Needed to realign cursors after an .ascii/.asciiz/.space directive
        Boundary dataCurrentBoundary = Boundary.WORD;
        Boundary kdataCurrentBoundary = Boundary.WORD;


        lineLoop:
        for (int lineIdx = 0; lineIdx < tokenProgram.size(); lineIdx++) {
            //FATAL!
            // AP200423: Dunno what I meant. To check:
            // - No OOBE occurs:
            //      . After macro recording
            //      . After a data directive
            // - No NPEs whatsoever
            // - No EMPTY LINES!
            // Check for line and token numbers
            SourceLine line = tokenProgram.get(lineIdx);


            // Deal with label definitions
            // Definitions which precede some directives may be misleading: .eqv, .macro, .include, and especially the segments!
            List<Token<?>> tokenLine = extractLabelDefinitions(Labels, program, line, currentAddress, EQVs, errors);
            // Additional check for lines that contained only label defs
            if (tokenLine.isEmpty()) continue;

            Token<?> first = tokenLine.get(0);

            if (first instanceof DirectiveToken && SOURCE_DIRS.contains(((DirectiveToken) first).value)) {
                switch ((Directive) first.value) {

                    // AP190730 - NOTES:
                    //      - Allowing empty EQVs
                    //      - Namespace is shared with 0-arity macros AP190921: MAY REMOVE
                    //      - Namespace is shared with labels
                    //<editor-fold defaultstate="collapsed" desc=".eqv">
                    case EQV: {

                        // Check that the line contains at least 2 tokens (allowing empty EQVs)
                        if (tokenLine.size() < 2) {
                            errors.add(new ErrorMessage(program, line.lineNumber, first.position, "Expected identifier after '.eqv'"));
                            throw new ProcessingException(errors);
                        }

                        // Check that the second token is an identifier (no operators, numbers or anything that is not classified as an identifier)
                        Token<?> identToken = tokenLine.get(1);

                        if (!(identToken instanceof IdentifierToken)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, identToken.position, "Not a legal EQV identifier"));
                            throw new ProcessingException(errors);
                        }

                        // Keyword-identifier couple established, we're looking at a syntactically correct EQV def
                        String ident = ((IdentifierToken) identToken).value;

                        // Check if this identifier isn't already used by a previous eqv definition
                        if (EQVs.containsKey(ident)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, identToken.position, "EQV redefinition"));
                            throw new ProcessingException(errors);
                        }

                        // Check if this identifier isn't already used by a previous eqv definition
                        if (Labels.containsKey(ident)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, identToken.position, "EQV name clashes with previous label"));
                            throw new ProcessingException(errors);
                        }

                        // Check if this identifier isn't already used by a previous 0-arity macro definition
                        // AP190921: Might remove this check if 0-arities are kept as strictly written with parentheses
                        Optional<Macro> redef = MACROs.stream().filter(m -> m.name.equals(ident) && m.formalParams.isEmpty()).findFirst();
                        if (redef.isPresent()) {
                            errors.add(new ErrorMessage(program, line.lineNumber, identToken.position, "EQV is already defined as a MACRO"));
                            throw new ProcessingException(errors);
                        }

                        // Verify that the expansion body does not have any illegal tokens
                        // AP190824: Filtering source directives, otherwise things can get nasty
                        // AP190731: An EQV acting as a data template might be very useful, thus memory directives are allowed
                        if (tokenLine.stream().skip(2).anyMatch((t) -> t instanceof DirectiveToken && SOURCE_DIRS.contains(((DirectiveToken) t).value))) {
                            errors.add(new ErrorMessage(program, line.lineNumber, identToken.position, "Source directives are not allowed in EQV expansion"));
                            throw new ProcessingException(errors);
                        }

                        // EQV is fresh, apply other EQVs in the expansion (EAGER EVALUATION), and add it to the map
                        applyEQVs(EQVs, tokenLine);
                        EQVs.put(ident, tokenLine.subList(2, tokenLine.size()));
                        continue;
                    }
                    //</editor-fold>

                    // AP190730: ACCEPT ARITHMETIC EXPRESSIONS?
                    //<editor-fold defaultstate="collapsed" desc=".macro">
                    case MACRO: {
                        // First token is a macro def token, check that the declaration is correctly formed
                        // Doing it step by step, in order to generate helpful error messages
                        // AP190730: Parentheses are mandatory, even in 0-arity case (might add permissive logic, or consider allowing separate namespaces for EQV and MACRO)

                        // Check that the following token is an identifier
                        if (tokenLine.size() < 2 || !(tokenLine.get(1) instanceof IdentifierToken)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, first.position, "Expected identifier after '.macro'"));
                            throw new ProcessingException(errors);
                        }
                        String ident = ((IdentifierToken) tokenLine.get(1)).value;

                        // AP190812 - TODO: For now, forbid parentheses-less definitions
                        // Check opening parentheses
                        if (tokenLine.size() < 3 || !(tokenLine.get(2) instanceof LeftParenToken)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, tokenLine.get(2).position, "Expected '(' after macro identifier"));
                            throw new ProcessingException(errors);
                        }

                        // AP190812 - TODO: Might report these kinds of issues in a differrent manner
                        // Check closing parenthesis
                        if (!(tokenLine.get(tokenLine.size() - 1) instanceof RightParenToken)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, tokenLine.get(tokenLine.size() - 1).position, "Missing ')' in macro definition"));
                            throw new ProcessingException(errors);
                        }

                        // Start interpreting the signature
                        List<? extends Token<?>> params = tokenLine.subList(3, tokenLine.size() - 1);
                        for (Token<?> param : params) {
                            if (!(param instanceof MacroParameterToken)) {
                                errors.add(new ErrorMessage(program, line.lineNumber, param.position, "Illegal token as formal parameter\nFormal parameters must begin with '%'"));
                                throw new ProcessingException(errors);
                            }
                        }

                        // Signature is valid, check for redefinitions
                        Optional<Macro> mac = MACROs.stream().filter((m) -> m.name.equals(ident) && m.formalParams.size() == params.size()).findFirst();
                        if (mac.isPresent()) {
                            errors.add(new ErrorMessage(program, line.lineNumber, 0, "Macro redefinition"));
                            throw new ProcessingException(errors);
                        }



                        // Signature is fresh,
                        // Look for the .end_macro directive
                        // Actually, START RECORDING
                        // - Source directives not allowed
                        // - all other directives allowed
                        // - EQVs and MACROs are eagerly applied to establish stronger consistency across invocations
                        // - All label definitions and references are considered exclusively local to the macro definition
                        // . NOTE: They will be dealt with on expansion, leave them as-is in the definition
                        // - Basically, all label references must be registered to the internal mapping, for the same reasons stated for EQVs
                        //      - Also, greatly simplifies logic
                        // Scan all lines and bail if a directive different from .end_macro occurs
                        List<SourceLine> macroBody = new ArrayList<>();
                        for (lineIdx++; lineIdx < tokenProgram.size(); lineIdx++) {

                            line = tokenProgram.get(lineIdx);
                            tokenLine = line.tokens;

                            // Shortcut for empty lines
                            if (tokenLine.isEmpty()) continue;

                            first = tokenLine.get(0);

                            // Check for .end_macro
                            if (first instanceof DirectiveToken && first.value == Directive.END_MACRO) {

                                // Only the directive must be there
                                if (tokenLine.size() > 1) {
                                    errors.add(new ErrorMessage(program, line.lineNumber, first.position, "Expected end of line after '.end_macro'"));
                                    throw new ProcessingException(errors);
                                }

                                // All went well, register the macro and move along
                                // AP200425: How to subcast a list: https://stackoverflow.com/questions/933447/how-do-you-cast-a-list-of-supertypes-to-a-list-of-subtypes
                                //MacroSpec spec = new MacroSpec(((List<MacroParameterToken>) params).stream().map(t -> t.value).collect(Collectors.toList()), macroBody);
                                Macro macro = new Macro(ident, ((List<MacroParameterToken>) params).stream().map(t -> t.value).collect(Collectors.toList()), macroBody);
                                MACROs.add(macro);
                                // Break outside the recording logic
                                continue lineLoop;
                            }

                            // Directive check - NO DIRECTIVES WHATSOEVER ALLOWED IN MACRO BODY
                            else if (tokenLine.stream().anyMatch((t) -> t instanceof DirectiveToken)) {
                                errors.add(new ErrorMessage(program, line.lineNumber, 0, "Directives are forbidden in macro definitions"));
                                throw new ProcessingException(errors);
                            }

                            // AP190730: Leave parameter references as-is, they will be substituted at expansion

                            // Apply all expansione: EQVs first, and macros later
                            // If this is a macro invocation, it must be expanded
                            applyEQVs(EQVs, tokenLine);
                            if (attemptMACROInvocation(tokenLine, program, tokenProgram, lineIdx, MACROs, errors)) {
                                // Need to rescan the body in order to capture its labels
                                lineIdx--;
                                continue;
                            }

                            // AP190723: Invalid macro parameters will be found in the assembling step

                            macroBody.add(line);

                        }

                        // If we get here, then we just tokenized the last line without finding any .end_macro token - this is wrong
                        errors.add(new ErrorMessage(program, line.lineNumber, 0, "Reached end of file while reading macro definition"));
                        throw new ProcessingException(errors);
                    }

                    //</editor-fold>

                    // WIP
                    //<editor-fold defaultstate="collapsed" desc=".include">
                    case INCLUDE: {
                        // Only syntax allowed here is: .include <QUOTED_STRING>
                        if (tokenLine.size() != 2 || !(tokenLine.get(1) instanceof StringToken)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, 0, "Malformed include directive"));
                            throw new ProcessingException(errors);
                        }

                        // Attempt to read the file right away
                        Path filename = Path.of(((StringToken) tokenLine.get(1)).value);
                        List<String> includedLines;
                        try {
                            includedLines = Files.readAllLines(filename);
                        }
                        catch (IOException ex) {
                            errors.add(new ErrorMessage(program, line.lineNumber, 0, "Could not access file \"" + filename + "\": " + ex.getMessage()));
                            throw new ProcessingException(errors);
                        }

                        // TODO - actually tokenize the lines, put them in the program list, and continue
                        errors.add(new ErrorMessage(program, line.lineNumber, 0, "Includes are not implemented yet"));
                        throw new ProcessingException(errors);
                    }
                    //</editor-fold>

                    // No .end_macro can appear here; those are consumed in the .macro case
                    //<editor-fold defaultstate="collapsed" desc=".end_macro">
                    case END_MACRO: {
                        errors.add(new ErrorMessage(program, line.lineNumber, 0, "No macro to end here"));
                        throw new ProcessingException(errors);
                    }
                    //</editor-fold>
                }
                
            }


            // Not a source directive, these tokens cannot appear anywhere else - throw if there is a "stray" one
            // AP190925: Maybe this is unnecessary... at least here
            if (tokenLine.stream().anyMatch(SOURCE_DIRS::contains)) {
                errors.add(new ErrorMessage(program, line.lineNumber, 0, "Stray source directive token"));
                throw new ProcessingException(errors);
            }


            // Apply all expansione: EQVs first, and macros later
            // If this is a macro invocation, it must be expanded
            applyEQVs(EQVs, tokenLine);
            // AP200428: Must consider cases where an EQV introduces a new label def!
            tokenLine = extractLabelDefinitions(Labels, program, line, currentAddress, EQVs, errors);
            // Additional check for lines that contained only label defs
            if (tokenLine.isEmpty()) continue;
            if (attemptMACROInvocation(tokenLine, program, tokenProgram, lineIdx, MACROs, errors)) {
                // Need to rescan the body in order to capture its labels
                lineIdx--;
                continue;
            }

            // Check first token again after expansions, but this time deal with the other directives exclusively
            first = tokenLine.get(0);
            if (first instanceof DirectiveToken) {
                Directive dir = ((DirectiveToken) first).value;
                switch (dir) {

                    //<editor-fold defaultstate="collapsed" desc=".globl">
                    case GLOBL: {
                        // Look up the labels to modify in the map

                        for (Token<?> token : tokenLine.subList(1, tokenLine.size())) {
                            if (!(token instanceof IdentifierToken)) {
                                errors.add(new ErrorMessage(program, line.lineNumber, 0, "Expected a label identifier"));
                                throw new ProcessingException(errors);
                            }

                            String ident = ((IdentifierToken) token).value;

                            // If the label hasn't been defined, declare it in the map
                            if (Labels.containsKey(ident))
                                Labels.get(ident).global = true;
                            else
                                Labels.put(ident, new Label(-1, false, true));
                        }

                        // AP200402: Directive is actually consumed, do not include it in the assembly transcript
                        continue;
                    }
                    //</editor-fold>

                    // - .extern may freely appear in any segment, without compromising functionality
                    // AP190927 - CAUTION: Asserting global segment follows extern segment, used for segmentation checks
                    // AP200412 - CAUTION: .extern originally allows overlapping to next segs, including .data itself!
                    //<editor-fold defaultstate="collapsed" desc=".extern">
                    case EXTERN: {
                        if (tokenLine.size() < 2) {
                            errors.add(new ErrorMessage(program, line.lineNumber, first.position, "Expected identifier"));
                            throw new ProcessingException(errors);
                        }

                        Token<?> identToken = tokenLine.get(1);

                        if (!(identToken instanceof IdentifierToken)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, 0, "Invalid identifier"));
                            throw new ProcessingException(errors);
                        }

                        String ident = ((IdentifierToken) identToken).value;

                        if (Labels.containsKey(ident)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, identToken.position, "Label redefinition"));
                            throw new ProcessingException(errors);
                        }

                        if (tokenLine.size() < 3 || !(tokenLine.get(2) instanceof IntegerToken)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, 0, "Expected byte length after identifier"));
                            throw new ProcessingException(errors);
                        }

                        if (tokenLine.size() > 3) {
                            errors.add(new ErrorMessage(program, line.lineNumber, tokenLine.get(2).position, "Expected newline after extern directive"));
                            throw new ProcessingException(errors);
                        }

                        // All is ok, process the directive
                        Labels.put(ident, new Label(externAddress, true, true));
                        externAddress += ((IntegerToken) tokenLine.get(2)).value;
                        // Actually, not yet: Check if extern caret overflowed the segment
                        if (externAddress >= machine.configuration().getAddress(Memory.Descriptor.GLOBAL_POINTER)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, 0, "Out of extern memory"));
                            throw new ProcessingException(errors);
                        }

                        // AP200402: Directive is actually consumed, do not include it in the assembly transcript
                        continue;

                    }
                    //</editor-fold>


                    // Originally, there were instances of directives being declared in the same line of a seg directive
                    // AP190927 - CAUTION: Using simulator functions for address checking, watch out for misunderstandings
                    // ^^^^^^^^: Also, decide which format is allowed for specifying addresses
                    //<editor-fold defaultstate="collapsed" desc=".data .text .kdata .ktext">
                    // Analyze for possible segment directives
                    case DATA:
                    case TEXT:
                    case KDATA:
                    case KTEXT: {
                        int targetAddress = 0;
                        boolean explicit = false;

                        // May specify target address
                        // AP200428: This is one, if not the only instance, where a label can't take the place of an integer
                        // Also, MARS does not give errors for putting anythng else after such directive, we'll notify the programmer through a warning
                        IntegerToken addrToken = null;
                        if (tokenLine.size() > 1) {

                            if (tokenLine.get(1) instanceof IntegerToken) {

                                addrToken = ((IntegerToken) tokenLine.get(1));
                                explicit = true;
                                targetAddress = addrToken.value;
                            }
                            else {
                                errors.add(new ErrorMessage(true, program, line.lineNumber, tokenLine.get(1).position, "Expected address literal, ignoring rest of line"));
                            }

                            if (tokenLine.size() > 2) {
                                errors.add(new ErrorMessage(true, program, line.lineNumber, tokenLine.get(2).position, "Ignoring rest of line"));
                            }
                        }


                        // Memorize caret in the current segment
                        switch (segment) {
                            case 0:
                                textAddress = currentAddress;
                                break;
                            case 1:
                                dataAddress = currentAddress;
                                break;
                            case 2:
                                ktextAddress = currentAddress;
                                break;
                            case 3:
                                kdataAddress = currentAddress;
                                break;
                            default:
                                errors.add(new ErrorMessage(program, line.lineNumber, 0, "INTERNAL ERROR - Unknown segment code"));
                                throw new ProcessingException(errors);
                        }



                        // Switch segment
                        switch ((Directive) first.value) {
                            case TEXT:
                                if (explicit) {
                                    if (!machine.getMemory().inTextSegment(targetAddress)) {
                                        errors.add(new ErrorMessage(program, line.lineNumber, addrToken.position, "Address outside of .text segment"));
                                        throw new ProcessingException(errors);
                                    }
                                    else currentAddress = targetAddress;
                                }
                                else currentAddress = textAddress;
                                segment = 0;
                                break;
                            case DATA:
                                if (explicit) {
                                    if (!machine.getMemory().inDataSegment(targetAddress)) {
                                        errors.add(new ErrorMessage(program, line.lineNumber, addrToken.position, "Address outside of .data segment"));
                                        throw new ProcessingException(errors);
                                    }
                                    else currentAddress = targetAddress;
                                }
                                else currentAddress = dataAddress;
                                segment = 1;
                                break;
                            case KTEXT:
                                if (explicit) {
                                    if (!machine.getMemory().inKernelTextSegment(targetAddress)) {
                                        errors.add(new ErrorMessage(program, line.lineNumber, addrToken.position, "Address outside of .ktext segment"));
                                        throw new ProcessingException(errors);
                                    }
                                    else currentAddress = targetAddress;
                                }
                                else currentAddress = ktextAddress;
                                segment = 2;
                                break;
                            case KDATA:
                                if (explicit) {
                                    if (!machine.getMemory().inKernelDataSegment(targetAddress)) {
                                        errors.add(new ErrorMessage(program, line.lineNumber, addrToken.position, "Address outside of .kdata segment"));
                                        throw new ProcessingException(errors);
                                    }
                                    else currentAddress = targetAddress;
                                }
                                else currentAddress = kdataAddress;
                                segment = 3;
                                break;
                            default:
                                errors.add(new ErrorMessage(program, line.lineNumber, 0, "INTERNAL ERROR - Unknown segment name"));
                                throw new ProcessingException(errors);
                        }

                        // Directive is processed, continue to next line
                        continue;

                    }
                    //</editor-fold>

                    // STATUS REMINDER:
                    // If we're here, no macro sub has been done, all EQVs are expanded,
                    //      and the line is not a source-label-segment directive, only data directives and MIPS instructions are left
                    // Note that identifiers are still present; they all are label refs,
                    //      which will be resolved at the assembling step when all definitions have been captured

                    // MULTIPLE NOTES:
                    // - multiple data directives cannot appear on the same line

                    // - align and space require exactly one operand: first accepts 0|1|2|3, the other requires space amount in bytes
                    // MISSING: SEGFAULT CHECKS!!!!
                    // MISSING segment overflow check
                    //<editor-fold defaultstate="collapsed" desc=".align">
                    case ALIGN: {

                        // First, verify that the directive is written in a data segment
                        // 0 and 2 denote a text segment, thus the check focuses on LSB being 1
                        if ((segment & 1) == 0) {
                            // We're in a text segment, bail
                            errors.add(new ErrorMessage(program, line.lineNumber, first.position, "Data directives are not allowed in a text segment"));
                            throw new ProcessingException(errors);
                        }


                        // Operand checks: there must be exactly one operand, which must be an integer, and within 0 and 3
                        if (tokenLine.size() < 2 || !(tokenLine.get(1) instanceof IntegerToken)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, tokenLine.get(1).position, "Expected integer"));
                            throw new ProcessingException(errors);
                        }

                        int alignDes = ((IntegerToken) tokenLine.get(1)).value;
                        if (alignDes < 0 || alignDes > 3) {
                            errors.add(new ErrorMessage(program, line.lineNumber, tokenLine.get(1).position, "Integer must be between 0 and 3"));
                            throw new ProcessingException(errors);
                        }

                        if (tokenLine.size() > 2) {
                            errors.add(new ErrorMessage(program, line.lineNumber, tokenLine.get(2).position, "Expected end of line"));
                            throw new ProcessingException(errors);
                        }

                        // All good, perform the necessary alignment
                        // Get the right byte count for the requested boundary
                        Boundary b;
                        switch (alignDes) {
                            case 0:
                                //Basically, a no-op...
                                // AP200411: CAUTION - Employing the formula actually increases the current address by one byte, which is unwanted. Bail out immediately!
                                b = Boundary.BYTE;
                                break;
                            case 1:
                                b = Boundary.HALF_WORD;
                                break;
                            case 2:
                                b = Boundary.WORD;
                                break;
                            case 3:
                                b = Boundary.DOUBLE_WORD;
                                break;
                            default:
                                // We've already checked for domain membership, should not happen
                                errors.add(new ErrorMessage(program, line.lineNumber, tokenLine.get(2).position, "INTERNAL ERROR: Invalid segment descriptor"));
                                throw new ProcessingException(errors);
                        }

                        // If the alignment requirement is not changing, it might be safely removed as well
                        if (b == (segment == 1 ? dataCurrentBoundary : kdataCurrentBoundary)) {
                            continue;
                        }

                        // See how many bytes are off the alignment
                        int tail = currentAddress % b.size;

                        if (tail == 0) continue; // No-op in this case too, although not so evident for issuing a linter hint

                        // Finally, there is an actual alignment to perform
                        int increment = b.size - tail;
                        // NOTE: Translate this directive to an equivalent .ascii, for leaner code afterwards
                        datasegs[segment / 2].put(currentAddress, new DataDir(Directive.ASCII, List.of(new StringToken(generateQuotedZeroString(increment), program, 0, 0))));
                        currentAddress += increment;

                        // Change the current boundary to be this one
                        if (segment == 1) {
                            dataCurrentBoundary = b;
                        }
                        else {
                            kdataCurrentBoundary = b;
                        }

                        // We're done
                        continue;
                    }
                    //</editor-fold>


                    // - align and space require exactly one operand: first accepts 0|1|2|3, the other requires space amount in bytes
                    // MISSING: SEGFAULT CHECKS!!!!
                    // MISSING segment overflow check
                    // MISSING: the data fill directives align based on themselves (.space aligns as a .byte)
                    //<editor-fold defaultstate="collapsed" desc=".space"> 
                    case SPACE: {

                        // First, verify that the directive is written in a data segment
                        // 0 and 2 denote a text segment, thus the check focuses on LSB being 1
                        if ((segment & 1) == 0) {
                            // We're in a text segment, bail
                            errors.add(new ErrorMessage(program, line.lineNumber, first.position, "Data directives are not allowed in a text segment"));
                            throw new ProcessingException(errors);
                        }
                        // Operand checks: there must be exactly one operand, which must be an integer
                        if (tokenLine.size() < 2 || !(tokenLine.get(1) instanceof IntegerToken)) {
                            errors.add(new ErrorMessage(program, line.lineNumber, first.position, "Expected an integer for .space"));
                            throw new ProcessingException(errors);
                        }
                        if (tokenLine.size() > 2) {
                            errors.add(new ErrorMessage(program, line.lineNumber, tokenLine.get(2).position, "Expected end of line"));
                            throw new ProcessingException(errors);
                        }

                        int increment = ((IntegerToken) tokenLine.get(1)).value;

                        // Move the current address and continue
                        // NOTE: Translate this directive to an equivalent .ascii, for leaner code afterwards
                        datasegs[segment / 2].put(currentAddress, new DataDir(Directive.ASCII, List.of(new StringToken(generateQuotedZeroString(increment), program, 0, 0))));
                        currentAddress += increment;

                        continue;
                    }
                    //</editor-fold>

                    // - the data insertions may span multiple lines
                    // - values outside the range of the specified directive should raise a truncation warning
                    // - labels may naturally sit in as words to be loaded in .data; usual truncation warnings are issued for smaller integer formats
                    // - labels are NOT strings
                    // MISSING: SEGFAULT CHECKS!!!!
                    // MISSING segment overflow check
                    // MISSING Truncation warnings for integer values
                    // MISSING: the data fill directives align based on themselves! (.space aligns as a .byte)
                    //<editor-fold defaultstate="collapsed" desc=".ascii .asciiz .byte .half .word .float .double">
                    case ASCII:
                    case ASCIIZ:
                    case BYTE:
                    case HALF:
                    case WORD:
                    case FLOAT:
                    case DOUBLE: {

                        // First, verify that the directive appears in a data segment
                        // 0 and 2 denote a text segment, thus the check focuses on LSB being 1
                        if ((segment & 1) == 0) {
                            // We're in a text segment, bail
                            errors.add(new ErrorMessage(program, line.lineNumber, first.position, "Data directives are not allowed in a text segment"));
                            throw new ProcessingException(errors);
                        }

                        // METHOD: Loop until the next line does impart a new directive, or an illegal token is encountered
                        List<Token<?>> list = tokenLine.subList(1, tokenLine.size());
                        List<Token<?>> dat = new ArrayList<>();

                        int addr = currentAddress;

                        while (true) {
                            // for each token we encounter
                            for (Token tk : list) {

                                // Check if the token is good for the current directive
                                if (!isLiteralForDirective(tk, dir)) {
                                    errors.add(new ErrorMessage(program, line.lineNumber, tk.position, "Bad literal for data directive"));
                                    throw new ProcessingException(errors);
                                }

                                // Token is of valid type, perform the increments and continue
                                dat.add(tk);
                                currentAddress += DATA_INCREMENTS.get(((DirectiveToken) first).value).applyAsInt(tk);

                            }

                            // Before peeking, check if we reached the end
                            if (lineIdx + 1 == tokenProgram.size()) {
                                // Program is over, and so is this directive
                                break lineLoop;
                            }
                            // Peek the next line, and see if it is a continuation of this directive
                            SourceLine nextLine = tokenProgram.get(lineIdx + 1);
                            List<Token<?>> nlTokens = nextLine.tokens;

                            if ((nlTokens.size() > 0 && nlTokens.get(0) instanceof DirectiveToken) || (nlTokens.size() > 2 && nlTokens.get(2) instanceof DirectiveToken)) {
                                // Next line is a new (tentative) directive, bail out
                                break;
                            }
                            else {
                                // Shift parsing to next line, and loop
                                lineIdx++;
                                line = nextLine;
                                // AP200426: Do extract any label defs here!
                                // NOT tokenLine, but list
                                list = extractLabelDefinitions(Labels, program, line, currentAddress, EQVs, errors);
                            }
                        }

                        // Record the directive in the right segment, and we're done
                        datasegs[segment / 2].put(addr, new DataDir(((DirectiveToken) first).value, dat));
                        continue;
                    }
                    //</editor-fold>


                }
            }

            // Now, only cases involving instructions are left, anything else must be malformed
            // AP200413: Add the line directly to the right segment map, we'll deal with the syntax later
            // ASSERTIONS: One line always yields one instruction
            // Check that the current address is inside a text segment
            //<editor-fold defaultstate="collapsed" desc="TEXT SEGMENT">
            if (tokenLine.get(0) instanceof OperatorToken) {
                if (segment != 0 && segment != 2) {
                    errors.add(new ErrorMessage(ErrorMessage.WARNING, program, line.lineNumber, 0, "Instructions are ignored in .data segments"));
                    //throw new ProcessingException(errors);
                }

                if ((!machine.getMemory().inTextSegment(currentAddress) && segment == 0)
                        || (!machine.getMemory().inKernelTextSegment(currentAddress) && segment == 2)) {

                    errors.add(new ErrorMessage(program, line.lineNumber, 0, "Crossed .?text segment boundary!"));
                    throw new ProcessingException(errors);
                }

                // Pick the right map
                HashMap<Integer, SourceLine> seg = textsegs[segment / 2];

                // Register the source line to assemble
                SourceLine put = seg.put(currentAddress, line);

                // Ckeck for overwriting scenarios
                if (put != null) {
                    errors.add(new ErrorMessage(program, line.lineNumber, 0, "Overwriting an already present instruction"));
                    throw new ProcessingException(errors);
                }

                // Increment the address pointer
                currentAddress += 4;

            }
            else {
                // I really don't know what it can be at this point... Drop the line and log a warning nonetheless
                errors.add(new ErrorMessage(ErrorMessage.WARNING, program, line.lineNumber, 0, "Unparsable line, dropped"));

            }
            //</editor-fold>


        }

        return s;
    }
    
    //</editor-fold>


    //<editor-fold defaultstate="collapsed" desc="ASSEMBLING & LOADING">
    
    /**
     * Loads all preprocessed data directives into the machine's memory
     *
     * @param program
     * @param mappings
     * @param machine
     * @throws ProcessingException
     */
    static void loadData(MIPSprogram program, HashMap<Integer, DataDir> mappings, MIPSMachine machine, Map<String, Label> Labels, ErrorList errors) throws ProcessingException {

        // Asserting the mapped addresses are all within their segments, and no overflow occurs

        for (Map.Entry<Integer, DataDir> entry : mappings.entrySet()) {
            int address = entry.getKey();
            DataDir datadir = entry.getValue();

            // Select the right action for the directive
            boolean asciiz = false;

            ArrayList<byte[]> dataBuffer;
            switch (datadir.type) {

                //<editor-fold defaultstate="collapsed" desc="strings">
                case ASCIIZ:
                    asciiz = true;
                case ASCII: {

                    dataBuffer = new ArrayList<>();

                    // for each (guaranteed) quoted string
                    for (StringToken tk : (List<StringToken>) (List<?>) datadir.values) {

                        // Get the literal
                        String literal = tk.value;

                        // Discern between the Z and non-Z variants
                        if (asciiz) literal += "\0";

                        // AP200412 - Char transcoding details:
                        // - Malformed inputs shouldn't happen AFAIK, so report them if they happen
                        // - Unmappable chars are replaced with a placeholder, which in this case is a question mark '?'
                        ByteBuffer data;
                        try {
                            data = ASCII_ENCODER
                                    .onMalformedInput(CodingErrorAction.REPORT)
                                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                                    .encode(CharBuffer.wrap(literal));
                        }
                        catch (CharacterCodingException ex) {
                            errors.add(new ErrorMessage(program, tk.lineNumber, tk.position, "String encoding error: " + ex.getMessage()));
                            throw new ProcessingException(errors);
                        }

                        // Add the transcoded string to the data to be written in memory
                        dataBuffer.add(data.array());
                    }

                    // "Collect" the arrays into a single one, and send it to memory
                    {
                        int totalBytes = dataBuffer.stream().mapToInt(a -> a.length).sum();
                        byte data[] = new byte[totalBytes];
                        int index = 0;
                        for (byte[] array : dataBuffer) {
                            System.arraycopy(array, 0, data, index, array.length);
                            index += array.length;
                        }
                        machine.getMemory().loadBytes(data, address);
                    }
                    return;
                }
                //</editor-fold>

                //<editor-fold defaultstate="collapsed" desc="integers">
                case BYTE:
                case HALF:
                case WORD:
                    // METHOD: Loop until the next line does impart a new directive, or an illegal token is encountered
                    // REMINDER (CAUTION): MARS is litte-endian!
                    dataBuffer = new ArrayList<>();

                    // for each token we encounter
                    for (Token<?> tk : datadir.values) {


                        int val;
                        if (tk instanceof IdentifierToken) {
                            // This is a label reference, change the string to one having the actual address

                            String ident = ((IdentifierToken) tk).value;

                            Label l = Labels.get(ident);
                            if (l == null || !l.valid) {
                                // Undefined label
                                errors.add(new ErrorMessage(program, tk.lineNumber, tk.position, "Undefined label"));
                                throw new ProcessingException(errors);
                            }

                            // We have a good label, proceed
                            val = l.address;
                        }
                        else {
                            // Assume IntegerToken
                            val = ((IntegerToken) tk).value;
                        }



                        try {
                            switch (datadir.type) {
                                case WORD: {
                                    ByteBuffer data = ByteBuffer.allocate(Integer.BYTES);
                                    data.order(ByteOrder.LITTLE_ENDIAN);
                                    data.putInt(val);
                                    // Add the transcoded string to the data to be written in memory
                                    dataBuffer.add(data.array());
                                    break;
                                }
                                case HALF: {
                                    if (val < Short.MIN_VALUE || val > Short.MAX_VALUE) {
                                        errors.add(new ErrorMessage(program, tk.lineNumber, tk.position, "Value outside of half-word bounds"));
                                        throw new ProcessingException(errors);
                                    }
                                    ByteBuffer data = ByteBuffer.allocate(Short.BYTES);
                                    data.order(ByteOrder.LITTLE_ENDIAN);
                                    data.putShort((short) val);
                                    // Add the transcoded string to the data to be written in memory
                                    dataBuffer.add(data.array());
                                    break;
                                }
                                default:
                                    if (val < Byte.MIN_VALUE || val > Byte.MAX_VALUE) {
                                        errors.add(new ErrorMessage(program, tk.lineNumber, tk.position, "Value outside of byte bounds"));
                                        throw new ProcessingException(errors);
                                    }
                                    // Assume byte
                                    dataBuffer.add(new byte[] {(byte) val});
                            }
                        }
                        catch (NumberFormatException ex) {
                            errors.add(new ErrorMessage(program, tk.lineNumber, tk.position, "Integer is out of '" + datadir.type.descriptor + "' range"));
                            throw new ProcessingException(errors);
                        }

                    }


                    // "Collect" the arrays into a single one, and send it to memory
                     {
                        int totalBytes = dataBuffer.stream().mapToInt(a -> a.length).sum();
                        byte data[] = new byte[totalBytes];
                        int index = 0;
                        for (byte[] array : dataBuffer) {
                            System.arraycopy(array, 0, data, index, array.length);
                            index += array.length;
                        }
                        machine.getMemory().loadBytes(data, address);
                    }
                    continue;
                //</editor-fold>

                //<editor-fold defaultstate="collapsed" desc="reals">
                case FLOAT:
                case DOUBLE:
                    errors.add(new ErrorMessage(program, datadir.values.get(0).lineNumber, 0, "Floating point directives are not implemented yet"));
                    throw new ProcessingException(errors);
                //</editor-fold>

            }
        }

    }

    // EXTERNAL HACKS REQUIRED:
    // - JAL has two distinct syntaxes by spec, the second one having an additional register than the first
    // - SYSCALL, BREAK, and all non-immediate traps may specify an additional code value, having either 10 bits for traps, or 20 otherwise
    static InstructionSyntax getBasicSyntax(String operator) {
        switch (operator) {

            // [ZrdshamtRinstr]
            case "mult":
            case "multu":
            case "div":
            case "divu":
            case "madd":
            case "maddu":
            case "msub":
            case "msubu":
                return InstructionSyntax.R2;
            case "tge": // TODO CODE MISSING!!
            case "tgeu": // TODO CODE MISSING!!
            case "tlt": // TODO CODE MISSING!!
            case "tltu": // TODO CODE MISSING!!
            case "teq": // TODO CODE MISSING!!
            case "tne": // TODO CODE MISSING!!
                return InstructionSyntax.R2C10;

            // [ZshamtReversedRinstr]
            case "sllv":
            case "srlv":
            case "srav":
            // [ZshamtRinstr]
            case "mul":
            case "add":
            case "addu":
            case "sub":
            case "subu":
            case "and":
            case "or":
            case "xor":
            case "nor":
            case "slt":
            case "sltu":
            case "movz":
            case "movn":
                return InstructionSyntax.R3;

            // [ZrsRinstr]
            case "sll":
            case "srl":
            case "sra":
                return InstructionSyntax.R2I5;

            // [ArithmIinstr]
            case "addi":
            case "addiu":
            case "andi":
            case "ori":
            case "xori":
            case "slti":
            case "sltiu":
                return InstructionSyntax.R2I16;

            // [LoadStoreIinstr]
            case "lb":
            case "lh":
            case "lwl":
            case "lw":
            case "lbu":
            case "lhu":
            case "lwr":
            case "sb":
            case "sh":
            case "swl":
            case "sw":
            case "swr":
            case "ll":
            case "sc":
                return InstructionSyntax.LS;

            // [BranchFuncIinstr]
            case "bgez":
            case "bgezal":
            case "bgtz":
            case "blez":
            case "bltz":
            case "bltzal":
                return InstructionSyntax.R1A16;
            case "tgei":
            case "tgeiu":
            case "tlti":
            case "tltiu":
            case "teqi":
            case "tnei":
                return InstructionSyntax.R1I16;

            // [BranchIinstr]
            case "beq":
            case "bne":
                return InstructionSyntax.R2A16;


            // [HiLoRinstr]
            case "mfhi":
            case "mflo":
            case "mthi":
            case "mtlo":
                return InstructionSyntax.R1;

            // [Jinstr]
            case "j":
            case "jal":
                return InstructionSyntax.A26;

            // [CountBitsRinstr]
            case "clo":
            case "clz":
                return InstructionSyntax.R2;

            // CUSTOM: lui REG, IMM
            case "lui":
                return InstructionSyntax.R1I16;

            // CUSTOM: jr REG
            case "jr":
                return InstructionSyntax.R1;

            // CAUTION: Two distinct binary forms are specified for a JAL! A possible solution is to return R2 and implement a hack in the pattern matcher
            //    jalr REG
            //    jalr REG, REG
            // NOTE: The hint field is always set to zero by R2000 spec
            case "jalr":
                return InstructionSyntax.R2; // CAN BE R1, CAUTION!

            case "eret":
            case "nop":
            case "syscall": // TODO CODE MISSING!!
            case "break": // TODO CODE MISSING!!
                return InstructionSyntax.E;


            // CAUTION: Immediate is 3-bit long
            case "movf":
            case "movt":
                return InstructionSyntax.R2I3;



            default:
                // Bad operator, it most probably is a pseudo-operator
                return null;
        }
    }

    /**
     * Given a list of tokens that model a MIPS instruction, returns its binary
     * encoding in a single 'word'.
     *
     * @param program
     * @param line
     * @param errors
     * @param opAddress
     * @param extendedAssemblerEnabled
     * @param Labels
     * @return
     * @throws ProcessingException
     */
    static int encodeInstruction(MIPSprogram program, List<Token<?>> line, ErrorList errors, int opAddress, boolean extendedAssemblerEnabled, Map<String, Label> Labels) throws ProcessingException {

        // Check if the first token is indeed an operator
        Token<?> operatorToken = line.get(0);
        if (!(operatorToken instanceof OperatorToken)) {
            errors.add(new ErrorMessage(program, operatorToken.lineNumber, operatorToken.position, "Expected an instruction operator"));
            throw new ProcessingException(errors);
        }

        String operator = ((OperatorToken) operatorToken).value;
        InstructionSyntax syn = getBasicSyntax(operator);

        if (syn == null) {
            throw new RuntimeException("INTERNAL ERROR: Unrecognized operator");
        }

        Class[] pattern = syn.sequence;

        int[] operands = new int[] {0, 0, 0};
        int i = 0;
        for (; i < pattern.length; i++) {

            // Check if line is already exausted
            if (line.size() - 1 == i) {

                //<editor-fold defaultstate="collapsed" desc="Exceptions">
                // Maybe the instruction expected a code, check for both syntaxes
                if (syn == InstructionSyntax.C20 && i == 0) {
                    // Should be either a break or a syscall, operands are set to zero already
                    break;
                }
                if (syn == InstructionSyntax.R2C10 && i == 2) {
                    // R-type trap instruction the reasoning above applies all the same
                    break;
                }

                // HACK: JALR also accepts an R1 syntay insted of R2
                if (operator.equals("jalr") && i == 1) {
                    // Last reg has been left implicit, is $31
                    operands[i] = Registers.Descriptor.$ra.ordinal();
                    break;
                }
                //</editor-fold>

                // Barring the aforedealt cases, this line is clearly missing something
                errors.add(new ErrorMessage(program, line.get(i).lineNumber, line.get(i).position, "Unexpected end of line"));
                throw new ProcessingException(errors);

            }

            Token<?> candidate = line.get(i + 1);

            //<editor-fold defaultstate="collapsed" desc="PARENTHESES">
            if (pattern[i] == RightParenToken.class || pattern[i] == LeftParenToken.class) {

                if (candidate.getClass() != pattern[i]) {
                    errors.add(new ErrorMessage(program, candidate.lineNumber, candidate.position, "Expected parenthesis"));
                    throw new ProcessingException(errors);
                }

                // Parenthesis recognized correctly
                continue;
            }
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="REGISTER">
            if (pattern[i] == RegisterToken.class) {

                if (candidate.getClass() != pattern[i]) {
                    errors.add(new ErrorMessage(program, candidate.lineNumber, candidate.position, "Expected a register"));
                    throw new ProcessingException(errors);
                }

                // Candidate is effectively a register, save the operand
                operands[i] = ((RegisterToken) candidate).value.ordinal();
                continue;
            }
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="IDENTIFIER">
            if (pattern[i] == IdentifierToken.class) {

                int address;
                if (candidate.getClass() == IdentifierToken.class) {
                    // There is a label reference, need to check if it is defined
                    Label l = Labels.get(((IdentifierToken) candidate).value);

                    if (l == null || !l.valid) {
                        errors.add(new ErrorMessage(program, candidate.lineNumber, candidate.position, "Undefined label"));
                        throw new ProcessingException(errors);
                    }

                    address = l.address;
                }
                else if (candidate.getClass() == IntegerToken.class) {
                    // Ehhhhhhhhhhhhhhhhhhhhhhhhhhh... throw a warning?

                    address = ((IntegerToken) candidate).value;
                }
                else {
                    errors.add(new ErrorMessage(program, candidate.lineNumber, candidate.position, "Expected a label"));
                    throw new ProcessingException(errors);
                }

                // Now check that this value can be correctly encoded inside the instruction
                // AP200422 - CAUTION: If we're here, then the instruction expects a label pointing to text segments, and thus its 2 LSBs are truncated;
                // Throw a warning on that too - BEWARE OF INTEGER INTERACTION
                if ((address & 0b11) != 0) {
                    // WARNING
                }
                address >>>= 2;

                int diff = Math.abs((opAddress >>> 2) - address);

                switch (syn) {
                    case A26:
                        if (diff > 0x03FFFFFF) {
                            // Truncation warning
                        }
                        break;

                    case R2A16:
                    case R1A16:
                        if (diff > 0x0000FFFF) {
                            // Truncation warning
                        }
                        break;

                    default:
                        throw new RuntimeException("INTERNAL ERROR: Unexpected syntax type");

                }

                operands[i] = address;
                continue;
            }
            //</editor-fold>

            //<editor-fold defaultstate="collapsed" desc="INTEGER">
            if (pattern[i] == IntegerToken.class) {

                int immediate;
                if (candidate.getClass() == IdentifierToken.class) {
                    // Ehhhhhhhhhhhhhhhhhhhhhhhhhhh... throw a warning?
                    // NOT IN CASE OF LS! AP200422: Actually, a warning is needed, since a label may convey the wrong message in telling to point to an arbitrary mem location, when it has only 16 bits

                    // There is a label reference, need to check if it is defined
                    Label l = Labels.get(((IdentifierToken) candidate).value);

                    if (l == null || !l.valid) {
                        errors.add(new ErrorMessage(program, candidate.lineNumber, candidate.position, "Undefined label"));
                        throw new ProcessingException(errors);
                    }

                    immediate = l.address;
                }
                else if (candidate.getClass() == IntegerToken.class) {

                    immediate = ((IntegerToken) candidate).value;
                }
                else {
                    errors.add(new ErrorMessage(program, candidate.lineNumber, candidate.position, "Expected an integer"));
                    throw new ProcessingException(errors);
                }

                // Now check that this value can be correctly encoded inside the instruction
                switch (syn) {
                    case C20:
                        if (immediate > 0x000FFFFF) {
                            // Truncation warning
                        }
                        break;

                    case R2I16:
                    case R1I16:
                    case LS:
                        if (immediate > 0x0000FFFF) {
                            // Truncation warning
                        }
                        break;

                    case R2C10:
                        if (immediate > 0x000003FF) {
                            // Truncation warning
                        }
                        break;

                    case R2I5:
                        if (immediate > 0x0000001F) {
                            // Truncation warning
                        }
                        break;

                    case R2I3:
                        if (immediate > 0x00000008) {
                            // Truncation warning
                        }
                        break;

                    default:
                        throw new RuntimeException("INTERNAL ERROR: Unexpected syntax type");

                }

                operands[i] = immediate;
                continue;
            }
            //</editor-fold>

            // We should never be here
            errors.add(new ErrorMessage(program, candidate.lineNumber, candidate.position, "INTERNAL ERROR: Bad token type in syntax pattern"));
            throw new ProcessingException(errors);
        }

        // Check for excess tokens
        if (i < line.size() - 1) {
            errors.add(new ErrorMessage(program, line.get(i + 1).lineNumber, line.get(i + 1).position, "Expected end of line"));
            throw new ProcessingException(errors);
        }

        // Finally, the line is correct, and operands are extracted. Encode the instruction
        return InstructionSetArchitecture.BasicInstructionEncodings.get(operator).applyAsInt(operands);

    }

    /**
     * Applies the
     *
     * @param program
     * @param segMap
     * @param extendedAssemblerEnabled
     * @param machine
     * @throws ProcessingException
     */
    static void loadText(MIPSprogram program, HashMap<Integer, SourceLine> segMap, boolean extendedAssemblerEnabled, MIPSMachine machine, ErrorList errors, Map<String, Label> Labels) throws ProcessingException {

        for (Map.Entry<Integer, SourceLine> entry : segMap.entrySet()) {
            int address = entry.getKey();
            SourceLine instruction = entry.getValue();

            machine.getMemory().loadInstruction(encodeInstruction(program, instruction.tokens, errors, address, extendedAssemblerEnabled, Labels), address);

        }

    }

    //</editor-fold>

    
    
    // AP200423: Some nice EQV shenanigans to exemplify their possible misuse
    // -------------------------------------------------------------------------
    /*
     * AP180627 IMPORTANT: Look for any EQVs to substitute before checking if
     * this line contains an EQV itself; this enforces the design constraint of
     * defining macros before use. Otherwise, weird cases might occur! For
     * example, this program:
     * -------------------------------------------------------------------------
     *
     * .eqv A B
     *
     * .eqv B nop
     *
     * A
     *
     * -------------------------------------------------------------------------
     * would successfully assemble into a "nop", despite the B macro being used
     * before its definition
     *
     * AP180710: Actually, it's worse: it will leave an A as an assembly token.
     * Cases like the following one are even more confusing:
     * -------------------------------------------------------------------------
     *
     * .eqv A div
     *
     * .eqv div mov
     *
     * A
     *
     * -------------------------------------------------------------------------
     *
     * would assemble into a div instruction instead of the expected mov
     *
     */
    //
    // AP190718: DESIGN DECISIONS:
    // - Deprecating "Assemble all", use includes to specify dependencies
    // - Deprecating "Exception handler option", treat it as any other assembly file, for now
    // In general, the directives altogether make up a "preprocessing" method, much akin to the C preprocessor
    ////////////////////////////////////////////////////////////////////////
    // TOKENIZATION
    //
    // Plain, old tokenization, each token kind having its own subclass of Token<?>. Each line is tokenized, and then scanned for:
    //      - Label definitions, and relative name clash checks
    //      - source directives (.include, .eqv, .macro/.end_macro)
    //      - EQV substitutions
    //      - MACRO substitutions
    //      - label directives (.globl, .extern)
    //      - segment directives (.data, .text, .kdata, .ktext)
    // All directives except the segment ones are consumed by the process.
    //
    // NOTES:
    //      - Quoted strings are immune to symbols
    //      - Directives transcend the basic/extended conundrum: they are actually necessary in order to write in different segments
    //      - EQVs are *eager*: if a reference is inside the body of another EQV, then the reference is resolved before recording the latter
    //      - Being referenced by their name only, labels and EQVs share the same name space: there cannot be a label and an EQV with the same name, lest potential ambiguities
    //      - Multiple label definitions on a single line are allowed, and will not compromise functionality (it might be useful to present some use cases...)
    //      - Labels can be defined at the beginning of any line, even directive ones: the current address cursor will always be used
    //
    // ISSUES:
    //      - Also, how goes the inclusion method? Depth-first? Forbid recursions? Forbid repetitions altogether? Automatic pragma-once? Program inclusion tracking?
    //      - Some more code must be rewritten to fill the tracking classes about relative line numbers, which line comes from which file, original include/macro/eqv lines (if kept), etc...
    //      - Consider the semplifications of forcing directive tokens to appear only at first position, barring label defs
    //
    // POTENTIAL BUGS:
    //      - A label definition inside the body of an EQV will not be captured!
    //      - A label definition preceding a segment may be misleading, in that it will take the address of the preceding segment
    //
    // SOURCE DIRECTIVES: TENTATIVE ORDER:
    // - EQV def    : add to dictionary (Body must not contain any source directive, prevents directive injection in macro body through EQV static substitution; labels allowed for now)
    // - MACRO def  : start recording; do apply EQVs and MACROs preemptively in the body (Any source directive in a macro body is FORBIDDEN, see rationale. Labels, both defs and refs, are completely local)
    // - INCLUDE    : recurse from the beginning onto the included file. Bring include list/tree, and macro/eqv/label dictionaries
    // - Look for eqv references, and apply them
    // - Parse a possible macro invocation, and perform expansion if applicable
    //
    //
    // What MACROs allow:
    // - Source directives NO
    // - eqv refs YES
    // - macro invokes YES
    // - label defs LOCAL
    // - label refs LOCAL
    // 
    // What EQVas allow:
    // - Source directives NO
    // - eqv refs YES
    // - macro invokes YES*
    // - label defs YES(!)
    // - label refs YES
    //  
    // Cautions to heed when designing macros:
    // - Macro definitions are subject to previously defined EQV and MACRO expansions, if any are present
    // - Conversely, macro invocations are NOT!
    // - Labels are entirely local; to "export" a label, use a macro parameter in its place
    // - Barring "exported" labels, .extern functionality is limited, and .globl is effectively useless
    // - Most importantly: On definition, always set which segment you're entering, and which segment you're leaving
    // - Also, specifying a constant address on segment directives will ensure failure on second invocation
    
    /**
     * 
     * @param leadFilename
     * @param extendedAssemblerEnabled
     * @param warningsAreErrors
     * @param errors
     * @return
     * @throws ProcessingException 
     */
    public static ExecutableProgram beginAssembling(Path leadFilename, MIPSMachine machine, boolean extendedAssemblerEnabled, boolean warningsAreErrors, ErrorList errors) throws ProcessingException {
        

        // Construct the source program object, and prepare the (un)necessary fields
        // LEGACY
        MIPSprogram program = new MIPSprogram();
        program.readSource(leadFilename.toString());
    
        

        List<String> lines;
        try {
			lines = Files.readAllLines(leadFilename);
		}
		catch (IOException e) {
			errors.add(new ErrorMessage((MIPSprogram) null, 0, 0, e.toString()));
			throw new ProcessingException(errors);
		}
        
        List<SourceLine> tokenProgram = new ArrayList(lines.size());
        
        for (int i = 0; i < lines.size(); i++) {

            // Get the line and tokenize it - Reminder: Comments will be removed
            String line = lines.get(i);
            List<Token<?>> tokenLine = tokenize(program, i + 1, line, extendedAssemblerEnabled, errors);

            // Don't add empty lines, simplifies all checks
            if (!tokenLine.isEmpty())
                tokenProgram.add(new SourceLine(line, tokenLine, leadFilename.toString(), i + 1));

        }

        // ------ REMINDER: From now on, lines have at least one token -------



        // Source/label preprocessing
        // This invocation:
        // - Consumes label definitions, and adds them to an address map
        // - Completely processes all macros and eqvs [TODO includes]
        // - Processes .globl and .extern directives
        // - Processes all segment directives
        // - Maps data directives to their respective addresses, labels are unresolved
        // - Maps text instructions to their respective addresses, labels are unresolved
        ExecutableProgram s = preprocessSourceUnit(program, tokenProgram, machine, new HashMap<>(), new HashSet<>(), new HashMap<>(), errors);

        // LEGACY - REDO
        if (errors.errorLimitExceeded())
            throw new ProcessingException(errors);

        // Status:
        // Program is split into 4 maps, one per segment, each entry with its address.
        // Labels are all mapped, they need to be expanded in the whole program.
        // Nothing else.
        // DATA
        loadData(program, s.datasegs[0], machine, s.Labels, errors);

        // KDATA
        loadData(program, s.datasegs[1], machine, s.Labels, errors);

        // TEXT
        loadText(program, s.textsegs[0], extendedAssemblerEnabled, machine, errors, s.Labels);

        // KTEXT
        loadText(program, s.textsegs[1], extendedAssemblerEnabled, machine, errors, s.Labels);
        
        return s;
    }

}
