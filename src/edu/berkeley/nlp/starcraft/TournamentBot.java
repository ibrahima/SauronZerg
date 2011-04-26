package edu.berkeley.nlp.starcraft;

import edu.berkeley.nlp.starcraft.overmind.Overmind;
import org.bwapi.proxy.ProxyBot;
import org.bwapi.proxy.ProxyBotFactory;
import org.bwapi.proxy.ProxyServer;
import org.bwapi.proxy.model.Game;
import tournament2.LurkerContain;

import java.util.Properties;

public class TournamentBot {
	public static void main(String[] args) {
		ProxyBotFactory factory = new ProxyBotFactory() {

			@Override
			public ProxyBot getBot(Game g) {
                return new Overmind(new LurkerContain(),new Properties());
			}
			
		};
		String heartbeatFilename = args.length > 1 ? args[1] : null;
		new ProxyServer(factory,ProxyServer.extractPort(args.length> 0 ? args[0] : null), heartbeatFilename).run();

	}

}
