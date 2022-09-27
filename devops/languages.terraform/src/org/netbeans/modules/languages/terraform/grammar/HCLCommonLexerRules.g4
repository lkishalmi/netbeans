lexer grammar HCLCommonLexerRules;
import HCLLexerBasics;

@lexer::members {
  private boolean heredocEndAhead(String partialHeredoc) {
    if (this.getCharPositionInLine() != 0) {
      // If the lexer is not at the start of a line, no end-delimiter can be possible
      return false;
    }

    // Get the delimiter
    String firstLine = partialHeredoc.split("\r?\n|\r")[0];
    String delimiter = firstLine.replaceAll("^<<-?", "");

    for (int n = 1; n < delimiter.length(); n++) {
      if (this._input.LA(n) != delimiter.charAt(n - 1)) {
        return false;
      }
    }

    int charAfterDelimiter = this._input.LA(delimiter.length() + 1);

    return charAfterDelimiter == EOF ||  Character.isWhitespace(charAfterDelimiter);
  }
}

HEREDOC
 : '<<' '-'? Letter LetterDigit* NL ( {!heredocEndAhead(getText())}? . )* Letter LetterDigit*
 ;

EQUAL
   : Equal
   ;

LBRACE
   : LBrace
   ;

RBRACE
   : RBrace
   ;

LBRACK
   : LBrack
   ;

RBRACK
   : RBrack
   ;

COMMA
   : Comma
   ;

DOT
   : Dot
   ;
 
PLUS
   : Plus
   ;


MINUS
   : Minus
   ;

BOOL
    : BoolLiteral
    ;

NULL
    : Null
    ;

IDENTIFIER
    : Letter LetterDigit*
    ;


QUOTE
    : DQuote  -> pushMode(String)
    ;

NUMBER
   : Minus? DecimalNumeral (Dot DecDigit +)?
   ;


WS
    : Hws +
    ;

NL
    : Vws +
    ;

COMMENT
    : BlockComment -> skip
    ;

LINE_COMMENT
    : LineComment -> skip
    ;


mode String;

STRING_ESCAPE
   : EscAny -> type (STRING_CONTENT)
   ;

INTERPOLATION_ESCAPE
   : EscInterpolation -> type(STRING_CONTENT)
   ;

INTERPOLATION_START
    : '${'      -> pushMode(Interpolation)
    ;

STRING_END
    : '"'       -> popMode
    ;

STRING_CONTENT
   : .
   ;


mode Interpolation;

INTERPOLATION_END
    : '}'       -> popMode
    ;

INTERPOLATION_QUOTE
    : '"'       -> pushMode(String)
    ;

INTERPOLATION_CODE
    : .
    ;
