/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.audit;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apiguardian.api.API;

import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.security.model.AuditLog;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
		log(null, username, action, resource, null, granted, reason, null);
	}

	public void log(@Nullable Long userId, @Nullable String username, String action, String resource,
			@Nullable Long resourceId, boolean granted, @Nullable String reason, @Nullable String ipAddress) {
		if (!this.enabled) {
			return;
		}
		Assert.hasText(action, "action must not be empty");
		Assert.hasText(resource, "resource must not be empty");

		AuditLog log = new AuditLog();
		log.setTimestamp(Instant.now());
		log.setUserId(userId);
		log.setUsername(username);
		log.setAction(action);
		log.setResource(resource);
		log.setResourceId(resourceId);
		log.setGranted(granted);
		log.setReason(reason);
		log.setIpAddress(ipAddress);
		this.template.save(log);
	}

	public List<AuditLog> queryLogs(@Nullable String username, @Nullable String action, @Nullable Instant startDate,
			@Nullable Instant endDate, @Nullable Boolean granted, int limit) {
		Assert.isTrue(limit > 0, "limit must be > 0");

		String cypher = "MATCH (n:_Security_AuditLog) WHERE 1=1";
		Map<String, Object> params = new HashMap<>();

		if (StringUtils.hasText(username)) {
			cypher += " AND n.username = $username";
			params.put("username", username);
		}
		if (StringUtils.hasText(action)) {
			cypher += " AND n.action = $action";
			params.put("action", action);
		}
		if (startDate != null) {
			cypher += " AND n.timestamp >= $startDate";
			params.put("startDate", startDate.toString());
		}
		if (endDate != null) {
			cypher += " AND n.timestamp <= $endDate";
			params.put("endDate", endDate.toString());
		}
		if (granted != null) {
			cypher += " AND n.granted = $granted";
			params.put("granted", granted);
		}

		cypher += " RETURN n ORDER BY n.timestamp DESC LIMIT $limit";
		params.put("limit", limit);

		return this.template.query(cypher, params, AuditLog.class);
	}

}
