package org.neo4j.app.todos;

import org.springframework.data.neo4j.repository.GraphRepository;

public interface TodoRepository extends GraphRepository<Todo> {

}
