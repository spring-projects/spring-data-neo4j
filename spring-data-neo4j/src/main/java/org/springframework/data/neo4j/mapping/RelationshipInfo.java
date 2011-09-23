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

import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.core.Direction;
import org.springframework.data.util.TypeInformation;
import scala.annotation.target.field;

import java.lang.reflect.Field;

public class RelationshipInfo {

    private boolean isMultiple;
    private final Direction direction;
    private final String type;
    private final TypeInformation<?> targetType;
    private final boolean isNodeRelationship;

    public Direction getDirection() {
        return direction;
    }

    public String getType() {
        return type;
    }
    public boolean isMultiple() {
        return isMultiple;
    }

    public RelationshipInfo(String type, Direction direction, TypeInformation<?> typeInformation) {
        this.type = type;
        this.direction = direction;
        isMultiple = typeInformation.isCollectionLike();
        targetType = typeInformation.getActualType();
        isNodeRelationship = isNodeEntity(targetType);
    }

    private boolean isNodeEntity(TypeInformation<?> targetType) {
        final Class<?> type = targetType.getType();
        if (type.isAnnotationPresent(NodeEntity.class)) return true;
        if (type.isAnnotationPresent(RelationshipEntity.class)) return false;
        throw new MappingException("Target type for relationship "+ this.type +" field is invalid "+type);
    }

    public static RelationshipInfo fromField(Field field, TypeInformation<?> typeInformation) {
        return new RelationshipInfo(field.getName(), Direction.OUTGOING, typeInformation);
    }
    public static RelationshipInfo fromField(Field field, RelatedTo annotation, TypeInformation<?> typeInformation) {
        return new RelationshipInfo(
                annotation.type().isEmpty() ? field.getName() : annotation.type(),
                annotation.direction(),
                typeInformation);
    }
    public static RelationshipInfo fromField(Field field, RelatedToVia annotation, TypeInformation<?> typeInformation) {
        return new RelationshipInfo(
                annotation.type().isEmpty() ? field.getName() : annotation.type(),
                annotation.direction(),
                typeInformation);
    }
}
