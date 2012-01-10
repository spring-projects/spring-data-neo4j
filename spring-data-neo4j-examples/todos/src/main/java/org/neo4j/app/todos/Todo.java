package org.neo4j.app.todos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

@NodeEntity
@Configurable
public class Todo {

    @SuppressWarnings("unused")
	@GraphId
    private Long id;
    
    private String title;

    private Boolean isDone = false;

    public Todo() {;}
    
    public Todo(Node n) {
        setPersistentState(n);
    }
    
    public Long getId() {
        return getNodeId();
    }

    public String getTitle() {
        return this.title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public Boolean getIsDone() {
        return this.isDone;
    }
    
    public void setIsDone(Boolean done) {
        this.isDone = done;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Done: ").append(getIsDone()).append(", ");
        sb.append("Title: ").append(getTitle());
        return sb.toString();
    }
    
    public String toJson() {
        return new JSONSerializer().exclude("*.class", "*.persistentState", "*.entityState").serialize(this);
    }    

    public static String toJsonArray(Iterable<Todo> collection) {
        return new JSONSerializer().exclude("*.class", "*.persistentState", "*.entityState").serialize(collection);
    }

    public static Todo fromJsonToTodo(String json) {
        return new JSONDeserializer<Todo>().use(null, Todo.class).deserialize(json);
    }
    
    public static Collection<Todo> fromJsonArrayToTodoes(String json) {
        return new JSONDeserializer<List<Todo>>().use(null, ArrayList.class).use("values", Todo.class).deserialize(json);
    }

}
