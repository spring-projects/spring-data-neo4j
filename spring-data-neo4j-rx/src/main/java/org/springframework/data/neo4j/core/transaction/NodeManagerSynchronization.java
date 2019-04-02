/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.transaction;

import org.springframework.data.neo4j.core.NodeManagerFactory;
import org.springframework.transaction.support.ResourceHolderSynchronization;

/**
 * @author Michael J. Simons
 */
public class NodeManagerSynchronization
	extends ResourceHolderSynchronization<NodeManagerHolder, Object> {

	private final NodeManagerHolder localNodeManagerHolder;

	NodeManagerSynchronization(NodeManagerHolder nodeManagerHolder, NodeManagerFactory nodeManagerFactory) {

		super(nodeManagerHolder, nodeManagerFactory);
		this.localNodeManagerHolder = nodeManagerHolder;
	}

	@Override
	protected void flushResource(NodeManagerHolder resourceHolder) {
		resourceHolder.getNodeManager().flush();
	}
}
