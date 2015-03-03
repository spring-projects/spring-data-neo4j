/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.metadata.info;

import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;
import org.neo4j.ogm.metadata.RelationshipUtils;
import org.neo4j.ogm.typeconversion.AttributeConverter;

public class FieldInfo {

    private static final String primitives = "I,J,S,B,C,F,D,Z,[I,[J,[S,[B,[C,[F,[D,[Z";

    private final String name;
    private final String descriptor;
    private final String typeParameterDescriptor;
    private final ObjectAnnotations annotations;

    private AttributeConverter<?, ?> converter;

    /**
     * Constructs a new {@link FieldInfo} based on the given arguments.
     *
     * @param name The name of the field
     * @param descriptor The field descriptor that expresses the type of the field using Java signature string notation
     * @param typeParameterDescriptor The descriptor that expresses the generic type parameter, which may be <code>null</code>
     *        if that's not appropriate
     * @param annotations The {@link ObjectAnnotations} applied to the field
     */
    public FieldInfo(String name, String descriptor, String typeParameterDescriptor, ObjectAnnotations annotations) {
        this.name = name;
        this.descriptor = descriptor;
        this.typeParameterDescriptor = typeParameterDescriptor;
        this.annotations = annotations;
        if (!this.annotations.isEmpty()) {
            setConverter(getAnnotatedTypeConverter());
        }
    }

    public String getName() {
        return name;
    }

    public boolean isTypeOf(Class<?> type) {
        String fieldSignature = "L" + type.getName().replace(".", "/") + ";";
        return descriptor.equals(fieldSignature);
    }

    // should these two methods be on PropertyReader, RelationshipReader respectively?
    public String property() {
        if (isSimple()) {
            try {
                return getAnnotations().get(Property.CLASS).get(Property.NAME, getName());
            } catch (NullPointerException npe) {
                return getName();
            }
        }
        return null;
    }

    public String relationship() {
        if (!isSimple()) {
            try {
                return getAnnotations().get(Relationship.CLASS).get(Relationship.TYPE, RelationshipUtils.inferRelationshipType(getName()));
            } catch (NullPointerException npe) {
                return RelationshipUtils.inferRelationshipType(getName());
            }
        }
        return null;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getTypeParameterDescriptor() {
        return typeParameterDescriptor;
    }

    public ObjectAnnotations getAnnotations() {
        return annotations;
    }

    public boolean isSimple() {
        return primitives.contains(descriptor)
                || converter != null
                || (descriptor.contains("java/lang/") && typeParameterDescriptor == null)
                || (typeParameterDescriptor != null && typeParameterDescriptor.contains("java/lang/"));
    }

    public AttributeConverter converter() {
        return converter;
    }

    public void setConverter( AttributeConverter<?, ?> converter ) {
        if (this.converter == null && converter != null) {
            this.converter = converter;
        } // we maybe set an annotated converter when object was constructed, so don't override with a default one
    }

    public boolean hasConverter() {
        return converter != null;
    }

    private AttributeConverter<?, ?> getAnnotatedTypeConverter() {
        if (typeParameterDescriptor == null) {
            return getAnnotations().getConverter(descriptor);
        } else {
            return getAnnotations().getConverter(typeParameterDescriptor);
        }
    }

    public String relationshipDirection() {
        if (relationship() != null) {
            AnnotationInfo annotationInfo = getAnnotations().get(Relationship.CLASS);
            if (annotationInfo == null) {
                return Relationship.OUTGOING;
            }
            return annotationInfo.get(Relationship.DIRECTION, Relationship.OUTGOING);
        }
        throw new RuntimeException("relationship direction call invalid");
    }
}
