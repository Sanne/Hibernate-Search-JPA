/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.standalone.factory;

import java.util.Collection;

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;

public final class SearchFactoryFactory {

	private SearchFactoryFactory() {
		throw new AssertionFailure( "can't touch this!" );
	}

	public static SearchFactory createSearchFactory(SearchConfiguration searchConfiguration, Collection<Class<?>> classes) {
		SearchIntegratorBuilder builder = new SearchIntegratorBuilder();
		// we have to build an integrator here (but we don't need it afterwards)
		builder.configuration( searchConfiguration ).buildSearchIntegrator();
		classes.forEach( (clazz) -> {
			builder.addClass( clazz );
		} );
		SearchIntegrator impl = builder.buildSearchIntegrator();
		SearchFactory factory = new SearchFactoryImpl( impl.unwrap( ExtendedSearchIntegrator.class ) );
		return factory;
	}

}