package tournament2;

import org.bwapi.proxy.model.*;

import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Ibrahim
 * Date: 4/15/11
 * Time: 1:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class ScoutManager {
    private Game game;
    private LurkerContain master;
    protected boolean corsairs = false;
    private boolean hasOverlordSpeed = false;
    public ScoutManager(Game game, LurkerContain lurkerContain){
        this.game = game;
        this.master = lurkerContain;
    }
    void overlordDance(ROUnit unit) {
        for (Unit u: master.overlords){
            if(unit.getDistance(u)<= UnitType.ZERG_OVERLORD.sightRange() || corsairs){
                master.overlordStateMap.put(u, LurkerContain.OverlordState.FLEEING);
            }
        }
    }

    void overlordManagement() {
        for(Unit o: master.overlords){//Silliness
            if(!master.overlordStateMap.containsKey(o)){
                master.overlordStateMap.put(o, LurkerContain.OverlordState.SCOUTING);
            }
        }
        if(!master.foundEnemyBase){
            Iterator<TilePosition> bit = master.startlocs.iterator();
            TilePosition next = master.myHome;
            if(master.startlocs.size()<2)System.out.println("You suck, where are the starting locations!");
            for(Unit o: master.overlords){
                if(bit.hasNext()){
                    next = bit.next();
                }
                if(o.isIdle() && o.getDistance(new Position(next))>100){
                    o.rightClick(next);

                }else{

                }
            }
        }else{
            boolean saveTheArmy = true;
            Iterator<Position> bit = master.basePositions.iterator();
            for(Unit o: master.overlords){
                if(o.isUnderAttack()){
                    master.overlordStateMap.put(o, LurkerContain.OverlordState.FLEEING);
                    master.saveOverlord(o);
                }
                switch(master.overlordStateMap.get(o)){
                    case WAITING:
                        break;
                    case SCOUTING:
                        if(saveTheArmy && master.armyState == LurkerContain.ArmyState.ATTACKING){
                            o.rightClick(master.target);
                            saveTheArmy = false;
                        }else if(o.isIdle() && bit.hasNext()){
//                            Position p = bit.next();
//                            if(p.getDistance(o.getPosition())>100)
//                                o.rightClick(p);
                            o.rightClick(master.pickRandomly(master.basePositions));
                        }
                        break;
                    case FLEEING:
                        if(!o.getTargetPosition().equals(master.myHome)){
                            o.rightClick(master.myHome);
                        }
                        if(master.myBaseLocation.getRegion().contains(o.getTilePosition())){
                            master.overlordStateMap.put(o, LurkerContain.OverlordState.SCOUTING);
                        }
                        break;
                }
            }
        }
    }

    public void sawCorsair() {
        //To change body of created methods use File | Settings | File Templates.
        //TODO: Implement runny away
        corsairs = true;
    }

    public void gotOverlordSpeed() {
        //To change body of created methods use File | Settings | File Templates.
        hasOverlordSpeed = true;
    }
}
