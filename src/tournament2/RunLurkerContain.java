package tournament2;

import edu.berkeley.nlp.starcraft.GeneralTest;
import org.bwapi.unit.model.BroodwarGameType;
import org.bwapi.unit.model.BroodwarRace;
import org.junit.Test;

public class RunLurkerContain extends GeneralTest {
	@Test
	public void runMining() {
		GeneralTest.test(new LurkerContain(), tourneyMap("SpaceUMS","t3"), new BroodwarRace[]{BroodwarRace.Zerg,BroodwarRace.Protoss}, BroodwarGameType.MELEE);
	}
}
