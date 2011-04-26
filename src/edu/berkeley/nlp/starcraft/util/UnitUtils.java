package edu.berkeley.nlp.starcraft.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Player;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;
import org.bwapi.proxy.model.WeaponType;
import org.bwapi.proxy.util.Pair;

public class UnitUtils {
	public static Log logger = Log.getLog("UnitUtils");

	public static <T extends ROUnit> T getClosest(ROUnit u, Collection<T> opps) {
		return getClosestAndDistance(u, opps).getFirst();
	}
	
	public static Unit assumeControl(ROUnit r) {
	  return ((Unit)r).commandableUnit(Game.getInstance().getDefaultValidator());
	}
 
	public static double getClosestDistance(ROUnit u, Collection<? extends ROUnit> opps) {
		return getClosestAndDistance(u, opps).getSecond();
	}
	
	public static <T extends ROUnit> Pair<T,Double> getClosestAndDistance(ROUnit u, Collection<T> opps) {
		T max = null;
		double distance = Double.POSITIVE_INFINITY;

		for (T other : opps) {
			double otherDistance = u.getDistance(other);
			if (distance > otherDistance) {
				distance = otherDistance;
				max = other;
			}
		}
		return Pair.makePair(max,distance);
	}

	public static <T extends ROUnit> T getClosest(Position pos, Collection<T> opps) {
		return getClosestAndDistance(pos, opps).getFirst();
	}

	public static double getClosestDistance(Position pos, Collection<? extends Unit> opps) {
		return getClosestAndDistance(pos, opps).getSecond();
	}

	public static <T extends ROUnit> Pair<T,Double> getClosestAndDistance(Position pos, Collection<T> opps) {
		T max = null;
		double distance = Double.POSITIVE_INFINITY;

		for (T other : opps) {
			double otherDistance = other.getLastKnownPosition().getDistance(pos);
			if (distance > otherDistance) {
				distance = otherDistance;
				max = other;
			}
		}
		return Pair.makePair(max,distance);
	}

	public static Position getClosestPosition(Position pos, Collection<Position> opps) {
		Position max = null;
		double distance = Double.POSITIVE_INFINITY;

		for (Position other : opps) {
			double otherDistance = other.getDistance(pos);
			if (distance > otherDistance) {
				distance = otherDistance;
				max = other;
			}
		}
		return max;
	}
	
	public static List<ROUnit> getAllMy(UnitType type) {
		List<ROUnit> us  = new ArrayList<ROUnit>();
		for (ROUnit u : Game.getInstance().self().getUnits()) {
			if (u.getType().equals(type)) us.add(u);
		}
		return us;
	}

	public static Position avePos(Collection<? extends ROUnit> units) {
		int x = 0, y = 0, n = 0;
		for (ROUnit u : units) {
			x += u.getPosition().x();
			y += u.getPosition().y();
			n++;
		}
		if(n==0) return Position.INVALID;
		return new Position(x / n, y / n);
	}

	public static double groupRadius(Collection<? extends ROUnit> units) {
		Position center = avePos(units);
		double r = 0;
		for (ROUnit u : units) {
			r = Math.max(r, u.getLastKnownPosition().getDistance(center));
		}
		return r;
	}

	public static Position medianPos(Collection<? extends ROUnit> units) {
		int[] xes = new int[units.size()];
		int[] yes = new int[units.size()];
		int i=0;
		for (ROUnit u : units) {
			xes[i] = u.getLastKnownPosition().x();
			yes[i++] = u.getLastKnownPosition().y();
		}
		Arrays.sort(xes);
		Arrays.sort(yes);
		int x = pickMedian(xes);
		int y = pickMedian(yes);
		return new Position(x, y);
	}

	private static int pickMedian(int[] sortedArray) {
		if (sortedArray.length % 2 == 1) {
			return sortedArray[sortedArray.length / 2];
		} else {
			int mid = sortedArray.length / 2;
			return (sortedArray[mid-1] + sortedArray[mid]) / 2;
		}
	}

	// TODO move these into some other class that deals with utilities
	
	public static int getUtility(UnitType type) {
		int cost = getCost(type);
		int futureValue = type.isWorker() ? 50 : 0;
		if (type.equals(UnitType.ZERG_HATCHERY) || type.equals(UnitType.ZERG_LAIR) || type.equals(UnitType.ZERG_HIVE) ||
				type.equals(UnitType.PROTOSS_NEXUS) || type.equals(UnitType.TERRAN_COMMAND_CENTER)) {
			futureValue = 300;
		}
		return cost + futureValue;
	}

	public static int getCost(UnitType type) {
		if (type.equals(UnitType.PROTOSS_ARCHON)) {
			return 2 * getCost(UnitType.PROTOSS_HIGH_TEMPLAR);
		}
		if (type.equals(UnitType.PROTOSS_DARK_ARCHON)) {
			return 2 * getCost(UnitType.PROTOSS_DARK_TEMPLAR);
		}
		int cost = type.mineralPrice() + type.gasPrice() * 3;
		if (type.isTwoUnitsInOneEgg()) cost /= 2;
		if (type.getName().startsWith("ZERG") && type.whatBuilds() != null &&
				!type.whatBuilds().getKey().equals(UnitType.NONE) &&
				!type.whatBuilds().getKey().equals(UnitType.ZERG_LARVA)) {
			cost += getCost(type.whatBuilds().getKey());
		}
		return cost;
	}

	// TODO: burkett, taylor, anyone? 
	public static int getRange(WeaponType weapon, Player player) {
	  // TODO Auto-generated method stub
	  return 0;
  }
	

}
