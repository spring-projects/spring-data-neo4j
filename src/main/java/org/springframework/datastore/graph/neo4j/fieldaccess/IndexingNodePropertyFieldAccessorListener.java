package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.index.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.datastore.graph.api.NodeBacked;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class IndexingNodePropertyFieldAccessorListener implements FieldAccessListener<NodeBacked, Object> {
    protected final Field field;
    private final IndexService indexService;

    public IndexingNodePropertyFieldAccessorListener(final Field field, final IndexService indexService) {
        this.field = field;
        this.indexService = indexService;
    }

    @Override
    public void valueChanged(final NodeBacked nodeBacked, final Object oldVal, final Object newVal) {
        if (newVal==null) indexService.removeIndex(nodeBacked.getUnderlyingNode(),getPropertyName());
        else indexService.index(nodeBacked.getUnderlyingNode(), getPropertyName(),newVal);
    }

    // todo
    private String getPropertyName() {
        return DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
    }

    public static FieldAccessorListenerFactory<NodeBacked> factory() {
        return new FieldAccessorListenerFactory<NodeBacked>() {
            @Autowired
            IndexService indexService;

            @Override
            public boolean accept(final Field f) {
                return NodePropertyFieldAccessor.factory().accept(f) || ConvertingNodePropertyFieldAccessor.factory().accept(f);
            }

            @Override
            public FieldAccessListener<NodeBacked,?> forField(final Field field) {
                return new IndexingNodePropertyFieldAccessorListener(field,indexService);
            }
        };
    }
}
