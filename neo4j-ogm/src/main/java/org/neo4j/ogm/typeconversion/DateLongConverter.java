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

package org.neo4j.ogm.typeconversion;

import java.util.Date;

public class DateLongConverter implements AttributeConverter<Date, Long> {

    @Override
    public Long toGraphProperty(Date value) {
        if (value == null) return null;
        return value.getTime();
    }

    @Override
    public Date toEntityAttribute(Long value) {
        if (value == null) return null;
        return new Date(value);
    }
}
