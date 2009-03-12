package Jarek_zerg;

import java.util.*;

import Jarek_zerg.Comms;
import Jarek_zerg.Comms.CompoundMessage;
import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Channeler extends GeneralRobot {

	/**
	 * Actual task of Channeler. Can be viewed as state of robot.
	 */
	public enum Mission {
		NONE, PING, CONSTANT_DRAIN
	}

	private Mission mission;
	
	private double lastEnergonLevel;

	public Channeler(RobotController rc) {
		super(rc);
		mission = Mission.PING;
		lastEnergonLevel = rc.getEnergonLevel();
	}

	@Override
	public void processMessage(List<Comms.CompoundMessage> cmsgs){
		for (CompoundMessage cmsg : cmsgs) {
			switch(cmsg.type){
			case STOP_DRAIN:
				mission = Mission.PING;
				myRC.setIndicatorString(1, "told to cease");
				break;
			case DRAIN:
				mission = Mission.CONSTANT_DRAIN;
				myRC.setIndicatorString(1, "told to drain");
				break;
			default:
				break;
			}
		}
	}

	@Override
	public void idling() throws GameActionException {
		myRC.setIndicatorString(2, mission.toString());
		if (mission == Mission.PING)
			checkEnergonLoss();
	}
	
	@Override
	public void action() throws GameActionException {
		myRC.setIndicatorString(2, mission.toString());
		if (mission == Mission.PING)
			checkEnergonLoss();
		switch (mission) {

		case CONSTANT_DRAIN:
			if (!myRC.isAttackActive())
				myRC.drain();
			break;
		default:
		}
	}

	/**
	 * Checks if energon loss is greater than expected.
	 */
	public void checkEnergonLoss(){
		double expectedEnergonLevel = lastEnergonLevel - myRC.getRobotType().energonUpkeep();
		lastEnergonLevel = myRC.getEnergonLevel();
		if (expectedEnergonLevel > lastEnergonLevel){
			/* most likely we're being attacked */
			mission = Mission.CONSTANT_DRAIN;
		}
	}
}
