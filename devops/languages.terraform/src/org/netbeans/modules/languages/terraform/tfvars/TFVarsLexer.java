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

import org.netbeans.modules.languages.terraform.AbstractHCLLexer;
import org.netbeans.modules.languages.terraform.HCLTokenId;
import org.netbeans.modules.languages.terraform.grammar.HCLCommonLexerRules;
import org.netbeans.spi.lexer.LexerRestartInfo;

import static org.netbeans.modules.languages.terraform.HCLTokenId.*;

/**
 *
 * @author lkishalmi
 */
public final class TFVarsLexer extends AbstractHCLLexer {

    public TFVarsLexer(LexerRestartInfo<HCLTokenId> info) {
        super(info, (input) -> new HCLCommonLexerRules(input));
    }
    
    @Override
    protected HCLTokenId mapTokenType(int type) {
        switch (type) {
            case HCLCommonLexerRules.LINE_COMMENT:
            case HCLCommonLexerRules.BLOCK_COMMENT:
                return COMMENT;

            case HCLCommonLexerRules.A_BOOL:
                return BOOLEAN;

            case HCLCommonLexerRules.A_NUMBER:
                return NUMBER;

            case HCLCommonLexerRules.IDENTIFIER:
                return VARIABLE;

            case HCLCommonLexerRules.LBRACE:
            case HCLCommonLexerRules.RBRACE:
            case HCLCommonLexerRules.LBRACK:
            case HCLCommonLexerRules.RBRACK:
            case HCLCommonLexerRules.COMMA:
            case HCLCommonLexerRules.DOT:
            case HCLCommonLexerRules.EQUAL:
                return SEPARATOR;

            case HCLCommonLexerRules.QUOTE:
            case HCLCommonLexerRules.HEREDOC:
                return STRING;

            case HCLCommonLexerRules.WS:
            case HCLCommonLexerRules.NL:
                return WHITESPACE;

            default:
                return ERROR;
        }
    }
    @Override
    protected HCLTokenId collateTokenType(int type) {
        switch (type) {
            case HCLCommonLexerRules.STRING_CONTENT:
                return STRING;
            default:
                return null;
        }
    }

}