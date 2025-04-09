package org.hibernate.assistant;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Incubating;
import org.hibernate.Internal;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.query.BindableType;
import org.hibernate.query.KeyedPage;
import org.hibernate.query.KeyedResultList;
import org.hibernate.query.Order;
import org.hibernate.query.Page;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryFlushMode;
import org.hibernate.query.QueryParameter;
import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.SelectionQuery;
import org.hibernate.query.TupleTransformer;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.TemporalType;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class AiQuery<R> implements SelectionQuery<R> {
	private final String hql;
	private final Class<R> resultClass;
	private final String message;
	private final Session session;

	private transient SelectionQuery<R> selectionQuery;

	private AiQuery(String hql, Class<R> resultClass, String message, Session session) {
		this.hql = hql;
		this.resultClass = resultClass;
		this.message = message;
		this.session = session;
	}

	public static <T> AiQuery<T> from(String hql, Class<T> resultClass, String message, Session session) {
		return new AiQuery<>( hql, resultClass, message, session );
	}

	private SelectionQuery<R> getSelectionQuery() {
		if ( selectionQuery == null ) {
			selectionQuery = session.createSelectionQuery( hql, resultClass );
		}
		return selectionQuery;
	}

	public String getHql() {
		return hql;
	}

	public String getMessage() {
		return message;
	}

	@Internal
	public SqmSelectStatement<R> getSqmStatement() {
		//noinspection unchecked
		return (SqmSelectStatement<R>) ( (SqmSelectionQuery<R>) getSelectionQuery() ).getSqmStatement();
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
		getSelectionQuery().setHint( hintName, value );
		return this;
	}

	@Override
	public ParameterMetadata getParameterMetadata() {
		return getSelectionQuery().getParameterMetadata();
	}

	@Override
	public SelectionQuery<R> setEntityGraph(EntityGraph<? super R> graph, GraphSemantic semantic) {
		getSelectionQuery().setEntityGraph( graph, semantic );
		return this;
	}

	@Override
	public QueryFlushMode getQueryFlushMode() {
		return getSelectionQuery().getQueryFlushMode();
	}

	@Override
	public SelectionQuery<R> setQueryFlushMode(QueryFlushMode queryFlushMode) {
		getSelectionQuery().setQueryFlushMode( queryFlushMode );
		return this;
	}

	@Override
	public SelectionQuery<R> addRestriction(Restriction<? super R> restriction) {
		getSelectionQuery().addRestriction( restriction );
		return this;
	}

	@Override
	public SelectionQuery<R> enableFetchProfile(String profileName) {
		getSelectionQuery().enableFetchProfile( profileName );
		return this;
	}

	@Override
	public SelectionQuery<R> disableFetchProfile(String profileName) {
		getSelectionQuery().disableFetchProfile( profileName );
		return this;
	}

	@Override
	public SelectionQuery<R> setFlushMode(FlushModeType flushMode) {
		getSelectionQuery().setFlushMode( flushMode );
		return this;
	}

	@Override
	public SelectionQuery<R> setHibernateFlushMode(FlushMode flushMode) {
		getSelectionQuery().setHibernateFlushMode( flushMode );
		return this;
	}

	@Override
	public SelectionQuery<R> setTimeout(int timeout) {
		getSelectionQuery().setTimeout( timeout );
		return this;
	}

	@Override
	public SelectionQuery<R> setComment(String comment) {
		getSelectionQuery().setComment( comment );
		return this;
	}

	@Override
	public Integer getFetchSize() {
		return getSelectionQuery().getFetchSize();
	}

	@Override
	public SelectionQuery<R> setFetchSize(int fetchSize) {
		getSelectionQuery().setFetchSize( fetchSize );
		return this;
	}

	@Override
	public boolean isReadOnly() {
		return getSelectionQuery().isReadOnly();
	}

	@Override
	public SelectionQuery<R> setReadOnly(boolean readOnly) {
		getSelectionQuery().setReadOnly( readOnly );
		return this;
	}

	@Override
	public int getMaxResults() {
		return getSelectionQuery().getMaxResults();
	}

	@Override
	public SelectionQuery<R> setMaxResults(int maxResult) {
		getSelectionQuery().setMaxResults( maxResult );
		return this;
	}

	@Override
	public int getFirstResult() {
		return getSelectionQuery().getFirstResult();
	}

	@Override
	public SelectionQuery<R> setFirstResult(int startPosition) {
		getSelectionQuery().setFirstResult( startPosition );
		return this;
	}

	@Incubating
	@Override
	public SelectionQuery<R> setPage(Page page) {
		getSelectionQuery().setPage( page );
		return this;
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
		getSelectionQuery().setCacheMode( cacheMode );
		return this;
	}

	@Override
	public SelectionQuery<R> setCacheStoreMode(CacheStoreMode cacheStoreMode) {
		getSelectionQuery().setCacheStoreMode( cacheStoreMode );
		return this;
	}

	@Override
	public SelectionQuery<R> setCacheRetrieveMode(CacheRetrieveMode cacheRetrieveMode) {
		getSelectionQuery().setCacheRetrieveMode( cacheRetrieveMode );
		return this;
	}

	@Override
	public boolean isCacheable() {
		return getSelectionQuery().isCacheable();
	}

	@Override
	public SelectionQuery<R> setCacheable(boolean cacheable) {
		getSelectionQuery().setCacheable( cacheable );
		return this;
	}

	@Override
	public boolean isQueryPlanCacheable() {
		return getSelectionQuery().isQueryPlanCacheable();
	}

	@Override
	public SelectionQuery<R> setQueryPlanCacheable(boolean queryPlanCacheable) {
		getSelectionQuery().setQueryPlanCacheable( queryPlanCacheable );
		return this;
	}

	@Override
	public String getCacheRegion() {
		return getSelectionQuery().getCacheRegion();
	}

	@Override
	public SelectionQuery<R> setCacheRegion(String cacheRegion) {
		getSelectionQuery().setCacheRegion( cacheRegion );
		return this;
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
		getSelectionQuery().setLockMode( lockMode );
		return this;
	}

	@Override
	public LockMode getHibernateLockMode() {
		return getSelectionQuery().getHibernateLockMode();
	}

	@Override
	public SelectionQuery<R> setHibernateLockMode(LockMode lockMode) {
		getSelectionQuery().setHibernateLockMode( lockMode );
		return this;
	}

	@Override
	public SelectionQuery<R> setLockMode(String alias, LockMode lockMode) {
		getSelectionQuery().setLockMode( alias, lockMode );
		return this;
	}

	@Override
	public SelectionQuery<R> setOrder(List<? extends Order<? super R>> orderList) {
		getSelectionQuery().setOrder( orderList );
		return this;
	}

	@Incubating
	@Override
	public SelectionQuery<R> setOrder(Order<? super R> order) {
		return getSelectionQuery().setOrder( order );
	}

	@Override
	public SelectionQuery<R> setFollowOnLocking(boolean enable) {
		return getSelectionQuery().setFollowOnLocking( enable );
	}

	@Override
	public <T> SelectionQuery<T> setTupleTransformer(TupleTransformer<T> transformer) {
		return getSelectionQuery().setTupleTransformer( transformer );
	}

	@Override
	public SelectionQuery<R> setResultListTransformer(ResultListTransformer<R> transformer) {
		getSelectionQuery().setResultListTransformer( transformer );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Object value) {
		getSelectionQuery().setParameter( name, value );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(String name, P value, Class<P> type) {
		getSelectionQuery().setParameter( name, value, type );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(String name, P value, BindableType<P> type) {
		getSelectionQuery().setParameter( name, value, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Instant value, TemporalType temporalType) {
		getSelectionQuery().setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Calendar value, TemporalType temporalType) {
		getSelectionQuery().setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(String name, Date value, TemporalType temporalType) {
		getSelectionQuery().setParameter( name, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Object value) {
		getSelectionQuery().setParameter( position, value );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(int position, P value, Class<P> type) {
		getSelectionQuery().setParameter( position, value, type );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(int position, P value, BindableType<P> type) {
		getSelectionQuery().setParameter( position, value, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Instant value, TemporalType temporalType) {
		getSelectionQuery().setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Date value, TemporalType temporalType) {
		getSelectionQuery().setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(int position, Calendar value, TemporalType temporalType) {
		getSelectionQuery().setParameter( position, value, temporalType );
		return this;
	}

	@Override
	public <T1> SelectionQuery<R> setParameter(QueryParameter<T1> parameter, T1 value) {
		getSelectionQuery().setParameter( parameter, value );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P value, Class<P> type) {
		getSelectionQuery().setParameter( parameter, value, type );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameter(QueryParameter<P> parameter, P val, BindableType<P> type) {
		getSelectionQuery().setParameter( parameter, val, type );
		return this;
	}

	@Override
	public <T1> SelectionQuery<R> setParameter(Parameter<T1> param, T1 value) {
		getSelectionQuery().setParameter( param, value );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(Parameter<Calendar> param, Calendar value, TemporalType temporalType) {
		getSelectionQuery().setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameter(Parameter<Date> param, Date value, TemporalType temporalType) {
		getSelectionQuery().setParameter( param, value, temporalType );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(String name, Collection values) {
		getSelectionQuery().setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, Class<P> javaType) {
		getSelectionQuery().setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, Collection<? extends P> values, BindableType<P> type) {
		getSelectionQuery().setParameterList( name, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(String name, Object[] values) {
		getSelectionQuery().setParameterList( name, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, P[] values, Class<P> javaType) {
		getSelectionQuery().setParameterList( name, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(String name, P[] values, BindableType<P> type) {
		getSelectionQuery().setParameterList( name, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(int position, Collection values) {
		getSelectionQuery().setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, Class<P> javaType) {
		getSelectionQuery().setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, Collection<? extends P> values, BindableType<P> type) {
		getSelectionQuery().setParameterList( position, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setParameterList(int position, Object[] values) {
		getSelectionQuery().setParameterList( position, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, P[] values, Class<P> javaType) {
		getSelectionQuery().setParameterList( position, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(int position, P[] values, BindableType<P> type) {
		getSelectionQuery().setParameterList( position, values, type );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, Collection<? extends P> values) {
		getSelectionQuery().setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(
			QueryParameter<P> parameter,
			Collection<? extends P> values,
			Class<P> javaType) {
		getSelectionQuery().setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(
			QueryParameter<P> parameter,
			Collection<? extends P> values,
			BindableType<P> type) {
		getSelectionQuery().setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values) {
		getSelectionQuery().setParameterList( parameter, values );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, Class<P> javaType) {
		getSelectionQuery().setParameterList( parameter, values, javaType );
		return this;
	}

	@Override
	public <P> SelectionQuery<R> setParameterList(QueryParameter<P> parameter, P[] values, BindableType<P> type) {
		getSelectionQuery().setParameterList( parameter, values, type );
		return this;
	}

	@Override
	public SelectionQuery<R> setProperties(Object bean) {
		getSelectionQuery().setProperties( bean );
		return this;
	}

	@Override
	public SelectionQuery<R> setProperties(Map bean) {
		getSelectionQuery().setProperties( bean );
		return this;
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
