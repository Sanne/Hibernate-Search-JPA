/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.db.events;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.metadata.impl.MetadataProvider;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.genericjpa.db.events.EventType;
import org.hibernate.search.genericjpa.db.events.IndexUpdater;
import org.hibernate.search.genericjpa.db.events.IndexUpdater.IndexWrapper;
import org.hibernate.search.genericjpa.db.events.UpdateConsumer.UpdateInfo;
import org.hibernate.search.genericjpa.test.db.entities.Place;
import org.hibernate.search.genericjpa.test.db.entities.Sorcerer;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.hibernate.search.standalone.entity.ReusableEntityProvider;
import org.hibernate.search.standalone.factory.SearchConfigurationImpl;
import org.hibernate.search.standalone.factory.Transaction;
import org.hibernate.search.standalone.metadata.MetadataRehasher;
import org.hibernate.search.standalone.metadata.MetadataUtil;
import org.hibernate.search.standalone.metadata.RehashedTypeMetadata;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Martin Braun
 */
public class IndexUpdaterTest {

	Map<Class<?>, List<Class<?>>> containedInIndexOf;
	Map<Class<?>, RehashedTypeMetadata> rehashedTypeMetadataPerIndexRoot;
	ReusableEntityProvider entityProvider;
	List<UpdateInfo> updateInfos;
	boolean changed;
	boolean deletedSorcerer;

	@Before
	public void setup() {
		this.changed = false;
		this.deletedSorcerer = false;
		MetadataProvider metadataProvider = MetadataUtil.getMetadataProvider( new SearchConfigurationImpl() );
		MetadataRehasher rehasher = new MetadataRehasher();
		List<RehashedTypeMetadata> rehashedTypeMetadatas = new ArrayList<>();
		rehashedTypeMetadataPerIndexRoot = new HashMap<>();
		for ( Class<?> indexRootType : Arrays.asList( Place.class ) ) {
			RehashedTypeMetadata rehashed = rehasher.rehash( metadataProvider.getTypeMetadataFor( indexRootType ) );
			rehashedTypeMetadatas.add( rehashed );
			rehashedTypeMetadataPerIndexRoot.put( indexRootType, rehashed );
		}
		this.containedInIndexOf = MetadataUtil.calculateInIndexOf( rehashedTypeMetadatas );
		this.entityProvider = new ReusableEntityProvider() {

			@SuppressWarnings("rawtypes")
			@Override
			public List getBatch(Class<?> entityClass, List<Object> ids) {
				throw new AssertionFailure( "not to be used in this test!" );
			}

			@Override
			public Object get(Class<?> entityClass, Object id) {
				return IndexUpdaterTest.this.obj( entityClass, false );
			}

			@Override
			public void open() {

			}

			@Override
			public void close() {

			}

		};
		this.updateInfos = this.createUpdateInfos();
	}

	@Test
	public void testWithoutIndex() {
		List<UpdateInfo> updateInfos = this.createUpdateInfos();
		Set<UpdateInfo> updateInfoSet = new HashSet<>( updateInfos );
		IndexWrapper indexWrapper = new IndexWrapper() {

			@Override
			public void delete(Class<?> entityClass, List<Class<?>> inIndexOf, Object id, Transaction tx) {
				Object obj = IndexUpdaterTest.this.obj( entityClass );
				System.out.println( entityClass );
				System.out.println( updateInfoSet );
				System.out.println( obj );
				assertTrue( updateInfoSet.remove( new UpdateInfo( entityClass, (Integer) id, EventType.DELETE ) ) );
			}

			@Override
			public void update(Object entity, Transaction tx) {
				if ( entity != null ) {
					try {
						assertTrue( updateInfoSet.remove( new UpdateInfo( entity.getClass(), (Integer) entity.getClass().getMethod( "getId" ).invoke( entity ),
								EventType.UPDATE ) ) );
					}
					catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
						throw new RuntimeException( e );
					}
				}
			}

			@Override
			public void index(Object entity, Transaction tx) {
				if ( entity != null ) {
					try {
						assertTrue( updateInfoSet.remove( new UpdateInfo( entity.getClass(), (Integer) entity.getClass().getMethod( "getId" ).invoke( entity ),
								EventType.INSERT ) ) );
					}
					catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
						throw new RuntimeException( e );
					}
				}
			}

		};
		IndexUpdater updater = new IndexUpdater( this.rehashedTypeMetadataPerIndexRoot, this.containedInIndexOf, this.entityProvider, indexWrapper );
		updater.updateEvent( updateInfos );
	}

	@Test
	public void testWithIndex() {
		SearchConfiguration searchConfiguration = new SearchConfigurationImpl();
		List<Class<?>> classes = Arrays.asList( Place.class, Sorcerer.class );

		SearchIntegratorBuilder builder = new SearchIntegratorBuilder();
		// we have to build an integrator here (but we don't need it afterwards)
		builder.configuration( searchConfiguration ).buildSearchIntegrator();
		classes.forEach( (clazz) -> {
			builder.addClass( clazz );
		} );
		ExtendedSearchIntegrator impl = (ExtendedSearchIntegrator) builder.buildSearchIntegrator();

		IndexUpdater updater = new IndexUpdater( this.rehashedTypeMetadataPerIndexRoot, this.containedInIndexOf, this.entityProvider, impl );
		this.reset( updater, impl );

		this.tryOutDelete( updater, impl, 0, 1, Place.class );
		// this shouldn't delete the root though
		this.tryOutDeleteNonRoot( updater, impl, 0, 2, Sorcerer.class, "sorcerers.name", "Saruman" );
		this.tryOutDeleteNonRoot( updater, impl, 1, 2, Sorcerer.class, "name", "Valinor" );

		this.tryOutUpdate( updater, impl, 0, 1, Place.class, "name", "Valinor" );
		this.tryOutUpdate( updater, impl, 0, 2, Sorcerer.class, "sorcerers.name", "Saruman" );
	}

	private void reset(IndexUpdater updater, ExtendedSearchIntegrator impl) {
		{
			Transaction tx = new Transaction();
			impl.getWorker().performWork( new Work( Place.class, null, WorkType.PURGE_ALL ), tx );
			tx.commit();
			this.assertCount( impl, 0 );
		}

		updater.updateEvent( Arrays.asList( new UpdateInfo( Sorcerer.class, 2, EventType.INSERT ) ) );
		this.assertCount( impl, 1 );

		{
			Transaction tx = new Transaction();
			impl.getWorker().performWork( new Work( Place.class, null, WorkType.PURGE_ALL ), tx );
			tx.commit();
			this.assertCount( impl, 0 );
		}

		updater.updateEvent( Arrays.asList( new UpdateInfo( Place.class, 1, EventType.INSERT ) ) );
		this.assertCount( impl, 1 );

		{
			Transaction tx = new Transaction();
			impl.getWorker().performWork( new Work( Place.class, null, WorkType.PURGE_ALL ), tx );
			tx.commit();
			this.assertCount( impl, 0 );
		}

		updater.updateEvent( this.createUpdateInfoForInsert() );
		this.assertCount( impl, 1 );
	}

	private void assertCount(ExtendedSearchIntegrator impl, int count) {
		assertEquals(
				count,
				impl.createHSQuery().targetedEntities( Arrays.asList( Place.class ) )
						.luceneQuery( impl.buildQueryBuilder().forEntity( Place.class ).get().all().createQuery() ).queryResultSize() );
	}

	private void tryOutDelete(IndexUpdater updater, ExtendedSearchIntegrator impl, int expectedCount, Object id, Class<?> clazz) {
		updater.updateEvent( Arrays.asList( new UpdateInfo( clazz, id, EventType.DELETE ) ) );
		assertEquals(
				expectedCount,
				impl.createHSQuery().targetedEntities( Arrays.asList( Place.class ) )
						.luceneQuery( impl.buildQueryBuilder().forEntity( Place.class ).get().all().createQuery() ).queryResultSize() );
		this.reset( updater, impl );
	}

	private void tryOutDeleteNonRoot(IndexUpdater updater, ExtendedSearchIntegrator impl, int expectedCount, Object id, Class<?> clazz,
			String fieldToCheckCount, String originalMatch) {
		this.deletedSorcerer = true;
		updater.updateEvent( Arrays.asList( new UpdateInfo( clazz, id, EventType.DELETE ) ) );
		assertEquals(
				expectedCount,
				impl.createHSQuery()
						.targetedEntities( Arrays.asList( Place.class ) )
						.luceneQuery(
								impl.buildQueryBuilder().forEntity( Place.class ).get().keyword().onField( fieldToCheckCount ).matching( originalMatch )
										.createQuery() ).queryResultSize() );
		this.deletedSorcerer = false;
		this.reset( updater, impl );
	}

	private void tryOutUpdate(IndexUpdater updater, ExtendedSearchIntegrator impl, int expectedCount, Object id, Class<?> clazz, String field,
			String originalMatch) {
		this.changed = true;
		updater.updateEvent( Arrays.asList( new UpdateInfo( clazz, id, EventType.UPDATE ) ) );
		assertEquals(
				expectedCount,
				impl.createHSQuery()
						.targetedEntities( Arrays.asList( Place.class ) )
						.luceneQuery(
								impl.buildQueryBuilder().forEntity( Place.class ).get().keyword().onField( field ).matching( originalMatch ).createQuery() )
						.queryResultSize() );
		this.changed = false;
		this.reset( updater, impl );
	}

	private Object obj(Class<?> entityClass) {
		return this.obj( entityClass, false );
	}

	private Object obj(Class<?> entityClass, boolean ignoreSorcererDelete) {
		Place place = new Place();
		place.setId( 1 );
		if ( !this.changed ) {
			place.setName( "Valinor" );
		}
		else {
			place.setName( "Alinor" );
		}
		if ( ignoreSorcererDelete || !this.deletedSorcerer ) {
			Sorcerer sorcerer = new Sorcerer();
			sorcerer.setId( 2 );
			sorcerer.setPlace( place );
			if ( !this.changed ) {
				sorcerer.setName( "Saruman" );
			}
			else {
				sorcerer.setName( "Aruman" );
			}
			place.setSorcerers( new HashSet<>( Arrays.asList( sorcerer ) ) );
			if ( entityClass.equals( Sorcerer.class ) ) {
				return sorcerer;
			}
		}
		if ( entityClass.equals( Place.class ) ) {
			return place;
		}
		return null;
	}

	private List<UpdateInfo> createUpdateInfos() {
		List<UpdateInfo> ret = new ArrayList<>();

		ret.addAll( this.createUpdateInfoForInsert() );

		ret.add( new UpdateInfo( Place.class, 1, EventType.UPDATE ) );
		ret.add( new UpdateInfo( Place.class, 1, EventType.DELETE ) );

		ret.add( new UpdateInfo( Sorcerer.class, 2, EventType.UPDATE ) );
		ret.add( new UpdateInfo( Sorcerer.class, 2, EventType.DELETE ) );

		return ret;
	}

	private List<UpdateInfo> createUpdateInfoForInsert() {
		List<UpdateInfo> ret = new ArrayList<>();
		ret.add( new UpdateInfo( Place.class, 1, EventType.INSERT ) );
		ret.add( new UpdateInfo( Sorcerer.class, 2, EventType.INSERT ) );
		return ret;
	}

}
