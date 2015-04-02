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
public class InterfacesInfo {

    private final Map<String, InterfaceInfo> interfaceMap = new HashMap<>();

    InterfacesInfo() {}

    public InterfacesInfo(DataInputStream dataInputStream, ConstantPool constantPool) throws IOException {
        int interfaceCount = dataInputStream.readUnsignedShort();
        for (int i = 0; i < interfaceCount; i++) {
            String interfaceName = constantPool.lookup(dataInputStream.readUnsignedShort()).replace('/', '.');
            interfaceMap.put(interfaceName, new InterfaceInfo(interfaceName));
        }
    }

    public Collection<InterfaceInfo> list() {
        return interfaceMap.values();
    }

    public InterfaceInfo get(String interfaceName) {
        return interfaceMap.get(interfaceName);
    }

    void add(InterfaceInfo interfaceInfo) {
        interfaceMap.put(interfaceInfo.name(), interfaceInfo);
    }

    public void append(InterfacesInfo interfacesInfo) {
        for (InterfaceInfo interfaceInfo : interfacesInfo.list()) {
            add(interfaceInfo);
        }
    }
}
