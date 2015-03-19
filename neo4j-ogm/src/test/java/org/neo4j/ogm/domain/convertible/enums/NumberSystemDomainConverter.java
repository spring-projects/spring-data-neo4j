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

package org.neo4j.ogm.domain.convertible.enums;

import org.neo4j.ogm.typeconversion.AttributeConverter;

public class NumberSystemDomainConverter implements AttributeConverter<NumberSystem, String> {

    @Override
    public String toGraphProperty(NumberSystem value) {
        return value.getDomain();
    }

    @Override
    public NumberSystem toEntityAttribute(String value) {
        for (NumberSystem numberSystem : NumberSystem.values()) {
            if (numberSystem.getDomain().equals(value)) {
                return numberSystem;
            }
        }
        throw new RuntimeException("Conversion failed!");
    }

}
