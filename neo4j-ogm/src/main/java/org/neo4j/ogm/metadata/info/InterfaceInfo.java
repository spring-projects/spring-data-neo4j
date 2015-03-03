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

import java.util.HashSet;
import java.util.Set;

/**
 * Direct and ancestral interfaces of a given interface.
 */
class InterfaceInfo {

    private final String interfaceName;
    private final Set<InterfaceInfo> superInterfaces = new HashSet<>();

    // what's this for?
    private final Set<InterfaceInfo> allSuperInterfaces = new HashSet<>();

    public InterfaceInfo(String name) {
        this.interfaceName = name;
    }

    public Set<InterfaceInfo> superInterfaces() {
        return superInterfaces;
    }

    public Set<InterfaceInfo> allSuperInterfaces() {
        return allSuperInterfaces;
    }

    String name() {
        return interfaceName;
    }

    public String toString() {
        return name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InterfaceInfo that = (InterfaceInfo) o;

        if (!interfaceName.equals(that.interfaceName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return interfaceName.hashCode();
    }
}
