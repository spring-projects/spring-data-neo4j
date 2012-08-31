/**
 * Copyright 2011 the original author or authors.
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
package org.springframework.data.neo4j.lifecycle;

import org.springframework.context.ApplicationEvent;

public class BeforeSaveEvent<T> extends ApplicationEvent {
    private final T entity;

    /**
     * @param source the component that published the event (never <code>null</code>)
     * @param entity the entity that is about to be saved
     */
    public BeforeSaveEvent(Object source, T entity) {
        super(source);

        this.entity = entity;
    }

    public T getEntity() {
        return entity;
    }
}
