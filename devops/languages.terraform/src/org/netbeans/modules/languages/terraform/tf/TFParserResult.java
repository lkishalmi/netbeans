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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.netbeans.modules.csl.api.Error;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.Severity;
import org.netbeans.modules.csl.spi.DefaultError;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.languages.terraform.grammar.TerraformLexer;
import org.netbeans.modules.languages.terraform.grammar.TerraformParser;
import org.netbeans.modules.languages.terraform.grammar.TerraformParserBaseListener;
import org.netbeans.modules.languages.terraform.grammar.TerraformParserListener;
import org.netbeans.modules.languages.terraform.model.DataDef;
import org.netbeans.modules.languages.terraform.model.ResourceDef;
import org.netbeans.modules.parsing.api.Snapshot;
import org.openide.filesystems.FileObject;

/**
 *
 * @author lkishalmi
 */
public class TFParserResult extends ParserResult {
    public final List<DefaultError> errors = new ArrayList<>();

    final List<OffsetRange> folds = new ArrayList<>();
    
    public final Map<String, ResourceDef> resources = new HashMap<>();
    public final Map<String, DataDef> data = new HashMap<>();
    
    volatile boolean finished = false;

    public TFParserResult(Snapshot snapshot) {
        super(snapshot);
    }

    TFParserResult get() {
        if (!finished) {
            CharStream cs = CharStreams.fromString(String.valueOf(getSnapshot().getText()));
            TerraformLexer lexer = new TerraformLexer(cs);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            TerraformParser ret = new TerraformParser(tokens);
            //ret.removeErrorListener(ConsoleErrorListener.INSTANCE);

            ret.addParseListener(createReferenceListener());
            ret.addParseListener(createFoldListener());

            ret.addErrorListener(createErrorListener());
            ret.tfFile();
            
            finished = true;
        }
        return this;
    }

    @Override
    protected void invalidate() {
    }

    @Override
    protected boolean processingFinished() {
        return finished;
    }

    @Override
    public List<? extends Error> getDiagnostics() {
        return Collections.unmodifiableList(errors);
    }

    protected final FileObject getFileObject() {
        return getSnapshot().getSource().getFileObject();
    }
        
    protected ANTLRErrorListener createErrorListener() {
        return new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                int errorPosition = 0;
                if (offendingSymbol instanceof Token) {
                    Token offendingToken = (Token) offendingSymbol;
                    errorPosition = offendingToken.getStartIndex();
                }
                errors.add(new DefaultError(null, msg, null, getFileObject(), errorPosition, errorPosition, Severity.ERROR));
            }

        };
    }
    private static String getString(TerraformParser.SimpleStringContext ctx) {
        TerraformParser.StringContext s = ctx.string();
        StringBuilder sb = new StringBuilder(ctx.stop.getStartIndex() - ctx.start.getStopIndex());
        if (s != null) {
            for (TerminalNode terminalNode : s.STRING_CONTENT()) {
                sb.append(terminalNode.getText());
            }
        }
        return sb.toString();
    }
    
    private TerraformParserListener createReferenceListener() {
        return new TerraformParserBaseListener() {
            @Override
            public void exitResourceDef(TerraformParser.ResourceDefContext ctx) {
                String type = getString(ctx.simpleString(0));
                String name = getString(ctx.simpleString(1));
                ResourceDef res = new ResourceDef(type, name);
                
                resources.put(res.getId(), res);
            }

            @Override
            public void exitDataDef(TerraformParser.DataDefContext ctx) {
                String type = getString(ctx.simpleString(0));
                String name = getString(ctx.simpleString(1));
                DataDef def = new DataDef(type, name);
                
                data.put(def.getId(), def);
            }
            
        };
    }

    private TerraformParserListener createFoldListener() {
        return new TerraformParserBaseListener() {
            
            private void addFold(ParserRuleContext ctx) {
                int start = ctx.start.getStopIndex() + 1;
                int stop = ctx.stop.getStartIndex();
                if(start >= stop) {
                    return;
                }
                OffsetRange range = new OffsetRange(start, stop);
                folds.add(range);
            }
            
            @Override
            public void exitArray(TerraformParser.ArrayContext ctx) {
                addFold(ctx);
            }

            @Override
            public void exitBlock(TerraformParser.BlockContext ctx) {
                addFold(ctx);
            }

            @Override
            public void exitSimpleBlock(TerraformParser.SimpleBlockContext ctx) {
                addFold(ctx);
            }
            
        };
    }
}
