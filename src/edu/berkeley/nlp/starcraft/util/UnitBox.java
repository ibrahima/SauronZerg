package edu.berkeley.nlp.starcraft.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.bwapi.proxy.model.ROUnit;

/**
 * A UnitBox is a stupid abstraction that holds a bunch of units, organized by their type.
 * @author dlwh
 *
 */
public class UnitBox {
	private Set<ROUnit> units = new HashSet<ROUnit>();
	private Set<ROUnit> buildings = new HashSet<ROUnit>();
	private Set<ROUnit> resourceDepots = new HashSet<ROUnit>();
	private Set<ROUnit> workers = new HashSet<ROUnit>();
	// TODO: others: flyers, ground units, mobile units, ???
	
	public void addUnit(ROUnit u) {
		units.add(u);
		if(u.getType().isBuilding()) buildings.add(u);
		if(u.getType().isResourceDepot()) resourceDepots.add(u);
		if(u.getType().isWorker()) workers.add(u);
	}
	
	public boolean removeUnit(ROUnit u) {		
		buildings.remove(u);
		resourceDepots.remove(u);
		workers.remove(u);
		return units.remove(u);
	}
	
	public Set<ROUnit> getUnits() {
  	return Collections.unmodifiableSet(units);
  }

	public Set<ROUnit> getBuildings() {
  	return Collections.unmodifiableSet(buildings);
  }

	public Set<ROUnit> getResourceDepots() {
  	return Collections.unmodifiableSet(resourceDepots);
  }
	
	public Set<ROUnit> getWorkers() {
		return Collections.unmodifiableSet(workers);
		
	}

}
