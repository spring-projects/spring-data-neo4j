package org.neo4j.examples.imdb.web;

import javax.servlet.ServletException;
import java.util.Map;

public interface FindControllerDelegate {
    void getModel(Object command, Map<String, Object> model) throws ServletException;

    String getFieldName();
}