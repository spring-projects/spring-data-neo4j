/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.conversion.domain;

import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;

/**
 * @author Adam George
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
@NodeEntity
public class PensionPlan {

	@Id @GeneratedValue private Long pensionPlanId;

	@Convert(graphPropertyType = Integer.class) private MonetaryAmount fundValue;

	@Convert(graphPropertyType = SiteMember.class) // nonsensical conversion for test purposes
	private JavaElement javaElement;

	private String providerName;

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
