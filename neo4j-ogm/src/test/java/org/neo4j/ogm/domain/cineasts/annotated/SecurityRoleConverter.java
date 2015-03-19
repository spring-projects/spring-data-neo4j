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

package org.neo4j.ogm.domain.cineasts.annotated;

import org.neo4j.ogm.typeconversion.AttributeConverter;

public class SecurityRoleConverter implements AttributeConverter<SecurityRole[],String[]> {

    @Override
    public String[] toGraphProperty(SecurityRole[] value) {
        if(value==null) {
            return null;
        }
        String[] values = new String[(value.length)];
        int i=0;
        for(SecurityRole securityRole : value) {
            values[i++]=securityRole.name();
        }
        return values;
    }

    @Override
    public SecurityRole[] toEntityAttribute(String[] value) {
        SecurityRole[] roles =new SecurityRole[value.length];
        int i=0;
        for(String role : value) {
            roles[i++] = SecurityRole.valueOf(role);
        }
        return roles;
    }
}