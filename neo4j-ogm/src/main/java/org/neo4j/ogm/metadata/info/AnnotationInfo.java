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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vince Bickers
 */
public class AnnotationInfo {

    private String annotationName;
    private final Map<String, String> elements = new HashMap<>();

    AnnotationInfo() {}

    public String getName() {
        return annotationName;
    }

    void setName(String annotationName) {
        this.annotationName = annotationName;
    }

    void put(String key, String value) {
        elements.put(key, value);
    }

    public String get(String key, String defaultValue) {
        if (elements.get(key) == null) {
            put(key, defaultValue);
        }
        return elements.get(key);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(annotationName);
        sb.append(": ");
        for (String key : elements.keySet()) {
            sb.append(key);
            sb.append(":'");
            sb.append(get(key, null));
            sb.append("'");
            sb.append(" ");
        }
        return sb.toString();
    }

    public AnnotationInfo(final DataInputStream dataInputStream, ConstantPool constantPool) throws IOException {
        String annotationFieldDescriptor = constantPool.lookup(dataInputStream.readUnsignedShort());
        String annotationClassName;
        if (annotationFieldDescriptor.charAt(0) == 'L'
                && annotationFieldDescriptor.charAt(annotationFieldDescriptor.length() - 1) == ';') {
            annotationClassName = annotationFieldDescriptor.substring(1,
                    annotationFieldDescriptor.length() - 1).replace('/', '.');
        } else {
            annotationClassName = annotationFieldDescriptor;
        }
        setName(annotationClassName);

        int numElementValuePairs = dataInputStream.readUnsignedShort();

        for (int i = 0; i < numElementValuePairs; i++) {
            String elementName = constantPool.lookup(dataInputStream.readUnsignedShort());
            Object value = readAnnotationElementValue(dataInputStream, constantPool);
            if (elementName != null && value != null) {
                put(elementName, value.toString());
            }
        }
    }

    private Object readAnnotationElementValue(final DataInputStream dataInputStream, ConstantPool constantPool) throws IOException {
        int tag = dataInputStream.readUnsignedByte();
        switch (tag) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
            case 's':
                // const_value_index
                return constantPool.lookup(dataInputStream.readUnsignedShort());
            case 'e':
                // enum_const_value (NOT HANDLED)
                dataInputStream.skipBytes(4);
                return null;
            case 'c':
                // class_info_index
                return constantPool.lookup(dataInputStream.readUnsignedShort());
            case '@':
                // Complex (nested) annotation
                return constantPool.lookup(dataInputStream.readUnsignedShort());
            case '[':
                // array_value (NOT HANDLED)
                final int count = dataInputStream.readUnsignedShort();
                // create an object[] here...
                for (int l = 0; l < count; ++l) {
                    // Nested annotation element value
                    readAnnotationElementValue(dataInputStream, constantPool);
                }
                return null;
            default:
                throw new ClassFormatError("Invalid annotation element type tag: 0x" + Integer.toHexString(tag));
        }
    }

}
