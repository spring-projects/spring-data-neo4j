package org.neo4j.graphdb;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

public interface Result extends ResourceIterator<Map<String, Object>>
{
    /**
     * The exact names used to represent each column in the result set.
     *
     * @return List of the column names.
     */
    List<String> columns();

    /**
     * Returns an iterator with the result objects from a single column of the result set. This method is best used for
     * single column results.
     *
     * <p><b>To ensure that any resources, including transactions bound to it, are properly closed, the iterator must
     * either be fully exhausted, or the {@link ResourceIterator#close() close()} method must be
     * called.</b></p>
     *
     * @param name exact name of the column, as it appeared in the original query
     * @param <T>  desired type cast for the result objects
     * @return an iterator of the result objects, possibly empty
     * @throws ClassCastException                  when the result object can not be cast to the requested type
     * @throws NotFoundException when the column name does not appear in the original query
     */
    <T> ResourceIterator<T> columnAs(String name);

    /**
     * Denotes there being more rows available in this result. These rows must either be consumed, by invoking
     * {@link #next()}, or the result has to be {@link #close() closed}.
     *
     * @return {@code true} if there is more rows available in this result, {@code false} otherwise.
     */
    boolean hasNext();

    /**
     * Returns the next row in this result.
     *
     * @return the next row in this result.
     */
    Map<String, Object> next();

    /**
     * Closes the result, freeing up any resources held by the result.
     *
     * This is an idempotent operation, invoking it multiple times has the same effect as invoking it exactly once.
     * It is thus safe (and even encouraged, for style and simplicity) to invoke this method even after consuming all
     * rows in the result through the {@link #next() next-method}.
     */
    void close();

    /**
     * Provides a textual representation of the query result.
     * <p><b>
     * The execution result represented by this object will be consumed in its entirety after this method is called.
     * Calling any of the other iterating methods on it should not be expected to return any results.
     * </b></p>
     *
     * @return the execution result formatted as a string
     */
    String resultAsString();

    /**
     * Provides a textual representation of the query result to the provided {@link PrintWriter}.
     * <p><b>
     * The execution result represented by this object will be consumed in its entirety after this method is called.
     * Calling any of the other iterating methods on it should not be expected to return any results.
     * </b></p>
     * @param writer the {@link PrintWriter} to receive the textual representation of the query result.
     */
    void writeAsStringTo(PrintWriter writer);

    /** Removing rows from the result is not supported. */
    void remove();
}
