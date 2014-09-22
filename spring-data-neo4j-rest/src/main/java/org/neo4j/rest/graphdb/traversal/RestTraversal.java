/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb.traversal;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.Predicate;
import org.neo4j.kernel.Traversal;
import org.neo4j.kernel.Uniqueness;
import org.neo4j.rest.graphdb.entity.RestNode;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author Michael Hunger
 * @since 02.02.11
 */
public class RestTraversal implements RestTraversalDescription {

    private final Map<String, Object> description=new HashMap<String, Object>();

    public RestTraversal() {
    }

    @Override
    public String toString() {
        return description.toString();
    }

    public RestTraversalDescription uniqueness(UniquenessFactory uniquenessFactory) {
        return uniqueness(uniquenessFactory,null);
    }

    public RestTraversalDescription uniqueness(UniquenessFactory uniquenessFactory, Object value) {
        String uniqueness = restify(uniquenessFactory);
        add("uniqueness",value==null ? uniqueness : toMap("name",uniqueness, "value", value));
        return null;
    }

    private String restify(UniquenessFactory uniquenessFactory) {
        if (uniquenessFactory instanceof Uniqueness) {
            return ((Uniqueness)uniquenessFactory).name().toLowerCase().replace("_"," ");
        }
        throw new UnsupportedOperationException("Only values of "+Uniqueness.class+" are supported");
    }

    public RestTraversalDescription prune(PruneEvaluator pruneEvaluator) {
    	if (pruneEvaluator == PruneEvaluator.NONE) {
              return add( "prune_evaluator", toMap( "language", "builtin", "name", "none" ) );
        }
        Integer maxDepth= getMaxDepthValueOrNull(pruneEvaluator);
        if (maxDepth!=null) {
            return maxDepth(maxDepth);
        }
        throw new UnsupportedOperationException("Only max depth supported");
    }

    private Integer getMaxDepthValueOrNull(PruneEvaluator pruneEvaluator) {
        try {
            final Field depthField = pruneEvaluator.getClass().getDeclaredField("val$depth");
            depthField.setAccessible(true);
            return (Integer) depthField.get(pruneEvaluator);
        } catch (Exception e) {
            return null;
        }
    }

    public RestTraversalDescription filter(Predicate<Path> pathPredicate) {
        if (pathPredicate == Evaluators.all()) return add("return_filter",toMap("language","builtin", "name","all"));
        if (pathPredicate == Evaluators.excludeStartPosition()) return add("return_filter",toMap("language","builtin", "name","all_but_start_node"));
        throw new UnsupportedOperationException("Only builtin paths supported");
    }

    public RestTraversalDescription evaluator(PathEvaluator evaluator) {
        if (evaluator == Evaluators.all()) return add("return_filter",toMap("language","builtin", "name","all"));
        if (evaluator == Evaluators.excludeStartPosition()) return add("return_filter",toMap("language","builtin", "name","all_but_start_node"));
        throw new UnsupportedOperationException("Only builtin paths supported");
    }

    public RestTraversalDescription evaluator(Evaluator evaluator) {
        if (evaluator == Evaluators.all()) return add("return_filter",toMap("language","builtin", "name","all"));
        if (evaluator == Evaluators.excludeStartPosition()) return add("return_filter",toMap("language","builtin", "name","all_but_start_node"));
        throw new UnsupportedOperationException("Only builtin paths supported");
    }

    public RestTraversalDescription prune(ScriptLanguage language, String code) {
        return add("prune_evaluator",toMap("language",language.name().toLowerCase(),"body",code ));
    }

    public RestTraversalDescription filter(ScriptLanguage language, String code) {
        return add("return_filter",toMap("language",language.name().toLowerCase(),"body",code ));
    }

    public RestTraversalDescription maxDepth(int depth) {
        return add("max_depth", depth);
    }

    public RestTraversalDescription order(BranchOrderingPolicy branchOrderingPolicy) {
        throw new UnsupportedOperationException();
    }

    public RestTraversalDescription depthFirst() {
        return add("order","depth_first");
    }

    public RestTraversalDescription breadthFirst() {
        return add("order", "breadth_first");
    }

    private RestTraversalDescription add(String key, Object value) {
        description.put(key,value);
        return this;
    }

    public RestTraversalDescription relationships(RelationshipType relationshipType) {
        return relationships(relationshipType, null);
    }

    public RestTraversalDescription relationships(RelationshipType relationshipType, Direction direction) {
        if (!description.containsKey("relationships")) {
            description.put("relationships",new HashSet<Map<String,Object>>());
        }
        Set<Map<String,Object>> relationships= (Set<Map<String, Object>>) description.get("relationships");
        relationships.add(toMap("type", relationshipType.name(), "direction", directionString(direction)));
        return this;
    }

    private Map<String, Object> toMap(Object...params) {
        if (params.length % 2 != 0) throw new IllegalArgumentException("toMap needs an even number of arguments, but was "+Arrays.toString(params));

        Map<String, Object> result = new HashMap<String, Object>();
        for (int i = 0; i < params.length; i+=2) {
            if (params[i+1] == null) continue;
            result.put(params[i].toString(), params[i + 1].toString());
        }
        return result;
    }

    private String directionString(Direction direction) {
        return RestDirection.from(direction).shortName;
    }

    public RestTraversalDescription expand(RelationshipExpander relationshipExpander) {
        throw new UnsupportedOperationException();
    }

    public Traverser traverse(Node node) {
        final RestNode restNode = (RestNode) node;
        return restNode.getRestApi().traverse(restNode, description);
    }

    public static RestTraversalDescription description() {
        return new RestTraversal();
    }

    public Map<String,Object> getPostData() {
        return description;
    }

    @Override
    public TraversalDescription expand(PathExpander<?> expander) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <STATE> TraversalDescription expand(PathExpander<STATE> expander, InitialStateFactory<STATE> initialState) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <STATE> TraversalDescription expand(PathExpander<STATE> expander, InitialBranchState<STATE> initialState) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TraversalDescription sort(Comparator<? super Path> comparator) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TraversalDescription reverse() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Traverser traverse(Node... startNode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Traverser traverse(Iterable<Node> nodes) {
        throw new UnsupportedOperationException();
    }
}
