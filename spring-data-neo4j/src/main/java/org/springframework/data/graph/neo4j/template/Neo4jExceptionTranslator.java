package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.index.impl.lucene.QueryNotPossibleException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.kernel.impl.core.ReadOnlyDbException;
import org.neo4j.kernel.impl.nioneo.store.StoreFailureException;
import org.neo4j.kernel.impl.persistence.IdGenerationFailedException;
import org.neo4j.kernel.impl.transaction.IllegalResourceException;
import org.neo4j.kernel.impl.transaction.LockException;
import org.neo4j.kernel.impl.transaction.xaframework.ReadPastEndException;
import org.springframework.dao.*;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.graph.UncategorizedGraphStoreException;

/**
 * @author mh
 * @since 21.02.11
 */
public class Neo4jExceptionTranslator implements PersistenceExceptionTranslator {

    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        // todo delete, duplicate semantics
        try {
            throw ex;
        } catch(IllegalArgumentException iae) {
            throw new InvalidDataAccessApiUsageException(iae.getMessage(),iae);
        } catch(DataAccessException dae) {
            throw dae;
        } catch(NotInTransactionException nit) {
            throw new InvalidDataAccessApiUsageException(nit.getMessage(), nit);
        } catch(TransactionFailureException tfe) {
            throw new UncategorizedGraphStoreException(tfe.getMessage(), tfe);
        } catch(ReadOnlyDbException rodbe) {
            throw new InvalidDataAccessResourceUsageException(rodbe.getMessage(), rodbe);
        } catch(IllegalResourceException ire) {
            throw new InvalidDataAccessResourceUsageException(ire.getMessage(), ire);
        } catch(StoreFailureException sfe) {
            throw new DataAccessResourceFailureException(sfe.getMessage(), sfe);
        } catch(IdGenerationFailedException idfe) {
            throw new NonTransientDataAccessResourceException(idfe.getMessage(), idfe);
        } catch(NotFoundException nfe) {
            throw new DataRetrievalFailureException(nfe.getMessage(), nfe);
        } catch(QueryNotPossibleException qnpe) {
            throw new ConcurrencyFailureException(qnpe.getMessage(),qnpe);
        } catch(DeadlockDetectedException dde) {
            throw new ConcurrencyFailureException(dde.getMessage(),dde);
        } catch(LockException le) {
            throw new ConcurrencyFailureException(le.getMessage(),le);
        } catch(RuntimeException e) {
            throw e; // exception thrown by the user
        }
    }

}
