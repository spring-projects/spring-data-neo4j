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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vince Bickers
 */
public class AnnotationsInfo {

    private final Map<String, AnnotationInfo> classAnnotations = new HashMap<>();

    AnnotationsInfo() {}

    public AnnotationsInfo(DataInputStream dataInputStream, ConstantPool constantPool) throws IOException {
        int attributesCount = dataInputStream.readUnsignedShort();
        for (int i = 0; i < attributesCount; i++) {
            String attributeName = constantPool.lookup(dataInputStream.readUnsignedShort());
            int attributeLength = dataInputStream.readInt();
            if ("RuntimeVisibleAnnotations".equals(attributeName)) {
                int annotationCount = dataInputStream.readUnsignedShort();
                for (int m = 0; m < annotationCount; m++) {
                    AnnotationInfo info = new AnnotationInfo(dataInputStream, constantPool);
                    // todo: maybe register just the annotations we're interested in.
                    classAnnotations.put(info.getName(), info);
                }
            }
            else {
                dataInputStream.skipBytes(attributeLength);
            }
        }
    }

    public Collection<AnnotationInfo> list() {
        return classAnnotations.values();
    }

    /**
     * @param annotationName The fully-qualified class name of the annotation type
     * @return The {@link AnnotationInfo} that matches the given name or <code>null</code> if it's not present
     */
    public AnnotationInfo get(String annotationName) {
        return classAnnotations.get(annotationName);
    }

    void add(AnnotationInfo annotationInfo) {
        classAnnotations.put(annotationInfo.getName(), annotationInfo);
    }

    public void append(AnnotationsInfo annotationsInfo) {
        for (AnnotationInfo annotationInfo : annotationsInfo.list()) {
            add(annotationInfo);
        }
    }
}
