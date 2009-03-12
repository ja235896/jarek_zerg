package Jarek_zerg;

import java.util.ArrayList;

import Jarek_zerg.Comms;
import Jarek_zerg.GeneralRobot;
import battlecode.common.*;


/**
 * 
 * @author Mimuw_ZERG
 *
 */

public class RobotPlayer implements Runnable {

	private final RobotController myRC;

	private GeneralRobot myRobot;

	public RobotPlayer(RobotController rc) {
		myRC = rc;
		switch (myRC.getRobotType()) {
		case ARCHON:
			myRobot = new Archon(myRC);
			break;
		case WORKER:
			myRobot = new Worker(myRC);
			break;
		case CHANNELER:
			myRobot = new Channeler(myRC);
			break;
		case SOLDIER:
			myRobot = new Soldier(myRC);
			break;
		case SCOUT:
			myRobot = new Scout(myRC);
			break;
		case CANNON:
			myRobot = new Cannon(myRC);
			break;
		default:
			System.out.println("Please create AI for my robot type " + myRC.getRobotType().toString());
		}
		say_hello();
	}

	public void run() {
		while (true) {
			try {
				/** * beginning of main loop ** */

				Message[] allMessages = myRC.getAllMessages();
				for (Message msg : allMessages) {
					ArrayList<Comms.CompoundMessage> cmsgs = myRobot.comms.translateMessage(msg);
					if (cmsgs!=null)
						myRobot.processMessage(cmsgs);
				}
				if (myRC.isMovementActive()) {
					myRobot.idling();
				} else {
					myRobot.action();
				}
				myRobot.comms.transmitMessages();
				myRC.yield();

				/** * end of main loop ** */ // yes, I noticed...
			} catch (Exception e) {
				// TODO: uncomment for testing XXX
				//System.out.println("caught exception:");
				//e.printStackTrace();
				myRC.yield(); /* prevent multiple exceptions */
			}
		}
	}

	public void say_hello() {
		myRC.setIndicatorString(0, String.format("%1$d %2$s %3$s", myRC.getRobot().getID(), myRC.getRobotType(), myRC.getLocation()));
	}
}
