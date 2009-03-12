package Jarek_zerg;

import java.util.*;

import Jarek_zerg.Comms;
import Jarek_zerg.Comms.CompoundMessage;
import Jarek_zerg.Navigation.NaviResult;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.Robot;
import battlecode.common.RobotController;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;

/**
 * 
 * @author Mimuw_ZERG
 *
 */

public class Cannon extends GeneralRobot {
	/** If true then print debug strings */
	private final boolean DEBUG = false;

	/**
	 * Actual task of Cannon. Can be viewed as state of robot.
	 */
	public enum Mission {
		AUTO, MANUAL
	}

	private Mission mission;
	
	private MapLocation nextTarget = null;
	private boolean targetInAir = false;

	public Cannon(RobotController rc) {
		super(rc);
		mission = Mission.AUTO;
	}

	@Override
	public void processMessage(List<Comms.CompoundMessage> cmsgs){
		for (CompoundMessage cmsg : cmsgs) {
			switch(cmsg.type){
			case ARTY:
				mission = Mission.MANUAL;
				nextTarget = cmsg.loc;
				targetInAir = (cmsg.param > 0); 
				String s;
				if (targetInAir)
					s = " in AIR";
				else
					s = " on GROUND";
				myRC.setIndicatorString(1, "aimed at " + cmsg.loc + s);
				break;
			case GOTO:
				if (DEBUG) System.out.println("GOTO: Destination: "+myRC.getLocation().directionTo(cmsg.loc));
				// Go to given location 
				if (myRC.getRoundsUntilMovementIdle() != 0){
					if (DEBUG) System.out.println("GOTO: We can't move now");
					break; // Unfortunately we can't move now. Ignore message 
				}
				List<MapLocation> tempPath = Collections.singletonList(cmsg.loc);
				while (true){
					NaviResult naviResult = navigation.goUsingPath(tempPath);
					if (naviResult == NaviResult.MOVED){
						if (DEBUG) System.out.println("GOTO: Moved");
						myRC.yield();
						break; // Moved. Back to other tasks.
					}else if (naviResult == NaviResult.CHANGED_DIRECTION){
						if (DEBUG) System.out.println("GOTO: Changed dir");
						myRC.yield();
					}else{
						if (DEBUG) System.out.println("GOTO: Error");
						break; // Some error occured. Move not possible. Ignore command.
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
		feedBrethren();
	}
	
	@Override
	public void action() throws GameActionException {
		myRC.setIndicatorString(2, mission.toString());
		switch (mission) {
		case MANUAL:
			// facing proper?
			if (nextTarget != null) {
				Direction dir = myRC.getLocation().directionTo(nextTarget);
				if (myRC.getDirection() != dir) {
					myRC.setDirection(dir);
					break;
				}
			}
			if ((nextTarget==null) && (myRC.getRoundsUntilAttackIdle() <= 1)){
				Comms.CompoundMessage cmsg = comms.new CompoundMessage();
				cmsg.type = Comms.MessageType.AIMME;
				cmsg.loc = myRC.getLocation();
				comms.sendMessage(cmsg);
			}
			// can attack at all?
			if (myRC.getRoundsUntilAttackIdle() > 0) break;
			if (nextTarget == null){
				mission = Mission.AUTO;
				break;
			}
			// is in range?
			if (myRC.getLocation().distanceSquaredTo(nextTarget) <= RobotType.CANNON.attackRadiusMaxSquared()){
				try{
				if (targetInAir)
					myRC.attackAir(nextTarget);
				else
					myRC.attackGround(nextTarget);
				} catch(Exception e){
					
				}
			}
			nextTarget = null;
			break;
		case AUTO:
			if (myRC.getRoundsUntilAttackIdle() <= 1){
				Comms.CompoundMessage cmsg = comms.new CompoundMessage();
				cmsg.type = Comms.MessageType.AIMME;
				cmsg.loc = myRC.getLocation();
				comms.sendMessage(cmsg);
			}
			if (myRC.getRoundsUntilAttackIdle() > 0) break;
			String s;
			if (targetInAir)
				s = " in AIR";
			else
				s = " on GROUND";
			myRC.setIndicatorString(1, "aimed at " + nextTarget + s);
			ArrayList<Robot> robots = new ArrayList<Robot>(); 
			Collections.addAll(robots, myRC.senseNearbyGroundRobots());
			Collections.addAll(robots, myRC.senseNearbyAirRobots());

			for (Robot robot : robots) {
				RobotInfo ri = myRC.senseRobotInfo(robot);
				if (ri.team != myRC.getTeam()) {
					nextTarget = myRC.senseRobotInfo(robot).location;
					try {
						if (robot.getRobotLevel() == RobotLevel.IN_AIR)
							myRC.attackAir(nextTarget);
						else
							myRC.attackGround(nextTarget);
					} catch (Exception e) {

					}
				}
			}
			nextTarget = null;
			break;
		default:
		}
		tryToGetNearEnemy();
	}

	/**
	 * Moves robot closer to enemy, but not further than Archon range
	 * @throws GameActionException
	 */
	private void tryToGetNearEnemy() throws GameActionException {
		if (nextTarget == null)
			return; /* no target locked */
		if (myRC.hasActionSet())
			return; /* we are doing something during this round */
		if (myRC.getRoundsUntilMovementIdle() > 0)
			return; /* we cannot move */
		
		Direction dir = myRC.getLocation().directionTo(nextTarget);
		if (myRC.getDirection() != dir) {
			myRC.setDirection(dir);
			return; /* rotated */
		}
		
		if (!myRC.canMove(dir))
			return; /* cannot move there */

		MapLocation destination = myRC.getLocation().add(dir);
		for (MapLocation archonLoc : myRC.senseAlliedArchons()) {
			if (destination.equals(archonLoc) || destination.isAdjacentTo(archonLoc)){
				/* great. we'll be in archon's range - move now */
				myRC.moveForward();
				return;
			}
		}
	}
}
