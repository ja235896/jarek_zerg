package Jarek_zerg;

import java.util.*;

import Jarek_zerg.astar.*;
import Jarek_zerg.exceptions.HungryException;
import Jarek_zerg.map.*;
import battlecode.common.*;
import battlecode.common.TerrainTile.TerrainType;

/**
 * 
 * @author Mimuw_ZERG
 *
 */

public class Navigation {
	/** If true then print debug strings */
	private final boolean DEBUG = false;

	/**
	 * Status of navigation.
	 */
	public enum NaviResult {
		CHANGED_DIRECTION, MOVED, UNKNOWN_ERROR, CANNOT_MOVE, CHANGE_PATH
	}

	private final RobotController myRC;
	
	public final Direction[] allDirections;	
	public final Random r = new Random();

	public Navigation(RobotController rc) {
		myRC = rc;
		allDirections = getAllDirections(Direction.NORTH);
		r.setSeed(myRC.hashCode());
	}
	
	public Direction[] getAllDirections(Direction start){
		Direction[] result = new Direction[8];
		result[0] = start;
		Direction currDir = start;
		for (int i = 1; i < 8; i+=2) {
			currDir = currDir.rotateLeft();
			result[i] = currDir;
		}
		currDir = start;
		for (int i = 2; i < 8; i+=2) {
			currDir = currDir.rotateRight();
			result[i] = currDir;
		}
		return result;
	}
	
	public Direction findNearestFreeDirection(Direction blockedDir){
		Direction[] allOtherDirections = getAllDirections(blockedDir);
		for (Direction currDir : allOtherDirections) {
			if (myRC.canMove(currDir))
				return currDir;
		}
		return Direction.NONE;
	}

	public MapLocation findNearestFreeLocationUsingGameMap(GameMap map, MapLocation loc, Direction blockedDir){
		Direction[] allOtherDirections = getAllDirections(blockedDir);
		for (Direction currDir : allOtherDirections) {
			Tile tile = map.get(loc.add(currDir));
			if ((tile!=null) && (tile.robotAtLocation == null) && (tile.terrainTile.getType() == TerrainType.LAND))
				return loc.add(currDir);
		}
		return loc;
	}
	
	public MapLocation findNearestLocationFromSet(Set<MapLocation> locations){
		MapLocationComparator mapLocationComparator = new MapLocationComparator(myRC.getLocation());
		MapLocation best = Collections.min(locations, mapLocationComparator);
		return best;
	}

	public MapLocation findFurthestLocationFromSet(Set<MapLocation> locations){
		MapLocationComparator mapLocationComparator = new MapLocationComparator(myRC.getLocation());
		MapLocation best = Collections.max(locations, mapLocationComparator);
		return best;
	}

	public List<MapLocation> sortLocationsByDistance(List<MapLocation> locations){
		MapLocationComparator mapLocationComparator = new MapLocationComparator(myRC.getLocation());
		Collections.sort(locations, mapLocationComparator);
		return locations;
	}

	/**
	 * Tries to move robot in specified direction (or close to it).
	 * Returns true if robot actually moved (it could just turn).
	 */
	public boolean goInDirection(Direction dir) throws GameActionException{
		if (!myRC.canMove(dir))
			dir = findNearestFreeDirection(dir);
		if ((dir == Direction.NONE) || (dir == Direction.OMNI))
			return false;

		if (myRC.getDirection() != dir) {
			myRC.setDirection(dir);
			return false;
		} else {
			myRC.moveForward();
			return true;
		}
	}
	
	public NaviResult goUsingPath(List<MapLocation> path) {
		//System.out.println(path);
		if (path.size() == 0){
			return NaviResult.UNKNOWN_ERROR;
		}
		Direction dir = myRC.getLocation().directionTo(path.get(0));
		if (!myRC.canMove(dir)){
			return NaviResult.CANNOT_MOVE;
		}
		
		if ((dir == Direction.NONE) || (dir == Direction.OMNI))
			return NaviResult.UNKNOWN_ERROR;

		try {
			if (myRC.getDirection() != dir) {
				myRC.setDirection(dir);
				return NaviResult.CHANGED_DIRECTION;
			} else {
				myRC.moveForward();
				return NaviResult.MOVED;
			}
		}catch (GameActionException e) {
			System.out.println("caught exception:");
			e.printStackTrace();
		}
		return NaviResult.UNKNOWN_ERROR;
	}
	
	/**
	 * Go one step to specified destination. Breaks only on error or when moved.
	 * @param destination
	 */
	public void forcedGoOneStep(MapLocation destination){
		List<MapLocation> tempPath = Collections.singletonList(destination);
		
		while (true){
			NaviResult naviResult = goUsingPath(tempPath);
			if (naviResult == NaviResult.MOVED){
				myRC.yield();
				return; /* Moved. Back to other tasks. */
			}else if (naviResult == NaviResult.CHANGED_DIRECTION){
				myRC.yield();
			}else{
				return; /* Some error occured. Move not possible. Ignore command. */
			}
		}	
	}
	
	public List<MapLocation> createSimplePath(MapLocation fromLoc, MapLocation toLoc){
		MapLocation actual = fromLoc;
		List<MapLocation> result = new ArrayList<MapLocation>();
		while (!actual.equals(toLoc)){
			actual = actual.add(actual.directionTo(toLoc));
			result.add(actual);
		}
		return result;
	}
	
	public Direction getRandomDirection(){
		return allDirections[r.nextInt(8)];
	}
	
	public boolean guessFluxByTriangulation(Direction movement, Direction previousSense, Direction currentSense){
		if (previousSense == currentSense) return false; // no change
		if (movement.opposite() == previousSense) return true; // no longer behind our back
		boolean prevLeft = false; // on left or right?
		Direction work = movement;
		while (work!=movement.opposite()){
			work = work.rotateLeft();
			if (work == previousSense){
				prevLeft = true;
				break;
			}
		}			
		if (prevLeft)
			return !(previousSense.rotateLeft() == currentSense);
		else
			return !(previousSense.rotateRight() == currentSense);
	}
	
	/**
	 * Finds path to target using A-star algorithm. Should be used by ground units.
	 * It's able to avoid other robots and choose optimal path.
	 * @param map - should be an always-updated GameMap
	 * @param includeTileBlockedPenalty if true, then if path is blocked by a robot a penalty is applied
	 * @return list containing MapLocations in path 
	 * @throws HungryException 
	 */
	public List<MapLocation> findPathUsingAStar(GameMap map, MapLocation fromLoc, MapLocation toLoc, boolean includeTileBlockedPenalty) throws HungryException {
		PathFinder finder = new AStarPathFinder(map, 500, includeTileBlockedPenalty, myRC);
		if (DEBUG) System.out.println("AStar START from:"+fromLoc+" to:"+toLoc);
		Path path = finder.findPath(fromLoc, toLoc);
		if (path == null)
			return new ArrayList<MapLocation>();
		List<MapLocation> result = path.toMapLocationList();
		if (DEBUG) System.out.println(result);
		if (DEBUG) System.out.println(changePathToDirections(result));
		if (DEBUG) System.out.println("AStar STOP");
		result.remove(0);
		return result;
	}
	
	/**
	 * Returns list of Directions matching directions in given path.
	 */
	public List<Direction> changePathToDirections(List<MapLocation> path){
		List<Direction> result = new ArrayList<Direction>();
		if (path.size() < 2) return result; 
		MapLocation oldLoc = path.get(0);
		for (MapLocation loc : path.subList(1, path.size())) {
			result.add(oldLoc.directionTo(loc));
			oldLoc = loc;
		}
		return result;
	}

	public List<Robot> senseNearbyRobots(boolean ground, boolean airborne){
		List<Robot> robots = new ArrayList<Robot>();
		if (ground)
			Collections.addAll(robots, myRC.senseNearbyGroundRobots());
		if (airborne)
			Collections.addAll(robots, myRC.senseNearbyAirRobots());
		return robots;
	}

	public List<RobotInfo> robotsToRobotsInfo(List<Robot> robots) throws GameActionException{
		List<RobotInfo> result = new ArrayList<RobotInfo>();
		for (Robot robot : robots) {
			result.add(myRC.senseRobotInfo(robot));
		}
		return result;
	}
	
	public List<MapLocation> robotsInfoToLocations(List<RobotInfo> robotsInfo) throws GameActionException{
		List<MapLocation> result = new ArrayList<MapLocation>();
		for (RobotInfo robotInfo : robotsInfo) {
			result.add(robotInfo.location);
		}
		return result;
	}

}


class MapLocationComparator implements Comparator<MapLocation>{
	MapLocation source;

	public MapLocationComparator(MapLocation source) {
		this.source = source;
	}

	public int compare(MapLocation o1, MapLocation o2) {
    	return Integer.valueOf(source.distanceSquaredTo(o1)).compareTo(source.distanceSquaredTo(o2));
    }
}