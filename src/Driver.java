import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.AffineTransformOp;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

import java.math.*;



/**
 * 
 * @author good0161
 * @version 5.0.1
 * Designs and creates an executable jar file to display the entire trip on a tripMap
 */

public class Driver {
	
	// Declare class data
	private static JFrame proFrame;
	private static JPanel topPanel;
	private static JButton play;
	private static JCheckBox enStop;
	private static JComboBox<String> aniTime;
	private static JMapViewer tripMap;
	private static int speed = 15;
	private static boolean showStops = false;
	private static Timer timer;
	private static BufferedImage marker = null;
	private static int numStops;
	private static AffineTransform rotate = new AffineTransform();
	
	
    public static void main(String[] args) throws FileNotFoundException, IOException {

    	// Read file and call stop detection
    	
    	TripPoint.readFile("triplog.csv");
    	numStops = TripPoint.h2StopDetection();
    	ArrayList<TripPoint> trip = TripPoint.getTrip();
    	ArrayList<TripPoint> stops = new ArrayList<>(trip);
    	ArrayList<TripPoint> movingTrip = TripPoint.getMovingTrip();
    	stops.removeAll(movingTrip);
    	marker = ImageIO.read(new File("arrow.png"));
    	
    	// Set up frame, include your name in the title
    	proFrame = new JFrame("Project 5 - Andrew Goodspeed");
        proFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        proFrame.setSize(1920, 1080);
        proFrame.setLayout(new BorderLayout());
    
        // Set up Panel for input selections
        topPanel = new JPanel();
    	
        // Play Button
        play = new JButton("Play");
    	
        // CheckBox to enable/disable stops
        enStop = new JCheckBox("Include Stops");

        // ComboBox to pick animation time
        String[] Times = {"Animation Time","15", "30", "60", "90"};
        aniTime = new JComboBox<String>(Times);
        aniTime.setSelectedItem(0);
        
    	aniTime.setEditable(false);
    	
        // Add all to top panel
        topPanel.add(play);
        topPanel.add(enStop);
        topPanel.add(aniTime);
        proFrame.add(topPanel, BorderLayout.NORTH);
        
        // Set up tripMap
        tripMap = new JMapViewer();
        tripMap.setTileSource(new OsmTileSource.TransportMap());
        
        proFrame.add(tripMap, BorderLayout.CENTER);
        
        // Add listeners for GUI components
        play.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent e) {
        		if(timer != null) 
        			timer.stop();
				Play(trip,stops, movingTrip);

        	}

        });
        enStop.addActionListener(new ActionListener() {
        	@Override
        	public void actionPerformed(ActionEvent e) {
        		if(enStop.isSelected())
        			showStops = true;
        		else
        			showStops = false;
        	}
        });
        aniTime.addItemListener(new ItemListener() {
        	@Override
        	public void itemStateChanged(ItemEvent e) {
        		if(e.getStateChange() == ItemEvent.SELECTED) {
        			String sel = (String) aniTime.getSelectedItem();
        			if(aniTime.getSelectedIndex() != 0) {
        				speed = Integer.parseInt(sel);
        			}        			
        		}
        	}
        });
        

        // Set the tripMap center and zoom level
        tripMap.setDisplayPosition(new Coordinate(35,-110), 6);
        proFrame.setVisible(true);
    }
    // Animate the trip based on selections from the GUI components


    private static void Play(ArrayList<TripPoint> Trip, ArrayList<TripPoint> Stops, ArrayList<TripPoint> MovingTrip){
    	
    	tripMap.removeAllMapMarkers();
    	tripMap.removeAllMapPolygons();
    	
    	List<Coordinate> line = new ArrayList<Coordinate>();
    	List<Coordinate> dot = new ArrayList<Coordinate>();
    	
    	
    	
    	Graphics g2d = (Graphics2D) proFrame.getGraphics();
    	g2d.setColor(Color.RED);
    	//Graphics2D img = marker.createGraphics();
    	int period = 1000;
    		
    	for(TripPoint y: Stops)
    		dot.add(new Coordinate(y.getLat(),y.getLon()));
    	
    	final int[] current = {0};
    	final int[] stopCount = {0};
    	final MapMarker[] prev = new MapMarker[1];
    	
    	if(showStops) {
    		for(TripPoint x: Trip) 
        		line.add(new Coordinate(x.getLat(),x.getLon()));
    		period = (speed * 1000) / (Trip.size());
    	}
    	else
    		for(TripPoint x: MovingTrip) 
        		line.add(new Coordinate(x.getLat(),x.getLon()));
    		period =  (speed * 1000) / (line.size());

    	timer = new Timer(period, new ActionListener() {
    			@Override
    			public void actionPerformed(ActionEvent e) {
    				
    				if(current[0] < line.size()-1) {
    					Coordinate from = line.get(current[0]);
    					Coordinate to = line.get(current[0] + 1);
    					
    					double toa = (to.getLat() - from.getLat()) / (to.getLon() - from.getLon());
    					BufferedImage reset = marker;
    					marker = rotate(marker, Math.toRadians(Math.atan(toa)));

    					
	    		    	MapPolygonImpl line = new MapPolygonImpl(from,from ,to);
	    		    	MapMarker pnt = new IconMarker(to , marker);
	    		    	marker = reset;
	    		    	
	    		    	line.setColor(Color.RED);
	    		    	tripMap.removeMapMarker(prev[0]);
	    		    	tripMap.addMapMarker(pnt);
	    		    	tripMap.addMapPolygon(line);
	    		    	prev[0] = pnt;
	    		    	if(showStops && stopCount[0] < numStops) { 
	    		    		Coordinate stop = dot.get(stopCount[0]);
	    		    		if(to.equals(stop)){
	    		    			MapMarkerDot stopMark = new MapMarkerDot(stop);
	    		    			stopMark.setBackColor(Color.RED);
	    		    			stopMark.setColor(Color.RED);
	    		    			
	    		    			tripMap.addMapMarker(stopMark);
	    		    			stopCount[0]++;
	    		    		}
	    		    	}
	    		    	
    				}
    				
    				current[0]++;
    				
    			}
    	});
    	timer.setInitialDelay(0);
    	timer.start();
    }
    public static BufferedImage rotate(BufferedImage img, Double angle)
    {
 
        // Getting Dimensions of image
        int width = img.getWidth();
        int height = img.getHeight();
 
        // Creating a new buffered image
        BufferedImage newImage = img;
 
        // creating Graphics in buffered image
        Graphics2D g2 = newImage.createGraphics();
 
        // Rotating image by degrees using toradians()
        // method
        // and setting new dimension t it

        rotate.rotate(angle);
        AffineTransformOp op = new AffineTransformOp(rotate ,null);
        g2.drawImage(img, op, 0, 0);
 
        // Return rotated buffer image
        return newImage;
    }
 
   
   
}