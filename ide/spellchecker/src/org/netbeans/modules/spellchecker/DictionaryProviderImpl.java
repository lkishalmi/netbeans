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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import org.netbeans.modules.spellchecker.spi.dictionary.Dictionary;
import org.netbeans.modules.spellchecker.spi.dictionary.DictionaryProvider;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Jan Lahoda
 */
@ServiceProvider(service=DictionaryProvider.class)
public class DictionaryProviderImpl implements DictionaryProvider {
    
    private static final String DICTIONARIES_ROOT = "Editors/Spellchecker/Dictionaries";
    
    /** Creates a new instance of DictionaryProviderImpl */
    public DictionaryProviderImpl() {
    }

    private final Map<Locale, Dictionary> dictionaries = new HashMap<>();
    
//    public DictionaryImpl getDefault() {
//        return getDictionary(Locale.getDefault());
//    }
    
    public synchronized void clearDictionaries() {
        dictionaries.clear();
    }
    
    @Override
    public synchronized Dictionary getDictionary(Locale locale) {
        return dictionaries.computeIfAbsent(locale, this::createDictionary);
    }
    
    public static synchronized Locale[] getInstalledDictionariesLocales() {
        Collection<Locale> hardcoded = new HashSet<>();
        Collection<Locale> maskedHardcoded = new HashSet<>();
        Collection<Locale> user = new HashSet<>();
        
        for (File dictDir : InstalledFileLocator.getDefault().locateAll("modules/dict", null, false)) {
            File[] children = dictDir.listFiles((File pathname) -> pathname.isFile() && pathname.getName().startsWith("dictionary_"));
            
            if (children == null)
                continue;
            
            for (int cntr = 0; cntr < children.length; cntr++) {
                String name = children[cntr].getName();
                
                name = name.substring("dictionary_".length());

                Collection<Locale> target;

                if (name.endsWith("_hidden")) {
                    target = maskedHardcoded;
                } else {
                    if (name.endsWith(".description")) {
                        target = hardcoded;
                    } else {
                        target = user;
                    }
                }
                
                int dot = name.indexOf('.');
                
                if (dot != (-1))
                    name = name.substring(0, dot);
                
                target.add(Utilities.name2Locale(name));
            }
        }

        hardcoded.removeAll(maskedHardcoded);
        hardcoded.addAll(user);
        return hardcoded.toArray(Locale[]::new);
    }
    
    private synchronized Dictionary createDictionary(Locale locale) {
        Dictionary ret = null;
        try {
            FileObject dicts = FileUtil.getConfigFile(DICTIONARIES_ROOT);
            if (dicts != null) {
                FileObject localeDict = dicts.getFileObject(locale.toString());
                if (localeDict != null) {
                    ret = HunspellDictionary.createFromFolder(localeDict);
                }
            }
        } catch (IOException e) {
            ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
        }
        return ret;
    }

    static String getDictionaryStream(Locale locale, List<URL> streams) throws IOException {
        FileObject dicts = FileUtil.getConfigFile(DICTIONARIES_ROOT);
        if (dicts != null) {
            Iterator<String> suffixes = getLocalizingSuffixes(locale);

            while (suffixes.hasNext()) {
                String currentSuffix = suffixes.next();
                
                FileObject lfolder = dicts.getFileObject(currentSuffix);
                if (lfolder != null) {
                    streams.add(lfolder.toURL());
                    return currentSuffix;
                }
            }
        }
        
        return null;
    }

    //Copied from NbBundle:
    /** Get a list of all suffixes used to search for localized resources.
     * Based on the default locale and branding, returns the list of suffixes
     * which various <code>NbBundle</code> methods use as the search order.
     * For example, you might get a sequence such as:
     * <ol>
     * <li><samp>"_branding_de"</samp>
     * <li><samp>"_branding"</samp>
     * <li><samp>"_de"</samp>
     * <li><samp>""</samp>
     * </ol>
     * @return a read-only iterator of type <code>String</code>
     * @since 1.1.5
     */
    static Iterator<String> getLocalizingSuffixes(Locale locale) {
        return new LocaleIterator(locale);
    }
    
    /** This class (enumeration) gives all localized suffixes using nextElement
     * method. It goes through given Locale and continues through Locale.getDefault()
     * Example 1:
     *   Locale.getDefault().toString() -> "_en_US"
     *   you call new LocaleIterator(new Locale("cs", "CZ"));
     *  ==> You will gets: "_cs_CZ", "_cs", "", "_en_US", "_en"
     *
     * Example 2:
     *   Locale.getDefault().toString() -> "_cs_CZ"
     *   you call new LocaleIterator(new Locale("cs", "CZ"));
     *  ==> You will gets: "_cs_CZ", "_cs", ""
     *
     * If there is a branding token in effect, you will get it too as an extra
     * prefix, taking precedence, e.g. for the token "f4jce":
     *
     * "_f4jce_cs_CZ", "_f4jce_cs", "_f4jce", "_f4jce_en_US", "_f4jce_en", "_cs_CZ", "_cs", "", "_en_US", "_en"
     *
     * Branding tokens with underscores are broken apart naturally: so e.g.
     * branding "f4j_ce" looks first for "f4j_ce" branding, then "f4j" branding, then none.
     */
    private static class LocaleIterator extends Object implements Iterator<String> {
//        /** this flag means, if default locale is in progress */
//        private boolean defaultInProgress = false;
        
        /** this flag means, if empty sufix was exported yet */
        private boolean empty = false;
        
        /** current locale, and initial locale */
        private Locale locale, initLocale;
        
        /** current sufix which will be returned in next calling nextElement */
        private String current;
        
        /** the branding string in use */
        private String branding;
        
        /** Creates new LocaleIterator for given locale.
         * @param locale given Locale
         */
        public LocaleIterator(Locale locale) {
            this.locale = this.initLocale = locale;
//            if (locale.equals(Locale.getDefault())) {
//                defaultInProgress = true;
//            }
            current = '_' + locale.toString();
            if (NbBundle.getBranding() == null)
                branding = null;
            else
                branding = "_" + NbBundle.getBranding(); // NOI18N
            //System.err.println("Constructed: " + this);
        }
        
        /** @return next sufix.
         * @exception NoSuchElementException if there is no more locale sufix.
         */
        @Override
        public String next() throws NoSuchElementException {
            if (current == null)
                throw new NoSuchElementException();
            
            final String ret;
            if (branding == null) {
                ret = current;
            } else {
                ret = branding + current;
            }
            int lastUnderbar = current.lastIndexOf('_');
            if (lastUnderbar == 0) {
                if (empty)
                    reset();
                else {
                    current = ""; // NOI18N
                    empty = true;
                }
            }
            else {
                if (lastUnderbar == -1) {
                        reset();
                }
                else {
                    current = current.substring(0, lastUnderbar);
                }
            }
            //System.err.println("Returning: `" + ret + "' from: " + this);
            return ret;
        }
        
        /** Finish a series.
         * If there was a branding prefix, restart without that prefix
         * (or with a shorter prefix); else finish.
         */
        private void reset() {
            if (branding != null) {
                current = '_' + initLocale.toString();
                int idx = branding.lastIndexOf('_');
                if (idx == 0)
                    branding = null;
                else
                    branding = branding.substring(0, idx);
                empty = false;
            } else {
                current = null;
            }
        }
        
        /** Tests if there is any sufix.*/
        @Override
        public boolean hasNext() {
            return (current != null);
        }
        
        @Override
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }
        
    } // end of LocaleIterator

}
