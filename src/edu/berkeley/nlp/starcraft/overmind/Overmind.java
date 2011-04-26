package edu.berkeley.nlp.starcraft.overmind;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.bwapi.proxy.ProxyBot;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;

import edu.berkeley.nlp.starcraft.Cerebrate;
import edu.berkeley.nlp.starcraft.Strategy;
import edu.berkeley.nlp.starcraft.util.Log;

/**
 * Entry point for the agent. Please keep this general enough that it can be
 * shared across all of our agents.
 * 
 * @author denero, nickjhay, dlwh
 */
public class Overmind implements ProxyBot {
	// Defaults
	public static final String DEFAULT_PROPERTIES_FILE = "resources/overmind.properties";

	// Fixed parts of an agent
	final public Properties agentParameters;
	
	public static Log logger = Log.getLog("Overmind");

	public static Random random = new Random();

	List<Cerebrate> cerebrates;

	private final Strategy strategy;

	public Overmind(Strategy strategy, Properties properties) {
		this.agentParameters = properties;
		this.strategy = strategy;
	}


	private void initialize() {
		try {
			Game game = Game.getInstance();
			game.enableFlag(org.bwapi.proxy.model.Flag.USER_INPUT);
			this.cerebrates = new ArrayList<Cerebrate>();

			cerebrates.addAll(strategy.getTopLevelCerebrates());
		} catch (Exception e) {
			logger.fatal("Exception in initialization:", e);
			throw new RuntimeException(e);
		}

	}

	@Override
	public void onStart() {
		try {

			if (agentParameters.containsKey(Log.LOG_KEY)) {
				Log.setLogFile(agentParameters.getProperty(Log.LOG_KEY));
			}
			Log.initLogger(Level.INFO, Level.DEBUG);
		} catch (Exception e) {
			Game.getInstance()
					.printf("Logger init exception " + e.getMessage());
			int i = 0;
			for (StackTraceElement elem : e.getStackTrace()) {
				if (++i > 10)
					break;
				Game.getInstance().printf(elem.toString());
			}
		}
		initialize();

		try {
			logger.info("Overmind online.");
			for (Entry<Object, Object> entry : agentParameters.entrySet()) {
				logger.info("  " + entry.getKey() + " = " + entry.getValue());
			}

			logger.info("Strategy: " + strategy);
			
			for(Cerebrate c: cerebrates) {
				c.onStart();
			}
		} catch (Exception e) {
			logger.fatal("Exception " + e);
			for (StackTraceElement elem : e.getStackTrace()) {
				logger.fatal(elem.toString());
			}
		}
	}

	@Override
	public void onFrame() {
		//if(Game.getInstance().isPaused()) return;
		try {
			for(Cerebrate c: cerebrates) {
				try {
					c.onFrame();
				} catch (Exception e) {
					logger.fatal("Exception in onFrame of cerebrate " + c, e);
				}
			}

		} catch (Throwable e) {
			logger.fatal("Exception in onFrame", e);
		}
	}

	@Override
	public void onSendText(String text) {
		for(Cerebrate c: cerebrates) 
			try {
				c.onSendText(text);
			} catch (Exception e) {
				logger.fatal("Exception in onFrame of cerebrate " + c, e);
			}
	}

	@Override
	public void onEnd(boolean isWinnerFlag) {
		for(Cerebrate c: cerebrates) 
			try {
				c.onEnd(isWinnerFlag);
			} catch (Exception e) {
				logger.fatal("Exception in onFrame of cerebrate " + c, e);
			}
	}

	@Override
	public void onPlayerLeft(Player player) {
		for(Cerebrate c: cerebrates) 
			try {
				c.onPlayerLeft(player);
			} catch (Exception e) {
				logger.fatal("Exception in onPlayerLeft of cerebrate " + c, e);
			}
	}

	@Override
	public void onNukeDetect(Position position) {
		for(Cerebrate c: cerebrates) 
			try {
				c.onNukeDetect(position);
			} catch (Exception e) {
				logger.fatal("Exception in onNukeDetect of cerebrate " + c, e);
			}
	}

	@Override
	public void onReceiveText(Player player, String text) {
		for(Cerebrate c: cerebrates) 
			try {
				c.onReceiveText(player,text);
			} catch (Exception e) {
				logger.fatal("Exception in onReceiveText of cerebrate " + c, e);
			}
	}

	@Override
	public void onDroppedConnection() {
		for(Cerebrate c: cerebrates) 
			try {
				c.onDroppedConnection();
			} catch (Exception e) {
				logger.fatal("Exception in onDroppedConnection of cerebrate " + c, e);
			}
	}

	@Override
	public void onUnitCreate(ROUnit unit) {
		for(Cerebrate c: cerebrates) 
			try {
				c.onUnitCreate(unit);
			} catch (Exception e) {
				logger.fatal("Exception in onUnitCreate of cerebrate " + c, e);
			}
	}

	@Override
	public void onUnitDestroy(ROUnit unit) {
		for(Cerebrate c: cerebrates) 
			try {
				c.onUnitDestroy(unit);
			} catch (Exception e) {
				logger.fatal("Exception in onUnitDestroy of cerebrate " + c, e);
			}
	}
	
	@Override
	public void onUnitHide(ROUnit unit) {
		for(Cerebrate c: cerebrates) 
			try {
				c.onUnitHide(unit);
			} catch (Exception e) {
				logger.fatal("Exception in onUnitHide of cerebrate " + c, e);
			}
	}

	@Override
	public void onUnitMorph(ROUnit unit) {
		for(Cerebrate c: cerebrates) 
			try {
				c.onUnitMorph(unit);
			} catch (Exception e) {
				logger.fatal("Exception in onUnitMorph of cerebrate " + c, e);
			}
	}

	@Override
	public void onUnitRenegade(ROUnit unit) {
		for(Cerebrate c: cerebrates) 
			try {
				c.onUnitRenegade(unit);
			} catch (Exception e) {
				logger.fatal("Exception in onUnitRenegade of cerebrate " + c, e);
			}
	}

	@Override
	public void onUnitShow(ROUnit unit) {
		for(Cerebrate c: cerebrates) 
			try {
				c.onUnitShow(unit);
			} catch (Exception e) {
				logger.fatal("Exception in onUnitShow of cerebrate " + c, e);
			}
	}
}
