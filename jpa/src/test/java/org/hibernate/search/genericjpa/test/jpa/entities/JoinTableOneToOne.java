/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.test.jpa.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * @author Martin Braun
 */
@Entity
@Table(name = "JOINTABLEONETOONE")
public class JoinTableOneToOne {

	@Id
	private Integer id;
	@OneToOne(mappedBy = "jtoto")
	@JoinTable(name = "PLACE_JTOTO", joinColumns = @JoinColumn(name = "PLACE_ID", referencedColumnName = "ID"), inverseJoinColumns = @JoinColumn(name = "JTOTO_ID", referencedColumnName = "ID"))
	private Place place;

	/**
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the place
	 */
	public Place getPlace() {
		return place;
	}

	/**
	 * @param place the place to set
	 */
	public void setPlace(Place place) {
		this.place = place;
	}

}
