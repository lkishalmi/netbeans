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

import org.netbeans.modules.ide.testing.api.TestCase.Snapshot;
import org.openide.util.Lookup;

/**
 *
 * @author lkishalmi
 */
public final class TestCase extends TestItem<Snapshot> {

    
    public enum Status {
        PENDING, PASSED, FAILED, SKIPPED, ERRORED, ABORTED, IGNORED
    }
    
    public interface Snapshot extends TestItem.Snapshot {
        Status getStatus();
        long startTime();
        long endTime();
    }
    
    private Status status;
    private TestOutput output;
    private long startTime;
    private long endTime;
    private String message;

    public TestCase(String id, String name, Lookup lookup) {
        super(id, name, lookup);
        startTime = System.currentTimeMillis();
    }
    
    @Override
    Snapshot getSnapshot() {
        return new SnapshotImpl();
    }
    
    public void finishIn(Status finalStatus, String message, long time) {
        setStatus(finalStatus);
        this.message = message;
        this.endTime = System.currentTimeMillis();
        this.startTime = this.endTime - time;
        
    }
    
    public void finish(Status finalStatus, String message, long time) {
        setStatus(finalStatus);
        this.message = message;
        this.endTime = time;
    }

    private void setStatus(Status finalStatus) {
        if (status != Status.PENDING) {
            throw new IllegalStateException("TestCase " + name + " is already " + status);
        }
        if (finalStatus == Status.PENDING) {
            throw new IllegalStateException("TestCase " + name + " cannot finish with PENDING status.");
        }
        status = finalStatus;
    }

    public void passed() {
        finish(Status.PASSED, null, System.currentTimeMillis());
    }
    
    public void passedIn(long time) {
        finishIn(Status.PASSED, null, time);        
    }
    
    public void failed(String message) {
        finish(Status.FAILED, message, System.currentTimeMillis());
    }
    
    public void failed
    private class SnapshotImpl extends TestItem.SnapshotImpl implements TestCase.Snapshot {
        
        private final Status status;
        private final long endTime;
        
        SnapshotImpl() {
            this.status = TestCase.this.status;
            this.endTime = status == TestCase.Status.PENDING ? System.currentTimeMillis() : TestCase.this.endTime;
        }

        @Override
        public int getTestCount() {
            return 1;
        }

        @Override
        public int getTestCountWithStatus(Status status) {
            return this.status == status ? 1 : 0;
        }

        @Override
        public Status getStatus() {
            return status;
        }

        @Override
        public long startTime() {
            return startTime;
        }

        @Override
        public long endTime() {
            return endTime;
        }
    }
}
