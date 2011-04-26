package edu.berkeley.nlp.starcraft;

import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;

public interface Cerebrate extends Underling {
	void onStart();
	void onFrame();
	void onEnd(boolean isWinnerFlag);

	void onSendText(String text);
	void onReceiveText(Player player, String text);
	void onPlayerLeft(Player player);
	void onNukeDetect(Position position);

	void onUnitCreate(ROUnit unit);
	void onUnitDestroy(ROUnit unit);	
	void onUnitHide(ROUnit unit);
	void onUnitShow(ROUnit unit);
	void onUnitMorph(ROUnit unit);
	void onUnitRenegade(ROUnit unit);

	void onDroppedConnection();
}
