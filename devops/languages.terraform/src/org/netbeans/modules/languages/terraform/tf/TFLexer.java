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
package org.netbeans.modules.languages.terraform.tf;

import org.netbeans.modules.languages.terraform.AbstractHCLLexer;
import org.netbeans.modules.languages.terraform.HCLTokenId;
import org.netbeans.modules.languages.terraform.grammar.TerraformLexer;
import org.netbeans.spi.lexer.LexerRestartInfo;

import static org.netbeans.modules.languages.terraform.HCLTokenId.*;
/**
 *
 * @author lkishalmi
 */
public class TFLexer extends AbstractHCLLexer {

    public TFLexer(LexerRestartInfo<HCLTokenId> info) {
        super(info, (input) -> new TerraformLexer(input));
    }

    @Override
    protected HCLTokenId mapTokenType(int type) {
        switch (type) {
            case TerraformLexer.BOOL:
            case TerraformLexer.COUNT:
            case TerraformLexer.DATA:
            case TerraformLexer.DEFAULT:
            case TerraformLexer.EACH:
            case TerraformLexer.FOR:
            case TerraformLexer.FOR_EACH:
            case TerraformLexer.IN:
            case TerraformLexer.LIFECYCLE:
            case TerraformLexer.LOCAL:
            case TerraformLexer.LOCALS:
            case TerraformLexer.MODULE:
            case TerraformLexer.MOVED:
            case TerraformLexer.OUTPUT:
            case TerraformLexer.PROVIDER:
            case TerraformLexer.RESOURCE:
            case TerraformLexer.TYPE:
            case TerraformLexer.TERRAFORM:
            case TerraformLexer.VAR:
            case TerraformLexer.VARIABLE:
                return KEYWORD;

            case TerraformLexer.LINE_COMMENT:
            case TerraformLexer.BLOCK_COMMENT:
                return COMMENT;

            case TerraformLexer.A_BOOL:
                return BOOLEAN;

            case TerraformLexer.A_NUMBER:
                return NUMBER;

            case TerraformLexer.IDENTIFIER:
                return VARIABLE;

            case TerraformLexer.LBRACE:
            case TerraformLexer.RBRACE:
            case TerraformLexer.LBRACK:
            case TerraformLexer.RBRACK:
            case TerraformLexer.LPAREN:
            case TerraformLexer.RPAREN:            
            case TerraformLexer.COMMA:
            case TerraformLexer.DOT:
            case TerraformLexer.EQUAL:
                return SEPARATOR;

            case TerraformLexer.COLON:
            case TerraformLexer.EQUALS:
            case TerraformLexer.GTE:
            case TerraformLexer.GT:
            case TerraformLexer.LTE:
            case TerraformLexer.LT:
            case TerraformLexer.PLUS:
            case TerraformLexer.MINUS:
            case TerraformLexer.NOT_EQUALS:
            case TerraformLexer.NOT:
            case TerraformLexer.QUESTION:
                return OPERATOR;
                
            case TerraformLexer.QUOTE:
            case TerraformLexer.HEREDOC:
                return STRING;

            case TerraformLexer.WS:
            case TerraformLexer.NL:
                return WHITESPACE;

            default:
                return ERROR;
        }
    }

    @Override
    protected HCLTokenId collateTokenType(int type) {
        switch (type) {
            case TerraformLexer.STRING_CONTENT:
                return STRING;
            case TerraformLexer.INTERPOLATION:
                return COMMENT;
            default:
                return null;
        }
    }

}

