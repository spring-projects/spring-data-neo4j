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

interface ConstantPoolTags {

    static final int UTF_8          = 1;
    static final int INTEGER        = 3;
    static final int FLOAT          = 4;
    static final int LONG           = 5;
    static final int DOUBLE         = 6;
    static final int CLASS          = 7;
    static final int STRING         = 8;
    static final int FIELD_REF      = 9;
    static final int METHOD_REF     =10;
    static final int INTERFACE_REF  =11;
    static final int NAME_AND_TYPE  =12;
    static final int METHOD_HANDLE  =15;
    static final int METHOD_TYPE    =16;
    static final int INVOKE_DYNAMIC =18;

}
