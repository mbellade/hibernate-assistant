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

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;

public class AiQuery<T> implements SelectionQuery<T> {
	private final String hql;
	private final Class<T> resultClass;
	private final Session session;

	private transient SelectionQuery<T> selectionQuery;

	private AiQuery(String hql, Class<T> resultClass, Session session) {
		this.hql = hql;
		this.resultClass = resultClass;
		this.session = session;
	}

	public static <T> AiQuery<T> from(String hql, Class<T> resultClass, Session session) {
		return new AiQuery<>( hql, resultClass, session );
	}

	private SelectionQuery<T> getSelectionQuery() {
		if ( selectionQuery == null ) {
			selectionQuery = session.createSelectionQuery( hql, resultClass );
		}
		return selectionQuery;
	}

	public Class<?> getResultType() {
		return ( (AbstractSelectionQuery<T>) getSelectionQuery() ).getResultType();
	}

	public String getHql() {
		return hql;
	}

	@Override
	public List<T> getResultList() {
		return getSelectionQuery().getResultList();
	}

	@Override
	public List<T> list() {
		return getSelectionQuery().list();
	}

	@Override
	public ScrollableResults<T> scroll() {
		return getSelectionQuery().scroll();
	}

	@Override
	public ScrollableResults<T> scroll(ScrollMode scrollMode) {
		return getSelectionQuery().scroll( scrollMode );
	}

	@Override
	public Stream<T> getResultStream() {
		return getSelectionQuery().getResultStream();
	}

	@Override
	public Stream<T> stream() {
		return getSelectionQuery().stream();
	}

	@Override
	public T uniqueResult() {
		return getSelectionQuery().uniqueResult();
	}

	@Override
	public T getSingleResult() {
		return getSelectionQuery().getSingleResult();
	}

	@Override
	public T getSingleResultOrNull() {
		return getSelectionQuery().getSingleResultOrNull();
	}

	@Override
	public Optional<T> uniqueResultOptional() {
		return getSelectionQuery().uniqueResultOptional();
	}

	@Override
	public long getResultCount() {
		return getSelectionQuery().getResultCount();
	}

	@Override
	public KeyedResultList<T> getKeyedResultList(KeyedPage<T> page) {
		return getSelectionQuery().getKeyedResultList( page );
	}

	@Override
	public SelectionQuery<T> setHint(String hintName, Object value) {
		return getSelectionQuery().setHint( hintName, value );
	}

	@Override
	public SelectionQuery<T> setEntityGraph(EntityGraph<T> graph, GraphSemantic semantic) {
		return getSelectionQuery().setEntityGraph( graph, semantic );
	}

	@Override
	public SelectionQuery<T> enableFetchProfile(String profileName) {
		return getSelectionQuery().enableFetchProfile( profileName );
	}

	@Override
	public SelectionQuery<T> disableFetchProfile(String profileName) {
		return getSelectionQuery().disableFetchProfile( profileName );
	}

	@Override
	public SelectionQuery<T> setFlushMode(FlushModeType flushMode) {
		return getSelectionQuery().setFlushMode( flushMode );
	}

	@Override
	public SelectionQuery<T> setHibernateFlushMode(FlushMode flushMode) {
		return getSelectionQuery().setHibernateFlushMode( flushMode );
	}

	@Override
	public SelectionQuery<T> setTimeout(int timeout) {
		return getSelectionQuery().setTimeout( timeout );
	}

	@Override
	public SelectionQuery<T> setComment(String comment) {
		return getSelectionQuery().setComment( comment );
	}

	@Override
	public Integer getFetchSize() {
		return getSelectionQuery().getFetchSize();
	}

	@Override
	public SelectionQuery<T> setFetchSize(int fetchSize) {
		return getSelectionQuery().setFetchSize( fetchSize );
	}

	@Override
	public boolean isReadOnly() {
		return getSelectionQuery().isReadOnly();
	}

	@Override
	public SelectionQuery<T> setReadOnly(boolean readOnly) {
		return getSelectionQuery().setReadOnly( readOnly );
	}

	@Override
	public int getMaxResults() {
		return getSelectionQuery().getMaxResults();
	}

	@Override
	public SelectionQuery<T> setMaxResults(int maxResult) {
		return getSelectionQuery().setMaxResults( maxResult );
	}

	@Override
	public int getFirstResult() {
		return getSelectionQuery().getFirstResult();
	}

	@Override
	public SelectionQuery<T> setFirstResult(int startPosition) {
		return getSelectionQuery().setFirstResult( startPosition );
	}

	@Incubating
	@Override
	public SelectionQuery<T> setPage(Page page) {
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
	public SelectionQuery<T> setCacheMode(CacheMode cacheMode) {
		return getSelectionQuery().setCacheMode( cacheMode );
	}

	@Override
	public SelectionQuery<T> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		return getSelectionQuery().setCacheStoreMode( cacheStoreMode );
	}

	@Override
	public SelectionQuery<T> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		return getSelectionQuery().setCacheRetrieveMode( cacheRetrieveMode );
	}

	@Override
	public boolean isCacheable() {
		return getSelectionQuery().isCacheable();
	}

	@Override
	public SelectionQuery<T> setCacheable(boolean cacheable) {
		return getSelectionQuery().setCacheable( cacheable );
	}

	@Override
	public boolean isQueryPlanCacheable() {
		return getSelectionQuery().isQueryPlanCacheable();
	}

	@Override
	public SelectionQuery<T> setQueryPlanCacheable(boolean queryPlanCacheable) {
		return getSelectionQuery().setQueryPlanCacheable( queryPlanCacheable );
	}

	@Override
	public String getCacheRegion() {
		return getSelectionQuery().getCacheRegion();
	}

	@Override
	public SelectionQuery<T> setCacheRegion(String cacheRegion) {
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
	public SelectionQuery<T> setLockMode(LockModeType lockMode) {
		return getSelectionQuery().setLockMode( lockMode );
	}

	@Override
	public LockMode getHibernateLockMode() {
		return getSelectionQuery().getHibernateLockMode();
	}

	@Override
	public SelectionQuery<T> setHibernateLockMode(LockMode lockMode) {
		return getSelectionQuery().setHibernateLockMode( lockMode );
	}

	@Override
	public SelectionQuery<T> setLockMode(String alias, LockMode lockMode) {
		return getSelectionQuery().setLockMode( alias, lockMode );
	}

	@Incubating
	@Override
	public SelectionQuery<T> setOrder(List<Order<? super T>> orders) {
		return getSelectionQuery().setOrder( orders );
	}

	@Incubating
	@Override
	public SelectionQuery<T> setOrder(Order<? super T> order) {
		return getSelectionQuery().setOrder( order );
	}

	@Deprecated(since = "6.2")
	@Override
	public @Remove SelectionQuery<T> setAliasSpecificLockMode(String alias, LockMode lockMode) {
		return getSelectionQuery().setAliasSpecificLockMode( alias, lockMode );
	}

	@Override
	public SelectionQuery<T> setFollowOnLocking(boolean enable) {
		return getSelectionQuery().setFollowOnLocking( enable );
	}

	@Override
	public SelectionQuery<T> setParameter(String name, Object value) {
		return getSelectionQuery().setParameter( name, value );
	}

	@Override
	public <P> SelectionQuery<T> setParameter(String name, P value, Class<P> type) {
		return getSelectionQuery().setParameter( name, value, type );
	}

	@Override
	public <P> SelectionQuery<T> setParameter(String name, P value, BindableType<P> type) {
		return getSelectionQuery().setParameter( name, value, type );
	}

	@Override
	public SelectionQuery<T> setParameter(String name, Instant value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( name, value, temporalType );
	}

	@Override
	public SelectionQuery<T> setParameter(String name, Calendar value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( name, value, temporalType );
	}

	@Override
	public SelectionQuery<T> setParameter(String name, Date value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( name, value, temporalType );
	}

	@Override
	public SelectionQuery<T> setParameter(int position, Object value) {
		return getSelectionQuery().setParameter( position, value );
	}

	@Override
	public <P> SelectionQuery<T> setParameter(int position, P value, Class<P> type) {
		return getSelectionQuery().setParameter( position, value, type );
	}

	@Override
	public <P> SelectionQuery<T> setParameter(int position, P value, BindableType<P> type) {
		return getSelectionQuery().setParameter( position, value, type );
	}

	@Override
	public SelectionQuery<T> setParameter(int position, Instant value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( position, value, temporalType );
	}

	@Override
	public SelectionQuery<T> setParameter(int position, Date value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( position, value, temporalType );
	}

	@Override
	public SelectionQuery<T> setParameter(int position, Calendar value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( position, value, temporalType );
	}

	@Override
	public <T1> SelectionQuery<T> setParameter(QueryParameter<T1> parameter, T1 value) {
		return getSelectionQuery().setParameter( parameter, value );
	}

	@Override
	public <P> SelectionQuery<T> setParameter(QueryParameter<P> parameter, P value, Class<P> type) {
		return getSelectionQuery().setParameter( parameter, value, type );
	}

	@Override
	public <P> SelectionQuery<T> setParameter(QueryParameter<P> parameter, P val, BindableType<P> type) {
		return getSelectionQuery().setParameter( parameter, val, type );
	}

	@Override
	public <T1> SelectionQuery<T> setParameter(Parameter<T1> param, T1 value) {
		return getSelectionQuery().setParameter( param, value );
	}

	@Override
	public SelectionQuery<T> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( param, value, temporalType );
	}

	@Override
	public SelectionQuery<T> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		return getSelectionQuery().setParameter( param, value, temporalType );
	}

	@Override
	public SelectionQuery<T> setParameterList(String name, Collection values) {
		return getSelectionQuery().setParameterList( name, values );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		return getSelectionQuery().setParameterList( name, values, javaType );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		return getSelectionQuery().setParameterList( name, values, type );
	}

	@Override
	public SelectionQuery<T> setParameterList(String name, Object[] values) {
		return getSelectionQuery().setParameterList( name, values );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(String name, P[] values, Class<P> javaType) {
		return getSelectionQuery().setParameterList( name, values, javaType );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(String name, P[] values, BindableType<P> type) {
		return getSelectionQuery().setParameterList( name, values, type );
	}

	@Override
	public SelectionQuery<T> setParameterList(int position, Collection values) {
		return getSelectionQuery().setParameterList( position, values );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		return getSelectionQuery().setParameterList( position, values, javaType );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		return getSelectionQuery().setParameterList( position, values, type );
	}

	@Override
	public SelectionQuery<T> setParameterList(int position, Object[] values) {
		return getSelectionQuery().setParameterList( position, values );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(int position, P[] values, Class<P> javaType) {
		return getSelectionQuery().setParameterList( position, values, javaType );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(int position, P[] values, BindableType<P> type) {
		return getSelectionQuery().setParameterList( position, values, type );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		return getSelectionQuery().setParameterList( parameter, values );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(
			QueryParameter<P> parameter,
			Collection<? extends P> values,
			Class<P> javaType) {
		return getSelectionQuery().setParameterList( parameter, values, javaType );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(
			QueryParameter<P> parameter,
			Collection<? extends P> values,
			BindableType<P> type) {
		return getSelectionQuery().setParameterList( parameter, values, type );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(QueryParameter<P> parameter, P[] values) {
		return getSelectionQuery().setParameterList( parameter, values );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		return getSelectionQuery().setParameterList( parameter, values, javaType );
	}

	@Override
	public <P> SelectionQuery<T> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		return getSelectionQuery().setParameterList( parameter, values, type );
	}

	@Override
	public SelectionQuery<T> setProperties(Object bean) {
		return getSelectionQuery().setProperties( bean );
	}

	@Override
	public SelectionQuery<T> setProperties(Map bean) {
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
