/**
 * Copyright 2014 the original author or authors.
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
import org.neo4j.graphdb.schema.IndexDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

import java.util.Arrays;


public class SchemaIndexingPropertyFieldAccessorListenerFactory<S extends PropertyContainer, T> implements FieldAccessorListenerFactory {

    private final PropertyFieldAccessorFactory propertyFieldAccessorFactory;
    private final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory;
    private final Neo4jTemplate template;

    public SchemaIndexingPropertyFieldAccessorListenerFactory(final Neo4jTemplate template, final PropertyFieldAccessorFactory propertyFieldAccessorFactory, final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory) {
        this.template = template;
    	this.propertyFieldAccessorFactory = propertyFieldAccessorFactory;
        this.convertingNodePropertyFieldAccessorFactory = convertingNodePropertyFieldAccessorFactory;
    }

    @Override
    public boolean accept(final Neo4jPersistentProperty property) {
        return isPropertyField(property) && property.isIndexed() && property.getIndexInfo().isLabelBased();
    }


    private boolean isPropertyField(final Neo4jPersistentProperty property) {
        return propertyFieldAccessorFactory.accept(property) || convertingNodePropertyFieldAccessorFactory.accept(property);
    }

    @Override
    public FieldAccessListener forField(Neo4jPersistentProperty property) {
        return new SchemaIndexingPropertyFieldAccessorListener(property, template);
    }


    /**
	 * @author Nicki Watt
	 * @since 09.02.2014
	 */
	public static class SchemaIndexingPropertyFieldAccessorListener<T extends PropertyContainer> implements FieldAccessListener {

	    private final static Logger log = LoggerFactory.getLogger(SchemaIndexingPropertyFieldAccessorListener.class);

        private final Neo4jPersistentProperty property;
        private final Neo4jTemplate template;

        public SchemaIndexingPropertyFieldAccessorListener(final Neo4jPersistentProperty property, Neo4jTemplate template) {
            this.property = property;
            this.template = template;
        }

	    @Override
        public void valueChanged(Object entity, Object oldVal, Object newVal) {
            // TODO - This logic should rather be done once when the
            //        entity is persisted for the first time rather than
            //        on each update ....
            final PropertyContainer state = template.getPersistentState(entity);
            if (state instanceof Node) {
                Node node = (Node) state;
                StoredEntityType set = template.getStoredEntityType(entity);
                if (set != null) {
                    applyMissingSchemaIndexLabels(node, set);
                }
            }
        }

        private void applyMissingSchemaIndexLabels(Node node, StoredEntityType set) {
            for (StoredEntityType ancestorSet : set.getSuperTypes()) {
                applyMissingSchemaIndexLabels(node, ancestorSet);
            }
            Label label = DynamicLabel.label( (String)set.getAlias());
            if (!node.hasLabel(label))
                node.addLabel(label);
        }

    }
}
