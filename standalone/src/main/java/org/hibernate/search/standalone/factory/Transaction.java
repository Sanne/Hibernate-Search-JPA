/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.standalone.factory;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.search.standalone.transaction.TransactionContext;

public class Transaction implements TransactionContext {

	private boolean progress = true;
	private List<Synchronization> syncs = new ArrayList<Synchronization>();

	@Override
	public boolean isTransactionInProgress() {
		return progress;
	}

	@Override
	public Object getTransactionIdentifier() {
		return this;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		syncs.add( synchronization );
	}

	public void commit() {
		this.progress = false;
		for ( Synchronization sync : syncs ) {
			sync.beforeCompletion();
		}

		for ( Synchronization sync : syncs ) {
			sync.afterCompletion( Status.STATUS_COMMITTED );
		}
	}
	
	public void rollback() {
		this.progress = false;
		for ( Synchronization sync : syncs ) {
			sync.beforeCompletion();
		}

		for ( Synchronization sync : syncs ) {
			sync.afterCompletion( Status.STATUS_ROLLEDBACK );
		}
	}

}
