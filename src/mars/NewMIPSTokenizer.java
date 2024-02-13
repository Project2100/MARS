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

package mars;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import mars.assembler.Directive;
import mars.assembler.Token;
import mars.assembler.TokenList;
import mars.assembler.TokenType;
import mars.assembler.Tokenizer;

/**
 * Labels and eqvs share the same namespace
 * Eager evaluation of eqv bodies - we do not permit symbol redefinitions
 * Labels are unaffected by EQVs - for now
 * EQV redefinitions are allowed only if the body is effectively the same - allows for definitions inside macros
 *
 * @author Project2100
 */
public class NewMIPSTokenizer {


	Map<String, List<Token>> equivalents = new HashMap<>();
	MacroIndex mi = new MacroIndex();
	Set<Path> inclFiles = new HashSet<>();
	Set<String> labels = new HashSet<>();


	//Requirements:
	// 

	// XXX IMPORTANT: .include does not support eqvs
	// XXX: currently, ech file can be included at most once inside an assembly unit
	List<TokenList> buildTokenizedProgram(Path directory, Path filename) {

		// If filename is abolute, this call simply assigns it to file
		Path file = directory.resolve(filename);

		// Get lines as plain strings
		List<String> lines;
		try {
			lines = Files.readAllLines(file);
		}
		catch (IOException ex) {
			throw new RuntimeException("Unable to read file " + file, ex);
		}

		// If map is empty, it means we're reading the entry point; add it now to avoid recursion onto self
		inclFiles.add(file);

		// tokenize lines into their corresponding token lists
		Tokenizer t = new Tokenizer();
		List<TokenList> tokenizedLines = lines.stream()
				.map((line) -> t.tokenizeLine(null, 0, line, false))
				.collect(Collectors.toList());

		// CAUTION: Process .include directives now - we aim for a final, single program with all includes resolved
		// Iterate over lines
		for (int lineIdx = 0; lineIdx < tokenizedLines.size(); lineIdx++) {
			TokenList tokens = tokenizedLines.get(lineIdx);

			// Iterate over tokens
			for (int tokenIdx = 0; tokenIdx < tokens.size(); tokenIdx++)
				// Check if this token is the include directive, and that it is followed by a quoted string
				if (tokens.get(tokenIdx).getValue().equalsIgnoreCase(Directive.INCLUDE.descriptor)
						&& (tokens.size() > tokenIdx + 1)
						&& tokens.get(tokenIdx + 1).getType() == TokenType.QUOTED_STRING) {

					// It is an include directive - consume it
					tokenizedLines.remove(lineIdx);

					// Extract the filename
					String candidateFN = tokens.get(tokenIdx + 1).getValue();
					candidateFN = candidateFN.substring(1, candidateFN.length() - 1); // get rid of quotes

					// Handle either absolute or relative pathname for .include file
					Path candidate = Paths.get(candidateFN);
					if (!Files.isRegularFile(candidate)) {
						// Maybe we got a relative path, let's try again
						candidate = directory.resolve(candidateFN);
						if (!Files.isRegularFile(candidate))
							throw new RuntimeException("Bad filename: " + candidateFN);
					}

					// Check if file has already been included
					if (inclFiles.contains(candidate))
						throw new RuntimeException("File " + candidateFN + " has already been included");

					// Candidate is good, add it, recurse, and then add the resulting lines in the source-s current position
					inclFiles.add(candidate);
					tokenizedLines.addAll(lineIdx, buildTokenizedProgram(directory, candidate));

					// Readjust index in line looping
					lineIdx--;
					break;
				}
		}
		System.out.println("Source file: " + file);
		lines.forEach(System.out::println);
		return tokenizedLines;
	}

	List<TokenList> applySubstitutions(List<TokenList> program) {


		boolean inMacroDefinition = false;

		// For each line:
		for (int lineNumber = 0; lineNumber < program.size(); lineNumber++) {
			TokenList tokens = program.get(lineNumber);

			// Short circuiting on empty lines
			if (tokens.isEmpty()) continue;

			// NOTE: Applying substitutions in macro definitions too, this saves some future work while enforcing correctness

			// Get position in token list of last non-comment token
			// AP180629: We need this kinda everywhere
			int lastTokenIdx = tokens.size() - ((tokens.get(tokens.size() - 1).getType() == TokenType.COMMENT) ? 2 : 1);

			// TODO question: are labels affected by substitution?

			// -----------------------------------------------------------------
			// Apply equivalences
			/**
			 * AP180627 IMPORTANT: Look for any EQVs to substitute before
			 * checking if this line contains an EQV itself; this enforces the
			 * design constraint of defining macros before use. Otherwise, weird
			 * cases might occur! For example, this program:
			 * -----------------------------------------------------------------
			 *
			 * .eqv A B
			 *
			 * .eqv B nop
			 *
			 * A
			 *
			 * -----------------------------------------------------------------
			 * would successfully assemble into a "nop", despite the B macro
			 * being used before its definition
			 *
			 * AP180710: Actually, it's worse: it will leave an A as an assembly
			 * token. Cases like the following one are even more confusing:
			 * -----------------------------------------------------------------
			 *
			 * .eqv A div
			 *
			 * .eqv div mov
			 *
			 * A
			 *
			 * -----------------------------------------------------------------
			 *
			 * would assemble into a div instruction instead of the expected mov
			 *
			 */
			for (int tokenIdx = 0; tokenIdx < lastTokenIdx; tokenIdx++) {
				Token token = tokens.get(tokenIdx);

				// Look up this token in the equivalence map, if it is of the IDENTIFIER type
				if (token.getType() == TokenType.IDENTIFIER && equivalents.containsKey(token.getValue())) {

					// Found an equiv, put the relative body in its place
					List<Token> eqvBody = equivalents.get(token.getValue());
					tokens.remove(tokenIdx);
					tokens.addAll(tokenIdx, eqvBody);

					// Index correction
					// NOTE: We're scanning through the newly added tokens for nested equivs
					// NOTE2: Actually, by how we scan substitutions (before making bindings), we may assert that body is free form symbols
					tokenIdx += eqvBody.size();
				}
			}

			// -----------------------------------------------------------------
			// Apply eventual macros
			// SPIM-style macro calling:
			// NOTE: Macro invocation forbids labels
			// Templates:
			// IDENTIFIER -[COMMENT]-
			// IDENTIFIER LEFT_PAREN ... RIGHT_PAREN -[COMMENT]-
			if (tokens.get(0).getType() == TokenType.IDENTIFIER
					&& (lastTokenIdx == 0
					|| (tokens.get(1).getType() == TokenType.LEFT_PAREN
					&& tokens.get(lastTokenIdx).getType() == TokenType.RIGHT_PAREN))) {

				// This looks very much like a macro invocation, extract macro arguments
				TokenList macroArgs = (TokenList) tokens.clone();
				macroArgs.remove(tokens.size() - 1);
				macroArgs.remove(1);
				macroArgs.remove(0);


				MacroIndex.Macro match = mi.lookupMacro(tokens.get(0).getValue(), macroArgs.size());

				if (match != null) {
					// The macro exists, apply it

					// Remove invocation
					program.remove(lineNumber);

					// Clone the body and substitute the parameters
					ArrayList<TokenList> matchedBody = (ArrayList<TokenList>) match.body.clone();
					matchedBody.forEach((line) -> {
						for (int p = 0; p < match.parameters.size(); p++)
							Collections.replaceAll(line, match.parameters.get(p), macroArgs.get(p));
					});

					// Add the processed lines
					program.addAll(lineNumber, matchedBody);

					// Read again in place, in order to parse the newly added lines
					lineNumber--;
					continue;
				}

			}

			// There should not be a label but if there is, the directive is in token position 2 (ident, colon, directive).
			// Substitutions are made, exclude eventual label for next computations
			int firstTokenIdx = (tokens.size() > 2 && tokens.get(0).getType() == TokenType.IDENTIFIER && tokens.get(1).getType() == TokenType.COLON) ? 2 : 0;

			if (inMacroDefinition) {
				// Ignore EQV definitions, we'll process them after macro substitution, making them available only after first invocation
				// Prevent macro definitions, nesting is not allowed (for now)
				if (tokens.stream().anyMatch((t) ->
						t.getType() == TokenType.DIRECTIVE && Directive.matchDirective(t.getValue()) == Directive.MACRO
				))
					throw new RuntimeException("Nested macro definitions are not allowed");


				// check for end macro token
				if (firstTokenIdx == lastTokenIdx
						&& tokens.get(firstTokenIdx).getType() == TokenType.DIRECTIVE
						&& Directive.matchDirective(tokens.get(firstTokenIdx).getValue()) == Directive.END_MACRO) {

					// Macro definition complete: commit, burn token and continue to next line
					mi.commmit();
					tokens.remove(firstTokenIdx);
					continue;

				}

				// We're still recording
				mi.record(tokens);

				// correct index increment
				program.remove(lineNumber);
				lineNumber--;

			}



			// -----------------------------------------------------------------
			// See if it is .eqv or .macro directive.  If so, record it...
			// Have to assure it is a well-formed statement right now (can't wait for assembler).
			if (tokens.get(firstTokenIdx).getType() == TokenType.DIRECTIVE)
				if (Directive.matchDirective(tokens.get(firstTokenIdx).getValue()) == Directive.EQV) {

					// There have to be at least two non-comment tokens beyond the directive
					if (lastTokenIdx < firstTokenIdx + 2)
						throw new RuntimeException("Too few operands for " + Directive.EQV.descriptor + " directive");

					// Token following the directive has to be an IDENTIFIER
					if (tokens.get(firstTokenIdx + 1).getType() != TokenType.IDENTIFIER)
						throw new RuntimeException(tokens.get(firstTokenIdx + 1).getType() == TokenType.OPERATOR
								? "Can't use an instruction operator as equivalence identifier: " + tokens.get(firstTokenIdx + 1).getValue()
								: "Malformed " + Directive.EQV.descriptor + " directive: " + tokens);

					String symbol = tokens.get(firstTokenIdx + 1).getValue();

					// Make sure the symbol is not contained in the expression.  Not likely to occur but if left
					// undetected it will result in infinite recursion.  e.g.  .eqv ONE, (ONE)
					for (int s = firstTokenIdx + 2; s <= lastTokenIdx; s++)
						if (tokens.get(s).getValue().equals(symbol))
							throw new RuntimeException("Cannot substitute " + symbol + " for itself in " + Directive.EQV.descriptor + " directive");

					// Expected syntax is symbol, expression.  I'm allowing the expression to comprise
					// multiple tokens, so I want to get everything from the IDENTIFIER to either the
					// COMMENT or to the end.

					// Consume the equiv, leave the eventual comment alone (?)
					List<Token> eqvBody = new ArrayList<>();
					// Burn the directive and the identifier tokens
					tokens.remove(firstTokenIdx);
					tokens.remove(firstTokenIdx);
					lastTokenIdx -= 2;
					// Move the body into the mapping
					while (lastTokenIdx >= firstTokenIdx) {
						eqvBody.add(tokens.remove(firstTokenIdx));
						lastTokenIdx--;
					}

					// Symbol cannot be redefined - the only reason for this is to act like the Gnu .eqv
					// AP180629: This enables EQVs inside macro definitions to be defined and used safely
					if (mi.isNameTaken(symbol))
						throw new RuntimeException("\"" + symbol + "\" is already used as a macro name");

					// Symbol cannot be redefined - the only reason for this is to act like the Gnu .eqv
					// AP180629: This enables EQVs inside macro definitions to be defined and used safely
					if (equivalents.containsKey(symbol) && !equivalents.get(symbol).equals(eqvBody))
						throw new RuntimeException("Redefinition of \"" + symbol + "\"");

					// Save the mapping
					equivalents.put(symbol, eqvBody);
				}
				else if (Directive.matchDirective(tokens.get(firstTokenIdx).getValue()) == Directive.MACRO) {

					// We got a macro def here, check if we're nesting first
					if (inMacroDefinition)
						throw new RuntimeException("Nested macros are not allowed");



					// Now check for signature syntax
					// There has to be at least one non-comment token beyond the directive, which is the macro symbol
					if (lastTokenIdx < firstTokenIdx + 1)
						throw new RuntimeException("Missing name for " + Directive.MACRO.descriptor + " directive");
					if (tokens.get(firstTokenIdx + 1).getType() != TokenType.IDENTIFIER)
						throw new RuntimeException("Illegal macro name: \"" + tokens.get(1).getValue() + "\"");

					// Now check signature syntax
					if (lastTokenIdx != firstTokenIdx + 1
							&& (lastTokenIdx == firstTokenIdx + 2
							|| tokens.get(firstTokenIdx + 2).getType() != TokenType.LEFT_PAREN
							|| tokens.get(lastTokenIdx).getType() != TokenType.RIGHT_PAREN
							|| !tokens.subList(firstTokenIdx + 3, lastTokenIdx).stream()
									.allMatch((token) -> token.getType() == TokenType.MACRO_PARAMETER)))
						throw new RuntimeException("Macro definition has malformed parameter signature");

					// See if the symbol is already defined
					if (equivalents.containsKey(tokens.get(1).getValue()) || mi.lookupMacro(tokens.get(1).getValue(), lastTokenIdx - (firstTokenIdx + 3)) != null)
						throw new RuntimeException("Macro name already used: \"" + tokens.get(1).getValue() + "\"");

					// Macro declaration line is ok, start recording
					mi.startRecording(tokens.get(firstTokenIdx + 1).getValue(), new ArrayList<>(tokens.subList(firstTokenIdx + 3, lastTokenIdx)));
					inMacroDefinition = true;

					// Consume the definition tokens
					// Burn the directive and the identifier tokens
					tokens.remove(firstTokenIdx);
					tokens.remove(firstTokenIdx);
					lastTokenIdx -= 2;
					// Burn the signature, we just recorded that
					while (lastTokenIdx >= firstTokenIdx) {
						tokens.remove(firstTokenIdx);
						lastTokenIdx--;
					}

				}
		}

		return program;

	}


	// Includes are permitted inside macros, with all the consequences
	// Macro definitions cannot nest
	// Equivs can appear anywhere, macros must be alone

	public static void main(String[] args) {
		Main.initialize();

		NewMIPSTokenizer t = new NewMIPSTokenizer();

		// Tokenize the program, this will process inclusion directives
		Path file = Paths.get("C:\\Users\\Project2100\\Desktop\\test");
		Path name = Paths.get("main.asm");
		List<TokenList> program = t.buildTokenizedProgram(file, name);

		System.out.println("\n\nINCLUDED PROGRAM:");
		program.forEach(System.out::println);

		t.applySubstitutions(program);

		System.out.println("\n\nFINAL PROGRAM:");
		program.forEach(System.out::println);
	}

	void f(Path f) {

//	Algo BUILD:
//

		// Tokenize the whole file
		List<TokenList> programTokens = tokenizeSingleFile(f);

		boolean inMacroDefinition = false;
		// For each line
		for (int lineIdx = 0; lineIdx < programTokens.size(); lineIdx++) {
			TokenList tokenLine = programTokens.get(lineIdx);

			// Skip if empty
			if (tokenLine.isEmpty()) continue;


			// Exclude label, if any
			int firstTokenIdx = 0;
			if (tokenLine.size() > 1 && tokenLine.get(0).getType() == TokenType.IDENTIFIER && tokenLine.get(1).getType() == TokenType.COLON) {
				boolean add = labels.add(tokenLine.get(0).getValue());
				if (!add) {
					throw new RuntimeException("Label redefinition");
				}
				// There is effectively a label, correct first index
				firstTokenIdx = 2;
			}

			// Exclude comment
			int lastTokenIdx = tokenLine.size() - 1;
			if (tokenLine.get(tokenLine.size() - 1).getType() == TokenType.COMMENT)
				lastTokenIdx--;

			// line is effectively empty ( XXX apply eqvs to label defs?)
			if (firstTokenIdx == lastTokenIdx) continue;

			// Directive tokens appear only at first place
			// XXX NOTE: Do not allow memory directive chaining, for now
			for (int tokenIdx = firstTokenIdx + 1; tokenIdx < tokenLine.size(); tokenIdx++) {
				if (tokenLine.get(tokenIdx).getType() == TokenType.DIRECTIVE)
					throw new RuntimeException("Unexpected directive token: " + tokenLine.get(tokenIdx).getValue());
			}

			// If this is a source directive line
			if (tokenLine.get(firstTokenIdx).getType() == TokenType.DIRECTIVE) {


				if (tokenLine.get(firstTokenIdx).getValue().equals(Directive.INCLUDE.descriptor)) {
					//TODO
				}
				else if (tokenLine.get(firstTokenIdx).getValue().equals(Directive.EQV.descriptor)) {
					if (inMacroDefinition) {
						// Ignore, we will memorize it once the macro is applied
					}
					else {

						//<editor-fold defaultstate="collapsed" desc="DEFINE EQV">
						// There have to be at least two non-comment tokens beyond the directive
						// AP180712: Permit empty EQVs
						if (lastTokenIdx < firstTokenIdx + 1)
							throw new RuntimeException("Too few operands for " + Directive.EQV.descriptor + " directive");

						// Token following the directive has to be an IDENTIFIER
						if (tokenLine.get(firstTokenIdx + 1).getType() != TokenType.IDENTIFIER)
							throw new RuntimeException(tokenLine.get(firstTokenIdx + 1).getType() == TokenType.OPERATOR
									? "Can't use an instruction operator as equivalence identifier: " + tokenLine.get(firstTokenIdx + 1).getValue()
									: "Malformed " + Directive.EQV.descriptor + " directive: " + tokenLine);

						// Get the symbol we are defining
						String symbol = tokenLine.get(firstTokenIdx + 1).getValue();

						// Look if it is already defined as another symbol type
						if (mi.isNameTaken(symbol))
							throw new RuntimeException("\"" + symbol + "\" is already used as a macro name");
						if (labels.contains(symbol)) {
							throw new RuntimeException("Symbol " + symbol + " is already defined as a label");
						}

						// Make sure the symbol itself is not contained in the expression.
						// If left undetected it will result in infinite recursion.  e.g.  .eqv ONE, (ONE)
						for (int s = firstTokenIdx + 2; s <= lastTokenIdx; s++)
							if (tokenLine.get(s).getValue().equals(symbol))
								throw new RuntimeException("Cannot substitute " + symbol + " for itself in " + Directive.EQV.descriptor + " directive");

						// Expected syntax is symbol, expression.  I'm allowing the expression to comprise
						// multiple tokens, so I want to get everything from the IDENTIFIER to either the
						// COMMENT or to the end.

						// Consume the equiv, leave the eventual comment alone (?)
						List<Token> eqvBody = new ArrayList<>();
						// Burn the directive and the identifier tokens
						tokenLine.remove(firstTokenIdx);
						tokenLine.remove(firstTokenIdx);
						lastTokenIdx -= 2;
						// Move the body into the mapping
						while (lastTokenIdx >= firstTokenIdx) {
							eqvBody.add(tokenLine.remove(firstTokenIdx));
							lastTokenIdx--;
						}


						// Symbol cannot be redefined - the only reason for this is to act like the Gnu .eqv
						// AP180629: This enables EQVs to be defined and used safely inside macro definitions
						if (equivalents.containsKey(symbol) && !equivalents.get(symbol).equals(eqvBody))
							throw new RuntimeException("Invalid redefinition of \"" + symbol + "\"");

						// Save the mapping
						equivalents.put(symbol, eqvBody);
						//</editor-fold>

					}
				}
				else if (tokenLine.get(firstTokenIdx).getValue().equals(Directive.MACRO.descriptor)) {
					if (inMacroDefinition) {
						// Nesed macros are not permitted
						throw new RuntimeException("Nested macro definitions are not allowed");
					}
					else {
						//TODO
					}
				}
				else if (tokenLine.get(firstTokenIdx).getValue().equals(Directive.END_MACRO.descriptor)) {


					if (inMacroDefinition) {
						//TODO
					}
					else {
						// We're not insise a macrodef
						throw new RuntimeException("No macro to end here");
					}
				}

			}

			else if (false) {
				// XXX IT CAN BE A MACRO INVOCATION
				//TODO
			}

			// Normal line
			else {
				//TODO
				if (inMacroDefinition) {
					applyEQVs(tokenLine, equivalents);
				}
				else {
					applyEQVs(tokenLine, equivalents);
				}
			}



		}

	}

	static class MacroIndex {

		class Macro {

			String name;
			List<Token> parameters;
			ArrayList<TokenList> body;

			public Macro(String name, List<Token> parameters, ArrayList<TokenList> body) {
				this.name = name;
				this.parameters = parameters;
				this.body = body;
			}

		}

		Set<Macro> macros;

		Macro current;

		public MacroIndex() {
			macros = new HashSet<>();
			current = null;
		}


		Macro lookupMacro(String name, int arity) {
			return macros.stream()
					.filter((macro) -> macro.name.equals(name) && macro.parameters.size() == arity)
					.findFirst()
					.orElse(null);
		}

		void startRecording(String name, List<Token> parameters) {
			if (current != null)
				throw new IllegalStateException("Already recording a macro");

			current = new Macro(name, parameters, new ArrayList<>());
		}

		void record(TokenList line) {
			if (current == null)
				throw new IllegalStateException("Not recording a macro");
			current.body.add(line);
		}

		void commmit() {
			if (current == null)
				throw new IllegalStateException("Not recording a macro");
			macros.add(current);
			current = null;
		}

		boolean isNameTaken(String name) {
			return macros.stream().anyMatch((macro) -> macro.name.equals(name));
		}
	}



	static List<TokenList> tokenizeSingleFile(Path f) {

		// Get the lines
		List<String> lines;
		try {
			lines = Files.readAllLines(f);
		}
		catch (IOException ex) {
			throw new RuntimeException("Unable to read file " + f, ex);
		}

		// Tokenize them into their corresponding token lists
		Tokenizer t = new Tokenizer();
		List<TokenList> tokenizedLines = lines.stream()
				.map((line) -> t.tokenizeLine(null, 0, line, false))
				.collect(Collectors.toList());

		return tokenizedLines;
	}


	static void applyEQVs(TokenList tokens, Map<String, List<Token>> equivalents) {
		for (int tokenIdx = 0; tokenIdx < tokens.size(); tokenIdx++) {
			Token token = tokens.get(tokenIdx);

			// Look up this token in the equivalence map, if it is of the IDENTIFIER type
			if (token.getType() == TokenType.IDENTIFIER && equivalents.containsKey(token.getValue())) {

				// Found an equiv, put the relative body in its place
				List<Token> eqvBody = equivalents.get(token.getValue());
				tokens.remove(tokenIdx);
				tokens.addAll(tokenIdx, eqvBody);

				// Index correction
				// NOTE: We're scanning through the newly added tokens for nested equivs
				// NOTE2: Actually, by how we scan substitutions (before making bindings), we may assert that body is free form symbols
				tokenIdx += eqvBody.size();
			}
		}
	}



}
