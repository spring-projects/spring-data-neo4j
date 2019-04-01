/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.transaction;

import org.springframework.data.neo4j.core.NodeManager;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * Dedicated holder for storing a NodeManager inside a transaction.
 * <p>
 * <strong>Note:</strong> Intended for internal usage only.
 *
 * @author Michael J. Simons
 */
public class NodeManagerHolder extends ResourceHolderSupport {

	@Nullable
	private final NodeManager nodeManager;

	public NodeManagerHolder(@Nullable NodeManager nodeManager) {
		this.nodeManager = nodeManager;
	}

	public NodeManager getNodeManager() {
		Assert.state(this.nodeManager != null, "No NodeManager available");
		return this.nodeManager;
	}

}
