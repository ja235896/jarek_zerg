package Jarek_zerg.astar;

import java.util.*;

import battlecode.common.MapLocation;

/**
 * A path determined by some path finding algorithm. A series of steps from
 * the starting location to the target location. This includes a step for the
 * initial location.
 * 
 * @author Kevin Glass
 */
public class Path {
	/** The list of steps building up this path */
	private List<Step> steps = new ArrayList<Step>();
	
	/**
	 * Create an empty path
	 */
	public Path() {
		
	}

	/**
	 * Get the length of the path, i.e. the number of steps
	 * 
	 * @return The number of steps in this path
	 */
	public int getLength() {
		return steps.size();
	}
	
	/**
	 * Get the step at a given index in the path
	 * 
	 * @param index The index of the step to retrieve. Note this should
	 * be >= 0 and < getLength();
	 * @return The step information, the position on the map.
	 */
	public Step getStep(int index) {
		return steps.get(index);
	}
	
	/**
	 * Get the x coordinate for the step at the given index
	 * 
	 * @param index The index of the step whose x coordinate should be retrieved
	 * @return The x coordinate at the step
	 */
	public MapLocation getLoc(int index) {
		return getStep(index).loc;
	}

	/**
	 * Append a step to the path.  
	 * 
	 * @param x The x coordinate of the new step
	 * @param y The y coordinate of the new step
	 */
	public void appendStep(MapLocation loc) {
		steps.add(new Step(loc));
	}

	/**
	 * Prepend a step to the path.  
	 * 
	 * @param x The x coordinate of the new step
	 * @param y The y coordinate of the new step
	 */
	public void prependStep(MapLocation loc) {
		steps.add(0, new Step(loc));
	}
	
	/**
	 * Check if this path contains the given step
	 * 
	 * @param x The x coordinate of the step to check for
	 * @param y The y coordinate of the step to check for
	 * @return True if the path contains the given step
	 */
	public boolean contains(MapLocation loc) {
		return steps.contains(new Step(loc));
	}
	
	public List<MapLocation> toMapLocationList() {
		List<MapLocation> result = new ArrayList<MapLocation>();
		for (Step step : this.steps) {
			result.add(step.loc);
		}
		return result;
	}
	
	/**
	 * A single step within the path
	 * 
	 * @author Kevin Glass
	 */
	public class Step {
		/** The x coordinate at the given step */
		private MapLocation loc;
		
		/**
		 * Create a new step
		 * 
		 * @param x The x coordinate of the new step
		 * @param y The y coordinate of the new step
		 */
		public Step(MapLocation loc) {
			this.loc = loc;
		}
		
		/**
		 * Get the x coordinate of the new step
		 * 
		 * @return The x coodindate of the new step
		 */
		public MapLocation getLoc() {
			return loc;
		}

		/**
		 * @see Object#hashCode()
		 */
		public int hashCode() {
			return loc.getX()*loc.getY();
		}

		/**
		 * @see Object#equals(Object)
		 */
		public boolean equals(Object other) {
			if (other instanceof Step) {
				Step o = (Step) other;
				
				return this.loc.equals(o.loc);
			}
			
			return false;
		}
	}
}
