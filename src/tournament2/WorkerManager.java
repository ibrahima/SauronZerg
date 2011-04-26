package tournament2;

import edu.berkeley.nlp.starcraft.util.Counter;
import edu.berkeley.nlp.starcraft.util.FastPriorityQueue;
import edu.berkeley.nlp.starcraft.util.UnitUtils;
import org.bwapi.proxy.model.*;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Ibrahim
 * Date: 4/15/11
 * Time: 3:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class WorkerManager {
    private LurkerContain lurkerContain;
    protected HashMap<ROUnit, LinkedList<Unit>> mineralmap = new HashMap<ROUnit, LinkedList<Unit>>();
    private HashMap<Unit, ROUnit> minerToPatch = new HashMap<Unit, ROUnit>();
    protected HashSet<Unit> workers = new HashSet<Unit>();
    protected ArrayList<Unit> gasminers = new ArrayList<Unit>();
    protected ArrayList<Unit> miners = new ArrayList<Unit>();

    private Game game;
    public WorkerManager(Game game, LurkerContain lurkerContain){
        this.game = game;
        this.lurkerContain = lurkerContain;
    }
    void assignWorker(Unit worker) {
        workers.add(worker);
        if(worker.getType().isWorker()){
            if(!saturatedGas(lurkerContain)){
                gasminers.add(worker);
                for(Unit ext : lurkerContain.extractors){
                    if(lurkerContain.extToGuy.get(ext).size()>=3)
                        continue;
                    lurkerContain.extToGuy.put(ext, worker);
                    if(ext.getRemainingBuildTime()>0){

                    }
                    
                }

            }else{
                miners.add(worker);
                ROUnit closestPatch = assignMiner(worker);
                mineralmap.get(closestPatch).add(worker);
                minerToPatch.put(worker, closestPatch);
                worker.rightClick(closestPatch);
            }
        }else{
            System.out.println("Why is a " + worker + " masquerading as a worker?");
            workers.remove(worker);
        }
    }

    void workerManagement() {
        List<Unit> toremove = new LinkedList<Unit>();
        for (Unit u : miners) {
            if(!u.getType().isWorker() && !u.getType().equals(UnitType.ZERG_EGG) && !u.getType().equals(UnitType.ZERG_LARVA)){
                System.out.println("Non-worker " + u.getType());
                toremove.add(u);
            }
            if (minerToPatch.containsKey(u) && u.exists() && (!u.isCarryingMinerals() || u.isIdle()) && !u.isGatheringMinerals()  ||
                    (!lurkerContain.baseHull.withinHull(u.getPosition()) && (u.isAttacking() ||u.getTarget()!=null))) {
                u.rightClick(minerToPatch.get(u));//TODO: Bug with this line apparently.
            }
            Set<? extends ROUnit> enemies = lurkerContain.enemy.getUnits();
            for (ROUnit e : lurkerContain.enemy.getUnits()) {
                if (lurkerContain.baseHull.withinHull(e.getPosition()) && e.isVisible() && e.getTarget()!= null && e.getDistance(u)<500) {
                    ROUnit enta = lurkerContain.enemyToTarget(u, enemies);
                    if(enta != null)
                        u.attack(enta);//TODO: This blows up against a scouting worker?
                    else
                        u.attack(e);
                }
            }
        }
        miners.removeAll(toremove);
    }

    private ROUnit assignMiner(Unit worker){
        //1. Get a list of the unsaturated mineral patches.
        //2. Find the closest one.
        Position workerpos = worker.getPosition();
        FastPriorityQueue<ROUnit> minerals = new FastPriorityQueue<ROUnit>();
        for(Map.Entry<ROUnit, LinkedList<Unit>> min : mineralmap.entrySet()){
            minerals.setPriority(min.getKey(), (2-min.getValue().size())*2000 + min.getKey().getDistance(workerpos));
        }
        return minerals.getFirst();
    }

    protected void removeWorker(Unit worker){
        for(Map.Entry<ROUnit, LinkedList<Unit>> entry : mineralmap.entrySet()){
            entry.getValue().remove(worker);
        }
        workers.remove(worker);
        miners.remove(worker);
        gasminers.remove(worker);
    }

    protected void addBase(BaseLocation base){
        for(ROUnit u: base.getMinerals()){
             mineralmap.put(u, new LinkedList<Unit>());
        }
        transferWorkers(base);
    }
    private void transferWorkers(BaseLocation base){
        int unitsLeft = 5;
        Iterator<? extends ROUnit> iter = base.getMinerals().iterator();
        for(LinkedList<Unit> ws : mineralmap.values()){
            if(!ws.isEmpty()){
                Unit u = ws.pop();
                unitsLeft--;
                if(!iter.hasNext())
                    iter = base.getMinerals().iterator();
                ROUnit min = iter.next();
                mineralmap.get(min).add(u);
                u.rightClick(min);
            }
            if(unitsLeft<=0)break;
        }
    }
    protected boolean saturatedGas(LurkerContain lurkerContain) {
        return gasminers.size() >= lurkerContain.completedExtractors*3;
    }

    private void rebalanceGasMiners(LurkerContain lurkerContain) {
        List<Unit> extraGasWorkers = new ArrayList<Unit>();
        List<Unit> gmCopy = new ArrayList<Unit>(gasminers);
        Counter<Unit> geysersDeficit = new Counter<Unit>();
        for(Unit geyser : lurkerContain.buildingManager.buildingsByType(UnitType.ZERG_EXTRACTOR)){
            BaseLocation gbase = lurkerContain.myBaseLocation;
            for(BaseLocation b: lurkerContain.baseLocations){
                if(b.getRegion().contains(geyser.getTilePosition())){
                    gbase = b;
                }
            }
            Iterator<Unit> iter = gmCopy.iterator();
            int guys = 0;
            while(iter.hasNext()){//Loop through all unlocated gas miners
                Unit worker = iter.next();
                if(gbase.getRegion().contains(worker.getLastKnownTilePosition())){//This worker is on this gas, basically
                    iter.remove();
                    guys++;
                    if(guys>3){
                        System.out.println(geyser.getTilePosition() +" has too many miners ");
                        extraGasWorkers.add(worker);
                    }
                }
            }
            if(guys<3){
                System.out.println(geyser.getTilePosition() + " only has " +guys);
                geysersDeficit.setCount(geyser, 3-guys);
            }
        }

        for(Map.Entry<Unit, Double> gEntry : geysersDeficit.entrySet()){
            Unit geyser = gEntry.getKey();
            int deficit = gEntry.getValue().intValue();
            Iterator<Unit> iter = extraGasWorkers.iterator();
            while(deficit>0 && iter.hasNext()){
                Unit w = iter.next();
                iter.remove();
                if(geyser!= null && geyser.getType().equals(UnitType.ZERG_EXTRACTOR) &&
                        geyser.isCompleted() && !geyser.getPosition().equals(Position.INVALID))
                    w.rightClick(geyser);
            }
        }
    }
    protected int numWorkers(){
        return workers.size();
    }

    protected int numMiners(){
        return miners.size();
    }

    protected int numGasMiners(){
        return gasminers.size();
    }
    protected boolean isWorker(Unit unit){
        return workers.contains(unit);
    }

    protected Unit getWorkerForBuilding(){
        if(miners.isEmpty()){
            System.out.println("No more workers :(");
            return null;
        }
        Unit worker = miners.remove(0);
        this.removeWorker(worker);
        return worker;
    }

    protected void drawDiagnostics(){
        for(Map.Entry<ROUnit, LinkedList<Unit>> entry : mineralmap.entrySet()){
            ROUnit min = entry.getKey();
            for(Unit w : entry.getValue()){
                game.drawLineMap(min.getPosition(), w.getPosition(), Color.PURPLE);
            }
        }

    }

    public double avgSaturation(){
        double sat = 0;
        int numPatches = 0;
        for(Map.Entry<ROUnit, LinkedList<Unit>> entry : mineralmap.entrySet()){
            if(!entry.getValue().isEmpty()){
                numPatches++;
                sat +=entry.getValue().size();
            }
        }
        return sat/(double)numPatches;
    }
}
