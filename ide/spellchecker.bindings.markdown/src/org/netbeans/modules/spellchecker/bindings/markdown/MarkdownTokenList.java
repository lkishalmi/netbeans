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
/*
 * Contributor(s): Stefan Riha, Roland Poppenreiter
 */
package org.netbeans.modules.spellchecker.bindings.markdown;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.editor.document.LineDocumentUtils;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.modules.spellchecker.spi.language.TokenList;
import org.netbeans.modules.spellchecker.spi.language.TokenListProvider;
import org.openide.ErrorManager;


public final class MarkdownTokenList implements TokenList {

    record SpellSpan(int begin, int end) {
        public static final SpellSpan NONE = new SpellSpan(-1, -1);
    };

    private final BaseDocument doc;
    private CharSequence currentWord;
    private int currentStartOffset;
    private int nextSearchOffset;
    private int ignoreBefore;

    /** Creates a new instance of HtmlXmlTokenList */
    MarkdownTokenList(BaseDocument doc) {
        this.doc = doc;
    }

    @Override
    public void setStartOffset(int offset) {
        currentWord = null;
        currentStartOffset = (-1);
        this.ignoreBefore = offset;
        try {
            this.nextSearchOffset = LineDocumentUtils.getLineStartOffset(doc, offset);
        } catch (BadLocationException ex) {
            Logger.getLogger(MarkdownTokenList.class.getName()).log(Level.FINE, null, ex);
            this.nextSearchOffset = offset;
        }
    }

    @Override
    public int getCurrentWordStartOffset() {
        return currentStartOffset;
    }

    @Override
    public CharSequence getCurrentWordText() {
        return currentWord;
    }

    @Override
    public boolean nextWord() {
        boolean next = nextWordImpl();

        while (next && (currentStartOffset + currentWord.length()) < ignoreBefore) {
            next = nextWordImpl();
        }

        return next;
    }

    private boolean nextWordImpl() {
        try {
            SpellSpan span = findNextSpellSpan();

            while (span.begin != -1) {
                int offset = (span.begin < nextSearchOffset) ? nextSearchOffset : span.begin;

                boolean searching = true;

					 /* find next word */
                while (offset < span.end) {
                    String t = doc.getText(offset, 1);
                    char c = t.charAt(0);

                    if (searching) {
                        if (Character.isLetter(c)) {
									 /* word beginn found */
                            searching = false;
                            currentStartOffset = offset;
                        }
                    } else {
                        if (!Character.isLetter(c)) {
                            /* word end found */
                            nextSearchOffset = offset;
                            currentWord = doc.getText(currentStartOffset, offset - currentStartOffset);
                            return true;
                        }
                    }

                    offset++;
                }

                nextSearchOffset = offset;

                if (!searching) {
                    currentWord = doc.getText(currentStartOffset, offset - currentStartOffset);
                    return true;
                }

                span = findNextSpellSpan();
            }

            return false;
        } catch (BadLocationException e) {
            ErrorManager.getDefault().notify(e);
            return false;
        }
    }

    private SpellSpan findNextSpellSpan() {
        TokenHierarchy<Document> h = TokenHierarchy.get((Document) doc);
        TokenSequence<?> ts = h.tokenSequence();
        if (ts != null) {
            ts.move(nextSearchOffset);

            while (ts.moveNext()) {
                TokenId id = ts.token().id();

                return new SpellSpan(ts.offset(), ts.offset() + ts.token().length());
            }
        }
        return SpellSpan.NONE;
    }


    @Override
    public void addChangeListener(ChangeListener l) {
    //ignored...
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
    //ignored...
    }

    public static final class Provider implements TokenListProvider {

        public Provider() {}

        @Override
        public TokenList findTokenList(Document doc) {
            if (doc instanceof BaseDocument bdoc) {
                String docMimetype = NbEditorUtilities.getMimeType(doc);

                if ("text/x-markdown".equals(docMimetype)) { //NOI18N
                    return new MarkdownTokenList(bdoc);
                }
            }
            return null;

        }

    }
}

