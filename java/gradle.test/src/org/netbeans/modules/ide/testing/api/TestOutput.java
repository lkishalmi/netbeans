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

import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author lkishalmi
 */
public class TestOutput {
    public enum Type { STDOUT, STDERR }
    
    public final static class Event {
        private final String message;
        private final Type type;
        private final long time;

        Event(String message, Type type, long time) {
            this.message = message;
            this.type = type;
            this.time = time;
        }
       
        public String getMessage() {
            return message;
        }

        public Type getType() {
            return type;
        }

        public long getTime() {
            return time;
        }
        
        
    }

    public static final TestOutput EMPTY = new TestOutput(0);
    
    private int length;
    private final LinkedList<Event> events = new LinkedList<>();
    private int maxEvents = 200;
    
    public TestOutput(int maxEvents) {
        this.maxEvents = maxEvents;
    }
    
    
    synchronized void clear() {
        events.clear();
        length = 0;
    }
    
    synchronized void append(Event evt) {
        if (maxEvents > 0) {
            events.addLast(evt);
            length += evt.message.length();
            while (events.size() > maxEvents) {
                Event rem = events.removeFirst();
                length -= rem.message.length();
            }
        }
    }
    
    public List<Event> getEvents() {
        return new LinkedList<>(events);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(length);
        for (Event event : events) {
            sb.append(event.message);
        }
        return sb.toString();
    }
}
