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
package org.netbeans.modules.terraform.indexing;

import java.io.IOException;
import java.util.List;
import org.netbeans.modules.languages.hcl.ast.HCLIdentifier;
import org.netbeans.modules.languages.hcl.terraform.TerraformParserResult;
import org.netbeans.modules.languages.hcl.terraform.TerraformParserResult.BlockType;
import org.netbeans.modules.parsing.api.Snapshot;
import org.netbeans.modules.parsing.spi.Parser;
import org.netbeans.modules.parsing.spi.indexing.Context;
import org.netbeans.modules.parsing.spi.indexing.EmbeddingIndexer;
import org.netbeans.modules.parsing.spi.indexing.EmbeddingIndexerFactory;
import org.netbeans.modules.parsing.spi.indexing.Indexable;
import org.netbeans.modules.parsing.spi.indexing.support.IndexDocument;
import org.netbeans.modules.parsing.spi.indexing.support.IndexingSupport;

/**
 *
 * @author lkishalmi
 */
public class TerraformIndexer extends EmbeddingIndexer {

    public static final String INDEXER_NAME = "terraform";
    public static final int INDEX_VERSION = 1;

    public static final String ROOT_BLOCK_KIND = "kind";
    public static final String ROOT_BLOCK_TYPE = "type";
    public static final String ROOT_BLOCK_NAME = "name";

    @Override
    protected void index(Indexable indexable, Parser.Result parserResult, Context context) {
        if (parserResult instanceof TerraformParserResult result){
            try {
                IndexingSupport support = IndexingSupport.getInstance(context);
                result.getDocument().blocks().forEach((block) -> {
                    IndexDocument doc = support.createDocument(indexable);
                    List<HCLIdentifier> d = block.declaration();
                    BlockType kind = BlockType.get(d.get(0).id());
                    if (kind != null) {
                        doc.addPair(ROOT_BLOCK_KIND, kind.name(), true, false);
                        switch (kind) {
                            case DATA, RESOURCE -> {
                                if (d.size() > 1) {
                                    doc.addPair(ROOT_BLOCK_TYPE, d.get(1).id(), true, true);
                                }
                                if (d.size() > 2) {
                                    doc.addPair(ROOT_BLOCK_NAME, d.get(2).id(), true, true);
                                }
                            }
                            case VARIABLE -> {
                                if (d.size() > 1) {
                                    doc.addPair(ROOT_BLOCK_NAME, d.get(1).id(), true, true);
                                }
                            }
                        }
                    }
                    support.addDocument(doc);
                });

            } catch (IOException io) {

            }
        }
    }

    public static class TerraformIndexerFactory extends EmbeddingIndexerFactory {
        private static  final TerraformIndexer INSTANCE = new TerraformIndexer();

        @Override
        public EmbeddingIndexer createIndexer(Indexable indexable, Snapshot snapshot) {
            return INSTANCE;
        }

        @Override
        public void filesDeleted(Iterable<? extends Indexable> deleted, Context context) {
            try {
                IndexingSupport support = IndexingSupport.getInstance(context);
                deleted.forEach(support::removeDocuments);
            } catch (IOException ie) {}
        }

        @Override
        public void filesDirty(Iterable<? extends Indexable> dirty, Context context) {
            try {
                IndexingSupport support = IndexingSupport.getInstance(context);
                dirty.forEach(support::markDirtyDocuments);
            } catch (IOException ie) {}
        }

        @Override
        public String getIndexerName() {
            return INDEXER_NAME;
        }

        @Override
        public int getIndexVersion() {
            return INDEX_VERSION;
        }

    }
}
