package org.springframework.data.graph.neo4j;

import org.springframework.data.graph.annotation.NodeEntity;

/**
 * @author mh
 * @since 20.01.11
 */
@NodeEntity
public class Named {
    public String name;

    public String getName() {
        return name;
    }
}
