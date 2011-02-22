package org.springframework.data.graph.neo4j.template;

/**
* @author mh
* @since 22.02.11
*/
public interface IterationController {
    public enum IterationMode {
        LAZY, EAGER, EAGER_STOP_ON_NULL
    }
    IterationMode getIterationMode();
}
