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
package org.netbeans.modules.terraform.completion;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.swing.text.AbstractDocument;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.modules.languages.hcl.ast.HCLAttribute;
import org.netbeans.modules.languages.hcl.ast.HCLBlock;
import org.netbeans.modules.languages.hcl.ast.HCLElement;
import org.netbeans.modules.languages.hcl.ast.HCLExpression;
import org.netbeans.modules.languages.hcl.terraform.TerraformLanguage;
import org.netbeans.modules.languages.hcl.terraform.TerraformParserResult;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.Source;
import org.netbeans.modules.parsing.spi.ParseException;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;

/**
 *
 * @author lkishalmi
 */
@MimeRegistration(mimeType = TerraformLanguage.MIME_TYPE, service = CompletionProvider.class)
public class TerraformCompletionProvider implements CompletionProvider {

    @Override
    public CompletionTask createTask(int queryType, JTextComponent component) {
        return switch(queryType) {
            case COMPLETION_QUERY_TYPE -> new AsyncCompletionTask(new TerraformCompletionQuery(), component);
            default -> null;
        };
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        return 0;
    }

    private static class TerraformCompletionQuery extends AsyncCompletionQuery {

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            try {
                if (doc instanceof AbstractDocument adoc) adoc.readLock();
                Source source = Source.create(doc);
                ParserManager.parse(Set.of(source), (results) -> {
                    Parser.Result result = results.getParserResult();
                    if (result instanceof TerraformParserResult tf) {
                        ContextType ctype = getContextType(tf, caretOffset);
                        switch (ctype) {
                            case ROOT:
                                Stream.of("data", "resource", "variable")
                                        .map((text) -> CompletionUtilities.newCompletionItemBuilder(text).build())
                                        .forEach(resultSet::addItem);
                        }
                    }
                });
                TokenHierarchy<?> th = source.createSnapshot().getTokenHierarchy();
                if (th != null) {
                    
                }

            } catch (ParseException pe) {
            } finally{
                if (doc instanceof AbstractDocument adoc) adoc.readUnlock();
                resultSet.finish();
            }
        }

    }

    private enum ContextType {
        ROOT,
        ATTRIBUTE,
        EXPRESSION
    }

    private static ContextType getContextType(TerraformParserResult tf, int caretOffset) {
        ContextType ret = null;
        List<? extends HCLElement> elementsAt = tf.getReferences().elementsAt(caretOffset);
        Iterator<? extends HCLElement> it = elementsAt.iterator();
        HCLElement prev = null;
        while ((ret == null) && it.hasNext()) {
            HCLElement e = it.next();
            if (e instanceof HCLAttribute attr) {
                ret = (prev == attr.value()) ? ContextType.EXPRESSION : ContextType.ATTRIBUTE;
            }
            if (e instanceof HCLBlock) {
                ret = ContextType.ATTRIBUTE;
            }
            prev = e;
        }
        return ret != null ? ret : ContextType.ROOT;
    }

    private static class CompletionItemFactory {

        public CompletionItem createItem(String text) {
            return CompletionUtilities.newCompletionItemBuilder(text).build();
        }
    }
}
