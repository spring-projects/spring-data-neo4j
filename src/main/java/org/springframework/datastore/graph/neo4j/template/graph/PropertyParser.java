package org.springframework.datastore.graph.neo4j.template.graph;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.springframework.datastore.graph.neo4j.template.util.Converter;

import static java.lang.String.format;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

public class PropertyParser
{
    private final Properties props;
    private final Converter converter = new Converter();

    public PropertyParser(final Properties props)
    {
        this.props = props;
    }

    public void load(final GraphDescription graph)
    {
        final SortedMap<Object, Object> sortedSet = new TreeMap<Object, Object>(props);
        for (Map.Entry<Object, Object> entry : sortedSet.entrySet())
        {
            if (entry.getKey() == null || entry.getValue() == null)
                throw new IllegalArgumentException(format("%s=%s is partially null%nproperties %s",
                        entry.getKey(), entry.getValue(), props));

            final String key = entry.getKey().toString();
            final String value = entry.getValue().toString();
            final String[] prop = key.split("\\.");
            if (prop.length == 2)
            {
                final String[] values = value.split(":");
                if (values.length != 2)
                    graph.add(prop[0], prop[1], value);
                else
                {
                    graph.add(prop[0], prop[1], converter.convert(values[0], values[1]));
                }
            } else
            {
                final String[] relationships = key.split("->");
                if (relationships.length == 2)
                {
                    graph.relate(relationships[0], DynamicRelationshipType.withName(relationships[1]), value);
                }
            }
        }
    }
}
