/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.repository;

import java.util.Optional;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.repository.support.FalkorDBEntityInformation;
import org.springframework.data.falkordb.repository.support.FalkorDBRepositoryFactory;
import org.springframework.data.falkordb.security.audit.AuditLogger;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.lang.Nullable;

/**
 * Security-aware extension of {@link FalkorDBRepositoryFactory} that
 * creates {@link SecureFalkorDBRepository} instances as the base class.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class FalkorDBSecurityRepositoryFactory extends FalkorDBRepositoryFactory {

	private final FalkorDBTemplate falkorDBTemplate;

	private final @Nullable AuditLogger auditLogger;

	public FalkorDBSecurityRepositoryFactory(FalkorDBTemplate falkorDBTemplate, @Nullable AuditLogger auditLogger) {
		super(falkorDBTemplate);
		this.falkorDBTemplate = falkorDBTemplate;
		this.auditLogger = auditLogger;
	}

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object getTargetRepository(RepositoryInformation information) {
		FalkorDBEntityInformation<?, Object> entityInformation = getEntityInformation(information.getDomainType());
		return new SecureFalkorDBRepository(this.falkorDBTemplate, entityInformation, this.auditLogger);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SecureFalkorDBRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
			ValueExpressionDelegate valueExpressionDelegate) {
		// Reuse the same query lookup strategy as the base factory
		return super.getQueryLookupStrategy(key, valueExpressionDelegate);
	}

}
