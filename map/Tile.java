package Jarek_zerg.map;

import Jarek_zerg.astar.Path.Step;
import battlecode.common.*;
import battlecode.common.TerrainTile.TerrainType;

/**
 * Represents single tile at map.
 * @author Jarek
 */
public class Tile {
	private final RobotController myRC;
	
	public MapLocation loc;
	public Integer totalHeight;
	public Integer blockHeight;
	public Robot robotAtLocation;
	public TerrainTile terrainTile;
	
	public Tile(RobotController rc, MapLocation loc) {
		this.myRC = rc;
		this.loc = loc;
	}
	
	public boolean canScan(){
		return myRC.canSenseSquare(loc);
	}
	
	public void rescan() throws GameActionException{
		terrainTile = myRC.senseTerrainTile(loc);
		if (terrainTile.getType() != TerrainType.LAND)
			return;
		blockHeight = myRC.senseNumBlocksAtLocation(loc);
		totalHeight = myRC.senseHeightOfLocation(loc);
		robotAtLocation = myRC.senseGroundRobotAtLocation(loc);
	}
	
	public boolean equals(Object other){
		if (other instanceof Step) {
			Tile o = (Tile) other;

			if (o == null) return false;
			if (!this.terrainTile.equals(o.terrainTile)) return false;
			if (this.terrainTile.getType() != TerrainType.LAND) return true;
			if (!this.blockHeight.equals(o.blockHeight)) return false;
			
			if (robotAtLocation == null){
				if (o.robotAtLocation != null) return false;
			}else{
				if (!this.robotAtLocation.equals(o.robotAtLocation)) return false;
			}
			return true;
		}
		return false;
	}

	public String toString() {
		return String.format("(%s, %s, %s)", totalHeight, robotAtLocation, terrainTile);
	}

}
