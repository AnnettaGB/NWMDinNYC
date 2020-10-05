/**
 * Disaster ABM in MASON
 * @author Annetta Burger
 * Aug2020
 */

package disaster;

// Class imports
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomVectorField;

import sim.util.Bag;
import sim.util.geo.GeomPlanarGraph;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;
import java.util.ArrayList;
import java.util.Iterator;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.Node;


// Agent class 
/**
 *  Our simple agent from the GridLock GeoMASON example.  The agent travels a directed graph 
 *  roadnetwork using an AStar Algorithm
 *  This superclass of human agents is the parent to
 *  Indv and Group subclasses
 */
@SuppressWarnings("restriction")
public class Agent extends SimState implements Steppable {

    public static final long serialVersionUID = -1113018274619047013L;
    
	// internal version of the world, i.e. how the agent perceives it
	// now don't need to pass the world in all Indv methods
	// updates to the individual's world occur internal to the class methods
	World state; 
	
	private String agentID = "";
	
    public int healthStatus = -1;		// agent's health, -1 = not set
	
	// Goals
	private String goal;  		// varies between "commute", "findshelter", "shelter", "flee"
	private Coordinate goalPoint;    // used to travel to a point not on a network
	private Node goalNode;		// used to set network paths that are not commutes
	public ArrayList<Node> multiGoalNodes = new ArrayList<Node>();  // list of goal nodes used for carpools and other multilocation paths
	
	// GIS, road network, and Movement Data
	private String startID = "";  // homeID
	private String endID = "";  // workID
	private String startRdID = "";  // homeRdID
	private String endRdID = "";  // workRdID
	private Node startNode;  // homeNode
	private Node endNode;  // workNode
	
    // point that denotes agent's position
	//  private Point location;
	private MasonGeometry location;
    Coordinate currentCoord = null;
    Coordinate lastCoord1 = null;
    Coordinate lastCoord2 = null;
	
	// Used by agent to walk along line segment
    double distMoved = 0.0;  // tracks the distance an agent travels on the network each step -- called in progress
	protected LengthIndexedLine segment = null;
	double startIndex = 0.0; // start position of current line
	double endIndex = 0.0; // end position of current line
	double currentIndex = 0.0; // current location along line
		
	// Edge handling
	GeomPlanarGraphEdge currentEdge = null;
	int linkDirection = 1;
	double speed = 0; // used for network travel
	boolean pathset = false; // whether agent has a path to travel
	ArrayList<GeomPlanarGraphDirectedEdge> currentPath = // path agent travels
			new ArrayList<GeomPlanarGraphDirectedEdge>();
	int indexOnPath = 0;  // index of which edge on the path
	int pathDirection = 1;
	boolean reachedDestination = false;
	PointMoveTo pointMoveTo = new PointMoveTo();
	int pathLength = 0;
	int disconnectedPaths = 0;

	// Path handling
	ArrayList<ArrayList<GeomPlanarGraphDirectedEdge>> multiPath = // list of paths
			new ArrayList<ArrayList<GeomPlanarGraphDirectedEdge>>();
	int multipathDirection = 1; // used for multipath travel
	int multipathIndex = 0; // index of multiPath; tracks which path agent is on
	boolean reachedFinalDestination = false; // used for multipath travel

	// Non-network path movement
    private double moveRateKmPerStep = 0.0;  // in units kilometers/step (minute)
	GeometryFactory fact = new GeometryFactory();	// used to create points for movement
	// methods using these attributes are Indv, and Group classes for the reroute, gotoDetour and Detour methods
	// a basic reroute method for the parent Agent class still needs to be written
	boolean needReroute = false; // used to indicate the agent needs to find a new path
	boolean haveDetour = false; // used to indicate the agent has a detour
	boolean onDetour; // agent is detouring to destination
	
	
    /** 
	 *	Agent Getters and Setters
     */
    public void setHealthStatus(int x) 	{ this.healthStatus = x; }
	public void setGoal(String x)		{ this.goal = x; }
	public void setGoalNode(Node x)		{ this.goalNode = x; }
	public void setGoalPoint(Coordinate x) { this.goalPoint = x; }
	public void setLocation(MasonGeometry x) { this.location = x; }
	public void setSegment(LengthIndexedLine x) { this.segment = x; }
    public void setID(String x)			{ this.agentID = x; }
    public void setStartID(String x)	{ this.startID = x; }
    public void setEndID(String x)		{ this.endID = x; }
    public void setStartRdID(String x)	{ this.startRdID = x; }
    public void setEndRdID(String x)	{ this.endRdID = x; }
    public void setStartNode(Node x)	{ this.startNode = x; }
    public void setEndNode(Node x)		{ this.endNode = x; }  
    public void setPathLength(int x)	{ this.pathLength = x; }
    public void setMoveRateKmPerStep(double x) { this.moveRateKmPerStep = x; }
    public void setneedReroute(boolean x) { this.needReroute = x; } // triggered when agent can't travel path/edge/segment
    public void sethaveDetour(boolean x)  { this.haveDetour = x; }
    public void setonDetour(boolean x)	{ this.onDetour = x; }

    public int getHealthStatus()		{ return this.healthStatus; }
    public String getGoal()				{ return this.goal; }
    public Node getGoalNode()			{ return this.goalNode; } // used for temp nonroutine goals, e.g. not the homeNode
    public Coordinate getGoalPoint()	{ return this.goalPoint; }
    public MasonGeometry getLocation()	{ return this.location; }
    public LengthIndexedLine getSegment()	{ return this.segment; }
    public String getID()     			{ return this.agentID; }
    public String getStartID() 			{ return this.startID; }
    public String getEndID() 			{ return this.endID; }
    public String getStartRdID()		{ return this.startRdID; }
    public String getEndRdID()			{ return this.endRdID; } 
    public Node getStartNode()			{ return this.startNode; }
    public Node getEndNode()			{ return this.endNode; }
    public int getPathLength()			{ return this.pathLength; }
    public double getMoveRateKmPerStep()		{ return this.moveRateKmPerStep; }
    public boolean getneedReroute()		{ return this.needReroute; }
    public boolean gethaveDetour()		{ return this.haveDetour; }
    public boolean getonDetour()		{ return this.onDetour; }
 
    
	/**
	 * Basic Constructor Method
	 * @param world
	 * @param seed
	 */
    public Agent(World world, long seed) {
    		super(seed);
    		state = world;
    }
    
    
    /** 
     * In basic agent class, agent step moves the agent 
     * with random direction coordinate values
     * Called every tick by the scheduler
     * @param world
     */
    @Override
	public void step(SimState world) {  // dummy step do we need this in the superclass?

 	   state = (World) world; // update agent's state every step
 	   long currentStep = state.schedule.getSteps();
 	   int simTime = Spacetime.time24(currentStep);
 	   System.out.println("Agent->step->" + currentStep + " time: " + simTime);
 	   
 	   randomDirection();
 	      
    }
    
  
    /**
     * Picks a random coordinate, move towards it
     * Once arrived at coordinate, pick a new coordinate
     */
    public void randomDirection() {
    	System.out.println("Agent>randomWalk>current coord: " + currentCoord);
    	
    	if ( getGoalPoint() == null ) {
    		// pick a new goal coordinate
    		double xcoord = random.nextDouble() * state.censusTracts.getWidth();
    		double ycoord = random.nextDouble() * state.censusTracts.getHeight(); 		
    		setGoalPoint(new Coordinate(xcoord,ycoord));		   		
    	}
    	
    	else if (!moveToCoord(getGoalPoint())) {  // agent either moves or returns false
    	}
    	
    	else {
    		// set new GoalPoint
    		double xcoord = random.nextDouble() * state.censusTracts.getWidth();
    		double ycoord = random.nextDouble() * state.censusTracts.getHeight(); 		
    		setGoalPoint(new Coordinate(xcoord,ycoord));
    	}
    	
    }
    
    
    /**
     * Create and set a multipath with multiple goal nodes
     * @return
     */
    public boolean setMultiPath() {
    	ArrayList<GeomPlanarGraphDirectedEdge> path = // path agent travels; local variable
    			new ArrayList<GeomPlanarGraphDirectedEdge>();
 	   	this.lastCoord1 = this.currentCoord;
 	   	this.lastCoord2 = this.currentCoord;
    		
    	Iterator i = multiGoalNodes.iterator();
    	int index = 0; // used to index through the multiple goal nodes
    	while (i.hasNext() && index < (multiGoalNodes.size()-1) ) {
    		startNode = multiGoalNodes.get(index);
    		index += 1;
    		endNode = multiGoalNodes.get(index);
    		path = findNewAStarPath(startNode, endNode);
    		multiPath.add(path);
    		ckPathNodes(path);

    	}
    	
    	if (multiPath == null) {
	   		return false;
    	}
    	else { 
    		beginMultiPath(multiPath);
        	return true;
    	}
    	
    }

    
    /**
     * Set agent at the beginning of its multipath
     * @param multiPath
     * @return
     */
    public boolean beginMultiPath(ArrayList<ArrayList<GeomPlanarGraphDirectedEdge>> multiPath) {
        // if the multipath works, lay it in
    	// set destination booleans
    	reachedDestination = false;
    	reachedFinalDestination = false;
    	multipathIndex = 1; // reinitialize onto the first path
    	//System.out.println("Agent->beginMultiPath->size: " + multiPath.size());
    	//System.out.println("Agent->beginMultiPath->first index: " + multiPath.get(0));
        if (multiPath != null && multiPath.size() > 0)
        {  		
 	        // set up how to travel on first path
        	currentPath = multiPath.get(0);
 	        beginPath(currentPath);
 	        //System.out.println("Agent->beginMultiPath->have path: " + currentPath.size());
 	        ckPathNodes(currentPath);
 	        return true;  	   		
        }
        else {
        	//System.out.println("Agent->beginMultiPath->Not a valid path");
     	   	return false;
        }
    }
    
    
    /**
     * Set and begin agent on next path in a multipath
     * @return
     */
    public boolean nextPath() {
    	// if haven't reached last path & path direction is positive, 
    	// agentID.begin next path in the array
 	   reachedDestination = false;
    	if (multipathIndex < multiPath.size() && pathDirection == 1) {
        	multipathIndex += 1;
        	ckPathNodes(currentPath);
        	currentPath = multiPath.get(multipathIndex-1);
    		beginPath(currentPath);
    		ckPathNodes(currentPath);
    		return true; 
    	}
    	// if haven't reached the first path & path direction is negative, begin next path backwards in the array
    	else if (multipathIndex > 0 && pathDirection == -1) {
    		multipathIndex -= 1;
    		ckPathNodes(currentPath);
        	currentPath = multiPath.get(multipathIndex);
    		beginPath(currentPath);
    		ckPathNodes(currentPath);
    		return true;
    	}
    	// otherwise report agent is at the final destination
    	else {
    		return false;
    	}
    }
    
    
    /** 
    * Set a paths of an Agent: find an A* path to work!
    * 	@param state
    * 	@return whether or not the agent successfully found a path to work
    */
   public boolean setPath()
   { 
	   // Set schedule and paths
	   // Create path and schedule
	   this.lastCoord1 = this.currentCoord;
	   this.lastCoord2 = this.currentCoord;
	   if (startNode != endNode) { // set path at start and leaving start location
	       currentPath = findNewAStarPath(startNode, endNode);
	       
	       if ( currentPath == null )  {
	    	   		return false;
	       }
	       else {  	   
	    	   		beginPath(currentPath);
	    	   		return true;
	       }
	       
	   }
	   else { // set the stay-at-home agents with no path
		   return false;
	   }

   }
    
   
   /**	
    * Plots a path between two nodes in the road network 
    * @param world
    * @param starting node
    * @param goal node
    * @return path
    */
   public ArrayList<GeomPlanarGraphDirectedEdge> findNewAStarPath(Node startNode, Node endNode)
   {   
       // get the home and work Nodes with which this Agent is associated
       Node currentJunction = startNode;
       Node destinationJunction = endNode;

       if (currentJunction == null)
       {
           return null; // just a check
       }
       // find the appropriate A* path between them
       AStar pathfinder = new AStar();
       ArrayList<GeomPlanarGraphDirectedEdge> path =
           pathfinder.astarPath(currentJunction, destinationJunction);

       // if the path works, return it
       if (path != null && path.size() > 0) {
    	   		setPathLength(path.size());	
    	   		return path;
       }
       else {
    	   		return null;
       }

   }

   
   /**
    * Set up agent to begin agent traveling on a path
    * @param World
    * @param ArrayList<GeomPlanarGraphDirectedEdge>
    * @return Boolean True if there is a valid path and the agent is set up
    */
   public boolean beginPath (ArrayList<GeomPlanarGraphDirectedEdge> path) {
       // if the path works, lay it in
       if (path != null && path.size() > 0)
       {  		
    	   if (multipathDirection > 0) {
	        // set up how to traverse this first link
    		indexOnPath = 0;
	        GeomPlanarGraphEdge edge =
	            (GeomPlanarGraphEdge) path.get(0).getEdge();
	           
	        setupEdge(edge);
	
	        // update the current position for this link
	        updatePosition(segment.extractPoint(currentIndex));
	                 
	        return true;
    	   }
    	   else {
   	        // set up how to traverse this first link
    		indexOnPath = path.size()-1;
   	        GeomPlanarGraphEdge edge =
   	            (GeomPlanarGraphEdge) path.get(path.size()-1).getEdge();
   	           
   	        setupEdge(edge);
   	
   	        // update the current position for this link
   	        updatePosition(segment.extractPoint(currentIndex));
   	              
   	        return true;  
    	   }
    	   
       }
       else {
    	   return false;
       }
   }
   
   
   /**
    * Method moves the agent towards a goal coordinate
    * Returns false, if the agent doesn't move and is at the goal coordinate
    * @param coord
    * @return
    */
   boolean moveToCoord (Coordinate coord) {
	   // Basic movement method, moves agent towards a coordinate at its moveRate
	   // If already at the coordinate return false
	   if (currentCoord.equals2D(coord)) {
		   return false;
	   }
	   else {
		   // move agent
		   // find x/y of the from and to coordinates
		   double currLat = currentCoord.y;
		   double currLong = currentCoord.x;
		   
		   // find the distances to the goal coordinate x/y and the move coordinate 
		   double latDist = coord.y - currentCoord.y;
		   double longDist = coord.x - currentCoord.x;

		   double distToCoord = Math.sqrt( (latDist*latDist) + (longDist*longDist) );
		   double speed = Spacetime.kilometersToDegrees(getMoveRateKmPerStep());
		   
		   if (distToCoord < speed) {
			   currentCoord = coord;
			   updatePosition(currentCoord); 
		   }
		   
		   else {
			   // calculate the movement coordinate for long and lat for this step based on the MovementRate
			   // movement is relative to the lat/long movement, 
			   // i.e. using Euclidean Distance for each new coordinate x/y
			   double moveCoordLong = ( speed * longDist / distToCoord ) + currLong;
			   double moveCoordLat = ( speed * latDist / distToCoord ) + currLat;
		   
			   Coordinate tempCoord = new Coordinate(moveCoordLong, moveCoordLat);
			   currentCoord = tempCoord;
			   updatePosition(currentCoord);
		   }
		   
		   return true;
	   }
   }

   
   /**
    * Move agent on its multipath
    */
   void travelMultiPath() {
	   int endPath = multiPath.size();
	   
	   // if at the end of a path
	   if (reachedDestination) {
		   // if at the end of the multiPath, set reachedFinalDestination to true
		   if (multipathIndex == endPath || multipathIndex == 0) {
			   reachedFinalDestination = true;
		   }
		   else {
			   nextPath();		   
		   }
	   }
	   
	   // if haven't reached a path destination, travel the current path
	   else {
		   travelPath();
	   }

   }
   
   
   /** 	Move agent on its AStar path
    * 	@param world
    */
   void travelPath()	{
       long currentStep = this.state.schedule.getSteps();

	   // get traffic data
	   double edgeTrafficsize = this.state.edgeTraffic.get(currentEdge).size();
	   double edgeDistance = this.state.edgesToDistance.get(currentEdge);
	   double speedLimit = this.state.edgesToSpeedLimit.get(currentEdge);
	   double moveRate = speedLimit / 60;  // (n kilometers / 60 steps) to get km/step
	   
	   // if agent gets stuck because edge is impassable
	   // assumes agent only gets stuck with moveRate = 0 one time when the roadnetwork is first destroyed
	   if (moveRate == 0) {
		   setHealthStatus(80); // changes color for tracking those agents stuck due to damaged edges
		   setneedReroute(true);
		   return; // break out of travelPath() if edge is impassable
	   }
	   
       // check that we've been placed on an Edge      
       if (segment == null) {
           return;
       } // check that we haven't already reached our destination
       else if (reachedDestination) {
    	   //System.out.println("Agent->step->travelPath->reached path destination: " + getID() + " " + currentCoord);
    	   return;
       }

       speed = progress(edgeTrafficsize, edgeDistance, moveRate);
       currentIndex += speed;

       // Coordinate currentPos;
       // check to see if the progress has taken the current index beyond its goal
       // given the direction of movement. If so, proceed to the next edge
       if (linkDirection == 1 && currentIndex > endIndex) {
           currentCoord = segment.extractPoint(endIndex);
           transitionToNextEdge(edgeTrafficsize, edgeDistance, currentIndex - endIndex);
       } else if (linkDirection == -1 && currentIndex < startIndex) {
           	currentCoord = segment.extractPoint(startIndex);
           	transitionToNextEdge(edgeTrafficsize, edgeDistance, startIndex - currentIndex);
       } else { // just update the position!
    	   currentCoord = segment.extractPoint(currentIndex);
       }
       updatePosition(currentCoord);  // updates Group location

   }
   
   
   /**
 	* Progress the agent over its path based on a moveRate and traffic congestion
    * @param edgeTraffic
    * @param distance
    * @param moveRate
    * @return
    */
   double progress(double edgeTraffic, double distance, double moveRate) {
       double edgeLength = distance;  // change to meters
       double traffic = edgeTraffic;
       
       // factor in traffic density on an edge
       double factor = 1000 * edgeLength / (traffic*5); // changed from traffic*5
       factor = Math.min(1, factor);
       double gridprog = moveRate * linkDirection * factor; // moves slower as factor increases
	   if ( distMoved >= 0)  {
		   distMoved += gridprog;
	   }
       return gridprog;
   }
 

   /**
    * Transition to the next edge in the path
    * @param World
    * @param trafficsize
    * @param distance is the length of the edge to be traveled (km)
    * @param residualMove the amount of distance the agent can still travel
    * this turn
    * 
    */
   void transitionToNextEdge(double trafficsize, double distance, double residualMove)
   {
	   long currentStep = this.state.schedule.getSteps();
	   
       // update the counter for where the index on the path is
       indexOnPath += pathDirection;  // moves the index up or down depending on pathDirection (-1 or 1)

       // check to make sure the Agent has not reached the end
       // of the path already
       if ((pathDirection > 0 && indexOnPath >= currentPath.size())  // check to see if the index >= the # of edges on the path
           || (pathDirection < 0 && indexOnPath < 0)) { // depends on where you're going!
    	   
           reachedDestination = true;
           
           // if the agent does not have a multipath, simply snap to the end or start node coordinates
           // (remember paths do not end at a Goal node ... only at its nearest end of a line segment)
	       if (multiPath ==  null) {    
           	   // snap agents to Node coordinates
	           // confirm we are at work (end of path) or at home (start of path)
	           if ( indexOnPath >= currentPath.size() ) {	// at the end of the path 
	        	   currentCoord = endNode.getCoordinate();
	    	   	   	   
	    	   	   updatePosition(currentCoord);
	    	   	   
	           }
	           else if ( indexOnPath == 0 ) {
	        	   currentCoord = startNode.getCoordinate();
   	   	   
	    	   	   updatePosition(currentCoord);
	    	   	   
	           }
	       }
	       
           // if on multipath, snap to the appropriate goalNode 
           // (remember paths do not end at a Goal node ... only at its nearest end of a line segment)
	       if (multiPath.size() > 1) {       
	    	   Coordinate snapCoord;
	           // identify the path goal node from the arraylist of GoalNodes
	    	   if (multipathDirection < 0) {
	    		   // get the start goal if the link direction is -1
		    	   snapCoord = multiGoalNodes.get(multipathIndex).getCoordinate();
	    	   }
	    	   else {
	    		   snapCoord = multiGoalNodes.get(multipathIndex).getCoordinate();
	    	   }

	    	   currentCoord = snapCoord;
	    	   
	    	   updatePosition(currentCoord); 	   	           
	       }	           
	              
	       indexOnPath -= pathDirection; // make sure index is correct    
	       return;

       }
       
       speed = progress(trafficsize, distance, residualMove);
       currentIndex += speed;
       
       // get and move to the next edge in the path
       GeomPlanarGraphEdge edge =
           (GeomPlanarGraphEdge) currentPath.get(indexOnPath).getEdge();
       
	   // if agent gets stuck because edge is impassable check whether to reroute and return
	   // assumes agent only gets stuck with moveRate = 0 one time when the roadnetwork is first destroyed
       if (edge == null ) {
		   setHealthStatus(80); // changes color for tracking those agents stuck due to damaged edges
		   setneedReroute(true);

		   return;
       }
       
       setupEdge(edge);

       // check to see if the progress has taken the current index beyond its goal
       // given the direction of movement. If so, proceed to the next edge
       // nested transitionToNextEdge loops the movement until agent reaches the end
       if (linkDirection == 1 && currentIndex > endIndex) {
           transitionToNextEdge(trafficsize, distance, currentIndex - endIndex);
           
       } else if (linkDirection == -1 && currentIndex < startIndex) {

           transitionToNextEdge(trafficsize, distance, startIndex - currentIndex);
       }
       
       else  {
    	   // just update the position
	          currentCoord = segment.extractPoint(currentIndex);
	          updatePosition(currentCoord);
       }

   }
      
   
	//===============================	
	//
	//	AGENT HELPER METHODS
	//
	//===============================	
   
   /**
    * Find and return nearest road network node
    * @param point
    * @param nodefield
    * @param roadNet
    * @param startDistance
    * @return
    */
   public static Node findNearestNode(MasonGeometry point, GeomVectorField nodefield, GeomPlanarGraph roadNet, double startDistance) {
	   Bag candidates = nodefield.getGeometries(); //world.roadIntersections.getGeometries();
	   double dist = startDistance;
	   while (true) {
		   // simply increase or decrease distance to find an appropriate number of candidates
		   candidates = nodefield.getObjectsWithinDistance(point, dist);  //world.roadIntersections.getObjectsWithinDistance(point, dist);
		   if (candidates.size() == 0) {
			   dist *= 10;
		   }
		   else if (candidates.size() > 3) {
			   dist *= 0.5;			   
		   }
		   else { break; }	   
	   }
	   
	   double minDist = Double.MAX_VALUE;
	   MasonGeometry nearest = null;
	   // refinement step: now it's time to find the nearest among candidates
	   for (Object ele : candidates) {
		   MasonGeometry geo = (MasonGeometry) ele;
		   double tmp = geo.geometry.distance(point.geometry);
		   if (minDist > tmp) {
			   nearest = geo;
			   minDist = tmp;
		   }
	   }
	   
	   Bag nearestNodes = nodefield.getCoveredObjects(nearest); 
	   MasonGeometry nearestObj = (MasonGeometry) nearestNodes.get(0);
	   Coordinate nearestNodeCoord = nearestObj.geometry.getCoordinate();
	   
	   Node nearestNode = null; 
	   
       Iterator<?> nodeIterator = roadNet.nodeIterator();
       
       while (nodeIterator.hasNext())
       {
       	// Create a GeometryVectorField of points representing road intersections
           Node node = (Node) nodeIterator.next();
           Coordinate coord = node.getCoordinate();
           if (nearestNodeCoord == coord) {
        	   nearestNode = node;
           }
       }

       return nearestNode;
   }
   
   
   /**
    * Calculates the length of a path in kilometers to get agent commuting distance
    * @param path
    * @return distance in kilometers
    */
   public double getpathDistance (ArrayList<GeomPlanarGraphDirectedEdge> path) {
	   
  		double distance = 0;

   		for (GeomPlanarGraphDirectedEdge directedEdge: path) {
   			double x =  Spacetime.degToKilometers(AStar.length(directedEdge));
   			distance = distance + x;
   		}
   		
   		return distance;	
   		
   }
   
   
   /**
    * Method to quickly get the goal node of a path regardless of link direction
    * @param path
    * @return GoalNode
    */
   public Node getGoalNode (ArrayList<GeomPlanarGraphDirectedEdge> path) {
	   ;
       if (path.size() == 1) {
	   		Node lastNode = path.get(0).getToNode();
	   		
	   		return lastNode;
       }
       else if (path.size() > 1) {
	   		   int pathIndex = path.size() - 1;
	   		   Node lastNode = path.get(pathIndex).getToNode();

	   		   return lastNode;
       }
       else if (path.size() <= 0) {

    	   		return null;
       }
	   else return null;
  }
   
   
   /**
    * Method to quickly get the start node of a path in a multipath
    * @param path
    * @return StartNode
    */
   public Node getPathStartNode (ArrayList<GeomPlanarGraphDirectedEdge> path) {
       if (path.size() >= 1) {
	   		Node firstNode = path.get(0).getFromNode();

	   		return firstNode;
	   		
       }
       
	   else return null;
       
   }
   
   
   /**
    * Method to quickly get the end node of a path in a multipath
    * @param path
    * @return EndNode
    */
   public Node getPathEndNode (ArrayList<GeomPlanarGraphDirectedEdge> path) {
	   int pathSize = path.size() - 1;
	   Node lastNode = null;
       if (path != null) {
	   		lastNode = path.get(pathSize).getToNode();
       }

	   return lastNode;
   }
   

   /** 
    * Sets the Agent up to proceed along an Edge
    * @param edge the GeomPlanarGraphEdge to traverse next
    */
   void setupEdge(GeomPlanarGraphEdge edge)
   {
       // clean up on old edge -- take agent off the edge
       if (currentEdge != null) {
           ArrayList<Agent> traffic = state.edgeTraffic.get(currentEdge);
           traffic.remove(this);
       }
       
       currentEdge = edge;
       
       // update new edge traffic
       if (this.state.edgeTraffic.get(currentEdge) == null) {
           this.state.edgeTraffic.put(currentEdge, new ArrayList<Agent>());
       }

       this.state.edgeTraffic.get(currentEdge).add(this);

       // set up the new segment and index info
       LineString line = null;
       // post-impact some edges are destroyed, agents needs a reroute
       // if there is no line, reroute, else get the line
       if (edge.getLine() == null) {
    	   setneedReroute(true);
    	   
    	   return;
       }
       else {
    	   line = edge.getLine();
       }
       
       segment = new LengthIndexedLine(line);
       startIndex = Spacetime.degToKilometers(segment.getStartIndex());
       endIndex = Spacetime.degToKilometers(segment.getEndIndex());
       linkDirection = 1;

       // check to ensure that Agent is moving in the right direction
       // then set currentIndex to start of path 
       // and set the link direction
       double distanceToStart = line.getStartPoint().distance(location.geometry),
           distanceToEnd = line.getEndPoint().distance(location.geometry);

       if (distanceToStart <= distanceToEnd) { // closer to start
           currentIndex = startIndex;
           linkDirection = 1;
       } else if (distanceToEnd < distanceToStart) { // closer to end
           currentIndex = endIndex;
           linkDirection = -1;
       }

   }


   /** 	
    * move the agent to the given coordinates 
    * @param world 
    * @param agent coordinate
    */
   public void updatePosition(Coordinate c)  {
	   this.lastCoord2 = this.lastCoord1;
	   this.lastCoord1 = this.currentCoord; // used to check movement

       pointMoveTo.setCoordinate(c);
       location.geometry.apply(pointMoveTo);
       
       // Courtesy of Joonseok Kim:
       // geometryChanged() is added to fix display bug...it ensures the agent position is updated properly
       location.geometry.geometryChanged(); 

   }


   /** return geometry representing agent location */
   public MasonGeometry getGeometry() {
       return location;
   }
   
   
	//==============================================
	//
	// 		VERIFICATION METHODS
	//
	//==============================================
  
   /**
    * Returns the first and last node coordinates of a given path
    * @param path
    */
   public void ckPathNodes(ArrayList<GeomPlanarGraphDirectedEdge> path) {
	   int lastIndex = path.size() - 1;
   }

   /**
    * Returns true if the path Nodes are full connected with its indexed edges
    * @param path
    * @return
    */
   public boolean ckPath(ArrayList<GeomPlanarGraphDirectedEdge> path) {
	   int lastIndex = path.size() - 1;
	   int currentIndex = 0;
	   boolean goodPath = true;
	   
	   while (currentIndex < lastIndex) {
		   GeomPlanarGraphDirectedEdge edge = path.get(currentIndex);
		   GeomPlanarGraphDirectedEdge nextedge = path.get(currentIndex + 1);
		   Node teststartNode = edge.getFromNode();
		   Node testendNode = edge.getToNode();
		   Node nextStartNode = nextedge.getFromNode();
		   Node nextEndNode = nextedge.getToNode();
		   
		   if ( endNode != nextStartNode ) {
			   goodPath = false;
			   System.out.println("Agent>ckPath>bad");
			   System.out.println("Agent>line1: " + teststartNode.getCoordinate() + " " + testendNode.getCoordinate());
			   System.out.println("Agent>line2: " + nextStartNode.getCoordinate() + " " + nextEndNode.getCoordinate());
			   return goodPath;
		   }
		   currentIndex += 1;
	   }
	   
	   return goodPath;
	   
   }
   
}
