/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.integration.cascading;

import java.util.List;

import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * Parent / Versioned / Externally generated id
 */
@Node
public class PVE implements Parent, Versioned, ExternalId {

	@Id
	@GeneratedValue(UUIDStringGenerator.class)
	private String id;

	@Version
	private Long version;

	private String name;

	@Relationship("HAS_SINGLE_CUI")
	private CUI singleCUI;

	@Relationship("HAS_SINGLE_CUE")
	private CUE singleCUE;

	@Relationship("HAS_MANY_CUI")
	private List<CUI> manyCUI;

	@Relationship("HAS_SINGLE_CVI")
	private CVI singleCVI;

	@Relationship("HAS_SINGLE_CVE")
	private CVE singleCVE;

	@Relationship("HAS_MANY_CVI")
	private List<CVI> manyCVI;

	public PVE(String name) {
		this.name = name;
		this.singleCUI = new CUI(name + ".singleCUI");
		this.singleCUE = new CUE(name + ".singleCUE");
		this.manyCUI = List.of(
				new CUI(name + ".cUI1"),
				new CUI(name + ".cUI2")
		);
		this.singleCVI = new CVI(name + ".singleCVI");
		this.singleCVE = new CVE(name + ".singleCVE");
		this.manyCVI = List.of(
				new CVI(name + ".cVI1"),
				new CVI(name + ".cVI2")
		);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Long getVersion() {
		return version;
	}
}
