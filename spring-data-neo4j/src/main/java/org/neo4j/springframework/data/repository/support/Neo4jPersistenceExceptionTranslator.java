/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.repository.support;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.neo4j.driver.exceptions.*;
import org.neo4j.driver.exceptions.value.ValueException;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.NonTransientDataAccessResourceException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * A PersistenceExceptionTranslator to get picked up by the Spring exception translation infrastructure.
 *
 * @author Michael J. Simons
 * @soundtrack Kummer - KIOX
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public final class Neo4jPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

	private static final LogAccessor log = new LogAccessor(
		LogFactory.getLog(Neo4jPersistenceExceptionTranslator.class));

	private static final Map<String, Optional<BiFunction<String, Throwable, DataAccessException>>> ERROR_CODE_MAPPINGS;

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {

		if (ex instanceof DataAccessException) {
			return (DataAccessException) ex;
		} else if (ex instanceof DiscoveryException) {
			return translateImpl((Neo4jException) ex, TransientDataAccessResourceException::new);
		} else if (ex instanceof DatabaseException) {
			return translateImpl((Neo4jException) ex, NonTransientDataAccessResourceException::new);
		} else if (ex instanceof ServiceUnavailableException) {
			return translateImpl((Neo4jException) ex, NonTransientDataAccessResourceException::new);
		} else if (ex instanceof SessionExpiredException) {
			return translateImpl((Neo4jException) ex, RecoverableDataAccessException::new);
		} else if (ex instanceof ProtocolException) {
			return translateImpl((Neo4jException) ex, NonTransientDataAccessResourceException::new);
		} else if (ex instanceof TransientException) {
			return translateImpl((Neo4jException) ex, TransientDataAccessResourceException::new);
		} else if (ex instanceof ValueException) {
			return translateImpl((Neo4jException) ex, InvalidDataAccessApiUsageException::new);
		} else if (ex instanceof AuthenticationException) {
			return translateImpl((Neo4jException) ex, PermissionDeniedDataAccessException::new);
		} else if (ex instanceof ResultConsumedException) {
			return translateImpl((Neo4jException) ex, InvalidDataAccessApiUsageException::new);
		} else if (ex instanceof FatalDiscoveryException) {
			return translateImpl((Neo4jException) ex, NonTransientDataAccessResourceException::new);
		} else if (ex instanceof TransactionNestingException) {
			return translateImpl((Neo4jException) ex, InvalidDataAccessApiUsageException::new);
		} else if (ex instanceof ClientException) {
			return translateImpl((Neo4jException) ex, InvalidDataAccessResourceUsageException::new);
		}

		log.warn(() -> String.format("Don't know how to translate exception of type %s", ex.getClass()));
		return null;
	}

	private static DataAccessException translateImpl(Neo4jException e,
		BiFunction<String, Throwable, DataAccessException> defaultTranslationProvider) {

		Optional<String> optionalErrorCode = Optional.ofNullable(e.code());
		String msg = String.format("%s; Error code '%s'", e.getMessage(), optionalErrorCode.orElse("n/a"));

		return optionalErrorCode.flatMap(code -> ERROR_CODE_MAPPINGS.getOrDefault(code, Optional.empty()))
			.orElse(defaultTranslationProvider).apply(msg, e.getCause());
	}

	static {
		Map<String, Optional<BiFunction<String, Throwable, DataAccessException>>> tmp = new HashMap<>();

		// Error codes as of Neo4j 4.0.0
		// https://neo4j.com/docs/status-codes/current/
		tmp.put("Neo.ClientError.Cluster.NotALeader", Optional.empty());
		tmp.put("Neo.ClientError.Database.DatabaseNotFound", Optional.empty());
		tmp.put("Neo.ClientError.Database.ExistingDatabaseFound", Optional.empty());
		tmp.put("Neo.ClientError.Fabric.AccessMode", Optional.empty());
		tmp.put("Neo.ClientError.General.ForbiddenOnReadOnlyDatabase", Optional.empty());
		tmp.put("Neo.ClientError.General.InvalidArguments", Optional.empty());
		tmp.put("Neo.ClientError.Procedure.ProcedureCallFailed", Optional.empty());
		tmp.put("Neo.ClientError.Procedure.ProcedureNotFound", Optional.empty());
		tmp.put("Neo.ClientError.Procedure.ProcedureRegistrationFailed", Optional.empty());
		tmp.put("Neo.ClientError.Procedure.ProcedureTimedOut", Optional.empty());
		tmp.put("Neo.ClientError.Procedure.TypeError", Optional.empty());
		tmp.put("Neo.ClientError.Request.Invalid", Optional.empty());
		tmp.put("Neo.ClientError.Request.InvalidFormat", Optional.empty());
		tmp.put("Neo.ClientError.Request.InvalidUsage", Optional.empty());
		tmp.put("Neo.ClientError.Schema.ConstraintAlreadyExists", Optional.empty());
		tmp.put("Neo.ClientError.Schema.ConstraintNotFound", Optional.empty());
		tmp.put("Neo.ClientError.Schema.ConstraintValidationFailed", Optional.of(DataIntegrityViolationException::new));
		tmp.put("Neo.ClientError.Schema.ConstraintViolation", Optional.of(DataIntegrityViolationException::new));
		tmp.put("Neo.ClientError.Schema.ConstraintWithNameAlreadyExists", Optional.empty());
		tmp.put("Neo.ClientError.Schema.EquivalentSchemaRuleAlreadyExists", Optional.empty());
		tmp.put("Neo.ClientError.Schema.ForbiddenOnConstraintIndex", Optional.empty());
		tmp.put("Neo.ClientError.Schema.IndexAlreadyExists", Optional.empty());
		tmp.put("Neo.ClientError.Schema.IndexMultipleFound", Optional.empty());
		tmp.put("Neo.ClientError.Schema.IndexNotApplicable", Optional.empty());
		tmp.put("Neo.ClientError.Schema.IndexNotFound", Optional.empty());
		tmp.put("Neo.ClientError.Schema.IndexWithNameAlreadyExists", Optional.empty());
		tmp.put("Neo.ClientError.Schema.RepeatedLabelInSchema", Optional.empty());
		tmp.put("Neo.ClientError.Schema.RepeatedPropertyInCompositeSchema", Optional.empty());
		tmp.put("Neo.ClientError.Schema.RepeatedRelationshipTypeInSchema", Optional.empty());
		tmp.put("Neo.ClientError.Schema.TokenNameError", Optional.empty());
		tmp.put("Neo.ClientError.Security.AuthenticationRateLimit", Optional.empty());
		tmp.put("Neo.ClientError.Security.AuthorizationExpired", Optional.empty());
		tmp.put("Neo.ClientError.Security.CredentialsExpired", Optional.empty());
		tmp.put("Neo.ClientError.Security.Forbidden", Optional.empty());
		tmp.put("Neo.ClientError.Security.Unauthorized", Optional.empty());
		tmp.put("Neo.ClientError.Statement.ArgumentError", Optional.empty());
		tmp.put("Neo.ClientError.Statement.ArithmeticError", Optional.empty());
		tmp.put("Neo.ClientError.Statement.ConstraintVerificationFailed", Optional.empty());
		tmp.put("Neo.ClientError.Statement.EntityNotFound", Optional.empty());
		tmp.put("Neo.ClientError.Statement.ExternalResourceFailed", Optional.empty());
		tmp.put("Neo.ClientError.Statement.NotSystemDatabaseError", Optional.empty());
		tmp.put("Neo.ClientError.Statement.ParameterMissing", Optional.empty());
		tmp.put("Neo.ClientError.Statement.PropertyNotFound", Optional.empty());
		tmp.put("Neo.ClientError.Statement.RuntimeUnsupportedError", Optional.empty());
		tmp.put("Neo.ClientError.Statement.SemanticError", Optional.empty());
		tmp.put("Neo.ClientError.Statement.SyntaxError", Optional.empty());
		tmp.put("Neo.ClientError.Statement.TypeError", Optional.empty());
		tmp.put("Neo.ClientError.Transaction.ForbiddenDueToTransactionType", Optional.empty());
		tmp.put("Neo.ClientError.Transaction.InvalidBookmark", Optional.empty());
		tmp.put("Neo.ClientError.Transaction.InvalidBookmarkMixture", Optional.empty());
		tmp.put("Neo.ClientError.Transaction.TransactionAccessedConcurrently", Optional.empty());
		tmp.put("Neo.ClientError.Transaction.TransactionHookFailed", Optional.empty());
		tmp.put("Neo.ClientError.Transaction.TransactionMarkedAsFailed", Optional.empty());
		tmp.put("Neo.ClientError.Transaction.TransactionNotFound", Optional.empty());
		tmp.put("Neo.ClientError.Transaction.TransactionTimedOut", Optional.empty());
		tmp.put("Neo.ClientError.Transaction.TransactionValidationFailed", Optional.empty());
		tmp.put("Neo.ClientNotification.Procedure.ProcedureWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.CartesianProductWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.DynamicPropertyWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.EagerOperatorWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.ExhaustiveShortestPathWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.ExperimentalFeature", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.FeatureDeprecationWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.JoinHintUnfulfillableWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.NoApplicableIndexWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.RuntimeUnsupportedWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.SuboptimalIndexForWildcardQuery", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.UnboundedVariableLengthPatternWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.UnknownLabelWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.UnknownPropertyKeyWarning", Optional.empty());
		tmp.put("Neo.ClientNotification.Statement.UnknownRelationshipTypeWarning", Optional.empty());
		tmp.put("Neo.DatabaseError.Database.DatabaseLimitReached", Optional.empty());
		tmp.put("Neo.DatabaseError.Database.UnableToStartDatabase", Optional.empty());
		tmp.put("Neo.DatabaseError.Database.Unknown", Optional.empty());
		tmp.put("Neo.DatabaseError.Fabric.RemoteExecutionFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.General.IndexCorruptionDetected", Optional.empty());
		tmp.put("Neo.DatabaseError.General.SchemaCorruptionDetected", Optional.empty());
		tmp.put("Neo.DatabaseError.General.StorageDamageDetected", Optional.empty());
		tmp.put("Neo.DatabaseError.General.UnknownError", Optional.empty());
		tmp.put("Neo.DatabaseError.Schema.ConstraintCreationFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Schema.ConstraintDropFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Schema.IndexCreationFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Schema.IndexDropFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Schema.LabelAccessFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Schema.PropertyKeyAccessFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Schema.RelationshipTypeAccessFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Schema.SchemaRuleAccessFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Schema.SchemaRuleDuplicateFound", Optional.empty());
		tmp.put("Neo.DatabaseError.Schema.TokenLimitReached", Optional.empty());
		tmp.put("Neo.DatabaseError.Statement.CodeGenerationFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Statement.ExecutionFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Transaction.TransactionCommitFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Transaction.TransactionLogError", Optional.empty());
		tmp.put("Neo.DatabaseError.Transaction.TransactionRollbackFailed", Optional.empty());
		tmp.put("Neo.DatabaseError.Transaction.TransactionStartFailed", Optional.empty());
		tmp.put("Neo.TransientError.Cluster.ReplicationFailure", Optional.empty());
		tmp.put("Neo.TransientError.Database.DatabaseUnavailable", Optional.empty());
		tmp.put("Neo.TransientError.General.OutOfMemoryError", Optional.empty());
		tmp.put("Neo.TransientError.General.StackOverFlowError", Optional.empty());
		tmp.put("Neo.TransientError.General.TransactionMemoryLimit", Optional.empty());
		tmp.put("Neo.TransientError.General.TransactionOutOfMemoryError", Optional.empty());
		tmp.put("Neo.TransientError.Request.NoThreadsAvailable", Optional.empty());
		tmp.put("Neo.TransientError.Security.AuthProviderFailed", Optional.empty());
		tmp.put("Neo.TransientError.Security.AuthProviderTimeout", Optional.empty());
		tmp.put("Neo.TransientError.Security.ModifiedConcurrently", Optional.empty());
		tmp.put("Neo.TransientError.Transaction.BookmarkTimeout", Optional.empty());
		tmp.put("Neo.TransientError.Transaction.ConstraintsChanged", Optional.empty());
		tmp.put("Neo.TransientError.Transaction.DeadlockDetected", Optional.empty());
		tmp.put("Neo.TransientError.Transaction.Interrupted", Optional.empty());
		tmp.put("Neo.TransientError.Transaction.LeaseExpired", Optional.empty());
		tmp.put("Neo.TransientError.Transaction.LockAcquisitionTimeout", Optional.empty());
		tmp.put("Neo.TransientError.Transaction.LockClientStopped", Optional.empty());
		tmp.put("Neo.TransientError.Transaction.MaximumTransactionLimitReached", Optional.empty());
		tmp.put("Neo.TransientError.Transaction.Outdated", Optional.empty());
		tmp.put("Neo.TransientError.Transaction.Terminated", Optional.empty());

		ERROR_CODE_MAPPINGS = Collections.unmodifiableMap(tmp);
	}
}
