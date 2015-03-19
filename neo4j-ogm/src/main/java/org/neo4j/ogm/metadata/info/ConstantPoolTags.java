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
