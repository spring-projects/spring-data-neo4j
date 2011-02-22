package org.springframework.data.graph.neo4j.template;

/**
 * If the @{see PathMapper} implements this interface the returned @{see IterationMode}
 * is considered for the path mapping operation.
 * @author mh
 * @since 22.02.11
 */
public interface IterationController {
    public enum IterationMode {
        /**
         * doesn't evaluate before the iterable is accessed
         */
        LAZY,
        /**
         * iterates eagerly over the results to ensure callbacks happening and also returns the full results
         */
        EAGER,
        /**
         * iterates eagerly over the results to ensure callbacks happening, stops iteration on first encountered
         * null value, useful for aborting iterations early
         */
        EAGER_STOP_ON_NULL,
        /**
         * iterates eagerly over the results to ensure callbacks happening, ignores results and returns null from
         * the iteration, i.e. callback only
         */
        EAGER_IGNORE_RESULTS
    }

    IterationMode getIterationMode();
}
