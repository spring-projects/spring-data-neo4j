package org.springframework.datastore.graph.neo4j.spi.node;

/**
 * @author Michael Hunger
 * @since 29.08.2010
 */
public class ShouldProceedOrReturn {
    public final boolean proceed;
    public final Object value;

    public ShouldProceedOrReturn() {
        this.proceed = true;
        this.value = null;
    }

    public ShouldProceedOrReturn(final Object value) {
        this.proceed = false;
        this.value = value;
    }

    public ShouldProceedOrReturn(final boolean proceed, final Object value) {
        this.proceed = true;
        this.value = value;
    }
}
