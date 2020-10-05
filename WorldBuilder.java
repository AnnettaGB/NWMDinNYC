/**
*  Disaster ABM in MASON
*  @author Annetta Burger
*  Aug2020
 */


package disaster;


/**
 * Class to build the basic simulation environment
 * Builds the physical infrastructure into a set of grids representing the environment
 * The set of grids includes one for the social -- individual agents in the population
 */

// Class imports
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.planargraph.Node;

import sim.field.geo.GeomGridField;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Random;


public class WorldBuilder {

	private static final long serialVersionUID = 1;
	
	static double dis = 0.1;
	
	/**
	 * Reporting variables to confirm input data
	 */
	private static int nullWrkIDs = 0;
	private static int nulldcWrkIDs = 0;
	private static int nullschlWrkIDs = 0;
	private static int nullhmWrkIDs = 0;
	private static int outofareaWrkIDs = 0;
	private static int wrkoutofRdNet = 0;
	private static int schloutofRdNet = 0;
	private static int dcareoutofRdNet = 0;
	private static int hmoutofRdNet = 0;
	private static int validWrkIDs = 0;
	private static int validHmIDs = 0;
	private static int validSchlIDs = 0;
	private static int validDcareIDs = 0;
	private static int invalidWrkIDs = 0;
	private static int badCommutepath = 0; // returns if there is no AStar Path found
	private static int longestpath = 0;
	private static int totalpath = 0;
	private static int avgpath = 0;
	private static double longestcomdist = 0;
	private static double totalcomdist = 0;
	private static double avgcomdist = 0;
	private static int noDrivers = 0;
	
	// Need a global MBR
	static Envelope globalMBR;
	
	// RdIDs are used to associate real world road networks with the ABM
	// road network edges and nodes.
	Set<String> wrknoRdIDs = new HashSet<String>();
	
	public static HashMap <String, String> wrkToRdIDs = new HashMap <String, String> ();
	public static HashMap <String, String> schlToRdIDs = new HashMap <String, String> ();
	public static HashMap <String, String> dCareToRdIDs = new HashMap <String, String> ();
	public static HashMap <String, String> cWrkToRdIDs = new HashMap <String, String> (); // total daytime locations of children (school +  daycare)
	// used to check which workplaces are outside the commuter region
	private static ArrayList<String> outwrkIDs = new ArrayList<String> ();
	private static ArrayList<String> rdIDs = new ArrayList<String> ();
	
	
	//========================================
	//
	//     INITIALIZE SIMULATION ENVIRONMENT
	//
	//========================================
	
	/**
	 * Initialize World with createEnvironment and createPopulation
	 * Main WorldBuilder method
	 * @param world
	 * @throws IOException
	 */
	static public void initialize (World world) throws IOException {
		
		System.out.println("WorldBuilder>initialize>");	
		createEnvironment(world); // load map layers
		createPopulation(world);  // create population from synthetic population data
		
	}
	
	
	//===============================	
	//
	//	CORE WORLDBUILDER METHODS
	//
	//===============================	

	//==================================	
	//	Load Map Layers and Road Network
	//==================================	
	
	/**
	 * Initialize World and build GeomVectorFields from data files
	 * Data files imported through Parameters Class
	 * @param world
	 * @throws IOException 
	 */
	static public void createEnvironment (World world) throws IOException {
		
		// read in the road maps to create the roads geometry vector field
		System.out.println("WorldBuilder>createEnvironment>reading roads layer...");			
		File roadfile = new File(Parameters.roadsShape);  
		URL roadURL;
		try {
			roadURL = roadfile.toURL();
			ShapeFileImporter.read(roadURL, world.roads);
		} catch (Exception e) {
			e.printStackTrace();
		}
		globalMBR = world.roads.getMBR();
			
        // read in the census tracts map file to create background
		System.out.println("WorldBuilder>createEnvironment>reading background layer...");
		File censusfile = new File(Parameters.censusShape);
		URL censusURL;
		try {
			censusURL = censusfile.toURL();
			ShapeFileImporter.read(censusURL, world.censusTracts);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		globalMBR.expandToInclude(world.censusTracts.getMBR());

        // read in the water map shapefile to create background
		System.out.println("WorldBuilder>createEnvironment>reading in water layer...");
        // read in the tracts to create the background
		File waterfile = new File(Parameters.waterShape);
		URL waterURL;
		try {
			waterURL = waterfile.toURL();
			ShapeFileImporter.read(waterURL, world.waterField);
		} catch (Exception e) {
			e.printStackTrace();
		}		
		System.out.println("WorldBuilder>createEnvironment>expand MBR for water layer...");
		globalMBR.expandToInclude(world.waterField.getMBR());
		
		System.out.println("WorldBuilder>createEnvironment>set fields...");
        // update so that everyone knows what the standard MBR is
        world.roads.setMBR(globalMBR);
        world.waterField.setMBR(globalMBR);
        world.censusTracts.setMBR(globalMBR);
        
		System.out.println("WorldBuilder>createEnvironment>read in environmental data layers...");
		
		
		// create the road network the agents will traverse
        System.out.println("WorldBuilder>createEnvironment>creating network...");
        world.roadNetwork.createFromGeomField(world.roads);
        
        // count nodes for data input report in WorldBuilder
        Iterator iNode = world.roadNetwork.nodeIterator();
        int icounter = 0;
        while (iNode.hasNext()) {
        		icounter++;
        		iNode.next();
        }      
        System.out.println("WorldBuilder>createEnvironment>number of nodes " + icounter);

        // count edges for data input report in WorldBuilder
        Iterator iEdge = world.roadNetwork.edgeIterator();       
        icounter = 0;
        while (iEdge.hasNext()) {
        		icounter++;
        		iEdge.next();
        }        
        System.out.println("WorldBuilder>createEnvironment>number of edges " + icounter);
        
        
        // set speed limits for traffic
        for (Object o : world.roadNetwork.getEdges())
        {
            GeomPlanarGraphEdge e = (GeomPlanarGraphEdge) o;
            
            String ID = Integer.toString(e.getIntegerAttribute("rdID")); // Use this attribute title for small map
            String MTFCC = e.getStringAttribute("MTFCC");

            /**
             * MTFCC Road types in the road network from Census Tiger Files
             * S1100 (num 24369) is a primary road 
             * S1200 (num 37017) is a secondary road
             * S1400 (num 190405) is a residential road; sometimes scenic highway  
             * S1500 (num 20) is a 4WD vehicular travel road (often private)
             * S1630 (num 9267) is a ramp
             * S1640 (num 903) is a service drive
             * S1710 (num 1211) is a walkway or pedestrian trail
             * S1720 (num 14) is a stair
             * S1730 (num 27) is an alley
             * S1740 (num 372) is a limited access (logging, oil, etc.)
             * S1750 (num 56) is internal census use
             * S1780 (num 56) is a parking lot road
             * S1820 (num 6) is a bike trail
             */
            
            double speedlimit;
            if (MTFCC.startsWith("S1100") || MTFCC.startsWith("S1200") ) {
            		speedlimit = Parameters.HIGHWAY;
            }
            else {
            		speedlimit = Parameters.RESIDENTIAL;
            }
            
            double edgeDistance = (e.getDoubleAttribute("distance")*0.001) ;  // edge distance in kilometers

            world.idsToEdges.put(ID, e);  // world.idsToEdges.put(e.getDoubleAttribute("ID_ID").intValue(), e);
            world.edgesToDistance.put(e, edgeDistance);
            world.edgesToSpeedLimit.put(e, speedlimit);
            
            e.setData(new ArrayList<Agent>());
        }
        
        System.out.println("WorldBuilder>createEnvironment>Number of network edges in HashMap: " + world.idsToEdges.size());
        
        
        /**
         * adds nodes corresponding to road intersections to GeomVectorField
         * <p/>
         * @param nodeIterator  Points to first node
         * @param intersections GeomVectorField containing intersection geometry
         * <p/>
         * Nodes will belong to a planar graph populated from LineString network.
         */
        
        GeometryFactory fact = new GeometryFactory();
        Coordinate coord = null;
        Point point = null;
        int counter = 0;

        Iterator<?> nodeIterator = world.roadNetwork.nodeIterator();
        
        while (nodeIterator.hasNext())
        {
        	// Create a GeometryVectorField of points representing road intersections
            Node node = (Node) nodeIterator.next();
            coord = node.getCoordinate();
            point = fact.createPoint(coord);

            world.roadIntersections.addGeometry(new MasonGeometry(point));
            counter++;
        }
        
        /**
		 * Read in file to use for check on which agents have work outside the commuter region
		 */
		try {
			
			FileInputStream fstream = new FileInputStream(Parameters.rdIDfile);
			
			BufferedReader w = new BufferedReader(new InputStreamReader(fstream));
			String t;		
			
			while ( (t = w.readLine()) != null ) { // read in all data
				
				String [] field = t.split(",");
				String rdid = field[1]; //road id
				
				// add rdIDs to an Array List
				rdIDs.add(rdid);
			}
			
			// clean up
			w.close(); 			
			
		} catch (Exception e) {
			System.out.println("WorldBuilder>createEnvironment>Read WorkIDs ERROR: issue with outerwork file: " + e);
		}
		
		System.out.println("WorldBuilder>createEnvironment>added rdIDs for verification: arraylist of length: " + rdIDs.size());
		
	}
	
	
	//=========================================
	//    Create Agent Population
	//=========================================
	 
	/** Create buildings and agent population from demographic input file
	 *  Also, creates the agent's ArrayList of household social connections
	 * @param world
	 * @throws IOException 
	 */
	static public void createPopulation(World world) throws IOException {
		System.out.println("WorldBuilder>createPopulation>");
		
		//******************
		//Read In Outer Work places
		//******************
		
		try {
				FileInputStream fstream = new FileInputStream(Parameters.outerwrkfile);
				BufferedReader w = new BufferedReader(new InputStreamReader(fstream));
				String t;
				w.readLine(); // get rid of the header			
				
				while ( (t = w.readLine()) != null ){ // read in all data
			
					String [] field = t.split(",");
					String workID = field[1];	// work id number
					
					// add IDs to an Array List
					outwrkIDs.add(workID);			
					//lines++;	
				}
				// clean up
				w.close(); 						
			} 
		catch (Exception e) {
				System.out.println("WorldBuilder>createPopulation>Read WorkIDs ERROR: issue with outerwork file: " + e);
			}
			System.out.println("WorldBuilder>createPopulation>added outerWrkIDs for verification: arraylist of length: " + outwrkIDs.size());
				
		//=======================
		// read work roadIDs
		//=======================

		// Read in the work to roadIDs file and create a HashMap for RoadID assignments
		try {
				//int lines = 0;
				FileInputStream fstream = new FileInputStream(Parameters.workfile);
				BufferedReader w = new BufferedReader(new InputStreamReader(fstream));
				String t;
				
				w.readLine(); // get rid of the header
				
				while ( (t=w.readLine()) != null ) { // read in all data
					String [] field = t.split(",");
					String workID = field[1]; // work ID number
					String rdID = field[2]; // school or daycare's nearest road ID number
					
					int Type = 2; // building type 2 is a work place
					
					wrkToRdIDs.put(workID, rdID);
					
					if (world.idsToEdges.get(rdID) == null) {
						//System.out.println("WorldBuilder>createPopulation>work places outside area");
					}
					else {
						GeomPlanarGraphEdge edge = world.idsToEdges.get(rdID);
					}
								
				}
				
				System.out.println("WorldBuilder>createPopulation>assigned road edges to work IDs: " + wrkToRdIDs.size());
				// clean up
				w.close(); 	
			} 
		catch (Exception e) {
			System.out.println("WorldBuilder>createPopulation>Read workIDs ERROR: issue with wrkToRdIDs file: " + e);
		}
				
		
		//=======================
		// read school IDs
		//=======================

		// Read in the school locations file and create HashMap for assignments
		try {
			int lines = 0;
			
			FileInputStream fstream = new FileInputStream(Parameters.schoolfile);
			
			BufferedReader w = new BufferedReader(new InputStreamReader(fstream));
			String t;
			
			w.readLine(); // get rid of the header
			
			while ( (t=w.readLine()) != null ) { // read in all data
				String [] field = t.split(",");
				String schlID = field[1]; // school ID number
				String rdID = field[2]; // school or daycare's nearest road ID number
				
				schlToRdIDs.put(schlID,rdID);
				cWrkToRdIDs.put(schlID, rdID);  // wrk IDs for children
				
				if (world.idsToEdges.get(rdID) == null) {
					//System.out.println("WorldBuilder>createPopulation>school outside area");
				}
				else {
					GeomPlanarGraphEdge edge = world.idsToEdges.get(rdID);
				}
							
				lines++;
				
			}
			
			System.out.println("WorldBuilder>createPopulation>assigned road edges to school IDs: " + schlToRdIDs.size());
			
			// clean up
			w.close(); 
			
		} catch (Exception e) {
			System.out.println("WorldBuilder>createPopulation>Read schlIDs ERROR: issue with schoolRdID file: " + e);
		}		
		
		
		//=======================
		// read daycare IDs
		//=======================

		// Read in the daycare file and create Hashmap for assignments
		try {
			int lines = 0;
			
			FileInputStream fstream = new FileInputStream(Parameters.daycarefile);
			
			BufferedReader w = new BufferedReader(new InputStreamReader(fstream));
			String t;
			
			w.readLine(); // get rid of the header
			
			while ( (t=w.readLine()) != null ) { // read in all data
				String [] field = t.split(",");
				String dcareID = field[1]; // school or daycare ID number
				String rdID = field[2]; // school or daycare's nearest road ID number
				
				dCareToRdIDs.put(dcareID,rdID);
				cWrkToRdIDs.put(dcareID, rdID);  // wrk IDs for children
				
				if (world.idsToEdges.get(rdID) == null) {
					//System.out.println("WorldBuilder>createPopulation>daycarel outside area");
				}
				else {
					GeomPlanarGraphEdge edge = world.idsToEdges.get(rdID);
				}
				
				lines++;
				
			}
			
			System.out.println("WorldBuilder>createPopulation>assigned road edges to dCare IDs: " + dCareToRdIDs.size());
			System.out.println("WorldBuilder>createPopulation>assigned road edges to cWrk IDs: " + cWrkToRdIDs.size());
			
			// clean up
			w.close(); 
			
		} catch (Exception e) {
			System.out.println("WorldBuilder>createPopulation>Read dcareIDs ERROR: issue with daycareRdID file: " + e);
		}
		
		
		//=======================
		// read population IDs
		//=======================

		// Read in the population file
		int counter = 0;
		try {
					
			FileInputStream fstream = new FileInputStream(Parameters.popfile);
					
			BufferedReader w = new BufferedReader(new InputStreamReader(fstream));
			String t;

			w.readLine(); // get rid of the header			
					
			while ( (t = w.readLine()) != null ){ // read in all data
				// note: can make the IDs smaller by scraping out the track numbers
				String [] field = t.split(",");
				
				//new load
				String agentID = field[1];	// ID number
				String tract = agentID.substring(0,11);
				String county = agentID.substring(0,5);
				String age = field[2];
				String sex = field[3];
				String homeID = field[4];  // ID of home location
				String workID = field[6]; // ID of daytime location
				//String workcounty = workID.substring(0,5); // used for testing sample population's movement
				String hmRdID = field[9];	// road ID number nearest home
				
				// get road ID for daytime location 
				String wrkRdID = "";
				
				if (workID.contains("w")) {  // check if string has 'w' (work), if not
					wrkRdID = wrkToRdIDs.get(workID);
					if (wrkRdID == null) {
						if (outwrkIDs.contains(workID)) {
							outofareaWrkIDs++;
						}
						workID = homeID;
						wrkRdID = hmRdID;
						nullWrkIDs++;
					}
					else if (!rdIDs.contains(wrkRdID)) {
						workID = homeID;
						wrkRdID = hmRdID;
						wrkoutofRdNet++;
					}
					else {
						validWrkIDs++;
					}
				}
				else if (workID.contains("s")) {  // check if string has 's' (school)
					wrkRdID = schlToRdIDs.get(workID);
					if (wrkRdID == null) {
						nullschlWrkIDs++;
					}
					else if (!rdIDs.contains(wrkRdID)) {
						workID = homeID;
						wrkRdID = hmRdID;
						schloutofRdNet++;
					}
					else {
						validSchlIDs++;
					}
				}
				else if (workID.contains("d")) {  // check if string has 'd' (daycare)
					wrkRdID = dCareToRdIDs.get(workID);
					if (wrkRdID == null) {
						wrkRdID = hmRdID; // if this is null, work defaults to home (until daycare file cleaned up)
						nulldcWrkIDs++;
					}
					else if (!rdIDs.contains(wrkRdID)) {
						workID = homeID;
						wrkRdID = hmRdID;
						dcareoutofRdNet++;
					}
					else {
						validDcareIDs++;
					}
				}
				else if (workID.contains("h")) {  // check if string has 'h'
					wrkRdID = hmRdID;
					if (wrkRdID == null) {
						nullhmWrkIDs++;
					}
					else if (!rdIDs.contains(wrkRdID)) {
						wrkRdID = hmRdID;
						hmoutofRdNet++;
					}
					validHmIDs++;
				}
				else { // if workIDs are bad assign it the homeID
					workID = homeID;
					wrkRdID = hmRdID; // place holder for other daytime locations
					invalidWrkIDs++;
				}
				

					if (counter % 1 == 0) {  // sample the data file by a given factor, l.e. 1, 10, 100, etc.
						
							// Add individual population to household network Hashmap and set agent household network
							add2household(world, homeID, agentID);		// put household in housenetwork hashmap		

							// Get household to add to agent data
			    			ArrayList<String> housenet = World.hholdnetworks.get(homeID); 
			    			ArrayList<String> household = new ArrayList<String>();
			    			
			    			int size = housenet.size();
			    			if (size > 12) {  // to break the institutional homes into smaller groups
			    				Random rand = new Random();
			    				int hholdsize = rand.nextInt(12) + 1;
			    				for (int i = 0; i < hholdsize; i++) {
			    					String randNode = housenet.get((int) Math.floor(Math.random() * housenet.size()));
			    					household.add(randNode);
			    				}
			    			}
			    			else {
			    				household = housenet;
			    			}


							Indv a = new Indv(world, serialVersionUID, tract, county, agentID, age, sex, homeID, workID, hmRdID, wrkRdID, 
									household);
							
							
							// Population's Path Statistics
							int path = a.getPathLength();
							totalpath += path;
							if ( path > longestpath) {
								longestpath = path;
							}
							
							double comdist = a.getcommuteDist();
							totalcomdist += comdist;
							if ( comdist > longestcomdist) {
								longestcomdist = comdist;
							}
							
							
							if (!a.pathset)
							{
								WorldBuilder.badCommutepath += 1;
								Log.badagent += 1;
								Log.atHomeCount -= 1;
								if (a.StayAtHome) { // don't double count the bad agent
									Log.stayAtHome -= 1;
								}
								
								continue; // DON'T ADD IT if it's bad; i.e. no commuting path
							}
	
							else {
								MasonGeometry newGeometry = a.getGeometry();
								newGeometry.isMovable = true;
								world.indvs.addGeometry(newGeometry);
								Log.indvList.add(a); // ArrayList of the individuals
								world.schedule.scheduleRepeating(a);
								
								Log.agentpopulation++; 
								world.idsToIndvs.put(a.getID(),a); // add to HashMap used to retrieve agent objects, if there is a path
								
								if (world.idsToEdges.get(hmRdID) == null) {
								}
								else {
									GeomPlanarGraphEdge edge = world.idsToEdges.get(hmRdID);
								}
							}
					}
					
				}
				if (counter % 100 == 0) {
					System.out.println("WorldBuilder>createPopulation>Number of Indv agents inititalized: " + counter + " idsToIndvs: " + world.idsToIndvs.size());
				}
				counter++;
                world.indvs.setMBR(globalMBR);
			
			avgpath = totalpath / counter;	
			avgcomdist = totalcomdist / counter;
			
			// clean up
			w.close(); 	
					
		} catch (Exception e) {
			System.out.println("WorldBuilder>createPopulation>Read population ERROR: issue with population file: " + e);
		}
		
		
		/**
		 * Check population -- clean up -- try to remove bad agents from lists and hashmaps
		 */
		Iterator<HashMap.Entry<String, Indv>> i = world.idsToIndvs.entrySet().iterator();
		int nullIDs = 0;
		while (i.hasNext()) {
			HashMap.Entry<String, Indv> pair = i.next();
			Indv a = pair.getValue();
			if (a == null) {
				nullIDs += 1;
				world.idsToIndvs.remove(pair.getKey(), pair.getValue());
				Log.indvList.remove(a); // ArrayList of the individuals
			}
			else {
				//System.out.print(a.getID() + " " + a.getWorkID() + " ");
			}
		}
		System.out.println("WorldBuilder>createPopulation>clean->null agents: " + nullIDs);
		
		/**
		 * Update population to include commute paths with daycare and school dropoffs
		 * Create first groups
		 */
		try {
//			if (Parameters.Grouping) {
			if (Parameters.Carpool) {
				dcCarpool(world);	// create carpools groups for daycare
				System.out.println("WorldBuilder>createPopulation>created commutepaths with daycare dropoffs");
			}
			
		} catch (Exception e) {
			System.out.println("WorldBuilder>createPopulation>dcCarpool ERROR: issue with daycare carpools" + e);
		}
		
		System.out.println("WorldBuilder>createPopulation>idsToIndvs size: " + world.idsToIndvs.size());
		
		System.out.println("*****************");
		System.out.println("WorldBuilder>createPopulation>Data Inputs Report: ");
		System.out.println("WorldBuilder>createPopulation>null fields from the data input (#null/#rows) WrkIDs:" + nullWrkIDs + "/" + wrkToRdIDs.size() +
				" schWrkIDs:" + nullschlWrkIDs + "/" + schlToRdIDs.size() + " dcWrkIDs:" + nulldcWrkIDs + "/" + dCareToRdIDs.size() + 
				" hmWrkIDs:" + nullhmWrkIDs + "/" + counter);
		System.out.println("WorldBuilder>createPopulation>out of road network area (#outofarea/#rows) wrk:" + wrkoutofRdNet + "/" + wrkToRdIDs.size() +
				" schl:" + schloutofRdNet + "/" + schlToRdIDs.size() + " daycare:" + dcareoutofRdNet + "/" + dCareToRdIDs.size() + 
				" hm:" + hmoutofRdNet + "/" + counter);
		System.out.println("WorldBuilder>createPopulation>Valid WrkIDs:" + validWrkIDs + " ValidSchlIDs:" + validSchlIDs +
				" Valid ChildCareIDs:" + validDcareIDs + " Valid HmIDs:" + validHmIDs + " Invalid WrkIDs:" + invalidWrkIDs);
		System.out.println("WorkdBuilder>createPopulation>Bad HomeNodes: " + Log.badhomeNode);
		System.out.println("WorldBuilder>createPopulation>Agents discarded due to bad commutepaths: " + WorldBuilder.badCommutepath);		
		System.out.println("WorldBuilder>createPopulation>Agents without commute path or valid homeID:" + Log.badagent);
		System.out.println("WorldBuilder>createPopulation>clean->null agents: " + nullIDs);
		System.out.println("WorldBuilder>createPopulation>Daycare agents without drivers:" + noDrivers);
		System.out.println("WorldBuilder>createPopulation>Longest path: " + longestpath + "  Avg path: " + avgpath);
		System.out.println("WorldBuilder>createPopulation>Longest commute: " + longestcomdist + " Avg comdist: " + avgcomdist);
		System.out.println("");
		System.out.println("WorldBuilder>createPopulation>created agent population size: " + Log.agentpopulation + "  number of groups: " + Log.grouppopulation); 
				// can also check: + " " + world.idsToAgents.size());
		System.out.println("WorldBuilder>createPopulation>stay-at-home agents: " + Log.stayAtHome 
				+ "  Agents currently at home: " + Log.atHomeCount);
		
//		// Uncomment if you want to export the initial household networks
//		System.out.println("WorldBuilder->createPopulation->exporting household networks");		
//		world.worldResults.exportSocialNet(world.hholdnetworks, "test1");
		
	}

	
	//===============================	
	//
	//	WORLDBUILDER HELPER METHODS
	//
	//===============================	
	
	/**
	 * Align geometry vector fields for the MBR
	 * @param base
	 * @param others
	 */
    static void alignVectorFields(GeomGridField base, GeomVectorField[] others)
    {
        Envelope globalMBR = base.getMBR();
        for(GeomVectorField vf: others)
            globalMBR.expandToInclude(vf.getMBR());
        for(GeomVectorField vf: others)
            vf.setMBR(globalMBR);
    }
    
    
    /**
     * Creates or updates the HashMap of household social networks
     * Either creates a new household network entry, adds an agent to an existing household, or creates the first household entry
     * @param hholdID
     * @param agentID
     * @param world
     */
	private static void add2household (World world, String hholdID, String agentID) 
    {
    		// Create first key, object
    		if (World.hholdnetworks.isEmpty()) {
    			ArrayList<String> housenet = new ArrayList<String>();
    			housenet.add(agentID); // add first agentID to the housenet
    			World.hholdnetworks.put(hholdID, housenet);  // put household network in the array of household networks
    		}
    		// If household is in the map, add agent to its household list
    		else if (World.hholdnetworks.containsKey(hholdID)) {
    			World.hholdnetworks.get(hholdID).add(agentID);
    		}
    		// Otherwise add household as key and the agent stringID
    		else {
    			ArrayList<String> housenet = new ArrayList<String>(); 
    			housenet.add(agentID);
    			World.hholdnetworks.put(hholdID, housenet);
    		}
    }

	
	/**
	 * Updates each agent's known living household members network to initial model groundtruth
	 * Creates HashMap of household members for each individual
	 * @param world
	 */
	private static void updateHouseNets(World world) {
	    Iterator<Entry<String, Indv>> it = world.idsToIndvs.entrySet().iterator();
	    
	    while (it.hasNext()) {
			HashMap.Entry <String, Indv>pair = it.next();
			Indv a = pair.getValue();
			ArrayList<String> housenet = World.hholdnetworks.get(a.getHomeID()); // retrieve the groundtruth household network
			a.sethholdnet(housenet); // reset the household network (ArrayList<String>) of the agent
			// iterate through housenet and create HashMap
			for (String ID: housenet) {
				a.idsToHouseMembers.put(ID, world.idsToIndvs.get(ID));
			}
			
	    }
	    
	    // check and count household sizes above 12
	    int counter = 0;
	    for (ArrayList<String> network: World.hholdnetworks.values()) {
	    	if (network.size() >= 12) {
	    		counter++;
	    	}
	    }
	    System.out.println("WorldBuilder>createPopulation>udpdateHouseNets># of households >= 12: " + counter);
	}
	
	
	/**
	 * Creates commute groups and paths for agents to drop children off at daycare
	 * @param world
	 */
	private static void dcCarpool(World world) {
		int badagents = 0;
		int count = 0;
		int countdc = 0;
		// iterate through the population households
		Iterator<HashMap.Entry<String, ArrayList<String>>> it =  World.hholdnetworks.entrySet().iterator();
		while (it.hasNext()) {
			count += 1;
			HashMap.Entry<String, ArrayList<String>> pair = it.next();			
			ArrayList<String> hhold = pair.getValue();

			for (Object a: hhold) {
				// if the agent object is null, remove it
				if (world.idsToIndvs.get(a) != null) {
					Indv person = world.idsToIndvs.get(a);
	
					if (person == null) { // confirm there are agents in the household
						badagents += 1;
					}
					else {
						if (person.getWorkID().contains("d")) { // check if person in the household goes to daycare
							countdc += 1;
							findDriver(world, hhold, person);
						}
					}
				}
			}  // close for loop iterating over each household's members
	
		}  // close while loop iterating over all the household in the population
				
		world.groups.setMBR(globalMBR);
		System.out.println("WorldBuilder>createPopulation>dcCarpool->badagents: " + badagents + " out of " + world.idsToIndvs.size());
		
	}
	
	/**
	 * Find a driver for the rider from the carpool list of potential drivers
	 * Method checks for an existing carpool or makes a new one
	 * @param world
	 * @param carPool
	 * @param child
	 */
	private static void findDriver(World world, ArrayList<String> hhold, Indv child) {
		int badagents = 0;
		boolean haveDriver = false;		
		// Check household for an existing driver
		for (String l: hhold) {
			if (world.idsToIndvs.get(l) != null) {
				Indv driver = world.idsToIndvs.get(l);
				if (driver.getisLeader()) {
					// add to existing carpool
					haveDriver = true;
					addtoCarpool(world, driver, child);
					return; // breakout of for loop, if found a driver
				}
			}
			else {
				badagents += 1;
			}
		}

		// If don't have a driver yet, first check household for stay-at-home driver and create carpool
		if (!haveDriver)  {
			for (String d: hhold) {
				if (world.idsToIndvs.get(d) != null) {
					Indv driver = world.idsToIndvs.get(d);
					if (haveDriver == false && driver.getHomeID() == driver.getWorkID()) {
						
						if (driver.getAge()>=18) {
							makeCarpool(world, driver, child);							
							haveDriver = true;

							return; // breakout of for loop, if found a driver
						}
							
					}  // close if statement checking for stay-at-home agents over 17
				}
				else {
					badagents += 1;
				}
			}  // close for loop 
		}
		
		if (!haveDriver)  {
		// If don't have a driver yet, check household for first available eligible driver and create carpool
			for (Object c: hhold) {
				if (world.idsToIndvs.get(c) != null ) {
					Indv driver = world.idsToIndvs.get(c);
					if (haveDriver == false && !driver.getWorkID().contains("d") && !driver.getWorkID().contains("s")) {

						if (driver.getAge()>=18) {
							makeCarpool(world, driver, child);							
							haveDriver = true;

							return; // breakout of for loop, if found a driver
						}			
						
					}  // close if statement checking for daycare or school age agents
				}
				else {
					badagents += 1;
				}
			}  // close for loop 
			
		}
				
		if (!haveDriver) { // print out confirmation of carpool for testing
			// note there is a household with 264 members and at least 5 daycare-- driven by same agent (stay-at-home)
			// if no driver was found after checking all household members
			noDrivers += 1;	
		}
		
	}
	
	
	/**
	 * Construct a group carpool of two riders
	 * @param world
	 * @param driver
	 * @param rider
	 */
	static void makeCarpool(World world, Indv driver, Indv rider) {
		// Create group
		Integer grpIDnum = world.idsToGrps.size(); // get next group ID number
		Group g = new Group(world, serialVersionUID, grpIDnum, driver, rider, "carpool");

		// record the group id for the carpool network
		driver.setindvGrpID(g.getID());
		rider.setindvGrpID(g.getID());
		driver.setcarGrpID(driver.getindvGrpID());
		rider.setcarGrpID(driver.getindvGrpID());
		
		// add members for the first carpool ride
		g.currentCarpool.add(driver.getID());
		g.currentCarpool.add(rider.getID());
		
		// If daycare rider and driver have same workcoordinate
		if (driver.getWorkNode().getCoordinate() == rider.getWorkNode().getCoordinate()) {
			g.multiGoalNodes.add(driver.getStartNode());
			g.multiGoalNodes.add(driver.getWorkNode());
		}
		else {
			// Set up commute paths for all the riders
			g.multiGoalNodes.add(driver.getStartNode());
			g.multiGoalNodes.add(rider.getWorkNode());
			g.multiGoalNodes.add(driver.getWorkNode());
			g.ckGoalNodes();
		}
		
		g.setToWork(false);
		g.setatWork(false);
		g.set_tcommuteStart(730);
		g.set_tcommuteEnd(1830);
		
		// set the carpool multipath
		if (!g.setMultiPath()) { // try to create a multipath, if false
			System.out.println("WorldBuilder>dcCarpool>findDriver>makeCarpool>couldn't set multiPath");
		}
		else { // else a multipath was created, set group in World vectorgrid and hashmap
			MasonGeometry newGeometry = g.getGeometry();
			newGeometry.isMovable = true;
			Log.grpList.add(g); // Arraylist of the groups
			
			world.schedule.scheduleRepeating(g);
			
			Log.grouppopulation++;

			world.groups.addGeometry(newGeometry);
			g.ckGoalNodes(); // prints out multipath goal nodes and individual group member goal nodes for verification
		}	

	}
	
	
	/**
	 * Add a rider to an existing carpool
	 * @param world
	 * @param driver
	 * @param rider
	 */
	static void addtoCarpool(World world, Indv driver, Indv rider) {
		// Add member
		String grpID = driver.getindvGrpID();
		Group g = world.idsToGrps.get(grpID);
		g.addMember(rider);
		rider.setcarGrpID(rider.getindvGrpID());
		g.currentCarpool.add(rider.getID()); // add rider to the first carpool ride
		
		// Update commute paths
		// Check whether new rider goalNode is already on the route
		Boolean onRoute = false;
		for (Node n: g.multiGoalNodes) {
			if (rider.getWorkNode().getCoordinate() == n.getCoordinate()) {
				onRoute = true;
			}
		}
		
		if (!onRoute) {
			g.ckGoalNodes(); // prints out multipath goal nodes and individual group member goal nodes for verification
			int lastIndex = g.multiGoalNodes.size() - 1;
			Node lastNode = g.multiGoalNodes.get(lastIndex);

			g.multiGoalNodes.remove(lastIndex);
			g.multiGoalNodes.add(rider.getWorkNode());
			g.multiGoalNodes.add(driver.getWorkNode());
			g.ckGoalNodes();

			if (g.setMultiPath()) {
				
			}
			else {
				g.ckGoalNodes(); // prints out multipath goal nodes and individual group member goal nodes for verification
			}
		}
		else { // add node and path to carpool route
			g.ckGoalNodes(); // prints out multipath goal nodes and individual group member goal nodes for verification
		}
		
	}
	
	
	//===============================	
	//
	//	WORLDBUILDER HELPER METHODS
	//
	//===============================	
	
	static void removehholdAgent(World world, Indv a) {
		ArrayList<String> housenet = a.getHholdnet();
		// remove the agent from it's household
		for (String memID: housenet) {
			a.idsToHouseMembers.remove(memID);
			Indv mem = world.idsToIndvs.get(memID);
			mem.getHholdnet().remove(a);
		}
		world.idsToIndvs.remove(a);
		Log.indvList.remove(a);
	}
	
	
}
