package edu.berkeley.nlp.starcraft.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Color;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Position;

import edu.berkeley.nlp.starcraft.overmind.Overmind;

public class ConvexHull {
	private static class Line {
		private final Vector pointOnHull;
		private final Vector endpoint;
		
		public Line(Vector pointOnHull, Vector endpoint) {
			this.pointOnHull = pointOnHull;
			this.endpoint = endpoint;
		}
	}
	
	List<Vector> hull;
	private List<Line> lines;
	
	public ConvexHull(Collection<Position> positions) {
		Set<Vector> inputs = new HashSet<Vector>();
		for(Position u : positions) {
			inputs.add(new Vector(u));
		}
		
		lines = new ArrayList<Line>();
		hull = getConvexHull(inputs);
	}
	
	public int size() { return hull.size(); }
	
	public Vector closestPointOnHull(Vector toTest) {
		if(hull.size()==1) return hull.get(0);
		
		double minDist = Double.MAX_VALUE;
		Vector best = null;
		for(int i = 0; i < hull.size(); i++) {
			Vector a = hull.get( (i+1) % hull.size());
			Vector b = hull.get(i);
			Vector closest = Vector.ClosestPointOnLineSegment(a, b, toTest);
			double distance = closest.distanceTo(toTest);
			if(minDist > distance) {
				minDist = distance;
				best = closest;
			}
		}
		return best;
	}

	public double distanceToHull(Vector toTest) {
		if(withinHull(toTest)) {
			return 0.0;
		}
		return closestPointOnHull(toTest).distanceTo(toTest);
	}	
	
	public boolean withinHull(Vector toTest) {
		if(hull.size() == 0) return false;
		if(hull.size() == 1) return toTest.equals(hull.get(0));
		// check if all cross products are the same sign
		int sign = 0;
		for(int i = 0; i < hull.size(); i++) {
			Vector line = hull.get(i == 0 ? hull.size()-1 : i-1).sub(hull.get(i));
			Vector point = toTest.sub(hull.get(i));
			
			int newSign = sign(point.crossProduct(line));
			
			if (newSign != 0 && sign != 0 && newSign != sign) {
				return false;
			}
			sign = newSign;
		}
		return true;	
	}
	
	int sign(double val) {
		if (val > 0) return 1;
		if (val < 0) return -1;
		return 0;
	}


	private List<Vector> getConvexHull(Set<Vector> enemies) {

		List<Vector> hull = new ArrayList<Vector>();
		if(enemies.size()==0 || enemies.size()==1) {
			for(Vector enemy : enemies) {
				hull.add(enemy);
			}
			return hull;
		}
		Vector leftmost = null;
		double x = Double.MAX_VALUE;

		for(Vector u : enemies) {
			if(u.dx<x) {
				x = u.dx;
				leftmost = u;
			}
		}

		Vector pointOnHull = leftmost;

		Iterator<Vector> tmp = enemies.iterator();
		Vector endpoint = tmp.next();
		if(endpoint.equals(leftmost)) {
			endpoint = tmp.next();
		}

		int counter = 0;
		while(!(endpoint.equals(leftmost)) && counter < enemies.size()) {
			hull.add(pointOnHull);
			endpoint = null;
			for(Vector enemy : enemies) {
				if(endpoint == null) {
					if(!enemy.equals(pointOnHull)) endpoint = enemy;
					else continue;
				}
				if(enemy.equals(endpoint) || enemy.equals(pointOnHull)) continue;
				double angle1 = enemy.sub(pointOnHull).getAngle();
				double angle2 = endpoint.sub(pointOnHull).getAngle();
				double diff = angle1-angle2;
				if(diff < 0) diff += Math.PI*2;
				if(diff > 2*Math.PI) diff -= Math.PI*2;
				if(diff < Math.PI) {
					endpoint = enemy;
				}
			}
			lines.add(new Line(pointOnHull, endpoint));
			pointOnHull = endpoint;
			counter++;
		}

		return hull;
	}
	
	public void draw() {
		for (Line l : lines) {
			Game.getInstance().drawLineMap((int)l.pointOnHull.dx, (int)l.pointOnHull.dy, (int)l.endpoint.dx, (int)l.endpoint.dy, Color.WHITE);
		}
	}

	public boolean isEmpty() {
		return hull.isEmpty();
	}

	public Vector normalFromNearestVertex(Vector vector) {
		if (hull.size() <= 1) {
			return new Vector(Overmind.random.nextDouble() - 0.5, Overmind.random.nextDouble() - 0.5).normalize();
		}
		int closestVertex = nearestIndex(vector);
		Vector v = hull.get(closestVertex);
		Vector a = v.sub(hull.get(closestVertex == 0 ? hull.size() - 1 : closestVertex - 1));
		Vector b = v.sub(hull.get(closestVertex == hull.size() - 1 ? 0 : closestVertex + 1));
		return a.add(b).normalize();
	}
	
	private int nearestIndex(Vector vector)  {
		double minDist = Double.MAX_VALUE;
		int best = -1;
		int i = 0;
		for (Vector v : hull) {
			double dist = v.distanceTo(vector);
			if (dist < minDist) {
				minDist = dist;
				best = i;
			}
			++i;
		}
		return best;
	}

	public Vector nearestVertex(Vector vector) {
		int idx = nearestIndex(vector);
		if (idx >= 0) return hull.get(idx);
		return null;
	}

	public boolean withinHull(Position p) {
	  return withinHull(new Vector(p));
  }

}
