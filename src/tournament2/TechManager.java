package tournament2;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.TechType;
import org.bwapi.proxy.model.UnitType;
import org.bwapi.proxy.model.UpgradeType;

/**
 * Created by IntelliJ IDEA.
 * User: Ibrahim
 * Date: 4/15/11
 * Time: 1:11 PM
 * To change this template use File | Settings | File Templates.
 */
public class TechManager {
    private Game game;
    private LurkerContain lurkerContain;

    public TechManager(Game game, LurkerContain lurkerContain){
        this.game = game;
        this.lurkerContain = lurkerContain;
    }
    void techManagement() {
        if(lurkerContain.pool != null && !lurkerContain.pool.isResearching()&& lurkerContain.canUpgrade(game, lurkerContain.pool, UpgradeType.METABOLIC_BOOST)){
            System.out.println("Zergling speed");
            lurkerContain.pool.upgrade(UpgradeType.METABOLIC_BOOST);
        }
        if(lurkerContain.pool != null && lurkerContain.myBase.getType().equals(UnitType.ZERG_HIVE) &&
                lurkerContain.canUpgrade(lurkerContain.game, lurkerContain.pool, UpgradeType.ADRENAL_GLANDS)){
            System.out.println("Cracklings =D");
            lurkerContain.pool.upgrade(UpgradeType.ADRENAL_GLANDS);
        }
        if(lurkerContain.myBase.getType().equals(UnitType.ZERG_LAIR) && lurkerContain.den != null && lurkerContain.game.canResearch(lurkerContain.den, TechType.LURKER_ASPECT)){
            lurkerContain.den.research(TechType.LURKER_ASPECT);
        }
        if(lurkerContain.den != null && lurkerContain.canUpgrade(lurkerContain.game, lurkerContain.den, UpgradeType.MUSCULAR_AUGMENTS) && !lurkerContain.lurkers.isEmpty()){
            lurkerContain.den.upgrade(UpgradeType.MUSCULAR_AUGMENTS);
        }
        if(lurkerContain.den != null && lurkerContain.canUpgrade(lurkerContain.game, lurkerContain.den, UpgradeType.GROOVED_SPINES) && !lurkerContain.lurkers.isEmpty()){
            lurkerContain.den.upgrade(UpgradeType.GROOVED_SPINES);
        }
        if(lurkerContain.evo != null && lurkerContain.canUpgrade(lurkerContain.game, lurkerContain.evo, UpgradeType.ZERG_MELEE_ATTACKS) && !lurkerContain.lurkers.isEmpty()){
            lurkerContain.evo.upgrade(UpgradeType.ZERG_MELEE_ATTACKS);
        }
        if(lurkerContain.evo != null && lurkerContain.canUpgrade(lurkerContain.game, lurkerContain.evo, UpgradeType.ZERG_MISSILE_ATTACKS) && !lurkerContain.lurkers.isEmpty()){
            lurkerContain.evo.upgrade(UpgradeType.ZERG_MISSILE_ATTACKS);
        }
        if(lurkerContain.evo != null && lurkerContain.canUpgrade(lurkerContain.game, lurkerContain.evo, UpgradeType.ZERG_CARAPACE) && !lurkerContain.lurkers.isEmpty()){
            lurkerContain.evo.upgrade(UpgradeType.ZERG_CARAPACE);
        }
        if(lurkerContain.canUpgrade(lurkerContain.game, lurkerContain.myBase, UpgradeType.PNEUMATIZED_CARAPACE) && !lurkerContain.lurkers.isEmpty()){
            lurkerContain.myBase.upgrade(UpgradeType.PNEUMATIZED_CARAPACE);
            lurkerContain.scoutManager.gotOverlordSpeed();
        }
    }
}
