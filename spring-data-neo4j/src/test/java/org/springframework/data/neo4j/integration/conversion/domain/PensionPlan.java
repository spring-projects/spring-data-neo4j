/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.conversion.domain;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

/**
 * @author Adam George
 * @author Luanne Misquitta
 */
@NodeEntity
public class PensionPlan {

    @GraphId
    private Long pensionPlanId;

    @Convert(graphPropertyType = Integer.class)
    private MonetaryAmount fundValue;

    @Convert(graphPropertyType = SiteMember.class) //nonsensical conversion for test purposes
    private JavaElement javaElement;

    private String providerName;

    PensionPlan() {
        // default constructor for OGM
    }

    public PensionPlan(MonetaryAmount fundValue, String providerName) {
        this.fundValue = fundValue;
        this.providerName = providerName;
    }

    public Long getPensionPlanId() {
        return pensionPlanId;
    }

    public String getProviderName() {
        return providerName;
    }

    public MonetaryAmount getFundValue() {
        return fundValue;
    }

    public JavaElement getJavaElement() {
        return javaElement;
    }

    public void setJavaElement(JavaElement javaElement) {
        this.javaElement = javaElement;
    }
}
