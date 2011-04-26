package tournament2;

import edu.berkeley.nlp.starcraft.util.ConvexHull;
import edu.berkeley.nlp.starcraft.util.UnitUtils;
import org.bwapi.proxy.model.*;
import org.bwapi.proxy.util.Pair;
import edu.berkeley.nlp.starcraft.collect.ArrayListMultimap;

import java.util.*;
import java.util.Map.Entry;

/**
 * Created by IntelliJ IDEA.
 * User: Ibrahim
 * Date: 3/15/11
 * Time: 2:22 PM
 * Keep track of build commands:
 * Worker ordered, building type, location.
 */
public class BuildingManager {
    private Game game;
    private HashMap<Unit, Pair<UnitType, TilePosition>> builders = new HashMap<Unit, Pair<UnitType, TilePosition>>();
    private Set<UnitType> buildingsInQueue = new HashSet<UnitType>();
    private HashMap<UnitType, Unit> buildingsInProgress = new HashMap<UnitType, Unit>();
    private List<ConvexHull> noBuildies = new LinkedList<ConvexHull>();
    private LurkerContain master;
    private ArrayListMultimap<UnitType, Unit> buildings = new ArrayListMultimap<UnitType, Unit>();
    private Set<BaseLocation> bases = new HashSet<BaseLocation>();
    protected Set<Unit> hatches = new HashSet<Unit>();
    private WorkerManager workerManager;
    private List<Unit> spores = new LinkedList<Unit>();

    public BuildingManager(Game game, LurkerContain lurkerContain){
        this.game = game;
        this.master = lurkerContain;
        bases.add(master.myBaseLocation);
        hatches.add(master.myBase);
        workerManager = master.workerManager;
    }

    public void processQueue(){
        Iterator<Entry<Unit, Pair<UnitType, TilePosition>>> iter = builders.entrySet().iterator();
        while(iter.hasNext()){
            Entry<Unit, Pair<UnitType, TilePosition>> entry = iter.next();
            Unit worker = entry.getKey();
            UnitType type = entry.getValue().getFirst();
            TilePosition loc = entry.getValue().getSecond();
            if(!game.isVisible(loc) && worker.isIdle()) worker.rightClick(loc);
            /*if(worker.isCompleted()){
                
            }else */
            if(!worker.exists()){
                iter.remove();
                buildingsInQueue.remove(type);
                System.out.println("Removed "+type+"at "+loc+" from the build queue, the worker died or something.");
                master.buildFailed(type, loc);
            }
            if(worker.isMorphing()){
                iter.remove();
                buildingsInQueue.remove(type);

                System.out.println("Removed "+type+"at "+loc+" from the build queue, it's morphing." + worker.getRemainingBuildTime());
                //This is just in the case of multiple buildings of the same type being built, only track the first one.
                if(!buildingsInProgress.containsKey(type))
                    buildingsInProgress.put(type, worker);
            }else if(type.equals(UnitType.ZERG_EXTRACTOR)){
                if(worker.isIdle()){
                    if(game.isVisible(loc)){
                        System.out.println("< > Special case for extractor...");
                        worker.build(loc, type);
                    }else{
                        worker.rightClick(loc);
                    }
                }
            } else if(buildCloseTo(worker, loc, type)){
                System.out.println("Maybe built "+type+"at "+loc);
//                if(type.equals(UnitType.ZERG_EXTRACTOR)){
//                    iter.remove();
//                    buildingsInQueue.remove(type);
//                    buildings.put(type, worker);
//                }
            }
        }
        Iterator<Entry<UnitType, Unit>> ipIter = buildingsInProgress.entrySet().iterator();
        while(ipIter.hasNext()){
            Entry<UnitType, Unit> entry = ipIter.next();
            UnitType type = entry.getKey();
            Unit worker = entry.getValue();
            if(worker.getRemainingBuildTime()<15){
                System.out.println("[x] Removed "+type+" from in progress, it's done");
                ipIter.remove();//Remove stuff that's finished building
                buildings.put(type, worker);
                if(type.equals(UnitType.ZERG_HATCHERY) || type.isResourceDepot()){
                    System.out.println("[?] Checking if this is a new base");
                    BaseLocation b = master.bwta.getNearestBaseLocation(worker.getTilePosition());
                    hatches.add(worker);
                    if(!bases.contains(b)){
                        bases.add(b);
                        System.out.println("Adding a new base");
                        workerManager.addBase(b);
                    }
                }
            }
        }
    }
    public void addToQueue(Unit worker, UnitType type, TilePosition location){
        builders.put(worker, new Pair<UnitType, TilePosition>(type, location));
        buildingsInQueue.add(type);
        worker.rightClick(location);
    }
    public int queueLength(){
        return builders.size();
    }
    public List<Unit> buildingsByType(UnitType type){
        return buildings.get(type);
    }
    public boolean isInQueue(UnitType type){
        return buildingsInQueue.contains(type);
    }
    public boolean isWorkerBuilding(Unit worker){
        return builders.containsKey(worker);
    }
    public void addNoBuildZone(ConvexHull zone){
        noBuildies.add(zone);
    }
    private boolean eligibleBuildSpot(Position p){
        for(ConvexHull c : noBuildies){
            if(c.withinHull(p))
                return false;
        }
        return true;
    }
    private boolean buildCloseTo(Unit worker, TilePosition pos, UnitType building) {
        //if(builders.contains(worker)) return false;
        if(!master.canBuild(building)){
            return false;
        }
        if(worker.getOrder().equals(Order.ZERG_BUILDING_MORPH)) return true;//It's already trying to build, stop spamming.
        System.out.println("[ ] Trying to build a "+building+" at "+ pos);
        Queue<TilePosition> q = new LinkedList<TilePosition>();
        Set<TilePosition> visited = new HashSet<TilePosition>();
        q.add(pos);
        int tries = 0;
        Set<? extends ROUnit> units = game.unitsOnTile(pos);
        for(ROUnit u: units){
            if(u.getPlayer().equals(game.self())){
                Unit unit = UnitUtils.assumeControl(u);
                unit.move(pos.add(2,2));
            }
        }
        if(!game.isVisible(pos))return false;
        while(!q.isEmpty()){
            TilePosition tp = q.poll();
            tries++;
            if(tries>500)break;
            visited.add(tp);
            Position p = new Position(tp);
            if(worker.canBuildHere(tp, building) && eligibleBuildSpot(p)
            /*&& (game.unitsOnTile(tp).isEmpty() || building.equals(UnitType.ZERG_EXTRACTOR))*/
                    ){
                worker.build(tp, building);
                System.out.println("[+] Building a " + building + " at " + pos);
                game.drawLineMap(worker.getPosition(), Position.centerOfTile(tp), Color.YELLOW);
                return true;
            }else{
                for(TilePosition succ : Arrays.asList(tp.add(0, 2),tp.add(2, 0),tp.add(0, -2),tp.add(-2, 0))){
                    if(!visited.contains(succ)){
                        q.add(succ);
                    }
                }
            }
        }
        System.out.println("( ) Failed to build a "+building+" at "+ pos);
        //game.printf("Failed to build a %s at %s", building, pos);
        game.drawBoxMap(pos.x(), pos.y(), 16, 16, Color.RED, true);
        return false;
    }

    void baseManagement() {
        if(master.me.gas()>=100){
            master.myBase.train(UnitType.ZERG_LAIR);
        }
        if(master.me.gas()>600 && master.myBase.getType().equals(UnitType.ZERG_LAIR)){
            master.myBase.train(UnitType.ZERG_HIVE);
        }
        if(master.myBase==null){
            System.out.println("OMFG");
            master.myBase = UnitUtils.assumeControl(UnitUtils.getAllMy(UnitType.ZERG_HATCHERY).iterator().next());
        }
        if(master.myBase.isBeingConstructed() && master.myBase.getOrderTimer()<UnitType.ZERG_HYDRALISK_DEN.buildTime()*.4 && master.den == null){
            master.den = workerManager.getWorkerForBuilding();
        }
        if(master.den != null && master.canBuild(UnitType.ZERG_HYDRALISK_DEN) && master.den.canMake(UnitType.ZERG_HYDRALISK_DEN) &&
                !master.den.isBeingConstructed()  && !master.den.isMorphing() && !master.den.isConstructing()){
            master.buildCloseTo(master.den, master.goodBuildSpot, UnitType.ZERG_HYDRALISK_DEN);
        }
        if(master.natgas == null && master.myBase.isBeingConstructed() && master.myBase.getOrderTimer()<UnitType.ZERG_HYDRALISK_DEN.buildTime()*.1){
            master.natgas = workerManager.getWorkerForBuilding();
            master.natgas.rightClick(master.nat.getTilePosition());
        }
        if(master.natgas != null && master.canBuild(UnitType.ZERG_HYDRALISK_DEN) && !master.natgas.isBeingConstructed()){
            try{
                ROUnit geyser = master.nat.getGeysers().iterator().next();
                master.natgas.build(geyser.getTilePosition(), UnitType.ZERG_EXTRACTOR);
            }catch (RuntimeException e){
                System.out.println("Natural gas destroyed but it thinks it's still an extractor, or something.");
            }
        }
        if(master.me.supplyUsed()/2>=12 && master.me.minerals()>250 && hatches.size()<2){
            System.out.println("Expanding to nat");
            master.nathatch = workerManager.getWorkerForBuilding();
            hatches.add(master.nathatch);
            master.nathatch.rightClick(master.nat.getTilePosition());
            System.out.println("Hatchery!");
            addToQueue(master.nathatch, UnitType.ZERG_HATCHERY, master.nat.getTilePosition());
        }
//
//        if(master.nathatch != null && master.me.minerals() >= UnitType.ZERG_HATCHERY.mineralPrice() && master.nathatch.getType().isWorker() &&
//                master.nathatch.isIdle() && !master.nathatch.isBeingConstructed()
//                && master.game.isVisible(master.nat.getTilePosition()) && hatches.size()==2){
//            master.buildCloseTo(master.nathatch, master.nat.getTilePosition(), UnitType.ZERG_HATCHERY);
//
//        }
        if(master.hatch3 == null && master.nathatch != null && master.nathatch.getType().isBuilding() && master.pool != null && master.pool.getType().isBuilding() &&
                master.me.supplyUsed()/2 >=13 && hatches.size()<3 && master.canBuild(UnitType.ZERG_HATCHERY)){
            master.hatch3 = workerManager.getWorkerForBuilding();
            hatches.add(master.hatch3);
            master.hatch3.move(master.myBaseLocation.getPosition());
            addToQueue(master.hatch3, UnitType.ZERG_HATCHERY, master.myBaseLocation.getTilePosition());
        }
        if(hatches.size() >=2 && master.me.supplyUsed()/2>=11 && master.me.minerals()>=200 && master.pool == null &&
                master.nathatch != null && master.nathatch.getType().isBuilding()){
            System.out.println(master.me.supplyUsed());
            if(master.pool == null) master.pool = workerManager.getWorkerForBuilding();
            master.buildCloseTo(master.pool, master.goodBuildSpot, UnitType.ZERG_SPAWNING_POOL);
            System.out.println("Spawning pool!" + master.me.supplyUsed());
        }
        if(master.me.supplyUsed()/2>=16 && master.me.minerals()>=75 && master.extractors.isEmpty()){
            System.out.println(master.me.supplyUsed());
            Unit ex = workerManager.getWorkerForBuilding();
            master.extractors.add(ex);
            TilePosition p = master.myBaseLocation.getGeysers().iterator().next().getTilePosition();
            addToQueue(ex, UnitType.ZERG_EXTRACTOR, p);
//            extractors.get(0).build(p, UnitType.ZERG_EXTRACTOR);
            System.out.println("Extractor");
        }
        if(hatches.size()==3 && master.me.minerals()>600 && workerManager.numWorkers()>24 &&
                !isInQueue(UnitType.ZERG_HATCHERY)){
            Unit worker = workerManager.getWorkerForBuilding();
            hatches.add(worker);
            Position loc = master.nat.getPosition();
            worker.move(loc);
            addToQueue(worker, UnitType.ZERG_HATCHERY, new TilePosition(loc));
        }
        if(hatches.size()>3 && hatches.size()<10 && master.me.minerals()>900 && workerManager.numWorkers()>24 &&
                (!isInQueue(UnitType.ZERG_HATCHERY) || master.me.minerals()>1600)){
            Unit worker = workerManager.getWorkerForBuilding();
            hatches.add(worker);
            BaseLocation base = master.pickRandomly(master.baseLocations);
            worker.move(base.getPosition());
            addToQueue(worker, UnitType.ZERG_HATCHERY, new TilePosition(base.getPosition()));
            try{
                Unit extractor = workerManager.getWorkerForBuilding();
                Position extp = base.getGeysers().iterator().next().getPosition();
                extractor.move(extp);
                addToQueue(extractor, UnitType.ZERG_EXTRACTOR, new TilePosition(extp));

            }catch (RuntimeException e){
                System.out.println("Well, I guess there's no extractor there.");
            }

            System.out.println("Going for extra bases!");
        }
        if(master.myBase.getType().equals(UnitType.ZERG_LAIR) && master.evo != null && master.canBuild(UnitType.ZERG_QUEENS_NEST) && master.me.gas()>600 &&
                !isInQueue(UnitType.ZERG_QUEENS_NEST) && UnitUtils.getAllMy(UnitType.ZERG_QUEENS_NEST).isEmpty()){
            Unit worker = workerManager.getWorkerForBuilding();
            master.buildCloseTo(worker, master.goodBuildSpot, UnitType.ZERG_QUEENS_NEST);
        }
        if(master.evo == null && !master.hydras.isEmpty() && master.canBuild(UnitType.ZERG_EVOLUTION_CHAMBER) &&
                !isBuildingOrBuilt(UnitType.ZERG_EVOLUTION_CHAMBER) && master.lurkers.size()>1){
            master.evo = workerManager.getWorkerForBuilding();
            addToQueue(master.evo, UnitType.ZERG_EVOLUTION_CHAMBER, master.goodBuildSpot);
            System.out.println("Getting an evo chamber!");
        }
        TilePosition natp = new TilePosition(master.nat.getPosition().add(master.antiMineralDirection(master.nat).scale(1.2).toPosition()));
        if(master.pool != null && master.me.supplyUsed()>20 && master.game.hasCreep(natp.x(), natp.y())&& master.canBuild(UnitType.ZERG_CREEP_COLONY) &&
                !isInQueue(UnitType.ZERG_CREEP_COLONY) && master.sunkens.size()<3){
            Unit worker = workerManager.getWorkerForBuilding();
            master.sunkens.add(worker);
            addToQueue(worker, UnitType.ZERG_CREEP_COLONY, natp);
            System.out.println("Getting a creep colony");
        }
        for(Unit u: master.sunkens){
            if(u.getType().equals(UnitType.ZERG_CREEP_COLONY) && !u.isMorphing()){
                u.morph(UnitType.ZERG_SUNKEN_COLONY);
            }
        }
        for(Unit u: spores){
            if(u.getType().equals(UnitType.ZERG_CREEP_COLONY) && !u.isMorphing()){
                u.morph(UnitType.ZERG_SPORE_COLONY);
            }
        }
    }

    public void buildingDestroyed(Unit building){
        if(buildings.containsKey(building.getType())){
            buildings.get(building.getType()).remove(building);
        }
        System.out.println("[:( Removed a destroyed building.");
    }

    public void buildSporeColony() {
        //To change body of created methods use File | Settings | File Templates.
        //TODO: Build spore colony.
        if(spores.size()<5){
            Unit worker = workerManager.getWorkerForBuilding();
            addToQueue(worker, UnitType.ZERG_CREEP_COLONY, master.myBaseLocation.getMinerals().iterator().next().getTilePosition());
            spores.add(worker);
        }
    }

    private void buildCloseTo(Unit worker, Position position, UnitType type) {
        //To change body of created methods use File | Settings | File Templates.
        buildCloseTo(worker, new TilePosition(position), type);
    }

    public void buildSpire() {
        //To change body of created methods use File | Settings | File Templates.
        if(!isBuildingOrBuilt(UnitType.ZERG_SPIRE)){
            Unit worker = workerManager.getWorkerForBuilding();
            addToQueue(worker, UnitType.ZERG_SPIRE, master.goodBuildSpot);
        }
    }

    protected boolean isBuildingOrBuilt(UnitType type) {
        return isInQueue(type) || buildingsInProgress.containsKey(type)
            || buildings.containsKey(type);
    }
}
