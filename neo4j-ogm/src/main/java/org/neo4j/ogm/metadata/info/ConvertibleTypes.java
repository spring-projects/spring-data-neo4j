/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.metadata.info;

import org.neo4j.ogm.annotation.typeconversion.DateString;
import org.neo4j.ogm.typeconversion.AttributeConverter;
import org.neo4j.ogm.typeconversion.ByteArrayBase64Converter;
import org.neo4j.ogm.typeconversion.ByteArrayWrapperBase64Converter;
import org.neo4j.ogm.typeconversion.DateArrayStringConverter;
import org.neo4j.ogm.typeconversion.DateCollectionStringConverter;
import org.neo4j.ogm.typeconversion.DateStringConverter;
import org.neo4j.ogm.typeconversion.EnumArrayStringConverter;
import org.neo4j.ogm.typeconversion.EnumCollectionStringConverter;
import org.neo4j.ogm.typeconversion.EnumStringConverter;
import org.neo4j.ogm.typeconversion.NumberArrayStringConverter;
import org.neo4j.ogm.typeconversion.NumberCollectionStringConverter;
import org.neo4j.ogm.typeconversion.NumberStringConverter;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public abstract class ConvertibleTypes {

    public static AttributeConverter<?, ?> getDateConverter() {
        return new DateStringConverter(DateString.ISO_8601);
    }

    public static AttributeConverter<?, ?> getDateArrayConverter() {
        return new DateArrayStringConverter(DateString.ISO_8601);
    }

    public static AttributeConverter<?, ?> getDateCollectionConverter(String collectionType) {
        try {
            Class collectionClazz = Class.forName(collectionType);
            return new DateCollectionStringConverter(DateString.ISO_8601,collectionClazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static AttributeConverter<?, ?> getEnumConverter(String enumDescriptor) {
        String className = enumDescriptor.replace("/", ".");
        try {
            Class clazz = Class.forName(className);
            return new EnumStringConverter(clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static AttributeConverter<?, ?> getEnumArrayConverter(String enumDescriptor) {
        String className = enumDescriptor.replace("/", ".");
        try {
            Class clazz = Class.forName(className);
            return new EnumArrayStringConverter(clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static AttributeConverter<?, ?> getEnumCollectionConverter(String enumDescriptor, String collectionType) {
        String className = enumDescriptor.replace("/", ".");
        try {
            Class clazz = Class.forName(className);
            Class collectionClazz = Class.forName(collectionType);
            return new EnumCollectionStringConverter(clazz,collectionClazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static AttributeConverter<?,?> getBigIntegerConverter() {
        return new NumberStringConverter(BigInteger.class);
    }

    public static AttributeConverter<?,?> getBigIntegerArrayConverter() {
        return new NumberArrayStringConverter(BigInteger.class);
    }

    public static AttributeConverter<?,?> getBigIntegerCollectionConverter(String collectionType) {
        try {
            Class collectionClazz = Class.forName(collectionType);
            return new NumberCollectionStringConverter(BigInteger.class,collectionClazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static AttributeConverter<?, ?> getBigDecimalConverter() {
        return new NumberStringConverter(BigDecimal.class);
    }

    public static AttributeConverter<?, ?> getBigDecimalArrayConverter() {
        return new NumberArrayStringConverter(BigDecimal.class);
    }
    public static AttributeConverter<?, ?> getBigDecimalCollectionConverter(String collectionType) {
        try {
            Class collectionClazz = Class.forName(collectionType);
            return new NumberCollectionStringConverter(BigDecimal.class,collectionClazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static AttributeConverter<?, ?> getByteArrayBase64Converter() {
        return new ByteArrayBase64Converter();
    }

    public static AttributeConverter<?, ?> getByteArrayWrapperBase64Converter() {
        return new ByteArrayWrapperBase64Converter();
    }

}
