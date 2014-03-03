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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

import java.util.Set;
import java.util.TreeSet;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

/**
 * @author Nicki Watt
 * @since 01.03.2014
 */
public class SchemaIndexingFieldAccessorFactory implements FieldAccessorFactory {
    private final Neo4jTemplate template;

    public SchemaIndexingFieldAccessorFactory(Neo4jTemplate template) {
        this.template = template;
    }

    @Override
	public boolean accept(final Neo4jPersistentProperty property) {
        return property.isIndexed() && property.getIndexInfo().isLabelBased();
	}

	@Override
	public FieldAccessor forField(final Neo4jPersistentProperty property) {
	    return new SchemaIndexedFieldAccessor(template,property);
	}

	public static class SchemaIndexedFieldAccessor extends PropertyFieldAccessorFactory.PropertyFieldAccessor {

        public SchemaIndexedFieldAccessor(Neo4jTemplate template,Neo4jPersistentProperty property) {
	        super(template,property);
        }

	    @Override
	    public boolean isWriteable(Object entity) {
	        return super.isWriteable(entity);
	    }

	    @Override
	    public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
            final PropertyContainer state = template.getPersistentState(entity);
            if (!(state instanceof Node)) {
                throw new IllegalArgumentException("not expecting to deal with non node property");
            }

            applyMissingSchemaIndexLabels(entity,(Node)state);
            checkForUniqueViolation(entity, newVal, (Node)state);
            return super.setValue(entity,newVal,mappingPolicy);
        }

        private void checkForUniqueViolation(Object entity,Object newVal, Node stateToBeSaved) {
            StoredEntityType set = template.getStoredEntityType(entity);
            if (newVal != null && property.isUnique()) {
                Object existingUniqueEntity = template.findUniqueEntity(set.getEntity().getType(),property.getNeo4jPropertyName(),newVal);
                if (existingUniqueEntity == null) return;
                final Node existingUniqueState = (Node)template.getPersistentState(existingUniqueEntity);
                if (existingUniqueState.equals(stateToBeSaved)) return;
                throw new DataIntegrityViolationException("Unique property "+property+" was to be set to duplicate value "+newVal);
            }
        }

        private void applyMissingSchemaIndexLabels(Object entity,Node state) {
            // TODO - This logic should rather be done once when the
            //        entity is persisted for the first time rather than
            //        on each update ....
            StoredEntityType set = template.getStoredEntityType(entity);
            if (set != null) {
                applyMissingSchemaIndexLabels(state, set);
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
