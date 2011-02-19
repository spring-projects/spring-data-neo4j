/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.GraphDatabaseService;

public interface GraphCallback<T> {
    T doWithGraph(final GraphDatabaseService graph) throws Exception;

    public abstract class WithoutResult implements GraphCallback<Void> {
        @Override
        public Void doWithGraph(GraphDatabaseService graph) throws Exception {
            doWithGraphWithoutResult(graph);
            return null;
        }
        public abstract void doWithGraphWithoutResult(GraphDatabaseService graph) throws Exception;
    }
}
