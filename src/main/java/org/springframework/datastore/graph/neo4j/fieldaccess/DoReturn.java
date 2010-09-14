package org.springframework.datastore.graph.neo4j.fieldaccess;

/**
 * @author Michael Hunger
 * @since 15.09.2010
 */
public class DoReturn<T> {
    public final T value;

    public DoReturn(T value) {
        this.value = value;
    }
    public static <T> DoReturn<T> doReturn(T value) {
        return new DoReturn<T>(value);
    }
}
