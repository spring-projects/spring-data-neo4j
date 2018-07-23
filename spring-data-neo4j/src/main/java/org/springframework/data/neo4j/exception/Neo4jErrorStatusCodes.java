/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.exception;

import java.util.HashMap;

import org.springframework.dao.*;

/**
 * Conversion table for Neo4j Errors to Spring Data Exception hierarchy.
 *
 * @author Mark Angrish
 */
public class Neo4jErrorStatusCodes {

	private static HashMap<String, Class<? extends DataAccessException>> errors;

	static {
		errors = new HashMap<>();
		errors.put("Neo.ClientError.General.ForbiddenOnReadOnlyDatabase", PermissionDeniedDataAccessException.class);
		errors.put("Neo.ClientError.LegacyIndex.LegacyIndexNotFound", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Procedure.ProcedureCallFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Procedure.ProcedureNotFound", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Procedure.ProcedureRegistrationFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Procedure.TypeError", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Request.Invalid", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Request.InvalidFormat", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Request.TransactionRequired", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Schema.ConstraintAlreadyExists", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Schema.ConstraintNotFound", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Schema.ConstraintValidationFailed", DataIntegrityViolationException.class);
		errors.put("Neo.ClientError.Schema.ConstraintVerificationFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Schema.ForbiddenOnConstraintIndex", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Schema.IndexAlreadyExists", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Schema.IndexNotFound", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Schema.TokenNameError", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Security.AuthenticationRateLimit", PermissionDeniedDataAccessException.class);
		errors.put("Neo.ClientError.Security.CredentialsExpired", PermissionDeniedDataAccessException.class);
		errors.put("Neo.ClientError.Security.EncryptionRequired", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Security.Forbidden", PermissionDeniedDataAccessException.class);
		errors.put("Neo.ClientError.Security.Unauthorized", PermissionDeniedDataAccessException.class);
		errors.put("Neo.ClientError.Statement.ArgumentError", InvalidDataAccessApiUsageException.class);
		errors.put("Neo.ClientError.Statement.ArithmeticError", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Statement.ConstraintVerificationFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Statement.EntityNotFound", EmptyResultDataAccessException.class);
		errors.put("Neo.ClientError.Statement.ExternalResourceFailed", NonTransientDataAccessResourceException.class);
		errors.put("Neo.ClientError.Statement.LabelNotFound", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Statement.ParameterMissing", InvalidDataAccessApiUsageException.class);
		errors.put("Neo.ClientError.Statement.PropertyNotFound", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Statement.SemanticError", NonTransientDataAccessException.class);
		errors.put("Neo.ClientError.Statement.SyntaxError", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Statement.TypeError", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Transaction.ForbiddenDueToTransactionType",
				InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Transaction.TransactionAccessedConcurrently",
				InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Transaction.TransactionEventHandlerFailed",
				InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Transaction.TransactionHookFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Transaction.TransactionMarkedAsFailed", ConcurrencyFailureException.class);
		errors.put("Neo.ClientError.Transaction.TransactionNotFound", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Transaction.TransactionTerminated", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.ClientError.Transaction.TransactionValidationFailed",
				InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.DatabaseError.General.IndexCorruptionDetected", DataAccessResourceFailureException.class);
		errors.put("Neo.DatabaseError.General.SchemaCorruptionDetected", DataAccessResourceFailureException.class);
		errors.put("Neo.DatabaseError.General.UnknownError", UncategorizedDataAccessException.class);
		errors.put("Neo.DatabaseError.Schema.ConstraintCreationFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.DatabaseError.Schema.ConstraintDropFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.DatabaseError.Schema.IndexCreationFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.DatabaseError.Schema.IndexDropFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.DatabaseError.Schema.LabelAccessFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.DatabaseError.Schema.LabelLimitReached", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.DatabaseError.Schema.PropertyKeyAccessFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.DatabaseError.Schema.RelationshipTypeAccessFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.DatabaseError.Schema.SchemaRuleAccessFailed", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.DatabaseError.Schema.SchemaRuleDuplicateFound", InvalidDataAccessResourceUsageException.class);
		errors.put("Neo.DatabaseError.Statement.ExecutionFailed", TransientDataAccessResourceException.class);
		errors.put("Neo.DatabaseError.Transaction.TransactionCommitFailed", TransientDataAccessResourceException.class);
		errors.put("Neo.DatabaseError.Transaction.TransactionLogError", TransientDataAccessResourceException.class);
		errors.put("Neo.DatabaseError.Transaction.TransactionRollbackFailed", TransientDataAccessResourceException.class);
		errors.put("Neo.DatabaseError.Transaction.TransactionStartFailed", TransientDataAccessResourceException.class);
		errors.put("Neo.TransientError.General.DatabaseUnavailable", TransientDataAccessResourceException.class);
		errors.put("Neo.TransientError.General.OutOfMemoryError", NonTransientDataAccessResourceException.class);
		errors.put("Neo.TransientError.General.StackOverFlowError", NonTransientDataAccessResourceException.class);
		errors.put("Neo.TransientError.Network.CommunicationError", DataAccessResourceFailureException.class);
		errors.put("Neo.TransientError.Schema.SchemaModifiedConcurrently", ConcurrencyFailureException.class);
		errors.put("Neo.TransientError.Security.ModifiedConcurrently", ConcurrencyFailureException.class);
		errors.put("Neo.TransientError.Transaction.ConstraintsChanged", ConcurrencyFailureException.class);
		errors.put("Neo.TransientError.Transaction.DeadlockDetected", ConcurrencyFailureException.class);
		errors.put("Neo.TransientError.Transaction.InstanceStateChanged", ConcurrencyFailureException.class);
		errors.put("Neo.TransientError.Transaction.LockClientStopped", NonTransientDataAccessResourceException.class);
		errors.put("Neo.TransientError.Transaction.LockSessionExpired", DataAccessResourceFailureException.class);
		errors.put("Neo.TransientError.Transaction.Outdated", TransientDataAccessResourceException.class);
		errors.put("Neo.TransientError.Transaction.Terminated", TransientDataAccessResourceException.class);
	}

	public static Class<? extends DataAccessException> translate(String errorCode) {
		return errors.get(errorCode);
	}
}
