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
package org.netbeans.modules.spellchecker;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import org.apache.lucene.analysis.hunspell.AffixedWord;
import org.apache.lucene.analysis.hunspell.Hunspell;
import org.apache.lucene.store.FSDirectory;
import org.netbeans.modules.spellchecker.spi.dictionary.Dictionary;
import org.netbeans.modules.spellchecker.spi.dictionary.ValidityType;
import org.openide.filesystems.FileObject;
import org.openide.modules.Places;

/**
 *
 * @author lkishalmi
 */
public final class HunspellDictionary implements Dictionary {
    
    private final Hunspell hunspell;

    private HunspellDictionary(Hunspell hunspell) {
        this.hunspell = hunspell;
    }

    @Override
    public ValidityType validateWord(CharSequence word) {
        return hunspell.spell(word.toString()) ? ValidityType.VALID : ValidityType.INVALID;
    }

    @Override
    public List<String> findValidWordsForPrefix(CharSequence word) {
        return hunspell.getAllWordForms(word.toString())
                .stream()
                .map(AffixedWord::getWord)
                .toList();
    }

    @Override
    public List<String> findProposals(CharSequence word) {
        return hunspell.suggest(word.toString());
    }
    
    static HunspellDictionary createFromFolder(FileObject folder) throws IOException {
        FileObject hunspellDic = folder.getFileObject("hunspell.dic");
        FileObject hunspellAff = folder.getFileObject("hunspell.aff");
        try (var is = hunspellDic.getInputStream()) {
        } catch (IOException ex){}
        File cacheDir = Places.getCacheSubdirectory("Spellchecker/Dictionaries/" + folder.getName());
        try (var dir = FSDirectory.open(cacheDir.toPath()); var dic = hunspellDic.getInputStream(); var aff = hunspellAff.getInputStream()) {
            var dictionary = new org.apache.lucene.analysis.hunspell.Dictionary(dir, "hunspell", dic, aff);
            var hunspell = new Hunspell(dictionary);
            return new HunspellDictionary(hunspell);
        } catch (IOException ex) {
            throw ex;
        } catch (ParseException pe) {
            throw new IOException(pe);
        }
    }
}
