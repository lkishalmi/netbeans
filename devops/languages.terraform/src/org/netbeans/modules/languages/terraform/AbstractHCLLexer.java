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
package org.netbeans.modules.languages.terraform;

import java.util.function.Function;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.misc.IntegerList;
import static org.antlr.v4.runtime.Recognizer.EOF;
import org.antlr.v4.runtime.Token;
import org.netbeans.modules.languages.terraform.grammar.HCLCommonLexerRules;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;

import static org.netbeans.modules.languages.terraform.HCLTokenId.*;

/**
 *
 * @author lkishalmi
 */
public abstract class AbstractHCLLexer implements org.netbeans.spi.lexer.Lexer<HCLTokenId> {
    
    private final TokenFactory<HCLTokenId> tokenFactory;
    protected final org.antlr.v4.runtime.Lexer lexer;
    private final LexerInputCharStream input;

    
    public AbstractHCLLexer(LexerRestartInfo<HCLTokenId> info, Function<CharStream, Lexer> lexerProvider) {
        this.tokenFactory = info.tokenFactory();
        this.input = new LexerInputCharStream(info.input());
        this.lexer = lexerProvider.apply(input);
        if (info.state() != null) {
            ((AbstractHCLLexer.LexerState) info.state()).restore(lexer);
        }
        input.markToken();
    }
    
    protected abstract HCLTokenId collateTokenType(int type);
    protected abstract HCLTokenId mapTokenType(int type);
        
    private Token preFetchedToken = null;
    
    @Override
    public final org.netbeans.api.lexer.Token<HCLTokenId> nextToken() {
        Token nextToken;
        if (preFetchedToken != null) {
            nextToken = preFetchedToken;
            lexer.getInputStream().seek(preFetchedToken.getStopIndex() + 1);
            preFetchedToken = null;
        } else {
            nextToken = lexer.nextToken();
        }
        int tokenType = nextToken.getType();
        if (tokenType == EOF) {
            return null;
        }
        HCLTokenId collate = collateTokenType(tokenType);
        return collate != null ? collate(tokenType, collate): token(mapTokenType(nextToken.getType()));
    }

    protected org.netbeans.api.lexer.Token<HCLTokenId> collate(int tokenType, HCLTokenId tokenId) {
        preFetchedToken = lexer.nextToken();
        while (preFetchedToken.getType() == tokenType) {
            preFetchedToken = lexer.nextToken();
        }
        lexer.getInputStream().seek(preFetchedToken.getStartIndex());
        return token(tokenId);
    }
    
    @Override
    public final Object state() {
        return new LexerState(lexer);
    }

    @Override
    public final void release() {
    }

    protected final org.netbeans.api.lexer.Token<HCLTokenId> token(HCLTokenId id) {
        input.markToken();
        return tokenFactory.createToken(id);
    }

    private static class LexerState {
        final int state;
        final int mode;
        final IntegerList modes;

        LexerState(Lexer lexer) {
            this.state= lexer.getState();

            this.mode = lexer._mode;
            this.modes = new IntegerList(lexer._modeStack);
        }

        public void restore(Lexer lexer) {
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
