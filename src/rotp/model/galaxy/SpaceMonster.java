/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *	 https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.model.galaxy;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rotp.model.combat.CombatStack;
import rotp.model.empires.Empire;
import rotp.model.events.IMonsterPos;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.incidents.KillMonsterIncident;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;

public abstract class SpaceMonster extends ShipFleet implements NamedObject {
	private static final long serialVersionUID = 1L;
	protected static final boolean x2 = false;		// for missiles
	protected static final boolean x5 = true;		// for missiles
	protected static final boolean LIGHT = false;	// for beam Weapons
	protected static final boolean HEAVY = true;	// for beam Weapons
	protected final String nameKey;
	protected int lastAttackerId;
	private final List<Integer> path = new ArrayList<>();
	public  float travelSpeed = 1f / (1.5f * max(1, 100.0f/galaxy().maxNumStarSystems()));
	private Float monsterLevel; // For non dynamic levels
	private transient List<CombatStack> combatStacks = new ArrayList<>();
	private transient BufferedImage shipImage;
	public  IMonsterPos event;
	private transient Float stackLevel = 1f;
	private transient int designTurn;
	protected transient ShipDesign[] designs;
	
	public SpaceMonster(String name, int empId, Float speed, Float level)	{
		super(empId, 0, 0);
		nameKey		 = name;
		monsterLevel = level;
		stackLevel	 = level;
		if (speed == null)
			travelSpeed = 1f / (1.5f * max(1, 100.0f/galaxy().maxNumStarSystems()));
		else
			travelSpeed = speed;
	}
	
	abstract protected ShipDesign designRotP();
	protected ShipDesign designMoO1()		{ return designRotP(); }
	protected void initDesigns()			{
		designs = new ShipDesign[ShipDesignLab.MAX_DESIGNS];
		monsterLevel = null;
		stackLevel	 = null;
		stackLevel();
		for (int i=0; i<ShipDesignLab.MAX_DESIGNS; i++)
			num(i, 0);
		int id = 0;
		num(id, 1);
		ShipDesign des;
		if (options().isMoO1Monster())
			des = designMoO1();
		else
			des = designRotP();
		des.setImage(image());
		des.name(text(nameKey));
		des.id(id);
		designs[id] = des;
	}

	public Empire lastAttacker()			{ return galaxy().empire(lastAttackerId); }
	public void lastAttacker(Empire c)		{ lastAttackerId = c.id; }
	public void visitSystem(int sysId)		{ path.add(sysId); }
	public List<Integer> vistedSystems()	{ return path; }
	public int vistedSystemsCount()			{ return path.size(); }
	public List<CombatStack> combatStacks()	{
		if (combatStacks == null)
			combatStacks = new ArrayList<>();
		return combatStacks; 
	}
	public Image image()					{ return image(nameKey); }
    public Image shipImage()				{ return image(); };
	public void	 initCombat()				{ 
		stackLevel(); // initialize
//		if (monsterLevel == null) {
//			if (isGuardian() && system() != null) {
//				stackLevel = options().guardianMonstersLevel() * system().monsterPlanetValue();
//				stackLevel = (float) Math.pow(stackLevel, 0.5);
//			}
//			else
//				stackLevel = options().monstersLevel();
//		}
//		else
//			stackLevel = monsterLevel;

		combatStacks().clear();
	}
	public void	 addCombatStack(CombatStack c)		{ combatStacks.add(c); }
	protected Float	 stackLevel()					{
		if (stackLevel == null) {
			if (monsterLevel == null) {
				if (isFusionGuardian() && system() != null) {
					stackLevel = options().guardianMonstersLevel() * system().monsterPlanetValue();
					stackLevel = (float) Math.pow(stackLevel, 0.5);
				}
				else
					stackLevel = options().monstersLevel();
			}
			else
				stackLevel = monsterLevel;
		}
		monsterLevel = stackLevel;
		return stackLevel;
	}
	protected int	 stackLevel(int val)			{ return (int) (val * stackLevel()); }
	protected int	 stackLevel(int val, int max)	{ return Math.min(max, stackLevel(val)); }
	@Override public String name()					{ return text(nameKey); }
	@Override public IMappedObject source()			{ return this; }
	@Override public void draw(GalaxyMapPanel map, Graphics2D g2)	{
		if (isOrionGuardian())
			drawGuard(map, g2);

		if (!displayed() || event==null) // TO DO BR: Uncomment
			return;

		int imgSize = 3;
		if (map.scaleX() > GalaxyMapPanel.MAX_FLEET_LARGE_SCALE) 
			imgSize--;
		if (map.scaleX() > GalaxyMapPanel.MAX_FLEET_SMALL_SCALE)
			imgSize--;
		// are we zoomed out too far to show a fleet of this size?
		if (imgSize < 1)
			return;

		Point.Float	  pos = event.pos();
		if (pos == null) {
			System.out.println("Draw Monster Pos = null  " + nameKey);
			pos = event.pos();
			return;
		}
		BufferedImage img = getImage();
		int w = img.getWidth();
		int h = img.getHeight();
		int x = map.mapX(pos.x);
		int y = map.mapY(pos.y);
		
		if (!hasDestination() || (destX() >= x())) {
			x -= w/4;
			y -= h/4;
			g2.drawImage(img, x, y, w, h, map);
		}
		else {
			x += w/2;
			y -= h/4;
			g2.drawImage(img, x+w, y, -w, h, map);
			
//			g2.drawImage(img, x+w/2, y-h/4, -w, h, map);
		}

		int pad = BasePanel.s8;
		selectBox().setBounds(x-pad,y-pad,w+pad+pad,h+pad+pad);
		int s5 = BasePanel.s5;
		int s10 = BasePanel.s10;
		int cnr = BasePanel.s10;
		if (map.parent().isClicked(this))
			drawSelection(g2, map, x-s5, y-s5, w+s10, h+s10, cnr);
		else if (map.parent().isHovering(this))
			drawHovering(g2, map, x-s5, y-s5, w+s10, h+s10, cnr);
		
		// Wandering path test For debug purpose!
		if (!IMonsterPos.DRAW_WANDERING)
			return;
		HashMap<Integer, Point.Float> wanderPath = event.wanderPath();
		if (wanderPath==null || wanderPath.isEmpty())
			return;
		BasicStroke stroke2 = new BasicStroke(scaled(2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); // modnar: round cap and join
	    Stroke prev = g2.getStroke();
		g2.setStroke(stroke2);
		g2.setColor(Color.blue);
		Point.Float lastPos = wanderPath.get(0);
		int r = scaled(3);
		int d = 2*r;
		int size = wanderPath.size();
		int xL = map.mapX(lastPos.x);
		int yL = map.mapY(lastPos.y);
		for (int i=0; i<size; i++) {
			pos = wanderPath.get(i);
			if (pos==null) {
				System.out.println("wanderPath.get(i) = null  i = " + i);
				continue;
			}
			x = map.mapX(pos.x);
			y = map.mapY(pos.y);
			g2.drawLine(xL, yL, x, y);
			g2.fillOval(x-r, y-r, d, d);
			xL = x;
			yL = y;
		}
		g2.setStroke(prev);
	}
	@Override public boolean canSendTo(int sysId)	{ return false; }
	@Override public float	 travelSpeed()			{ return travelSpeed; }
	@Override public boolean visibleTo(int empId)	{ return true; }
	@Override public int	 empId()				{ return -2; }
	@Override public boolean inTransit()			{ return !isOrionGuardian(); }
	@Override public float	 transitX()				{ return fromX(); }
	@Override public float	 transitY()				{ return fromY(); }
	@Override public boolean deployed()				{ return false; }
	@Override public int	 maxMapScale()			{ return GalaxyMapPanel.MAX_FLEET_HUGE_SCALE; }
	@Override public boolean isArmed()				{ return true; }
	@Override public boolean isArmedForShipCombat()	{ return true; }
	@Override public boolean isPotentiallyArmed(Empire e)	{ return true; }
	@Override public boolean decideWhetherDisplayed(GalaxyMapPanel map) {
		if (!map.displays(this))
			return false;
		if (map.scaleX() > maxMapScale())
			return false;
		return true;
	}
    @Override public Empire	 empire()            	{ return galaxy().orionEmpire(); }
    @Override protected ShipDesign[] designs()		{
		// To allow the player to change the level in game.
    	validate(); // BR: For backward compatibility.
		int turn = galaxy().currentTurn();
		designs = null; // TO DO BR: REMOVE
		if (designs == null || designTurn != turn)			{
			designTurn = turn;
			clearFleetStats();
			initDesigns();
		}
		return designs;
    }
	@Override public ShipDesign design(int i)		{ return designs()[i]; }
	@Override public StarSystem system()			{ return destination(); }
	@Override public void launch()					{ launch(fromX(), fromY()); }

	public boolean alive()	{ 
		boolean alive = false;
		for (CombatStack st: combatStacks) {
			if (!st.destroyed())
				return true;
		}
		return alive;
	}
	public void plunder()	{ notifyGalaxy(); }
	protected void removeGuardian()	{
		// Remove the Monster from the system
		system().monster(null);
        // all empires now know this system is no longer guarded
        for (Empire emp1: galaxy().empires()) 
            emp1.sv.view(sysId()).refreshSystemEntryScan();
	}
	protected void setEmpireSystem(int sysId, StarSystem s) {
		sysId(sysId);
		setXY(s);
	}
	protected DiplomaticIncident killIncident(Empire emp) { return KillMonsterIncident.create(emp.id, lastAttackerId, nameKey); }

    protected int moO1Level (int base, int step, int min, float overStep, float lowStep)	{
		float upStep	= 0.25f;
		float downStep	= 0.1f;
		float downLimit	= -2 * downStep;
		float upLimit	=  2 * upStep;
		float pFactor	= options().aiProductionModifier() - 1;

		// Above MoO1 max difficulty
		if (pFactor >= upLimit) {
			pFactor -= upLimit;
			base  += upLimit * step;
			return base + round(pFactor/overStep * step);
		}

		// Standard MoO1 difficulties
		if (pFactor >= 0)
			return base + round(pFactor/upStep * step);
		if (pFactor >= downLimit)
			return base + round(pFactor/downStep * step);

		// Bellow MoO1 min difficulty
		pFactor += downLimit;
		base  += downLimit * step;
		return max(min, base + round(pFactor/lowStep * step));
    }
    
	protected void notifyGalaxy()				{
		Empire slayerEmp = lastAttacker();
		for (Empire emp: galaxy().empires()) {
			if ((emp.id != lastAttackerId) && emp.knowsOf(slayerEmp)) {
				DiplomaticIncident inc = killIncident(emp);
				emp.diplomatAI().noticeIncident(inc, slayerEmp);
			}
		}
	}
	private int[] locateImage(Image img)		{
		int imgW = img.getWidth(null);
		int imgH = img.getHeight(null);
		BufferedImage shipImg = new BufferedImage(imgW, imgH, TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) shipImg.getGraphics();
		g.drawImage(img, 0, 0, null);
		g.dispose();
		int yMin = Integer.MAX_VALUE;
		int yMax = Integer.MIN_VALUE;
		int xMin = Integer.MAX_VALUE;
		int xMax = Integer.MIN_VALUE;

		for(int y=0; y<imgH; y++) {
			for(int x=0; x<imgW; x++) {
				int color = shipImg.getRGB(x, y);
				int alpha = color >> 24 & 255;
				if (alpha > 32) {
					yMin = yMin>y ? y : yMin;
					yMax = yMax<y ? y : yMax;
					xMin = xMin>x ? x : xMin;
					xMax = xMax<x ? x : xMax;
				}
			}
		}
		return new int[]{xMin, yMin, xMax, yMax};
	}
	protected BufferedImage getImage()			{
		// shipImage = null;
		if (shipImage == null) {
			Image baseShipImg = shipImage();
			int[] loc = locateImage(baseShipImg);
			int imgW  = loc[2]-loc[0];
			int imgH  = loc[3]-loc[1];
			int destW = BasePanel.s30;
			int destH = BasePanel.s20;
			float scaleW = (float) destW / imgW;
			float scaleH = (float) destH / imgH;
			if (scaleH < scaleW) {
				destW *= scaleH/scaleW;
			} else {
				destH *= scaleW/scaleH;
			}
				
			shipImage = newBufferedImage(destW, destH);
			Graphics2D g = (Graphics2D) shipImage.getGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY); 
			g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.drawImage(baseShipImg, 0, 0, destW, destH, loc[0], loc[1], loc[2], loc[3], null);
			g.dispose();
		}
		return shipImage;
	}
	public boolean isFusionGuardian()			{ return false; } // Not Orion Guardian
	public boolean isOrionGuardian()			{ return false; }
	public SpaceMonster getCopy()				{ return null; } // For Amoeba
	private void drawGuard(GalaxyMapPanel map, Graphics2D g2)	{
		if (!displayed()) // TO DO BR: Uncomment
			return;

		int imgSize = 3;
		if (map.scaleX() > GalaxyMapPanel.MAX_FLEET_LARGE_SCALE) 
			imgSize--;
		if (map.scaleX() > GalaxyMapPanel.MAX_FLEET_SMALL_SCALE)
			imgSize--;
		// are we zoomed out too far to show a fleet of this size?
		if (imgSize < 1)
			return;

		Point.Float	  pos = new Point.Float(fromX(), fromY());
		BufferedImage img = getImage();
		int w = img.getWidth();
		int h = img.getHeight();
		int x = map.mapX(pos.x)-w;
		int y = map.mapY(pos.y)-h;
		
//		g2.drawImage(img, x-w/4, y-h/4, w, h, map);
		g2.drawImage(img, x, y, w, h, map);

		int pad = BasePanel.s8;
		selectBox().setBounds(x-pad,y-pad,w+pad+pad,h+pad+pad);
		int s5 = BasePanel.s5;
		int s10 = BasePanel.s10;
		int cnr = BasePanel.s10;
		if (map.parent().isClicked(this))
			drawSelection(g2, map, x-s5, y-s5, w+s10, h+s10, cnr);
		else if (map.parent().isHovering(this))
			drawHovering(g2, map, x-s5, y-s5, w+s10, h+s10, cnr);
	}
}
