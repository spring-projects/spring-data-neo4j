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

package org.springframework.data.neo4j.fieldaccess;


import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.annotation.Labels;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.Set;
import java.util.TreeSet;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class LabelFieldAccessorFactory implements FieldAccessorFactory {
    private final Neo4jTemplate template;

    public LabelFieldAccessorFactory(Neo4jTemplate template) {
        this.template = template;
    }

    @Override
    public boolean accept(final Neo4jPersistentProperty property) {
        return property.isAnnotationPresent(Labels.class);
    }

    @Override
    public FieldAccessor forField(final Neo4jPersistentProperty property) {
        return new LabelFieldAccessor(property, template);
    }

    public static class LabelFieldAccessor implements FieldAccessor {
        protected final Neo4jPersistentProperty property;
        private final Neo4jTemplate template;

        public LabelFieldAccessor(final Neo4jPersistentProperty property, Neo4jTemplate template) {
            this.property = property;
            this.template = template;
        }

        @Override
        public boolean isWriteable(Object entity) {
            return true;
        }

        @Override
        public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
            if (entity==null) return entity;
            final PropertyContainer state = template.getPersistentState(entity);
            if (state instanceof Node) {
                Node node = (Node) state;
                Set<String> oldLabels = getLabels(node);
                for (String newLabel : (Iterable<String>) newVal) {
                    if (oldLabels.remove(newLabel)) continue;
                    node.addLabel(DynamicLabel.label(newLabel));
                }
                for (String removedLabels : oldLabels) {
                    node.removeLabel(DynamicLabel.label(removedLabels));
                }
                return doReturn(newVal);
            }
            throw new MappingException("Error setting labels on "+entity);
        }

        @Override
        public Object getValue(final Object entity, MappingPolicy mappingPolicy) {
            final PropertyContainer state = template.getPersistentState(entity);
            if (state instanceof Node) {
                return doReturn(getLabels((Node) state));
            }
            throw new MappingException("Error retrieving labels from "+entity);
        }

        private Set<String> getLabels(Node state) {
            Set<String> labels = new TreeSet<>();
            for (Label label : state.getLabels()) {
                labels.add(label.name());
            }
            return labels;
        }

        @Override
        public Object getDefaultValue() {
            return null;
        }

    }
}
