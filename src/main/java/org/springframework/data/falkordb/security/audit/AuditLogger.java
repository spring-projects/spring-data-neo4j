/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.audit;

import java.time.Instant;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.security.model.AuditLog;

/**
 * Minimal synchronous audit logger that persists {@link AuditLog} entries
 * using {@link FalkorDBTemplate}.
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class AuditLogger {

	private final FalkorDBTemplate template;

	private final boolean enabled;

	public AuditLogger(FalkorDBTemplate template) {
		this(template, true);
	}

	public AuditLogger(FalkorDBTemplate template, boolean enabled) {
		this.template = template;
		this.enabled = enabled;
	}

	public void log(String username, String action, String resource, boolean granted, String reason) {
		if (!this.enabled) {
			return;
		}
		AuditLog log = new AuditLog();
		log.setTimestamp(Instant.now());
		log.setUsername(username);
		log.setAction(action);
		log.setResource(resource);
		log.setGranted(granted);
		log.setReason(reason);
		this.template.save(log);
	}

}
