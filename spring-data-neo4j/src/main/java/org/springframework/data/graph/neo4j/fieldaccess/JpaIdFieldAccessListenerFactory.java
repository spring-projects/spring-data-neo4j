/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.springframework.data.graph.core.NodeBacked;

import javax.persistence.Id;
import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class JpaIdFieldAccessListenerFactory implements FieldAccessorListenerFactory<NodeBacked> {
    @Override
    public boolean accept(final Field f) {
        return f.isAnnotationPresent(Id.class);
    }

    @Override
    public FieldAccessListener<NodeBacked, ?> forField(final Field field) {
        return new JpaIdFieldListener(field);
    }

    public static class JpaIdFieldListener implements FieldAccessListener<NodeBacked, Object> {
        protected final Field field;

        public JpaIdFieldListener(final Field field) {
            this.field = field;
        }

        @Override
        public void valueChanged(NodeBacked nodeBacked, Object oldVal, Object newVal) {
            if (newVal != null) {
                EntityStateAccessors stateAccessors=nodeBacked.getStateAccessors();
                stateAccessors.createAndAssignState();
            }
        }
    }
}
