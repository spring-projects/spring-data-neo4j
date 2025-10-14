/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.falkordb.integration;

import java.util.List;

import org.springframework.data.falkordb.core.schema.GeneratedValue;
import org.springframework.data.falkordb.core.schema.Id;
import org.springframework.data.falkordb.core.schema.InternalIdGenerator;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.falkordb.core.schema.Property;
import org.springframework.data.falkordb.core.schema.Relationship;

/**
 * Hashtag entity for FalkorDB integration testing.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Node(labels = { "Hashtag" })
public class Hashtag {

	@Id
	@GeneratedValue(generatorClass = InternalIdGenerator.class)
	private Long id;

	@Property("tag")
	private String tag;

	@Property("usage_count")
	private Integer usageCount;

	// Relationships
	@Relationship(value = "HAS_HASHTAG", direction = Relationship.Direction.INCOMING)
	private List<Tweet> tweets;

	// Constructors
	public Hashtag() {
	}

	public Hashtag(String tag) {
		this.tag = tag;
		this.usageCount = 0;
	}

	// Getters and Setters
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTag() {
		return this.tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public Integer getUsageCount() {
		return this.usageCount;
	}

	public void setUsageCount(Integer usageCount) {
		this.usageCount = usageCount;
	}

	public List<Tweet> getTweets() {
		return this.tweets;
	}

	public void setTweets(List<Tweet> tweets) {
		this.tweets = tweets;
	}

	@Override
	public String toString() {
		return "Hashtag{" + "id=" + this.id + ", tag='" + this.tag + '\'' + ", usageCount=" + this.usageCount + '}';
	}

}
