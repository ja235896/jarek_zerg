package Jarek_zerg;

import battlecode.common.*;

public class Scout extends GeneralRobot {

	/**
	 * Actual task of Scout. Can be viewed as state of robot.
	 */
	public enum Mission {
		SEARCH, REPORT
	}

	private Mission mission;
	
	//private Dictionary<MapLocation,int> path;
	
	private Direction chosenDir = Direction.NONE;

	public Scout(RobotController rc) {
		super(rc);
		mission = Mission.SEARCH;
	}

	@Override
	public void idling() throws GameActionException {
		myRC.setIndicatorString(2, mission.toString());
		switch (mission) {

		case SEARCH:
			break;
			
		case REPORT:
			break;

		default:
		}
	}

	@Override
	public void action() throws GameActionException {
		myRC.setIndicatorString(2, mission.toString());
		switch (mission) {

		case SEARCH:
			if (chosenDir == Direction.NONE)
				chosenDir = navigation.getRandomDirection();
			if (myRC.canMove(chosenDir)){
				try{
				if (chosenDir != myRC.getDirection())
					myRC.setDirection(chosenDir);
				else
					myRC.moveForward();
				} catch (GameActionException e){
					chosenDir = navigation.getRandomDirection();					
				}
			} else {
				MapLocation loc = myRC.getLocation();
				loc = loc.add(chosenDir);
				if (myRC.senseAirRobotAtLocation(loc) == null)
					chosenDir = navigation.getRandomDirection();
			}
			break;
			
		case REPORT:
			break;

		default:
		}
	}
	
}
