package Jarek_zerg;

import java.util.ArrayList;
import java.util.Collections;

import Jarek_zerg.Comms;
import Jarek_zerg.map.GameMap;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.Robot;

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

	public GeneralRobot(RobotController rc) {
		myRC = rc;
		navigation = new Navigation(myRC);
		comms = new Comms(myRC);
		gameMap = new GameMap(myRC);
	}

	public abstract void idling() throws GameActionException;
	public abstract void action() throws GameActionException;
	public void processMessage(ArrayList<Comms.CompoundMessage> cmsgs){
		myRC.setIndicatorString(1, "Got (" + cmsgs.size() + ") message(s)");
	}
	
	public void checkChannelers() throws GameActionException{
		ArrayList<Robot> robots = new ArrayList<Robot>(); 
		Collections.addAll(robots, myRC.senseNearbyGroundRobots());
		ArrayList<RobotInfo> channelers = new ArrayList<RobotInfo>();
		RobotInfo ri;
		for (Robot r : robots) {
			ri = myRC.senseRobotInfo(r);
			// is it one of our channelers?
			if ((ri.type == RobotType.CHANNELER) && (ri.team == myRC.getTeam())){
				// can I see enough to notify it?
				if (ri.location.distanceSquaredTo(myRC.getLocation()) <=  
					myRC.getRobotType().sensorRadius()*myRC.getRobotType().sensorRadius() 
					+ RobotType.CHANNELER.attackRadiusMaxSquared()){
						// remember it
						channelers.add(ri);
				}
			}
		}
		if (channelers.size()==0) return;
		Collections.addAll(robots, myRC.senseNearbyAirRobots());
		ArrayList<RobotInfo> channelersToRemove = new ArrayList<RobotInfo>();
		for (Robot r : robots) {
			ri = myRC.senseRobotInfo(r);
			// is it a valid target?
			if (ri.team != myRC.getTeam()){
				// check every remembered channeler 
				for (RobotInfo chan : channelers) {
					// is the enemy in range?
					if (ri.location.distanceSquaredTo(chan.location) <= 
						RobotType.CHANNELER.attackRadiusMaxSquared()){
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
		for (RobotInfo chan : channelers){
			Comms.CompoundMessage cmsg  = comms.new CompoundMessage();
			cmsg.type = Comms.MessageType.YOU_STOP_DRAIN;
			cmsg.address = chan.location;
			comms.sendMessage(cmsg);				
		}
	}

	public void checkCannons() throws GameActionException{
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
		if (cannons.size()==0) return;
		Collections.addAll(robots, myRC.senseNearbyAirRobots());
		ArrayList<RobotInfo> cannonsToRemove = new ArrayList<RobotInfo>();
		for (Robot r : robots) {
			ri = myRC.senseRobotInfo(r);
			// is it a valid target?
			if (ri.team != myRC.getTeam()){
				// check every remembered channeler 
				for (RobotInfo cann : cannons) {
					// is the enemy in range?
					if (ri.location.distanceSquaredTo(cann.location) <= 
						RobotType.CANNON.attackRadiusMaxSquared()){
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
}
