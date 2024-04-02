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
package org.springframework.data.neo4j.integration.issues.gh2886;

import org.springframework.data.neo4j.core.schema.Node;

/**
 * GH-2886
 */
@Node(primaryLabel = "MagicalFruit")
public class MagicalFruit extends Fruit {

	private double volume;

	private String color;

	public double getVolume() {
		return this.volume;
	}

	public String getColor() {
		return this.color;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}

	public void setColor(String color) {
		this.color = color;
	}
}
