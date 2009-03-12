package Jarek_zerg;

import java.util.*;

import Jarek_zerg.Comms.CompoundMessage;
import Jarek_zerg.Navigation.NaviResult;
import battlecode.common.*;

/**
 * 
 * @author 
 *
 */

public class Soldier extends GeneralRobot {
	/** If true then print debug strings */
	private final boolean DEBUG = false;

	/**
	 * Actual task of Soldier. Can be viewed as state of robot.
	 */
	public enum Mission {

		NONE, SENS_SURROUNDINGS, PATROL, GOTO_ARCHON, GOTO_ENEMY, ATTACK, GOTO_LOCATION
	}
	private Mission mission;
	private Direction nextDirection;
	private Robot enemy;
	private MapLocation archonLocation;
	private int archonDistance;
	private MapLocation loc;

	public Soldier(RobotController rc) {
		super(rc);
		enemy = null;

		archonLocation = null;
		archonDistance = Integer.MAX_VALUE;
		nextDirection = Direction.NORTH;

		mission = Mission.SENS_SURROUNDINGS;
		
		senseAlliedArchons();
	}
	
	@Override
	public void processMessage(List<Comms.CompoundMessage> cmsgs){
		for (CompoundMessage cmsg : cmsgs) {
			switch(cmsg.type){
			case ATTACK:
				mission = Mission.GOTO_LOCATION;
				loc = cmsg.loc;
				break;
			case GOTO:
				if (DEBUG) System.out.println("GOTO: Destination: "+myRC.getLocation().directionTo(cmsg.loc));
				/* Go to given location */
				if (myRC.getRoundsUntilMovementIdle() != 0){
					if (DEBUG) System.out.println("GOTO: We can't move now");
					break; /* Unfortunately we can't move now. Ignore message */
				}
				List<MapLocation> tempPath = Collections.singletonList(cmsg.loc);
				while (true){
					NaviResult naviResult = navigation.goUsingPath(tempPath);
					if (naviResult == NaviResult.MOVED){
						if (DEBUG) System.out.println("GOTO: Moved");
						myRC.yield();
						break; /* Moved. Back to other tasks. */
					}else if (naviResult == NaviResult.CHANGED_DIRECTION){
						if (DEBUG) System.out.println("GOTO: Changed dir");
						myRC.yield();
					}else{
						if (DEBUG) System.out.println("GOTO: Error");
						break; /* Some error occured. Move not possible. Ignore command. */
					}
				}
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void idling() throws GameActionException {
		myRC.setIndicatorString(2, mission.toString());
		attack();
	}

	@Override
	public void action() throws GameActionException {
		myRC.setIndicatorString(2, mission.toString());

		switch (mission) {
			case SENS_SURROUNDINGS:
				sensSurroundings();
				if (enemy == null) {
					mission = Mission.PATROL;
				} else {
					mission = Mission.GOTO_ENEMY;
				}
				break;

			case PATROL:
				if (myRC.getEnergonLevel() * 2.0 < myRC.getMaxEnergonLevel()) {
					senseAlliedArchons();
					mission = Mission.GOTO_ARCHON;
					break;
				}
				sensSurroundings();
				if (enemy == null && myRC.getRoundsUntilMovementIdle() == 0) {
					nextDirection = nextDirection.rotateRight();
					if (nextDirection == Direction.OMNI) 
						nextDirection = Direction.NORTH;
					myRC.setDirection(nextDirection);
					myRC.yield();
					/* try not to get directly under Archon. It's high ground and workers like that spot */
					if (!myRC.getLocation().add(nextDirection).equals(archonLocation))
						navigation.goInDirection(nextDirection);
				} else {
					mission = Mission.GOTO_ENEMY;
					goToRobot(enemy);
				}
				break;

			case GOTO_ARCHON:
				if (archonLocation == null) {
					senseAlliedArchons();
					break;
				}
				if (myRC.getEnergonLevel() * 3.0 > myRC.getMaxEnergonLevel()) {
					sensSurroundings();
					break;
				}
				/* try not to get directly under Archon. It's high ground and workers like that spot */
				if (!myRC.getLocation().isAdjacentTo(archonLocation))
					goToLocation(archonLocation);
				break;

			case GOTO_ENEMY:
				if (enemy == null) {
					mission = Mission.SENS_SURROUNDINGS;
					sensSurroundings();
					break;
				}
				if (goToRobot(enemy) == true) {
					mission = Mission.ATTACK;
					attack();
				} else {
					if (enemy == null) {
						mission = Mission.SENS_SURROUNDINGS;
						sensSurroundings();
						break;
					}
				}
				break;

			case ATTACK:
				if (enemy == null) {
					mission = Mission.SENS_SURROUNDINGS;
					sensSurroundings();
					break;
				}
				attack();
				if (enemy == null) {
					mission = Mission.SENS_SURROUNDINGS;
					sensSurroundings();
				}
				break;
			
			case GOTO_LOCATION:
				sensSurroundings();
				if (enemy != null) {
					mission = Mission.ATTACK;
					attack();
				} else {
					if (loc.isAdjacentTo(myRC.getLocation())) {
						mission = Mission.PATROL;
						break;
					} else {
						goToLocation(loc);
					}
				}
				break;

			default:
		}
	}

	private boolean goToLocation(MapLocation location) throws GameActionException {

		if (location == null) {
			return false;
		} else {
			if (location == myRC.getLocation()) {
				return true;
			}
		}

		while (myRC.getRoundsUntilMovementIdle() > 0) myRC.yield();

		nextDirection = myRC.getLocation().directionTo(location);

		return navigation.goInDirection(nextDirection);
	}

	private boolean goToRobot(Robot robot) throws GameActionException {

		if (robot == null) {
			return false; 
		}

		while (myRC.getRoundsUntilMovementIdle() > 0) {
			return false;
		}

		RobotInfo info = null;
		try {
			info = myRC.senseRobotInfo(robot);
		} catch (GameActionException e) {
			robot = null;
			return false;
		}

		if (info.location.isAdjacentTo(myRC.getLocation()))
			return true;

		if (goToLocation(info.location) == false) {
			if (DEBUG) System.out.println("can't move in direction " + nextDirection);
			robot = null;
			return false;
		}

		return true;
	}

	private void attack() throws GameActionException {

		while (myRC.getRoundsUntilAttackIdle() > 0) {
			return;
		}

		if (enemy == null) {
			return;
		}

		if (myRC.getRoundsUntilAttackIdle() > 0) {
			return;
		}

		RobotInfo enemyInfo = null;
		try {
			enemyInfo = myRC.senseRobotInfo(enemy);
		} catch (GameActionException e) {
			if (e.getType() == GameActionExceptionType.CANT_SENSE_THAT) {
				if (DEBUG) System.out.println("enemy killed(?)!!!!!!!");
			}
			enemy = null;
			return;
		}

		if (enemyInfo.location.isAdjacentTo(myRC.getLocation())) {
			if (myRC.getEnergonLevel() > myRC.getRobotType().attackPower()) {
				if (enemyInfo.type.isAirborne()) {
					myRC.attackAir(enemyInfo.location);
				} else {
					myRC.attackGround(enemyInfo.location);
				}
			}
		} else {
			enemy = null;
		}
	}

	private void senseAlliedArchons() {
		MapLocation[] archonLocations = myRC.senseAlliedArchons();

		MapLocationComparator mapLocationComparator = new MapLocationComparator(myRC.getLocation());
		archonLocation = Collections.min(Arrays.asList(archonLocations), mapLocationComparator);
	}

	private void sensSurroundings() throws GameActionException {
		List<Robot> robots = new ArrayList<Robot>();
		Collections.addAll(robots, myRC.senseNearbyAirRobots());
		Collections.addAll(robots, myRC.senseNearbyGroundRobots());

		int minDistance = Integer.MAX_VALUE;
		MapLocation myLocation = myRC.getLocation();
		for (Robot robot : robots) {
			RobotInfo info = myRC.senseRobotInfo(robot);
			if (info.team != myRC.getTeam()) {
				int distance = myLocation.distanceSquaredTo(info.location);
				if (distance < minDistance) {
					nextDirection = myLocation.directionTo(info.location);
					enemy = robot;
					minDistance = distance;
				}
			// System.out.format("Found enemy %1$s. Next direction is %2$s",
			// fluxInfo, nextDirection);
			} else {
				if (info.type == RobotType.ARCHON) {
					int distance = myLocation.distanceSquaredTo(info.location);
					if (archonLocation == null || archonDistance > distance) {
						archonLocation = info.location;
						archonDistance = distance;
					}
				}
			}
		}

		//System.out.println("koniec szukania");

		if (enemy != null) {
			mission = Mission.GOTO_ENEMY;
		} else {
			mission = Mission.PATROL;
		}
	}
}
