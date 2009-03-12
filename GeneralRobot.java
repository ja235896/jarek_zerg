package Jarek_zerg;

import static battlecode.common.GameConstants.ENERGON_RESERVE_SIZE;

import java.util.*;

import Jarek_zerg.Comms;
import Jarek_zerg.map.GameMap;

import battlecode.common.*;

/**
 * 
 * @author Mimuw_ZERG
 * 
 */

public abstract class GeneralRobot {
	protected final RobotController myRC;
	protected Navigation navigation;
	protected Comms comms;
	protected int spawnTime;
	protected GameMap gameMap;
	protected Spam spam;

	public GeneralRobot(RobotController rc) {
		myRC = rc;
		navigation = new Navigation(myRC);
		comms = new Comms(myRC);
		gameMap = new GameMap(myRC);
		spam = new Spam(myRC);
	}

	public abstract void idling() throws GameActionException;

	public abstract void action() throws GameActionException;

	public void processMessage(List<Comms.CompoundMessage> cmsgs) {
		myRC.setIndicatorString(1, "Got (" + cmsgs.size() + ") message(s)");
	}

	public void checkChannelers() throws GameActionException {
		ArrayList<Robot> robots = new ArrayList<Robot>();
		Collections.addAll(robots, myRC.senseNearbyGroundRobots());
		ArrayList<RobotInfo> channelers = new ArrayList<RobotInfo>();
		RobotInfo ri;
		for (Robot r : robots) {
			ri = myRC.senseRobotInfo(r);
			// is it one of our channelers?
			if ((ri.type == RobotType.CHANNELER) && (ri.team == myRC.getTeam())) {
				// can I see enough to notify it?
				if (ri.location.distanceSquaredTo(myRC.getLocation()) <= myRC
						.getRobotType().sensorRadius()
						* myRC.getRobotType().sensorRadius()
						+ RobotType.CHANNELER.attackRadiusMaxSquared()) {
					// remember it
					channelers.add(ri);
				}
			}
		}
		if (channelers.size() == 0)
			return;
		Collections.addAll(robots, myRC.senseNearbyAirRobots());
		ArrayList<RobotInfo> channelersToRemove = new ArrayList<RobotInfo>();
		for (Robot r : robots) {
			ri = myRC.senseRobotInfo(r);
			// is it a valid target?
			if (ri.team != myRC.getTeam()) {
				// check every remembered channeler
				for (RobotInfo chan : channelers) {
					// is the enemy in range?
					if (ri.location.distanceSquaredTo(chan.location) <= RobotType.CHANNELER
							.attackRadiusMaxSquared()) {
						Comms.CompoundMessage cmsg = comms.new CompoundMessage();
						cmsg.type = Comms.MessageType.YOU_DRAIN;
						cmsg.address = chan.location;
						comms.sendMessage(cmsg);
						channelersToRemove.add(chan);
					}
				}
				for (RobotInfo chan : channelersToRemove)
					channelers.remove(chan);
				channelersToRemove.clear();
			}
		}
		for (RobotInfo chan : channelers) {
			Comms.CompoundMessage cmsg = comms.new CompoundMessage();
			cmsg.type = Comms.MessageType.YOU_STOP_DRAIN;
			cmsg.address = chan.location;
			comms.sendMessage(cmsg);
		}
	}

	public void checkCannons() throws GameActionException {
		ArrayList<Robot> robots = new ArrayList<Robot>();
		Collections.addAll(robots, myRC.senseNearbyGroundRobots());
		ArrayList<RobotInfo> cannons = new ArrayList<RobotInfo>();
		RobotInfo ri;
		for (Robot r : robots) {
			ri = myRC.senseRobotInfo(r);
			// is it one of our cannons?
			if ((ri.type == RobotType.CANNON) && (ri.team == myRC.getTeam())) {
				if (ri.roundsUntilAttackIdle <= 3)
					// remember it
					cannons.add(ri);
			}
		}
		if (cannons.size() == 0)
			return;
		Collections.addAll(robots, myRC.senseNearbyAirRobots());
		ArrayList<RobotInfo> cannonsToRemove = new ArrayList<RobotInfo>();
		for (Robot r : robots) {
			ri = myRC.senseRobotInfo(r);
			// is it a valid target?
			if (ri.team != myRC.getTeam()) {
				// check every remembered channeler
				for (RobotInfo cann : cannons) {
					// is the enemy in range?
					if (ri.location.distanceSquaredTo(cann.location) <= RobotType.CANNON
							.attackRadiusMaxSquared()) {
						Comms.CompoundMessage cmsg = comms.new CompoundMessage();
						cmsg.type = Comms.MessageType.YOU_ARTY;
						cmsg.address = cann.location;
						cmsg.loc = ri.location;
						if (r.getRobotLevel() == RobotLevel.IN_AIR)
							cmsg.param = 1;
						else
							cmsg.param = 0;
						comms.sendMessage(cmsg);
						cannonsToRemove.add(cann);
					}
				}
				for (RobotInfo cann : cannonsToRemove)
					cannons.remove(cann);
				cannonsToRemove.clear();
			}
		}
	}

	/**
	 * Try to equalise energon level.
	 * 
	 * @throws GameActionException
	 */
	public void feedBrethren() throws GameActionException{
		List<Robot> nearbyRobots = navigation.senseNearbyRobots(true, true);
		List<RobotInfo> robotsInfo = navigation.robotsToRobotsInfo(nearbyRobots);
		MapLocation myLoc = myRC.getLocation();
		Team myTeam = myRC.getTeam();
		double myEnergon = myRC.getEnergonLevel();
		if (myEnergon * 2.5 < myRC.getMaxEnergonLevel())
			return; /* we are hungry too */

		for (RobotInfo info : robotsInfo) {
			if (myTeam.equals(info.team))
				if (myLoc.equals(info.location) || (myLoc.isAdjacentTo(info.location)))
					/* potentially good robot */
					if (info.energonLevel < myEnergon) {
						double transferAmount = Math.min(ENERGON_RESERVE_SIZE
								- info.energonReserve, (myEnergon - info.energonLevel)/2);
						RobotLevel level = RobotLevel.ON_GROUND;
						if ((info.type == RobotType.ARCHON)||(info.type == RobotType.SCOUT))
							level = RobotLevel.IN_AIR;
						myRC.transferEnergon(transferAmount, info.location, level);
						return;
					}
		}
	}
}
