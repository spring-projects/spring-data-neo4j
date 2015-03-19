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

import org.neo4j.ogm.annotation.Transient;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author Vince Bickers
 */
public class FieldsInfo {

    private static final int STATIC_FIELD = 0x0008;
    private static final int FINAL_FIELD = 0x0010;
    private static final int TRANSIENT_FIELD = 0x0080;

    private final Map<String, FieldInfo> fields = new HashMap<>();

    FieldsInfo() {}

    public FieldsInfo(DataInputStream dataInputStream, ConstantPool constantPool) throws IOException {
        // get the field information for this class
        int fieldCount = dataInputStream.readUnsignedShort();
        for (int i = 0; i < fieldCount; i++) {
            int accessFlags = dataInputStream.readUnsignedShort();
            String fieldName = constantPool.lookup(dataInputStream.readUnsignedShort()); // name_index
            String descriptor = constantPool.lookup(dataInputStream.readUnsignedShort()); // descriptor_index
            int attributesCount = dataInputStream.readUnsignedShort();
            ObjectAnnotations objectAnnotations = new ObjectAnnotations();
            String typeParameterDescriptor = null; // available as an attribute for parameterised collections
            for (int j = 0; j < attributesCount; j++) {
                String attributeName = constantPool.lookup(dataInputStream.readUnsignedShort());
                int attributeLength = dataInputStream.readInt();
                if ("RuntimeVisibleAnnotations".equals(attributeName)) {
                    int annotationCount = dataInputStream.readUnsignedShort();
                    for (int m = 0; m < annotationCount; m++) {
                        AnnotationInfo info = new AnnotationInfo(dataInputStream, constantPool);
                        // todo: maybe register just the annotations we're interested in.
                        objectAnnotations.put(info.getName(), info);
                    }
                } else if ("Signature".equals(attributeName)) {
                    String signature = constantPool.lookup(dataInputStream.readUnsignedShort());
                    if (signature.contains("<")) {
                        typeParameterDescriptor = signature.substring(signature.indexOf('<') + 1, signature.indexOf('>'));
                    }
                }
                else {
                    dataInputStream.skipBytes(attributeLength);
                }
            }
            if ((accessFlags & (STATIC_FIELD | FINAL_FIELD | TRANSIENT_FIELD)) == 0 && objectAnnotations.get(Transient.CLASS) == null) {
                fields.put(fieldName, new FieldInfo(fieldName, descriptor, typeParameterDescriptor, objectAnnotations));
            }
        }
    }

    public Collection<FieldInfo> fields() {
        return fields.values();
    }

    public FieldInfo get(String name) {
        return fields.get(name);
    }

    public void append(FieldsInfo fieldsInfo) {
        for (FieldInfo fieldInfo : fieldsInfo.fields()) {
            if (!fields.containsKey(fieldInfo.getName())) {
                fields.put(fieldInfo.getName(), fieldInfo);
            }
        }
    }
}
