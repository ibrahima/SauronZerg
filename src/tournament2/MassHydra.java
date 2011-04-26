package tournament2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.bwapi.proxy.ProxyBot;
import org.bwapi.proxy.ProxyBotFactory;
import org.bwapi.proxy.ProxyServer;
import org.bwapi.proxy.model.BaseLocation;
import org.bwapi.proxy.model.Bwta;
import org.bwapi.proxy.model.Color;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;
import org.bwapi.proxy.model.UpgradeType;

import edu.berkeley.nlp.starcraft.AbstractCerebrate;
import edu.berkeley.nlp.starcraft.Cerebrate;
import edu.berkeley.nlp.starcraft.Strategy;
import edu.berkeley.nlp.starcraft.overmind.Overmind;
import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class MassHydra extends AbstractCerebrate implements Strategy {
	private TilePosition myHome;
	private TilePosition enemyHome;
	private List<TilePosition> startLocs = new ArrayList<TilePosition>();
	private List<Unit> myBase = new ArrayList<Unit>();
	private List<Unit> workers = new ArrayList<Unit>();
	private List<Unit> overlords = new ArrayList<Unit>();
	private List<Unit> zerglings = new ArrayList<Unit>();
	private List<Unit> lisks = new ArrayList<Unit>();
	private List<Integer> attackTimer = new ArrayList<Integer>();
	private List<Unit> larva = new ArrayList<Unit>();
	private List<Unit> gasWorkers1 = new ArrayList<Unit>();
	private List<Unit> gasWorkers2 = new ArrayList<Unit>();
	private Set<ROUnit> enemyUnits = new HashSet<ROUnit>();
	private Unit scout;
	private Unit extractor1;
	private Unit extractor2;
	private Unit naturalBuilder;
	private Player enemy;
	private Player self;
	Random randomizer = new Random();
	int FRAMES_TO_WAIT = 15; //Time before checking to make sure builder is building the building
	int OVERLORD_FRAMES = 300; //Time before checking to build another overlord
	int EXTRA_BUILD = 6; //How much of a buffer to have of supply before building another overlord
	int NUM_TO_ATTACK = 20; //How many lisks to attack with
	int STOP_WORKER_BUILD = 20; //How many workers to stop building workers at 
	//(will be 3 more for gas workers and keeps building a few extra)
	int SCOUT_RANGE = 10; //Dist to scout in spirals for overlord
	int NEAR_DIST = 100; //Dist to count as close enough


	private TilePosition natural;

	private Unit den = null;
	private Unit spawningPool = null;
	private int numZerglings = 0;

	boolean buildOverlord1 = false;
	boolean moreDrones1 = false;
	boolean buildNatural = false;
	boolean buildSpawning = false;
	boolean moreDrones2 = false;
	boolean buildHatchery = false;
	boolean buildExtractor1 = false;
	boolean buildOverlord2 = false;
	boolean buildDen = false;
	boolean buildExtractor2 = false;
	boolean done = false;
	boolean speedUpgrade = false;
	boolean rangeUpgrade = false;
	boolean buildLisks = false;
	boolean scouting = true;
	boolean kiting = false;
	boolean scoutIsDead = false;
	boolean goToNatural = false;
	int overlordScout = 0;
	int step = 0;
	int waitFramesNat = FRAMES_TO_WAIT;
	int waitFramesSpawn = FRAMES_TO_WAIT;
	int waitFramesHatch = FRAMES_TO_WAIT;
	int waitFramesExtract1 = FRAMES_TO_WAIT;
	int waitFramesDen = FRAMES_TO_WAIT;
	int waitFramesExtract2 = FRAMES_TO_WAIT;
	int waitFramesOverlord = OVERLORD_FRAMES;

	int startLoc = 0;


	@Override
	public List<Cerebrate> getTopLevelCerebrates() {
        return Arrays.<Cerebrate>asList(this);
	}

	//Currently, next thing being true stops current action, should be current action
	//turning to false
	//Why is hydralisk den waiting so long?
	@Override
	public void onFrame() {
		Game.getInstance().drawLineMap(Position.centerOfTile(natural), Position.centerOfTile(myHome), Color.YELLOW);
		if (!scoutIsDead) {
			Game.getInstance().drawLineMap(Position.centerOfTile(myHome), scout.getPosition(), Color.GREEN);
		}
		for (int i = 0; i < workers.size(); i++) {
			Unit u = workers.get(i);
			if (u.isIdle()) {
				if (myBase.size() < 2) {
					ROUnit closestPatch = UnitUtils.getClosest(Position.centerOfTile(myHome), Game.getInstance().getMinerals());
					if (closestPatch != null) {
						u.rightClick(closestPatch);
					}
				} else {
					if (i < workers.size()/2) {
						ROUnit closestPatch = UnitUtils.getClosest(Position.centerOfTile(myHome), Game.getInstance().getMinerals());
						if (closestPatch != null) {
							u.rightClick(closestPatch);
						}						
					} else {
						ROUnit closestPatch = UnitUtils.getClosest(Position.centerOfTile(natural), Game.getInstance().getMinerals());
						if (closestPatch != null) {
							u.rightClick(closestPatch);
						}
					}
				}
			}
		}
		/*
		for(Unit u: workers) {
			if(u.isIdle()) {
				ROUnit closestPatch = UnitUtils.getClosest(u, Game.getInstance().getMinerals());
				if (closestPatch != null) {
					u.rightClick(closestPatch);
				}
			}
			//Game.getInstance().drawLineMap(u.getPosition(), Position.centerOfTile(myHome), Color.GREEN);
		}
		 */
		for(Unit u: gasWorkers1) {
			if((u.isGatheringMinerals() | u.isIdle()) & !extractor1.isMorphing()) {
				u.rightClick(extractor1);
			}
			Game.getInstance().drawLineMap(u.getPosition(), Position.centerOfTile(myHome), Color.BLUE);
		}
		for(Unit u: gasWorkers2) {
			if((u.isIdle() | u.isGatheringMinerals()) & !extractor2.isMorphing()) {
				u.rightClick(extractor2);
			}
			Game.getInstance().drawLineMap(u.getPosition(), Position.centerOfTile(natural), Color.ORANGE);
		}
		if (!scoutIsDead) {
			if (scouting) {
				sendScout(scout);
			}
			if (!scouting & scout.isAttackFrame() & step == 0) {
				step++;
				kiting = true;
			}
			if (kiting == true) {
				kiteProbes(scout);
			}
		}
		if (lisks.isEmpty() & underAttack()) {
			attackLocal(workers);
		}

		if (underAttack() & !lisks.isEmpty()) {
			attackLocal(lisks);
		}
		if (workers.size() < 9 & self.minerals() >= 50) {
			trainUnit(UnitType.ZERG_DRONE);
			if (workers.size() >= 8 & self.supplyTotal() == 18 & !buildOverlord1) {
				buildOverlord1 = true;
			}
		}
		if (buildOverlord1 & !moreDrones1) {
			if (trainUnit(UnitType.ZERG_OVERLORD)) {
				moreDrones1 = true;
			}
		}
		if (moreDrones1 & !goToNatural) {
			trainUnit(UnitType.ZERG_DRONE);
			if (workers.size() >= 12) {
				goToNatural = true;
				naturalBuilder = workers.get(0);
				workers.remove(0);
			}
		}
		if (goToNatural & !buildNatural) {
			if (naturalBuilder.isIdle() || naturalBuilder.isGatheringMinerals()) {
				naturalBuilder.move(natural);
			}
			if (isNear(naturalBuilder, natural)) {
				buildNatural = true;
				waitFramesNat = FRAMES_TO_WAIT;
			}
		}
		if (buildNatural & !buildSpawning) {
			if (!naturalBuilder.isMoving()) {
				if (waitFramesNat == FRAMES_TO_WAIT & self.minerals() >= 300) {
					if (isNear(naturalBuilder, natural)) {
						TilePosition buildSpot = getPosNear(naturalBuilder, natural, UnitType.ZERG_HATCHERY, 5);
						if (buildSpot.equals(new TilePosition(-1, -1))) {
							naturalBuilder.build(getPosNear(naturalBuilder, natural, UnitType.ZERG_HATCHERY, 20), UnitType.ZERG_HATCHERY);
						} else {
							naturalBuilder.build(buildSpot, UnitType.ZERG_HATCHERY);
						}
					} else {
						buildNatural = false;
					}
				}
				Game.getInstance().drawLineMap(naturalBuilder.getPosition(), Position.centerOfTile(myHome), Color.WHITE);
				--waitFramesNat;
				//Replace position with natural later
				if (waitFramesNat < 0) {
					if (naturalBuilder.isMorphing()) {
						buildSpawning = true;
						waitFramesSpawn = FRAMES_TO_WAIT;
					} else {
						waitFramesNat = FRAMES_TO_WAIT;
					}
				}
			}
		}
		if (buildSpawning & !moreDrones2) {
			if (waitFramesSpawn == FRAMES_TO_WAIT & self.minerals() >= 200) {
				TilePosition near = myBase.get(0).getTilePosition();
				TilePosition spot = getPosNear(workers.get(0), near, UnitType.ZERG_SPAWNING_POOL, 16);
				workers.get(0).build(spot, UnitType.ZERG_SPAWNING_POOL);
			}
			--waitFramesSpawn;
			Game.getInstance().drawLineMap(workers.get(0).getPosition(), Position.centerOfTile(myHome), Color.WHITE);
			if (waitFramesSpawn < 0) {
				if (workers.get(0).isMorphing()) {
					workers.remove(0);
					moreDrones2 = true;
					waitFramesHatch = FRAMES_TO_WAIT;
				} else {
					waitFramesSpawn = FRAMES_TO_WAIT;
				}
			}
		}
		if (moreDrones2 & !buildHatchery) {
			trainUnit(UnitType.ZERG_DRONE);
			if (workers.size() >= 13) {
				buildHatchery = true;
			}
		}
		if (buildHatchery & !buildExtractor1) {
			if (waitFramesHatch == FRAMES_TO_WAIT & self.minerals() >= 300) {
				TilePosition near = spawningPool.getTilePosition();
				TilePosition spot = getPosNear(workers.get(0), near, UnitType.ZERG_HATCHERY, 10);
				workers.get(0).build(spot, UnitType.ZERG_HATCHERY);
			}
			--waitFramesHatch;
			Game.getInstance().drawLineMap(workers.get(0).getPosition(), Position.centerOfTile(myHome), Color.WHITE);
			if (waitFramesHatch < 0) {
				if (workers.get(0).isMorphing()) {
					workers.remove(0);
					buildExtractor1 = true
					;
					waitFramesExtract1 = FRAMES_TO_WAIT;
				} else {
					waitFramesHatch = FRAMES_TO_WAIT;
				}
			}
		}
		/*
		 * Not working...
	  if (spawningPool != null & zerglings.size() < 2) {
		  if (trainUnit(UnitType.ZERG_ZERGLING)) {
			  numZerglings++;
		  }
	  }
		 */
		if (buildExtractor1 & !buildOverlord2 & self.minerals() >= 50) {
			Unit w = workers.get(0);
			if (w.isGatheringMinerals() || w.isIdle()) {
				ROUnit closestGeyser = UnitUtils.getClosest(w, Game.getInstance().getGeysers());
				if (closestGeyser != null) {
					w.build(closestGeyser.getTilePosition(), UnitType.ZERG_EXTRACTOR);
				}
			}
			/*
			if (waitFramesExtract1 == FRAMES_TO_WAIT) {
				Unit w = workers.get(0);
				ROUnit closestGeyser = UnitUtils.getClosest(w, Game.getInstance().getGeysers());
				if (closestGeyser != null) {
					w.build(closestGeyser.getTilePosition(), UnitType.ZERG_EXTRACTOR);
				}
			}
			--waitFramesExtract1;
			Game.getInstance().drawLineMap(workers.get(0).getPosition(), Position.centerOfTile(myHome), Color.WHITE);
			if (waitFramesExtract1 < 0) {
				waitFramesExtract1 = FRAMES_TO_WAIT; //Normal code is in onUnitMorph			
			}
			 */
		}
		if (buildOverlord2 & !buildDen) {
			if (trainUnit(UnitType.ZERG_OVERLORD)) {
				buildDen = true;
			}
		}
		if (buildDen & !buildExtractor2) {
			if (waitFramesDen == FRAMES_TO_WAIT & self.minerals() >= 100 & self.gas() >= 50) {
				TilePosition near = myBase.get(myBase.size()-1).getTilePosition();
				TilePosition spot = getPosNear(workers.get(0), near, UnitType.ZERG_HYDRALISK_DEN, 15);
				workers.get(0).build(spot, UnitType.ZERG_HYDRALISK_DEN);
			}
			--waitFramesDen;
			Game.getInstance().drawLineMap(workers.get(0).getPosition(), Position.centerOfTile(myHome), Color.WHITE);
			if (waitFramesDen < 0) {
				if (workers.get(0).isMorphing()) {
					workers.remove(0);
					buildExtractor2 = true;
					waitFramesExtract2 = FRAMES_TO_WAIT;
				} else {
					waitFramesDen = FRAMES_TO_WAIT;
				}
			}
		}
		//BuildExtractor2 not implemented
		if (buildExtractor2 & !done) {
			Unit w = workers.get(0);
			if (w.isIdle() || w.isGatheringMinerals()) {
				ROUnit closestGeyser = UnitUtils.getClosest(myBase.get(1), Game.getInstance().getGeysers());
				if (closestGeyser != null) {
					if (closestGeyser.getDistance(Position.centerOfTile(natural)) < 400) {
						w.build(closestGeyser.getTilePosition(), UnitType.ZERG_EXTRACTOR);	//Normal code is in onUnitMorph
					} else {
						done = true;
					}
				}
			}
			/*
			if (waitFramesExtract2 == FRAMES_TO_WAIT) {
				ROUnit closestGeyser = UnitUtils.getClosest(myBase.get(1), Game.getInstance().getGeysers());
				if (closestGeyser != null) {
					workers.get(0).build(closestGeyser.getTilePosition(), UnitType.ZERG_EXTRACTOR);
				}
			}
			--waitFramesExtract2;
			Game.getInstance().drawLineMap(workers.get(0).getPosition(), Position.centerOfTile(myHome), Color.WHITE);
			if (waitFramesExtract2 < 0) {
				waitFramesExtract2 = FRAMES_TO_WAIT;	//Normal code is in onUnitMorph	
			}
			 */
		}
		if (den != null & !speedUpgrade & self.minerals() >= 150 & self.gas() >= 150) {
			if (den.isCompleted()) {
				den.upgrade(UpgradeType.MUSCULAR_AUGMENTS);
				buildLisks = true; 
				speedUpgrade = true;
			}
		}
		if (workers.size() < STOP_WORKER_BUILD & den != null & spawningPool != null) {
			trainUnit(UnitType.ZERG_DRONE);
		}
		if (speedUpgrade & !rangeUpgrade & self.minerals() >= 150 & self.gas() >= 150) {
			den.upgrade(UpgradeType.GROOVED_SPINES);
			//rangeUpgrade = true;
		}
		//When speed upgrade done, get range upgrade
		if (buildLisks) {
			trainUnit(UnitType.ZERG_HYDRALISK);
			//massLisks();
		}
		if (overlords.size()*16 < self.supplyUsed()+EXTRA_BUILD  & buildLisks) {
			if (waitFramesOverlord == OVERLORD_FRAMES) {
				boolean x = trainUnit(UnitType.ZERG_OVERLORD);
				if (!x) {
					waitFramesOverlord++; //If it didn't get trained, let it try next frame
				}
			}
			--waitFramesOverlord;
			if (waitFramesOverlord < 0) {
				waitFramesOverlord = OVERLORD_FRAMES;
			}
		}
		if (lisks.size() >= NUM_TO_ATTACK) {
			attackEnemy(lisks);
		}
		/*
		for (int i = 0 ; i < lisks.size() ; i++) {
			Unit u = lisks.get(i);
			int curr = attackTimer.get(i);
			if (u.isAttacking() && curr >= 0) {
				if (curr == 0) {
					u.move(myHome);
					attackTimer.add(i, -40);
					attackTimer.remove(i+1);
				} else {
					curr--;
					attackTimer.add(i, curr);
					attackTimer.remove(i+1);
				}
			}
			if (u.isMoving() && curr < 0) {
				if (curr == -1) {
					u.attackMove(Position.centerOfTile(enemyHome));
					attackTimer.add(i, 10);
					attackTimer.remove(i+1);
				} else {
					curr++;
					attackTimer.add(i, curr);
					attackTimer.remove(i+1);
				}
			}
		}
		*/
	}



	/*
	 * Sends the start location to start location, if an enemy building is seen
	 * sets that location as the enemy's base (DO NOT MESS THAT UP) and starts the 
	 * cheese
	 */
	public void sendScout(Unit u) {
		if (u.isIdle()) {
			u.move(startLocs.get(startLoc));
		}
		if (isNear(u, startLocs.get(startLoc))) {
			startLoc++;
		}
		if (containsBuilding(enemyUnits)) {
			//Calculate where the scout is closest to, set that as enemy start position
			//Start cheese
			double min = 1000000000;
			for (TilePosition t: startLocs) {
				if(u.getDistance(new Position(t)) < min) {
					min = u.getDistance(new Position(t));
					enemyHome = t;
				}
			}
			scouting = false;
		}
	}

	public void kiteProbes(Unit u) {
		if (step == 1 && u.getDistance(new Position(myHome.add(5, 5))) < 100) {
			step = 2;
		}
		if (step == 1) {
			u.rightClick(myHome.add(5, 5));
		}
		if (step == 2 && u.getDistance(new Position(myHome.add(5, -5))) < 100) {
			step = 3;
		}
		if (step == 2) {
			u.rightClick(myHome.add(5, -5));
		}
		if (step == 3 && u.getDistance(new Position(myHome.add(-5, -5))) < 100) {
			step = 4;
		}
		if (step == 3) {
			u.rightClick(myHome.add(-5, -5));
		}
		if (step == 4 && u.getDistance(new Position(myHome.add(-5, 5))) < 100) {
			step = 0;
			kiting = false;
			u.attackMove(new Position(enemyHome));
		}
		if (step == 4) {
			u.rightClick(myHome.add(-5, 5));
		}
	}



	//Returns true if the set contains a building
	public boolean containsBuilding(Set<ROUnit> e) {
		for(ROUnit u : e) {
			if (u.getType().isBuilding()) {
				return true;
			}
		}
		return false;
	}


	//Test this
	public void attackLocal(List<Unit> a) {
		for (Unit u : a) {
			if (u.isGatheringGas() || u.isGatheringMinerals() || u.isIdle()) {
				ROUnit toAttack = nearestDistEnemy(u, enemyUnits);
				if (toAttack != null) {
					if (u.getDistance(toAttack) < 200) {
						u.attackUnit(toAttack);//get nearest distance, if less than 100, attack	
					}
				}
			}
		}
	}
	//Test this
	public ROUnit nearestDistEnemy(Unit a, Set<ROUnit> b) {
		int min = 1000000000;
		ROUnit minUnit = null;
		for (ROUnit u: b) {
			if (a.getDistance(u) < min) {
				min = a.getDistance(u);
				minUnit = u;
			}
		}
		return minUnit;
	}

	//Returns true if enemy units are there, used for zergling rushes
	public boolean underAttack() {
		for (ROUnit u: enemyUnits) {
			if (u.getDistance(Position.centerOfTile(myHome)) < 300) {
				return true;
			}
		}
		return false;
	}

	public void attackEnemy(List<Unit> a) {
		for (Unit u: a) {
			if (isNear(u, enemyHome) && !u.isAttacking() && u.isIdle()) {
				if (enemyUnits.isEmpty()) {
					//explore random
					int y = randomizer.nextInt(Game.getInstance().getMapHeight());
					int x = randomizer.nextInt(Game.getInstance().getMapWidth());
					Position p = new Position(x, y);
					System.out.println(p);
					u.attackMove(p);
				} else {
					int d = randomizer.nextInt(enemyUnits.size());
					ROUnit enemy = (ROUnit) enemyUnits.toArray()[d];
					System.out.println(enemy);
					u.attackMove(((ROUnit) enemyUnits.toArray()[d]).getLastKnownPosition());
					//attack an enemy you know about
				}
			} else {
				if (u.isIdle() || u.isPatrolling()) {
					u.attackMove(Position.centerOfTile(enemyHome));
				}
			}
		}
		if (overlords.get(4).isIdle()) {
			overlords.get(4).follow(lisks.get(0));
		}
	}


	public void massLisks() {
		for (int i = 0; i < myBase.size(); i++) {
			myBase.get(i).train(UnitType.ZERG_HYDRALISK);
		}
	}  

	public boolean trainUnit(UnitType u) {
		if (larva.isEmpty()) {
			return false;
		}
		if (self.supplyTotal() < (self.supplyUsed()+u.supplyRequired()) & !u.equals(UnitType.ZERG_OVERLORD)) {
			return false;
		}
		if (self.minerals() < u.mineralPrice()) {
			return false;
		}
		if (self.gas() < u.gasPrice()) {
			return false;
		}
		larva.get(0).train(u);
		return true;
	}

	public TilePosition getPosNear(Unit builder, TilePosition t, UnitType building, int rand) {
		if (Game.getInstance().canBuildHere(builder, t, building)) {
			return t;
		}
		int i = 0;
		int half = rand/2;
		while (true) {
			int x = randomizer.nextInt(rand);
			int y = randomizer.nextInt(rand);
			if (Game.getInstance().canBuildHere(builder, t.add(x-half, y-half), building)) {
				return t.add(x-half,y-half);
			}
			i++;
			if (i > 500) {
				System.out.println("Couldn't find spot to build");
				return new TilePosition(-1, -1);
			}
		}
	}

	public boolean isNear(Unit u, TilePosition pos) {
		if (u.getDistance(Position.centerOfTile(pos)) < NEAR_DIST) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onStart() {
		self = Game.getInstance().self();
		myHome = Game.getInstance().self().getStartLocation();
		for (TilePosition t : Game.getInstance().getStartLocations()) {
			if (!myHome.equals(t)) {
				startLocs.add(t);
			}
		}
		for (Player p : Game.getInstance().getPlayers()) {
			if (p.isEnemy(self)) {
				enemy = p;
			}
		}
		for(ROUnit u: Game.getInstance().self().getUnits()) {
			if(u.getType().isWorker()) {
				workers.add(UnitUtils.assumeControl(u));
			} else if(u.getType().isResourceDepot()) {
				myBase.add(UnitUtils.assumeControl(u));
			} else if(u.getType().isFlyer()) {
				overlords.add(UnitUtils.assumeControl(u));
			} else if(u.getType().equals(UnitType.ZERG_LARVA)) {
				larva.add(UnitUtils.assumeControl(u));
			}
		}
		scout = workers.get(0);
		workers.remove(0);
		Set<Position> bases = new HashSet<Position>();
		for (BaseLocation b: Bwta.getInstance().getBaseLocations()) {
			if (!b.getTilePosition().equals(myHome)) {
				bases.add(b.getPosition());
			}
		}
		natural = new TilePosition(UnitUtils.getClosestPosition(Position.centerOfTile(myHome), bases));
	}

	@Override
	public void onUnitCreate(ROUnit unit) {
		if (unit.getType().equals(UnitType.ZERG_ZERGLING) & unit.getPlayer().equals(self)) {
			zerglings.add(UnitUtils.assumeControl(unit));
		}
		if(unit.getType().equals(UnitType.ZERG_LARVA) & unit.getPlayer().equals(self)) {
			larva.add(UnitUtils.assumeControl(unit));
		}
	}

	@Override
	public void onUnitDestroy(ROUnit unit) {
		if (gasWorkers1.contains(unit)) {
			gasWorkers1.remove(unit);
			gasWorkers1.add(workers.get(0));
			workers.remove(0);
		}
		if (gasWorkers2.contains(unit)) {
			gasWorkers2.remove(unit);
			gasWorkers2.add(workers.get(0));
			workers.remove(0);
		}
		if (enemyUnits.contains(unit)) {
			enemyUnits.remove(unit);
		}
		if (lisks.contains(unit)) {
			attackTimer.remove(lisks.indexOf(unit));
			lisks.remove(unit);

		}
		if (workers.contains(unit)) {
			workers.remove(unit);
		}
		if (overlords.contains(unit)) {
			overlords.remove(unit);
		}
		if (scout == unit) {
			scoutIsDead = true;
		}
		if (naturalBuilder == unit) {
			naturalBuilder = workers.get(0);
			workers.remove(0);
		}
	}

	@Override
	public void onUnitHide(ROUnit unit) {

	}

	@Override
	public void onUnitMorph(ROUnit unit) {
		if (unit.getType().isWorker()& unit.getPlayer().equals(self)) {
			workers.add(UnitUtils.assumeControl(unit));
		}
		if (larva.contains(unit)) {
			larva.remove(unit);
		}
		if (unit.getType().equals(UnitType.ZERG_OVERLORD)& unit.getPlayer().equals(self)) {
			overlords.add(UnitUtils.assumeControl(unit));
		}
		if (unit.getType().equals(UnitType.ZERG_HATCHERY)& unit.getPlayer().equals(self)) {
			myBase.add(UnitUtils.assumeControl(unit));
		}
		if (unit.getType().equals(UnitType.ZERG_SPAWNING_POOL)& unit.getPlayer().equals(self)) {
			spawningPool = UnitUtils.assumeControl(unit);
		}
		if (unit.getType().equals(UnitType.ZERG_ZERGLING)& unit.getPlayer().equals(self)) {
			zerglings.add(UnitUtils.assumeControl(unit));
		}
		if (unit.getType().equals(UnitType.ZERG_HYDRALISK)& unit.getPlayer().equals(self)) {
			Unit u = UnitUtils.assumeControl(unit);
			lisks.add(u);
			attackTimer.add(10);
			if (isNear(u, natural)) {
				u.patrol(Position.centerOfTile(myHome));
			} else {
				u.patrol(Position.centerOfTile(natural));
			}
		}
		if (unit.getType().equals(UnitType.ZERG_HYDRALISK_DEN)& unit.getPlayer().equals(self)) {
			den = UnitUtils.assumeControl(unit);
		}
		if (unit.getType().equals(UnitType.ZERG_EXTRACTOR)& unit.getPlayer().equals(self)) {
			if (gasWorkers1.isEmpty()) {
				workers.remove(0);
				buildOverlord2 = true;
				waitFramesDen = FRAMES_TO_WAIT;
				extractor1 = UnitUtils.assumeControl(unit); 
				gasWorkers1.add(workers.get(0));
				workers.remove(0);
				gasWorkers1.add(workers.get(0));
				workers.remove(0);
				gasWorkers1.add(workers.get(0));
				workers.remove(0);
			} else {
				workers.remove(0);
				done = true;
				extractor2 = UnitUtils.assumeControl(unit);
				gasWorkers2.add(workers.get(0));
				workers.remove(0);
				gasWorkers2.add(workers.get(0));
				workers.remove(0);
				//gasWorkers2.add(workers.get(0));
				//workers.remove(0);
			}
		}
	}

	@Override
	public void onUnitShow(ROUnit unit) {
		if (unit.getPlayer().equals(enemy)) {
			enemyUnits.add(unit);
		}
		if (unit.getType().equals(UnitType.RESOURCE_MINERAL_FIELD) & myHome.getDistance(unit.getTilePosition()) > 15) {
			if (!natural.equals(myHome)) {
				if (myHome.getDistance(unit.getPosition()) < myHome.getDistance(new Position(natural))) {
					natural = new TilePosition(unit.getPosition());
				}		
			} else {
				natural = new TilePosition(unit.getPosition());
			}	
		}
	}


	@Override
	public void onEnd(boolean isWinnerFlag) {

	}

	public static void main(String[] args) {
		ProxyBotFactory factory = new ProxyBotFactory() {

			@Override

			public ProxyBot getBot(Game g) {
				// TODO put code to initialize your bot here
				return new Overmind(new MassHydra(),new Properties());
				//return null;
			}

		};
		String heartbeatFilename = args.length > 1 ? args[1] : null;
		new ProxyServer(factory,ProxyServer.extractPort(args.length> 0 ? args[0] : null), heartbeatFilename).run();
	}
}
