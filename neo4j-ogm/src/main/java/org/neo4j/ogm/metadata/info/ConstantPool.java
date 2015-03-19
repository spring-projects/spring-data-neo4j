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

class ConstantPool {

    private final Object[] pool;

    public ConstantPool(DataInputStream stream) throws IOException {

        int size = stream.readUnsignedShort();
        pool = new Object[size];

        for (int i = 1; i < size; i++) {
            final int flag = stream.readUnsignedByte();
            switch (flag) {
                case ConstantPoolTags.UTF_8:
                    pool[i] = stream.readUTF();
                    break;
                case ConstantPoolTags.INTEGER:
                case ConstantPoolTags.FLOAT:
                    stream.skipBytes(4);
                    break;
                case ConstantPoolTags.LONG:
                case ConstantPoolTags.DOUBLE:
                    stream.skipBytes(8);
                    i++; // double slot
                    break;
                case ConstantPoolTags.CLASS:
                case ConstantPoolTags.STRING:
                    pool[i] = stream.readUnsignedShort();
                    break;
                case ConstantPoolTags.FIELD_REF:
                case ConstantPoolTags.METHOD_REF:
                case ConstantPoolTags.INTERFACE_REF:
                case ConstantPoolTags.NAME_AND_TYPE:
                    stream.skipBytes(2); // cypherReference to owning class
                    pool[i]=stream.readUnsignedShort();
                    break;
                case ConstantPoolTags.METHOD_HANDLE:
                    stream.skipBytes(3);
                    break;
                case ConstantPoolTags.METHOD_TYPE:
                    stream.skipBytes(2);
                    break;
                case ConstantPoolTags.INVOKE_DYNAMIC:
                    stream.skipBytes(4);
                    break;
                default:
                    throw new ClassFormatError("Unknown tag value for constant pool entry: " + flag);
            }
        }
    }

    public String lookup(int entry) {

        Object constantPoolObj = pool[entry];
        return (constantPoolObj instanceof Integer
                ? (String) pool[(Integer) constantPoolObj]
                : (String) constantPoolObj);
    }
}
