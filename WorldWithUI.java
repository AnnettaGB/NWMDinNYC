/** 
 *  Disaster ABM in MASON
 *  @author Annetta Burger
 *  2018 */

package disaster;

//Class imports
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Toolkit;		// to make beep
import javax.swing.JFrame;
import org.jfree.data.xy.XYSeries;

import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.simple.ShapePortrayal2D;
import sim.util.geo.MasonGeometry;
import sim.util.media.chart.TimeSeriesChartGenerator;

public class WorldWithUI extends GUIState
{
	
	/**
	 * Display frames
	 */
    public Display2D display;
    public JFrame displayFrame;
    private GeomVectorFieldPortrayal roadsPortrayal = new GeomVectorFieldPortrayal(true);
    private GeomVectorFieldPortrayal tractsPortrayal = new GeomVectorFieldPortrayal(true);
    private GeomVectorFieldPortrayal waterPortrayal = new GeomVectorFieldPortrayal(true);
    private GeomVectorFieldPortrayal indvPortrayal = new GeomVectorFieldPortrayal();
    private GeomVectorFieldPortrayal groupPortrayal = new GeomVectorFieldPortrayal();
    
    
    /**
     * Chart schemes
     */
    TimeSeriesChartGenerator indvsBehaviorChart;
    XYSeries indvsPopulation;	// total number of agents
    XYSeries indvsNotWorking;  // Monitors.stayhome
    XYSeries indvsAtHome;	// Monitors.atHomeCount
    XYSeries indvsToSchoolDaycare; // Monitors.toSchoolDaycare
    XYSeries indvsCommuting;	// Monitors.onCommuteCount
    XYSeries indvsAtWork;	// Monitors.atWorkCount;
    XYSeries indvsDead;		// Monitors.indvDeaths;
    XYSeries indvsFleeing;	// Monitors.victimsFleeing
    XYSeries indvsFleeingHome; // Monitors.victimsHeadedHome
    XYSeries indvsResponding;  // Monitors.inZoneFirstResponders
    XYSeries indvsBlocked;	// Monitors.indvsBlocked by ???

    TimeSeriesChartGenerator indvsHealthChart;
    XYSeries indvsHCat0;   // Monitors.healthCat[0]
    XYSeries indvsHCat1;	// Monitors.healthCat[1]
    XYSeries indvsHCat2;	// Monitors.healthCat[2]
    XYSeries indvsHCat3;	
    XYSeries indvsHCat4;	
    XYSeries indvsHCat5;  
    XYSeries indvsHCat6;	
    XYSeries indvsHCat7;	
    XYSeries indvsHCat8;	
    XYSeries indvsHCat9;	
    XYSeries indvsHCat10;	// Monitors.healthCat[10];
    XYSeries indvsHCat11;	
    XYSeries indvsHCat12;	
    XYSeries indvsHCat13;	
    XYSeries indvsHCat14;	
    XYSeries indvsHCat15;	
    
    
    /**
     * Color schemes for display
     * Derived from ColorBrewer: http://colorbrewer2.org/
     */
    // Background layers
    final Color road = new Color (255,255,255);
    final Color census = new Color (240,240,240); //(246,239,199);
    final Color water = new Color (222,235,247); //(190,233,249);
    
    // Agent health
    final Color healthy = new Color (35,132,67);    // green //(65,171,93);		// lighter green
	final Color sickLow  = new Color(253,141,60); 	// orange
	final Color sickMed  = new Color(253,141,60); 	// orange
	final Color sickHigh = new Color(253,141,60);	// orange
	final Color lethal   = new Color(189,0,38); 	// red
	final Color dead     = new Color(0,0,0); 		// black
	
	// Agent Post-Impact Status
	final Color aided	 = new Color(200,100,200);	// pink
	final Color aiding   = new Color(100,100,200);  // light blue
	final Color homeward = new Color(100,250,100);  // light green
	
	
    protected WorldWithUI(SimState state)
    {
        super(state);
    }

    
    /**
     * Main function
     * @param args
     */
    public static void main(String[] args)  {
    	System.out.println("WorldWithGUI->Starting Disaster Simulation with GUI");
        WorldWithUI WorldGUI = new WorldWithUI(new World(System.currentTimeMillis()));
        Console c = new Console(WorldGUI);
        c.setVisible(true);
    }


    /**
     * @return name of the simulation
     */
    public static String getName() {
        return "Disaster";
    }


    /**
     *  This must be included to have model tab, which allows mid-simulation
     *  inspection.
     */
    @Override
	public Object getSimulationInspectedObject() {
        return state;
    }  // non-volatile

    
    /**
     * Set up inspector
     * No inspectors at this point
     * There is a bug in GeoMason 1.5 -- the slop for agents on the grid does not scale to the mouse click
     * Click inspectors works inconsistently
     */
    
    
    /**
     * Called when first beginning a WorldWithUI. Sets up the display window,
     * the JFrames, and the chart structure.
     */
    @Override
	public void init(Controller c) {
        
    	super.init(c);

        // make the displayer
        display = new Display2D(1200, 900, this);
        displayFrame = display.createFrame();
        displayFrame.setTitle("World Display");
        c.registerFrame(displayFrame); // register the frame so it appears in
       
        // the "Display" list
        displayFrame.setVisible(true);

        display.attach(tractsPortrayal, "Census Tracts");
        display.attach(waterPortrayal, "Water");
        display.attach(roadsPortrayal, "Roads");
        display.attach(indvPortrayal, "Individuals");
        display.attach(groupPortrayal, "Groups");  
        
        //Legend -- needs to be edited/scaled from Dadaab size to disaster
        Dimension dl = new Dimension(150,360);
        Legend legend = new Legend();
        legend.setSize(dl);
        
        JFrame legendframe = new JFrame();
        legendframe.setVisible(false);
        legendframe.setPreferredSize(dl);
        legendframe.setSize(150, 300);
        
        legendframe.setBackground(Color.white);
        legendframe.setTitle("Legend");
        legendframe.getContentPane().add(legend);   
        legendframe.pack();
        c.registerFrame(legendframe);
        
        // Behavior Chart
        indvsBehaviorChart = new TimeSeriesChartGenerator();
        indvsBehaviorChart.setTitle("Agents' Locations");
        indvsBehaviorChart.setYAxisLabel("Counts");
        indvsBehaviorChart.setXAxisLabel("Steps");
        JFrame chartFrame = indvsBehaviorChart.createFrame(this);
        chartFrame.pack();
        controller.registerFrame(chartFrame); 
        
        //Health Chart
        indvsHealthChart = new TimeSeriesChartGenerator();
        indvsHealthChart.setTitle("Agents' Health");
        indvsHealthChart.setYAxisLabel("Counts");
        indvsHealthChart.setXAxisLabel("Steps");
        JFrame HchartFrame = indvsHealthChart.createFrame(this);
        HchartFrame.pack();
        controller.registerFrame(HchartFrame);   
        
    }


    /**
     * Called when starting a new run of the simulation. Sets up the portrayals
     * and chart data.
     */
    @Override
	public void start()
    {
        super.start();
        
        setupPortrayals();
        setupCharts();
        
        System.out.println("WorldWithUI>ready for user input through control panel. (beep!)");
        Toolkit.getDefaultToolkit().beep();  // generate beep

    }
    

    /**
     * called when quitting a simulation. Does appropriate garbage collection.
     */
    @Override
	public void quit()
    {
        super.quit();

        if (displayFrame != null)
        {
            displayFrame.dispose();
        }
        displayFrame = null; // let gc
        display = null; // let gc
    }
    
    
    
	//======================================
	//
	//     PORTRAYALS
	//
	//======================================
    
    
    /**
     * Portrayals
     * Includes: map layer boundary data and agents
     */
    private void setupPortrayals() {
    	
    	System.out.println("WorldWithUI>setupPortrayals>");
    	World world = (World)state;
    		
        roadsPortrayal.setField(world.roads);
        roadsPortrayal.setPortrayalForAll(new GeomPortrayal(road, 0.0005, false));

        tractsPortrayal.setField(world.censusTracts);
        tractsPortrayal.setPortrayalForAll(new GeomPortrayal(census, true));

        waterPortrayal.setField(world.waterField);
        waterPortrayal.setPortrayalForAll(new GeomPortrayal(water, true));
          
        indvPortrayal.setField(world.indvs);
        indvPortrayal.setPortrayalForAll(new indvGeomPortrayal());

        if (!Log.grpList.isEmpty()) {
	        groupPortrayal.setField(world.groups);
	        groupPortrayal.setPortrayalForAll(new groupGeomPortrayal());
        }
        
        display.reset();
        display.setBackdrop(Color.WHITE);
        display.repaint();		
    }

    
    
	//======================================
	//
	//     CHARTS
	//
	//======================================
    
    
    /**
     * Charts
     */
    private void setupCharts() {
    	System.out.println("WorldWithUI>setupCharts>");
    	
    	// Set up IndvsBehavior Chart 
    	indvsPopulation = new XYSeries("Indvs Population");
        indvsNotWorking = new XYSeries("Indvs Staying Home");  // Monitors.stayhome
        indvsToSchoolDaycare = new XYSeries("Agents in school or daycare");  // Monitors.toSchoolDaycare
        indvsAtHome = new XYSeries("Agents at Home");	// Monitors.atHomeCount
        indvsCommuting = new XYSeries("Agents Commuting to Work");	// Monitors.onCommuteCount
        indvsAtWork = new XYSeries("Agents at Work");	// Monitors.atWorkCount;
        indvsDead = new XYSeries("Agents Dead");		// Monitors.agentDeaths;
        indvsFleeing = new XYSeries("Agents Fleeing"); 		// Monitors.victimsHeadedHome
        indvsFleeingHome = new XYSeries("Agents Fleeing to Home");  // Monitors.victimsHeadedHome
        indvsResponding = new XYSeries("First Responders");  // Monitors.inZoneFirstResponders
        indvsBlocked = new XYSeries("Agents Blocked"); 		// Monitors.agentsBlocked
        
        indvsBehaviorChart.removeAllSeries();
        indvsBehaviorChart.addSeries(indvsPopulation, null);
        indvsBehaviorChart.addSeries(indvsNotWorking, null);
        indvsBehaviorChart.addSeries(indvsToSchoolDaycare, null);
        indvsBehaviorChart.addSeries(indvsAtHome, null);
        indvsBehaviorChart.addSeries(indvsCommuting, null);
        indvsBehaviorChart.addSeries(indvsAtWork, null);
        indvsBehaviorChart.addSeries(indvsDead, null);
        indvsBehaviorChart.addSeries(indvsFleeingHome, null);
        indvsBehaviorChart.addSeries(indvsResponding, null);
        indvsBehaviorChart.addSeries(indvsBlocked, null);
        
        // Set up IndvsHealth Chart 
        indvsHCat0 = new XYSeries("Category 0");
        indvsHCat1 = new XYSeries("Category 1");
        indvsHCat2 = new XYSeries("Category 2");
        indvsHCat3 = new XYSeries("Category 3");
        indvsHCat4 = new XYSeries("Category 4");
        indvsHCat5 = new XYSeries("Category 5");
        indvsHCat6 = new XYSeries("Category 6");
        indvsHCat7 = new XYSeries("Category 7");
        indvsHCat8 = new XYSeries("Category 8");
        indvsHCat9 = new XYSeries("Category 9");
        indvsHCat10 = new XYSeries("Category 10");
        
        indvsHealthChart.removeAllSeries();
        indvsHealthChart.addSeries(indvsHCat0, null);
        indvsHealthChart.addSeries(indvsHCat1, null);
        indvsHealthChart.addSeries(indvsHCat2, null);
        indvsHealthChart.addSeries(indvsHCat3, null);
        indvsHealthChart.addSeries(indvsHCat4, null);
        indvsHealthChart.addSeries(indvsHCat5, null);
        indvsHealthChart.addSeries(indvsHCat6, null);
        indvsHealthChart.addSeries(indvsHCat7, null);
        indvsHealthChart.addSeries(indvsHCat8, null);
        indvsHealthChart.addSeries(indvsHCat9, null);
        indvsHealthChart.addSeries(indvsHCat10, null);

    	
    	state.schedule.scheduleRepeating(new Steppable()  {
        
    		@Override
			public void step (SimState state) {
    			
            	double steps = state.schedule.getSteps();
           	
            	indvsPopulation.add(steps, state.schedule.getSteps());
            	indvsNotWorking.add(steps, Log.stayAtHome, true);
            	indvsToSchoolDaycare.add(steps, Log.toSchoolDaycare, true);
            	indvsAtHome.add(steps, Log.atHomeCount, true);
            	indvsCommuting.add(steps, Log.onCommuteCount, true);
            	indvsAtWork.add(steps, Log.atWorkCount, true);
            	indvsDead.add(steps, Log.indvDeaths, true);
            	indvsFleeingHome.add(steps,Log.affectedHeadedHome, true);
            	indvsResponding.add(steps,Log.inZoneFirstResponders, true);
            	indvsBlocked.add(steps,Log.agentsBlocked, true);
            	
            	indvsHCat0.add(steps, Log.healthCat[0], true);
            	indvsHCat1.add(steps, Log.healthCat[1], true);
            	indvsHCat2.add(steps, Log.healthCat[2], true);
            	indvsHCat3.add(steps, Log.healthCat[3], true);
            	indvsHCat4.add(steps, Log.healthCat[4], true);
            	indvsHCat5.add(steps, Log.healthCat[5], true);
            	indvsHCat6.add(steps, Log.healthCat[6], true);
            	indvsHCat7.add(steps, Log.healthCat[7], true);
            	indvsHCat8.add(steps, Log.healthCat[8], true);
            	indvsHCat9.add(steps, Log.healthCat[9], true);
            	indvsHCat10.add(steps, Log.healthCat[10], true); 
            	
            	int step = (int) steps;
            	
            	if ( ((step % 240 ) == 0) ) //&& (steps > Parameters.tDetonation))
            		System.out.println("text              Step wPop stayH kids atHome commuting atWork fleeing first dead zone2 zone3 blocked");
            	
            	if ( (steps % 30 == 0) && (steps < Parameters.tDetonation) ) //> Parameters.tDetonation ) 
        		   System.out.println("WorldWithUI>step>" 
        				   + " " + step 
        				   + " " + Log.agentpopulation
        				   + " " + Log.stayAtHome
        				   + " " + Log.toSchoolDaycare
        				   + " " + Log.atHomeCount
        				   + " " + Log.onCommuteCount 
        				   + " " + Log.atWorkCount );
        		
            	if ( (steps % 30 == 0) && (steps > Parameters.tDetonation) ) //> Parameters.tDetonation ) 
            		System.out.println("WorldWithUI>step>" 
            				+ " " + step
            				+ " " + Log.agentpopulation
            				+ " " + Log.stayAtHome
            				+ " " + Log.toSchoolDaycare
            				+ " " + Log.atHomeCount
            				+ " " + Log.onCommuteCount 
            				+ " " + Log.atWorkCount 
            				+ " " + Log.affectedFleeing
            				+ " " + Log.affectedHeadedHome
            				+ " " + Log.firstResponders	
            				+ " " + Log.indvDeaths
            				+ " " + Log.popZone2
            				+ " " + Log.popZone3
            				+ " " + Log.agentsBlocked);
            	
            	if ( (steps > Parameters.tDetonation) )
            		System.out.println("step--- " + step);
    		}    
    			
        });  // end scheduleRepeating
    		
    }
    
    
    
	//======================================
	//
	//     ALTERNATIVE PORTRAYALS
	//
	//======================================
    

    /**
     * Individual Portrayal
     */
    public class indvGeomPortrayal extends GeomPortrayal {
    	
    		public Paint determineColor(Object object) {		
	    		// determines color by healthstatus of the agent	
	    		int hColor = ((Indv) ((MasonGeometry)object).getUserData()).getHealthStatus();
	    		
				if (hColor == 0)	{ scale=0.0009; return Color.red; }  // testing agent has health = 0
				else if (hColor == 99)		{ scale=0.0008; return Color.blue; }	// first responder
				else if (hColor == 98)		{ scale=0.0008; return aiding; }		// first responder providing aid
				else if (hColor == 80)		{ scale=0.0005; return new Color(220, 50, 190); }		// pink agent is on a damaged edge
				else if (hColor == 89)		{ scale=0.0005; return new Color(162, 50, 179); }		// purple agent is rerouting around the damaged area
				else if (hColor == 72)		{ scale=0.0005; return Color.gray; }	// blocked
				else if (hColor < 2)  		{ return healthy; } // assume healthy first...to reduce computation
				else if (hColor == 10)	{ return dead; }
				else if (hColor >= 5)  	{ return lethal; }
				else if (hColor > 1)  	{ return sickMed; }  // future coloring may include other health
				if (hColor == -1)		{ scale=0.0005; return homeward; }  // temporary for verification, etc.
				else return healthy; // default to healthy
    		}
    		
    		@Override
			public void draw (Object object, Graphics2D graphics, DrawInfo2D info) {
    			scale = 0.0005; // set scale of the portrayal (0.0003 or 0.0005)  // for commuter region
//    			scale = 0.003; // for entire region
    			paint = determineColor(object);
    			super.draw(object, graphics, info);
    		} 		
    }

    
    /**
     * Group Portrayal
     */
    public class groupGeomPortrayal extends GeomPortrayal {
    	
    		public Paint determineColor(Object object) {		
	    		// determines color by healthstatus of the agent	
	    		int hColor = ((Group) ((MasonGeometry)object).getUserData()).getHealthStatus();
	    		
				if (hColor == 0)	{ scale=0.0009; return Color.red; }  // testing agent has health = 0
				else if (hColor == 99)		{ scale=0.0008; return Color.blue; }	// first responder
				else if (hColor == 98)		{ scale=0.0008; return aiding; }		// first responder providing aid
				else if (hColor == 80)		{ scale=0.0008; return new Color(220, 50, 190); }		// pink agent is on a damaged edge
				else if (hColor == 89)		{ scale=0.0008; return new Color(162, 50, 179); }		// purple agent is rerouting around the damaged area
				else if (hColor == 72)		{ scale=0.0005; return Color.gray; }	// blocked
				else if (hColor < 2)  		{ return healthy; } // assume healthy first...to reduce computation
				else if (hColor == 10)	{ return dead; }
				else if (hColor >= 5)  	{ return lethal; }
				else if (hColor > 1)  	{ return sickMed; }  // future coloring may include other health
				if (hColor == -1)		{ scale=0.0005; return homeward; }  // temporary for verification, etc.
				else return healthy; // default to healthy
				
    		}
    		
    		@Override
			public void draw (Object object, Graphics2D graphics, DrawInfo2D info) {
    			scale = 0.0009; // set scale of the portrayal (0.0003 or 0.0005)  // for commuter region
    			paint = determineColor(object);
    			super.draw(object, graphics, info);
    		} 		
    }
       
    
}
