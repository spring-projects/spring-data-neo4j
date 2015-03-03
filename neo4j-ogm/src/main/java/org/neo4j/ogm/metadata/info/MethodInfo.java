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

public class MethodInfo {

    private static final String primitiveGetters="()I,()J,()S,()B,()C,()F,()D,()Z,()[I,()[J,()[S,()[B,()[C,()[F,()[D,()[Z";
    private static final String primitiveSetters="(I)V,(J)V,(S)V,(B)V,(C)V,(F)V,(D)V,(Z)V,([I)V,([J)V,([S)V,([B)V,([C)V,([F)V,([D)V,([Z)V";

    private final String name;
    private final String descriptor;
    private final ObjectAnnotations annotations;
    private final String typeParameterDescriptor;

    private AttributeConverter<?, ?> converter;

    /**
     * Constructs a new {@link MethodInfo} based on the given arguments.
     *
     * @param name The name of the method
     * @param descriptor The method descriptor that expresses the parameters and return type using Java signature string
     *        notation
     * @param typeParameterDescriptor If the method parameter or return type is parameterised, this is the descriptor that
     *        expresses its generic type, or <code>null</code> if that's not appropriate
     * @param annotations The {@link ObjectAnnotations} applied to the field
     */
    public MethodInfo(String name, String descriptor, String typeParameterDescriptor, ObjectAnnotations annotations) {
        this.name = name;
        this.descriptor = descriptor;
        this.typeParameterDescriptor = typeParameterDescriptor;
        this.annotations = annotations;
        if (!this.getAnnotations().isEmpty()) {
            setConverter(getAnnotatedTypeConverter());
        }
    }

    public String getName() {
        return name;
    }

    public String property() {
       if (isSimpleSetter() || isSimpleGetter()) {
            try {
                return getAnnotations().get(Property.CLASS).get(Property.NAME, getName());
            } catch (NullPointerException npe) {
                if (name.startsWith("get") || name.startsWith("set")) {
                    StringBuilder sb = new StringBuilder(name.substring(3));
                    sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
                    return sb.toString();
                }
                return getName();
            }
        }
        return null;
    }

    public String relationship() {
        if (!isSimpleSetter() && !isSimpleGetter()) {
            try {
                return getAnnotations().get(Relationship.CLASS).get(Relationship.TYPE, RelationshipUtils.inferRelationshipType(getName()));
            } catch (NullPointerException npe) {
                // TODO: consider whether to check parameter/return type here for an @RelationshipEntity annotation
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

    public boolean isSimpleGetter() {
        return primitiveGetters.contains(descriptor)
                || hasConverter()
                || usesSimpleJavaTypes();
    }

    public boolean isSimpleSetter() {
        return primitiveSetters.contains(descriptor)
                || hasConverter()
                || usesSimpleJavaTypes();
    }

    private boolean usesSimpleJavaTypes() {
        return (descriptor.contains("java/lang/") && typeParameterDescriptor == null)
                || (typeParameterDescriptor != null && typeParameterDescriptor.contains("java/lang/"));
    }

    public AttributeConverter converter() {
        return converter;
    }

    public boolean hasConverter() {
        return converter != null;
    }

    private AttributeConverter getAnnotatedTypeConverter() {
        if (typeParameterDescriptor == null) {
            return getAnnotations().getConverter(descriptor);
        } else {
            return getAnnotations().getConverter(typeParameterDescriptor);
        }
    }

    public void setConverter(AttributeConverter<?, ?> converter) {
        if (this.converter == null && converter != null) {
            this.converter = converter;
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
