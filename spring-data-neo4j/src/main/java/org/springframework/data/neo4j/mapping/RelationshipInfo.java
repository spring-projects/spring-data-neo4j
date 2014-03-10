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
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.lang.reflect.Field;

public class RelationshipInfo {

    private boolean isCollection;
    private final Direction direction;
    private final String type;
    private final TypeInformation<?> targetType;
    private final boolean relatedTo;
    private boolean readonly;
    private Neo4jPersistentEntity targetEntity;

    public Direction getDirection() {
        return direction;
    }

    public String getType() {
        return type;
    }
    public RelationshipType getRelationshipType() {
        return DynamicRelationshipType.withName(type);
    }

    public boolean isCollection() {
        return isCollection;
    }
    public boolean isSingle() {
        return !isCollection;
    }

    public RelationshipInfo(String type, Direction direction, TypeInformation<?> typeInformation, TypeInformation<?> concreteActualType, Neo4jMappingContext ctx) {
        this.type = type;
        this.direction = direction;
        isCollection = typeInformation.isCollectionLike();
        targetType = concreteActualType!=null ? concreteActualType : typeInformation.getActualType();
        this.targetEntity = ctx.getPersistentEntity(targetType);
        relatedTo = targetEntity.isNodeEntity();
        this.readonly = isCollection() && typeInformation.getType().equals(Iterable.class);
    }

    public static RelationshipInfo fromField(String name, TypeInformation<?> typeInformation, Neo4jMappingContext ctx) {
        return new RelationshipInfo(name, Direction.OUTGOING, typeInformation,null, ctx);
    }

    public static RelationshipInfo fromField(String name, RelatedTo annotation, TypeInformation<?> typeInformation, Neo4jMappingContext ctx) {
        RelationshipInfo relationshipInfo = new RelationshipInfo(
                annotation.type().isEmpty() ? name : annotation.type(),
                annotation.direction(),
                typeInformation,
                annotation.elementClass() != Object.class ? ClassTypeInformation.from(annotation.elementClass()) : null,
                ctx
        );
        if (relationshipInfo.isRelatedToVia()) {
            throw new MappingException("Relationship field with NodeEntity "+relationshipInfo.getTargetEntity().getType()+" annotated with @RelatedTo");
        }
        return relationshipInfo;
    }

    public static RelationshipInfo fromField(String name, RelatedToVia annotation, TypeInformation<?> typeInformation, Neo4jMappingContext ctx) {
        final TypeInformation<?> elementClass = elementClass(annotation, typeInformation);
        RelationshipInfo relationshipInfo = new RelationshipInfo(
                relationshipType(annotation, typeInformation),
                annotation.direction(),
                typeInformation,
                elementClass,
                ctx
        );
        if (relationshipInfo.isRelatedTo()) throw new MappingException("Relationship field with RelationshipEntity "+relationshipInfo.getTargetEntity().getType()+" annotated with @RelatedToVia");
        return relationshipInfo;
    }

    private static String relationshipType(RelatedToVia annotation, TypeInformation<?> typeInformation) {
        if (!annotation.type().isEmpty()) return annotation.type();
        final TypeInformation<?> relationshipEntityType = elementClass(annotation, typeInformation);
        final RelationshipEntity relationshipEntity = relationshipEntityType.getType().getAnnotation(RelationshipEntity.class);
        if (relationshipEntity==null) {
            throw new MappingException(typeInformation.getType()+" is no RelationshipEntity");
        }
        if (relationshipEntity.type()==null || relationshipEntity.type().isEmpty()) {
            throw new MappingException("Relationship entity must have a default type");
        }
        return relationshipEntity.type();
    }

    private static TypeInformation<?> elementClass(RelatedToVia annotation, TypeInformation<?> typeInformation) {
        return annotation.elementClass() != Object.class ? ClassTypeInformation.from(annotation.elementClass()) : typeInformation.getActualType();
    }

    public TypeInformation<?> getTargetType() {
        return targetType;
    }

    public boolean isRelatedTo() {
        return relatedTo;
    }
    public boolean isRelatedToVia() {
        return !isRelatedTo();
    }

    public boolean isReadonly() {
        return readonly;
    }

    public Neo4jPersistentEntity getTargetEntity() {
        return targetEntity;
    }
}
