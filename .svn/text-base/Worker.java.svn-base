package Jarek_zerg;

import static battlecode.common.GameConstants.WORKER_MAX_HEIGHT_DELTA;

import java.util.*;

import Jarek_zerg.Comms.CompoundMessage;
import Jarek_zerg.Navigation.NaviResult;
import Jarek_zerg.exceptions.HungryException;
import Jarek_zerg.map.Tile;
import battlecode.common.*;

/**
 * 
 * @author Jarek_zerg
 *
 */

public class Worker extends GeneralRobot {
	/** If true then print debug strings */
	private final boolean DEBUG = false;

	/**
	 * Actual task of Worker. Can be viewed as state of robot.
	 */
	public enum Mission {
		NONE, FIND_BLOCK, GO_TO_BLOCK, LOAD_BLOCK, UNLOAD_BLOCK, GO_TO_FLUX, RANDOM_RESTART, HUNGRY
	}

	private FluxDepositInfo fluxInfo;

	private MapLocation blockLoc;

	private Mission mission;
	
	/** Stairs used to climb to flux. Do not take blocks from it */
	private List<MapLocation> stairs;

	/** Height at which flux was, when the stairs were planned */
	private int fluxHeightWhenStairsPlanned = -1;
	
	private List<MapLocation> path;
	
	/** Time used on waiting for someone to move */
	private int waitingTime = 0;
	/** Last round when waited */
	private int waitingLastRound = 0;
	
	/** Should path be rechecked. */
	private boolean recheckUnloadPath;

	/** Should the area be scanned. */
	private boolean scanLocation = true;
	
	public Worker(RobotController rc) {
		super(rc);
		fluxInfo = null;
		mission = Mission.NONE;
		stairs = new ArrayList<MapLocation>();
		path = new ArrayList<MapLocation>();
	}

	@Override
	public void idling() throws GameActionException {
		myRC.setIndicatorString(0, String.format("%1$d %2$s %3$s", myRC.getRobot().getID(), myRC.getRobotType(), myRC.getLocation()));
		myRC.setIndicatorString(1, path.toString());
		myRC.setIndicatorString(2, mission.toString());
		
		if (scanLocation){
			scanLocation = false;
			gameMap.scanNeighbourhood();
			//List<MapLocation> scanLocation = gameMap.scanNeighbourhood();
			//for (MapLocation changedLocation : scanLocation) {
			//	if (changedLocation != changedLocation) break; //do something with changedLocation
			//}
		}
		
		if (mission != Mission.NONE)
			checkIfHungry();
		feedBrethren();
		
		switch (mission) {

		case NONE:
			prepareWorkplace();
			mission = Mission.FIND_BLOCK;
			break;

		case FIND_BLOCK:
			try {
				if (myRC.getNumBlocks() >= 1) {
					setupUnloadBlock();
					break;
				}
				planDynamicStairs();
				scanForBlock();
				if (blockLoc == null){
					/* no nearby block - search further than we can sense */
					scanForBlockFarAway();
				}
				if (blockLoc == null) {
					/* still nothing found. let's wonder around randomly. maybe we'll find something */
					setupRandomRestart();
				} else {
					setupLoadBlock();
				}
			} catch (HungryException e) {
				mission = Mission.HUNGRY;
			}
			break;

		case GO_TO_FLUX:
			if (recheckUnloadPath)
				performUnloadPathRecheck();
			break;

		default:
		}
	}

	@Override
	public void action() throws GameActionException {
		myRC.setIndicatorString(0, String.format("%1$d %2$s %3$s", myRC.getRobot().getID(), myRC.getRobotType(), myRC.getLocation()));
		myRC.setIndicatorString(1, path.toString());
		myRC.setIndicatorString(2, mission.toString());
		
		if (mission != Mission.NONE)
			checkIfHungry();
		
		switch (mission) {
		case FIND_BLOCK:
			idling();
			break;
		case GO_TO_BLOCK:
			switch (navigation.goUsingPath(path)){
			case MOVED:
				path.remove(0);
				scanLocation = true;
				if (path.size() == 1)
					mission = Mission.LOAD_BLOCK;
				break;
			case CHANGED_DIRECTION:
				break;
			case CANNOT_MOVE:
				sendGoAwayMessage(path.get(0));
				break;
			default:
				// Giving up. Something went wrong, so let's just start over.
				mission = Mission.FIND_BLOCK;
			}
			break;

		case LOAD_BLOCK:
			if (myRC.canLoadBlockFromLocation(blockLoc)) {
				myRC.loadBlockFromLocation(blockLoc);
				try {
					setupUnloadBlock();
				} catch (HungryException e) {
					mission = Mission.HUNGRY;
				}
			} else {
				sendGoAwayMessage(blockLoc);
			}
			break;

		case GO_TO_FLUX:
			switch (navigation.goUsingPath(path)){
			case MOVED:
				path.remove(0);
				scanLocation = true;
				recheckUnloadPath = true;
				if (path.size() == 1)
					mission = Mission.UNLOAD_BLOCK;
				break;
			case CHANGED_DIRECTION:
				break;
			case CANNOT_MOVE:
				sendGoAwayMessage(path.get(0));
				break;
			default:
				// Giving up. Something went wrong, so let's just start over.
				mission = Mission.FIND_BLOCK;
			}
			break;

		case UNLOAD_BLOCK:
			int toHeight = myRC.senseHeightOfLocation(path.get(0));
			int myHeight = myRC.senseHeightOfLocation(myRC.getLocation());
			
			if (toHeight - myHeight >= WORKER_MAX_HEIGHT_DELTA) {
				/* Something went wrong. Destination to high. Retry pathfinding */
				mission = Mission.FIND_BLOCK;
				break;
			}

			if (!myRC.canUnloadBlockToLocation(path.get(0))){
				/* Assume other robot is blocking */
				sendGoAwayMessage(path.get(0));
			}else{
				myRC.unloadBlockToLocation(path.get(0));
				if (!stairs.contains(path.get(0)))
					stairs.add(path.get(0));
				mission = Mission.FIND_BLOCK;
			}
			break;

		case RANDOM_RESTART:
			if (path.size() == 0)
				try {
					setupRandomRestart();
				} catch (HungryException e) {
					mission = Mission.HUNGRY;
					break;
				}
			switch (navigation.goUsingPath(path)){
			case MOVED:
				path.remove(0);
				scanLocation = true;
				if (path.size() == 0)
					mission = Mission.FIND_BLOCK;
				break;
			case CHANGED_DIRECTION:
				break;
			case CANNOT_MOVE:
				sendGoAwayMessage(path.get(0));
				break;
			default:
				// Giving up. Something went wrong, so let's just start over.
				mission = Mission.FIND_BLOCK;
			}
			break;
		case HUNGRY:
			/* go to Archon using simplest direction */
			Direction dirToArchon = myRC.getLocation().directionTo(fluxInfo.location);
			/* try not to get directly under Archon. It's high ground */
			if (!myRC.getLocation().isAdjacentTo(fluxInfo.location))
				navigation.goInDirection(dirToArchon);
			
			if (myRC.getEnergonLevel() >= myRC.getMaxEnergonLevel()*0.75) {
				/* i'm full. thank's for the meal */
				mission = Mission.FIND_BLOCK;
			}
			break;
		default:
		}
	}

	private void sendGoAwayMessage(MapLocation blockerLoc) throws GameActionException {
		if (waitingLastRound + 1 == Clock.getRoundNum()){
			/* another round waiting */
			waitingTime++;
			if (waitingTime > 10){
				/* i'm bored with waiting. Let's do something else. Start over. */
				mission = Mission.FIND_BLOCK;
				return;
			}
		}else{
			waitingTime = 1;
		}
		waitingLastRound = Clock.getRoundNum();
		
		/* Send message YOU_GOTO */
		Comms.CompoundMessage cmsg = comms.new CompoundMessage();
		cmsg.type = Comms.MessageType.YOU_GOTO;
		cmsg.address = blockerLoc;
		cmsg.loc = navigation.findNearestFreeLocationUsingGameMap(gameMap, blockerLoc, blockerLoc.directionTo(myRC.getLocation()));
		comms.sendMessage(cmsg);
	}

	private void scanForBlock() throws GameActionException {
		List<MapLocation> locations = new ArrayList<MapLocation>(Arrays.asList(myRC.senseNearbyBlocks()));
		locations.removeAll(stairs);
		if (locations.size() == 0) {
			blockLoc = null;
			return;
		}

		locations = navigation.sortLocationsByDistance(locations);
		Collections.reverse(locations);
		for (MapLocation block : locations) {
			if (myRC.senseGroundRobotAtLocation(block) == null){
				/* this location is unoccupied */
				blockLoc = block;
				return;
			}
		}
		blockLoc = null;
	}

	/**
	 returns the nearest block that cannot be sensed */
	private void scanForBlockFarAway() {
		List<MapLocation> locations = gameMap.senseNearbyBlocks();
		locations.removeAll(stairs);
		if (locations.size() == 0) {
			blockLoc = null;
			return;
		}
		/* remove blocks that can be sensed */
		Iterator<MapLocation> iter = locations.iterator();
		while(iter.hasNext()) {
			MapLocation loc = iter.next();
			if (myRC.canSenseSquare(loc))
				iter.remove();
		} 
		
		locations = navigation.sortLocationsByDistance(locations);
		//Collections.reverse(locations);
		for (MapLocation block : locations) {
			if (gameMap.get(block).robotAtLocation == null){
				/* this location is unoccupied */
				blockLoc = block;
				return;
			}
		}
		blockLoc = null;
	}

	private void prepareWorkplace() throws GameActionException {
		/* find nearest flux deposit */
		FluxDeposit[] fluxDeposits = myRC.senseNearbyFluxDeposits();
		for (FluxDeposit deposit : fluxDeposits) {
			FluxDepositInfo info = myRC.senseFluxDepositInfo(deposit);
			if (fluxInfo == null)
				fluxInfo = info;
			if (fluxInfo.location.distanceSquaredTo(myRC.getLocation()) > info.location.distanceSquaredTo(myRC.getLocation()))
				fluxInfo = info;
		}
	}

	/**
	 * Plans where to put blocks to create a tower.
	 * Deprecated. Now planDynamicStairs is used.
	 */
	@SuppressWarnings("unused")
	private void planTower(Direction freeDirection) throws GameActionException {
		stairs.clear();
		stairs.add(fluxInfo.location);
		if (true) return;
		
		List<MapLocation> blocks = new ArrayList<MapLocation>(Arrays.asList(myRC.senseNearbyBlocks()));
		int howMany = 0;
		int towerSize = 0;
		for (MapLocation blockLocation : blocks) {
			howMany += myRC.senseNumBlocksAtLocation(blockLocation);
		}
		
		stairs.add(fluxInfo.location);
		towerSize++;
		howMany -= 2;
		
		Direction towerDirection = freeDirection.rotateRight().rotateRight();
		while ((howMany > 0) && (towerDirection != freeDirection)){
			stairs.add(1, fluxInfo.location.add(towerDirection));
			towerSize++;
			howMany -= towerSize * 2;
			towerDirection = towerDirection.rotateRight();
		}
		
		Collections.reverse(stairs);
	}
	
	/**
	 * Checks where to unload block on path to flux.
	 */
	private void performUnloadPathRecheck() {
		recheckUnloadPath = false;
		Tile oldTile = gameMap.get(myRC.getLocation());
		Tile actTile;
		
		/* cut where path is not accessible */
		for (MapLocation loc : path) {
			actTile = gameMap.get(loc);
			if (actTile.totalHeight - oldTile.totalHeight > WORKER_MAX_HEIGHT_DELTA) {
				path = path.subList(0, path.indexOf(loc));
				break;
			}
			oldTile = actTile;
		}
		
		/* cut where we can unload */
		path.add(0, myRC.getLocation());
		if (path.size() >= 2)
			if (gameMap.get(path.get(path.size() - 1)).totalHeight - 
				gameMap.get(path.get(path.size() - 2)).totalHeight >= WORKER_MAX_HEIGHT_DELTA) {
				/* we can't unload at the end of the path. get to the end and drop it */
				path.add(path.get(path.size() - 2));
			}
		path.remove(0);
//		path.add(0, myRC.getLocation());
//		int size = path.size();
//		oldTile = gameMap.get(path.get(size - 1));
//		while(size >= 2){
//			actTile = gameMap.get(path.get(size - 2));
//			if (gameMap.get(path.get(path.size() - 1)).totalHeight - gameMap.get(path.get(path.size() - 2)).totalHeight >= WORKER_MAX_HEIGHT_DELTA) {
//				path.remove(size - 1);
//				size--;
//			} else
//				break;
//			oldTile = actTile;
//		}
//		path.remove(0);
	}
	
	private void setupLoadBlock() throws GameActionException, HungryException{
		mission = Mission.GO_TO_BLOCK;
		path = navigation.findPathUsingAStar(gameMap, myRC.getLocation(), blockLoc, true);
		if (path.size() == 0)
			/* no path found */
			setupRandomRestart();
		if (path.size() == 1)
			mission = Mission.LOAD_BLOCK;
	}

	private void setupUnloadBlock() throws GameActionException, HungryException{
		mission = Mission.GO_TO_FLUX;
		path = navigation.findPathUsingAStar(gameMap, myRC.getLocation(), fluxInfo.location, true);
		performUnloadPathRecheck();
		if (path.size() == 0){
			/* no path found */
			MapLocation unloadLoc = canUnloadAnywhere();
			if (unloadLoc != null){
				/* drop cargo anywhere */
				path = Collections.singletonList(unloadLoc);
				mission = Mission.UNLOAD_BLOCK;
			} else {
				/* total panic. nowhere to unload block. restart */
				setupRandomRestart();
			}
		} else
		if (path.size() == 1)
			mission = Mission.UNLOAD_BLOCK;
	}
	
	/**
	 * Finds if anywhere around is a valid place to unload.
	 * Used when carried block is a burden. 
	 */
	private MapLocation canUnloadAnywhere() {
		for (Direction dir : navigation.allDirections) {
			MapLocation unloadLoc = myRC.getLocation().add(dir);
			if (myRC.canUnloadBlockToLocation(unloadLoc))
				return unloadLoc;
		}
		return null;
	}

	private void setupRandomRestart() throws GameActionException, HungryException{
		mission = Mission.RANDOM_RESTART;
		MapLocation loc = myRC.getLocation();
		for (int i=0; i<2; i++){
			loc = loc.add(navigation.getRandomDirection());
		}
		path = navigation.findPathUsingAStar(gameMap, myRC.getLocation(), loc, true);
	}
	
	/**
	 * Finds the best place to create stairs to flux.
	 * It's all heuristic. Generates search from two locations near flux.
	 * Then it checks for common suffix in found paths. This suffix becomes new stairs.
	 * It works, because if from two locations the best path has common end, then this end is the best way to get to flux.
	 * @throws GameActionException
	 * @throws HungryException 
	 */
	private void planDynamicStairs() throws GameActionException, HungryException {
		Integer fluxHeight = gameMap.get(fluxInfo.location).totalHeight;
		if (fluxHeight == fluxHeightWhenStairsPlanned){
			if (DEBUG) System.out.println("No need to plan stairs now");
			//return; /* No return for now. Always plan stairs, so no stupid block lockups will occur */
		}
		
		if (DEBUG) System.out.println("-----TOWER-----");
		Direction randDir = navigation.getRandomDirection();
		MapLocation start1 = fluxInfo.location.add(randDir).add(randDir);
		MapLocation start2 = fluxInfo.location.subtract(randDir).subtract(randDir);
		if ((gameMap.get(start1) == null) || (gameMap.get(start2) == null))
			/* unlucky - start position wasn't scanned yet */
			return;
		
		fluxHeightWhenStairsPlanned = fluxHeight;
		
		List<MapLocation> fluxPath1 = navigation.findPathUsingAStar(gameMap, start1, fluxInfo.location, false);
		//if (DEBUG) System.out.println(fluxPath1);
		List<MapLocation> fluxPath2 = navigation.findPathUsingAStar(gameMap, start2, fluxInfo.location, false);
		//if (DEBUG) System.out.println(fluxPath2);
		Collections.reverse(fluxPath1);
		Collections.reverse(fluxPath2);
		List<MapLocation> result = new ArrayList<MapLocation>();
		while ((fluxPath1.size() > 0) && (fluxPath2.size() > 0) && (fluxPath1.get(0).equals(fluxPath2.get(0)))){
			result.add(fluxPath1.remove(0));
			fluxPath2.remove(0);
		}
		Collections.reverse(result);
		if (result.size() >= stairs.size())
			/* prefer fresh data, but not too short */
			stairs = result;
		if (DEBUG) System.out.println(result);
		if (DEBUG) System.out.println(navigation.changePathToDirections(result));
		if (DEBUG) System.out.println("----/TOWER-----");
	}
	
	@Override
	public void processMessage(List<Comms.CompoundMessage> cmsgs){
		for (CompoundMessage cmsg : cmsgs) {
			switch(cmsg.type){
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
						scanLocation = true;
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
			}
		}
	}
	
	/**
	 * Tests if robots energon level is low. If that's the case, then it changes mission to Hungry.
	 */
	private void checkIfHungry(){
		if (myRC.getEnergonLevel() * 2.5 < myRC.getMaxEnergonLevel()) {
			mission = Mission.HUNGRY;
		}
	}
}
