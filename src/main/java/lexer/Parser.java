package lexer;

import java.io.StringReader;

import java_cup.runtime.ComplexSymbolFactory;

import common.astnodes.Program;

/** Interface between driver and parser. */
public class Parser {

    /** Return the Program AST resulting from parsing INPUT.  Turn on
     *  parser debugging iff DEBUG. */
    public static Program process(String input, boolean debug) {
        PyLangLexer lexer = new PyLangLexer(new StringReader(input));
        PyLangParser parser =
            new PyLangParser(lexer, new ComplexSymbolFactory());
        return parser.parseProgram(debug);
    }
}


