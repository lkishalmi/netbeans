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

import java.util.LinkedHashMap;
import java.util.Map;
import org.openide.util.Lookup;

/**
 *
 * @author lkishalmi
 */
public final class TestSession {
    
    public enum Status { RUNNING, ABORTED, FINISHED }
    
    Map<String, TestSuite> testSuites = new LinkedHashMap<>();
    Status status = Status.RUNNING;
    
    public TestSuite newTestSuite(String id, String name, Lookup lookup) {
        TestSuite ret = new TestSuite(id, name, lookup);
        testSuites.put(ret.getId(), ret);
        return ret;
    }

    public TestSuite newTestSuite(String name, Lookup lookup) {
        return newTestSuite(name, name, lookup);
    }
    
    public TestSuite newTestSuite(String id, String name) {
        return newTestSuite(id, name, Lookup.EMPTY);
    }

    public TestSuite newTestSuite(String name) {
        return newTestSuite(name, name, Lookup.EMPTY);
    }
    
    public TestReport createReport() {
        return new TestReportImpl();
    }
    
    private class TestReportImpl implements TestReport {

        private int testCount = 0;
        private int[] summary = new int[TestCase.Status.values().length];
        
        private TestReportImpl() {
            Map<String, TestSuite> tss = new LinkedHashMap<>();
            for (Map.Entry<String, TestSuite> entry : testSuites.entrySet()) {
                TestSuite.Snapshot ss = entry.getValue().getSnapshot();
                testCount += ss.getTestCount();
                for (TestCase.Status status : TestCase.Status.values()) {
                    summary[status.ordinal()] += ss.getTestCountWithStatus(status);
                }
            }
        }
        
        @Override
        public int getTestCount() {
            return testCount;
        }

        @Override
        public int getTestCountWithStatus(TestCase.Status status) {
            return summary[status.ordinal()];
        }
    
    }
}
