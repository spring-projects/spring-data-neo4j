/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.repository;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.falkordb.core.FalkorDBOperations;
import org.springframework.data.falkordb.repository.support.FalkorDBEntityInformation;
import org.springframework.data.falkordb.repository.support.SimpleFalkorDBRepository;
import org.springframework.data.falkordb.security.annotation.SecurityMetadata;
import org.springframework.data.falkordb.security.audit.AuditLogger;
import org.springframework.data.falkordb.security.annotation.SecurityMetadataUtils;
import org.springframework.data.falkordb.security.context.FalkorSecurityContext;
import org.springframework.data.falkordb.security.context.FalkorSecurityContextHolder;
import org.springframework.data.falkordb.security.model.Action;

/**
 * Security-aware repository that delegates to {@link SimpleFalkorDBRepository}
 * but enforces RBAC checks using {@link FalkorSecurityContext} and
 * {@link org.springframework.data.falkordb.security.annotation.Secured}
 * metadata on the domain type.
 *
 * @param <T> domain type
 * @param <ID> id type
 */
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public class SecureFalkorDBRepository<T, ID>
		implements org.springframework.data.falkordb.repository.FalkorDBRepository<T, ID> {

	private final Class<T> domainType;

	private final SecurityMetadata metadata;

	private final AuditLogger auditLogger;

	private final org.springframework.data.falkordb.repository.FalkorDBRepository<T, ID> delegate;

	public SecureFalkorDBRepository(FalkorDBOperations operations,
			FalkorDBEntityInformation<T, ID> entityInformation,
			@org.springframework.lang.Nullable AuditLogger auditLogger) {
		this.domainType = entityInformation.getJavaType();
		this.metadata = SecurityMetadataUtils.resolveMetadata(this.domainType);
		this.delegate = new DelegateRepository<>(operations, entityInformation);
		this.auditLogger = auditLogger;
	}

	private static final class DelegateRepository<T, ID> extends SimpleFalkorDBRepository<T, ID> {

		DelegateRepository(FalkorDBOperations operations, FalkorDBEntityInformation<T, ID> entityInformation) {
			super(operations, entityInformation);
		}
	}

	// --- Public CRUD operations with security checks ---

	@Override
	public Optional<T> findById(ID id) {
		require(Action.READ);
		Optional<T> result = delegate.findById(id);
		return result.map(this::applyReadPropertyMasking);
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {
		require(Action.READ);
		List<T> results = delegate.findAllById(ids);
		return applyReadPropertyMasking(results);
	}

	@Override
	public List<T> findAll() {
		require(Action.READ);
		List<T> results = delegate.findAll();
		return applyReadPropertyMasking(results);
	}

	@Override
	public List<T> findAll(Sort sort) {
		require(Action.READ);
		List<T> results = delegate.findAll(sort);
		return applyReadPropertyMasking(results);
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		require(Action.READ);
		Page<T> page = delegate.findAll(pageable);
		List<T> masked = applyReadPropertyMasking(page.getContent());
		return new SimplePageImpl<>(masked, pageable, page.getTotalElements());
	}

	@Override
	public long count() {
		require(Action.READ);
		return delegate.count();
	}

	@Override
	public boolean existsById(ID id) {
		require(Action.READ);
		return delegate.existsById(id);
	}

	@Override
	public <S extends T> S save(S entity) {
		// MVP: treat all save operations as WRITE
		require(Action.WRITE);
		validateWrite(entity);
		return delegate.save(entity);
	}

	@Override
	public <S extends T> List<S> saveAll(Iterable<S> entities) {
		require(Action.WRITE);
		List<S> list = new ArrayList<>();
		for (S entity : entities) {
			validateWrite(entity);
			list.add(entity);
		}
		return delegate.saveAll(list);
	}

	@Override
	public void deleteById(ID id) {
		require(Action.DELETE);
		delegate.deleteById(id);
	}

	@Override
	public void delete(T entity) {
		require(Action.DELETE);
		delegate.delete(entity);
	}

	@Override
	public void deleteAllById(Iterable<? extends ID> ids) {
		require(Action.DELETE);
		delegate.deleteAllById(ids);
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {
		require(Action.DELETE);
		delegate.deleteAll(entities);
	}

	@Override
	public void deleteAll() {
		require(Action.DELETE);
		delegate.deleteAll();
	}

	// Query-by-example / fluent queries are left as-is for now and will
	// go through the same READ/WRITE gates when implemented.

	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {
		require(Action.READ);
		return this.delegate.findOne(example);
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example) {
		require(Action.READ);
		return this.delegate.findAll(example);
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
		require(Action.READ);
		return this.delegate.findAll(example, sort);
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		require(Action.READ);
		return this.delegate.findAll(example, pageable);
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		require(Action.READ);
		return this.delegate.count(example);
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		require(Action.READ);
		return this.delegate.exists(example);
	}

	@Override
	public <S extends T, R> R findBy(Example<S> example,
			java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
		require(Action.READ);
		return this.delegate.findBy(example, queryFunction);
	}

	// --- Internal helpers ---

	private void require(Action action) {
		FalkorSecurityContext ctx = FalkorSecurityContextHolder.getContext();
		if (ctx == null) {
			throw new IllegalStateException("No FalkorSecurityContext set for current thread");
		}

		String resourceKey = resourceKey();
		boolean granted = ctx.can(action, resourceKey);
		if (!granted) {
			logDecision(ctx, action, resourceKey, false, "permission check failed");
			throw new SecurityException("Access denied for action " + action + " on resource " + resourceKey);
		}
		logDecision(ctx, action, resourceKey, true, null);
	}

	private String resourceKey() {
		return this.domainType.getName();
	}

	private List<T> applyReadPropertyMasking(List<T> results) {
		if (results == null || results.isEmpty()) {
			return results;
		}
		List<T> masked = new ArrayList<>(results.size());
		for (T entity : results) {
			T filtered = applyRowLevelFilter(entity);
			if (filtered != null) {
				masked.add(applyReadPropertyMasking(filtered));
			}
		}
		return Collections.unmodifiableList(masked);
	}

	private T applyRowLevelFilter(T entity) {
		if (entity == null) {
			return null;
		}
		String filter = this.metadata.getRowFilterExpression();
		if (filter == null || filter.isEmpty()) {
			return entity;
		}
		FalkorSecurityContext ctx = FalkorSecurityContextHolder.getContext();
		if (ctx == null || ctx.getUser() == null) {
			// No security context or user: treat as denied for RLS-protected entities
			return null;
		}
		// MVP: support simple pattern "owner == principal.username"
		if ("owner == principal.username".equals(filter)) {
			Object ownerValue = getFieldValue(entity, "owner");
			String username = ctx.getUser().getUsername();
			if (ownerValue != null && ownerValue.toString().equals(username)) {
				return entity;
			}
			// Not owned by current user
			logDecision(ctx, Action.READ, resourceKey(), false, "row-level filter denied");
			return null;
		}
		// Unknown filter expression: allow for now
		return entity;
	}

	@SuppressWarnings("unchecked")
	private T applyReadPropertyMasking(T entity) {
		if (entity == null) {
			return null;
		}
		FalkorSecurityContext ctx = FalkorSecurityContextHolder.getContext();
		if (ctx == null) {
			return entity;
		}

		Map<String, Set<String>> denied = this.metadata.getDeniedPropertiesFor(Action.READ);
		if (denied.isEmpty()) {
			return entity;
		}

		T copy = entity;
		for (Map.Entry<String, Set<String>> entry : denied.entrySet()) {
			String propertyName = entry.getKey();
			Set<String> roles = entry.getValue();
			if (isDeniedForAnyRole(ctx, roles)) {
				nullOutField(copy, propertyName);
			}
		}
		return copy;
	}

	private void validateWrite(T entity) {
		if (entity == null) {
			return;
		}
		FalkorSecurityContext ctx = FalkorSecurityContextHolder.getContext();
		if (ctx == null) {
			return;
		}

		Map<String, Set<String>> denied = this.metadata.getDeniedPropertiesFor(Action.WRITE);
		if (denied.isEmpty()) {
			return;
		}

		for (Map.Entry<String, Set<String>> entry : denied.entrySet()) {
			String propertyName = entry.getKey();
			Set<String> roles = entry.getValue();
			if (isDeniedForAnyRole(ctx, roles) && fieldHasNonNullValue(entity, propertyName)) {
				throw new SecurityException("Write access to property '" + propertyName + "' is denied");
			}
		}
	}

	private boolean isDeniedForAnyRole(FalkorSecurityContext ctx, Set<String> roles) {
		for (String role : roles) {
			if (ctx.getEffectiveRoles().contains(role)) {
				return true;
			}
		}
		return false;
	}

	private void nullOutField(Object target, String fieldName) {
		Field field = findField(target.getClass(), fieldName);
		if (field == null) {
			return;
		}
		boolean accessible = field.canAccess(target);
		try {
			if (!accessible) {
				field.setAccessible(true);
			}
			field.set(target, null);
		}
		catch (IllegalAccessException ignored) {
			// Ignore and leave field as-is
		}
		finally {
			if (!accessible) {
				field.setAccessible(false);
			}
		}
	}

	private boolean fieldHasNonNullValue(Object target, String fieldName) {
		Object value = getFieldValue(target, fieldName);
		return value != null;
	}

	private Object getFieldValue(Object target, String fieldName) {
		Field field = findField(target.getClass(), fieldName);
		if (field == null) {
			return null;
		}
		boolean accessible = field.canAccess(target);
		try {
			if (!accessible) {
				field.setAccessible(true);
			}
			return field.get(target);
		}
		catch (IllegalAccessException e) {
			return null;
		}
		finally {
			if (!accessible) {
				field.setAccessible(false);
			}
		}
	}

	private void logDecision(FalkorSecurityContext ctx, Action action, String resourceKey,
			boolean granted, String reason) {
		if (this.auditLogger == null) {
			return;
		}
		String username = ctx.getUser() != null ? ctx.getUser().getUsername() : null;
		this.auditLogger.log(username, action.name(), resourceKey, granted, reason);
	}

	private Field findField(Class<?> type, String name) {
		Objects.requireNonNull(type, "type must not be null");
		Objects.requireNonNull(name, "name must not be null");
		Class<?> current = type;
		while (current != null && current != Object.class) {
			for (Field field : current.getDeclaredFields()) {
				if (field.getName().equals(name)) {
					return field;
				}
			}
			current = current.getSuperclass();
		}
		return null;
	}

	/**
	 * Simple Page implementation to wrap masked content.
	 */
	private static final class SimplePageImpl<T> implements Page<T> {

		private final List<T> content;

		private final Pageable pageable;

		private final long total;

		SimplePageImpl(List<T> content, Pageable pageable, long total) {
			this.content = content;
			this.pageable = pageable;
			this.total = total;
		}

		@Override
		public int getTotalPages() {
			return (int) Math.ceil((double) this.total / (double) getSize());
		}

		@Override
		public long getTotalElements() {
			return this.total;
		}

		@Override
		public <S> Page<S> map(java.util.function.Function<? super T, ? extends S> converter) {
			List<S> mapped = new ArrayList<>(this.content.size());
			for (T t : this.content) {
				mapped.add(converter.apply(t));
			}
			return new SimplePageImpl<>(mapped, this.pageable, this.total);
		}

		@Override
		public int getNumber() {
			return this.pageable.getPageNumber();
		}

		@Override
		public int getSize() {
			return this.pageable.getPageSize();
		}

		@Override
		public int getNumberOfElements() {
			return this.content.size();
		}

		@Override
		public List<T> getContent() {
			return this.content;
		}

		@Override
		public boolean hasContent() {
			return !this.content.isEmpty();
		}

		@Override
		public Sort getSort() {
			return this.pageable.getSort();
		}

		@Override
		public boolean isFirst() {
			return !hasPrevious();
		}

		@Override
		public boolean isLast() {
			return !hasNext();
		}

		@Override
		public boolean hasNext() {
			return getNumber() + 1 < getTotalPages();
		}

		@Override
		public boolean hasPrevious() {
			return getNumber() > 0;
		}

		@Override
		public Pageable nextPageable() {
			return hasNext() ? this.pageable.next() : Pageable.unpaged();
		}

		@Override
		public Pageable previousPageable() {
			return hasPrevious() ? this.pageable.previousOrFirst() : Pageable.unpaged();
		}

		@Override
		public java.util.Iterator<T> iterator() {
			return this.content.iterator();
		}

	}

}
