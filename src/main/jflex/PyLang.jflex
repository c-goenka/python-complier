package lexer;
import java_cup.runtime.*;
import java.util.*;

%%

/*** Do not change the flags below unless you know what you are doing. ***/

%unicode
%line
%column

%class PyLangLexer
%public

%cupsym PyLangTokens
%cup
%cupdebug

%eofclose false

/*** Do not change the flags above unless you know what you are doing. ***/

/* The following code section is copied verbatim to the
 * generated lexer class. */
%{
    /** Producer of token-related values for the parser. */
    final ComplexSymbolFactory symbolFactory = new ComplexSymbolFactory();

    /** Return a terminal symbol of syntactic category TYPE and no
     *  semantic value at the current source location. */
    private Symbol symbol(int type) {
        return symbol(type, yytext());
    }

    /** Return a terminal symbol of syntactic category TYPE and semantic
     *  value VALUE at the current source location. */
    private Symbol symbol(int type, Object value) {
        return symbolFactory.newSymbol(PyLangTokens.terminalNames[type], type,
            new ComplexSymbolFactory.Location(yyline + 1, yycolumn + 1),
            new ComplexSymbolFactory.Location(yyline + 1,yycolumn + yylength()),
            value);
    }

    /* Removes all backslashes and start/end quotes */
    private String clean_string(String match) {
        match = match.replace("\\", "");
        return (match.substring(1, match.length() - 1));
    }

    /* Indent/Dedent Stack */
    Stack<Integer> indent_lvl = new Stack<Integer>();
    int curr_indent;

%}


/* get the starting indent level */
%init{

    indent_lvl.push(0);
    curr_indent = 0;
    
%init}

/* for dedent and end of file */
%eofval{

    if (indent_lvl.peek() > 0) {
        indent_lvl.pop();
        zzAtEOF = false;
        return symbol(PyLangTokens.DEDENT);
    }
    return symbol(PyLangTokens.EOF);

%eofval}


/* macros */
WhiteSpace = [ \t]
Indent = [ \t]*
LineBreak  = \r|\n|\r\n
Comment = # [^\r\n]*
IntegerLiteral = 0 | [1-9][0-9]*
Identifier = [a-zA-Z_][a-zA-Z0-9_]*
IdString = \"[!-~]*\"
String = \"[ -~]*\"
InputCharacter = [^\r\n]


%state INPUT_CHARACTER
%%

<YYINITIAL> {

/* Identation */

    {LineBreak}                 { curr_indent = 0; }
    {Comment}                   { /* ignore */ }
    {Indent}                    { curr_indent = yylength();
                                    if (curr_indent > indent_lvl.peek()) {
                                        indent_lvl.push(curr_indent);
                                        return symbol(PyLangTokens.INDENT, yytext());
                                    } else if (curr_indent < indent_lvl.peek()) {
                                        yypushback(yylength());
                                        indent_lvl.pop();
                                        return symbol(PyLangTokens.DEDENT);
                                    }
                                }
    {InputCharacter}            { if (curr_indent == 0) {
                                    if (curr_indent < indent_lvl.peek()) {
                                        yypushback(1);
                                        indent_lvl.pop();
                                        return symbol(PyLangTokens.DEDENT);
                                    }
                                  }
                                    yypushback(1);
                                    yybegin(INPUT_CHARACTER);
                                }

}

<INPUT_CHARACTER> {
  
/* Keywords */
    "False"                     { return symbol(PyLangTokens.FALSE); }
    "None"                      { return symbol(PyLangTokens.NONE); }
    "True"                      { return symbol(PyLangTokens.TRUE); }
    "and"                       { return symbol(PyLangTokens.AND); }
    "as"                        { return symbol(PyLangTokens.AS); }
    "assert"                    { return symbol(PyLangTokens.ASSERT); }
    "async"                     { return symbol(PyLangTokens.ASYNC); }
    "await"                     { return symbol(PyLangTokens.AWAIT); }
    "break"                     { return symbol(PyLangTokens.BREAK); }
    "class"                     { return symbol(PyLangTokens.CLASS); }
    "continue"                  { return symbol(PyLangTokens.CONTINUE); }
    "def"                       { return symbol(PyLangTokens.DEF); }
    "del"                       { return symbol(PyLangTokens.DEL); }
    "elif"                      { return symbol(PyLangTokens.ELIF); }
    "else"                      { return symbol(PyLangTokens.ELSE); }
    "except"                    { return symbol(PyLangTokens.EXCEPT); }
    "finally"                   { return symbol(PyLangTokens.FINALLY); }
    "for"                       { return symbol(PyLangTokens.FOR); }
    "from"                      { return symbol(PyLangTokens.FROM); }
    "global"                    { return symbol(PyLangTokens.GLOBAL); }
    "if"                        { return symbol(PyLangTokens.IF); }
    "import"                    { return symbol(PyLangTokens.IMPORT); }
    "in"                        { return symbol(PyLangTokens.IN); }
    "is"                        { return symbol(PyLangTokens.IS); }
    "lambda"                    { return symbol(PyLangTokens.LAMBDA); }
    "nonlocal"                  { return symbol(PyLangTokens.NONLOCAL); }
    "not"                       { return symbol(PyLangTokens.NOT); }
    "or"                        { return symbol(PyLangTokens.OR); }
    "pass"                      { return symbol(PyLangTokens.PASS); }
    "raise"                     { return symbol(PyLangTokens.RAISE); }
    "return"                    { return symbol(PyLangTokens.RETURN); }
    "try"                       { return symbol(PyLangTokens.TRY); }
    "while"                     { return symbol(PyLangTokens.WHILE); }
    "with"                      { return symbol(PyLangTokens.WITH); }
    "yield"                     { return symbol(PyLangTokens.YIELD); }

/* Identifiers. */
    {Identifier}                { return symbol(PyLangTokens.IDENTIFIER, yytext()); }

/* Delimiters. */
    {LineBreak}                 {  curr_indent = 0;
                                    yybegin(YYINITIAL); 
                                    return symbol(PyLangTokens.NEWLINE); }

/* Literals. */
    {IntegerLiteral}            { return symbol(PyLangTokens.INTEGER, Integer.parseInt(yytext())); }

    {IdString}                  { return symbol(PyLangTokens.IDSTRING, clean_string(yytext())); }

    {String}                    { return symbol(PyLangTokens.STRING, clean_string(yytext())); }

/* Operators. */
    "+"                         { return symbol(PyLangTokens.PLUS, yytext()); }
    "-"                         { return symbol(PyLangTokens.MINUS, yytext()); }
    "*"                         { return symbol(PyLangTokens.MULT, yytext()); }
    "//"                        { return symbol(PyLangTokens.DIV, yytext()); }
    "%"                         { return symbol(PyLangTokens.MOD, yytext()); }
    "<"                         { return symbol(PyLangTokens.LT, yytext()); }
    ">"                         { return symbol(PyLangTokens.GT, yytext()); }
    "<="                        { return symbol(PyLangTokens.LEQ, yytext()); }
    ">="                        { return symbol(PyLangTokens.GEQ, yytext()); }
    "=="                        { return symbol(PyLangTokens.EQEQ, yytext()); }
    "!="                        { return symbol(PyLangTokens.NOTEQ, yytext()); }
    "="                         { return symbol(PyLangTokens.EQ, yytext()); }
    "("                         { return symbol(PyLangTokens.LPAREN, yytext()); }
    ")"                         { return symbol(PyLangTokens.RPAREN, yytext()); }
    "["                         { return symbol(PyLangTokens.LINDEX, yytext()); }
    "]"                         { return symbol(PyLangTokens.RINDEX, yytext()); }
    ","                         { return symbol(PyLangTokens.COMMA, yytext()); }
    ":"                         { return symbol(PyLangTokens.COLON, yytext()); }
    "."                         { return symbol(PyLangTokens.DOT, yytext()); }
    "->"                        { return symbol(PyLangTokens.ARROW, yytext()); }

/* Comments. */
  {Comment}                   { /* ignore */ }

/* Whitespace. */
  {WhiteSpace}                { /* ignore */ }

}

/* Error fallback. */
[^]                           { return symbol(PyLangTokens.UNRECOGNIZED); }