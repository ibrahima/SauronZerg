package tournament2;

import edu.berkeley.nlp.starcraft.util.FastPriorityQueue;
import edu.berkeley.nlp.starcraft.util.UnitUtils;
import org.bwapi.proxy.model.*;

import java.util.*;
import java.util.Map.Entry;

/**
 * Created by IntelliJ IDEA.
 * User: Ibrahim
 * Date: 4/15/11
 * Time: 1:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ArmyManager {
    private Game game;
    private LurkerContain master;

    public ArmyManager(Game game, LurkerContain lurkerContain){
        this.game = game;
        this.master = lurkerContain;
    }

    void armyManagement() {
        switch (master.armyState){
            case WAITING:
                if(master.zerglings.size()/2 + master.hydras.size() + master.lurkers.size()>20 && !master.lurkers.isEmpty()){
                    master.armyState = LurkerContain.ArmyState.ATTACKING;
                    for(Unit l: master.lurkers){
                        l.unburrow();
                    }
                }
                for(Unit u: master.army){
                    if(u.isIdle() && u.getDistance(master.rally)>250){
                        u.move(master.rally);
                    }
                }
                for(Unit h : master.hydras){
                    if(h.isIdle()){
                        if(h.canMake(UnitType.ZERG_LURKER) && master.airUnitsSeen<5+ master.hydras.size()/2){
                            h.morph(UnitType.ZERG_LURKER);
                            master.lurkers.add(h);
                        }
                    }
                }
                for(Unit l : master.lurkers){
                    if(l.isUnderAttack() && !l.isBurrowed()){
                        l.burrow();
                    }
                    if(l.isBurrowed()){
                        ROUnit closestenemy = UnitUtils.getClosest(l, master.enemy.getUnits());
                        if(closestenemy != null && l.isInRange(closestenemy)){//This shouldn't have to be manual I don't get it.
                            l.attack(closestenemy);
                        }
                    }
                }
                break;
            case DEFENDING:
            case ATTACKING:
                if(master.zerglings.size()/2 + master.hydras.size() + master.lurkers.size()<20 && master.armyState != LurkerContain.ArmyState.DEFENDING){
                    master.armyState = LurkerContain.ArmyState.WAITING;
                }

                if(!master.enemy.getUnits().isEmpty()){
                    master.target = master.enemy.getUnits().iterator().next().getLastKnownPosition();
                }else if(!master.unexplored.isEmpty()){
                    master.target = new Position(master.unexplored.get(0));
                }else{
                    master.target = master.baseScores.argMax().getPosition();
                }
                for(Unit z : master.zerglings){
                    if(z.isIdle()){
                        z.attackMove(master.target);
                    }else{
//                        if(game.getFrameCount() % 600 == 4){//TODO: This just makes lings go back a little, but it's stupid
//                            z.move(z.getPosition().add(Vector.diff(z.getPosition(), target).normalize().scale(50.0).toPosition()));
//                        }
                    }
                }
                Set<? extends ROUnit> enemies = master.enemy.getUnits();
                for(Unit h : master.hydras){
                    if(h.isIdle()){
                        if(h.canMake(UnitType.ZERG_LURKER) && master.airUnitsSeen<5+ master.hydras.size()/2){
                            h.morph(UnitType.ZERG_LURKER);
                            master.lurkers.add(h);
                        }else{
                            if(h.getDistance(master.target)>800){
                                h.attackMove(master.target);
                            }else{
                                try{
                                    h.attack(master.enemyToTarget(h, enemies));
                                }catch(Exception e){
                                    h.attackMove(master.target);
                                }
                            }
                        }
                    }
                }
                for(Unit l : master.lurkers){
                    ROUnit closestenemy = UnitUtils.getClosest(l, master.enemy.getUnits());
                    if(l.isUnderAttack() || (closestenemy != null && l.isInRange(closestenemy))){
                        l.burrow();
                    }
                    else{
                        l.unburrow();
                        l.move(master.target);
                    }
                }
            break;
        }
    }

    List<Chokepoint> chokePath(Position start, Position end) {
    	List<Chokepoint> currList;
    	Region startRegion = getRegionOfPos(start);
    	Region endRegion = getRegionOfPos(end);
    	FastPriorityQueue<List<Chokepoint>> fringe = new FastPriorityQueue<List<Chokepoint>>();
    	List<Chokepoint> nextChokes = getPossNextChoke(startRegion);
    	for (Chokepoint c: nextChokes) {
    		List<Chokepoint> cl = new ArrayList<Chokepoint>();
    		cl.add(c);
    		fringe.setPriority(cl, getCost(cl, start)+getHeuristic(cl, end));
    	}
    	/*
    	 * First set of possible moves are now on fringe (special case where
    	 * prev point was the start position)
    	 */
    	while(!fringe.isEmpty()) {
    		currList = fringe.removeFirst();
    		//Goal check
    		Chokepoint lastChoke = currList.get(currList.size()-1);
    		if (lastChoke.getRegions().getKey().equals(endRegion) || lastChoke.getRegions().getValue().equals(endRegion)) {
    			return currList;
    		}
    		/*
    		 * Find the region that wasn't used before (if first iteration, then
    		 * it uses the region that wasn't the start region, otherwise uses
    		 * region that wasn't shared with the previous chokepoint)
    		 */
    		if (currList.size() < 2) {
    			if (currList.get(0).getRegions().getValue().equals(startRegion)) {
    				nextChokes = getPossNextChoke(currList.get(0).getRegions().getKey());
    			} else {
    				nextChokes = getPossNextChoke(currList.get(0).getRegions().getValue());
    			}
    		} else {
    			nextChokes = getPossNextChoke(sharedRegion(currList.get(currList.size()-1), currList.get(currList.size()-2)));
    		}
    		nextChokes.remove(currList.get(currList.size()-1)); //remove current chokepoint
    		//nextChokes is now next possible chokes to go to
    		for(Chokepoint c: nextChokes) {
    			List<Chokepoint> cl = new ArrayList<Chokepoint>();
    			cl.addAll(currList);
    			cl.add(c);
    			fringe.setPriority(cl, getCost(cl, start)+getHeuristic(cl, end));
    		}
    		//Next possible moves are now added to fringe
    	}
    	System.out.println("Didn't find path to endRegion???");
    	return null;
    }

    List<Chokepoint> getPossNextChoke(Region r1, Region r2) {
    	Iterator<Chokepoint> chokeIter = master.chokepoints.iterator();
    	Chokepoint currChoke;
    	List<Chokepoint> toRet = new ArrayList<Chokepoint>();
    	while (chokeIter.hasNext()) {
    		currChoke = chokeIter.next();
    		Region r3 = currChoke.getRegions().getKey();
    		Region r4 = currChoke.getRegions().getValue();
    		if (r3.equals(r1) || r3.equals(r2) || r4.equals(r1) || r4.equals(r2)) {
    			toRet.add(currChoke);
    		}
    	}
    	return toRet;
    }

    List<Chokepoint> getPossNextChoke(Region r1) {
    	Iterator<Chokepoint> chokeIter = master.chokepoints.iterator();
    	Chokepoint currChoke;
    	List<Chokepoint> toRet = new ArrayList<Chokepoint>();
    	while (chokeIter.hasNext()) {
    		currChoke = chokeIter.next();
    		Region r3 = currChoke.getRegions().getKey();
    		Region r4 = currChoke.getRegions().getValue();
    		if (r3.equals(r1) || r4.equals(r1)) {
    			toRet.add(currChoke);
    		}
    	}
    	return toRet;
    }

    Region getRegionOfPos(Position p) {
    	Iterator<Chokepoint> chokeIter = master.chokepoints.iterator();
    	Chokepoint currChoke;
    	while (chokeIter.hasNext()) {
    		currChoke = chokeIter.next();
    		if (currChoke.getRegions().getValue().contains(p)) {
    			return currChoke.getRegions().getValue();
    		}
    		if (currChoke.getRegions().getKey().contains(p)) {
    			return currChoke.getRegions().getKey();
    		}
    	}
    	System.out.println("Position is not in any region?");
    	return null; //Not in any region???
    }

    //Must be consecutive chokepoints
    Region sharedRegion(Chokepoint c1, Chokepoint c2) {
    	Map.Entry<Region, Region> r1s = c1.getRegions();
    	Entry<Region, Region> r2s = c2.getRegions();
    	if (r1s.getKey().equals(r2s.getKey()) || r1s.getKey().equals(r2s.getValue())) {
    		return r1s.getKey();
    	} else {
    		return r1s.getValue();
    	}
    }

    double getCost(List<Chokepoint> chokes, Position start) {
    	Iterator<Chokepoint> chokeIter = chokes.iterator();
    	double toRet = 0.0;
    	Chokepoint prevChoke;
    	Chokepoint currChoke;
    	if (chokeIter.hasNext()) {
    		currChoke = chokeIter.next();
    	} else {
    		return 0;
    	}
    	toRet += currChoke.getCenter().getDistance(start);
    	while (chokeIter.hasNext()) {
    		prevChoke = currChoke;
    		currChoke = chokeIter.next();
    		toRet += prevChoke.getCenter().getDistance(currChoke.getCenter());
    	}
    	return toRet;
    }

    double getHeuristic (List<Chokepoint> chokes, Position end) {
    	Chokepoint endChoke = chokes.get(chokes.size()-1);
    	return endChoke.getCenter().getDistance(end);
    }
}
