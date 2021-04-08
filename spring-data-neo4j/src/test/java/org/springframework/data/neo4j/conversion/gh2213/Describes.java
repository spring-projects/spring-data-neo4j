/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.conversion.gh2213;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 * @author Michael J. Simons
 */
@RelationshipEntity("DESCRIBES")
public class Describes {

    @Id @GeneratedValue
    private Long id;

    private DescribeType something;

    @StartNode
    private NodeA nodeA;

    @EndNode
    private NodeB nodeB;

    Describes() {
    }

    public Describes(DescribeType something, NodeA nodeA, NodeB nodeB) {
        this.something = something;
        this.nodeA = nodeA;
        this.nodeB = nodeB;
    }

    public Long getId() {
        return id;
    }

    public DescribeType getSomething() {
        return something;
    }

    public void setSomething(DescribeType something) {
        this.something = something;
    }

    public NodeA getNodeA() {
        return nodeA;
    }

    public void setNodeA(NodeA nodeA) {
        this.nodeA = nodeA;
    }

    public NodeB getNodeB() {
        return nodeB;
    }

    public void setNodeB(NodeB nodeB) {
        this.nodeB = nodeB;
    }
}
