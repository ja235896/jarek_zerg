package Jarek_zerg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Jarek_zerg.Comms;
import Jarek_zerg.Comms.CompoundMessage;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

/**
 * 
 * @author Mimuw_ZERG
 *
 */

public class Archon extends GeneralRobot {
	/** If true then print debug strings */
	private final boolean DEBUG = false;

	/**
	 * Actual task of Archon. Can be viewed as state of robot.
	 */
	public enum Mission {
		NONE, 
		INIT, 
		SPAWN_SCOUT, FEED_SCOUT, 
		FIND_FLUX_DEPOSIT, FIND_RANDOM_FLUX_DEPOSIT, DRAIN_DEPOSIT, FOUND_DEPOSIT,
		GO_TO_ALLIES, GO_TO_COMBAT,
		SPAWN_ARMED_FORCES, FEED_ARMED_FORCES, 
		SIEGE, DEFEND_FLUX;
		
		public boolean isCombat(){
			return ((this==SIEGE)||(this==DEFEND_FLUX));				
		}
		
		public boolean isDraining(){
			return ((this==SPAWN_ARMED_FORCES)
					|| (this==DEFEND_FLUX)
					|| (this==DRAIN_DEPOSIT)
					|| (this==FOUND_DEPOSIT)
					|| (this==FEED_ARMED_FORCES));			
		}
		
		public double feedValue(RobotType rt){
			if (isCombat()){
				switch(rt){
				case ARCHON: return 0.2;
				case SOLDIER:
				case CHANNELER:
				case CANNON:
					return 0.5;
				default: return 0.3;
				}
			} else {
				switch(rt){
				case ARCHON: return 0.2;
				default: return 0.6;
				}
			}
		}
	}
	
	public enum CombatMode { 
		NONE, 
		STD, SEC, FLANK;
		
		static public CombatMode supplement(CombatMode prev, Comms.MessageType mt){
			switch(mt){
			case DEFENDING: return SEC;
			case SIEGE_STD:
				if ((prev == NONE)||(prev == STD))
					return SEC;
				else
					return prev;
			case SIEGE_SEC: 
				if ((prev == NONE)||(prev == SEC))
					return FLANK;
				else
					return prev;
			case SIEGE_FLANK: // we dont want trashing, so at worst all end up FLANK
				if (prev == NONE)
					return STD;
				else
					return prev;
			default:
				if (prev == NONE)
					return STD;
				else
					return prev;
			}
		}
		
		public Comms.MessageType msgType() {
			switch (this) { 
			case SEC: return Comms.MessageType.SIEGE_SEC;
			case FLANK: return Comms.MessageType.SIEGE_FLANK;
			default: return Comms.MessageType.SIEGE_STD;
			}
		}
		
		public int armySize(){
			switch(this){
			case SEC: return 2;
			default: return 4;
			}
		}
	}

	private FluxDepositInfo fluxInfo;
	private ArrayList<MapLocation> avoidedFlux = new ArrayList<MapLocation>();
	private int boastCounter = 0;
	private Direction previousSense = null;
	
	private int boastCountdown = 50;

	private Direction nextDirection = Direction.NONE;

	private Mission mission;
	private int myNumber = -1;
	
	private int spawned = 0;
	
	private int armySize = 0;
	private boolean doneFeeding = false;
	
	private MapLocation siegeLoc;
	private CombatMode combatMode = CombatMode.NONE;
	
	private int maxSpawnSeries = 2;
	private int scoutCount = 2;
	private boolean initialScouts = false;
	private int workerCount = 2;
	
	private int oppScore = 0;
	
	private RobotType nextRobotType(int sequence){
		//if (sequence == 0) return RobotType.CHANNELER;
		//if (sequence == 2) return RobotType.WORKER;
		return RobotType.CANNON;
	}
	
	private boolean useFluxBurn = true;
	private double fluxBurnThreshold = 0.2;
	private MapLocation[] oldAlliedArchons = new MapLocation[6];
	
	/** Energon treshold. Won't spawn when less than this will remain */
	private final double minimalEnergonLevel = 20.0;

	/** Panic level. If energon is that low, then we panic */
	private final double panicEnergonLevel = 24.0;

	public Archon(RobotController rc) {
		super(rc);
		fluxInfo = null;
		mission = Mission.INIT;
	}
	
	@Override
	public void processMessage(List<Comms.CompoundMessage> cmsgs){
		for (CompoundMessage cmsg : cmsgs) {
			switch(cmsg.type){
			case GOING_TO_LOC:
				avoidedFlux.add(cmsg.loc);
				break;
			case DEFENDING:
			case SIEGE_SEC:
			case SIEGE_STD:
			case SIEGE_FLANK:
				if ((!mission.isCombat() && (!mission.isDraining()))){
					myRC.setIndicatorString(1, "Going to a fight");
					mission = Mission.GO_TO_COMBAT;
					siegeLoc = cmsg.loc;
					combatMode = CombatMode.supplement(combatMode,cmsg.type);
				}
				break;
			case AIMME:
				try{
					checkCannons();
				}catch(Exception e){
					
				}
				break;
			default:
				break;
			}
		}
	}
	
	public void commonPreMission() throws GameActionException{
		if (Clock.getRoundNum() %11 == 0)
			checkChannelers();
		//if (Clock.getRoundNum() %10 == 0)
		//	checkCannons(); // cannons require precision...
		if (mission == Mission.SIEGE)
			myRC.setIndicatorString(2, Integer.toString(myNumber) + " " + mission.toString() + " " + combatMode);
		else
			myRC.setIndicatorString(2, Integer.toString(myNumber) + " " + mission.toString());
		//checkIfArchonDied(); /* it seems not to be the best solution */
	}
	
	/**
	 * Checks if any archon died, and if so then it goes there.
	 */
	@SuppressWarnings("unused")
	private void checkIfArchonDied() {
		MapLocation[] alliedArchons = myRC.senseAlliedArchons();
		if (oldAlliedArchons.length != alliedArchons.length){
			/* oh noes! someone died */

			if (DEBUG) System.out.println("oh noes! someone died");
			MapLocation archonDiedLoc = whereArchonDied(alliedArchons);
			if (DEBUG) System.out.println(archonDiedLoc);
			
			/* revenge! let's go there and deal with enemy */
			myRC.setIndicatorString(1, "Going to a fight");
			mission = Mission.GO_TO_COMBAT;
			siegeLoc = archonDiedLoc;
			if (combatMode == CombatMode.NONE)
				combatMode =  CombatMode.STD;
			
			//myRC.breakpoint();
		}
		this.oldAlliedArchons = alliedArchons;
	}

	/**
	 * Guesses where archon died.
	 */
	private MapLocation whereArchonDied(MapLocation[] alliedArchons){
		int j;
		for (int died=0; died<oldAlliedArchons.length; died++){
			boolean fail = false;
			for (int i=0; i<oldAlliedArchons.length; i++){
				if (i == died) continue;
				if (i > died)
					j = i - 1;
				else
					j = i;
				if (alliedArchons[j].distanceSquaredTo(oldAlliedArchons[i]) >= 3.0)
					fail = true;
			}
			if (!fail)
				return oldAlliedArchons[died];
		}
		
		if (DEBUG) System.out.println("Error: Have no idea where archon died");
		return oldAlliedArchons[0]; // search failed
	}
	
	/**
	 * Is this situation dangerous?
	 * @throws GameActionException 
	 */
	private void shouldIPanic() throws GameActionException{
		if (Clock.getRoundNum() <= 20) return; /* why panic so early? */
		
		if (myRC.hasActionSet() || (myRC.getRoundsUntilMovementIdle() > 0)){
			/* cannot move. panic won't be useful */
			return;
		}
			
		if (myRC.getEnergonLevel() > panicEnergonLevel){
			/* not hungry. no need to panic. */
			return;
		}

		Team myTeam = myRC.getTeam();

		int enemyCount = 0;
		int enemyCenterX = 0;
		int enemyCenterY = 0;

		/* where's the center of enemies */
		List<RobotInfo> nearbyRobots = navigation.robotsToRobotsInfo(navigation.senseNearbyRobots(true, true));
		for (RobotInfo info : nearbyRobots) {
			if (info.team != myTeam) {
				enemyCount++;
				enemyCenterX += info.location.getX();
				enemyCenterY += info.location.getY();
			}
		}
		
		if (enemyCount == 0) {
			/* we're hungry, but no need to panic. no enemies nearby */
			return;
		}
		enemyCenterX /= enemyCount;
		enemyCenterY /= enemyCount;
		MapLocation enemyCenter = new MapLocation(enemyCenterX, enemyCenterY);
		
		/* ok. now i'm going to panic */
		Direction directionToRunAway = myRC.getLocation().directionTo(enemyCenter).opposite();
		if (!myRC.canMove(directionToRunAway)){
			directionToRunAway = navigation.findNearestFreeDirection(directionToRunAway);
		}
		if (DEBUG) System.out.println("Panic! Run away "+directionToRunAway);
		navigation.forcedGoOneStep(myRC.getLocation().add(directionToRunAway));
	}
	
	public void commonPostMission() throws GameActionException{
		if (Clock.getRoundNum() % 10 == 0)
			feedMinions();
		if ((mission.isCombat()) && (useFluxBurn)){
			if (myRC.getEnergonLevel() < fluxBurnThreshold * RobotType.ARCHON.maxEnergon()){
				int myScore = myRC.senseTeamPoints(myRC.getRobot());
				if (Math.abs(oppScore - myScore) > 1000){ /* if score is close then don't risk losing */
					try{
						myRC.burnFlux();
					}
					catch (Exception e){}
				}
			}
		}
		shouldIPanic();
	}

	@Override
	public void idling() throws GameActionException {
		commonPreMission();
		switch (mission) { // TODO: add stuff, refactor some from action() into methods
		case DEFEND_FLUX:
			defendFlux();
			break;
		case SIEGE:
			siege();
			break;
		default:
		}
		commonPostMission();
	}

	@Override
	public void action() throws GameActionException {
		commonPreMission();
		MapLocation loc;
		switch (mission) {
		
		case INIT:
			// get archon number
			loc = myRC.getLocation();
			MapLocation archons[] = myRC.senseAlliedArchons();
			for (int i = 0; i < archons.length; i++)
				if (loc == archons[i])
					myNumber = i;
			if (myNumber == -1) myNumber = -2; // WTF?
			// set archon initial mission - number-dependant
			if (myNumber >= 2)
				if (myNumber >= 4){
				//mission = Mission.SPAWN_SCOUT;
				mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
				spawned = 0;
				initialScouts = false;
				} else mission = Mission.FIND_FLUX_DEPOSIT;
			else {
				mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
			}
			break;			
		
		case SPAWN_SCOUT:
			if (performAttackCheck()) break;
			if (myRC.canMove(myRC.getDirection())){
				if (myRC.getEnergonLevel() - minimalEnergonLevel > RobotType.SCOUT.spawnCost()){
					myRC.spawn(RobotType.SCOUT);
					mission = Mission.FEED_SCOUT;
					spawned++;
				}
			} else
				myRC.setDirection(myRC.getDirection().rotateLeft());
			break;
		
		case FEED_SCOUT:
			if (performAttackCheck()) break;
			feedMinions();
			if (doneFeeding)
				if ((spawned < scoutCount)&&initialScouts)
					mission = Mission.SPAWN_SCOUT;
				else {
					mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
				}
			break;

		case FIND_RANDOM_FLUX_DEPOSIT:
			if (performAttackCheck()) break;
			if(locateNearestFlux()){
				previousSense = null;
				mission = Mission.FIND_FLUX_DEPOSIT;
			} else {
				if (Clock.getRoundNum() % 8 == 0){
					ArrayList<MapLocation> avoidedFluxToRemove = new ArrayList<MapLocation>();
					for (MapLocation mapLocation : avoidedFlux) {
						if (mapLocation.distanceSquaredTo(myRC.getLocation()) >= 36)
							avoidedFluxToRemove.add(mapLocation);
					}
					for (MapLocation mapLocation : avoidedFluxToRemove)
						avoidedFlux.remove(mapLocation);
				}
				if (nextDirection == Direction.NONE)
					nextDirection = navigation.getRandomDirection();
				else {
					Direction nextSense = myRC.senseDirectionToUnownedFluxDeposit(); 
					if (previousSense != null) {
						if (navigation.guessFluxByTriangulation(nextDirection,
								previousSense, nextSense)) {
							mission = Mission.FIND_FLUX_DEPOSIT;
							previousSense = null;
							break;
						}
					}
					previousSense = nextSense;
				}
				if (myRC.canMove(nextDirection)) {
					try {
						if (nextDirection != myRC.getDirection())
							myRC.setDirection(nextDirection);
						else
							myRC.moveForward();
					} catch (GameActionException e) {
						nextDirection = navigation.getRandomDirection();
					}
				} else {
					loc = myRC.getLocation();
					loc = loc.add(nextDirection);
					if (myRC.senseAirRobotAtLocation(loc) == null)
						nextDirection = navigation.getRandomDirection();
					else
						navigation.goInDirection(nextDirection);
				}
			}
			break;
			
		case FIND_FLUX_DEPOSIT:
			if (performAttackCheck()) break;
			fluxInfo = null;
			if(myRC.senseNearbyFluxDeposits().length == 0){ // no deposit nearby
				nextDirection = myRC.senseDirectionToUnownedFluxDeposit();
			} else {
				// deposit nearby 
				if (locateNearestFlux()) { // deposit can be taken over 
					// locateNearestFlux() success --> fluxInfo is set
					if (boastCounter == 0) { // broadcast message
						boastCounter = boastCountdown;

						Comms.CompoundMessage cmsg = comms.new CompoundMessage();
						cmsg.type = Comms.MessageType.GOING_TO_LOC;
						cmsg.loc = fluxInfo.location;
						comms.sendMessage(cmsg);
					}
					boastCounter--;
				} else { // deposit nearby, can't be taken over
					mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
					nextDirection = Direction.NONE;
					break;
				}
			}
			goToFluxDeposit();
			break;

		case FOUND_DEPOSIT:
			nextDirection = findSpawnLocation(myRC.getDirection());
			if (nextDirection == Direction.NONE) {
				mission = Mission.DRAIN_DEPOSIT;
				break;
			}
			if (nextDirection != myRC.getDirection()) {
				myRC.setDirection(nextDirection);
			} else {
				myRC.spawn(RobotType.WORKER);
				mission = Mission.SPAWN_ARMED_FORCES;
				spawned = 0;
			}
			break;

		case SPAWN_ARMED_FORCES:
			if (performDefendCheck()) break;
			nextDirection = findSpawnLocation(myRC.getDirection());
			if (nextDirection == Direction.NONE) {
				mission = Mission.DRAIN_DEPOSIT;
				break;
			}
			if (nextDirection != myRC.getDirection()) {
				myRC.setDirection(nextDirection);
			} else {
				RobotType rt = nextRobotType(spawned);
				if (myRC.getEnergonLevel() - minimalEnergonLevel >= rt.spawnCost()) {
					myRC.spawn(rt);
					++spawned;
					mission = Mission.FEED_ARMED_FORCES;
				}
			}
			break;
			
		case FEED_ARMED_FORCES:
			if (performDefendCheck()) break;
			feedMinions();
			if (doneFeeding)
				mission = Mission.DRAIN_DEPOSIT;
			if (fluxInfo.roundsAvailableAtCurrentHeight == 0){
				avoidedFlux.add(fluxInfo.location);
				mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
			}
			break;

		case DRAIN_DEPOSIT:
			if (performDefendCheck()) break;
			/* Draining flux */
			FluxDeposit fluxDeposit = myRC.senseFluxDepositAtLocation(myRC.getLocation());
			if (fluxDeposit != null)
				fluxInfo = myRC.senseFluxDepositInfo(fluxDeposit);
			/* Leave if drained */
			if ((fluxDeposit == null) || (fluxInfo.roundsAvailableAtCurrentHeight == 0)) {
				mission = Mission.FIND_FLUX_DEPOSIT;
				locateNearestFlux();
				break;
			}
			if ((spawned < maxSpawnSeries) || (armySize < maxSpawnSeries)){
				mission = Mission.SPAWN_ARMED_FORCES;
				break;
			}
			checkWorkerCount();
			break;

		case GO_TO_ALLIES:
			if (performAttackCheck()) break;
			MapLocation[] locations = myRC.senseAlliedArchons();
			nextDirection = myRC.getLocation().directionTo(locations[0]);
			navigation.goInDirection(nextDirection);
			break;
		
		case GO_TO_COMBAT:
			MapLocation tmp = siegeLoc;
			if (performAttackCheck()) break;
			siegeLoc = tmp;
			nextDirection = myRC.getLocation().directionTo(siegeLoc);
			navigation.goInDirection(nextDirection);			
			break;

		case SIEGE:
			siege();
			break;
		
		case DEFEND_FLUX:
			defendFlux();
			break;
			
		default:
		}
		commonPostMission();
	}

	private void performArmySpawn() throws GameActionException {
		Direction dir = findSpawnLocation(myRC.getDirection());
		if (dir != myRC.getDirection()){
			if (myRC.getRoundsUntilMovementIdle() > 0)
				return; /* sorry, no movement - no spawning */
			if (dir.equals(Direction.NONE))
				return; /* sorry, no place to spawn */
			myRC.setDirection(dir);
		}
		RobotType rt = null;
		//switch(combatMode){
		//case SEC:
			//if (spawned == 0)
				rt = RobotType.CANNON;
			//else 
				//rt = RobotType.CHANNELER;
		//	break;
		//default:
		//	rt = RobotType.SOLDIER;
		//	break;
		//}
		if (myRC.getEnergonLevel() - minimalEnergonLevel > rt.spawnCost()) {
			myRC.spawn(rt);
			spawned++;
		}
	}

	private void performArmySend() throws GameActionException {
		if (spawned == combatMode.armySize()) {
			spawned = 0;
			Comms.CompoundMessage cmsg = comms.new CompoundMessage();
			cmsg.type = Comms.MessageType.ATTACK;
			cmsg.loc = siegeLoc;
			comms.sendMessage(cmsg);
		}
	}
	
	private boolean performAttackCheck() throws GameActionException {
		if (checkEnemiesToAttack()) {
			mission = Mission.SIEGE;
			spawned = 0;

			boastCounter = boastCountdown;
			Comms.CompoundMessage cmsg = comms.new CompoundMessage();
			cmsg.type = combatMode.msgType();
			cmsg.loc = siegeLoc;
			comms.sendMessage(cmsg);

			myRC.setIndicatorString(1, "We will crush you like an ant!");
			return true;
		} else
			return false;
	}
	
	private boolean performDefendCheck() throws GameActionException {
		if (checkEnemiesToAttack()) {
			mission = Mission.DEFEND_FLUX;
			spawned = 0;

			boastCounter = boastCountdown;
			Comms.CompoundMessage cmsg = comms.new CompoundMessage();
			cmsg.type = Comms.MessageType.DEFENDING;
			cmsg.loc = myRC.getLocation();
			comms.sendMessage(cmsg);

			myRC.setIndicatorString(1, "We will hold our ground!");
			return true;
		} else
			return false; 
	}
	
	private boolean checkEnemiesToAttack() throws GameActionException{
		List<Robot> robots = new ArrayList<Robot>();
		Collections.addAll(robots, myRC.senseNearbyGroundRobots());
		Collections.addAll(robots, myRC.senseNearbyAirRobots());
		int seen = 0;
		for (Robot robot : robots) {
			RobotInfo info = myRC.senseRobotInfo(robot);			
			if ((info.team != myRC.getTeam())) {
				if (info.type == RobotType.ARCHON)
					oppScore = myRC.senseTeamPoints(robot); /* update opp score */
				if ((info.type == RobotType.ARCHON) && // hard-coded, so what
						(info.location.distanceSquaredTo(myRC.getLocation()) < 40)) {
					siegeLoc = info.location;
					return true;
				}
				if (info.type == RobotType.SOLDIER)
					seen+=2;
				else if ((info.type == RobotType.CANNON) || (info.type == RobotType.CHANNELER))
					seen+=3;
				else
					seen++;
				if (seen >= 4){
					siegeLoc = info.location;
					return true;
				}
			}
		}
		siegeLoc = null;
		return false;
	}
	
	private void feedMinions() throws GameActionException {
		List<Robot> robots = new ArrayList<Robot>();
		Collections.addAll(robots, myRC.senseNearbyGroundRobots());
		Collections.addAll(robots, myRC.senseNearbyAirRobots());
		doneFeeding = true;
		armySize = 0;
		double threshold;

		double myEnergonLevel = myRC.getEnergonLevel() - minimalEnergonLevel;
		for (Robot robot : robots) {
			RobotInfo info = myRC.senseRobotInfo(robot);
			if (info.team == myRC.getTeam()) {
				threshold = mission.feedValue(info.type);
				if (info.location.isAdjacentTo(myRC.getLocation()) || info.location.equals(myRC.getLocation()))
					if ((info.energonLevel < myEnergonLevel)
							&& (info.energonLevel <= threshold * info.maxEnergon)) {
						double transferAmount = Math.min(ENERGON_RESERVE_SIZE
								- info.energonReserve, myEnergonLevel);
						myRC.transferEnergon(transferAmount, info.location,
								robot.getRobotLevel());
						myEnergonLevel -= transferAmount;
					}
				if ((info.type != RobotType.ARCHON) && (info.energonLevel <= threshold * info.maxEnergon))
					if (info.location.isAdjacentTo(myRC.getLocation()))
						doneFeeding = false;
				if ((info.type != RobotType.WORKER) && (info.type != RobotType.ARCHON))
					armySize++;
			}
		}
	}

	private void goToFluxDeposit() throws GameActionException {
		if (nextDirection == Direction.NONE) {
			mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
			return;
		}
		if (nextDirection == Direction.OMNI) {
			mission = Mission.FOUND_DEPOSIT;
			boastCounter = 0;
			return;
		}

		navigation.goInDirection(nextDirection);
	};
	
	private boolean checkFluxOkToTakeOver(FluxDepositInfo info) throws GameActionException {
		if (avoidedFlux.contains(info.location)){
			myRC.setIndicatorString(1, "Flux blacklisted, leave");
			return false;			
		}
		Robot r = myRC.senseAirRobotAtLocation(info.location);
		if (r == null){
			myRC.setIndicatorString(1, "No one over flux, get it!");
			return true;
		}
		if (r == myRC.getRobot()){
			myRC.setIndicatorString(1, "Directly over flux, happy!");
			return true;
		}
		RobotInfo ri = myRC.senseRobotInfo(r);
		if ((ri.team == myRC.getTeam()) && (ri.type == RobotType.ARCHON)){
			myRC.setIndicatorString(1, "Flux controlled by one of ours, leave");
			return false;
		}
		myRC.setIndicatorString(1, "Flux controlled by them! Get it!");
		return true;
	}

	private boolean locateNearestFlux() throws GameActionException {
		FluxDeposit[] fluxDeposits = myRC.senseNearbyFluxDeposits();
		for (FluxDeposit deposit : fluxDeposits) {
			FluxDepositInfo info = myRC.senseFluxDepositInfo(deposit);
			if (checkFluxOkToTakeOver(info)){
				nextDirection = myRC.getLocation().directionTo(info.location);
				fluxInfo = info;
				return true;
			}
		}
		return false;
	}

	public Direction findSpawnLocation(Direction start) throws GameActionException {
		MapLocation archonLoc = myRC.getLocation();
		Direction[] allDirections = navigation.getAllDirections(myRC.getDirection());
		for (Direction direction : allDirections) {
			MapLocation spawnLoc = archonLoc.add(direction);
			if (myRC.senseTerrainTile(spawnLoc).isTraversableAtHeight(RobotLevel.ON_GROUND))
				if (myRC.senseGroundRobotAtLocation(spawnLoc) == null)
					return direction;
		}
		return Direction.NONE;
	}

	private void checkWorkerCount() throws GameActionException{
		int workersNearby = 0;
		ArrayList<Robot> robots = new ArrayList<Robot>(); 
		Collections.addAll(robots, myRC.senseNearbyGroundRobots());
		RobotInfo ri;
		for (Robot r : robots) {
			ri = myRC.senseRobotInfo(r);
			// is it one of our workers?
			if ((ri.type == RobotType.WORKER) && (ri.team == myRC.getTeam())) {
				if (ri.roundsUntilAttackIdle <= 3)
					// remember it
					workersNearby++;
			}
		}
		if (workersNearby<workerCount){
			// spawn a Worker
			Direction dir = findSpawnLocation(myRC.getDirection());
			if (dir == Direction.NONE) return;
			if (dir != myRC.getDirection()) {
				myRC.setDirection(dir);
				return;
			}
			RobotType rt = RobotType.WORKER;
			if (myRC.getEnergonLevel() - minimalEnergonLevel >= rt.spawnCost()) {
				myRC.spawn(rt);
			}
		}
	}
	
	private void defendFlux() throws GameActionException{
		FluxDeposit fluxDeposit = myRC.senseFluxDepositAtLocation(myRC.getLocation());
		if (fluxDeposit == null) {
			mission = Mission.FIND_FLUX_DEPOSIT;
			locateNearestFlux();
			return;
		}

		if (!checkEnemiesToAttack()) {
			mission = Mission.FEED_ARMED_FORCES;
			myRC.setIndicatorString(1, "");
			return;
		}
		combatMode = CombatMode.NONE;
		if (Clock.getRoundNum() % 2 == 0) {
			feedMinions();
		} else
			doneFeeding = true;
		if (doneFeeding) {
			performArmySpawn();
			performArmySend();
		}
		if (boastCounter <= 0) {
			boastCounter = boastCountdown;
			Comms.CompoundMessage cmsg = comms.new CompoundMessage();
			cmsg.type = Comms.MessageType.DEFENDING;
			cmsg.loc = siegeLoc;
			comms.sendMessage(cmsg);
		}
		boastCounter--;
	}
	
	private void siege() throws GameActionException{

		if (!checkEnemiesToAttack()){
			mission = Mission.FIND_RANDOM_FLUX_DEPOSIT;
			return;
		}
		if (combatMode == CombatMode.NONE)
			combatMode = CombatMode.STD;			
		if (Clock.getRoundNum() % 2 == 0){
			feedMinions();
		} else 
			doneFeeding = true;
		if (doneFeeding){
			performArmySpawn();
			performArmySend();
		}
		if (boastCounter <= 0) {
			boastCounter = boastCountdown;
			Comms.CompoundMessage cmsg = comms.new CompoundMessage();
			cmsg.type = combatMode.msgType();				
			cmsg.loc = siegeLoc;
			comms.sendMessage(cmsg);
		}
		boastCounter--;
	}
}
