package org.hibernate.assistant;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Remove;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.BindableType;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.spi.AbstractSelectionQuery;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

public class AiQuery<R> implements SelectionQuery<R> {
	private final String hql;
	private final Class<R> resultClass;
	private final Session session;

	private transient SelectionQuery<R> selectionQuery;

	private AiQuery(String hql, Class<R> resultClass, Session session) {
		this.hql = hql;
		this.resultClass = resultClass;
		this.session = session;
	}

	public static <T> AiQuery<T> from(String hql, Class<T> resultClass, Session session) {
		return new AiQuery<>( hql, resultClass, session );
	}

	private SelectionQuery<R> getSelectionQuery() {
		if ( selectionQuery == null ) {
			selectionQuery = session.createSelectionQuery( hql, resultClass );
		}
		return selectionQuery;
	}

	public Class<?> getResultType() {
		return ( (AbstractSelectionQuery<R>) getSelectionQuery() ).getResultType();
	}

	public SqmSelectStatement<R> getSqmStatement() {
		//noinspection unchecked
		return (SqmSelectStatement<R>) ( (SqmSelectionQuery<R>) getSelectionQuery() ).getSqmStatement();
	}

	public String getHql() {
		return hql;
	}

	@Override
	public List<R> getResultList() {
		return getSelectionQuery().getResultList();
	}

	@Override
	public List<R> list() {
		return getSelectionQuery().list();
	}

	@Override
	public ScrollableResults<R> scroll() {
		return getSelectionQuery().scroll();
	}

	@Override
	public ScrollableResults<R> scroll(ScrollMode scrollMode) {
		return getSelectionQuery().scroll( scrollMode );
	}

	@Override
	public Stream<R> getResultStream() {
		return getSelectionQuery().getResultStream();
	}

	@Override
	public Stream<R> stream() {
		return getSelectionQuery().stream();
	}

	@Override
	public R uniqueResult() {
		return getSelectionQuery().uniqueResult();
	}

	@Override
	public R getSingleResult() {
		return getSelectionQuery().getSingleResult();
	}

	@Override
	public R getSingleResultOrNull() {
		return getSelectionQuery().getSingleResultOrNull();
	}

	@Override
	public Optional<R> uniqueResultOptional() {
		return getSelectionQuery().uniqueResultOptional();
	}

	@Override
	public long getResultCount() {
		return getSelectionQuery().getResultCount();
	}

	@Override
	public KeyedResultList<R> getKeyedResultList(KeyedPage<R> page) {
		return getSelectionQuery().getKeyedResultList( page );
	}

	@Override
	public SelectionQuery<R> setHint(String hintName, Object value) {
		return getSelectionQuery().setHint( hintName, value );
	}

	@Override
	public SelectionQuery<R> setEntityGraph(EntityGraph<R> graph, GraphSemantic semantic) {
		return getSelectionQuery().setEntityGraph( graph, semantic );
	}

	@Override
	public SelectionQuery<R> enableFetchProfile(String profileName) {
		return getSelectionQuery().enableFetchProfile( profileName );
	}

	@Override
	public SelectionQuery<R> disableFetchProfile(String profileName) {
		return getSelectionQuery().disableFetchProfile( profileName );
	}

	@Override
	public SelectionQuery<R> setFlushMode(FlushModeType flushMode) {
		return getSelectionQuery().setFlushMode( flushMode );
	}

	@Override
	public SelectionQuery<R> setHibernateFlushMode(FlushMode flushMode) {
		return getSelectionQuery().setHibernateFlushMode( flushMode );
	}

	@Override
	public SelectionQuery<R> setTimeout(int timeout) {
		return getSelectionQuery().setTimeout( timeout );
	}

	@Override
	public SelectionQuery<R> setComment(String comment) {
		return getSelectionQuery().setComment( comment );
	}

	@Override
	public Integer getFetchSize() {
		return getSelectionQuery().getFetchSize();
	}

	@Override
	public SelectionQuery<R> setFetchSize(int fetchSize) {
		return getSelectionQuery().setFetchSize( fetchSize );
	}

	@Override
	public boolean isReadOnly() {
		return getSelectionQuery().isReadOnly();
	}

	@Override
	public SelectionQuery<R> setReadOnly(boolean readOnly) {
		return getSelectionQuery().setReadOnly( readOnly );
	}

	@Override
	public int getMaxResults() {
		return getSelectionQuery().getMaxResults();
	}

	@Override
	public SelectionQuery<R> setMaxResults(int maxResult) {
		return getSelectionQuery().setMaxResults( maxResult );
	}

	@Override
	public int getFirstResult() {
		return getSelectionQuery().getFirstResult();
	}

	@Override
	public SelectionQuery<R> setFirstResult(int startPosition) {
		return getSelectionQuery().setFirstResult( startPosition );
	}

	@Incubating
	@Override
	public SelectionQuery<R> setPage(Page page) {
		return getSelectionQuery().setPage( page );
	}

	@Override
	public CacheMode getCacheMode() {
		return getSelectionQuery().getCacheMode();
	}

	@Override
	public CacheStoreMode getCacheStoreMode() {
		return getSelectionQuery().getCacheStoreMode();
	}

	@Override
	public CacheRetrieveMode getCacheRetrieveMode() {
		return getSelectionQuery().getCacheRetrieveMode();
	}

	@Override
	public SelectionQuery<R> setCacheMode(CacheMode cacheMode) {
		return getSelectionQuery().setCacheMode( cacheMode );
	}

	@Override
	public SelectionQuery<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		return getSelectionQuery().setCacheStoreMode( cacheStoreMode );
	}

	@Override
	public SelectionQuery<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		return getSelectionQuery().setCacheRetrieveMode( cacheRetrieveMode );
	}

	@Override
	public boolean isCacheable() {
		return getSelectionQuery().isCacheable();
	}

	@Override
	public SelectionQuery<R> setCacheable(boolean cacheable) {
		return getSelectionQuery().setCacheable( cacheable );
	}

	@Override
	public boolean isQueryPlanCacheable() {
		return getSelectionQuery().isQueryPlanCacheable();
	}

	@Override
	public SelectionQuery<R> setQueryPlanCacheable(boolean queryPlanCacheable) {
		return getSelectionQuery().setQueryPlanCacheable( queryPlanCacheable );
	}

	@Override
	public String getCacheRegion() {
		return getSelectionQuery().getCacheRegion();
	}

	@Override
	public SelectionQuery<R> setCacheRegion(String cacheRegion) {
		return getSelectionQuery().setCacheRegion( cacheRegion );
	}

	@Override
	public LockOptions getLockOptions() {
		return getSelectionQuery().getLockOptions();
	}

	@Override
	public LockModeType getLockMode() {
		return getSelectionQuery().getLockMode();
	}

	@Override
	public SelectionQuery<R> setLockMode(LockModeType lockMode) {
		return getSelectionQuery().setLockMode( lockMode );
	}

	@Override
	public LockMode getHibernateLockMode() {
		return getSelectionQuery().getHibernateLockMode();
	}

	@Override
	public SelectionQuery<R> setHibernateLockMode(LockMode lockMode) {
		return getSelectionQuery().setHibernateLockMode( lockMode );
	}

	@Override
	public SelectionQuery<R> setLockMode(String alias, LockMode lockMode) {
		return getSelectionQuery().setLockMode( alias, lockMode );
	}

	@Incubating
	@Override
	public SelectionQuery<R> setOrder(List<Order<? super R>> orders) {
		return getSelectionQuery().setOrder( orders );
	}

	@Incubating
	@Override
	public SelectionQuery<R> setOrder(Order<? super R> order) {
		return getSelectionQuery().setOrder( order );
	}

	@Deprecated(since = "6.2")
	@Override
	public @Remove SelectionQuery<R> setAliasSpecificLockMode(String alias, LockMode lockMode) {
		return getSelectionQuery().setAliasSpecificLockMode( alias, lockMode );
	}

	@Override
	public SelectionQuery<R> setFollowOnLocking(boolean enable) {
		return getSelectionQuery().setFollowOnLocking( enable );
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Object value) {
		return getSelectionQuery().setParameter( name, value );
	}

	@Override
	public <P> SelectionQuery<R> setParameter(String name, P value, Class<P> type) {
		return getSelectionQuery().setParameter( name, value, type );
	}

	@Override
	public <P> SelectionQuery<R> setParameter(String name, P value, BindableType<P> type) {
		return getSelectionQuery().setParameter( name, value, type );
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Instant value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( name, value, temporalType );
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( name, value, temporalType );
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Date value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( name, value, temporalType );
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Object value) {
		return getSelectionQuery().setParameter( position, value );
	}

	@Override
	public <P> SelectionQuery<R> setParameter(int position, P value, Class<P> type) {
		return getSelectionQuery().setParameter( position, value, type );
	}

	@Override
	public <P> SelectionQuery<R> setParameter(int position, P value, BindableType<P> type) {
		return getSelectionQuery().setParameter( position, value, type );
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Instant value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( position, value, temporalType );
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Date value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( position, value, temporalType );
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( position, value, temporalType );
	}

	@Override
	public <T1> SelectionQuery<R> setParameter(QueryParameter<T1> parameter, T1 value) {
		return getSelectionQuery().setParameter( parameter, value );
	}

	@Override
	public <P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type) {
		return getSelectionQuery().setParameter( parameter, value, type );
	}

	@Override
	public <P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P val, BindableType<P> type) {
		return getSelectionQuery().setParameter( parameter, val, type );
	}

	@Override
	public <T1> SelectionQuery<R> setParameter(Parameter<T1> param, T1 value) {
		return getSelectionQuery().setParameter( param, value );
	}

	@Override
	public SelectionQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( param, value, temporalType );
	}

	@Override
	public SelectionQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( param, value, temporalType );
	}

	@Override
	public SelectionQuery<R> setParameterList(String name, Collection values) {
		return getSelectionQuery().setParameterList( name, values );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		return getSelectionQuery().setParameterList( name, values, javaType );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		return getSelectionQuery().setParameterList( name, values, type );
	}

	@Override
	public SelectionQuery<R> setParameterList(String name, Object[] values) {
		return getSelectionQuery().setParameterList( name, values );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, P[] values, Class<P> javaType) {
		return getSelectionQuery().setParameterList( name, values, javaType );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, P[] values, BindableType<P> type) {
		return getSelectionQuery().setParameterList( name, values, type );
	}

	@Override
	public SelectionQuery<R> setParameterList(int position, Collection values) {
		return getSelectionQuery().setParameterList( position, values );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		return getSelectionQuery().setParameterList( position, values, javaType );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		return getSelectionQuery().setParameterList( position, values, type );
	}

	@Override
	public SelectionQuery<R> setParameterList(int position, Object[] values) {
		return getSelectionQuery().setParameterList( position, values );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, P[] values, Class<P> javaType) {
		return getSelectionQuery().setParameterList( position, values, javaType );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, P[] values, BindableType<P> type) {
		return getSelectionQuery().setParameterList( position, values, type );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		return getSelectionQuery().setParameterList( parameter, values );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(
			QueryParameter<P> parameter,
			Collection<? extends P> values,
			Class<P> javaType) {
		return getSelectionQuery().setParameterList( parameter, values, javaType );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(
			QueryParameter<P> parameter,
			Collection<? extends P> values,
			BindableType<P> type) {
		return getSelectionQuery().setParameterList( parameter, values, type );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		return getSelectionQuery().setParameterList( parameter, values );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		return getSelectionQuery().setParameterList( parameter, values, javaType );
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		return getSelectionQuery().setParameterList( parameter, values, type );
	}

	@Override
	public SelectionQuery<R> setProperties(Object bean) {
		return getSelectionQuery().setProperties( bean );
	}

	@Override
	public SelectionQuery<R> setProperties(Map bean) {
		return getSelectionQuery().setProperties( bean );
	}

	@Override
	public String getComment() {
		return getSelectionQuery().getComment();
	}

	@Override
	public Integer getTimeout() {
		return getSelectionQuery().getTimeout();
	}

	@Override
	public FlushMode getHibernateFlushMode() {
		return getSelectionQuery().getHibernateFlushMode();
	}

	@Override
	public FlushModeType getFlushMode() {
		return getSelectionQuery().getFlushMode();
	}
}
