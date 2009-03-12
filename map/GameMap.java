package Jarek_zerg.map;

import java.util.*;
import java.util.Map.Entry;

import Jarek_zerg.astar.*;

import battlecode.common.*;
import battlecode.common.TerrainTile.TerrainType;

/**
 * Cached representation of a game map.
 * @author Jarek
 */
public class GameMap implements TileBasedMap {
	private final RobotController myRC;

	private Map<MapLocation, Tile> tiles;
	
	private int ortMoveCost;
	private int diagMoveCost;

	public GameMap(RobotController rc) {
		myRC = rc;
		tiles = new HashMap<MapLocation, Tile>();
		ortMoveCost = myRC.getRobotType().moveDelayOrthogonal();
		diagMoveCost = myRC.getRobotType().moveDelayDiagonal();
	}

	public List<MapLocation> scanNeighbourhood() throws GameActionException {
		int radius = myRC.getRobotType().sensorRadius();
		MapLocation start = new MapLocation(myRC.getLocation().getX() - radius,
				myRC.getLocation().getY() - radius);
		List<MapLocation> updatedLocations = new ArrayList<MapLocation>();
		for (MapLocation locLeft = start; locLeft.getY() - start.getY() <= radius * 2; locLeft = locLeft
				.add(Direction.SOUTH))
			for (MapLocation loc = locLeft; loc.getX() - start.getX() <= radius * 2; loc = loc
					.add(Direction.EAST)) {
				Tile newTile = new Tile(myRC, loc);
				if (!newTile.canScan()) continue;

				newTile.rescan();
				if (newTile.terrainTile.getType() != TerrainType.LAND)
					continue;
				Tile oldTile = tiles.put(loc, newTile);
				if (!newTile.equals(oldTile))
					updatedLocations.add(loc);
			}
		return updatedLocations;
	}

	public Map<MapLocation, Tile> getTiles() {
		return tiles;
	}
	
	public Tile get(MapLocation loc){
		return tiles.get(loc);
	}
	
	public float getHeuristicCost(Tile fromTile, Tile toTile) {		
		float dx = toTile.loc.getX() - fromTile.loc.getX();
		float dy = toTile.loc.getY() - fromTile.loc.getY();
		
		float result = (float) (Math.sqrt((dx*dx)+(dy*dy)));
		
		result = result * (float) ortMoveCost;
		
		int delta = toTile.totalHeight - fromTile.totalHeight;
		if (delta >= 2){
			result += GameConstants.CLIMBING_PENALTY_RATE * (delta * delta);
		}
		if (delta <= -2){
			result += GameConstants.FALLING_PENALTY_RATE * Math.abs(delta);
		}
		
		return result;
	}

	public boolean blocked(Tile tile) {
		/* if not land then it is blocked */
		if (tile.terrainTile.getType() != TerrainType.LAND)
			return true;
		///* if robot at location then blocked */
		//return (tile.robotAtLocation != null);
		return false;
	}

	public float getCost(Tile fromTile, Tile toTile, Direction dir, boolean includeTileBlockedPenalty) {
		int cost;
		if (dir.isDiagonal())
			cost = diagMoveCost;
		else
			cost = ortMoveCost;

		int delta = toTile.totalHeight - fromTile.totalHeight;
		if (delta >= 2){
			cost += GameConstants.CLIMBING_PENALTY_RATE * (delta * delta);
		}
		if (delta <= -2){
			cost += GameConstants.FALLING_PENALTY_RATE * Math.abs(delta);
		}
		if (myRC.getRobotType() == RobotType.WORKER)
			if ((delta > GameConstants.WORKER_MAX_HEIGHT_DELTA) && (myRC.getNumBlocks() > 0))
				cost += BIG_FLOAT; //Workers should not climb so high

		/* HEUR: if robot at location then add waiting cost for it to move */
		if (includeTileBlockedPenalty)
			if (toTile.robotAtLocation != null)
				cost += 10;
		return cost;
	}

	/**
	 * Returns list of all known block locations.
	 */
	public List<MapLocation> senseNearbyBlocks() {
		List<MapLocation> result = new ArrayList<MapLocation>();
		for (Entry<MapLocation, Tile> entry : tiles.entrySet()) {
			if (entry.getValue().blockHeight > 0)
				result.add(entry.getKey());
		}
		return result;
	}

}
