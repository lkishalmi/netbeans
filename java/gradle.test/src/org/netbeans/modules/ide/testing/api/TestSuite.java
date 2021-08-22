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
package org.netbeans.modules.ide.testing.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.netbeans.modules.ide.testing.api.TestSuite.Snapshot;
import org.openide.util.Lookup;

/**
 *
 * @author lkishalmi
 */
public final class TestSuite extends TestItem<Snapshot> {
    final Map<String, TestCase> testcases = new LinkedHashMap<>();

    
    interface Snapshot extends TestItem.Snapshot {
        Map<String, TestCase.Snapshot> getTestcases();
    }

    TestSuite(String id, String name, Lookup lookup) {
        super(id, name, lookup);
    }
    
    public TestCase newTestCase(String id, String name , Lookup lookup) {
        TestCase ret = new TestCase(id, name, lookup);
        testcases.put(ret.getId(), ret);
        return ret;
    }
    
    public TestCase newTestCase(String name, Lookup lookup) {
        return newTestCase(name, name, lookup);
    }
    
    public TestCase newTestCase(String id, String name) {
        return newTestCase(id, name, Lookup.EMPTY);
    }
    
    public TestCase newTestCase(String name) {
        return newTestCase(name, name, Lookup.EMPTY);
    }
    
    public TestCase getTestCase(String id) {
        return testcases.get(id);
    }
    
    @Override
    Snapshot getSnapshot() {
        return new SnapshotImpl();
    }
    
    private class SnapshotImpl extends TestItem.SnapshotImpl implements TestSuite.Snapshot {

        final Map<String, TestCase.Snapshot> testcases;
        final int[] summary = new int[TestCase.Status.values().length];
        
        SnapshotImpl() {
            Map<String, TestCase.Snapshot> tcs = new LinkedHashMap<>(TestSuite.this.testcases.size());
            for (TestCase tc: TestSuite.this.testcases.values()) {
                TestCase.Snapshot snapshot = tc.getSnapshot();
                summary[snapshot.getStatus().ordinal()]++;
            }
            testcases = Collections.unmodifiableMap(tcs);
        }
        
        @Override
        public int getTestCount() {
            return testcases.size();
        }

        @Override
        public int getTestCountWithStatus(TestCase.Status status) {
            return summary[status.ordinal()];
        }

        @Override
        public Map<String, TestCase.Snapshot> getTestcases() {
            return testcases;
        }
        
    }
}
