/** 
 *  Disaster ABM in MASON
 *  @author Annetta Burger, Bill Kennedy, and Richard Jiang
 *  2018-19
 *  
 */

package disaster;

// Class imports
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import sim.util.geo.GeomPlanarGraphEdge;


//=========================
/** 
* Results Class to manage model outputs and simulation results
*/
//=========================
public class Results {
		
	private static int agentLocationRecordsCount = 0; 
	
	/**
	 * Method to initialize/construct the results class in world
	 */
	public static void initializeResults() {
	
	}
	
	
	//======================================
	//
	//     EXPORT
	//
	//======================================
	
	/**
	 * Method to export the entire social network hashmap into a csv file
	 * @param network
	 * @param filename
	 * @throws IOException
	 */
	public void exportSocialNet(HashMap <String, ArrayList<String>> network, String filename) throws IOException {
		
	    FileWriter writer = new FileWriter("data/" + filename + ".csv");

	    for (String key: network.keySet()) {
	    		ArrayList<String> agentnet = new ArrayList<String>();
	    		agentnet = network.get(key);
	    		agentnet.add(0, key); // add agentID string as the first element of the list
	    		
	    		String collect = agentnet.stream().collect(Collectors.joining(","));
	    	    
	    	    writer.write(collect); // write agent network as csv line
	    	    writer.write("\n"); // new line in csv file
	    }
	    
	    writer.close();
	    System.out.println("Results>social network hashmap data file closed");
	}
	
	
	/**
	 * Method to write out emergent network edge list
	 * @param filename
	 * @param world
	 * @throws IOException
	 */
	public static void exportEgrpEdgeList(String filename, World world) throws IOException {
		
		FileWriter writer = new FileWriter("data/" + filename + ".csv");
		
		// set up the source & target columns
		writer.append("Source,");
		writer.append("Target");
		writer.append("\n");
		
		for (String id: world.emerGroups) {
			Group g = world.idsToGrps.get(id);
			
			// write out the emergent network edgelist row
			for (String member: g.members) {
				
				// write tied members
				for (String memID: g.members) {
					if (memID != member) {
						writer.append(member);
						writer.append(",");
						writer.append(memID);
						writer.append("\n");
					}
				}
			}
			
		} // end writing an agent's emergent network edgelist data
		
		writer.flush();
		writer.close();	
		System.out.println("Results>emergent edge list data file closed");
	}
	
	
	/**
	 * Method to export a network edge list, such as a household network
	 * @param filename
	 * @throws IOException
	 */
	public static void exportEdgeList(String filename) throws IOException {
		
		FileWriter writer = new FileWriter("data/" + filename + ".csv");
		
		// set up the source & target columns
		writer.append("Source,");
		writer.append("Target");
		writer.append("\n");
		
		for (Indv a: Log.indvList) {
			ArrayList<String> hhold = new ArrayList<String>();
			hhold = a.getHholdnet();
			
			// writer out the household network edgelist row
			for (String id: hhold) {
				writer.append(a.getID());
				writer.append(",");
				writer.write(id);
				//writer.append(",");
				writer.append("\n");
			}
			
			// check for an emergent group
			if (a.getEmergnet() != null) {
				ArrayList<String>  emergnet = new ArrayList<String>();
				emergnet = a.getEmergnet();
				
				// writer out the emergent group network edgelist row
				for (String id: emergnet) {
					writer.append(a.getID());
					writer.append(",");
					writer.write(id);
					//writer.append(",");
					writer.append("\n");
				}		
			}			
		} // end writing an agent's edgelist data
		
		writer.flush();
		writer.close();		
		System.out.println("Results>edge list data file closed");
	}
	
	
	/**
	 * Method to export emergent group node list
	 * @param filename
	 * @throws IOException
	 */
	public static void exportNodeList(String filename) throws IOException {
		
		FileWriter writer = new FileWriter("data/" + filename + ".csv");
		
		// set up the source & target columns
		writer.append("Id,");
		writer.append("HmID,");
		writer.append("WrkID,");
		writer.append("Sex,");
		writer.append("Age,");
		writer.append("Grp");
		writer.append("\n");
		
		for (Indv a: Log.indvList) {
			writer.append(a.getID());
			writer.append(",");
			writer.append(a.getHomeID());
			writer.append(",");
			writer.append(a.getWorkID());
			writer.append(",");
			writer.append(a.getSex());
			writer.append(",");
			writer.append(String.valueOf(a.getAge()));
			if (a.getEmergnet() != null) {
				writer.append(",");
				writer.append("inGrp");
			}
			writer.append("\n");
		}
		
		writer.flush();
		writer.close();	
		System.out.println("Results>node list data file closed");
	}
	
	
	//======================================
	//
	//     VISUALIZATION
	//
	//======================================
	
	/**
	 * Open csv file for exporting agent location visualizations
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static FileWriter openExportVizDataFile (String filename) throws IOException 
	{
		// FileWriter writer = new FileWriter("data/VizData/" + filename + step + ".csv");
		// need to finish writing this method

		FileWriter csvWriter = new FileWriter(filename);
		csvWriter.append("Step");
		csvWriter.append(",");
		csvWriter.append("Year");
		csvWriter.append(",");
		csvWriter.append("Month");
		csvWriter.append(",");
		csvWriter.append("Day");
		csvWriter.append(",");
		csvWriter.append("HourMin");
		csvWriter.append(",");
		csvWriter.append("ID");
		csvWriter.append(",");
		csvWriter.append("Long");
		csvWriter.append(",");
		csvWriter.append("Lat");
		csvWriter.append(",");
		csvWriter.append("health");
		csvWriter.append("\n");
		System.out.println("Results>viz data file opened.");
		return csvWriter;
	}
	
	
	/**
	 * Write agent location data to csv file
	 * @param csvWriter
	 * @param step
	 * @param agentID
	 * @param alat
	 * @param along
	 * @param health
	 * @throws IOException
	 */
	public static void exportVizDataRow (FileWriter csvWriter, int step, int year, int month, int day, int hourMin, String agentID, double alat, double along, int health) throws IOException 
	{
		String row = step + "," + year + "," + month + "," + day + "," + hourMin + "," + agentID + "," + alat + "," + along + "," + health;
		csvWriter.append(String.join(",", row));
		csvWriter.append("\n");
	}
	
	
	/**
	 * Close visualization data file
	 * @param csvWriter
	 * @throws IOException
	 */
	public static void closeExportVizDataFile (FileWriter csvWriter) throws IOException
	{
		csvWriter.flush();
		csvWriter.close();
		System.out.println("Results>viz data file closed.");
	}
	
	
		//==========================
		//
		// Save Agent Location Data
		//
		//==========================
	
		/**
		 * Open csv file for exporting agent location data
		 * @param filename
		 * @return
		 * @throws IOException
		 */
		public static FileWriter openAgentLocationDataFile (String filename) throws IOException 
		{
			FileWriter csvWriter = new FileWriter(filename);
			csvWriter.append("Step");
			csvWriter.append(",");
			csvWriter.append("workForce");
			csvWriter.append(",");
			csvWriter.append("notWorking");	// adults not working out of the house
			csvWriter.append(",");
			csvWriter.append("atHome");		// workers & kids at home
			csvWriter.append(",");
			csvWriter.append("commuting");	// people commuting to work, school, or day care
			csvWriter.append(",");
			csvWriter.append("atWork");		// people at work, school, or day care
			csvWriter.append(",");
			csvWriter.append("fleeing");	// victims fleeing zone 2 or zone 3;
			csvWriter.append(",");
			csvWriter.append("dead");
			
//			csvWriter.append(",");
//			csvWriter.append("popZone2");	// victims of zone 2 (mortally wounded)
//			csvWriter.append(",");
//			csvWriter.append("popZone3");	// victims of zone 3 (only injured)
//			csvWriter.append(",");
//			csvWriter.append("firstResp");	// first responders inside damage zones
//			csvWriter.append(",");
//			csvWriter.append("beingAided");	// victims being assisted by first responders;
			csvWriter.append("\n");
			System.out.println("Results> location counts data file opened.");
			return csvWriter;
		}

		
		/**
		 * Export agent locations into csv file
		 * @param csvWriter
		 * @param step
		 * @throws IOException
		 */
		public static void exportAgentLocationsDataRow (FileWriter csvWriter, int step) throws IOException 
		{
			String row = step 
					+ "," + Log.agentpopulation
					+ "," + Log.stayAtHome
					+ "," + Log.atHomeCount
					+ "," + Log.onCommuteCount
					+ "," + Log.atWorkCount
					+ "," + Log.affectedFleeing
					+ "," + Log.indvDeaths
					
//					+ "," + Monitors.popZone2
//					+ "," + Monitors.popZone3
//					+ "," + Monitors.firstResponders
//					+ "," + Monitors.beingAided
				    ;
					
			csvWriter.append(String.join(",", row));
			csvWriter.append("\n");
			agentLocationRecordsCount ++;	// increment count
			
		}
		
		
		/**
		 * Close csv file for agent location data
		 * @param csvWriter
		 * @throws IOException
		 */
		public static void closeAgentLocationsDataFile (FileWriter csvWriter) throws IOException
		{
			csvWriter.flush();
			csvWriter.close();
			System.out.println("Results>agent location data file closed with " + agentLocationRecordsCount + " records + header.");
		}
	
	
	//======================================
	//
	//     TESTING
	//
	//======================================
	
	/**
	 * exportRdIDs used to verify RdIDs from the Road Network file
	 * @param RdNetHashMap
	 * @param filename
	 * @throws IOException
	 */
	public void exportRdIDs(HashMap <String, GeomPlanarGraphEdge> RdNetHashMap, String filename) throws IOException {
		System.out.println("Results->exporting RdIDstoEdges for verification");

	    FileWriter writer = new FileWriter("data/" + filename + ".csv");
	    
	    for (String key: RdNetHashMap.keySet()) {
	    	String ID = key;
	    	writer.write(ID);
	    	writer.write("\n");	    	
	    }
	    
	    writer.close();		
	}
	

	// Test agent travel coordinates
	public static final String path = "data/agent_output.tsv";
	
	
	/**
	 * Write out line for agent testing output
	 * @param line
	 * @throws IOException
	 */
	public static void writeLine(String line) throws IOException {
		 File fout = new File(path);
		 if(fout.exists() == false) {
			 FileOutputStream fos = new FileOutputStream(fout);
			 
			 BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
			 bw.write("AgentId\tStep\tStart Coord.x\tStart Coord.y\tEnd Coord.x\tEnd Coord.y\tCurrent Coord.x\tCurrent Coord.y\ttoWork\tatWork\treachDestination\n");
			 bw.close();
		 }
		 File file = new File(path);
		 FileWriter fr = new FileWriter(file, true);
		 BufferedWriter br = new BufferedWriter(fr);
		 br.write(line+"\n");
		 br.close();	
	}
	
	
	/**
	 * Delete test log file
	 */
	public static void deleteLogFile() {
		File file = new File(path);
		if (file.exists()) {
			file.delete();
		}
	}
	
}