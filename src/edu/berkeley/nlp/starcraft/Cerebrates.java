package edu.berkeley.nlp.starcraft;

public class Cerebrates {
	private Cerebrates(){}
	
	public static Cerebrate promoteUnderling(final Underling u) {
		return new AbstractCerebrate() {
			@Override 
			public void onFrame() {
				u.onFrame();
			}
			
			@Override
			public String toString() {
				return "PromotedCerebrate[" + u +"]";
			}
		};
		
	}
}
