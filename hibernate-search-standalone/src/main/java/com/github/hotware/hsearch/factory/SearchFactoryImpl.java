package com.github.hotware.hsearch.factory;

import java.io.IOException;
import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.DeletionQuery;
import org.hibernate.search.backend.spi.DeleteByQueryWork;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engine.impl.FilterDef;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.filter.FilterCachingStrategy;
import org.hibernate.search.indexes.IndexReaderAccessor;
import org.hibernate.search.query.dsl.QueryContextBuilder;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.stat.Statistics;

import com.github.hotware.hsearch.dto.HibernateSearchQueryExecutor;
import com.github.hotware.hsearch.query.HSearchQuery;
import com.github.hotware.hsearch.query.HSearchQueryImpl;
import com.github.hotware.hsearch.transaction.TransactionContext;

public class SearchFactoryImpl implements SearchFactory {

	private final ExtendedSearchIntegrator searchIntegrator;
	private final HibernateSearchQueryExecutor queryExec;

	public SearchFactoryImpl(ExtendedSearchIntegrator searchIntegrator) {
		super();
		this.searchIntegrator = searchIntegrator;
		this.queryExec = new HibernateSearchQueryExecutor();
	}

	@Override
	public void index(Iterable<?> entities, TransactionContext tc) {
		this.doIndexWork(entities, WorkType.ADD, tc);
	}

	@Override
	public void update(Iterable<?> entities, TransactionContext tc) {
		this.doIndexWork(entities, WorkType.UPDATE, tc);
	}

	@Override
	public void delete(Iterable<?> entities, TransactionContext tc) {
		this.doIndexWork(entities, WorkType.PURGE, tc);
	}

	@Override
	public IndexReaderAccessor getIndexReaderAccessor() {
		return this.searchIntegrator.getIndexReaderAccessor();
	}

	@Override
	public void close() throws IOException {
		this.searchIntegrator.close();
	}

	@Override
	public QueryContextBuilder buildQueryBuilder() {
		return this.searchIntegrator.buildQueryBuilder();
	}

	@Override
	public void optimize() {
		this.searchIntegrator.optimize();
	}

	@Override
	public void optimize(Class<?> entity) {
		this.searchIntegrator.optimize(entity);
	}

	@Override
	public Statistics getStatistics() {
		return this.searchIntegrator.getStatistics();
	}

	private void doIndexWork(Iterable<?> entities, WorkType workType,
			TransactionContext tc) {
		Worker worker = this.searchIntegrator.getWorker();
		for (Object object : entities) {
			worker.performWork(new Work(object, workType), tc);
		}
	}

	@Override
	public void purgeAll(Class<?> entityClass, TransactionContext tc) {
		Worker worker = this.searchIntegrator.getWorker();
		worker.performWork(new Work(entityClass, null, WorkType.PURGE_ALL), tc);
	}

	public void doIndexWork(Object entities, WorkType workType) {
		this.doIndexWork(Arrays.asList(entities), workType);
	}

	@Override
	public <T> HSearchQuery<T> createQuery(Class<T> targetetEntity, Query query) {
		HSQuery hsQuery = this.searchIntegrator.createHSQuery();
		hsQuery.luceneQuery(query);
		hsQuery.targetedEntities(Arrays.asList(targetetEntity));
		return new HSearchQueryImpl<T>(hsQuery, this.queryExec, targetetEntity);
	}

	@Override
	public FilterCachingStrategy getFilterCachingStrategy() {
		return this.searchIntegrator.getFilterCachingStrategy();
	}

	@Override
	public FilterDef getFilterDefinition(String name) {
		return this.searchIntegrator.getFilterDefinition(name);
	}

	@Override
	public int getFilterCacheBitResultsSize() {
		return this.searchIntegrator.getFilterCacheBitResultsSize();
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return this.searchIntegrator.getAnalyzer(name);
	}

	@Override
	public Analyzer getAnalyzer(Class<?> entityClass) {
		return this.searchIntegrator.getAnalyzer(entityClass);
	}

	@Override
	public void deleteByQuery(Class<?> entityClass,
			DeletionQuery deletionQuery, TransactionContext tc) {
		Worker worker = this.searchIntegrator.getWorker();
		worker.performWork(new DeleteByQueryWork(entityClass, deletionQuery),
				tc);
	}

}
