/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.ui.main;

import static rotp.model.galaxy.Ship.EMPIRE_ID;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import rotp.model.Sprite;
import rotp.model.empires.Empire;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.Nebula;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.model.game.IGameOptions;
import rotp.model.game.IMapOptions;
import rotp.model.tech.TechCategory;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.UserPreferences;
import rotp.ui.history.HistoryUI.GalaxyMapPane;
import rotp.ui.map.IMapHandler;
import rotp.ui.sprites.FlightPathDisplaySprite;
import rotp.ui.sprites.FlightPathSprite;
import rotp.ui.sprites.GridCircularDisplaySprite;
import rotp.ui.sprites.OptionsWidgetSprite;
import rotp.ui.sprites.RangeDisplaySprite;
import rotp.ui.sprites.ShipDisplaySprite;
import rotp.ui.sprites.SpyReportSprite;
import rotp.ui.sprites.SystemNameDisplaySprite;
import rotp.ui.sprites.TechStatusSprite;
import rotp.ui.sprites.TreasurySprite;
import rotp.ui.sprites.ZoomInWidgetSprite;
import rotp.ui.sprites.ZoomOutWidgetSprite;

public class GalaxyMapPanel extends BasePanel implements IMapOptions, ActionListener, MouseListener, MouseWheelListener, MouseMotionListener {
    private static final long serialVersionUID = 1L;
    private static final int NO_TARGET = -999;
    
    // BR These values may be changed remotely!
    public static int MAX_FLAG_SCALE			= 80;
    public static int MAX_STARGATE_SCALE 		= 40;
    public static int MAX_RALLY_SCALE 			= 100;
    public static int MAX_FLEET_UNARMED_SCALE	= 40;
    public static int MAX_FLEET_TRANSPORT_SCALE = 60;
    public static int MAX_FLEET_SMALL_SCALE		= 60;
    public static int MAX_FLEET_LARGE_SCALE		= 80;
    public static int MAX_FLEET_HUGE_SCALE		= 100;
    private static boolean debugShowAll = false;
	// \BR:
    public static Color gridLight = new Color(160,160,160);
    public static Color gridDark = new Color(64,64,64);

    public static void maxFlagScale(int val)			{ MAX_FLAG_SCALE	 = val; }
    public static void maxStargateScale(int val)		{ MAX_STARGATE_SCALE = val; }
    public static void maxRallyScale(int val)			{ MAX_RALLY_SCALE	 = val; }
    public static void maxFleetUnarmedScale(int val)	{ MAX_FLEET_UNARMED_SCALE   = val; }
    public static void maxFleetTransportScale(int val)	{ MAX_FLEET_TRANSPORT_SCALE = val; }
    public static void maxFleetSmallScale(int val)		{ MAX_FLEET_SMALL_SCALE = val; }
    public static void maxFleetLargeScale(int val)		{ MAX_FLEET_LARGE_SCALE = val; }
    public static void maxFleetHugeScale(int val)		{ MAX_FLEET_HUGE_SCALE  = val; }

    private final IMapHandler parent;

    // static fields shared across all galaxy map panels to keep them 
    // visually in synch
    private static Location center = new Location();
    private static float scaleX, scaleY;
    private static float sizeX, sizeY;
    private static final List<Sprite> baseControls = new ArrayList<>();

    private float desiredScale;
    private Image mapBuffer;
    public static BufferedImage sharedStarBackground;
    private static BufferedImage sharedNebulaBackground;
    private final float zoomBase = 1.1f;
    private boolean dragSelecting = false;
    public boolean historyMode = false;
    private int selectX0, selectY0, selectX1, selectY1;
    private int lastMouseX, lastMouseY;
    private long lastMouseTime;
    private boolean redrawRangeMap = true;
    public Sprite hoverSprite;
    private int backOffsetX = 0;
    private int backOffsetY = 0;
    private float areaOffsetX = 0;
    private float areaOffsetY = 0;
    private Area shipRangeArea;
    private Area scoutRangeArea;
    private Area darkRangeArea;
    private int maxMouseVelocity = -1;
    private boolean searchingSprite = false;

    private final Timer zoomTimer;

    private static boolean warView = false;
    private static int targetSysId = NO_TARGET;
    private boolean lastHoverAltDown = false;

    
    public static boolean isWarView()	 	{ return warView; }
    public static void toggleWarView()	 	{ warView = !warView; }
    private void clearWarView()				{
		targetSysId	= NO_TARGET;
		warView		= false;
    }
    private boolean setTargetSys(int sysId)	{
    	List<StarSystem> systems = player().orderedUnderAttackSystems(true, true);
    	StarSystem sys = galaxy().system(sysId);
    	if (systems.contains(sys)) {
    		targetSysId = sysId;
    		return true;
    	}
    	clearWarView();
    	return false;
    }
    

    public IMapHandler parent()     { return parent; }
    public boolean showArmedShips() { return true; }
    public void clearHoverSprite()  { hoverSprite = null; }
    private Location currentFocus()  { return parent.mapFocus();  }
    private void currentFocus(IMappedObject o)  { parent.mapFocus(o); }
    private Location center()       { return center; }
    private void center(Location o)  { center = o; }
    private float mapMinX()          { return center.x()-(scaleX*2/5); }
    private float mapMinY()          { return center.y()-(scaleY*2/5); }
    private float mapMaxX()          { return center.x()+(scaleX*3/5); }
    private float mapMaxY()          { return center.y()+(scaleY*3/5); }
    public void centerX(float x)    { center.x(x); }
    public void centerY(float y)    { center.y(y); }
    public float centerX()          { return center.x(); }
    public float centerY()          { return center.y(); }
    public float sizeX()            { return sizeX; }
    public float sizeY()            { return sizeY; }
    private void sizeX(float s)      { sizeX = s; }
    private void sizeY(float s)      { sizeY = s; }

    public float scaleX()           { return scaleX; }
    public float scaleY()           { return scaleY; }
    private void scaleX(float s)     { scaleX = s; }
    private void scaleY(float s)     { scaleY = s; }

    public boolean displays(IMappedObject obj) {
        if (center() == null)
            return true;

        float x = obj.x();
        float y = obj.y();
        return (x >= (mapMinX()*0.9f)) && (x <= (mapMaxX()*1.1f)) && (y >= (mapMinY()*0.9f)) && (y <= (mapMaxY()*1.1f));
    }
    public GalaxyMapPanel(IMapHandler p) {
        parent = p;
        zoomTimer = new Timer(10, this);
        init0();
    }
    private void init0() {
        setBackground(Color.BLACK);
        setOpaque(true);

//	Do Not initialize these, keep the former values!
//        flightPathDisplay = parent.defaultFleetDisplay();
//        shipDisplay = SHOW_ALL_SHIPS;
//        showShipRanges = parent.defaultShipRangesDisplay();
//        showGridCircular = parent.defaultGridCircularDisplay();

        if (baseControls.isEmpty()) {
            baseControls.add(new OptionsWidgetSprite(10,295,30,30));
            baseControls.add(new ZoomOutWidgetSprite(10,260,30,30));
            baseControls.add(new ZoomInWidgetSprite(10,225,30,30));
            baseControls.add(new RangeDisplaySprite(10,190,30,30));
            baseControls.add(new GridCircularDisplaySprite(10,155,30,30));
            baseControls.add(new FlightPathDisplaySprite(10,120,30,30));
            baseControls.add(new ShipDisplaySprite(10,85,30,30));
            baseControls.add(new SystemNameDisplaySprite(10,50,30,30));

            int y0 = unscaled(getHeight())-310;
            baseControls.add(new TechStatusSprite(TechCategory.WEAPON,       10,y0+210,30,30));
            baseControls.add(new TechStatusSprite(TechCategory.PROPULSION,   10,y0+175,30,30));
            baseControls.add(new TechStatusSprite(TechCategory.PLANETOLOGY,  10,y0+140,30,30));
            baseControls.add(new TechStatusSprite(TechCategory.FORCE_FIELD,  10,y0+105, 30,30));
            baseControls.add(new TechStatusSprite(TechCategory.CONSTRUCTION, 10,y0+70, 30,30));
            baseControls.add(new TechStatusSprite(TechCategory.COMPUTER,     10,y0+35, 30,30));
            baseControls.add(new TreasurySprite(10,y0, 30,30));
            baseControls.add(new SpyReportSprite(10,y0-70, 30,30));
        }
        
        addMouseListener(this);
        addMouseWheelListener(this);
        addMouseMotionListener(this);
    }
    public void repaintTechStatus() {
        int y = getHeight()-scaled(275);
        this.repaint(s10,y,s30,scaled(205));
    }
    // scale(float) will translate any arbitrary "real" distance
    // into a map distance in pixels
    public int scale(float d) {
        return (int) (d*this.getSize().width/scaleX());
    }
    // objX(int) and objY(int) will translate any arbitrary on-screen
    // map coordinates into "real" coordinates
    private float objX(int x) {
        int w = getSize().width;
        return mapMinX()+(scaleX()*x/w);
    }
    private float objY(int y) {
        int h = getSize().height;
        return mapMinY()+(scaleY()*y/h);
    }
    // mapX(float) and mapY(float) will translate any arbitrary "real"
    // coordinates into map coordinates
    public int mapX(float x) {
        float rX = (x-center.x())/scaleX()+.4f;
        return (int) (rX*getSize().width);
    }
    public int mapY(float y) {
        float rY = (y-center.y())/scaleY()+.4f;
        return (int) (rY*getSize().height);
    }
    private float fMapX(float x) {
        float rX = (x-center.x())/scaleX()+.4f;
        return rX*getSize().width;
    }
    private float fMapY(float y) {
        float rY = (y-center.y())/scaleY()+.4f;
        return rY*getSize().height;
    }
    public void setBounds(float x1, float x2, float y1, float y2) {
        float scaleFromY = y2-y1;
        float scaleFromX = (x2-x1)*getSize().height/getSize().width;

        setScale(max(scaleFromY, scaleFromX));
        center().x((x1+x2)/2);
        center().y((y1+y2)/2);
    }
    public void setScale(float scale) {
        int mapSizeX = getSize().width;
        int mapSizeY = getSize().height;
        if (scaleY != scale) {
            clearRangeMap();
            resetRangeAreas();
        }
        scaleY(scale);
        scaleX(scale*mapSizeX/mapSizeY);
    }
    public void initBenchmark() {
    	IGameOptions opts = options();
    	debugShowAll = opts.debugBMShowAll();
    	if (opts.debugBMZoomOut()) {
    		centerX(sizeX()/2);
        	centerY(sizeY()/2);
        	setScale(maxScale());
    	}
    }
    private float maxScale() {
        float largestAxis = Math.max(sizeX(), sizeY());
        return galaxy().maxScaleAdj()*largestAxis;
    }
    @Override
    public void paintComponent(Graphics g) {
    	// debugShowAll = true; // TO DO BR: COMMENT
    	// debugShowAll = false; // TO DO BR: COMMENT
        Graphics2D g2 = (Graphics2D) g;
        setFontHints(g2);
        parent.checkMapInitialized();
        paintToImage(mapBuffer());
        g2.drawImage(mapBuffer,0,0,null);
        parent.paintOverMap(this, g2);
    }
    private void paintToImage(Image img) {
        Graphics2D g2 = (Graphics2D) img.getGraphics();
        IGameOptions opts	 = options();
        boolean darkShowSpy  = false;
        boolean darkShowName = false;
        boolean darkGalaxy	 = opts.selectedDarkGalaxy();
        super.paintComponent(g2); //paint background

        Shape clipOld = g2.getClip();
        if (darkGalaxy) { // BR: add mask for dark galaxy mode
            if (darkRangeArea == null)
            	initDarkRangeArea();
            if (darkRangeArea != null) {
            	g2.setColor(Color.black);
            	g2.fillRect(0, 0, img.getWidth(null), img.getHeight(null));
            	darkShowSpy = opts.darkGalaxySpy();
            	darkShowName = darkShowSpy || opts.darkGalaxyNoSpy();
            }
        }
        setScale(scaleY());
        	
        //log("map scale:", fmt(scaleX(),2), "@", fmt(scaleY(),2), "  center:", fmt(center().x(),2), "@", fmt(center().y(),2), "  x-rng:", fmt(mapMinX()), "-", fmt(mapMaxX(),2), "  y-rng:", fmt(mapMinY()), "-", fmt(mapMaxY(),2));
        //drawBackground(g2); // modnar: not needed due to drawShipRanges below
        Empire emp = parent.empireBoundaries();
        boolean isPlayer = emp.isPlayer();
        isPlayer = true;
        if (isPlayer)
        	drawShipRanges(g2);
       	if (darkGalaxy)
       		g2.setClip(darkRangeArea);
        if (!isPlayer)
        	drawShipRanges(g2);
        
    	// BR: check for WarView Mode
    	if (warView) {
    		Sprite clickedSprite = parent.clickedSprite();
    		if (clickedSprite instanceof StarSystem)
    			setTargetSys(((StarSystem) clickedSprite).id);
    		else
    			clearWarView();
    	}

       	// display background stars
        if (parent.drawBackgroundStars() && showStars()) {
            float alpha = 8/5*sizeX()/scaleX();
            Composite prev = g2.getComposite();
            if (alpha < 1) {
                Composite comp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER , alpha);
                g2.setComposite(comp );     
            }
            drawBackgroundStars(g2);
            g2.setComposite(prev);
        }
        // Display Nebulae
        if (UserPreferences.texturesMap())
            drawBackgroundNebula(g2);       
        drawGrids(g2);
        drawNebulas(g2);

       	if (darkGalaxy) {
            g2.setClip(clipOld);
            drawStarSystems(g2);
        	drawEmpireNames(g2, !darkShowName);
            g2.setClip(darkRangeArea);
       	} else {
       		drawStarSystems(g2);
        	drawEmpireNames(g2, false);
       	}

        drawWorkingFlightPaths(g2);
        drawShipsAndPath(g2);
        g2.setClip(clipOld);

        parent.drawYear(g2);
        parent.drawTitle(g2);
        parent.drawAlerts(g2);
        drawControlSprites(g2);
        drawNextTurnSprites(g2);
        drawRangeSelect(g2);
        g2.dispose();
    }
    private Image mapBuffer() {
        if ((mapBuffer == null) || (mapBuffer.getWidth(this) != getWidth()) || (mapBuffer.getHeight(this) != getHeight())) 
            generateNewMapBuffers();
        
        return mapBuffer;
    }
    private void generateNewMapBuffers() {
        int w = getWidth();
        int h = getHeight();
        mapBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        
        if (sharedStarBackground == null) {
            sharedStarBackground = new BufferedImage(RotPUI.instance().getWidth(), RotPUI.instance().getHeight(), BufferedImage.TYPE_INT_ARGB);
            drawBackgroundStars(sharedStarBackground, this);
        }
        if (sharedNebulaBackground == null) {
            sharedNebulaBackground = new BufferedImage(RotPUI.instance().getWidth(), RotPUI.instance().getHeight(), BufferedImage.TYPE_INT_ARGB);
            drawBackgroundNebula(sharedNebulaBackground);
        }
    }
    public void init() {
        resetRangeAreas();
        
        if (UserPreferences.sensitivityMedium())
            maxMouseVelocity = 500;
        else if (UserPreferences.sensitivityLow())
            maxMouseVelocity = 100;
        else
            maxMouseVelocity = -1;
    }
    public void clearRangeMap()   { redrawRangeMap = true; }
    public void resetRangeAreas() {
        clearRangeMap();
        shipRangeArea  = null;
        scoutRangeArea = null;
       	darkRangeArea  = null;
        areaOffsetX = 0;
        areaOffsetY = 0;    	
    }
    private void resetDarkArea() {
        if (options().selectedDarkGalaxy()) {
        	clearRangeMap();
        	resetRangeAreas();
        }
    }
    private void setFocusToCenter() {
        currentFocus(new Location());
        currentFocus().setXY(sizeX()/2, sizeY()/2);
        center(currentFocus());
    }
    public void recenterMapOn(IMappedObject obj) {
    	if (obj != null) { // BR: Just in case!
        	recenterMap(obj.x(), obj.y());
            resetDarkArea();
    	}
    }
    private void recenterMap(float x, float y) {
        float bestX = bounds(0, x, sizeX());
        float bestY = bounds(0, y, sizeY());

        areaOffsetX += (currentFocus().x()-bestX);
        areaOffsetY += (currentFocus().y()-bestY);
        
        currentFocus().setXY(bestX, bestY);
        center(parent.mapFocus());
        
        clearRangeMap();
    }
    public void adjustZoom(int z) {
        int MIN_SCALE = 4;
        if (parent.canChangeMapScales()) {
            float multiplier = pow(zoomBase,z);
            float newScale = scaleY()*multiplier;
            newScale = max(MIN_SCALE, min(2*maxScale(), newScale));
            desiredScale = newScale;
            zoomTimer.start();
        }
    }
    public void initializeMapData() {
        sizeX(galaxy().width());
        sizeY(galaxy().height());
        clearRangeMap();

        setFocusToCenter();

        // try to set starting scale to what parent UI wants
        float largestAxis = max(sizeX(), sizeY());
        float parentStartingScale = parent.startingScalePct()*largestAxis;
        // however, if parent allows us to adjust the scale, then the
        // starting scale must be capped at the maximum adjustable scale
        if (parent.canChangeMapScales()) 
            parentStartingScale = min(parentStartingScale, maxScale());
        
        setScale(parentStartingScale);
    }
    public void maxZoomOut(float sX, float sY) {
    	centerX(sizeX()/2/sX);
    	centerY(sizeY()/2/sY);
    	desiredScale = maxScale();
        zoomTimer.start();
    }
    private void initDarkRangeArea() {
        Empire pl  = player();
        Galaxy gal = galaxy();
        float darkRange  = pl.darkRange();
        if (darkRange > gal.width())
            return;

        // Get extended Star System
        List<StarSystem> systems = pl.systemsForCiv(pl);
        List<StarSystem> alliedSystems = new ArrayList<>();
        for (Empire ally: pl.allies())
            for (StarSystem sys: ally.allColonizedSystems()) {
                if (pl.sv.empId(sys.id) == ally.id)
                    alliedSystems.add(sys);
            }
        float scale = getWidth()/scaleX();
        float darkR = darkRange*scale;
        float darkD = 2*darkR;
       	float shipR = max(1, pl.shipScanningRange())*scale;
        float shipD = 2*shipR;

//        long time1 = System.nanoTime();
        List<Area> toAdd = new ArrayList<>();
        // Add Colony Scan
        for (StarSystem sv: alliedSystems)
            toAdd.add(new Area( new Ellipse2D.Float(fMapX(sv.x())-darkR, fMapY(sv.y())-darkR, darkD, darkD) )); 
        for (StarSystem sv: systems)
            toAdd.add(new Area( new Ellipse2D.Float(fMapX(sv.x())-darkR, fMapY(sv.y())-darkR, darkD, darkD) ));
        // Add Ship Scan
       	List<ShipFleet> fleets = gal.ships.allFleets(pl.id);
       	for (ShipFleet fl: fleets)
       		toAdd.add(new Area( new Ellipse2D.Float(fMapX(fl.x())-shipR, fMapY(fl.y())-shipR, shipD, shipD) ));
        Area tmpRangeArea = parallelAdd(toAdd);
        darkRangeArea = tmpRangeArea;
//        long time2 = System.nanoTime();
//        double ms = (time2-time1) / 1_000_000.0;
//        System.out.format("RRR base %.2f ms\n", ms);
    }
/*    // BR: modnar commented the only call to this method
    private void drawBackground(Graphics2D g) {
        if (showShipRanges()) {
            if (redrawRangeMap) {
                redrawRangeMap = false;
                Graphics2D g0 =  (Graphics2D) rangeMapBuffer.getGraphics();
                setFontHints(g0);
                g0.setColor(unreachableBackground);
                g0.fillRect(0,0,getWidth(),getHeight());
                if (parent.showShipRanges())
                    drawExtendedRangeDisplay(g0);
                drawOwnershipDisplay(g0);
            }
            g.drawImage(rangeMapBuffer,0,0,null);
        }
    } */
	// modnar: make regular ship fuel range cover starry background
 	private void drawShipRanges(Graphics2D g) {
		if (showShipRanges()) {
			if (redrawRangeMap) {
                //redrawRangeMap = false;
				//Graphics2D g0 = (Graphics2D) rangeMapBuffer.getGraphics();
				Graphics2D g0 = (Graphics2D) g.create(); // use create() to not leave afterimage
				setFontHints(g0);
				if(!historyMode)
                	drawExtendedRangeDisplay(g0);
				drawOwnershipDisplay(g0);
			}
        }
	}

    private void drawGrids(Graphics2D g) {
        if (showGridCircular()) {
            drawGridCircularDisplayDark(g);
            //drawGridCircularDisplayLight(g);
        }
    }
    private void drawOwnershipDisplay(Graphics2D g) { // BR: Dark Galaxy
        int r0 = scale(1.0f);
        int r1 = scale(0.8f);

        Galaxy gal = galaxy();
        Empire pl = player();
         
        for (int id=0; id < pl.sv.count(); id++) {
            StarSystem sys = gal.system(id);
            Empire emp = debugShowAll? sys.empire() : parent.knownEmpire(id, pl);

            if (options().selectedDarkGalaxy() && (pl!=emp) && !options().darkGalaxySpy() && !debugShowAll) {
        		float dist = pl.sv.distance(id);
    			float scoutRange = pl.scoutRange();
    			float scanRange = pl.planetScanningRange();
    			float range = max(scoutRange, scanRange);
    			if (dist>range)
    				continue;
        	}
            //Shape sysCircle = new Ellipse2D.Float(mapX(sys.x())-ownerR, mapY(sys.y())-ownerR, 2*ownerR, 2*ownerR);
            if ((emp != null) && parent.showOwnership(sys)) {
                int shape = emp.shape();
                g.setColor(emp.ownershipColor());
                int x = mapX(sys.x());
                int y = mapY(sys.y());
                switch(shape) {
                    case Empire.SHAPE_SQUARE:
                        g.fillRect(x-r1, y-r1, r1+r1, r1+r1); break;
                    case Empire.SHAPE_DIAMOND:
                        Polygon p = new Polygon();
                        p.addPoint(x, y-r0);
                        p.addPoint(x-r0, y);
                        p.addPoint(x, y+r0);
                        p.addPoint(x+r0, y);
                        g.fill(p); break;
                    case Empire.SHAPE_TRIANGLE1:
                        Polygon p1 = new Polygon();
                        p1.addPoint(x, y-r0);
                        p1.addPoint(x-r0, y+r1);
                        p1.addPoint(x+r0, y+r1);
                        g.fill(p1); break;
                    case Empire.SHAPE_TRIANGLE2:
                        Polygon p2 = new Polygon();
                        p2.addPoint(x, y+r0);
                        p2.addPoint(x-r0, y-r1);
                        p2.addPoint(x+r0, y-r1);
                        g.fill(p2);
                        break;
                    case Empire.SHAPE_CIRCLE:
                    default:
                        g.fillOval(x-r0, y-r0, r0+r0, r0+r0); break;
                }
                //g.fill(sysCircle);
            }
        }
    }
    private void drawExtendedRangeDisplay(Graphics2D g) {
        Empire emp = parent.empireBoundaries();
        float shipRange  = emp.shipRange();
        float scoutRange = emp.scoutRange();
        if (shipRange > galaxy().width())
            return;

        Empire pl = player();
        Color normalBorder = emp.shipBorderColor();
        Color extendedBorder = emp.scoutBorderColor();
        Color normalBackground = emp.empireRangeColor();
        boolean isPlayer = emp.isPlayer();
        IGameOptions opts = options();

        // draw extended range
        List<StarSystem> systems;
        if (!isPlayer && opts.selectedDarkGalaxy() && !opts.darkGalaxySpy())
        	systems = player().systemsForCivDark(emp);
        else
        	systems = player().systemsForCiv(emp);
        List<StarSystem> alliedSystems = new ArrayList<>();
        // only show range for allied systems when player is selected
        if (isPlayer) {
        	List<Empire> allyEmp = new ArrayList<>(emp.allies());
            for (Empire ally: allyEmp) {
            	List<StarSystem> allySystems = new ArrayList<>(ally.allColonizedSystems());
                for (StarSystem sys: allySystems) {
                    if (sys != null && pl.sv.empId(sys.id) == ally.id)
                        alliedSystems.add(sys);
                }
            }
        }

        float scale = getWidth()/scaleX();

        AffineTransform prevXForm = g.getTransform();
        if ((areaOffsetX != 0) || (areaOffsetY != 0)) {
            float ctrX = parent.mapFocus().x();
            float ctrY = parent.mapFocus().y();
            float mapOffsetX = fMapX(ctrX)- fMapX(ctrX-areaOffsetX);
            float mapOffsetY = fMapY(ctrY)-fMapY(ctrY-areaOffsetY);
            AffineTransform xForm = g.getTransform();
            xForm.setToIdentity();
            xForm.translate(mapOffsetX, mapOffsetY);
            g.setTransform(xForm);
        }
        float extR = scoutRange*scale;
        float baseR = shipRange*scale;
        Area tmpRangeArea = scoutRangeArea;
        if (tmpRangeArea == null) {
//            long time1 = System.nanoTime();
            List<Area> toAdd = new ArrayList<>();
            for (StarSystem sv: alliedSystems)
                toAdd.add(new Area( new Ellipse2D.Float(fMapX(sv.x())-extR, fMapY(sv.y())-extR, 2*extR, 2*extR) )); 
            for (StarSystem sv: systems)
                toAdd.add(new Area( new Ellipse2D.Float(fMapX(sv.x())-extR, fMapY(sv.y())-extR, 2*extR, 2*extR) )); 
            tmpRangeArea = parallelAdd(toAdd);
            scoutRangeArea = tmpRangeArea;
//            long time2 = System.nanoTime();
//            double ms = (time2-time1) / 1_000_000.0;
//            System.out.format("RRR scout %.2f ms\n", ms);
        }
        g.setColor(extendedBorder);
        g.setStroke(stroke2);
        g.draw(tmpRangeArea);   


        tmpRangeArea = shipRangeArea;
        if (tmpRangeArea == null) {
//            long time1 = System.nanoTime();
            List<Area> toAdd = new ArrayList<>();
            for (StarSystem sv: alliedSystems)
                toAdd.add(new Area( new Ellipse2D.Float(fMapX(sv.x())-baseR, fMapY(sv.y())-baseR, 2*baseR, 2*baseR) )); 
            for (StarSystem sv: systems)
                toAdd.add(new Area( new Ellipse2D.Float(fMapX(sv.x())-baseR, fMapY(sv.y())-baseR, 2*baseR, 2*baseR) ));
            tmpRangeArea = parallelAdd(toAdd);
            shipRangeArea = tmpRangeArea;
//            long time2 = System.nanoTime();
//            double ms = (time2-time1) / 1_000_000.0;
//            System.out.format("RRR base %.2f ms\n", ms);
        }
        
        g.setColor(normalBackground);
        g.fill(tmpRangeArea);
        g.setColor(normalBorder);
        g.setStroke(stroke2);
        g.draw(tmpRangeArea);   
        
        g.setTransform(prevXForm);
    }

    private static ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static Function<List<Area>, Area> addAreas = list -> {
        Area total = list.get(0);
        for (int i = 1; i < list.size(); i ++) {
            Area area = list.get(i);
            total.add(area);
        }
        return total;
    };
    private Area parallelAdd(List<Area> areas) {
        if (areas == null || areas.isEmpty()) {
            return new Area();
        }
        // split areas into parts of X areas per per thread, and
        int step = Math.max(2, areas.size() / Runtime.getRuntime().availableProcessors());
        List<Future<Area>> futures = new ArrayList<>();
        int i;
        for (i = 0; i < areas.size() - step; i+= step) {
            List<Area> part = areas.subList(i, i+step);
            Future<Area> future = executor.submit(() -> addAreas.apply(part));
            futures.add(future);
        }
        if (i < areas.size()) {
            List<Area> part = areas.subList(i, areas.size());
            Future<Area> future = executor.submit(() -> addAreas.apply(part));
            futures.add(future);
        }
//        System.out.println("RRR Processed in "+futures.size()+" parts of "+step+" areas each");
        try {
            Area total = futures.get(0).get();
            for (i = 1; i < futures.size(); i++) {
                Area area = futures.get(i).get();
                total.add(area);
            }
            return total;
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    private void drawGridCircularDisplayDark(Graphics2D g) {
        Galaxy gal = galaxy();
        Sprite clicked = parent.clickedSprite();
        if (clicked == null)
            return;

        float x = clicked.source().x();
        float y = clicked.source().y();

        Stroke prevStroke = g.getStroke();
        g.setStroke(stroke1);
        g.setColor(gridDark);

        Empire pl = player();
        float rng1 = pl.shipRange();
        float rng2 = pl.scoutRange();
        
        for (int r=1;r<=gal.width();r++) {
            int x0 = mapX(x-r);
            int y0 = mapY(y-r);
            int diam = Math.abs(x0-mapX(x+r));
            if ((clicked instanceof StarSystem) && ((r == rng1) || (r == rng2))) {
                g.setColor(gridLight);
                g.drawOval(x0, y0, diam, diam);
                g.setColor(gridDark);
            }
            else
                g.drawOval(x0, y0, diam, diam);
        }
        g.setStroke(prevStroke);
    }
    private void drawBackgroundStars(Graphics2D g) {
        int w = sharedStarBackground.getWidth();
        int h = sharedStarBackground.getHeight();
        // java modulo does not handle negative numbers properly
        int x0 = backOffsetX;
        while (x0<0)   x0+=w;
        int y0 = backOffsetY;
        while (y0<0)  y0 +=h;
        
        int x = x0 % w;
        int y = y0 % h;
        
        if ((x > 0) && (y > 0)) {
            BufferedImage topL = sharedStarBackground.getSubimage(w-x,h-y,x, y);
            g.drawImage(topL,0,0, null);
        }
        if (y > 0) {
            BufferedImage topR = sharedStarBackground.getSubimage(0,h-y,w-x, y);
            g.drawImage(topR,x,0, null);
        }
        if (x > 0) {
            BufferedImage botL = sharedStarBackground.getSubimage(w-x,0,x, h-y);
            g.drawImage(botL,0,y, null);
        }
        
        BufferedImage botRight = sharedStarBackground.getSubimage(0,0,w-x, h-y);
        g.drawImage(botRight,x,y,null);
    }
    private void drawBackgroundNebula(Graphics2D g) {
        int w = sharedNebulaBackground.getWidth();
        int h = sharedNebulaBackground.getHeight();
        
        // java modulo does not handle negative numbers properly
        int x0 = backOffsetX;
        while (x0<0)   x0+=w;
        int y0 = backOffsetY;
        while (y0<0)  y0 +=h;
        
        int x = x0 % w;
        int y = y0 % h;
        
        if ((x > 0) && (y > 0)) {
            BufferedImage topL = sharedNebulaBackground.getSubimage(w-x,h-y,x, y);
            g.drawImage(topL,0,0, null);
        }
        if (y > 0) {
            BufferedImage topR = sharedNebulaBackground.getSubimage(0,h-y,w-x, y);
            g.drawImage(topR,x,0, null);
        }
        if (x > 0) {
            BufferedImage botL = sharedNebulaBackground.getSubimage(w-x,0,x, h-y);
            g.drawImage(botL,0,y, null);
        }
        
        BufferedImage botRight = sharedNebulaBackground.getSubimage(0,0,w-x, h-y);
        g.drawImage(botRight,x,y,null);
    }
    private void drawNebulas(Graphics2D g) {
        for (Nebula neb: galaxy().nebulas()) {
            if (parent.shouldDrawSprite(neb))
                neb.draw(this, g);
        }
    }
    private void drawEmpireNames(Graphics2D g, boolean darkMode) {
        for (Empire emp: galaxy().empires()) {
            if (parent.shouldDrawEmpireName(emp, scaleX()) || debugShowAll) 
                parent.drawEmpireName(emp, this, g, darkMode);
        }
    }
    private void drawStarSystems(Graphics2D g) {
        Galaxy gal = galaxy();
        for (int id=0; id< gal.numStarSystems();id++) {
            StarSystem sys = gal.system(id);
            if (debugShowAll) {
                sys.draw(this, g);
                if (sys.transportSprite()!=null)
                    sys.transportSprite().draw(this, g);
                if (sys.rallySprite()!=null)
                    sys.rallySprite().draw(this, g);
            }
            else {
                if (parent.shouldDrawSprite(sys))
                    sys.draw(this, g);
                if (parent.shouldDrawSprite(sys.transportSprite()))
                    sys.transportSprite().draw(this, g);
                if (parent.shouldDrawSprite(sys.rallySprite()))
                    sys.rallySprite().draw(this, g);
            }
        }
    }
    private void drawShipsAndPath(Graphics2D g) {
        if (!parent.drawShips() &&  !debugShowAll)
            return;
        Empire pl = player();

        if(debugShowAll) {
            for (Ship sh: galaxy().ships.allFleets()) {
                sh.setDisplayed(this);
                Sprite spr = (Sprite) sh;
                // if we are drawing the ship, then check if its flight path should be drawn first
                if ((sh.deployed() || sh.retreating() || sh.inTransit() || sh.isRallied())
                && (parent.shouldDrawSprite(sh.pathSprite()) || debugShowAll)) {
                    if (sh.pathSprite()!=null)
                        sh.pathSprite().draw(this,g);
                spr.draw(this, g);
                }
            }
        	for (SpaceMonster sh: galaxy().spaceMonsters()) {
        		sh.setDisplayed(this);
        		Sprite spr = (Sprite) sh;
        		if (sh.pathSprite()!=null)
        			sh.pathSprite().draw(this, g);
        		spr.draw(this, g);
        	}
        }
        else if (warView) {
            // commodification exception here without this copy
            List<Ship> visibleShips = new ArrayList<>(pl.visibleShips());
            for (Ship sh: visibleShips) {
            	if (sh != null) {
            		// display only ship targeting the clicked system
        			if (sh.inTransit()) {
        				if (sh.destSysId() == targetSysId) {
	            			if (!pl.knowETA(sh))
	            				continue;
        				}
        				else
        					continue;
        			}
        			else if (sh.deployed()) {
        				if (sh.destSysId() == targetSysId) {
	            			if (!pl.knowETA(sh))
	            				continue;
        				}
        				else
        					continue;
        			}
        			else {
            			// or display only ship orbiting the clicked system
            			if (sh instanceof ShipFleet) { 
            				ShipFleet fl = (ShipFleet) sh;
            				if (fl.isOrbiting()) {
	            				if (fl.sysId() != targetSysId)
	            					continue;
            				}
            				else
            					continue;
            			}
            			else
            				continue;
        			}

        			sh.setDisplayed(this);
                    if (sh.displayed() || debugShowAll) {
                        Sprite spr = (Sprite) sh;
                        // if we are drawing the ship, then check if its flight path should be drawn first
                        if ((sh.deployed() || sh.retreating() || sh.inTransit() || sh.isRallied())
                        && (parent.shouldDrawSprite(sh.pathSprite()) || debugShowAll)) {
                            if (pl.knowETA(sh) && sh.pathSprite()!=null)
                                sh.pathSprite().draw(this,g);
                            else if (options().selectedTrackUFOsAcrossTurns()) {
                                StarSystem suspectedDestination = player().suspectedDestinationOfVisibleShip(sh);
                                if (suspectedDestination != null)
                                	// BR: To fix dHannash resulting pathSprite bug
                                	if (sh instanceof Transport)
                                		((Transport) sh).pathSpriteTo(suspectedDestination).draw(this, g);
                                	else if (sh instanceof ShipFleet)
                                		((ShipFleet) sh).pathSpriteTo(suspectedDestination).draw(this, g);
                            }
                        }
                        spr.draw(this, g);
                    }            		
            	}
            }
            List<SpaceMonster> visibleMonsters = new ArrayList<>(pl.visibleMonsters());
            for (SpaceMonster sh: visibleMonsters) {
            	if (sh != null) {
            		if (!sh.inTransit())
        				continue;
        			if (sh.destSysId() != targetSysId)
        				continue;

        			sh.setDisplayed(this);
                    if (sh.displayed()) {
                        Sprite spr = (Sprite) sh;
                        if (sh.event!=null && sh.event.notified() && sh.pathSprite()!=null)
                            sh.pathSprite().draw(this, g);
                        spr.draw(this, g);
                    }
            		
            	}
            }    		
    	}
        else {
            // commodification exception here without this copy
            List<Ship> visibleShips = new ArrayList<>(pl.visibleShips());
            for (Ship sh: visibleShips) {
            	if (sh != null) {
                    sh.setDisplayed(this);
                    if (sh.displayed() || debugShowAll) {
                        Sprite spr = (Sprite) sh;
                        // if we are drawing the ship, then check if its flight path should be drawn first
                        if ((sh.deployed() || sh.retreating() || sh.inTransit() || sh.isRallied())
                        && (parent.shouldDrawSprite(sh.pathSprite()) || debugShowAll)) {
                            if (pl.knowETA(sh) && sh.pathSprite()!=null)
                                sh.pathSprite().draw(this,g);
                            else if (options().selectedTrackUFOsAcrossTurns()) {
                                StarSystem suspectedDestination = player().suspectedDestinationOfVisibleShip(sh);
                                if (suspectedDestination != null)
                                	// BR: To fix dHannash resulting pathSprite bug
                                	if (sh instanceof Transport)
                                		((Transport) sh).pathSpriteTo(suspectedDestination).draw(this, g);
                                	else if (sh instanceof ShipFleet)
                                		((ShipFleet) sh).pathSpriteTo(suspectedDestination).draw(this, g);
                            }
                        }
                        spr.draw(this, g);
                    }            		
            	}
            }
            List<SpaceMonster> visibleMonsters = new ArrayList<>(pl.visibleMonsters());
            for (SpaceMonster sh: visibleMonsters) {
            	if (sh != null) {
                    sh.setDisplayed(this);
                    if (sh.displayed()) {
                        Sprite spr = (Sprite) sh;
                        if (sh.event!=null && sh.event.notified() && sh.pathSprite()!=null)
                            sh.pathSprite().draw(this, g);
                        spr.draw(this, g);
                    }
            		
            	}
            }
        }
    }
    private void drawWorkingFlightPaths(Graphics2D g) {
        for (FlightPathSprite spr: FlightPathSprite.workingPaths()) {
            if (parent.shouldDrawSprite(spr) || debugShowAll) 
                spr.draw(this, g);
        }
    }
    private void drawControlSprites(Graphics2D g) {
        for (Sprite sprite: baseControls) {
            if (parent.shouldDrawSprite(sprite))
                sprite.draw(this, g);
        }
        for (Sprite sprite: parent.controlSprites()) {
            if (parent.shouldDrawSprite(sprite))
                sprite.draw(this, g);
        }
    }
    private void drawNextTurnSprites(Graphics2D g) {
        for (Sprite sprite: parent.nextTurnSprites()) {
            if (parent.shouldDrawSprite(sprite))
                sprite.draw(this, g);
        }
    }
    private void drawRangeSelect(Graphics2D g) {
        if ((selectX0 == selectX1) || (selectY0 == selectY1))
            return;
        
        int x = min(selectX0, selectX1);
        int y = min(selectY0, selectY1);
        int w = abs(selectX0-selectX1);
        int h = abs(selectY0-selectY1);
        Stroke prev = g.getStroke();
        g.setStroke(stroke1);
        g.setColor(Color.white);
        g.drawRect(x,y,w,h);
        
        g.setStroke(prev);
    }
    private Sprite spriteAt(int x1, int y1, boolean ctrlDown) {
        // In order of hovering priority:
        // 1. Next Turn Sprites
        // 2. Map Control Sprites
        // 3. Ships & Flight Paths
        // 4. Systems
        for (Sprite sprite: parent.nextTurnSprites()) {
            if (sprite.isSelectableAt(this, x1, y1))
                return sprite;
        }
        for (Sprite sprite: parent.controlSprites()) {
            if (sprite.isSelectableAt(this, x1, y1))
                return sprite;
        }
        for (Sprite sprite: baseControls) {
            if (sprite.isSelectableAt(this, x1, y1))
                return sprite;
        }
        // is there any overlay covering the map and preventing a search
        if (parent.masksMouseOver(x1,y1))
            return null;

        Galaxy gal = galaxy();
        Empire pl = player();

        // if ctrl down, Star systems have priority
        if (ctrlDown && parent.hoverOverSystems()) {
            for (int id=0;id<gal.numStarSystems();id++) {
                if (gal.system(id).isSelectableAt(this, x1, y1))
                    return gal.system(id);
            }
        }

        List<Ship> ships = null;
        if (scaleX() <= MAX_FLEET_HUGE_SCALE) {
            if (parent.hoverOverFleets()) {
                ships = new ArrayList<>(pl.visibleShips());
                ships.sort(EMPIRE_ID);
                float minDistance = Float.MAX_VALUE;
                Sprite closestShip = null;
                for (Ship sh: ships) {
                    if (sh.displayed()) {
                        Sprite spr = (Sprite) sh;
                        if (parent.shouldDrawSprite(spr)
                        && spr.isSelectableAt(this, x1, y1)) {
                            float dist = spr.selectDistance(this, x1, y1);
                            if (dist == 0)
                               return spr;
                            if (dist < minDistance) {
                                minDistance = dist;
                                closestShip = spr;
                            }
                        }
                    }
                }
                if (closestShip != null)
                    return closestShip;
                List<SpaceMonster> monsters = new ArrayList<>(pl.visibleMonsters());
                minDistance = Float.MAX_VALUE;
                for (Ship sh: monsters) {
                    if (sh.displayed()) {
                        Sprite spr = (Sprite) sh;
                        if (parent.shouldDrawSprite(spr)
                        && spr.isSelectableAt(this, x1, y1)) {
                            float dist = spr.selectDistance(this, x1, y1);
                            if (dist == 0)
                               return spr;
                            if (dist < minDistance) {
                                minDistance = dist;
                                closestShip = spr;
                            }
                        }
                    }
                }
                if (closestShip != null)
                    return closestShip;

            }
        }
        if (!ctrlDown && parent.hoverOverSystems()) {
            for (int id=0;id<gal.numStarSystems();id++) {
                if (gal.system(id).isSelectableAt(this, x1, y1))
                    return gal.system(id);
            }
        }
        // working flight paths are always displayed and can always be selected
        List<FlightPathSprite> workingPaths = FlightPathSprite.workingPaths();
        for (FlightPathSprite path: workingPaths) {
            if (path.isSelectableAt(this,x1,y1))
                return path;
        }
        if (scaleX() <= MAX_FLEET_HUGE_SCALE) {
            if (parent.hoverOverFlightPaths()) {
                if (ships == null)
                    ships = new ArrayList<>(pl.visibleShips());
                for (Ship sh: ships) {
                    if (sh.displayed()) {
                        FlightPathSprite fpSpr = sh.pathSprite();
                        if (parent.shouldDrawSprite(fpSpr)
                        && fpSpr.isSelectableAt(this, x1, y1))
                            return fpSpr;
                    }
                }
            }
        }
        return null;
    }
    public void dragMap(int deltaX, int deltaY) {
        backOffsetX += deltaX/10;
        backOffsetY += deltaY/10;

        float objX = parent.mapFocus().x();
        float objY = parent.mapFocus().y();
        float newObjX = objX;
        float newObjY = objY;
        
        // we need to recalculate the new focusX/Y before
        // recentering so that we can have the proper pixel
        // offset for the range areas
        // only do this for changed X/Y values to avoid
        // introducing rounding errors
        if (deltaX != 0) {
            int focusX = mapX(parent.mapFocus().x());
            newObjX = bounds(0, objX(focusX-deltaX), sizeX());
        }
        if (deltaY != 0) {
            int focusY = mapY(parent.mapFocus().y());
            newObjY = bounds(0, objY(focusY-deltaY), sizeY());
        }

        areaOffsetX += (objX-newObjX);
        areaOffsetY += (objY-newObjY);
        
        parent.mapFocus().setXY(newObjX, newObjY);
        center(parent.mapFocus());
        clearRangeMap();
        repaint();
        resetDarkArea();
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        float currentScale = scaleY();
        float zoomAdj = desiredScale/currentScale;
        if (zoomAdj == 1.0) {
            zoomTimer.stop();
            return;
        }

        float zoomAmt = zoomBase;
        if (zoomAdj > zoomAmt)
            zoomAdj = zoomAmt;
        else if (zoomAdj < (1.0f/zoomAmt))
            zoomAdj = 1.0f/zoomAmt;

        setScale(currentScale*zoomAdj);
        repaint();
    }
    @Override
    public void animate() {
        if (session().performingTurn() && parent.suspendAnimationsDuringNextTurn())
            return;
        if (zoomTimer.isRunning())
            return;

        if (playAnimations() && (animationCount() % 5 == 0)) 
            repaint();	
    }
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    	setModifierKeysState(e); // BR: For the Flag color selection
        // if we are hovering over a sprite and it accepts mousewheel
        // then do that
        if ((hoverSprite != null) && hoverSprite.acceptWheel()) {
            hoverSprite.wheel(this, e.getWheelRotation(), false);
            return;
        }

        if (parent.forwardMouseEvents())
            parent.mouseWheelMoved(e);
        else
            adjustZoom(e.getWheelRotation());
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();

        boolean rightClick = SwingUtilities.isRightMouseButton(e) && parent.allowsDragSelect();
        int deltaX = x - lastMouseX;
        int deltaY = y - lastMouseY;

        if (rightClick) {
            int prevSelectX0 = selectX0;
            int prevSelectY0 = selectY0;
            int prevSelectX1 = selectX1;
            int prevSelectY1 = selectY1;
            int boundsX0 = min(prevSelectX0, prevSelectX1, x);
            int boundsY0 = min(prevSelectY0, prevSelectY1, y);
            int boundsX1 = max(prevSelectX0, prevSelectX1, x);
            int boundsY1 = max(prevSelectY0, prevSelectY1, y);
            if (x > selectX0) {
                selectX1 = x;
            }
            else if (x < selectX0) {
                selectX0 = x; selectX1 = prevSelectX1; 
            }
            if (y > selectY0) {
                selectY1 = y;
            }
            else if (y < selectY0) {
                selectY0 = y; selectY1 = prevSelectY1; 
            }
            paintImmediately(boundsX0, boundsY0, s5+boundsX1-boundsX0, s5+boundsY1-boundsY0);
        }
        else if (parent.canChangeMapScales()) {
            // find the xy map coords of current focus, adjust by the
            // dragged deltas, then recenter the map on the new focus
            dragMap(deltaX, deltaY);
            lastMouseX = x;
            lastMouseY = y;
            lastMouseTime = System.currentTimeMillis();
        }
    }
    @Override
    public void mouseMoved(MouseEvent e) {
    	mouseMoved(e.getX(), e.getY(), e.isControlDown());
    }
    public void updateHover() { // BR: To remove the need of moving the mouse
    	Point pos = getMousePosition();
    	if (pos == null)
	    	mouseMoved(lastMouseX, lastMouseY, false);
    	else
	    	mouseMoved(pos.x, pos.y, false);
    }
    private void mouseMoved(int x, int y, boolean ctrlDown) {
       long prevTime = lastMouseTime;
        int prevX = lastMouseX;
        int prevY = lastMouseY;
        lastMouseX = x;
        lastMouseY = y;
        lastMouseTime = System.currentTimeMillis();
        
        if (maxMouseVelocity > 0) {
            long timeS = (lastMouseTime - prevTime);
            if (timeS == 0)
                return;
            // quick and dirty mouse speed test
            int dist = Math.abs(lastMouseX-prevX)+Math.abs(lastMouseY-prevY);
            long speed = dist*1000/timeS;
            if (speed >= maxMouseVelocity)
                return;
        }
        
        Sprite prevHover = hoverSprite;
        
        // skip the check if we are currently in the midst of a check
        if (searchingSprite)
            return;      
        searchingSprite = true;
        try { hoverSprite = spriteAt(x,y, ctrlDown); }
        finally { searchingSprite = false; }
        
        // still hovering over same sprite... do nothing
        boolean altDown = isAltDown();
        boolean altChange = altDown!=lastHoverAltDown;
        if (altChange)
        	lastHoverAltDown = altDown;
        if (hoverSprite == prevHover && !altChange) 
            return;

        // if sprite changed, but we are also still over the prevHover
        // if the prevHover is higher display priority than the new,
        // then do nothing. What is the point of this? If a fleet is in 
        // the same area as a system (lower priority), then we want to 
        // display the fleet until we mouse away from it because it is
        // drawn over the system.
        if ((prevHover != null) && prevHover.isSelectableAt(this,x,y)) {
            int prevPriority = prevHover.displayPriority();
            if (ctrlDown) { // BR: invert the priority
                int hoverPriority = (hoverSprite == null) ? Integer.MAX_VALUE : hoverSprite.displayPriority();
                if (hoverPriority > prevPriority) {
                    hoverSprite = prevHover;
                    return;
                }            	
            } else {
                int hoverPriority = (hoverSprite == null) ? 0 : hoverSprite.displayPriority();
                if (hoverPriority < prevPriority) {
                    hoverSprite = prevHover;
                    return;
                }
            }
        }
            
        // sprite has changed, so pass it to the parent UI for proper handling
        parent.hoveringOverSprite(hoverSprite);
    }
    @Override
    public void mouseClicked(MouseEvent e)     {}
    @Override
    public void mouseEntered(MouseEvent arg0)  { }
    @Override
    public void mouseExited(MouseEvent e)      {
        if (parent.hoveringSprite() != null)
            parent.hoveringSprite().mouseExit(this);
        parent.hoveringOverSprite(null);
    }
    @Override
    public void mousePressed(MouseEvent e)     {
        boolean rightClick = SwingUtilities.isRightMouseButton(e) && parent.allowsDragSelect();
        if (rightClick) {
            dragSelecting = true;
            selectX0 = selectX1 = e.getX();
            selectY0 = selectY1 = e.getY();
        }
    }
    @Override
    public void mouseReleased(MouseEvent e)    {
    	setModifierKeysState(e); // BR: For the Flag color selection
    	if (historyMode) {
    		GalaxyMapPane historyPane = (GalaxyMapPane) parent;
    		if (historyPane.showHelp) {
    			historyPane.cancelHelp();
    			return;
    		}
    	}
        boolean shift = e.isShiftDown();
        if (dragSelecting) {
            if ((selectX0 != selectX1) && (selectY0 != selectY1)) 
                parent.dragSelect(selectX0, selectY0, selectX1, selectY1, shift);
            selectX0 = selectX1 = selectY0 = selectY1 = 0;
            dragSelecting = false;
            repaint();
            return;               
        }       

        if (e.getButton() > 3)
            return;
        
        int clicks = e.getClickCount();
        boolean rightClick = SwingUtilities.isRightMouseButton(e);
        boolean middleClick = SwingUtilities.isMiddleMouseButton(e);
        Sprite newSelection = hoverSprite;

        if (newSelection == null) 
            parent.clickingNull(1, rightClick);
        else if ((clicks == 1) || newSelection.acceptDoubleClicks())
            parent.clickingOnSprite(newSelection, 1, rightClick, true, middleClick);
            
        parent.hoveringOverSprite(newSelection);
    }
    public void altToggled (boolean isAltDown) { parent.hoveringOverSprite(hoverSprite); }
}
