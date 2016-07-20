/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;

/**
 * A Spring {@link ApplicationEvent} that gets published by {@link Neo4jTemplate} to notify interested parties about data
 * manipulation events.  In previous versions of Spring Data Neo4j this was known as <code>Neo4jLifecycleEvent</code> but
 * has been renamed to better describe the nature of the events.
 *
 * @author Adam George
 * @deprecated Now automatically handled in {@link Neo4jTransactionManager}.
 */
@Deprecated
public class Neo4jDataManipulationEvent extends ApplicationEvent {

    private static final long serialVersionUID = -9025087608146228149L;

    private Object entity;

    public Neo4jDataManipulationEvent(Object source, Object entity) {
        super(source);
        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }

}
