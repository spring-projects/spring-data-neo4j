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

	@Relationship(value = "HAS_SINGLE_CUI", cascadeUpdates = false)
	private CUI singleCUI;

	@Relationship(value = "HAS_SINGLE_CUE", cascadeUpdates = false)
	private CUE singleCUE;

	@Relationship("HAS_MANY_CUI")
	private List<CUI> manyCUI;

	@Relationship(value = "HAS_SINGLE_CVI", cascadeUpdates = false)
	private CVI singleCVI;

	@Relationship(value = "HAS_SINGLE_CVE", cascadeUpdates = false)
	private CVE singleCVE;

	@Relationship(value = "HAS_MANY_CVI", cascadeUpdates = false)
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

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Long getVersion() {
		return version;
	}

	@Override
	public List<CUI> getManyCUI() {
		return manyCUI;
	}

	@Override
	public List<CVI> getManyCVI() {
		return manyCVI;
	}

	@Override
	public CUE getSingleCUE() {
		return singleCUE;
	}

	@Override
	public CUI getSingleCUI() {
		return singleCUI;
	}

	@Override
	public CVE getSingleCVE() {
		return singleCVE;
	}

	@Override
	public CVI getSingleCVI() {
		return singleCVI;
	}
}
