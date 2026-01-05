/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2168;

/**
 * @author Michael J. Simons
 */
@SuppressWarnings("HiddenField")
public class UnrelatedObject {

	private boolean aBooleanValue;

	private Long aLongValue;

	public UnrelatedObject() {
		this.aLongValue = 0L;
	}

	public UnrelatedObject(boolean aBooleanValue, Long aLongValue) {
		this.aBooleanValue = aBooleanValue;
		this.aLongValue = aLongValue;
	}

	public boolean isABooleanValue() {
		return this.aBooleanValue;
	}

	public void setABooleanValue(boolean aBooleanValue) {
		this.aBooleanValue = aBooleanValue;
	}

	public Long getALongValue() {
		return this.aLongValue;
	}

	public void setALongValue(Long aLongValue) {
		this.aLongValue = aLongValue;
	}

}
