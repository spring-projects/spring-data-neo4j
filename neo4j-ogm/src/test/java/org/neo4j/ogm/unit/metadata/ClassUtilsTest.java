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

package org.neo4j.ogm.unit.metadata;

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.ogm.metadata.ClassUtils;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class ClassUtilsTest {

    @Test
    public void shouldResolveParameterTypeForSetterMethodFromSignatureString() {
        Assert.assertEquals(Date.class, ClassUtils.getType("(Ljava/util/Date;)V"));
        assertEquals(String[].class, ClassUtils.getType("([Ljava/lang/String;)V"));
        assertEquals(boolean.class, ClassUtils.getType("(Z)V"));
        assertEquals(byte.class, ClassUtils.getType("(B)V"));
        assertEquals(char.class, ClassUtils.getType("(C)V"));
        assertEquals(double.class, ClassUtils.getType("(D)V"));
        assertEquals(float.class, ClassUtils.getType("(F)V"));
        assertEquals(int.class, ClassUtils.getType("(I)V"));
        assertEquals(long.class, ClassUtils.getType("(J)V"));
        assertEquals(short.class, ClassUtils.getType("(S)V"));
    }

}
