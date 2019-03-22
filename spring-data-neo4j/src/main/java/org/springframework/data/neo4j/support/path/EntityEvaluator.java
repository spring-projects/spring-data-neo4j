/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.support.path;

import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.mapping.EntityPersister;
import org.springframework.data.neo4j.support.Neo4jTemplate;

/**
 * @author mh
 * @since 26.02.11
 */
public abstract class EntityEvaluator<S, E> implements Evaluator {

    private final Neo4jTemplate template;

    protected EntityEvaluator(Neo4jTemplate template) {
        this.template = template;
    }

    @Override
    public Evaluation evaluate(Path path) {
        return evaluate(new ConvertingEntityPath<S,E>(path, template));
    }
    public abstract Evaluation evaluate(EntityPath<S,E> path);

}
