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

package org.neo4j.ogm.integration.hierarchy.domain.trans;

import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.integration.hierarchy.domain.plain.PlainSingleClass;

/**
 *
 */
public class PlainClassWithTransientFields {

    private Long id;

    private TransientSingleClass transientField;

    @Transient
    private PlainSingleClass anotherTransientField;

    private transient PlainSingleClass yetAnotherTransientField;

    public TransientSingleClass getTransientField() {
        return transientField;
    }

    public void setTransientField(TransientSingleClass transientField) {
        this.transientField = transientField;
    }

    public PlainSingleClass getAnotherTransientField() {
        return anotherTransientField;
    }

    public void setAnotherTransientField(PlainSingleClass anotherTransientField) {
        this.anotherTransientField = anotherTransientField;
    }

    public PlainSingleClass getYetAnotherTransientField() {
        return yetAnotherTransientField;
    }

    public void setYetAnotherTransientField(PlainSingleClass yetAnotherTransientField) {
        this.yetAnotherTransientField = yetAnotherTransientField;
    }
}
