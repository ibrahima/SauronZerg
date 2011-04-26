package edu.berkeley.nlp.starcraft.util;

import java.util.Set;

import org.bwapi.proxy.model.Position;

public class Vector {
	public static final Vector ZERO = new Vector(0, 0);

	public static final Vector[] ALLDIRS = new Vector[] {
		new Vector(-1,-1),new Vector(-1,0),new Vector(-1,1),
		new Vector( 0,-1),new Vector( 0,0),new Vector( 0,1),
		new Vector( 1,-1),new Vector( 1,0),new Vector( 1,1),
	};
	
	public double dx;
	public double dy;
	
	public Vector(double dx, double dy) {
		this.dx = dx;
		this.dy = dy;
	}
	
	public Vector(Position pos) {
	  this.dx = pos.x();
	  this.dy = pos.y();
  }

	public Vector rotateCCWInPlace(double angle) {
		double tmpdx = Math.cos(angle)*dx-Math.sin(angle)*dy;
		dy = Math.sin(angle)*dx+Math.cos(angle)*dy;
		dx = tmpdx;
		return this;
	}
	public Vector rotateCCW(double angle) {
		return new Vector(Math.cos(angle)*dx-Math.sin(angle)*dy,Math.sin(angle)*dx+Math.cos(angle)*dy);
	}
	
	public Vector extendToward(Vector other, double scale) {
		return other.sub(this).normalize(scale).add(this);
	}
	
	public double distanceTo(Position p) {
		return distanceTo(p.x(), p.y());
	}
	
	public double length() {
		return Math.sqrt(dx*dx+dy*dy);
	}

	public double distanceTo(Vector other) {
		return distanceTo(other.dx, other.dy);
	}
	
	public double distanceTo(double x, double y) {
		return Math.sqrt((dx-x)*(dx-x)+(dy-y)*(dy-y));
	}
	
	public Position toPosition() {
		return new Position((int)dx, (int)dy);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Vector)) return false;
		Vector v = (Vector)obj;
		return v.dx == dx && v.dy == dy;
	}
	
	@Override
	public int hashCode() {
		return (int)(Double.doubleToLongBits(dx) * 31 + Double.doubleToLongBits(dy));
	}
	
	public Vector add(Vector v) {
		return new Vector(dx + v.dx, dy + v.dy);
	}
	
	public Vector addInPlace(Vector v) {
		dx += v.dx;
		dy += v.dy;
		return this;
	}

	public Vector sub(Vector v) {
		return new Vector(dx - v.dx, dy - v.dy);
	}
	
	public double dot(Vector v) {
		return dx*v.dx + dy*v.dy;
	}
	
	public double crossProduct(Vector v) {
		return (dx*v.dy - dy*v.dx);
	}
	
	public Position addTo(Position p) {
		return new Position(p.x() + (int)dx, p.y() + (int)dy);
	}

	public Vector normalize() {
		return normalize(1.0);
	}
	
	public Vector normalize(double alpha) {
		double sum = dx * dx + dy * dy;
		double scale = sum <=0 ? 0 : alpha / Math.sqrt(sum);
		return scalePositive(scale);
	}
	
	/*
	public void draw(Position source, Color color) {
		Position dest = addTo(source);
		DrawUtils.drawArrow(source, dest, color);
	}
	TODO: probably want this
	*/
	
	public static Vector diff(Position source, Position target) {
		return new Vector(target.x() - source.x(), target.y() - source.y());
	}

	public Vector scalePositive(double scale) {
		if (scale > 0) {
			return new Vector(dx * scale, dy * scale);
		} else {
			return ZERO;
		}
	}
	
	public Vector scale(double scale) {
		return new Vector(dx*scale, dy*scale);
	}
	
	public Vector scaleInPlace(double scale) {
		dx *= scale;
		dy *= scale;
		return this;
	}
	
	public double getAngle() {
		return Math.atan2(dy,dx);
	}
	
	@Override
	public String toString() {
		return "x: " + dx + " y: " + dy;
	}	

	public static Vector ClosestPointOnLineSegment(Vector a, Vector b, Vector test) {
		Vector c = test.sub(a);
		Vector v = b.sub(a).normalize();
		double d = b.sub(a).length();
		double t = v.dot(c);
		
		if(t < 0) {
			// a
			return a;
		}
		else if (t > d) {
			// b
			return b;
		}
		else {
			v = v.scale(t);
			v = a.add(v);
			return v;
		}
	}
	public static double PointToLineSegmentDistance(Vector a, Vector b, Vector test) {
		return ClosestPointOnLineSegment(a, b, test).distanceTo(test);
	}

	public double norm() {
		return Math.sqrt(dx*dx+dy*dy);
	}

	public static double closestDistance(Vector vector,
			Set<Vector> potentialTargets) {
		double ret = Double.MAX_VALUE;
		for(Vector v : potentialTargets) {
			ret = Math.min(ret, v.distanceTo(vector));
		}
		return ret;
	}
	
}
