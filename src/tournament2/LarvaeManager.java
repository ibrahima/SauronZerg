package tournament2;

import edu.berkeley.nlp.starcraft.util.UnitUtils;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

/**
 * Created by IntelliJ IDEA.
 * User: Ibrahim
 * Date: 4/15/11
 * Time: 1:02 PM
 * To change this template use File | Settings | File Templates.
 */
public class LarvaeManager {
    private Game game;
    private LurkerContain master;
    private int mineralsReserved = 0;
    private int gasReserved = 0;
    private int workerCounter = 0;
    public LarvaeManager(Game game, LurkerContain lurkerContain){
        this.game = game;
        this.master = lurkerContain;
    }

    /**
     * Reserves some minerals for building something.
     *
     * If building a unit would prevent you from being able to build something, this would prevent it from doing that.
     * @param minerals
     * @param gas
     */
    public void setReservation(int minerals, int gas){
        mineralsReserved = minerals;
        gasReserved = gas;
    }
    public void clearReservation(){
        mineralsReserved = 0;
        gasReserved = 0;
    }

    private boolean satisfyReservation(UnitType type){
        return master.me.minerals()-type.mineralPrice() >= mineralsReserved &&
                master.me.gas() - type.gasPrice() >= gasReserved;
    }
    public void larvaeManagement() {
        if(master.larvae.isEmpty()){
            for(ROUnit u : UnitUtils.getAllMy(UnitType.ZERG_LARVA)){
                master.larvae.add(UnitUtils.assumeControl(u));
            }
        }
        try{
            if(master.larvae.isEmpty())return;
            if(master.me.supplyTotal() - master.me.supplyUsed()<5 && master.me.minerals()>=100 && master.ovieggs.size()<(1 + master.me.supplyUsed()/40)
                    && master.me.supplyTotal()<400){
                Unit u = master.larvae.remove(0);
                u.morph(UnitType.ZERG_OVERLORD);
                master.ovieggs.add(u);
            }
            if(master.larvae.isEmpty())return;
            if(master.den != null && master.den.getType().isBuilding() && master.den.isCompleted() && !master.larvae.isEmpty() && master.hydras.size()<= master.lurkers.size()+6+master.airUnitsSeen/2){
                Unit u = master.larvae.remove(0);
                u.train(UnitType.ZERG_HYDRALISK);
            }
            if(master.larvae.isEmpty())return;
            if(master.scoutManager.corsairs && !master.buildingManager.buildingsByType(UnitType.ZERG_SPIRE).isEmpty()){
                Unit u = master.larvae.remove(0);
                u.train(UnitType.ZERG_SCOURGE);
            }
            if(master.me.supplyUsed()/2<=30 ||
                    (master.den!= null && (master.den.isMorphing()||master.den.isCompleted()) )){
                if(master.pool != null && master.me.minerals()>=50 && master.me.supplyUsed() < master.me.supplyTotal()
                        && master.zerglingBalance()){
                    master.larvae.remove(0).morph(UnitType.ZERG_ZERGLING);
                }
                if(master.larvae.isEmpty())return;
                if(master.me.minerals()>=50 && master.me.supplyUsed() < master.me.supplyTotal()){
                    Unit u = master.larvae.remove(0);
                    u.morph(UnitType.ZERG_DRONE);
                    //workers.add(u);
                }
            }
        }catch(RuntimeException e){
            System.out.println("Some sort of stupidity with larvae going on, not sure why.");
        }
    }
}
