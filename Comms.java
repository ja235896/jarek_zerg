package Jarek_zerg;

import java.util.*;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.Message;
import battlecode.common.RobotController;
import battlecode.common.Team;

/**
 * 
 * @author Mimuw_ZERG
 *
 */

public class Comms {
	/** If true then print debug strings */
	private final boolean DEBUG = false;

	private final RobotController myRC;
	public final int signature;
	
	public enum MessageType {
		/**
		 * first parameter - MessageType code
		 * second parameter - YOU_ messages are addressed to a particular robot
		 * third parameter - some messages have an additional int parameter
		 * fourth parameter - some messages have an additional MapLocation parameter
		 */
		NONE		(0,false,false,false),
		GOTO		(2,false,false,true), 	YOU_GOTO		(3,true,false,true),
		ATTACK		(4,false,false,true), 	YOU_ATTACK		(5,true,false,true),
		DRAIN		(6,false,false,false), 	YOU_DRAIN		(7,true,false,false), 
		STOP_DRAIN	(8,false,false,false), 	YOU_STOP_DRAIN	(9,true,false,false),
		GOING_IN_DIR(10,false,true,false), 	GOING_TO_LOC	(11,false,false,true),
		SCOUT		(12,false,false,true), 	YOU_SCOUT		(13,true,false,true),
		SIEGE_STD   (14,false,false,true),  SIEGE_SEC  		(15,false,false,true),
		SIEGE_FLANK (16,false,false,true),
		DEFENDING	(17,false,false,true),
		ARTY		(18,false,true,true),	YOU_ARTY		(19,true,true,true),
		AIMME		(20,false,false,true);
		
		public int code;
		public boolean personalized;
		public boolean hasParam;
		public boolean hasLoc;
		private MessageType(int code,boolean personalized,boolean hasParam,boolean hasLoc){
			this.code = code;
			this.personalized = personalized;
			this.hasParam = hasParam;
			this.hasLoc = hasLoc;
		}
		
		public static MessageType fromInt(int i){
			switch(i){
			case 2: return GOTO;
			case 3: return YOU_GOTO;
			case 4: return ATTACK;
			case 5: return YOU_ATTACK;
			case 6: return DRAIN;
			case 7: return YOU_DRAIN;
			case 8: return STOP_DRAIN;
			case 9: return YOU_STOP_DRAIN;
			case 10: return GOING_IN_DIR;
			case 11: return GOING_TO_LOC;
			case 12: return SCOUT;
			case 13: return YOU_SCOUT;
			case 14: return SIEGE_STD;
			case 15: return SIEGE_SEC;
			case 16: return SIEGE_FLANK;
			case 17: return DEFENDING;
			case 18: return ARTY;
			case 19: return YOU_ARTY;
			case 20: return AIMME;
			default: return NONE;
			}
		}
		
		public MessageType unpersonalize(){
			switch(this){
			case YOU_GOTO: return GOTO;
			case YOU_ATTACK: return ATTACK;
			case YOU_DRAIN: return DRAIN;
			case YOU_STOP_DRAIN: return STOP_DRAIN;
			case YOU_SCOUT: return SCOUT;
			case YOU_ARTY: return ARTY;
			default: return this;
			}
		}
	}
	
	public class CompoundMessage{
		// message type
		public MessageType type;
		// who is this message to
		MapLocation address;
		// parameters
		MapLocation loc;
		int param;
		public CompoundMessage(){}
	}
	
	private ArrayList<CompoundMessage> queued = new ArrayList<CompoundMessage>();

	public Comms(RobotController rc) {
		myRC = rc;
		if (myRC.getTeam() == Team.A)
			signature = 154;
		else
			signature = 210;
	}
	
	public int dirToInt(Direction dir){
		switch (dir){
		case EAST: return 0;
		case NONE: return 1;
		case NORTH: return 2;
		case NORTH_EAST: return 3;
		case NORTH_WEST: return 4;
		case OMNI: return 5;
		case SOUTH: return 6;
		case SOUTH_EAST: return 7;
		case SOUTH_WEST: return 8;
		case WEST: return 9;
		default: return 10;
		}
	}
	
	public Direction intToDir(int l){
		switch(l){
		case 0: return Direction.EAST;
		case 1: return Direction.NONE;
		case 2: return Direction.NORTH;
		case 3: return Direction.NORTH_EAST;
		case 4: return Direction.NORTH_WEST;
		case 5: return Direction.OMNI;
		case 6: return Direction.SOUTH;
		case 7: return Direction.SOUTH_EAST;
		case 8: return Direction.SOUTH_WEST;
		case 9: return Direction.WEST;
		default: return Direction.NONE;
		}
	}
	
	// convert message to a list of easy-to-read classes, ignoring messages not addressed to me
	public ArrayList<CompoundMessage> translateMessage(Message msg){
		ArrayList<CompoundMessage> result = new ArrayList<CompoundMessage>();
		int i = 1, j = 0;
		if ((msg != null) && (msg.ints != null)) {
			if (msg.ints[0] != signature) return result;
			while (i < msg.ints.length) {
				CompoundMessage cmsg = new CompoundMessage();
				cmsg.type = MessageType.fromInt(msg.ints[i]);
				i++;
				if (cmsg.type.personalized) {
					cmsg.address = msg.locations[j];
					j++;
					// only add YOU_ messages if I'm the one it's addressed to
					if (!cmsg.address.equals(myRC.getLocation()))
						continue;
					else
						cmsg.type = cmsg.type.unpersonalize();
				}
				if (cmsg.type.hasParam) {
					cmsg.param = msg.ints[i];
					i++;
				}
				if (cmsg.type.hasLoc) {
					cmsg.loc = msg.locations[j];
					j++;
				}
				result.add(cmsg);
			}
		}
		return result;
	}
	
	public Message buildMessage(List<CompoundMessage> cmsgs){
		Message result = new Message();
		int intsSize = 1, locsSize = 0;
		for (CompoundMessage cmsg : cmsgs) {
			if (cmsg.type.hasParam)
				intsSize += 2;
			else
				intsSize++;
			if (cmsg.type.hasLoc) locsSize++;
			if (cmsg.type.personalized) locsSize++;
		}
		if (intsSize!=0)
			result.ints = new int[intsSize];
		result.ints[0] = signature;
		if (locsSize!=0)
			result.locations = new MapLocation[locsSize];
		int i = 1, j = 0;
		for (CompoundMessage cmsg : cmsgs) {
			String s = "Sending message " + cmsg.type;
			result.ints[i] = cmsg.type.code;
			i++;
			if (cmsg.type.hasParam){
				result.ints[i] = cmsg.param;
				i++;
				s += " with " + cmsg.param;
			}
			if (cmsg.type.personalized){
				result.locations[j] = cmsg.address;
				j++;
				s += " for " + cmsg.address;
			}
			if (cmsg.type.hasLoc){
				result.locations[j] = cmsg.loc;
				j++;
				s += " loc " + cmsg.loc;
			}
			if (DEBUG) System.out.println(s);
		}
		return result;
	}
	
	public void sendMessage(CompoundMessage cmsg){
		queued.add(cmsg);
	}
	
	public void sendMessage(List<CompoundMessage> cmsgs){
		for (CompoundMessage compoundMessage : cmsgs) {
			queued.add(compoundMessage);
		}
	}
	
	public void transmitMessages() throws GameActionException{
		if (queued.size() > 0) {
			Message msg = buildMessage(queued);
			myRC.broadcast(msg);
			queued.clear();
		}
	}
}
