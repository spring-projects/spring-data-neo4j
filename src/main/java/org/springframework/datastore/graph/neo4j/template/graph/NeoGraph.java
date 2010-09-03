package org.springframework.datastore.graph.neo4j.template.graph;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Traverser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.datastore.graph.neo4j.template.Graph;
import org.springframework.datastore.graph.neo4j.template.traversal.Traversal;

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

