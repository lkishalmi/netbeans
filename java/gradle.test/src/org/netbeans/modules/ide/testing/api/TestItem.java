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

import org.netbeans.modules.ide.testing.api.TestItem.Snapshot;
import org.openide.util.Lookup;

/**
 *
 * @author lkishalmi
 */
public abstract class TestItem<T extends Snapshot> {

    final String id;
    final String name;
    final Lookup lookup;
    TestOutput output;

    TestItem(String id, String name, Lookup lookup) {
        this.id = id;
        this.name = name;
        this.lookup = lookup;
    }

    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public Lookup getLookup() {
        return lookup;
    }
    
    public synchronized void output(String message, TestOutput.Type type, long time ) {
        if (output == null) {
            output = new TestOutput(200);
        }
        output.append(new TestOutput.Event(message, type, time));
    }
    
    public void output(String message, TestOutput.Type type) {
        output(message, type, -1);
    }

    public void output(String message) {
        output(message, TestOutput.Type.STDOUT, -1);
    }
    
    public synchronized TestOutput getOutput() {
        return output != null ? output : TestOutput.EMPTY;
    }
    
    abstract T getSnapshot();

    public interface Snapshot extends TestSummary {
        String getId();
        String getName();
        Lookup getLookup();
    }

    abstract class SnapshotImpl implements Snapshot {

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Lookup getLookup() {
            return lookup;
        }
        
    }
}
