/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.executiongraph;

import java.util.Iterator;
import java.util.NoSuchElementException;

class AllVerticesIterator<EV extends AccessExecutionVertex, EJV extends AccessExecutionJobVertex>
        implements Iterator<EV> {

    private final Iterator<EJV> jobVertices;

    private EV[] currVertices;

    private int currPos;

    // 该迭代器传入的泛型是 EJV ,输出的泛型是 EV ( 内部相当于有 flat map 的功效 )
    public AllVerticesIterator(Iterator<EJV> jobVertices) {
        this.jobVertices = jobVertices;
    }

    @Override
    public boolean hasNext() {
        while (true) {
            if (currVertices != null) {
                if (currPos < currVertices.length) {
                    return true;
                } else {
                    currVertices = null;
                }
            } else if (jobVertices.hasNext()) {
                currVertices = (EV[]) jobVertices.next().getTaskVertices();
                currPos = 0;
            } else {
                return false;
            }
        }
    }

    @Override
    public EV next() {
        if (hasNext()) {
            return currVertices[currPos++];
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
