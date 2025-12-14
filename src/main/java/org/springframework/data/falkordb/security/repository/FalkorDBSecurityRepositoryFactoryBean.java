/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.repository;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.repository.support.FalkorDBRepositoryFactoryBean;
import org.springframework.data.falkordb.security.audit.AuditLogger;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.util.Assert;

/**
 * RepositoryFactoryBean that creates security-aware repositories backed by
 * {@link SecureFalkorDBRepository}.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class FalkorDBSecurityRepositoryFactoryBean<T extends Repository<S, ID>, S, ID>
		extends FalkorDBRepositoryFactoryBean<T, S, ID> {

	private FalkorDBTemplate falkorDBTemplate;

	private AuditLogger auditLogger;

	public FalkorDBSecurityRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	@org.springframework.beans.factory.annotation.Autowired(required = false)
	public void setAuditLogger(AuditLogger auditLogger) {
		this.auditLogger = auditLogger;
	}

	@Override
	public void setFalkorDBTemplate(FalkorDBTemplate falkorDBTemplate) {
		super.setFalkorDBTemplate(falkorDBTemplate);
		this.falkorDBTemplate = falkorDBTemplate;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		Assert.state(this.falkorDBTemplate != null, "FalkorDBTemplate must not be null");
		return new FalkorDBSecurityRepositoryFactory(this.falkorDBTemplate, this.auditLogger);
	}

}
