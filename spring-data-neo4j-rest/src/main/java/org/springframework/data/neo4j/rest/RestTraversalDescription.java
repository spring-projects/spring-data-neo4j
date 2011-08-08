/**
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.neo4j.rest;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.Predicate;

/**
 * @author Michael Hunger
 * @since 03.02.11
 */
public interface RestTraversalDescription extends TraversalDescription {
    RestTraversalDescription prune(ScriptLanguage language, String code);

    RestTraversalDescription filter(ScriptLanguage language, String code);

    RestTraversalDescription maxDepth(int depth);

    @Override
    RestTraversalDescription uniqueness(UniquenessFactory uniquenessFactory);

    @Override
    RestTraversalDescription uniqueness(UniquenessFactory uniquenessFactory, Object o);

    @Override
    RestTraversalDescription prune(PruneEvaluator pruneEvaluator);

    @Override
    RestTraversalDescription filter(Predicate<Path> pathPredicate);

    @Override
    RestTraversalDescription evaluator(Evaluator evaluator);

    @Override
    RestTraversalDescription order(BranchOrderingPolicy branchOrderingPolicy);

    @Override
    RestTraversalDescription depthFirst();

    @Override
    RestTraversalDescription breadthFirst();

    @Override
    RestTraversalDescription relationships(RelationshipType relationshipType);

    @Override
    RestTraversalDescription relationships(RelationshipType relationshipType, Direction direction);

    @Override
    RestTraversalDescription expand(RelationshipExpander relationshipExpander);

    @Override
    Traverser traverse(Node node);

    public enum ScriptLanguage {
        JAVASCRIPT;
    }
}
