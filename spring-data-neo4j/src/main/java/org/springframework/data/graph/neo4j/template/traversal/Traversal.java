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

package org.springframework.data.graph.neo4j.template.traversal;

import org.neo4j.graphdb.*;
import org.springframework.data.graph.neo4j.template.util.NodeEvaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class Traversal
{
    private final Collection<RelationShipTypeDirection> relationShipTypeDirections = new HashSet<RelationShipTypeDirection>();

    private Traverser.Order order = Traverser.Order.BREADTH_FIRST;
    private StopEvaluator stopEvaluator = StopEvaluator.END_OF_GRAPH;
    private ReturnableEvaluator returnableEvaluator = ReturnableEvaluator.ALL_BUT_START_NODE;

    private Traversal()
    {
    }

    Traverser.Order getOrder()
    {
        return order;
    }

    StopEvaluator getStopEvaluator()
    {
        return stopEvaluator;
    }

    ReturnableEvaluator getReturnableEvaluator()
    {
        return returnableEvaluator;
    }

    public static Traversal walk()
    {
        return new Traversal();
    }

    public Traversal order(Traverser.Order order)
    {
        if (order == null)
            throw new IllegalArgumentException("Order must not be null");
        this.order = order;
        return this;
    }

    public Traversal accept(ReturnableEvaluator evaluator)
    {
        if (evaluator == null)
            throw new IllegalArgumentException("Evaluator must not be null");
        this.returnableEvaluator = evaluator;
        return this;
    }

    public Traversal accept(final NodeEvaluator evaluator)
    {
        if (evaluator == null)
            throw new IllegalArgumentException("Evaluator must not be null");
        this.returnableEvaluator = new ReturnableEvaluator()
        {
            public boolean isReturnableNode(final TraversalPosition traversalPosition)
            {
                return evaluator.accept(traversalPosition.currentNode());
            }
        };
        return this;
    }

    public Traversal stopOn(StopEvaluator stop)
    {
        if (stop == null)
            throw new IllegalArgumentException("StopEvaluator must not be null");
        this.stopEvaluator = stop;
        return this;
    }

    public Traversal breadthFirst()
    {
        return order(Traverser.Order.BREADTH_FIRST);
    }

    public Traversal depthFirst()
    {
        return order(Traverser.Order.DEPTH_FIRST);
    }

    public Traversal first()
    {
        return stopOn(StopEvaluator.DEPTH_ONE);
    }

    public Traversal all()
    {
        return stopOn(StopEvaluator.END_OF_GRAPH);
    }

    public Traversal incoming(final RelationshipType relationShipType)
    {
        if (relationShipType == null)
            throw new IllegalArgumentException("RelationShipType must not be null");
        relationShipTypeDirections.add(RelationShipTypeDirection.incoming(relationShipType));
        return this;
    }

    public Traversal outgoing(final RelationshipType relationShipType)
    {
        if (relationShipType == null)
            throw new IllegalArgumentException("RelationShipType must not be null");
        relationShipTypeDirections.add(RelationShipTypeDirection.outgoing(relationShipType));
        return this;
    }

    public Traversal twoway(final RelationshipType relationShipType)
    {
        if (relationShipType == null)
            throw new IllegalArgumentException("RelationShipType must not be null");
        relationShipTypeDirections.add(RelationShipTypeDirection.twoway(relationShipType));
        return this;
    }

    public Traversal both(final RelationshipType relationShipType)
    {
        return twoway(relationShipType);
    }

    public Traverser from(final Node node)
    {
        if (node == null)
            throw new IllegalArgumentException("Node must not be null");
        return node.traverse(getOrder(), getStopEvaluator(), getReturnableEvaluator(),
                getRelationshipTypeDirectionPairs());
    }

    private Object[] getRelationshipTypeDirectionPairs()
    {
        Collection<Object> result = new ArrayList<Object>(relationShipTypeDirections.size());
        for (RelationShipTypeDirection relationShipTypeDirection : relationShipTypeDirections)
        {
            result.addAll(relationShipTypeDirection.asList());
        }
        return result.toArray();
    }
}
