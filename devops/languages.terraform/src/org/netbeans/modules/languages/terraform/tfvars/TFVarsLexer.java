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
package org.netbeans.modules.languages.terraform.tfvars;

import static org.antlr.v4.runtime.Recognizer.EOF;
import org.antlr.v4.runtime.misc.IntegerList;
import org.netbeans.api.lexer.Token;
import org.netbeans.modules.languages.terraform.LexerInputCharStream;
import org.netbeans.modules.languages.terraform.grammar.HCLCommonLexerRules;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;

import static org.netbeans.modules.languages.terraform.tfvars.TFVarsTokenId.*;

/**
 *
 * @author lkishalmi
 */
public final class TFVarsLexer implements Lexer<TFVarsTokenId> {

    private final TokenFactory<TFVarsTokenId> tokenFactory;
    protected final org.antlr.v4.runtime.Lexer lexer;
    private final LexerInputCharStream input;

    public TFVarsLexer(LexerRestartInfo<TFVarsTokenId> info) {
        this.tokenFactory = info.tokenFactory();
        this.input = new LexerInputCharStream(info.input());
        this.lexer = new HCLCommonLexerRules(input);
        if (info.state() != null) {
            ((LexerState) info.state()).restore(lexer);
        }
        input.markToken();
    }

    private org.antlr.v4.runtime.Token preFetchedToken = null;

    @Override
    public Token<TFVarsTokenId> nextToken() {
        org.antlr.v4.runtime.Token nextToken;
        if (preFetchedToken != null) {
            nextToken = preFetchedToken;
            lexer.getInputStream().seek(preFetchedToken.getStopIndex() + 1);
            preFetchedToken = null;
        } else {
            nextToken = lexer.nextToken();
        }
        if (nextToken.getType() == EOF) {
            return null;
        }
        switch (nextToken.getType()) {
            case HCLCommonLexerRules.LINE_COMMENT:
            case HCLCommonLexerRules.COMMENT:
                return token(COMMENT);
                
            case HCLCommonLexerRules.IDENTIFIER:
                return token(VARIABLE);
                
            case HCLCommonLexerRules.LBRACE:
            case HCLCommonLexerRules.RBRACE:
            case HCLCommonLexerRules.LBRACK:
            case HCLCommonLexerRules.RBRACK:
            case HCLCommonLexerRules.COMMA:
            case HCLCommonLexerRules.DOT:
            case HCLCommonLexerRules.EQUAL:
                return token(SEPARATOR);
                
            case HCLCommonLexerRules.STRING_CONTENT:
                preFetchedToken = lexer.nextToken();
                while (preFetchedToken.getType() == HCLCommonLexerRules.STRING_CONTENT) {
                    preFetchedToken = lexer.nextToken();
                }
                lexer.getInputStream().seek(preFetchedToken.getStartIndex());
                return token(STRING);
                
            case HCLCommonLexerRules.QUOTE:
            case HCLCommonLexerRules.STRING_END:
            case HCLCommonLexerRules.HEREDOC:
                return token(STRING);
                
            case HCLCommonLexerRules.WS:
            case HCLCommonLexerRules.NL:
                return token(WHITESPACE);
                
            default:
                return token(ERROR);
        }
    }

    @Override
    public Object state() {
        return new LexerState(lexer);
    }

    @Override
    public void release() {
    }

    protected final Token<TFVarsTokenId> token(TFVarsTokenId id) {
        input.markToken();
        return tokenFactory.createToken(id);
    }

    private static class LexerState {
        final int state;
        final int mode;
        final IntegerList modes;

        LexerState(org.antlr.v4.runtime.Lexer lexer) {
            this.state= lexer.getState();

            this.mode = lexer._mode;
            this.modes = new IntegerList(lexer._modeStack);
        }

        public void restore(org.antlr.v4.runtime.Lexer lexer) {
            lexer.setState(state);
            lexer._modeStack.addAll(modes);
            lexer._mode = mode;
        }

        @Override
        public String toString() {
            return String.valueOf(state);
        }

    }
    
}
