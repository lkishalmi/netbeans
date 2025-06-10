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

package org.netbeans.modules.java.source.usages;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.OpenBitSet;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.api.java.source.ClassIndex.SearchScopeType;
import org.netbeans.modules.java.source.usages.ClassIndexImpl.UsageType;
import org.netbeans.modules.parsing.lucene.support.Queries;
import org.netbeans.modules.parsing.lucene.support.StoppableConvertor;
import org.openide.util.Pair;
import org.openide.util.Parameters;

/**
 *
 * @author Tomas Zezula
 */
class QueryUtil {
        
    
    
    static Query createUsagesQuery(
            final @NonNull String resourceName,
            final @NonNull Set<? extends ClassIndexImpl.UsageType> mask,
            final @NonNull Occur operator) {
        Parameters.notNull("resourceName", resourceName);
        Parameters.notNull("mask", mask);
        Parameters.notNull("operator", operator);
        if (operator == Occur.SHOULD) {
            final BooleanQuery query = new BooleanQuery ();
            for (ClassIndexImpl.UsageType ut : mask) {
                final Query subQuery = new WildcardQuery(
                    DocumentUtil.referencesTerm (
                        resourceName,
                        EnumSet.of(ut),
                        false));
                query.add(subQuery, operator);
            }
            return query;
        } else if (operator == Occur.MUST) {
            return new WildcardQuery(
                DocumentUtil.referencesTerm (
                    resourceName,
                    mask,
                    false));
        } else {
            throw new IllegalArgumentException();
        }
    }

    @NonNull
    static Query createPackageUsagesQuery (
            @NonNull final String packageName,
            @NonNull final Set<? extends UsageType> mask,
            @NonNull Occur operator) {
        Parameters.notNull("packageName", packageName); //NOI18N
        Parameters.notNull("mask", mask); //NOI18N
        final String pattern = Pattern.quote(packageName) + "\\.[^\\.]+";   //NOI18N
        if (operator == Occur.SHOULD) {
            final BooleanQuery query = new BooleanQuery ();
            for (ClassIndexImpl.UsageType ut : mask) {
                final Term t = DocumentUtil.referencesTerm (
                        pattern,
                        EnumSet.of(ut),
                        true);
                query.add(Queries.createQuery(t.field(), t.field(), t.text(), Queries.QueryKind.REGEXP), operator);
            }
            return query;
        } else if (operator == Occur.MUST) {
            final Term t = DocumentUtil.referencesTerm (
                    pattern,
                    mask,
                    true);
            return Queries.createQuery(t.field(), t.field(), t.text(), Queries.QueryKind.REGEXP);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @CheckForNull
    static Query scopeFilter (
            @NonNull final Query q,
            @NonNull final Set<? extends SearchScopeType> scope) {
        assert q != null;
        assert scope != null;
        TreeSet<String> pkgs = null;
        for (SearchScopeType s : scope) {
            final Set<? extends String> sp = s.getPackages();
            if (sp != null) {
                if (pkgs == null) {
                    pkgs = new TreeSet<>();
                }
                pkgs.addAll(sp);
            }
        }
        if (pkgs == null) {
            return q;
        }
        switch (pkgs.size()) {
            case 0:
                return null;
            case 1:
            {
                //Todo perf: Use filter query
                final BooleanQuery qFiltered = new BooleanQuery();
                qFiltered.add(
                    new TermQuery(
                        new Term (
                            DocumentUtil.FIELD_PACKAGE_NAME,
                            pkgs.iterator().next())),
                    Occur.MUST);
                qFiltered.add(q, Occur.MUST);
                return qFiltered;
            }
            default:
            {
                return new FilteredQuery(q, new PackagesFilter(pkgs));
            }
        }
    }

    static Pair<StoppableConvertor<Term,String>,Term> createPackageFilter(
            final @NullAllowed String prefix,
            final boolean directOnly) {
        final Term startTerm = new Term (DocumentUtil.FIELD_PACKAGE_NAME, prefix);
        final StoppableConvertor<Term,String> filter = new PackageFilter(startTerm, directOnly);
        return Pair.of(filter,startTerm);
    }

                                    
    private static final class PackageFilter implements StoppableConvertor<Term, String> {
        
        private static final Stop STOP = new Stop();
        
        private final boolean directOnly;
        private final boolean all;
        private final String fieldName;
        private final String value;
        
        PackageFilter(final @NonNull Term startTerm, final boolean directOnly) {
            this.fieldName = startTerm.field();
            this.value = startTerm.text();
            this.directOnly = directOnly;
            this.all = value.length() == 0;
        }
        
        @Override
        public String convert(Term currentTerm) throws Stop {
            if (fieldName != currentTerm.field()) {
                throw STOP;
            }
            String currentText = currentTerm.text();
            if (all || currentText.startsWith(value)) {
                if (directOnly) {
                    int index = currentText.indexOf('.', value.length());    //NOI18N
                    if (index>0) {
                        currentText = currentText.substring(0,index);
                    }
                }
                return currentText;
            }
            return null;
        }
    }

    private static final class PackagesFilter extends Filter {

        private final SortedSet<String> pkgs;

        PackagesFilter(@NonNull SortedSet<String> pkgs) {
            assert pkgs != null;
            this.pkgs = pkgs;
        }

        @NonNull
        @Override
        public DocIdSet getDocIdSet(AtomicReaderContext arc, Bits bits) throws IOException {
            if (pkgs.isEmpty()) {
                return DocIdSet.EMPTY_DOCIDSET;
            }
            List<Term> terms = pkgs.stream()
                                   .map(pkg -> new Term(DocumentUtil.FIELD_PACKAGE_NAME, pkg))
                                   .toList();
            AtomicReader reader = arc.reader();
            OpenBitSet bitSet = new OpenBitSet(reader.maxDoc());
            for (Term term : terms) {
                DocsEnum docsEnum = reader.termDocsEnum(term);
                int doc = docsEnum.nextDoc();
                while (doc != DocsEnum.NO_MORE_DOCS) {
                    bitSet.set(doc);
                    doc = docsEnum.nextDoc();
                }
            }
            return bitSet;
        }

    }
}
