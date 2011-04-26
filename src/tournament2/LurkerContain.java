package tournament2;

import edu.berkeley.nlp.starcraft.AbstractCerebrate;
import edu.berkeley.nlp.starcraft.Cerebrate;
import edu.berkeley.nlp.starcraft.Strategy;
import edu.berkeley.nlp.starcraft.collect.ArrayListMultimap;
import edu.berkeley.nlp.starcraft.util.ConvexHull;
import edu.berkeley.nlp.starcraft.util.Counter;
import edu.berkeley.nlp.starcraft.util.FastPriorityQueue;
import edu.berkeley.nlp.starcraft.util.UnitUtils;
import org.bwapi.proxy.model.*;

import java.util.*;
import java.util.Map.Entry;
import edu.berkeley.nlp.starcraft.util.Vector;
/**
 * User: Ibrahim
 */
public class LurkerContain extends AbstractCerebrate implements Strategy {
    protected TilePosition myHome;
    protected Unit myBase;

    protected Player me;
    protected Player enemy;
    protected ArrayList<Unit> larvae = new ArrayList<Unit>();


    protected ArrayList<Unit> builders = new ArrayList<Unit>();
    protected ArrayList<Unit> army = new ArrayList<Unit>();
    protected ArrayList<Unit> extractors = new ArrayList<Unit>();
    protected int completedExtractors = 0;
    protected ArrayList<Unit> ovieggs = new ArrayList<Unit>();
    protected ArrayList<Unit> overlords = new ArrayList<Unit>();
    protected ArrayList<Unit> zerglings = new ArrayList<Unit>();
    protected ArrayList<Unit> hydras = new ArrayList<Unit>();
    protected ArrayList<Unit> lurkers = new ArrayList<Unit>();

    protected Unit pool = null;
    protected Unit nathatch = null;
    protected Unit hatch3 = null;
    protected Unit den = null;
    protected Unit natgas = null;
    protected Unit evo = null;

    protected BaseLocation nat;
    protected Bwta bwta;
    protected Game game;
    protected ArrayList<TilePosition> unexplored = new ArrayList<TilePosition>();
    protected Counter<BaseLocation> baseScores = new Counter<BaseLocation>();
    protected Set<Chokepoint> chokepoints;
    protected Set<TilePosition> startlocs;
    protected List<TilePosition> armyWaypoints;
    protected boolean haveLurkers = false;
    protected boolean foundEnemyBase = false;
    protected List<BaseLocation> baseLocations = new ArrayList<BaseLocation>();
    protected List<Position> basePositions= new ArrayList<Position>();
    protected BaseLocation myBaseLocation;

    protected BuildingManager buildingManager;
    protected LarvaeManager larvaeManager;
    protected ScoutManager scoutManager;
    protected ArmyManager armyManager;
    protected TechManager techManager;
    protected WorkerManager workerManager;

    protected ConvexHull baseHull;
    protected Position target;
    protected ConvexHull noBuildZone;
    protected TilePosition goodBuildSpot;
    protected int airUnitsSeen = 0;
    protected ArrayListMultimap<Unit, Unit> extToGuy = new ArrayListMultimap<Unit, Unit>();
    protected List<Unit> sunkens = new LinkedList<Unit>();
    protected Position rally;

    protected List<BaseLocation> bases = new LinkedList<BaseLocation>();

    public void buildFailed(UnitType type, TilePosition loc) {
        //To change body of created methods use File | Settings | File Templates.
    }

    public enum ArmyState{
        WAITING,
        MOVING,
        ATTACKING,
        DEFENDING
    };
    protected ArmyState armyState = ArmyState.WAITING;
    public enum OverlordState{
        WAITING,
        SCOUTING,
        CHILLING,
        FLEEING
    }
    protected Map<Unit, OverlordState> overlordStateMap = new HashMap<Unit, OverlordState>();
    @Override
    public void onStart() {
        System.out.println("Starting up");
        game = Game.getInstance();
        me = game.self();
        bwta = Bwta.getInstance();
        System.out.println("Base locations" + bwta.getBaseLocations().size());

        myHome = me.getStartLocation();
        for(BaseLocation b: bwta.getBaseLocations()){
            if(!myHome.equals(b.getTilePosition())){
                if(!b.isMineralOnly() && !b.isIsland()){
                    basePositions.add(b.getPosition());
                    baseLocations.add(b);
                }
            }else{
                myBaseLocation = b;
                bases.add(b);
            }
            
        }

        Position natpos = UnitUtils.getClosestPosition(Position.centerOfTile(me.getStartLocation()), basePositions);
        for(BaseLocation b : bwta.getBaseLocations()){
            if(b.getPosition().equals(natpos)){
                nat = b;
            }
        }
        startlocs = new HashSet<TilePosition>();
        for(BaseLocation b: bwta.getStartLocations()){
            b.getRegion();
            baseScores.setCount(b, 0);
            startlocs.add(b.getTilePosition());
            unexplored.add(b.getTilePosition());
        }

        chokepoints = bwta.getChokepoints();
        System.out.println("Chokepoints" + chokepoints.size());
        workerManager = new WorkerManager(game, this);
        buildingManager = new BuildingManager(game, this);
        larvaeManager = new LarvaeManager(game, this);
        System.out.println(larvaeManager);
        scoutManager = new ScoutManager(game, this);
        armyManager = new ArmyManager(game, this);
        techManager = new TechManager(game, this);

        for(ROUnit u: myBaseLocation.getMinerals()){
             workerManager.mineralmap.put(u, new LinkedList<Unit>());
        }

        for (Player p : game.getPlayers()) {
            if (p.isEnemy(me))
                enemy = p;
        }
        for (ROUnit u : me.getUnits()) {
            Unit unitc = UnitUtils.assumeControl(u);
            if (u.getType().isWorker()) {
                workerManager.assignWorker(unitc);
            } else if (u.getType().isResourceDepot()) {
                myBase = unitc;
            } else if(u.getType().equals(UnitType.ZERG_LARVA)){
                larvae.add(unitc);
            }
        }

        game.setLocalSpeed(0);
        System.out.println("Finished initializing");
        List<Position> basePts = new ArrayList<Position>();
        for(Position p: myBaseLocation.getRegion().getPolygon()){
            basePts.add(p);
        }
        for(Position p: nat.getRegion().getPolygon()){
            basePts.add(p);
        }
        baseHull = new ConvexHull(basePts);

        getMiningObstructionArea(myBaseLocation);
        buildingManager.addNoBuildZone(noBuildZone);
        target = new Position(pickRandomly(new ArrayList<TilePosition>(startlocs)));
        goodBuildSpot = new TilePosition(new Position(myHome).add(antiMineralDirection(myBaseLocation).toPosition()));
        rally = nat.getPosition().add(antiMineralDirection(nat).scale(1.6).toPosition());

    }

    private void getMiningObstructionArea(BaseLocation base) {
        ArrayList<Position> noBuild = new ArrayList<Position>();
        for(ROUnit min : base.getMinerals()){
            noBuild.add(min.getPosition().add(-min.getType().dimensionLeft(), 0));
            noBuild.add(min.getPosition().add(min.getType().dimensionRight(), 0));
            noBuild.add(min.getPosition().add(0, min.getType().dimensionDown()));
            noBuild.add(min.getPosition().add(0, -min.getType().dimensionUp()));
        }
        for(ROUnit geyser : base.getGeysers()){
            noBuild.add(geyser.getPosition().add(-geyser.getType().dimensionLeft(), 0));
            noBuild.add(geyser.getPosition().add(geyser.getType().dimensionRight(), 0));
            noBuild.add(geyser.getPosition().add(0, geyser.getType().dimensionDown()));
            noBuild.add(geyser.getPosition().add(0, -geyser.getType().dimensionUp()));
        }
        
        noBuild.add(myBase.getPosition().add(-myBase.getType().dimensionLeft(), 0));
        noBuild.add(myBase.getPosition().add(myBase.getType().dimensionRight(), 0));
        noBuild.add(myBase.getPosition().add(0, myBase.getType().dimensionDown()));
        noBuild.add(myBase.getPosition().add(0, -myBase.getType().dimensionUp()));
        noBuildZone = new ConvexHull(noBuild);
    }

    @Override
    public void onFrame() {
        int frameCount = game.getFrameCount();
        larvaeManager.larvaeManagement();
        //scoutAndHarass();

        if(frameCount %5 == 1){
            buildingManager.baseManagement();
            techManager.techManagement();
            buildingManager.processQueue();
        }
        if(frameCount %5 == 2){
            scoutManager.overlordManagement();
        }
        if(frameCount %5 == 3){
            workerManager.workerManagement();
        }
        if(frameCount %2 == 0){//FOR TEH MICRO (Mainly for overlords because they're vulnerable creatures)
            armyManager.armyManagement();
            scoutManager.overlordManagement();
        }
        if(frameCount %100 == 0){
            recountUnits();
        }
        if(frameCount % 30 == 0){
            trackEnemies();
            checkplaces();
        }
/*        if(frameCount % 300 == 0){
            moveCamera();
        }*/
        graphics();
    }

    private void moveCamera() {
        int thingy = game.getFrameCount()/300;
        Position p = new Position(myHome);
        if(thingy % 5 == 0){
        }else if(thingy % 5 == 0){
            p = pickRandomly(overlords).getPosition();
        }else if(thingy % 5 == 2){
        }else if(thingy % 5 == 3 && !army.isEmpty()){
            p = pickRandomly(army).getPosition();
        }
        if(thingy % 5 == 4){
            p = target;
        }
        p.add(-200, -100);//Seems to center it weird.
        game.setScreenPosition(p);

    }

    private void trackEnemies() {
        Set<? extends ROUnit> enemyUnits = enemy.getUnits();
        for(ROUnit u : enemyUnits){
            if(!u.isVisible()) continue;
            if(baseHull.withinHull(u.getPosition())){
                armyState = ArmyState.ATTACKING;
            }
        }
    }

    protected void saveOverlord(Unit o) {
        int units = 0;
        for(Unit z : zerglings){
            if(units>5) break;
            z.attackMove(o.getPosition());
            units++;
        }
    }

    /**
     * Fixes counts of all unit groups
     */
    private void recountUnits() {
        Set<? extends ROUnit> units = me.getUnits();
//        larvae.clear();;
        ovieggs.clear();
        overlords.clear();
        hydras.clear();
        lurkers.clear();
        zerglings.clear();
        for(ROUnit r : units){
            Unit u = UnitUtils.assumeControl(r);
            if(u.getType().equals(UnitType.ZERG_EGG)){
                if(u.getTrainingQueue().contains(UnitType.ZERG_OVERLORD)){
                    ovieggs.add(u);
                }
            }else if(u.getType().equals(UnitType.ZERG_OVERLORD)){
                overlords.add(u);
            }else if(u.getType().isWorker()){
                if(!workerManager.isWorker(u) && !buildingManager.isWorkerBuilding(u)){
                    workerManager.workers.add(u);
                    if(u.isIdle()) u.rightClick(UnitUtils.getClosest(u, game.getMinerals()));
                }
            }else if(u.getType().equals(UnitType.ZERG_ZERGLING)){
                zerglings.add(u);
            }else if(u.getType().equals(UnitType.ZERG_HYDRALISK)){
                hydras.add(u);
            }else if(u.getType().equals(UnitType.ZERG_LURKER)){
                lurkers.add(u);
            }
        }

//        if(hatches != null && !hatches.isEmpty()){//TODO FIXME
//            for(Unit u: hatches){//TODO I have no idea how it could get here and throw an exception
//                if(u != null && !u.getType().isBuilding() && !u.isMorphing() && u.isIdle()){
//                    hatches.remove(u);
//                }
//            }
//        }
        for(Unit u : extToGuy.keySet()){//Fix to make sure the right workers are mining gas from the right extractor.
            for(Unit v: extToGuy.get(u)){
                if(!v.isCarryingGas() && u != null && u.exists())
                    v.rightClick(u);//TODO: Make sure this thing exists.
                else{
                    if(!u.exists())
                        System.out.println("You are silly, geyser does not exist " + u);
                }
            }
        }
        army.clear();
        army.addAll(zerglings);
        army.addAll(hydras);
        army.addAll(lurkers);
    }

    private void checkplaces() {
        Iterator<TilePosition> iter = unexplored.iterator();
        while(iter.hasNext()){
            TilePosition tp = iter.next();
            if(game.isExplored(tp)){
                iter.remove();
            }
        }
    }

    protected boolean zerglingBalance() {
        return workerManager.workers.size()>12 && (zerglings.size() < workerManager.workers.size()*0.6 && (me.supplyUsed()/2<45 || workerManager.avgSaturation()>2)||
                (workerManager.workers.size()>40 && me.minerals()-me.gas()>400)) && (den==null || hydras.size()>zerglings.size()*.5);
    }
    protected boolean canBuild(UnitType type){
        return me.minerals() >= type.mineralPrice() && me.gas() >= type.gasPrice() && (me.supplyTotal()- me.supplyUsed()) >= type.supplyRequired();
    }

    @Override
    public void onEnd(boolean isWinnerFlag) {
        if(isWinnerFlag){
            System.out.println("I won!");
        }else{
            System.out.println("I lost!");
        }

    }

    @Override
    public void onSendText(String text) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onReceiveText(Player player, String text) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onPlayerLeft(Player player) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onNukeDetect(Position position) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onUnitCreate(ROUnit unit) {
        if(unit.getPlayer().equals(me)){
            Unit unitc = UnitUtils.assumeControl(unit);
            if(unit.getType().equals(UnitType.ZERG_LARVA)){
                larvae.add(unitc);
                larvae = new ArrayList<Unit>(new HashSet<Unit>(larvae));
            }else if (unit.getType().equals(UnitType.ZERG_ZERGLING)){
                zerglings.add(unitc);
            }
        }
    }

    @Override
    public void onUnitDestroy(ROUnit unit) {
        if(unit.getPlayer().equals(me)){
            if(unit.getType().equals(UnitType.ZERG_LARVA)){
                larvae.remove(UnitUtils.assumeControl(unit));
            }else if(unit.getType().equals(UnitType.ZERG_EGG)){
                ovieggs.remove(unit);
            }
            else if(unit.getType().isWorker()){
                extractors.remove(unit);
                workerManager.removeWorker(UnitUtils.assumeControl(unit));
            }else if(unit.getType().equals(UnitType.ZERG_ZERGLING)){
                zerglings.remove(unit);
            }else if(unit.getType().equals(UnitType.ZERG_HYDRALISK)){
                hydras.remove(unit);
                lurkers.remove(unit);
            }else if(unit.getType().equals(UnitType.ZERG_LURKER)){
                lurkers.remove(unit);
            }else if(unit.getType().isBuilding()){//TODO: Recover from losing buildings
                if(unit.getType().equals(UnitType.ZERG_CREEP_COLONY) || unit.getType().equals(UnitType.ZERG_SUNKEN_COLONY)){
                    sunkens.remove(unit);
                }
            }
        }else{//Enemy units
            if(unit.getType().isResourceContainer()){
                System.out.println("Resources can die too?" + unit);
            }
            if(unit.isFlying() && !unit.getType().equals(UnitType.ZERG_OVERLORD)){
                airUnitsSeen--;
            }
            if(unit.getType().isBuilding()){
                for(BaseLocation b: baseScores.keySet()){
                    if(b.getRegion().contains(unit.getPosition())){
                        System.out.println(unit.getPlayer());
                        baseScores.incrementCount(b, -1.0);//If we destroy a building, reduce a location's score
                    }
                }
            }
        }

    }

    @Override
    public void onUnitHide(ROUnit unit) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onUnitShow(ROUnit unit) {
        if(unit.getPlayer().isEnemy(me)){//Enemy units
            scoutManager.overlordDance(unit);
            if(unit.getType().isBuilding()){//Enemy buildings
                for(BaseLocation b: baseScores.keySet()){
                    if(b.getRegion().contains(unit.getPosition())){
                        System.out.println(unit.getPlayer());
                        baseScores.incrementCount(b, 1.0);
                        foundEnemyBase = true;
                    }
                }
                if(unit.getType().equals(UnitType.ZERG_SPIRE) || unit.getType().equals(UnitType.PROTOSS_STARGATE) ||
                       unit.getType().equals(UnitType.TERRAN_STARPORT) ){
                    game.printf("I saw a building that produces air units, need to prepare defenses or get a spire.");
                    buildingManager.buildSpire();
                }
            }else{//Enemy units
                for(BaseLocation b: baseScores.keySet()){//Units are helpful but less indicative
                    if(b.getRegion().contains(unit.getPosition())){
                        baseScores.incrementCount(b, 0.1);
                    }
                }
                if(baseHull.withinHull(unit.getPosition())){
                    armyState = ArmyState.ATTACKING;
                }
                if(unit.isFlying() && !unit.getType().equals(UnitType.ZERG_OVERLORD)){
                    airUnitsSeen++;
                    game.printf("I saw an air unit, thus I will make some defenses and fewer lurkers.");
                    buildingManager.buildSporeColony();
                    if(unit.getType().equals(UnitType.PROTOSS_CORSAIR)){
                        game.printf("I saw a corsair, overlords need to be more careful.");
                        scoutManager.sawCorsair();
                        scoutManager.overlordDance(unit);
                    }
                }

            }
        }else{//Not sure why you'd care when you see your own units, but just in case.

        }
    }

    @Override
    public void onUnitMorph(ROUnit unit) {
        Unit unitc = UnitUtils.assumeControl(unit);
        if (unit.getType().isWorker() && !workerManager.workers.contains(unit)) {
            workerManager.assignWorker(unitc);
        }else if(unit.getType().equals(UnitType.ZERG_LARVA)){
            larvae.add(unitc);
            System.out.println("Added a larvae");
        }else if(unit.getType().equals(UnitType.ZERG_EGG)){
            larvae.remove(unit);
        }else if(unit.getType().equals(UnitType.ZERG_OVERLORD)){
            System.out.println("Overlord morphed!");
            Unit u = unitc;
            ovieggs.clear();
            overlordStateMap.put(unitc, OverlordState.SCOUTING);
        }else if(unit.getType().isBuilding()){
            workerManager.removeWorker(unitc);//Just to make sure the builder got removed from the worker list.
            if(unit.getType().equals(UnitType.ZERG_SPAWNING_POOL)){
                pool = unitc;
            }else if(unit.getType().equals(UnitType.ZERG_EXTRACTOR)){
                completedExtractors++;
                extractors.add(unitc);
            }else if(unit.getType().equals(UnitType.ZERG_HATCHERY)){
            }
        }
        else if(unit.getType().equals(UnitType.ZERG_ZERGLING)){
            zerglings.add(unitc);
        }else if(unit.getType().equals(UnitType.ZERG_HYDRALISK)){
            hydras.add(unitc);
        }
    }

    @Override
    public void onUnitRenegade(ROUnit unit) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void onDroppedConnection() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected boolean buildCloseTo(Unit worker, TilePosition pos, UnitType building) {
        //if(builders.contains(worker)) return false;
        buildingManager.addToQueue(worker, building, pos);
        return true;
    }
    private void graphics() {
        for(BaseLocation b : Bwta.getInstance().getBaseLocations()){
            game.drawBoxMap(b.getPosition().x(), b.getPosition().y(), 32, 32, Color.CYAN, true);
        }
        for(Unit u: workerManager.workers){
            Color c;
            if(builders.contains(u)){
                c = Color.ORANGE;
            }else{
                c = Color.CYAN;
            }
            if(u.exists()) game.drawLineMap(u.getPosition(), u.getTargetPosition(), c);
        }
        for(Unit u : workerManager.gasminers){
            if(u.exists()) game.drawLineMap(u.getPosition(), u.getTargetPosition(), Color.GREEN);
        }
        if(nathatch!= null){
            game.drawBoxMap(nathatch.getPosition().x()-20, nathatch.getPosition().y()-20, nathatch.getPosition().x()+20, nathatch.getPosition().y() + 20, Color.PURPLE, false);
        }
        if(den!= null){
            game.drawBoxMap(den.getPosition().x()-20, den.getPosition().y()-20, den.getPosition().x()+20, den.getPosition().y() + 20, Color.PURPLE, false);
        }
        if(hatch3 != null){
            game.drawBoxMap(hatch3.getPosition().x()-20, hatch3.getPosition().y()-20, hatch3.getPosition().x()+20, hatch3.getPosition().y() + 20, Color.ORANGE, false);
        }
        for(Chokepoint c : chokepoints){
            Position center = c.getCenter();
            Position s1 = c.getSides().getKey();
            Position s2 = c.getSides().getValue();
            game.drawTriangleMap(center.x(), center.y(), s1.x(), s1.y(), s2.x(), s2.y(), Color.PURPLE, false);
        }
        int x = 0;
        for(Entry<BaseLocation, Double> e: baseScores.entrySet()){
            game.drawTextScreen(400, 80+x, e.getKey().getTilePosition()+": "+e.getValue());
            x+=15;
        }
        baseHull.draw();
        noBuildZone.draw();
        Position p = new Position(goodBuildSpot);
        game.drawBoxMap(p.x(), p.y(), 12, 12, Color.GREEN, false);
        game.drawTextScreen(400, 68, "Army state: " + armyState);
        game.drawTextScreen(500, 15, "Workers: " + workerManager.numWorkers());
        game.drawTextScreen(420, 15, "Larvae: " + larvae.size());
        game.drawTextScreen(420, 25, "Ovieggs: " + ovieggs.size());
        game.drawTextScreen(570, 15, "Lings: " + zerglings.size());
        game.drawTextScreen(570, 25, "Hydras: " + hydras.size());
        game.drawTextScreen(570, 35, "Lurkers: " + lurkers.size());
        game.drawTextScreen(500, 25, "On gas: " + workerManager.numGasMiners());
        game.drawTextScreen(400, 35, "Miners: " + workerManager.numMiners() + " Sat:" + workerManager.avgSaturation());
        game.drawTextScreen(500, 45, "Frame count: " + game.getFrameCount());
        game.drawTextScreen(500, 55, "Hatcheries: " + buildingManager.hatches.size());
        int i=0;
        for(Unit ext : extToGuy.keySet()){
            game.drawTextScreen(400, 150+10*i++, "Ext"+ext.getTilePosition()+": "+extToGuy.get(ext).size());
        }
        game.drawTextScreen(400, 75, "Gases: "+extractors.size());
        workerManager.drawDiagnostics();
    }

    @Override
    public List<Cerebrate> getTopLevelCerebrates() {
        return Arrays.<Cerebrate>asList(this);
    }
    
    protected ROUnit enemyToTarget(Unit myUnit, Set<? extends ROUnit> enemies){
        if(enemies.isEmpty()){
            System.out.println("[!] No enemies to target, what are you talking about?");
            return null;
        }
        FastPriorityQueue<ROUnit> units = new FastPriorityQueue<ROUnit>();
        for(ROUnit u: enemies){
            units.setPriority(u, -myUnit.getDistance(u));
        }
        FastPriorityQueue<ROUnit> closeUnits = new FastPriorityQueue<ROUnit>();
        double range = myUnit.getGroundRange();
        while(units.getPriority() > -1.5*range || closeUnits.isEmpty()){//TODO: Apparently this is broken for drones
            ROUnit en = units.next();
            closeUnits.setPriority(en, en.getHitPoints());
        }
        return closeUnits.getFirst();
    }

    public boolean canUpgrade(Game game, Unit unit, UpgradeType type) {
		if (game.self() == null) {
			return false;
		}
		if (unit != null) {
			if (unit.getPlayer() != game.self()) {
				return false;
			}
			if (!unit.getType().equals(type.whatUpgrades())) {
				return false;
			}
		}
		if (game.self().getUpgradeLevel(type) >= type.maxRepeats()) {
			return false;
		}
		if (game.self().minerals() < type.mineralPriceBase() + type.mineralPriceFactor()
		    * (game.self().getUpgradeLevel(type))) {
			return false;
		}
		if (game.self().gas() < type.gasPriceBase() + type.gasPriceFactor() * (game.self().getUpgradeLevel(type))) {
			return false;
		}
		return true;
	}
    public <E> E pickRandomly(List<E> list){
        return list.get((int)(Math.random()*list.size()));
    }
    public Vector antiMineralDirection(BaseLocation base){
        ROUnit min = UnitUtils.getClosest(base.getPosition(), base.getMinerals());
        return Vector.diff(min.getPosition(), base.getPosition());
    }
}
