/**
 * Disaster ABM in MASON
 * @author Annetta Burger & Bill Kennedy
 * Legend Class based on code from Dadaab ABM by Ates
 * 
 */

package disaster;

import java.awt.*;
import java.awt.Color;
import java.awt.geom.Line2D;


//=========================
/** 
* Legend Class to paint the model legend display
*/
//=========================

public class Legend extends Canvas {
    
    /**
     * Color schemes for display
     * Derived from ColorBrewer: http://colorbrewer2.org/
     */
    // Background layers
    final Color road = new Color (255,255,255);		// white
    final Color census = new Color (240,240,240); 	//(246,239,199);
    final Color water = new Color (212,225,237); 	//slightly darker than (222,235,247);
    
    // Buildings
    final Color bldg = new Color (82,82,82);  		// generic buildings can be coded dark gray;
    final Color home = bldg;						// dark gray;
    final Color work = new Color (102,37,6);		// dark brown
    final Color school = new Color (8,48,107);		// light blue
    final Color dCare = new Color (73,0,106);		// pink
    
    // Agent health
    final Color healthy  = new Color (35,132,67);   // green //(65,171,93);		// lighter green
	final Color sickLow  = new Color(253,141,60); 	// orange
	final Color sickMed  = new Color(253,141,60); 	// orange
	final Color sickHigh = new Color(189,0,38);		// orange
	final Color lethal   = new Color(189,0,38);	 	// red
	final Color dead     = new Color(0,0,0); 		// black    
	final Color stopped  = Color.gray; 				// gray    
	final Color responder= Color.blue; 				// blue 
	final Color homeward = new Color(100,250,100);  // light green

		
	//========================
	/**
	 * Method to paint legend
	 */
	//========================
    @Override
	public void paint(Graphics legend)
    {
    	// general
        	Graphics2D leg = (Graphics2D)legend;
        	leg.scale(0.6, 0.6);	// sets the size of window with 3 groupings 
        							// (0.7,0.7) works for 2 groupings
        	       
        //==========================
        // title and first grouping
	        Font f = new Font("Serif", Font.BOLD, 24);    	// font f   
	        leg.setFont(f);   
	        leg.setColor(Color.black);
	        leg.drawString("LEGEND", 60, 40);
	
	        // Symbols
	        // road
	        Line2D line = new Line2D.Double(20, 60, 70, 70);
	        leg.setColor(road);
	        leg.setStroke(new BasicStroke(3));
	        leg.draw(line);               
	        // water      
	        legend.setColor(water);
	        legend.fillRect(25, 100, 20, 20);
	
	        // labels
	        Font f3 = new Font("Serif", Font.PLAIN, 20);    	// font f3
	        leg.setFont(f3);         
	        leg.setColor(Color.black);      
	        leg.drawString("Road",     90, 80);
	        legend.drawString("Water", 90, 115);

        //==============================
        // agent health status grouping
	        Font f2 = new Font("Serif", Font.BOLD, 18);    		// font f2       
	        leg.setFont(f2);        
	        leg.setColor(Color.black); 
	        leg.drawString("Agent Health Status", 20, 145);
	
	        // Symbols
	        int s1 = 155;			// plot height for symbols
	        leg.setColor(healthy);
	        leg.fillOval(25, s1,    20, 20);
	        leg.setColor(sickMed); //244, 165, 130
	        leg.fillOval(25, s1+30, 20, 20);
	        leg.setColor(lethal);
	        leg.fillOval(25, s1+60, 20, 20);
	        leg.setColor(dead);
	        leg.fillOval(25, s1+90, 20, 20);      
	
	        // labels
	        leg.setFont(f3);         
	        leg.setColor(Color.black);
	        int l1 = s1+15; 			// plot height for labels
	        leg.drawString("Healthy", 70, l1);
	        leg.drawString("Injured", 70, l1+30);
	        leg.drawString("Dying",   70, l1+60);
	        leg.drawString("Dead",    70, l1+90);

        //==============================
        // agent behavior grouping
	        leg.setFont(f2);        
	        leg.setColor(Color.black); 
	        leg.drawString("Agent Behavior", 20, 290);
	
	        // Symbols
	        int s2 = 300;			// plot height for symbols
	        leg.setColor(stopped);
	        leg.fillOval(25, s2,    20, 20);      
	        leg.setColor(responder);
	        leg.fillOval(25, s2+30, 20, 20);      
	        leg.setColor(homeward);
	        leg.fillOval(25, s2+60, 20, 20);      
	
	        // labels
	        int l2 = s2+15; 			// plot height for labels
	        leg.setFont(f3);         
	        leg.setColor(Color.black);              
	        leg.drawString("Blocked",   70, l2);
	        leg.drawString("Responder", 70, l2+30);
	        leg.drawString("Victim",    70, l2+60);
        
        //==============================
        // buildings grouping
	        leg.setFont(f2);        
	        leg.setColor(Color.black); 
	        leg.drawString("Buildings", 20, 405);
	
	        // Symbols
	        int s3 = 415;			// plot height for symbols
	        leg.setColor(home);       
	        leg.fillRect(25, s3,  20, 20);  //470        
	        leg.setColor(work);       
	        leg.fillRect(25, s3+30,  20, 20);  //510
	        leg.setColor(school);       
	        leg.fillRect(25, s3+60,  20, 20);  //550
	        leg.setColor(dCare);       
	        leg.fillRect(25, s3+90, 20, 20);  //590
	        
	        // labels
	        int l3 = s3+15; 			// plot height for labels
	        leg.setFont(f3);         
	        leg.setColor(Color.black);      
	        leg.drawString("Home",     70, l3);
	        leg.drawString("Work",     70, l3+30);
	        leg.drawString("School",   70, l3+60);
	        leg.drawString("Day Care", 70, l3+90);
              
    }

}


