package edu.berkeley.nlp.starcraft;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import org.bwapi.proxy.ProxyBot;
import org.bwapi.proxy.ProxyBotFactory;
import org.bwapi.proxy.model.Game;
import org.bwapi.unit.BwapiTestCase;
import org.bwapi.unit.BwapiTestInformation;
import org.bwapi.unit.model.BroodwarFileMap;
import org.bwapi.unit.model.BroodwarGameType;
import org.bwapi.unit.model.BroodwarPlayer;
import org.bwapi.unit.model.BroodwarRace;

import edu.berkeley.nlp.starcraft.overmind.Overmind;

/**
 * Uses the overmind.properties file to parameterize a test game
 * 
 * @author dlwh
 */
public class GeneralTest extends BwapiTestCase {

	private static final String CHAOS_LAUNCHER_DIR = "c:/starcraftresources/chaoslauncher/";
	private static final String STARCRAFT_DIR = inferStarcraftDir();
	private static final String MAPS_DIR = "c:/starcraftresources/maps/";

	private static String inferStarcraftDir() {
		return guessX64() ? "c:/program files (x86)/starcraft/" : "c:/program files/starcraft/";
	}
	

	private static boolean guessX64() {
		return new File("c:\\program files (x86)").exists();
	}
	
	public static File tourneyMap(final String name) {
		File[] files = new File(MAPS_DIR,"t4").listFiles(new FilenameFilter() {

			@Override
      public boolean accept(File arg0, String arg1) {
	      return arg1.contains(name) && !arg1.contains("ob.s");
      }
			
		});
		return files[0];
	}
	
	public static File tourneyMap(final String name, String mapsDir) {
		File[] files = new File(MAPS_DIR,mapsDir).listFiles(new FilenameFilter() {

			@Override
      public boolean accept(File arg0, String arg1) {
	      return arg1.contains(name) && !arg1.contains("ob.s");
      }
			
		});
		return files[0];
	}

	public static void test(final Strategy strat, File mapPath, BroodwarRace[] races, BroodwarGameType gameType) {

		// Run Brood War
		ArrayList<BroodwarPlayer> players = new ArrayList<BroodwarPlayer>();
		
		for(BroodwarRace race: races) {
			players.add(new BroodwarPlayer(players.isEmpty(), race));
		}

		System.out.println("Starting " + Arrays.toString(races) + " game on "
				+ mapPath.getName());
		
		final ProxyBotFactory fac = new ProxyBotFactory() {

			@Override
      public ProxyBot getBot(Game g) {
	      return new Overmind(strat,new Properties());
      }
			
		};
		
		BwapiTestInformation info = new BwapiTestInformation(STARCRAFT_DIR,
				CHAOS_LAUNCHER_DIR, fac, new BroodwarFileMap(mapPath.getAbsoluteFile()), gameType,  players.toArray(new BroodwarPlayer[0]));
		try {
			execute(info);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}



}
