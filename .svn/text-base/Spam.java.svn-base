package Jarek_zerg;

import java.util.*;

import battlecode.common.*;

/**
 * Intercept enemy messages and resend them.
 * 
 * @author Jarek
 */
public class Spam {
	protected final RobotController myRC;
	private List<Message> enemyMsgList;
	protected Navigation navigation;
	private Team myTeam;
	private Random rand;
	private Message lastMessage;

	public Spam(RobotController rc) {
		myRC = rc;
		navigation = new Navigation(myRC);
		enemyMsgList = new ArrayList<Message>();
		myTeam = myRC.getTeam();
		rand = new Random();
		lastMessage = null;
	}

	public void add(Message msg) {
		enemyMsgList.add(msg);
	}

	public void sendSpam() throws GameActionException {
		if (enemyMsgList.isEmpty()) return;
		
		Message luckyGuess;
		if (lastMessage == null)
			luckyGuess = enemyMsgList.get(rand.nextInt(enemyMsgList.size()));
		else
			luckyGuess = lastMessage;
		
		List<Robot> nearbyRobots = navigation.senseNearbyRobots(true, true);
		nearbyRobots.add(myRC.getRobot());
		List<RobotInfo> nearbyRobotsInfo = navigation.robotsToRobotsInfo(nearbyRobots);
		List<RobotInfo> alliedRobots = new ArrayList<RobotInfo>();
		List<RobotInfo> enemyRobots = new ArrayList<RobotInfo>();
		
		/* split */
		for (RobotInfo info : nearbyRobotsInfo) {
			if (info.team == myTeam)
				alliedRobots.add(info);
			else
				enemyRobots.add(info);
		}
		
		while (!enemyMsgList.isEmpty()){
			Message msg = enemyMsgList.remove(0);
			if (msg.locations == null) continue;
			for (int i = 0; i < msg.locations.length; i++) {
				for (RobotInfo allyInfo : alliedRobots) {
					if (msg.locations[i].equals(allyInfo.location)){
						/* found match. prepare and send */
						//System.out.println("Sending spam");
						//Comms comms = new Comms(myRC);
						//comms.printMessage(msg);
						
						MapLocation prepare = prepare(allyInfo, enemyRobots);
						//System.out.println(msg.locations[i]+" -> "+prepare);
						msg.locations[i] = prepare;
						//comms.printMessage(msg);

						myRC.broadcast(msg);
						lastMessage = msg;
						enemyMsgList.clear();
						return;
					}
				}
			}
		}
		if (rand.nextInt(1 + alliedRobots.size()) == 0){ /* only once a while - minimize loops */
//			System.out.println("Sending spam (lucky)");
//			Comms comms = new Comms(myRC);
//			comms.printMessage(luckyGuess);
		
			myRC.broadcast(luckyGuess);
		}
		enemyMsgList.clear();
	}

	private MapLocation prepare(RobotInfo allyInfo, List<RobotInfo> enemyRobots) {
		boolean allyAirborne = allyInfo.type.isAirborne();
		/* same height */
		for (RobotInfo enemyInfo : enemyRobots) {
			if (enemyInfo.type.isAirborne() == allyAirborne)
				return enemyInfo.location;
		}
		/* different height */
		for (RobotInfo enemyInfo : enemyRobots) {
			if (enemyInfo.type.isAirborne() != allyAirborne)
				return enemyInfo.location;
		}
		/* random juggle */
		MapLocation loc = allyInfo.location;
		for (int i = 0; i < 20; i++) {
			loc = loc.add(navigation.getRandomDirection());
		}
		return loc;
	}
	
}
