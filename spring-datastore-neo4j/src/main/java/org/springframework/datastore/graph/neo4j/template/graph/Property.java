package org.springframework.datastore.graph.neo4j.template.graph;

public class Property
{
    private final String name;
    private final Object value;

    private Property(final String name, final Object value)
    {
        if (name == null)
            throw new IllegalArgumentException("Name must not be null");
        if (value == null)
            throw new IllegalArgumentException("Value must not be null");
        this.name = name;
        this.value = value;
    }

    public static Property _(String name, Object value)
    {
        return new Property(name, value);
    }

    public Object getValue()
    {
        return value;
    }

    public String getName()
    {
        return name;
    }
}
