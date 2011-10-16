/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.rest.support;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.server.plugins.*;
import org.springframework.context.ApplicationContext;
import org.springframework.data.neo4j.aspects.*;
import org.springframework.data.neo4j.server.ProvidedClassPathXmlApplicationContext;
import org.springframework.data.neo4j.support.Neo4jTemplate;

/**
 * @author mh
 * @since 14.04.11
 */
@Description("A test plugin for spring data graph usage")
public class TestServerPlugin extends ServerPlugin {

    private ApplicationContext ctx;
    private PersonRepository personRepository;
    private Neo4jTemplate template;

    public TestServerPlugin() {
        System.out.println("Initializing ServerPlugin");
    }

    @Name( "person")
    @PluginTarget(GraphDatabaseService.class)
    public Node person(@Source GraphDatabaseService graphDb, @Parameter(name="name") String name) {
        context(graphDb);
        final Person result = personRepository.findByPropertyValue(Person.NAME_INDEX, "name",name);
        return result!=null ? result.getPersistentState() : null;
    }

    private synchronized ApplicationContext context(GraphDatabaseService graphDb) {
        if (ctx==null) {
            ctx = new ProvidedClassPathXmlApplicationContext(graphDb, "Plugin-context.xml");
            personRepository = ctx.getBean(PersonRepository.class);
            template = ctx.getBean(Neo4jTemplate.class);
        }
        return ctx;
    }

    @Name( "get_all_friends" )
    @Description("gets all friends of the given node")
    @PluginTarget(Node.class)
    public Iterable<Node> allFriendsOf(@Source Node target) {
        context(target.getGraphDatabase());
        final Person person = template.createEntityFromState(target, Person.class);
        return new IterableWrapper<Node, Friendship>(person.getFriendships()) {
            @Override
            protected Node underlyingObjectToObject(Friendship friendship) {
                return friendship.getPerson2().getPersistentState();
            }
        };
    }
}
