/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

channels { OFF_CHANNEL , COMMENT }

BLOCK_COMMENT
    : BlockComment -> channel(COMMENT)
    ;

LINE_COMMENT
    : LineComment -> channel(COMMENT)
    ;

HEREDOC
 : HereDocIntro Letter LetterDigit* NL ( {!heredocEndAhead(getText())}? . )* Letter LetterDigit*
 ;

EQUALS
   : Equal Equal
   ;

RARROW
   : RArrow
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

LPAREN
   : LParen
   ;

RPAREN
   : RParen
   ;

QUESTION
   : Question
   ;

COLON
   : Colon
   ;

GTE
   : Gt Equal
   ;

GT
   : Gt
   ;

LTE
   : Lt Equal
   ;

LT
   : Lt
   ;

NOT_EQUALS
   : Bang Equal
   ;

NOT
   : Bang
   ;

COMMA
   : Comma
   ;

ELLIPSIS
   : Ellipsis
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

STAR
   : Star
    ;

SLASH
   : Slash
   ;

AND
    : And
    ;

OR
    : Or
    ;

A_BOOL
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

A_NUMBER
   : Minus? DecimalNumeral (Dot DecDigit+)?
   ;


WS
    : Hws + -> channel(OFF_CHANNEL)
    ;

NL
    : Vws + -> channel(OFF_CHANNEL)
    ;

ERRCHAR
   : . -> channel (HIDDEN)
   ;


mode String;

STRING_ESCAPE
   : EscAny -> type (STRING_CONTENT)
   ;

INTERPOLATION_ESCAPE
   : EscInterpolation -> type(STRING_CONTENT)
   ;

INTERPOLATION_START
    : InterpolationStart -> type(INTERPOLATION), pushMode(Interpolation)
    ;

STRING_END
    : DQuote       -> type(QUOTE), popMode
    ;

STRING_CONTENT
    : NonVws
    ;

STRING_ERR
   : .
   ;


mode Interpolation;

INTERPOLATION_END
    : RBrace       -> type(INTERPOLATION), popMode
    ;

INTERPOLATION_QUOTE
    : DQuote       -> type(INTERPOLATION), pushMode(InterpolationString)
    ;

INTERPOLATION
    : .
    ;

mode InterpolationString;

ISTRING_ESCAPE
   : EscAny -> type (INTERPOLATION)
   ;

IINTERPOLATION_ESCAPE
   : EscInterpolation -> type(INTERPOLATION)
   ;

IINTERPOLATION_START
    : InterpolationStart -> type(INTERPOLATION), pushMode(Interpolation)
    ;

ISTRING_END
    : DQuote       -> type(INTERPOLATION), popMode
    ;

ISTRING_CONTENT
    : NonVws       -> type(INTERPOLATION)
    ;

ISTRING_ERR
   : .             -> type(ERRCHAR)
   ;

