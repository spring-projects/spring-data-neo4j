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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Traverser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.graph.neo4j.template.traversal.Traversal;

import java.util.ArrayList;
import java.util.List;

public class NeoGraph implements Graph
{
    private final GraphDatabaseService neo;

    public NeoGraph(final GraphDatabaseService neo)
    {
        if (neo == null)
            throw new IllegalArgumentException("NeoService must not be null");
        this.neo = neo;
    }

    @Override
    public Node createNode(final Property... params)
    {
        final Node node = neo.createNode();
        if (params == null || params.length == 0) return node;
        for (final Property property : params)
        {
            node.setProperty(property.getName(), property.getValue());
        }
        return node;
    }

    @Override
    public Node getReferenceNode()
    {
        return neo.getReferenceNode();
    }

    @Override
    public Node getNodeById(final long id)
    {
        return neo.getNodeById(id);
    }

    @Override
    public Traverser traverse(final Traversal traversal)
    {
        return traverse(neo.getReferenceNode(), traversal);
    }

    @Override
    public Traverser traverse(final Node startNode, final Traversal traversal)
    {
        if (startNode == null)
            throw new IllegalArgumentException("StartNode must not be null");
        if (traversal == null)
            throw new IllegalArgumentException("Traversal must not be null");
        return traversal.from(startNode);
    }

    @Override
    public <T> List<T> traverse(final Node startNode, final Traversal traversal, final Converter<Node, T> converter)
    {
        if (converter == null)
            throw new IllegalArgumentException("Converter must not be null");

        return map(converter, traverse(startNode, traversal));
    }

    private <T> List<T> map(final Converter<Node, T> mapper, final Iterable<Node> nodes)
    {
        final List<T> result = new ArrayList<T>();
        for (final Node node : nodes)
        {
            result.add(mapper.convert(node));
        }
        return result;
    }

    @Override
    public void load(final GraphDescription description)
    {
        if (description == null)
            throw new IllegalArgumentException("GraphDescription must not be null");
        description.addToGraph(this);
    }
}

