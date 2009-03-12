package Jarek_zerg.astar;

import java.util.*;
import java.util.Map.Entry;

import Jarek_zerg.exceptions.HungryException;
import Jarek_zerg.map.Tile;
import battlecode.common.*;

/**
 * A path finder implementation that uses the AStar heuristic based algorithm
 * to determine a path. 
 * 
 * @author Kevin Glass
 */
public class AStarPathFinder implements PathFinder {
	/** If true then print debug strings */
	private static final boolean DEBUG = false;
	/** The set of nodes that have been searched through */
	private List<Node> closed = new ArrayList<Node>();
	/** The set of nodes that we do not yet consider fully searched */
	private SortedSet<Node> open = new TreeSet<Node>();
	
	/** The map being searched */
	private TileBasedMap map;
	/** The maximum depth of search we're willing to accept before giving up */
	private int maxSearchDistance;
	
	/** The complete set of nodes across the map */
	private Map<MapLocation, Node> nodes;
	private boolean includeTileBlockedPenalty;
	private RobotController myRC;
	
	/**
	 * Create a path finder 
	 * 
	 * @param heuristic The heuristic used to determine the search order of the map
	 * @param map The map to be searched
	 * @param maxSearchDistance The maximum depth we'll search before giving up
	 * @param myRC 
	 * @param allowDiagMovement True if the search should try diagonal movement
	 */
	public AStarPathFinder(TileBasedMap map, int maxSearchDistance, boolean includeTileBlockedPenalty, RobotController rc) {
		this.map = map;
		this.maxSearchDistance = maxSearchDistance;
		this.nodes = new HashMap<MapLocation, Node>();
		this.includeTileBlockedPenalty = includeTileBlockedPenalty;
		this.myRC = rc;
		
		Map<MapLocation, Tile> tiles = map.getTiles();
		
		for (Entry<MapLocation, Tile> entry : tiles.entrySet()) {
			nodes.put(entry.getKey(), new Node(entry.getKey(), entry.getValue()));
		}
	}
	
	/**
	 * @throws HungryException 
	 * @see PathFinder#findPath(Mover, int, int, int, int)
	 */
	public Path findPath(MapLocation sloc, MapLocation tloc) throws HungryException {
		Node snode = nodes.get(sloc);
		Node tnode = nodes.get(tloc);

		// easy first check, if the destination is blocked, we can't get there
		if (!isValidLocation(tnode)) {
			return null;
		}
		
		// initial state for A*. The closed group is empty. Only the starting
		// tile is in the open list and it's cost is zero, i.e. we're already there
		snode.cost = 0;
		snode.depth = 0;
		closed.clear();
		open.clear();
		open.add(snode);
		
		tnode.parent = null;
		
		// while we haven't found the goal and haven't exceeded our max search depth
		int maxDepth = 0;
		Integer count = 0;
		while ((maxDepth < maxSearchDistance) && (open.size() != 0)) {
			checkIfHungry();
			
			// pull out the first node in our open list, this is determined to 
			// be the most likely to be the next step based on our heuristic
			Node current = getFirstInOpen();
			count++;
			if (DEBUG) System.out.println(current.loc.toString()+" "+count.toString()+" "+(current.cost+current.heuristic));
			if (current == tnode) {
				break;
			}
			
			removeFromOpen(current);
			addToClosed(current);
			
			// search through all the neighbours of the current node evaluating
			// them as next steps
			for (Direction dir : Direction.values()) {
				if ((dir == Direction.NONE) || (dir == Direction.OMNI))
					continue;
				MapLocation ploc = current.loc.add(dir);
				
				Node neighbour = nodes.get(ploc);
				if (isValidLocation(neighbour)) {
					// the cost to get to this node is cost the current plus the movement
					// cost to reach this node. Note that the heursitic value is only used
					// in the sorted open list
					float nextStepCost = current.cost + getMovementCost(current.tile, neighbour.tile, dir);
					//map.pathFinderVisited(xp, yp);
					
					// if the new cost we've determined for this node is lower than 
					// it has been previously makes sure the node hasn't been discarded. We've
					// determined that there might have been a better path to get to
					// this node so it needs to be re-evaluated
					if (nextStepCost < neighbour.cost) {
						if (inOpenList(neighbour)) {
							removeFromOpen(neighbour);
						}
						if (inClosedList(neighbour)) {
							removeFromClosed(neighbour);
						}
					}
					
					// if the node hasn't already been processed and discarded then
					// reset it's cost to our current cost and add it as a next possible
					// step (i.e. to the open list)
					if (!inOpenList(neighbour) && !(inClosedList(neighbour))) {
						neighbour.cost = nextStepCost;
						neighbour.heuristic = getHeuristicCost(neighbour.tile, tnode.tile);
						maxDepth = Math.max(maxDepth, neighbour.setParent(current));
						addToOpen(neighbour);
					}
				}
			}
		}

		// since we've got an empty open list or we've run out of search 
		// there was no path. Just return null
		if (tnode.parent == null) {
			return null;
		}
		
		//// path is not accessible
		//if (tnode.cost >= TileBasedMap.BIG_FLOAT)
		//	return null;
		
		if (DEBUG) System.out.println(""+tnode.cost+" "+tnode.depth);
		
		// At this point we've definitely found a path so we can uses the parent
		// references of the nodes to find out way from the target location back
		// to the start recording the nodes on the way.
		Path path = new Path();
		Node target = tnode;
		while (target != snode) {
			path.prependStep(target.loc);
			target = target.parent;
		}
		path.prependStep(sloc);
		
		// thats it, we have our path 
		return path;
	}

	/**
	 * Get the first element from the open list. This is the next
	 * one to be searched.
	 * 
	 * @return The first element in the open list
	 */
	protected Node getFirstInOpen() {
		return open.first();
	}
	
	/**
	 * Add a node to the open list
	 * 
	 * @param node The node to be added to the open list
	 */
	protected void addToOpen(Node node) {
		open.add(node);
	}
	
	/**
	 * Check if a node is in the open list
	 * 
	 * @param node The node to check for
	 * @return True if the node given is in the open list
	 */
	protected boolean inOpenList(Node node) {
		return open.contains(node);
	}
	
	/**
	 * Remove a node from the open list
	 * 
	 * @param node The node to remove from the open list
	 */
	protected void removeFromOpen(Node node) {
		open.remove(node);
	}
	
	/**
	 * Add a node to the closed list
	 * 
	 * @param node The node to add to the closed list
	 */
	protected void addToClosed(Node node) {
		closed.add(node);
	}
	
	/**
	 * Check if the node supplied is in the closed list
	 * 
	 * @param node The node to search for
	 * @return True if the node specified is in the closed list
	 */
	protected boolean inClosedList(Node node) {
		return closed.contains(node);
	}
	
	/**
	 * Remove a node from the closed list
	 * 
	 * @param node The node to remove from the closed list
	 */
	protected void removeFromClosed(Node node) {
		closed.remove(node);
	}
	
	/**
	 * Check if a given location is valid for the supplied mover
	 * 
	 * @param mover The mover that would hold a given location
	 * @param sx The starting x coordinate
	 * @param sy The starting y coordinate
	 * @param x The x coordinate of the location to check
	 * @param y The y coordinate of the location to check
	 * @return True if the location is valid for the given mover
	 */
	protected boolean isValidLocation(Node node) {
		//if (loc.equals(sloc))
		//	return true;
		//else
		if (node == null)
			return false;
		else
			return !map.blocked(node.tile);
	}
	
	/**
	 * Get the cost to move through a given location
	 * 
	 * @param mover The entity that is being moved
	 * @param sx The x coordinate of the tile whose cost is being determined
	 * @param sy The y coordiante of the tile whose cost is being determined
	 * @param tx The x coordinate of the target location
	 * @param ty The y coordinate of the target location
	 * @return The cost of movement through the given tile
	 */
	private float getMovementCost(Tile stile, Tile ttile, Direction dir) {
		return map.getCost(stile, ttile, dir, this.includeTileBlockedPenalty);
	}
	
	/**
	 * Get the heuristic cost for the given location. This determines in which 
	 * order the locations are processed.
	 * 
	 * @param mover The entity that is being moved
	 * @param x The x coordinate of the tile whose cost is being determined
	 * @param y The y coordiante of the tile whose cost is being determined
	 * @param tx The x coordinate of the target location
	 * @param ty The y coordinate of the target location
	 * @return The heuristic cost assigned to the tile
	 */
	public float getHeuristicCost(Tile fromTile, Tile toTile) {
		return map.getHeuristicCost(fromTile, toTile);
	}

	/**
	 * Tests if robots energon level is low. If that's the case, then throw HungryException.
	 * @throws HungryException 
	 */
	private void checkIfHungry() throws HungryException{
		if (myRC.getEnergonLevel() * 2.5 < myRC.getMaxEnergonLevel()) {
			throw new HungryException();
		}
	}
	
	
	/**
	 * A single node in the search graph
	 */
	private class Node implements Comparable<Node> {
		/** The x coordinate of the node */
		private MapLocation loc;
		/** The path cost for this node */
		private float cost;
		/** The parent of this node, how we reached it in the search */
		private Node parent;
		/** The heuristic cost of this node */
		private float heuristic;
		/** The search depth of this node */
		private int depth;
		private Tile tile;
		
		/**
		 * Create a new node
		 * @param tile 
		 * 
		 * @param x The x coordinate of the node
		 * @param y The y coordinate of the node
		 */
		public Node(MapLocation loc, Tile tile) {
			this.loc = loc;
			this.tile = tile;
		}
		
		/**
		 * Set the parent of this node
		 * 
		 * @param parent The parent node which lead us to this node
		 * @return The depth we have no reached in searching
		 */
		public int setParent(Node parent) {
			depth = parent.depth + 1;
			this.parent = parent;
			
			return depth;
		}
		
		/**
		 * @see Comparable#compareTo(Object)
		 */
		public int compareTo(Node o) {
			float f = heuristic + cost;
			float of = o.heuristic + o.cost;
			
			if (f < of) {
				return -1;
			} else if (f > of) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}
