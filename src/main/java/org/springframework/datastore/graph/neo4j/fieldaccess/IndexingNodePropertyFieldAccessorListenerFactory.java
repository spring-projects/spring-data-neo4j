package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.index.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.api.NodeBacked;

import java.lang.reflect.Field;

@Configurable
class IndexingNodePropertyFieldAccessorListenerFactory implements FieldAccessorListenerFactory<NodeBacked> {
    @Autowired
    IndexService indexService;

    @Override
    public boolean accept(final Field f) {
	    return new PropertyFieldAccessorFactory().accept(f) || new ConvertingNodePropertyFieldAccessorFactory().accept(f);
    }

    @Override
    public FieldAccessListener<NodeBacked,?> forField(final Field field) {
        return new IndexingNodePropertyFieldAccessorListener(field,indexService);
    }

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class IndexingNodePropertyFieldAccessorListener implements FieldAccessListener<NodeBacked, Object> {
	    protected final Field field;
	    private final IndexService indexService;

	    public IndexingNodePropertyFieldAccessorListener(final Field field, final IndexService indexService) {
	        this.field = field;
	        this.indexService = indexService;
	    }

	    @Override
	    public void valueChanged(final NodeBacked nodeBacked, final Object oldVal, final Object newVal) {
	        if (newVal==null) indexService.removeIndex(nodeBacked.getUnderlyingState(),getPropertyName());
	        else indexService.index(nodeBacked.getUnderlyingState(), getPropertyName(),newVal);
	    }

	    // todo
	    private String getPropertyName() {
	        return DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
	    }

	    public static FieldAccessorListenerFactory<NodeBacked> factory() {
	        return new IndexingNodePropertyFieldAccessorListenerFactory();
	    }

	}
}
