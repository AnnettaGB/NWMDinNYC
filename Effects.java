/**
 * Disaster ABM in MASON
 * @author Bill Kennedy
 * Aug2020
 */

package disaster;

// Class imports
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomVectorField;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;


/**
* This class provided the effects of a nuclear WMD for our model
*/
public class Effects implements Steppable {
	
	private static final long serialVersionUID = -1113018274619047013L;

	public static ArrayList<Indv> zoneList = new ArrayList<Indv>();	// used to monitor visitors to nWMD effects area
	
	// time & loc of detonation
	private static int  zdet = Parameters.tDetonation;	//2210;//3295;			// units: in minutes for the detonation (3rd day at 10:45am)
	private static Double zLat = Parameters.tLat; 	//40.764290;		// units: decimal degrees (intersection of 6th Ave & 57th Street)
	private static Double zLong = Parameters.tLong;	//-73.977290;	// units: decimal degrees

	public static World world;
	public static int detEventTime = zdet;
		
	// first radius (all inside dead & vaporized
	private static Double z1radius = Parameters.z1radius;
	// second ring (annulus between r1 and r2) 
	private static Double z2radius = Parameters.z2radius;
	// third ring (annulus between r2 and r3)
	private static Double z3radius = Parameters.z3radius;
		
	// list of agents with significant exposures in each zone
	private static ArrayList<Indv> r1Agents = new ArrayList<Indv>();
	private static ArrayList<Indv> r2Agents = new ArrayList<Indv>();
	private static ArrayList<Indv> r3Agents = new ArrayList<Indv>();
    
	// support for calculating distance from ground zero    
	private static GeometryFactory fact = new GeometryFactory();
	private static Point groundZero = Parameters.groundZero;
	
	// list of possible mode change nodes
	public static ArrayList<Node> nodesToGetOnOffRoadNetwork = new ArrayList<Node>();
    
	
    /**==================================================
     *         INIT EFFECTS
     * 
     * initialize (schedule) nWMD event
     * =================================================
    */
	
	/**
	 * Effects Constructor
	 */
    public static void Effects() {
    	System.out.println("Effects>initialization>detonation scheduled for " + zdet);
    }

    /**
     * NWMD Detonation Method
     * @param world
     */
    public static void detonation(World world) {
    	System.out.println("Effects>detonation> ==============nWMD event====================");
    	System.out.println("Effects>detonation>  fatality radius: " + Parameters.R1 
    			+  "m, mortally injured radius: " + Parameters.R2 
    			+ " , and survivable injury radius: " + Parameters.R3 + ".");
    	
    	int killed = 0;
    	
    	// fleeing parameters, degrees per minute
    	double fleeSlowest 	= Parameters.fleeSlowestDegreesPerMin;		// 1 meter per minute  (to be calibrated)
    	double fleeSlow 	= Parameters.fleeSlowDegreesPerMin;			// 5 meters per min
    	double fleeFast 	= Parameters.fleeFastDegreesPerMin;		// 10 meters per min
    	
    	// go through all agents
		for (Indv a : Log.indvList) { // victimList) 
			if (a.dead) { 
				continue; 
			}	// skip dead agents

			// Distance from ground zero
			double dist = a.getLocation().geometry.distance(groundZero);    	

			//====================================
	    	// identify agents within first radius
			if (dist <= z1radius) {
				a.dead = true;
				a.dose = 10;			// not precise, but indicator of death outright from nWMD 100Sv=10,000REM
				a.setHealthStatus(10);	// dead
				r1Agents.add(a);	// add this agent to exposed list
				killed = killed + 1;
				Log.healthCat[(int)a.dose]++;  // ??????
				
				Log.indvDeaths++; // accounting: count death of each agent
				
				// how should we handle wanting to track where the dead bodies are?
				// remove from at work, etc.
				if ( a.atWork ) {
					a.setatWork(false);
					Log.atWorkCount -= 1;
				}
				if (a.atHome ) {
					a.setatHome(false);
					Log.atHomeCount -= 1;
				}
				if ( a.onCommute ) {
					a.setonCommute(false);
					Log.onCommuteCount -= 1;
				}
				
				if (a.getIsFirstResp()) {
					a.setHealthStatus(10);
					Log.firstRespZone1++;
				}

			 } // inside R1
			
			//=====================================
	    	// identify agents within second radius
			if ((dist > z1radius) && (dist <= z2radius)) {
				a.dose = (int) (4+6*((z2radius - dist)/(z2radius-z1radius) * (z2radius - dist)/(z2radius-z1radius)) );   // d=health/d^2
				
				if ((a.dose > 12)||(a.dose < 1)) { 
					System.out.println("Effects>weird dose: in Zone2, dist= " + dist + " dose= " + a.dose); 
					a.dose = 9;
				}
				
				a.setHealthStatus((int)a.dose);
				Log.healthCat[(int)a.dose]++;	// monitor health 
				a.fleeing = true;					// set fleeing
				a.setGoal("flee");
				a.setMoveRateKmPerStep(Spacetime.degToKilometers(fleeSlowest));  // convert degree movement rate to the model's kilometers standard
				System.out.println("Effects>agent movement rate in km: " + a.getMoveRateKmPerStep());
				Log.affectedFleeing ++;			// increase count of fleeing

				// remove previous accounting
				if ( a.atWork ) {
					a.setatWork(false);
					Log.atWorkCount --;
					Log.IDPwork += 1;
				} 
				if (a.onCommute) {
					Log.onCommuteCount --; // changed from ++
				}
				if (a.atHome) {
					a.setatHome(false);
					Log.atHomeCount --;
					Log.IDPhome += 1;
				}
										
				// set goal to walk to
				createFleeingGoalCord(a, world);
		    	
				if (a.getIsFirstResp()) {
					a.setHealthStatus(99);
					Log.firstRespZone2++;
				}

				r2Agents.add(a);	// add this agent to exposed list

			} // between R1 and R2
			
			//====================================
	    	// identify agents within third radius
			if ((dist > z2radius) && (dist <= z3radius)) {
				a.dose = (int) (1+3*( (z3radius - dist)/(z3radius-z2radius) * (z3radius - dist)/(z3radius-z2radius)) );   // d=health/d^2

				if ((a.dose > 12)||(a.dose < 1)) { 
					System.out.println("Effects>weird in Zone3, dist= " + dist + " dose= " + a.dose); 
					a.dose = 3;
				}
				
				a.setHealthStatus((int)a.dose);
				Log.healthCat[(int)a.dose]++;	// monitor health 
				a.fleeing = true;	// set fleeing		** should this be delayed?
				a.setGoal("flee");
				a.setMoveRateKmPerStep(Spacetime.degToKilometers(fleeSlow));	// convert degree movement rate to the model's kilometers standard

				Log.affectedFleeing++;	// count
				Log.popZone3++;

				if ( a.atWork ) {
					a.setatWork(false);
					Log.atWorkCount -= 1;
					Log.IDPwork += 1; 
				}

				// set fleeing goal point
				if ( a.getIsFirstResp())		 { 
					createFirstResponderSearchGoal(a); 	// goals for first responders diff...
					a.setHealthStatus(99);
					Log.firstRespZone3++;
				}
				else
					createFleeingGoalCord(a, world);		// fleeing on foot goal
				
					r3Agents.add(a);	// add this agent to exposed list

			} // between R2 and R3
			
			//========================
			// give first responders outside R3 goal to head toward ground zero
			if (a.getIsFirstResp() && (dist > z3radius) ) {
				setFirstRespNearZoneGoalNode(a);
				a.setHealthStatus(99);
				Log.firstRespZone4++;
			}
			
		}
		
		if (Parameters.Carpool || Parameters.Emergent) {  // note status of agents (i.e. atWork, onCommute, etc.) handled in the agent if statement
			System.out.println("Effects>detonation>impact groups");
	    	// go through all groups
			// groups don't receive radiation doses
			if (Log.grpList.isEmpty()) {
				System.out.println("Effects>detonation>no groups");
			}
			else {				
				for (Group g : Log.grpList) {
					// Distance from ground zero
					double dist = g.getLocation().geometry.distance(groundZero);   
					
			    	// identify groups within first radius
					if (dist <= z1radius) {
						// group did not survive
						g.updateHealth();  // all members of the group died
					}		
					
			    	// identify agents within second and third radii
					if ((dist > z1radius) && (dist <= z2radius)) {
						g.updateHealth();
						g.setGoal("flee");					
						// set goal to walk to
						createFleeingGoalCord(g, world);
						g.setMoveRateKmPerStep(Spacetime.degToKilometers(fleeSlowest));	// convert degree movement rate to the model's kilometers standard
					}
					
			    	// identify agents within second and third radii
					if ((dist > z2radius) && (dist <= z3radius)) {
						g.updateHealth();
						g.setGoal("flee");					
						// set goal to walk to
						createFleeingGoalCord(g, world);
						g.setMoveRateKmPerStep(Spacetime.degToKilometers(fleeSlow));	// convert degree movement rate to the model's kilometers standard
					}
				}
			}
		}
		
		Log.popZone2 = r2Agents.size();
		Log.popZone3 = r3Agents.size();
		
		System.out.println("Effects>detonation> " + r1Agents.size() + " killed instantly, " 
												  + r2Agents.size() + " fataly injured, and " 
												  + r3Agents.size() + " others injured."); // for testing agent health post detonation
		System.out.println("Effects>detonation> health distribution (healthy...dead): " 
												  + Log.healthCat[0] + " "
												  + Log.healthCat[1] + " "
												  + Log.healthCat[2] + " "
												  + Log.healthCat[3] + " "
												  + Log.healthCat[4] + " "
												  + Log.healthCat[5] + " "
												  + Log.healthCat[6] + " "
												  + Log.healthCat[7] + " "
												  + Log.healthCat[8] + " "
												  + Log.healthCat[9] + " "
												  + Log.healthCat[10] + " " );
											  

		// generate list of nodes where agents will change modes of travel, between road network and "on foot"
		genOnOffRoadNodes(world);
	
		// damage to road network edges -- enables agents to detect the road network has been destroyed 
		damageToRdEdges(world);
		
		// remove damaged road network nodes and edges
		damageToRoadNetwork(world);
		
		System.out.println("Effects>detonation> ==============nWMD event end================");
		
    }// end detonation
    
	/**
	 *  Method to create post boom movement goals 
	 */
    //===========================================
    public static void createFleeingGoalCord(Agent b, World world) {
		// first movement goal is to move away the same distance as agent is to ground zero
		// use zLat and zLong as ground zero posit
		double cLat 	= b.getLocation().geometry.getCentroid().getY();
		double cLong	= b.getLocation().geometry.getCentroid().getX();
		double gLat		= cLat + (cLat - zLat);
		double gLong	= cLong + (cLong - zLong);
		
		GeometryFactory fact = new GeometryFactory();
		Coordinate fleeingGoalCoord = new Coordinate(gLong,gLat);
		
		b.setGoalPoint(fleeingGoalCoord);

    }
    
    //================================================ ===
    /**
     * Create first responder goal point
     * @param b
     */
    public static void createFirstResponderSearchGoal(Agent b) {
		// first movement goal is to move away the same distance as agent is to ground zero
		// use zLat and zLong as ground zero posit
		double gLat		= zLat;			// moving toward ground zero
		double gLong	= zLong;  		// moving toward ground zero
	
		GeometryFactory fact = new GeometryFactory();
		Coordinate firstRespInZoneGoalCoord = new Coordinate(gLong,gLat);
		
		b.setGoalPoint(firstRespInZoneGoalCoord);

    }
    
    //===========================================
    /**
     * Set first responder goal node
     * @param b
     */
    public static void setFirstRespNearZoneGoalNode(Agent b) {
		// first movement goal is to move away the same distance as agent is to ground zero
		// use zLat and zLong as ground zero posit
		double cLat 	= b.getLocation().geometry.getCentroid().getY();
		double cLong	= b.getLocation().geometry.getCentroid().getX();
		double gLat		= cLat -(cLat - zLat);		// moving toward ground zero
		double gLong	= cLong - (cLong - zLong);  // moving toward ground zero
	
		GeometryFactory fact = new GeometryFactory();
		Coordinate firstRespNearZoneGoalCoord = new Coordinate(gLong,gLat);
		
		b.setGoalPoint(firstRespNearZoneGoalCoord);

    }
    
   
    /**
     * Test whether agent is in damage area
     * within damage radius, R2
     * @param a
     * @return
     */
    public static boolean inExZone(Agent a) {
    	if ( (a == null) || (a.currentCoord == null) ) return false;
    	double dist = a.getLocation().geometry.distance(groundZero);  
    	if (dist > z2radius) return false;
    	return true;
    }
    
   
    /**
     * Test whether agent is in damage area
     * within damage radius, R3
     * @param a
     * @return
     */
    public static boolean inAnyZone(Agent a) {
    	if ( (a == null) || (a.currentCoord == null) ) return false;
    	double dist = a.getLocation().geometry.distance(groundZero);  
    	if (dist > z3radius) return false;
    	return true;
    }
    

    /**
     * Generate possible mode change nodes
     * @param world
     */
    private static void genOnOffRoadNodes(World world) {
    	// uses world
    	// uses geo factory: fact
    	// uses ground zero point: groundZero
    	// uses array of nodes: modeChgNodes (generated here)
       Coordinate coord = null;
       Point point = null;
       int countNodes = 0;
       int countfound = 0;
       
 	   Iterator<?> nodeIterator = world.roadNetwork.nodeIterator();
       Node node = (Node) nodeIterator.next();

       while (nodeIterator.hasNext()) {
    	   countNodes++;
    	   node = (Node) nodeIterator.next();
    	   coord = node.getCoordinate();
    	   point = fact.createPoint(coord);	   
    	   double dist = groundZero.distance(point);
    	   
    	   if (dist > Parameters.z3radius) {
    		   if (dist < Parameters.z3radius * 1.2) { // 1.2 gets about 300 nodes nearby outside R3 
    			   nodesToGetOnOffRoadNetwork.add(node);
    			   countfound++;
    		   }
    	   }
	   }
   	
    }
    
    /**
     * Damages the road network edges with start/end nodes within the outer radius
     * Found edges have speed limits set to 0, allowing agents to detect a problem with the road
     * segment when the moveRate is set in commute(), carPool() or travelPath() methods
     * @param world
     */
    static void damageToRdEdges(World world) {
        Coordinate coord = null;
        Point point = null;
        int count = 0;
        
        Iterator<?> nodeIterator = world.roadNetwork.nodeIterator();
        
        // Check each node in the roadnetwork
        while (nodeIterator.hasNext())
        {
            Node node = (Node) nodeIterator.next();
            coord = node.getCoordinate();
     	   	point = fact.createPoint(coord);
     	   	  	   	
     	   	double dist = groundZero.distance(point); // find distance of node to groundZero
     	   	
     	   	// if the distance of the node is within Z2, reduce speed on its edges to 0
     	   	// these edges become impassable due to the detonation impact
	    	// identify agents within second radius
			if (dist <= z2radius) {
				
				// loop through outEdges of the node and update speed limit
            	for (Object obj: node.getOutEdges().getEdges()) {
            		// change the directed edge to a non-directed edge for code handling
            		GeomPlanarGraphDirectedEdge dEdge = (GeomPlanarGraphDirectedEdge) obj;
            		GeomPlanarGraphEdge edge = (GeomPlanarGraphEdge) dEdge.getEdge();
            		
            		// If the edge is in range and not yet impacted, 
            		// reduce the speedlimit on the edge to 0, i.e. can't be traveled
            		if  (world.edgesToSpeedLimit.get(edge) != 0 ) {
            			// System.out.println("Effects->detonation->damageToRdEdges->edge: " + edge);
            			world.edgesToSpeedLimit.replace(edge, (double) 0);
            			count += 1;
            		}
            		
            	}   // end for loop
			}
            if ( (dist > z2radius) && (dist <= z3radius) ) { 
            	
            	// loop through outEdges of the node and update speed limit
            	for (Object obj: node.getOutEdges().getEdges()) {
            		// change the directed edge to a non-directed edge for code handling
            		GeomPlanarGraphDirectedEdge dEdge = (GeomPlanarGraphDirectedEdge) obj;
            		GeomPlanarGraphEdge edge = (GeomPlanarGraphEdge) dEdge.getEdge();
            		
            		// If the edge is in Z3 range 
            		// reduce the speedlimit to 10km/hour, i.e. difficult to travel
            		if  (world.edgesToSpeedLimit.get(edge) != 0 ) {
            			world.edgesToSpeedLimit.replace(edge, (10.0));
            			count += 1;
            		}
            		
            	}   // end for loop
            	
            }   // end if statement
            
        }    // end while loop
        
        System.out.println("Effects>detonation>damageToRdEdges>number of edges destroyed: " + count);
    }
    
    /**
     * Modifies road network to reflect event impact
     * All nodes in the event outer radius and their edges are removed from the road network
     * @param world
     */
    // Need to further harm those agents whose homes/work locations are damaged in z3ring?
    static void damageToRoadNetwork(World world) {
        Coordinate coord = null;
        Point point = null;
        ArrayList<Node> damagedNodes = new ArrayList<Node>(); 
        int nodecount = 0;
        int startNumEdges = world.roadNetwork.getEdges().size(); // used for verification
        // rewrite the roadIntersections as a new GeomVectorField usable for finding nearest Nodes
        world.roadIntersections = new GeomVectorField();
        
        Iterator<?> nodenetIterator = world.roadNetwork.nodeIterator();
        
        // Check each node in the roadnetwork
        while (nodenetIterator.hasNext()) {
            Node node = (Node) nodenetIterator.next();
            coord = node.getCoordinate();
     	   	point = fact.createPoint(coord);
     	   	  	   	
     	   	double dist = groundZero.distance(point); // find distance of node to groundZero
     	   	
     	   	// if the distance of the node is within innermost damaged zone, add it to an ArrayList of nodes for removal
            if (dist <= z2radius) { 
            	damagedNodes.add(node);
            }
            if ((dist > z2radius) && (dist <= z3radius)) {
            	// generate a 50/50 chance of the node removal with a random boolean
            	Random random = new Random();
            	if (random.nextBoolean()) {
            		damagedNodes.add(node);
            	}        	
            }           
        }    // end while loop
        
        // Remove all nodes and associated edges in the damagedNodes ArrayList from the roadNetwork
        for (Node n: damagedNodes) {
        	world.roadNetwork.remove(n);
        	nodecount += 1;
        }
        
        // rewrite the roadIntersections GeomVectorField to find nearest Nodes
        Iterator<?> nodeIterator = world.roadNetwork.nodeIterator();
        
        while (nodeIterator.hasNext()) {
        	// Create a GeometryVectorField of points representing road intersections
            Node node = (Node) nodeIterator.next();
            coord = node.getCoordinate();
            point = fact.createPoint(coord);

            world.roadIntersections.addGeometry(new MasonGeometry(point));
        }
      
        System.out.println("Effects>detonation>damageToRoadNetwork>nodes removed: " + nodecount + " edges removed: " + (startNumEdges - world.roadNetwork.getEdges().size()));
        System.out.println("Effect>detonation>damageToRoadNetworks>nodes in rewritten roadIntersections: " + world.roadIntersections.getGeometries().size());

    }
    
    
    // =================================
    /**  
     * 	STEP
     *  update exposure & health status
     *  post detonation
     */
    //=================================
    @Override
	public void step(SimState state)
    {
    	// check fatally exposed agents and evaluate their health
    	World world = (World) state;
    	long steps = world.schedule.getSteps();
		double pdeath = 0;						// placeholder for p(death)
    	
    	// detonate the NWMD
    	if (steps == Parameters.tDetonation) {
    		zLat = Parameters.tLat;
    		zLong = Parameters.tLong;
    		detonation(world);
    	}
    	
    	// after detonation?
    	if (steps > Parameters.tDetonation) {
    		// check fatally exposed agents and evaluate their health    		
			for (Indv a : r2Agents) { //dying Agents)		    		
				if (!a.dead) {	// not dead
					pdeath = Math.random();	// random number used to see they should die this step
					if (pdeath < 0.001)	{	// dies this step
		    			a.dead = true;			// this agent has died
		    			a.setHealthStatus(10);	// dead

		    			Log.indvDeaths++; // accounting
		    			Log.popZone2 --;
		    			Log.affectedFleeing --;
		    			
		    		}  
				} // end test of already dead	    					   				
			}  // loop for agents in zone 2
    		
    		
    		// update those in zone 3
			for (Indv a : r3Agents) { //dying Agents)
				// no new exposures (until fallout calculations)
				
				if (!a.dead) { // not dead
					// dies this step?
					pdeath = Math.random();
					double dose = a.dose;
					if ( ( (dose > 8) && (pdeath < (dose*0.00001)))		// death in 1-2 days
						|| ( (dose <=8) && (pdeath < (dose*0.000005)) ) ) { // death 2-14 days
						
						a.dead = true;			// this index agent has died
						a.setHealthStatus(10);	// dead
						
						// if dead agent was in group, reset the leader
//						if (a.getinGroup()) {
//							Group g = a.getGrp(a.getID(), world);
//							if (g.getSize() > 2) {
//								g.selectSeniorLead();
//							}
//							// if the group now has only one member:
//							else {
//								for (Indv mem: g.idsToMem.values()) {
//									mem.setinGroup(false);
//								}
//								if (g.getGrptype() == "emergent") {
//									Log.emerGrpRemNo += 1;  // count number of remove members from emergent group
//									Log.defunctGrps.add(g.getID());
//									Log.inactvemerGrps += 1;
//								}
//								g.setDefunct(true);
//							}
//						}
						
						Log.indvDeaths++;	// accounting
						Log.popZone3 --;
						Log.affectedFleeing --;

					} 
				}  
			}  // loop of agents in zone 3		
    	} // after boom
    }    


}
