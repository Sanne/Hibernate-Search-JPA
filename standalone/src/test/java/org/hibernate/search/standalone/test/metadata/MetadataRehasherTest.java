/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.standalone.test.metadata;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.metadata.impl.MetadataProvider;
import org.hibernate.search.engine.metadata.impl.TypeMetadata;
import org.hibernate.search.standalone.factory.SearchConfigurationImpl;
import org.hibernate.search.standalone.metadata.MetadataRehasher;
import org.hibernate.search.standalone.metadata.MetadataUtil;
import org.hibernate.search.standalone.metadata.RehashedTypeMetadata;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Martin Braun
 */
public class MetadataRehasherTest {

	private MetadataProvider metadataProvider;
	private MetadataRehasher metadataRehasher;

	@Before
	public void setup() {
		SearchConfiguration searchConfiguration = new SearchConfigurationImpl();
		metadataProvider = MetadataUtil.getMetadataProvider( searchConfiguration );
		this.metadataRehasher = new MetadataRehasher();
	}

	@Test
	public void test() {		
		TypeMetadata fromRoot = this.metadataProvider.getTypeMetadataFor( RootEntity.class );
		RehashedTypeMetadata fromRootRehashed = this.metadataRehasher.rehash( fromRoot );
		{

			assertEquals( fromRoot, fromRootRehashed.getOriginalTypeMetadata() );

			// THE ID FIELD NAMES
			{
				Map<Class<?>, List<String>> idFieldNamesForType = fromRootRehashed.getIdFieldNamesForType();

				assertEquals( 3, idFieldNamesForType.get( RootEntity.class ).size() );
				assertTrue( idFieldNamesForType.get( RootEntity.class ).contains( "MAYBE_ROOT_NOT_NAMED_ID" ) );
				assertTrue( idFieldNamesForType.get( RootEntity.class ).contains( "recursiveSelf.MAYBE_ROOT_NOT_NAMED_ID" ) );
				assertTrue( idFieldNamesForType.get( RootEntity.class ).contains( "recursiveSelf.recursiveSelf.MAYBE_ROOT_NOT_NAMED_ID" ) );

				assertEquals( 2, idFieldNamesForType.get( SubEntity.class ).size() );
				assertTrue( idFieldNamesForType.get( SubEntity.class ).contains( "otherEntity.SUB_NOT_NAMED_ID" ) );
				assertTrue( idFieldNamesForType.get( SubEntity.class ).contains( "recursiveSelf.otherEntity.SUB_NOT_NAMED_ID" ) );
			}

			// THE ID PROPERTY NAMES
			{
				assertEquals( "rootId", fromRootRehashed.getIdPropertyNameForType().get( RootEntity.class ) );
				assertEquals( "subId", fromRootRehashed.getIdPropertyNameForType().get( SubEntity.class ) );
			}

			// THE DOCUMENT_FIELD_META_DATA
			{
				assertEquals( 5, fromRootRehashed.getDocumentFieldMetadataForIdFieldName().size() );
				// make sure all of these are different
				assertEquals( 5, new HashSet<>( fromRootRehashed.getDocumentFieldMetadataForIdFieldName().values() ).size() );
			}
		}

		TypeMetadata fromAnotherRoot = this.metadataProvider.getTypeMetadataFor( AnotherRootEntity.class );
		RehashedTypeMetadata fromAnotherRootRehashed = this.metadataRehasher.rehash( fromAnotherRoot );

		Set<Class<?>> indexRelevantEntities = MetadataUtil.calculateIndexRelevantEntities( Arrays.asList( fromRootRehashed, fromAnotherRootRehashed ) );
		assertEquals( 3, indexRelevantEntities.size() );
		assertTrue( indexRelevantEntities.contains( RootEntity.class ) );
		assertTrue( indexRelevantEntities.contains( AnotherRootEntity.class ) );
		assertTrue( indexRelevantEntities.contains( SubEntity.class ) );

		Map<Class<?>, List<Class<?>>> inIndexOf = MetadataUtil.calculateInIndexOf( Arrays.asList( fromRootRehashed, fromAnotherRootRehashed ) );
		assertEquals( 1, inIndexOf.get( RootEntity.class ).size() );
		assertTrue( inIndexOf.get( RootEntity.class ).contains( RootEntity.class ) );

		assertEquals( 1, inIndexOf.get( AnotherRootEntity.class ).size() );
		assertTrue( inIndexOf.get( AnotherRootEntity.class ).contains( AnotherRootEntity.class ) );

		assertEquals( 2, inIndexOf.get( SubEntity.class ).size() );
		assertTrue( inIndexOf.get( SubEntity.class ).contains( RootEntity.class ) );
		assertTrue( inIndexOf.get( SubEntity.class ).contains( AnotherRootEntity.class ) );

		// FIXME: unit-test the TermDeletion stuff
	}

}
