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
package rotp.ui.main.overlay;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LinearGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import rotp.model.Sprite;
import rotp.model.combat.CombatStack;
import rotp.model.combat.ShipCombatManager;
import rotp.model.empires.DiplomaticEmbassy;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireStatus;
import rotp.model.empires.EmpireView;
import rotp.model.empires.Race;
import rotp.model.empires.ShipView;
import rotp.model.empires.SpyNetwork;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.ui.BasePanel;
import rotp.ui.RotPUI;
import rotp.ui.combat.ShipBattleUI;
import rotp.ui.main.GalaxyMapPanel;
import rotp.ui.main.MainUI;
import rotp.ui.main.SystemPanel;
import rotp.ui.races.RacesMilitaryUI;
import rotp.ui.races.RacesUI;
import rotp.ui.sprites.MapSprite;
import rotp.ui.vipconsole.IVIPListener;
import rotp.ui.vipconsole.VIPConsole;

public class MapOverlayShipCombatPrompt extends MapOverlay implements IVIPListener {
    static final Color destroyedTextC = new Color(255,32,32,192);
    static final Color destroyedMaskC = new Color(0,0,0,160);
    Color maskC  = new Color(40,40,40,160);
    Area mask;
    BufferedImage planetImg;
    MainUI parent;
    int sysId;
    ShipFleet fleet;
    int pop, bases, fact, shield;
    public int boxX, boxY, boxW, boxH;
    boolean drawSprites = false;
    int showInfo = 0;
    public ShipCombatManager mgr;
    AutoResolveBattleSprite resolveButton = new AutoResolveBattleSprite();
    SmartResolveBattleSprite smartResolveButton = new SmartResolveBattleSprite();
    RetreatAllBattleSprite retreatButton = new RetreatAllBattleSprite();
    EnterBattleSprite battleButton = new EnterBattleSprite();
    SystemFlagSprite flagButton = new SystemFlagSprite();
    StartWarBattleSprite warButton = new StartWarBattleSprite();
    public MapOverlayShipCombatPrompt(MainUI p) {
        parent = p;
    }
    public void init(ShipCombatManager m) {
        mgr = m;
        sysId = mgr.system().id;
        Empire pl = player();
        flagButton.reset();
        StarSystem sys = galaxy().system(sysId);
        showInfo = 0;
        fleet = null;
        planetImg = null;
        drawSprites = true;
        pop = pl.sv.population(sysId);
        bases = pl.sv.bases(sysId);
        fact = pl.sv.factories(sysId);
        shield = pl.sv.shieldLevel(sysId);
        parent.hideDisplayPanel();
        parent.map().setScale(20);
        parent.map().recenterMapOn(sys);
        parent.mapFocus(sys);
        parent.clickedSprite(sys);
        parent.repaint();
        initConsoleSelection("Ship Combat", false);
    }
    public void startCombat(int combatFlag) {
        drawSprites = false;
        parent.clearOverlay();
        parent.repaintAllImmediately();
        RotPUI.instance().selectShipBattlePanel(mgr, combatFlag);
    }
    private StarSystem starSystem() {
        return galaxy().system(sysId);
    }
    private void toggleFlagColor(boolean reverse) {
        player().sv.toggleFlagColor(sysId, reverse);
        parent.repaint();
    }
    private void resetFlagColor()	{
        player().sv.resetFlagColor(sysId);
        parent.repaint();
    }
    private void startWar()	{
    	Empire alien  = mgr.results().aiEmpire();
    	if (alien == null)
    		return;
    	Empire player = mgr.player();
    	DiplomaticEmbassy embassy = player.viewForEmpire(alien).embassy();
    	embassy.declareWar();
    }
    private void drawFleetsInfo(Graphics2D g) {
    	if (showInfo == 0)
    		return;
    	Empire player = player();
    	RacesMilitaryUI milPane = RacesUI.instance.militaryPanel;

    	int ws = parent.getWidth();
    	int w = scaled(947);
    	int h = BasePanel.s80;
       	int dh = h+BasePanel.s2;
		int x = (ws-w)/4;
		int yi = BasePanel.s10;
		int y = scaled(166);

    	if (showInfo == 1) {
    		if (mgr.results().isMonsterAttack()) {
    			for(CombatStack st : mgr.activeStacks()) {
                    if(st.isMonster()) {
                    	Empire emp = st.empire();
                    	ShipView view = emp.shipViewFor(st.design());
                    	h = max(h, BasePanel.s20 * st.design().maxSpecials());
                    	milPane.drawShipDesign(g, view, st.num, x, y, w, h, MainUI.paneBackground);
                        y += dh;
                        milPane.paintMonsterData(g, emp, x, yi, MainUI.paneBackground);
                    }
        		}
    			return;
    		}
    		else {
            	Empire alien  = mgr.results().attacker();
            	if (alien == player)
            		alien = mgr.results().defender();
            	SpyNetwork spies = player.viewForEmpire(alien).spies();
        		for(CombatStack st : mgr.activeStacks()) {
                    if(st.isShip()) {
                        if(st.empire() == alien) {
                        	ShipView view = spies.shipViewFor(st.design());
                        	milPane.drawShipDesign(g, view, st.num, x, y, w, h, MainUI.paneBackground);
                            y += dh;
                        }
                    }
        		}
    			milPane.paintAlienData(g, alien, x, yi, MainUI.paneBackground);
    		}
    	}
    	else if (showInfo == 2) {
    		for(CombatStack st : mgr.activeStacks()) {
                if(st.isShip()) {
                    if(st.empire() == player) {
                    	ShipView view = player.shipViewFor(st.design());
                    	milPane.drawShipDesign(g, view, st.num, x, y, w, h, MainUI.paneBackground);
                        y += dh;
                    }
                }
    		}
    		milPane.paintPlayerData(g, x, yi, MainUI.paneBackground);
    	}
    }
    @Override
    public boolean drawSprites()   { return drawSprites; }
    @Override
    public boolean masksMouseOver(int x, int y)   { return true; }
    @Override
    public boolean hoveringOverSprite(Sprite o) { return false; }
    @Override
    public void advanceMap() {
        startCombat(ShipBattleUI.ENTER_COMBAT);
    }
    @Override
    public void paintOverMap(MainUI parent, GalaxyMapPanel ui, Graphics2D g) {
        StarSystem sys = galaxy().system(sysId);
        Empire pl = player();

        int s7  = BasePanel.s7;
        int s10 = BasePanel.s10;
        int s15 = BasePanel.s15;
        int s20 = BasePanel.s20;
        int s25 = BasePanel.s25;
        int s30 = BasePanel.s30;
        int s40 = BasePanel.s40;
        int s50 = BasePanel.s50;
        int s60 = BasePanel.s60;

        int w = ui.getWidth();
        int h = ui.getHeight();

        int bdrW = s7;
        boxW = scaled(540);
        int boxH1 = BasePanel.s68;
        int boxH2 = scaled(172);
        int buttonPaneH = scaled(35);
        boxH = boxH1 + boxH2 + buttonPaneH;
        
        boxX = -s40+(w/2);
        boxY = -s40+(h-boxH)/2;

        // draw map mask
        if (mask == null) {
            int r = s60;
            int centerX = w*2/5;
            int centerY = h*2/5;
            Ellipse2D window = new Ellipse2D.Float();
            window.setFrame(centerX-r, centerY-r, r+r, r+r);
            Area st1 = new Area(window);
            Rectangle blackout  = new Rectangle();
            blackout.setFrame(0,0,w,h);
            mask = new Area(blackout);
            mask.subtract(st1);
        }
        g.setColor(maskC);
        g.fill(mask);
        // draw border
        g.setColor(MainUI.paneShadeC);
        g.fillRect(boxX-bdrW, boxY-bdrW, boxW+bdrW+bdrW, boxH+bdrW+bdrW);

        // draw Box
        g.setColor(MainUI.paneBackground);
        g.fillRect(boxX, boxY, boxW, boxH1);

        boolean scouted = player().sv.isScouted(sys.id);
        // draw planet image
        if (planetImg == null) {
            if (!scouted || sys.planet().type().isAsteroids()) {
                planetImg = newBufferedImage(boxW, boxH2);
                Graphics imgG = planetImg.getGraphics();
                imgG.setColor(Color.black);
                imgG.fillRect(0, 0, boxW, boxH2);
                drawBackgroundStars(imgG, boxW, boxH2);
                parent.drawStar((Graphics2D) imgG, sys.starType(), s60, boxW*4/5, boxH2/3);
                imgG.dispose();
            }
            else {
                planetImg = sys.planet().type().panoramaImage();
                int planetW = planetImg.getWidth();
                int planetH = planetImg.getHeight();
                Graphics imgG = planetImg.getGraphics();
                Empire emp = sys.empire();
                if (emp != null) {
                    BufferedImage fortImg = emp.race().fortress(sys.colony().fortressNum());
                    int fortW = scaled(fortImg.getWidth());
                    int fortH = scaled(fortImg.getHeight());
                    int fortScaleW = fortW*planetW/w;
                    int fortScaleH = fortH*planetW/w;
                    int fortX = planetImg.getWidth()-fortScaleW;
                    int fortY = planetImg.getHeight()-fortScaleH+(planetH/5);
                    imgG.drawImage(fortImg, fortX, fortY, fortX+fortScaleW, fortY+fortScaleH, 0, 0, fortImg.getWidth(), fortImg.getHeight(), null);
                    imgG.dispose();
                }
            }
        }
        g.drawImage(planetImg, boxX, boxY+boxH1, boxW, boxH2, null);

        // draw header info
        int leftW = boxW * 2/5;
        String yearStr = displayYearOrTurn();
        g.setFont(narrowFont(40));
        int sw = g.getFontMetrics().stringWidth(yearStr);
        int x0 = boxX+((leftW-sw)/2);
        drawBorderedString(g, yearStr, 2, x0, boxY+boxH1-s20, SystemPanel.textShadowC, SystemPanel.orangeText);

        
        
        Empire aiEmpire = mgr.results().aiEmpire();
        String titleStr;
        if (aiEmpire == null)
            titleStr = text("SHIP_COMBAT_TITLE_MONSTER_DESC", mgr.results().aiRaceName());
        else {
            titleStr = text("SHIP_COMBAT_TITLE_DESC");
            titleStr = aiEmpire.replaceTokens(titleStr, "alien");
        }
        g.setColor(Color.black);
        int titleFontSize = scaledFont(g, titleStr, boxW-leftW, 20, 14);
        g.setFont(narrowFont(titleFontSize));
        drawString(g,titleStr, boxX+leftW, boxY+s20);

        // print prompt string
        String sysName = player().sv.name(sys.id);
        String promptStr = scouted ? text("SHIP_COMBAT_TITLE_SYSTEM", sysName) : text("SHIP_COMBAT_TITLE_UNSCOUTED");
        int promptFontSize = scaledFont(g, promptStr, boxW-leftW-s30, 24, 20);
        g.setFont(narrowFont(promptFontSize));
        drawShadowedString(g, promptStr, 4, boxX+leftW, boxY+s50, SystemPanel.textShadowC, Color.white);

        // init and draw battle and resolve buttons
        parent.addNextTurnControl(battleButton);
        battleButton.init(this, g);
        battleButton.mapX(boxX+boxW-battleButton.width());
        battleButton.mapY(boxY+boxH-battleButton.height());
        battleButton.draw(parent.map(), g);

        parent.addNextTurnControl(resolveButton);
        resolveButton.init(this, g);
        resolveButton.mapX(boxX);
        resolveButton.mapY(battleButton.mapY());
        resolveButton.draw(parent.map(), g);
        
        parent.addNextTurnControl(smartResolveButton);
        smartResolveButton.init(this, g);
       	smartResolveButton.mapX(resolveButton.mapX()+resolveButton.width()+s7);
        smartResolveButton.mapY(battleButton.mapY());
        smartResolveButton.draw(parent.map(), g);

        if(options().selectedRetreatRestrictions() < 2
				|| options().selectedRetreatRestrictionTurns() == 0) {
            parent.addNextTurnControl(retreatButton);
            retreatButton.init(this, g);
            retreatButton.mapX(smartResolveButton.mapX()+smartResolveButton.width()+s7);
            retreatButton.mapY(battleButton.mapY());
            retreatButton.draw(parent.map(), g);
        }

        // draw planet info, from bottom up
        int x1 = boxX+s15;
        int x2 = boxX+boxW/2+s15;
        int y1 = boxY+boxH1+s15;
        int y2 = y1;
        int lineH = s20;
        int desiredFont = 18;
        
        HashMap<String, Integer> mySizes = new HashMap<>();
        HashMap<String, Integer> aiSizes = new HashMap<>();
        for(CombatStack st : mgr.activeStacks())
        {
            int putVal = st.num;
            if(st.isShip())
            {
                if(st.empire() == pl)
                {
                    if(mySizes.containsKey(st.design().sizeDesc()))
                        putVal += mySizes.get(st.design().sizeDesc());
                    mySizes.put(st.design().sizeDesc(), putVal);
                }
                else
                {
                    if(aiSizes.containsKey(st.design().sizeDesc()))
                        putVal += aiSizes.get(st.design().sizeDesc());
                    aiSizes.put(st.design().sizeDesc(), putVal);
                }
            }
            else if(st.isColony() && st.isArmed())
            {
                if(st.empire() == pl)
                    mySizes.put(text("MAIN_COLONY_BASES"), putVal);
                else
                    aiSizes.put(text("MAIN_COLONY_BASES"), putVal);
            }
        }
        for(Entry<String, Integer> entry : mySizes.entrySet())
        {
            drawBorderedString(g, entry.getValue() + " " + entry.getKey(), 1, x1, y1, Color.black, pl.color());
            y1 += lineH;
        }
        for(Entry<String, Integer> entry : aiSizes.entrySet())
        {
        	Color txtColor = aiEmpire==null? Color.RED : aiEmpire.color();
            drawBorderedString(g, entry.getValue() + " " + entry.getKey(), 1, x2, y2, Color.black, txtColor);
            y2 += lineH;
        }
       
        // if unscouted, no planet info
        if (!scouted) {
        	drawFleetsInfo(g);
        	return;
        }
        
        x1 = boxX+s15;
        y1 = boxY+boxH1+boxH2-s10;

        if (pl.sv.isUltraPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_ULTRA_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isPoor(sys.id)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_POOR_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isUltraRich(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ULTRA_RICH_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.sv.isOrionArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ANCIENTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.sv.isArtifact(sys.id)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_ARTIFACTS_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        if (pl.isEnvironmentHostile(sys)) {
            g.setColor(SystemPanel.redText);
            String s1 = text("MAIN_SCOUT_HOSTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.isEnvironmentFertile(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_FERTILE_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else if (pl.isEnvironmentGaia(sys)) {
            g.setColor(SystemPanel.greenText);
            String s1 = text("MAIN_SCOUT_GAIA_DESC");
            int fontSize = scaledFont(g, s1, boxW-s25, desiredFont, 15);
            g.setFont(narrowFont(fontSize));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        // classification line
        if (sys.planet().type().isAsteroids()) {
            String s1 = text("MAIN_SCOUT_NO_PLANET");
            g.setFont(narrowFont(desiredFont+3));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }
        else {
            String s1 = text("MAIN_SCOUT_TYPE", text(sys.planet().type().key()), (int)sys.planet().maxSize());
            g.setFont(narrowFont(desiredFont+3));
            drawBorderedString(g, s1, 1, x1, y1, Color.black, Color.white);
            y1 -= lineH;
        }

        // planet name
        y1 -= scaled(5);
        g.setFont(narrowFont(40));
        drawBorderedString(g, sysName, 1, x1, y1, Color.darkGray, SystemPanel.orangeText);
        
        // planet flag
        parent.addNextTurnControl(flagButton);
        flagButton.init(this, g);
        flagButton.mapX(boxX+boxW-flagButton.width()+s10);
        flagButton.mapY(boxY+boxH-buttonPaneH-flagButton.height()+s10);
        flagButton.draw(parent.map(), g);

        // Empire flag
        int margin = BasePanel.s4;
    	parent.addNextTurnControl(warButton);
        warButton.init(this, g);
        warButton.mapX(boxX+boxW - warButton.width()-margin);
        warButton.mapY(boxY + margin);            	           	
        warButton.draw(parent.map(), g);

        drawFleetsInfo(g);
    }
    @Override
    public boolean handleKeyPress(KeyEvent e) {
        boolean shift = e.isShiftDown();
        Empire aiEmpire = mgr.results().aiEmpire();
        switch(e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            	if (showInfo!=0) { // break info before entering battle
            		showInfo = 0;
            		parent.repaint();
            		break;
            	}
            case KeyEvent.VK_E:
                startCombat(ShipBattleUI.ENTER_COMBAT);
                break;
            case KeyEvent.VK_A:
                if (aiEmpire != null)
                    startCombat(ShipBattleUI.AUTO_RESOLVE);
                break;
            case KeyEvent.VK_S:
                if (aiEmpire != null)
                    startCombat(ShipBattleUI.SMART_RESOLVE);
                break;
            case KeyEvent.VK_R:
                if (aiEmpire != null)
                    startCombat(ShipBattleUI.RETREAT_ALL);
                break;
            case KeyEvent.VK_F:
                toggleFlagColor(shift);
                break;
            case KeyEvent.VK_W:
            	startWar();
                break;
            case KeyEvent.VK_H:
            	switch (showInfo) {
	            	case 0: showInfo = 1; break;
	            	case 1: showInfo = 2; break;
	            	case 2: showInfo = 0; break;
            	}
            	buttonClick();
            	parent.repaint();
                break;
            default:
            	if (!shift) // BR: to avoid noise when changing flag color
            		misClick();
                break;
        }
        return true;
    }
    class AutoResolveBattleSprite extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(59,59,59);
        private final Color midC = new Color(93,93,93);
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlayShipCombatPrompt parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("SHIP_COMBAT_AUTO_RESOLVE"); }
        private Font font()       { return narrowFont(18); }
        public void reset()       { background = null; }

        public void init(MapOverlayShipCombatPrompt p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s40 + g.getFontMetrics(font()).stringWidth(label());
            buttonH = BasePanel.s30;
            selectW = buttonW;
            selectH = buttonH;
        }
        public void setSelectionBounds(int x, int y, int w, int h) {
            selectX = x;
            selectY = y;
            selectW = w;
            selectH = h;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= selectX
                        && x <= selectX+selectW
                        && y >= selectY
                        && y <= selectY+selectH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            if (background == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D start = new Point2D.Float(mapX, 0);
                Point2D end = new Point2D.Float(mapX+buttonW, 0);
                Color[] colors = {edgeC, midC, edgeC };
                background = new LinearGradientPaint(start, end, dist, colors);
            }
            int s3 = BasePanel.s3;
            int s5 = BasePanel.s5;
            int s10 = BasePanel.s10;
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(mapX+s3, mapY+s3, buttonW,buttonH,s10,s10);
            g.setPaint(background);
            g.fillRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
            g.setColor(c0);
            Stroke prevStr =g.getStroke();
            g.setStroke(BasePanel.stroke2);
            g.drawRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            g.setStroke(prevStr);
            g.setFont(font());

            String str = label();
            int sw = g.getFontMetrics().stringWidth(str);
            int x2a = mapX+((buttonW-sw)/2);
            drawBorderedString(g, str, x2a, mapY+buttonH-s10, SystemPanel.textShadowC, c0);
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click, boolean middleClick) {
            startCombat(ShipBattleUI.AUTO_RESOLVE);
        };
    }
    class SmartResolveBattleSprite extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(59,59,59);
        private final Color midC = new Color(93,93,93);
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlayShipCombatPrompt parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("SHIP_COMBAT_SMART_RESOLVE"); }
        private Font font()       { return narrowFont(18); }
        public void reset()       { background = null; }

        public void init(MapOverlayShipCombatPrompt p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s40 + g.getFontMetrics(font()).stringWidth(label());
            buttonH = BasePanel.s30;
            selectW = buttonW;
            selectH = buttonH;
        }
        public void setSelectionBounds(int x, int y, int w, int h) {
            selectX = x;
            selectY = y;
            selectW = w;
            selectH = h;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= selectX
                        && x <= selectX+selectW
                        && y >= selectY
                        && y <= selectY+selectH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            if (background == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D start = new Point2D.Float(mapX, 0);
                Point2D end = new Point2D.Float(mapX+buttonW, 0);
                Color[] colors = {edgeC, midC, edgeC };
                background = new LinearGradientPaint(start, end, dist, colors);
            }
            int s3 = BasePanel.s3;
            int s5 = BasePanel.s5;
            int s10 = BasePanel.s10;
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(mapX+s3, mapY+s3, buttonW,buttonH,s10,s10);
            g.setPaint(background);
            g.fillRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
            g.setColor(c0);
            Stroke prevStr =g.getStroke();
            g.setStroke(BasePanel.stroke2);
            g.drawRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            g.setStroke(prevStr);
            g.setFont(font());

            String str = label();
            int sw = g.getFontMetrics().stringWidth(str);
            int x2a = mapX+((buttonW-sw)/2);
            drawBorderedString(g, str, x2a, mapY+buttonH-s10, SystemPanel.textShadowC, c0);
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click, boolean middleClick) {
            startCombat(ShipBattleUI.SMART_RESOLVE);
        };
    }
    class RetreatAllBattleSprite extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(59,59,59);
        private final Color midC = new Color(93,93,93);
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlayShipCombatPrompt parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("SHIP_COMBAT_RETREAT_ALL"); }
        private Font font()       { return narrowFont(18); }
        public void reset()       { background = null; }

        public void init(MapOverlayShipCombatPrompt p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s40 + g.getFontMetrics(font()).stringWidth(label());
            buttonH = BasePanel.s30;
            selectW = buttonW;
            selectH = buttonH;
        }
        public void setSelectionBounds(int x, int y, int w, int h) {
            selectX = x;
            selectY = y;
            selectW = w;
            selectH = h;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= selectX
                        && x <= selectX+selectW
                        && y >= selectY
                        && y <= selectY+selectH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            if (background == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D start = new Point2D.Float(mapX, 0);
                Point2D end = new Point2D.Float(mapX+buttonW, 0);
                Color[] colors = {edgeC, midC, edgeC };
                background = new LinearGradientPaint(start, end, dist, colors);
            }
            int s3 = BasePanel.s3;
            int s5 = BasePanel.s5;
            int s10 = BasePanel.s10;
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(mapX+s3, mapY+s3, buttonW,buttonH,s10,s10);
            g.setPaint(background);
            g.fillRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
            g.setColor(c0);
            Stroke prevStr =g.getStroke();
            g.setStroke(BasePanel.stroke2);
            g.drawRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            g.setStroke(prevStr);
            g.setFont(font());

            String str = label();
            int sw = g.getFontMetrics().stringWidth(str);
            int x2a = mapX+((buttonW-sw)/2);
            drawBorderedString(g, str, x2a, mapY+buttonH-s10, SystemPanel.textShadowC, c0);
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click, boolean middleClick) {
            startCombat(ShipBattleUI.RETREAT_ALL);
        };
    }
    class StartWarBattleSprite extends MapSprite {
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;
        private MapOverlayShipCombatPrompt parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("SHIP_COMBAT_START_WAR"); }
        private Font font()       { return narrowFont(16); }

        public void init(MapOverlayShipCombatPrompt p, Graphics2D g)  {
            parent = p;
            buttonH = BasePanel.s60;
            buttonW = buttonH;
            selectW = buttonW;
            selectH = buttonH;
        }
        public void setSelectionBounds(int x, int y, int w, int h) {
            selectX = x;
            selectY = y;
            selectW = w;
            selectH = h;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= selectX
                        && x <= selectX+selectW
                        && y >= selectY
                        && y <= selectY+selectH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
    		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY); 
    		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
    		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        	Empire player = player();
        	Empire alien  = mgr.results().attacker();
        	if (alien == player)
        		alien = mgr.results().defender();

        	Image flag;
            Image flagWar;
            boolean anyWar;
            EmpireView view = null;
            DiplomaticEmbassy embassy = null;
            
            if (mgr.results().isMonsterAttack()) {
            	SpaceMonster monster = mgr.results().monster();
            	flagWar = monster.image();
        		flag    = flagWar;
        		anyWar  = true;
            	int sx2 = flagWar.getWidth(null);
            	int sy2 = flagWar.getHeight(null);
            	int y = mapY + BasePanel.s15;
        		g.drawImage(flagWar, mapX, y, mapX+buttonW, y+buttonH, 0, 0, sx2, sy2, null);
        		return;
            }
            else if (alien == null) { // Should never happen...
        		Race race = player.race();
        		flagWar = race.flagWar();
        		flag    = race.flagPact();
        		anyWar  = true;
        	}
        	else {
        		view    = player.viewForEmpire(alien);
        		embassy = view.embassy();
        		flag    = view.flag();
        		flagWar = alien.race().flagWar();
        		anyWar  = embassy.anyWar();
        	}
            if (hovering) {
            	int s10 = BasePanel.s10;
            	int sx2 = flagWar.getWidth(null);
            	int sy2 = flagWar.getHeight(null);
            	g.drawImage(flagWar, mapX, mapY, mapX+buttonW, mapY+buttonH, 0, 0, sx2, sy2, null);

            	// draw pop-up
            	g.setFont(font());
            	int lineH = BasePanel.s18;
            	int bd = BasePanel.s3;
            	int cnr = BasePanel.s10;
            	int w3 = scaled(200);
            	int h3 = BasePanel.s30;
            	int x3 = boxX + boxW - w3;
            	int y3 = mapY - h3 - BasePanel.s15;
            	int ws = w3 - BasePanel.s20;
            	int xs = x3+((w3-ws)/2);
            	int ys = y3 + h3 - BasePanel.s9;
            	if (anyWar) {
            		if (embassy != null) { // Thus not a monster
            			g.setColor(MainUI.paneShadeC);
                		g.fillRoundRect(x3-bd, y3-bd, w3+bd+bd, h3+bd+bd, cnr, cnr);
            			g.setColor(MainUI.textBoxShade0);
                		g.fillRoundRect(x3, y3, w3, h3, cnr, cnr);
                		String str = embassy.treatyStatus();
                		int sw = g.getFontMetrics().stringWidth(str);
                		int xv = x3+((w3-sw)/2);
                		g.setColor(Color.RED);
                		g.drawString(str, xv, ys);
            		}
            	}
            	else {
            		int xe = x3 + w3 - s10;
            		h3 = scaled(130);
            		y3 = mapY - h3 - BasePanel.s15;
            		ys = y3 + lineH;
            		g.setColor(MainUI.paneShadeC);
            		g.fillRoundRect(x3-bd, y3-bd, w3+bd+bd, h3+bd+bd, cnr, cnr);
            		g.setColor(MainUI.textBoxShade0);
            		g.fillRoundRect(x3, y3, w3, h3, cnr, cnr);
            		String str = embassy.treatyStatus();
            		g.setColor(SystemPanel.blackText);
            		g.drawString(str, xs, ys);

            		ys += lineH;
            		str = text("RACES_DIPLOMACY_TRADE_TREATY");
            		g.drawString(str, xs, ys);
            		str = text("RACES_DIPLOMACY_TRADE_AMT", view.trade().level());
            		int sw = g.getFontMetrics().stringWidth(str);
            		int xv = xe - sw;
            		g.drawString(str, xv, ys);

            		ys += lineH;
            		str = text("RACES_DIPLOMACY_CURRENT_TRADE");
            		g.drawString(str, xs, ys);
            		int amt = (int) view.trade().profit();
            		str = text("RACES_DIPLOMACY_TRADE_AMT", str(amt));
            		sw = g.getFontMetrics().stringWidth(str);
            		xv = xe - sw;
            		g.drawString(str, xv, ys);

            		// Fleets power
            		ys += lineH;
            		str = "Military Power Ratio";
            		g.drawString(str, xs, ys);
            		int age =  alien.status().age(player());
            		float alienPower  = alien.status().lastViewValue(player, EmpireStatus.FLEET);
            		float playerPower = player.status().ageViewValue(player, EmpireStatus.FLEET, age);
            		float ratio = playerPower/alienPower;
            		str = df1.format(ratio);
            		if (age>1) // Current turn has not been computed
            			str += "(" + (age-1) + ")";
            		sw = g.getFontMetrics().stringWidth(str);
            		xv = xe - sw;
            		g.drawString(str, xv, ys);

            		// Empires power
            		ys += lineH;
            		str = "Empires Power Ratio";
            		g.drawString(str, xs, ys);
            		age =  alien.status().age(player());
            		alienPower  = alien.status().lastViewValue(player, EmpireStatus.POWER);
            		playerPower = player.status().ageViewValue(player, EmpireStatus.POWER, age);
            		ratio = playerPower/alienPower;
            		str = df1.format(ratio);
            		if (age>1) // Current turn has not been computed
            			str += "(" + (age-1) + ")";
            		sw = g.getFontMetrics().stringWidth(str);
            		xv = xe - sw;
            		g.drawString(str, xv, ys);

            		// declare war?
            		ys += lineH + s10;
            		str = label();
                    sw = g.getFontMetrics().stringWidth(str);
                    xv = x3+((w3-sw)/2);
                    g.setColor(Color.RED);
                    drawBorderedString(g, str, xv, ys, SystemPanel.grayText, Color.RED);
            	}
            }
            else {
            	g.drawImage(flag, mapX, mapY, mapX+buttonW, mapY+buttonH,
            			0, 0, flag.getWidth(null), flag.getHeight(null), null);
            }
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click, boolean middleClick) {
        	startWar();
        };
    }
    class EnterBattleSprite extends MapSprite {
        private LinearGradientPaint background;
        private final Color edgeC = new Color(44,59,30);
        private final Color midC = new Color(70,93,48);
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlayShipCombatPrompt parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        private String label()    { return text("SHIP_COMBAT_ENTER_BATTLE"); }
        private Font font()       { return narrowFont(18); }
        public void reset()       { background = null; }

        public void init(MapOverlayShipCombatPrompt p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s40 + g.getFontMetrics(font()).stringWidth(label());
            buttonH = BasePanel.s30;
            selectW = buttonW;
            selectH = buttonH;
        }
        public void setSelectionBounds(int x, int y, int w, int h) {
            selectX = x;
            selectY = y;
            selectW = w;
            selectH = h;
        }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= selectX
                        && x <= selectX+selectW
                        && y >= selectY
                        && y <= selectY+selectH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            if (background == null) {
                float[] dist = {0.0f, 0.5f, 1.0f};
                Point2D start = new Point2D.Float(mapX, 0);
                Point2D end = new Point2D.Float(mapX+buttonW, 0);
                Color[] colors = {edgeC, midC, edgeC };
                background = new LinearGradientPaint(start, end, dist, colors);
            }
            int s3 = BasePanel.s3;
            int s5 = BasePanel.s5;
            int s10 = BasePanel.s10;
            g.setColor(SystemPanel.blackText);
            g.fillRoundRect(mapX+s3, mapY+s3, buttonW,buttonH,s10,s10);
            g.setPaint(background);
            g.fillRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            Color c0 = hovering ? SystemPanel.yellowText : SystemPanel.whiteText;
            g.setColor(c0);
            Stroke prevStr =g.getStroke();
            g.setStroke(BasePanel.stroke2);
            g.drawRoundRect(mapX, mapY, buttonW,buttonH,s5,s5);
            g.setStroke(prevStr);
            g.setFont(font());

            String str = label();
            int sw = g.getFontMetrics().stringWidth(str);
            int x2a = mapX+((buttonW-sw)/2);
            drawBorderedString(g, str, x2a, mapY+buttonH-s10, SystemPanel.textShadowC, c0);
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click, boolean middleClick) {
            startCombat(ShipBattleUI.ENTER_COMBAT);
        };
    }
    class SystemFlagSprite extends MapSprite {
        private int mapX, mapY, buttonW, buttonH;
        private int selectX, selectY, selectW, selectH;

        private MapOverlayShipCombatPrompt parent;

        protected int mapX()      { return mapX; }
        protected int mapY()      { return mapY; }
        public void mapX(int i)   { selectX = mapX = i; }
        public void mapY(int i)   { selectY = mapY = i; }

        public int width()        { return buttonW; }
        public int height()       { return buttonH; }
        public void reset()       {  }

        public void init(MapOverlayShipCombatPrompt p, Graphics2D g)  {
            parent = p;
            buttonW = BasePanel.s70;
            buttonH = BasePanel.s70;
            selectW = buttonW;
            selectH = buttonH;
        }
        public void setSelectionBounds(int x, int y, int w, int h) {
            selectX = x;
            selectY = y;
            selectW = w;
            selectH = h;
        }
        @Override
        public boolean acceptDoubleClicks()         { return true; }
        @Override
        public boolean acceptWheel()                { return true; }
        @Override
        public boolean isSelectableAt(GalaxyMapPanel map, int x, int y) {
            hovering = x >= selectX
                        && x <= selectX+selectW
                        && y >= selectY
                        && y <= selectY+selectH;
            return hovering;
        }
        @Override
        public void draw(GalaxyMapPanel map, Graphics2D g) {
            if (!parent.drawSprites())
                return;
            StarSystem sys = parent.starSystem();
            Image flagImage = parent.parent.flagImage(sys);
            Image flagHaze = parent.parent.flagHaze(sys);
            g.drawImage(flagHaze, mapX, mapY, buttonW, buttonH, null);
            if (hovering) {
                Image flagHover = parent.parent.flagHover(sys);
                g.drawImage(flagHover, mapX, mapY, buttonW, buttonH, null);
            }
            g.drawImage(flagImage, mapX, mapY, buttonW, buttonH, null);
        }
        @Override
        public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click, boolean middleClick) {
	     	// BR: if 3 buttons:
	     	//   - Middle click = Reset
	     	//   - Right click = Reverse
	        if (middleClick)
	        	parent.resetFlagColor();
	        else if (rightClick)
	        	if (has3Buttons())
	        		parent.toggleFlagColor(true);
	        	else
	        		parent.resetFlagColor();
	        else
	        	parent.toggleFlagColor(false);
        };
        @Override
        public void wheel(GalaxyMapPanel map, int rotation, boolean click) {
            if (rotation < 0)
                parent.toggleFlagColor(true);
            else
                parent.toggleFlagColor(false);
        };
    }

    // ##### Console Tools
	@Override public List<ConsoleOptions> getOptions() {
		List<ConsoleOptions> options = new ArrayList<>();
		options.add(new ConsoleOptions(KeyEvent.VK_A, "A", "Auto Resolve combat."));
		options.add(new ConsoleOptions(KeyEvent.VK_S, "S", "Smart Resolve combat, retreat if overwhelmed."));
		if(options().selectedRetreatRestrictions() < 2 || options().selectedRetreatRestrictionTurns() == 0)
			options.add(new ConsoleOptions(KeyEvent.VK_R, "R", "Retreat Fleet."));
		return options;
	}
	@Override public String getMessage() {
    	Empire aiEmpire	= mgr.results().aiEmpire();
    	Empire player	= player();
        StarSystem sys	= galaxy().system(sysId);
    	String message	= displayYearOrTurn();
    	String titleStr;
        if (aiEmpire == null)
            titleStr = text("SHIP_COMBAT_TITLE_MONSTER_DESC", mgr.results().aiRaceName());
        else {
            titleStr = text("SHIP_COMBAT_TITLE_DESC");
            titleStr = aiEmpire.replaceTokens(titleStr, "alien");
        }
        message += ", " + titleStr;
 
        HashMap<String, Integer> mySizes = new HashMap<>();
        HashMap<String, Integer> aiSizes = new HashMap<>();
        for(CombatStack st : mgr.activeStacks()) {
            int putVal = st.num;
            if (st.isShip()) {
                if (st.empire() == player) {
                    if (mySizes.containsKey(st.design().sizeDesc()))
                        putVal += mySizes.get(st.design().sizeDesc());
                    mySizes.put(st.design().sizeDesc(), putVal);
                }
                else {
                    if (aiSizes.containsKey(st.design().sizeDesc()))
                        putVal += aiSizes.get(st.design().sizeDesc());
                    aiSizes.put(st.design().sizeDesc(), putVal);
                }
            }
            else if (st.isColony() && st.isArmed()) {
                if (st.empire() == player)
                    mySizes.put(text("MAIN_COLONY_BASES"), putVal);
                else
                    aiSizes.put(text("MAIN_COLONY_BASES"), putVal);
            }
        }
        message += NEWLINE + "My fleet consist of:";
        for (Entry<String, Integer> entry : mySizes.entrySet())
        	message += NEWLINE + entry.getValue() + " " + entry.getKey();
        message += NEWLINE + "Opponent fleet consist of:";
        for (Entry<String, Integer> entry : aiSizes.entrySet())
        	message += NEWLINE + entry.getValue() + " " + entry.getKey();
       
        // if unscouted, no planet info
        message += NEWLINE + "System Info:";
        boolean scouted = player.sv.isScouted(sys.id);
        if (scouted) {
        	//message += lineSplit + CommandConsole.cc().viewSystemInfo(sys, false);
        	message += NEWLINE + VIPConsole.systemInfo(sys);
        }
        else
        	message += NEWLINE + text("SHIP_COMBAT_TITLE_UNSCOUTED");

        message += NEWLINE + getMessageOption();
		return message;
	}
}
