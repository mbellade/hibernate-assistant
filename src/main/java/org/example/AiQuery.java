package org.example;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.SelectionQuery;

public class AiQuery<T> {
	private final String hql;
	private final Class<T> resultClass;
	private final Session session;

	private AiQuery(String hql, Class<T> resultClass, Session session) {
		this.hql = hql;
		this.resultClass = resultClass;
		this.session = session;
	}

	public String getHql() {
		return hql;
	}

	public static <T> AiQuery<T> from(String hql, Class<T> resultClass, Session session) {
		return new AiQuery<>( hql, resultClass, session );
	}

	public List<T> getResultList() {
		return session.createSelectionQuery( hql, resultClass ).getResultList();
	}

	public T getSingleResult() {
		return session.createSelectionQuery( hql, resultClass ).getSingleResult();
	}

	public SelectionQuery<T> createSelectionQuery() {
		return session.createSelectionQuery( hql, resultClass );
	}
}
