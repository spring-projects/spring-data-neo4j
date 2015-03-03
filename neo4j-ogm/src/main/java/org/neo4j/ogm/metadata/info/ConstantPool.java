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
