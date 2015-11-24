/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.template;

import org.neo4j.ogm.exception.*;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.NoTransactionException;

/**
 * @author Luanne Misquitta
 */
public class Neo4jOgmExceptionTranslator {

	public static DataAccessException translateExceptionIfPossible(Exception ex) {
		try {
			throw ex;
		}  catch(NotFoundException nfe) {
			throw new DataRetrievalFailureException(nfe.getMessage(), nfe);
		} catch(InvalidDepthException ide) {
			throw new InvalidDataAccessApiUsageException(ide.getMessage(), ide);
		} catch(MappingException me) {
			throw new UncategorizedGraphStoreException(me.getMessage(), me);
		} catch(ResultProcessingException rpe) {
			throw new DataRetrievalFailureException(rpe.getMessage(), rpe);
		} catch(TransactionException te) {
			throw new NoTransactionException(te.getMessage(), te);
		} catch(RuntimeException re) {
			throw new UncategorizedGraphStoreException(re.getMessage(),re);
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}


}
