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

package org.springframework.data.neo4j.mapping;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.core.RelationshipBacked;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.lang.reflect.Field;

public class RelationshipInfo {

    private boolean isMultiple;
    private final Direction direction;
    private final String type;
    private final TypeInformation<?> targetType;
    private final boolean targetsNodes;
    private boolean readonly;

    public Direction getDirection() {
        return direction;
    }

    public String getType() {
        return type;
    }
    public RelationshipType getRelationshipType() {
        return DynamicRelationshipType.withName(type);
    }

    public boolean isMultiple() {
        return isMultiple;
    }

    public RelationshipInfo(String type, Direction direction, TypeInformation<?> typeInformation, TypeInformation<?> concreteActualType, boolean targetsNode) {
        this.type = type;
        this.direction = direction;
        isMultiple = typeInformation.isCollectionLike();
        targetType = concreteActualType!=null ? concreteActualType : typeInformation.getActualType();
        targetsNodes = isNodeEntity(targetType);
        this.readonly = isMultiple() && typeInformation.getType().equals(Iterable.class);
    }

    private boolean isNodeEntity(TypeInformation<?> targetType) {
        final Class<?> type = targetType.getType();
        if (type.isAnnotationPresent(NodeEntity.class)) return true;
        if (type.isAnnotationPresent(RelationshipEntity.class)) return false;
        throw new MappingException("Target type for relationship " + this.type + " field is invalid " + type);
    }

    public static RelationshipInfo fromField(Field field, TypeInformation<?> typeInformation) {
        return new RelationshipInfo(field.getName(), Direction.OUTGOING, typeInformation,null,true);
    }

    public static RelationshipInfo fromField(Field field, RelatedTo annotation, TypeInformation<?> typeInformation) {
        return new RelationshipInfo(
                annotation.type().isEmpty() ? field.getName() : annotation.type(),
                annotation.direction(),
                typeInformation,
                annotation.elementClass() != NodeBacked.class ? ClassTypeInformation.from(annotation.elementClass()) : null,
                true);
    }

    public static RelationshipInfo fromField(Field field, RelatedToVia annotation, TypeInformation<?> typeInformation) {
        return new RelationshipInfo(
                annotation.type().isEmpty() ? field.getName() : annotation.type(),
                annotation.direction(),
                typeInformation,
                annotation.elementClass() != RelationshipBacked.class ? ClassTypeInformation.from(annotation.elementClass()) : null,
                false);
    }

    public TypeInformation<?> getTargetType() {
        return targetType;
    }

    public boolean targetsNodes() {
        return targetsNodes;
    }

    public boolean isReadonly() {
        return readonly;
    }
}
