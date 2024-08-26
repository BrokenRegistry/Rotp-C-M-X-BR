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
package rotp.model.tech;

import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.JOIN_BEVEL;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static rotp.model.combat.CombatShield.ABOVE;
import static rotp.model.combat.CombatShield.BELLOW;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import rotp.model.combat.CombatShield;
import rotp.model.combat.CombatStack;
import rotp.model.combat.CombatStackColony;
import rotp.model.combat.CombatStackMissile;
import rotp.model.combat.CombatStackShip;
import rotp.model.empires.Empire;
import rotp.model.game.IGameOptions;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipWeaponBeam;
import rotp.ui.BasePanel;
import rotp.ui.combat.ShipBattleUI;

public final class TechShipWeapon extends Tech {
	private static int WIND_UP_FRAMES = 3;
	private static int HOLD_FRAMES = 0;

	private final int s10 = scaled(10);
	
    private int damageLow = 0;
    private int damageHigh = 0;
    public int range = 1;

    public boolean heavyAllowed = false;
    private int heavyDamageLow = 0;
    private int heavyDamageHigh = 0;
    public int heavyRange = 2;

    public int attacksPerRound = 1;
    public int computer = 0;
    public float enemyShieldMod = 1;
    public boolean streaming = false;
    private String soundEffect = "ShipLaser";

    // graphic effects
    public int weaponSpread = 1;
    public int windUpFrames = WIND_UP_FRAMES;
    public int holdFrames = HOLD_FRAMES;

    private int beamStroke, dashStroke;
    private transient Color beamColor;
    private transient Color beamColor2;
    private transient Color cycleColor;
    private transient Color cycleColor2;
    private Stroke weaponStroke;
    private long lastSleep;

    public int damageLow()       { return (int) (session().damageBonus() * damageLow); }
    public int damageHigh()      { return (int) (session().damageBonus() * damageHigh); }
    public int heavyDamageLow()  { return (int) (session().damageBonus() * heavyDamageLow); }
    public int heavyDamageHigh() { return (int) (session().damageBonus() * heavyDamageHigh); }
    
    public float comparableDamageValue() {
        return 7.0f* level * 0.5f*(damageLow() + damageHigh()) * attacksPerRound / enemyShieldMod / (size + power);
    }  
    protected String soundEffect()		{ return "ShipLaser"; }
    protected String soundEffectMulti()	{ return "ShipMultiLaser"; }
    protected String newSoundEffect()	{ return soundEffect; } // BR:

    public TechShipWeapon(String typeId, int lv, int seq, boolean b, TechCategory c) {
        id(typeId, seq);
        typeSeq = seq;
        level = lv;
        cat = c;
        free = b;
        init();
//        System.out.println(soundEffect); // TO DO BR: COMMENT
//        playAudioClip(soundEffect);
//        sleep(1500);
    }
    @Override
    public boolean canBeMiniaturized()      { return true; }
    @Override
    public void init() {
        super.init();
        techType = Tech.SHIP_WEAPON;
        dashStroke = 0;

        switch(typeSeq) {
            case 0: // LASER
                damageLow = 1;
                damageHigh = 4;
                heavyAllowed = true;
                heavyDamageLow = 1;
                heavyDamageHigh = 7;
                cost = 8;
                size = 10;
                power = 25;
                beamColor = new Color(0x9f,0x33,0x35);
                beamStroke = 1;
                soundEffect = "ShipLaser";
                break;
            case 1: // GATLING LASER
                damageLow = 1;
                damageHigh = 4;
                attacksPerRound = 4;
                cost = 20;
                size = 20;
                power = 70;
                beamColor = new Color(0xbd,0x23,0x3a);
                beamColor2 = new Color(0x72,0x10,0x10);
                beamStroke = 1;
                soundEffect = "ShipMultiLaser";
                break;
            case 2: // NEUTRON PELLET GUN
                damageLow = 2;
                damageHigh = 5;
                enemyShieldMod = .5f;
                cost = 7.5f;
                size = 15;
                power = 25;
                beamColor = new Color(0xa8,0xb4,0x85);
                beamStroke = 1;
                dashStroke = 3;
                soundEffect = "ShipNeutronPelletGun";
                break;
            case 3: // ION CANNON
                damageLow = 3;
                damageHigh = 8;
                heavyAllowed = true;
                heavyDamageLow = 3;
                heavyDamageHigh = 15;
                cost = 10;
                size = 15;
                power = 35;
                beamColor = new Color(0xa4,0x7b,0x56);
                beamStroke = 1;
                soundEffect = "ShipIonCannon";
                break;
            case 4: // MASS DRIVER
                weaponSpread = 1;
                damageLow = 5;
                damageHigh = 8;
                enemyShieldMod = .5f;
                cost = 18;
                size = 55;
                power = 50;
                beamColor = new Color(0xac,0xac,0xac);
                beamStroke = 1;
                dashStroke = 3;
                soundEffect = "ShipMassDriver";
                break;
            case 5: // NEUTRON BLASTER
                damageLow = 3;
                damageHigh = 12;
                heavyAllowed = true;
                heavyDamageLow = 3;
                heavyDamageHigh = 24;
                cost = 15;
                size = 20;
                power = 60;
                beamColor = new Color(0x3c,0x03,0x78);
                beamColor2 = new Color(0xcd,0xb1,0xe8);
                beamStroke = 1;
                soundEffect = "ShipNeutronBlaster";
                break;
            case 6: // GRAVITON BEAM
                damageLow = 1;
                damageHigh = 15;
                streaming = true;
                weaponSpread = 4;
                holdFrames = 6;
                cost = 12;
                size = 30;
                power = 60;
                beamColor = new Color(0x28,0x00,0x7e);
                beamColor2 = new Color(0xf5,0xb7,0xf3);
                beamStroke = 1;
                soundEffect = "ShipGravitonBeam";
                break;
            case 7: // HARD BEAM
                weaponSpread = 1;
                damageLow = 8;
                damageHigh = 12;
                enemyShieldMod = .5f;
                cost = 25;
                size = 50;
                power = 100;
                beamColor = new Color(0xf0,0xb5,0x6e);
                beamColor2 = new Color(0xcb,0x81,0x29);
                beamStroke = 1;
                soundEffect = "ShipHardBeam";
                break;
            case 8: // FUSION BEAM
                damageLow = 4;
                damageHigh = 16;
                heavyAllowed = true;
                heavyDamageLow = 4;
                heavyDamageHigh = 30;
                cost = 13;
                size = 20;
                power = 75;
                beamColor = new Color(0x0c,0x56,0x0c);
                beamColor2 = new Color(0x82,0xc8,0x82);
                beamStroke = 1;
                soundEffect = "ShipFusionBeam";
                break;
            case 9: // MEGABOLT CANNON
                damageLow = 2;
                damageHigh = 20;
                weaponSpread = 4;
                holdFrames = 6;
                computer = 3;
                cost = 16;
                size = 30;
                power = 65;
                beamColor = new Color(0xe5,0xee,0xbe);
                cycleColor = new Color(0xce,0xe2,0x89);
                cycleColor2 = new Color(0xea,0xbb,0xea);
                beamStroke = 1;
                soundEffect = "ShipMegaBoltCannon";
                break;
            case 10: // PHASOR
                damageLow = 5;
                damageHigh = 20;
                heavyAllowed = true;
                heavyDamageLow = 5;
                heavyDamageHigh = 40;
                cost = 18;
                size = 20;
                power = 90;
                beamColor = new Color(0xb6,0x07,0x5a);
                beamColor2 = new Color(0xde,0x8d,0xb3);
                beamStroke = 1;
                soundEffect = "ShipPhasor";
                break;
            case 11: // AUTO-BLASTER
                damageLow = 4;
                damageHigh = 16;
                attacksPerRound = 3;
                cost = 24;
                size = 30;
                power = 90;
                beamColor = new Color(0x24,0xbe,0x93);
                beamColor2 = new Color(0x03,0x25,0x1d);
                beamStroke = 1;
                soundEffect = "ShipAutoBlaster";
                break;
            case 12: // TACHYON BEAM
                damageLow = 1;
                damageHigh = 25;
                weaponSpread = 4;
                holdFrames = 6;
                streaming = true;
                cost = 18;
                size = 30;
                power = 80;
                beamColor = new Color(0x36,0x06,0x00);
                beamColor2 = new Color(0xe1,0xa3,0x8d);
                beamStroke = 1;
                soundEffect = "ShipTachyonBeam";
                break;
            case 13: // GAUSS AUTO-CANNON
                damageLow = 7;
                damageHigh = 10;
                enemyShieldMod = .5f;
                attacksPerRound = 4;
                cost = 40;
                size = 105;
                power = 105;
                beamColor = new Color(0xac,0xac,0xac);
                beamStroke = 3;
                dashStroke = 1;
                soundEffect = "ShipGaussAutoCannon";
                break;
            case 14: // PARTICLE BEAM
                damageLow = 10;
                damageHigh = 20;
                enemyShieldMod = .5f;
                cost = 26;
                size = 90;
                power = 75;
                beamColor = new Color(0x3b,0x39,0x48);
                beamColor2 = new Color(0x95,0x94,0x9d);
                dashStroke = 1;
                beamStroke = 3;
                soundEffect = "ShipParticleBeam";
                break;
            case 15: // PLASMA CANNON
                damageLow = 6;
                damageHigh = 30;
                weaponSpread = 4;
                cost = 24;
                size = 30;
                power = 110;
                beamColor = new Color(0xfe,0x29,0x28);
                beamColor2 = new Color(0xce,0x1f,0x1e);
                beamStroke = 1;
                soundEffect = "ShipPlasmaCannon";
                break;
            case 16: // DEATH RAY
                range = 1;
                damageLow = 200;
                damageHigh = 1000;
                weaponSpread = 7;
                holdFrames = 6;
                restricted = true;
                cost = 120;
                size = 2000;
                power = 2000;
                beamColor = new Color(0x56,0x02,0xc2);
                beamColor2 = new Color(0xcb,0x33,0x5e);
                beamStroke = 1;
                soundEffect = "ShipDeathRay";
                break;
            case 17: // DISRUPTOR
                damageLow = 10;
                damageHigh = 40;
                range = 2;
                cost = 100;
                size = 70;
                power = 160;
                beamColor = new Color(0xa4,0x7b,0x56);
                beamColor2 = new Color(0x82,0xc8,0x82);
                beamStroke = 1;
                soundEffect = "ShipDisruptor";
                break;
            case 18: // PULSE PHASOR
                damageLow = 5;
                damageHigh = 20;
                attacksPerRound = 3;
                holdFrames = 4;
                cost = 42;
                size = 40;
                power = 120;
                beamColor = new Color(0xb6,0x07,0x5a);
                cycleColor = new Color(0xde,0x8d,0xb3);
                beamStroke = 1;
                soundEffect = "ShipPulsePhasor";
                break;
            case 19: // TRI-FOCUS PLASMA CANNON
                damageLow = 20;
                damageHigh = 50;
                weaponSpread = 2;
                cost = 55;
                size = 65;
                power = 180;
                beamColor = new Color(0xfe,0x29,0x28);
                beamColor2 = new Color(0xce,0x1f,0x1e);
                beamStroke = 1;
                soundEffect = "ShipTriFocusPlasmaCannon";
                break;
            case 20: // STELLAR CONVERTOR
                damageLow = 10;
                damageHigh = 35;
                attacksPerRound = 4;
                weaponSpread = 4;
                holdFrames = 4;
                range = 3;
                cost = 105;
                size = 200;
                power = 300;
                beamColor = new Color(0xff,0xff,0xb0);
                cycleColor = new Color(0xff,0xff,0xff);
                beamStroke = 1;
                soundEffect = "ShipStellarConvertor";
                break;
            case 21: // MAULER DEVICE
                damageLow = 20;
                damageHigh = 100;
                weaponSpread = 7;
                holdFrames = 4;
                cost = 120;
                size = 150;
                power = 300;
                beamColor = new Color(0x00,0xaf,0x7d);
                beamColor2 = new Color(0x4c,0xd0,0xab);
                beamStroke = 1;
                soundEffect = "ShipMauler";
                break;
            case 22: // AMOEBA STREAM
                damageLow = 250;
                damageHigh = 1000;
                range = 3;
                streaming = true;
                restricted = true;
                beamColor = Color.green;
                beamStroke = 5;
                soundEffect = "ShipAmoebaStream";
                break;
            case 23: // CRYSTAL RAY
                damageLow = 100;
                damageHigh = 300;
                range = 3;
                attacksPerRound = 4;
                restricted = true;
                beamColor = Color.white;
                beamStroke = 4;
                soundEffect = "ShipMultiLaser";
                break;
            case 24: // JELLYFISH DART
                range = 2;
                damageLow = 1;
                damageHigh = 5;
                enemyShieldMod = .2f;
                weaponSpread = 3;
                holdFrames = 3;
                restricted = true;
                cost = 15;
                size = 20;
                power = 25;
                beamColor = Color.blue;
                beamStroke = 1;
                dashStroke = 3;
                soundEffect = "ShipJellyfishDart";
                break;
        }
    }
    @Override
    public float baseValue(Empire c) { return c.ai().scientist().baseValue(this); }
    @Override
    public float warModeFactor()        { return 2; }
    @Override
    public boolean providesShipComponent()  { return true; }
    @Override
    public float baseCost()   { return cost; }
    @Override
    public float baseSize()   { return size; }
    @Override
    public float basePower()  { return power; }
    @Override
    public boolean isObsolete(Empire c) {
        return (c.tech().topShipWeaponTech() != null) && (level < c.tech().topShipWeaponTech().level);
    }
    @Override
    public void provideBenefits(Empire c) {
        super.provideBenefits(c);

        ShipWeaponBeam sh = new ShipWeaponBeam(this, false);
        c.shipLab().addWeapon(sh);
        if (!isObsolete(c))
            c.tech().topShipWeaponTech(this);
        
        if (heavyAllowed) {
            ShipWeaponBeam sh2 = new ShipWeaponBeam(this, true);
            c.shipLab().addWeapon(sh2);
        }
        if (c.isPlayerControlled())
            galaxy().giveAdvice("MAIN_ADVISOR_SHIP_WEAPON");
    }
    @Override // Shielded
    public void drawIneffectiveAttack(CombatStack source, CombatStack target, int wpnNum, int count) {
        ShipBattleUI ui = source.mgr.ui;
        if (ui == null)
            return;

        int stW = ui.stackW();
        int stH = ui.stackH();
        int st0X = ui.stackX(source);
        int st0Y = ui.stackY(source);
        int st1X = ui.stackX(target);
        int st1Y = ui.stackY(target);

        int x0 = st0X > st1X ? st0X+(stW/3) :st0X+(stW*2/3);
        int y0 = st0Y+stH/2;
        int x1 = st1X+stW/2;
        int y1 = st1Y+stH/2;
        drawAttack(source, target, x0, y0, x1, y1, wpnNum, -1f, count, stW, stH, 0f);
    }
    @Override  // Miss
    public void drawUnsuccessfulAttack(CombatStack source, CombatStack target, int wpnNum, int count) {
        ShipBattleUI ui = source.mgr.ui;
        if (ui == null)
            return;

        int stW = ui.stackW();
        int stH = ui.stackH();
        int st0X = ui.stackX(source);
        int st0Y = ui.stackY(source);
        int st1X = ui.stackX(target);
        int st1Y = ui.stackY(target);

        int xRoll = roll(0,2);
        int xMiss = stW/4 + (xRoll *stW/4);
        int yMiss = xRoll == 1 ? stH/4+roll(0,1) *stH/2 : stH/4+(roll(0,2) *stH/4);

        int x0 = st0X > st1X ? st0X+(stW/3) :st0X+(stW*2/3);
        int y0 = st0Y+stH/2;
        int x1 = st1X+xMiss;
        int y1 = st1Y+yMiss;
        drawAttack(source, target, x0, y0, x1, y1, wpnNum, 0f, count, stW, stH, 0f);
    }
    @Override
    public void drawSuccessfulAttack(CombatStack source, CombatStack target, int wpnNum, float dmg, int count, float force) {
        ShipBattleUI ui = source.mgr.ui;
        if (ui == null)
            return;

        int stW = ui.stackW();
        int stH = ui.stackH();
        int st0X = ui.stackX(source);
        int st0Y = ui.stackY(source);
        int st1X = ui.stackX(target);
        int st1Y = ui.stackY(target);

        int x0 = st0X > st1X ? st0X+(stW/3) :st0X+(stW*2/3);
        int y0 = st0Y+stH/2;
        int x1 = st1X+stW/2;
        int y1 = st1Y+stH/2;
        drawAttack(source, target, x0, y0, x1, y1, wpnNum, dmg, count, stW, stH, force);
    }
    private long pause(long time) {
    	long oldLastSleep = lastSleep;
    	lastSleep = System.currentTimeMillis();
    	long duration = time-(lastSleep-oldLastSleep);
    	// System.out.println("duration = " + duration);
    	if (duration > 2)
    		sleep(duration);
    	return duration;
    }
    private void pauseInit() { lastSleep = System.currentTimeMillis(); }
    private void drawAttack(CombatStack source, CombatStack target,
    		int weaponX, int weaponY, int impactX, int impactY,
    		int wpnNum, float damage, int count, int boxW, int boxH, float force) {
    	if (options().shieldType2D()) {
    		drawOldAttack(source, target, weaponX, weaponY, impactX, impactY, wpnNum, damage, count, boxW, boxH, force);
    		return;
    	}
        IGameOptions opt	 = options();
    	boolean tripleBuffer = opt.shieldType3Buffer();
    	boolean ForceShield	 = opt.alwaysShowsShield();
    	if (!ForceShield) {
        	boolean inNebula	= source.mgr.system().inNebula();
        	boolean hasShield	= target.shieldLevel()>0;
        	boolean notMissed	= damage != 0;
        	boolean askShield3D	= tripleBuffer || opt.shieldType3D();
        	boolean shielded	= notMissed && hasShield && !inNebula;
        	boolean showShield	= askShield3D && shielded;
        	if ( !showShield ) {
        		drawNoShieldAttack(source, target, weaponX, weaponY, impactX, impactY, wpnNum, damage, count, boxW, boxH, force);
        		return;
        	}
    	}
        ShipBattleUI ui = source.mgr.ui;
        if (!source.mgr.showAnimations()) {
        	ui.animationCompleted();
        	return;
        }

        BufferedImage shipImg = new BufferedImage(boxW, boxH, TYPE_INT_ARGB);
		Graphics2D gS = (Graphics2D) shipImg.getGraphics();
        drawStack(ui, gS, target, 0, 0, boxW, boxH);
        gS.dispose();

        int targetTLx	= ui.stackX(target); // Top Left points
		int targetTLy	= ui.stackY(target);
        int targetCtrX	= targetTLx + boxW/2;
		int targetCtrY	= targetTLy + boxH/2;
		int sourceTLx	= ui.stackX(source);
		int sourceTLy	= ui.stackY(source);
		int shipTLx		= targetTLx+(boxW-shipImg.getWidth())/2;
		int shipTLy		= targetTLy+(boxH-shipImg.getHeight())/2;
		int distance	= (int) Math.round(Math.sqrt (
								Math.pow(weaponX-impactX, 2) +
								Math.pow(weaponY-impactY, 2)));
		int distFactor	= 8*distance;
		int weaponDx	= (impactX-weaponX)/distFactor;
		int weaponDy	= (impactY-weaponY)/distFactor;
        int weaponDz	= opt.weaponZRandom();
        int weaponZ		= scaled(opt.weaponZposition()+roll(-weaponDz,weaponDz));
        boolean isAbove = weaponZ>=0;
        long sleepTime	= opt.beamAnimationDelay(); // Original = 50;
        int targetSize	= 3;
        if (target.design() != null)
        	targetSize = target.design().size();
        int sourceSize	= 3;
        boolean playerEcho = opt.playerSoundEcho() && !target.isColony();
        if (source.design() != null)
        	sourceSize = source.design().size();
        int shieldBorders = opt.shieldBorder(sourceSize);
		boolean playerIsTarget = (target.empire() != null) && target.empire().isPlayer();

        Graphics2D g = (Graphics2D) ui.getGraphics();
        ShipComponent wpn = source.weapon(wpnNum);
        int wpnCount = count / attacksPerRound / source.num;

        int beamWidth = scaled(3);
        int spotWidth = scaled(attacksPerRound *(1 + 2*weaponSpread));
		if ((dashStroke > 0) && (weaponStroke == null)) {
			float dash[] = new float[]{0, beamWidth*dashStroke};
			weaponStroke = new BasicStroke(beamWidth, CAP_ROUND, JOIN_BEVEL, 0, dash, 0);
		}
        if (weaponStroke != null)
            g.setStroke(weaponStroke);
        else if (wpn.heavy()) {
        	spotWidth = scaled(attacksPerRound * (1 + 2*(beamStroke+weaponSpread-1)));
        	g.setStroke(BasePanel.baseStroke(beamStroke*2+1));
        } else {
        	spotWidth = scaled(2*(beamStroke+weaponSpread-1)*attacksPerRound);
        	g.setStroke(BasePanel.baseStroke(beamStroke*2));
        }
        
        int windUpFramesNum = opt.beamWindupFrames();
        int holdFramesNum = max(0, holdFrames + opt.beamHoldFrames());
        if (wpn.heavy())
        	holdFramesNum += opt.heavyBeamHoldFrames();
        int fadingFramesNum = opt.shieldFadingFrames()? windUpFramesNum+2 : 0;
        int landUpFramesNum = windUpFramesNum;

		CombatShield cs = new CombatShield(holdFramesNum, landUpFramesNum, fadingFramesNum,
				boxW, boxH, targetCtrX, targetCtrY, target.shieldBaseColor(), opt.shieldEnveloping(), shieldBorders,
				opt.shieldTransparency(), opt.shieldFlickering(), opt.shieldNoisePct(), (int)target.maxShield, shipImg,
				weaponX, weaponY, weaponZ, beamColor, spotWidth, attacksPerRound, damage, force);

		Rectangle shieldRec = cs.shieldRec();
		int shieldTLx	= shieldRec.x;
		int shieldTLy	= shieldRec.y;
		int shieldWidth	= shieldRec.width;
		int shieldHeight= shieldRec.height;
		
		BufferedImage shieldBackGround = null;
		int shipX3B = shipTLx - shieldTLx;
		int shipY3B = shipTLy - shieldTLy;
		int weaponX3B	= weaponX - shieldTLx;
		int weaponY3B	= weaponY - shieldTLy;
		shieldBackGround = new BufferedImage(shieldWidth, shieldHeight, TYPE_INT_ARGB);
		Graphics2D gbg = (Graphics2D) shieldBackGround.getGraphics();
		gbg.setColor(Color.black);
		gbg.fillOval(0, 0, shieldWidth, shieldHeight);
		gbg.dispose();

		// Full Beam trajectory generation
		ArrayList<Line2D.Double> lines = new ArrayList<>();
		double insideRatio = 0;
		int[] xAdj = new int[attacksPerRound];
		int[] yAdj = new int[attacksPerRound];
		int[] zAdj = new int[attacksPerRound]; // init to 0 by default
		int roll = 3*(targetSize+1);
		for(int i = 0; i < attacksPerRound; ++i) {
			xAdj[i] = scaled(roll(-roll,roll));
			yAdj[i] = scaled(roll(-roll,roll));
		}
		int[][] shipImpact = cs.setImpact(xAdj, yAdj, zAdj);
		BufferedImage[][] shieldArray = cs.getShieldArray();
		insideRatio	= cs.meanInsideRatio();
		for(int i = 0; i < attacksPerRound; ++i) {
			if (weaponSpread > 1) {
				int xMod = (sourceTLy == targetTLy) ? 0 : 1;
				int yMod = (sourceTLx == targetTLx) ? 0 : 1;
				if ((sourceTLx < targetTLx) && (sourceTLy < targetTLy))
					xMod = -1;
				else if ((sourceTLx > targetTLx) && (sourceTLy > targetTLy))
					xMod = -1;
				for (int n = -1 * weaponSpread; n <= weaponSpread; n++) {
					int adj = scaled(n);
					lines.addAll(addMultiLines(sourceSize, wpnCount, weaponX, weaponY,
							shipImpact[i][0]+(xMod*adj), shipImpact[i][1]+(yMod*adj)));
				}
			} else {
				lines.addAll(addMultiLines(sourceSize, wpnCount, weaponX, weaponY, shipImpact[i][0], shipImpact[i][1]));
			}
		}
		double fraction =  (1-insideRatio) / (windUpFramesNum);
		//
		// Animations start Here
		//
		
		boolean ellipse = false;
		
		float hiddenBeamAlpha = 0.3f;
		if(beamColor2 == null && cycleColor == null)
			beamColor2 = multColor(beamColor, 0.75f);
		setPaint(g, 0, weaponX, weaponY, weaponDx, weaponDy);
        // Start playing sound if Player is shooting
		if (!playerIsTarget) {		
	        if (opt.newWeaponSound())
	        	 playAudioClip(newSoundEffect()); // BR:
	        else if (attacksPerRound > 1)
	            playAudioClip(soundEffectMulti());
	        else
	            playAudioClip(soundEffect());
		}
		// Beams Progression toward target
		SortedMap<Integer, ArrayList<Line2D.Double>> partLines = new TreeMap<>();
		boolean doubleMap = tripleBuffer && !isAbove;
		SortedMap<Integer, ArrayList<Line2D.Double>> bellowLines = null;
		pauseInit();
		if (doubleMap)
			bellowLines = new TreeMap<>();
		for(int i = 0; i < windUpFramesNum; ++i) {
			ArrayList<Line2D.Double> pl = new ArrayList<>();
			ArrayList<Line2D.Double> pb = null;
			if (doubleMap)
				pb = new ArrayList<>();
			for(Line2D.Double line : lines) {
				double newX1 = line.getX1() + (line.getX2() - line.getX1()) * i * fraction;
				double newY1 = line.getY1() + (line.getY2() - line.getY1()) * i * fraction;
				double newX2 = line.getX1() + (line.getX2() - line.getX1()) * (i + 1) * fraction;
				double newY2 = line.getY1() + (line.getY2() - line.getY1()) * (i + 1) * fraction;
				pl.add(new Line2D.Double(newX1, newY1, newX2, newY2));
				if (doubleMap) // Lines relative to shield
					pb.add(new Line2D.Double(newX1-shieldTLx, newY1-shieldTLy, newX2-shieldTLx, newY2-shieldTLy));
			}
			partLines.put(i, pl);
			if (doubleMap)
				bellowLines.put(i, pb);
			setPaint(g, 0, weaponX, weaponY, weaponDx, weaponDy);
			paintLines(pl, g);
			if (!isAbove)
				g.drawImage(shipImg, shipTLx, shipTLy, null);
			pause(sleepTime);
		}
		
		// Beams reach the shield
		ArrayList<Line2D.Double> hitLines = new ArrayList<>(); // The ones that go thru the shield
		setPaint(g, windUpFramesNum, weaponX, weaponY, weaponDx, weaponDy);
		if (tripleBuffer) { // hitLines relative to shield
			for(Line2D.Double line : lines) {
				double newX1 = line.getX1()-shieldTLx + (line.getX2()-line.getX1()) * windUpFramesNum * fraction;
				double newY1 = line.getY1()-shieldTLy + (line.getY2()-line.getY1()) * windUpFramesNum * fraction;
				double newX2 = line.getX2()-shieldTLx;
				double newY2 = line.getY2()-shieldTLy;
				hitLines.add(new Line2D.Double(newX1, newY1, newX2, newY2));
			}
		} else {
			for(Line2D.Double line : lines) {
				double newX1 = line.getX1() + (line.getX2() - line.getX1()) * windUpFramesNum * fraction;
				double newY1 = line.getY1() + (line.getY2() - line.getY1()) * windUpFramesNum * fraction;
				double newX2 = line.getX2();
				double newY2 = line.getY2();
				hitLines.add(new Line2D.Double(newX1, newY1, newX2, newY2));
			}
		}
			
        // Start playing sound if Player is the target
		if (playerIsTarget) {		
	        if (opt.newWeaponSound())
				if(playerEcho)
					playAudioClip(newSoundEffect(), targetSize);
				else
					playAudioClip(newSoundEffect());
	        else if (attacksPerRound > 1)
	            playAudioClip(soundEffectMulti());
	        else
	            playAudioClip(soundEffect());
		}

		// Draw first shield part
		if (tripleBuffer) {
			if (isAbove) {
				BufferedImage buffer = new BufferedImage(shieldWidth, shieldHeight, TYPE_INT_ARGB);
				Graphics2D gb = (Graphics2D) buffer.getGraphics();
				setPaint(gb, windUpFramesNum, weaponX3B, weaponY3B, weaponDx, weaponDy);
				gb.drawImage(shieldArray[BELLOW][0], 0, 0, null);
				gb.drawImage(shipImg, shipX3B, shipY3B, null);
				gb.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hiddenBeamAlpha));
				paintLines(hitLines, gb); // hitting lines are in-between
				gb.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				gb.drawImage(shieldArray[ABOVE][0], 0, 0, null);
				gb.dispose();

				g.drawImage(buffer, shieldTLx, shieldTLy, null);
				setPaint(g, windUpFramesNum, weaponX, weaponY, weaponDx, weaponDy);
				paintLines(partLines, g); // visible line are above
			} else {
				BufferedImage buffer = new BufferedImage(shieldWidth, shieldHeight, TYPE_INT_ARGB);
				Graphics2D gb = (Graphics2D) buffer.getGraphics();
				setPaint(gb, windUpFramesNum, weaponX3B, weaponY3B, weaponDx, weaponDy);
				gb.drawImage(shieldArray[BELLOW][0], 0, 0, null);
				paintLines(hitLines, gb); // hitting lines are in-between
				gb.drawImage(shipImg, shipX3B, shipY3B, null);
				gb.drawImage(shieldArray[ABOVE][0], 0, 0, null);		
				gb.dispose();

				setPaint(g, windUpFramesNum, weaponX, weaponY, weaponDx, weaponDy);
				paintLines(partLines, g); // visible line are bellow
				g.drawImage(buffer, shieldTLx, shieldTLy, null);
			}
		} else {
			if (isAbove) {
				g.drawImage(shieldArray[BELLOW][0], shieldTLx, shieldTLy, null);
				g.drawImage(shipImg, shipTLx, shipTLy, null);
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hiddenBeamAlpha));
				paintLines(hitLines, g); // hitting lines are in-between
				g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				g.drawImage(shieldArray[ABOVE][0], shieldTLx, shieldTLy, null);
				paintLines(partLines, g); // visible line are above
			} else {
				paintLines(partLines, g); // visible line are bellow
				g.drawImage(shieldArray[BELLOW][0], shieldTLx, shieldTLy, null);
				paintLines(hitLines, g); // hitting lines are in-between
				g.drawImage(shipImg, shipTLx, shipTLy, null);
				g.drawImage(shieldArray[ABOVE][0], shieldTLx, shieldTLy, null);				
			}
		}
		pause(sleepTime);

		// Show Continuous
		if (tripleBuffer) {
			if (isAbove) {
				for(int i = 0; i < holdFramesNum; i++) {
					BufferedImage buffer = new BufferedImage(shieldWidth, shieldHeight, TYPE_INT_ARGB);
					Graphics2D gb = (Graphics2D) buffer.getGraphics();
					gb.drawImage(shieldBackGround, 0, 0, null);
					setPaint(gb, i+1+windUpFramesNum, weaponX3B, weaponY3B, weaponDx, weaponDy);
					int shieldIdx = i+1;
					gb.drawImage(shieldArray[BELLOW][shieldIdx], 0, 0, null);
					gb.drawImage(shipImg, shipX3B, shipY3B, null);
					gb.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hiddenBeamAlpha));
					paintLines(hitLines, gb); // hitting lines are in-between
					gb.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
					gb.drawImage(shieldArray[ABOVE][shieldIdx], 0, 0, null);
					gb.dispose();
		
					g.drawImage(buffer, shieldTLx, shieldTLy, null);
					setPaint(g, i+1+windUpFramesNum, weaponX, weaponY, weaponDx, weaponDy);
					paintLines(partLines, g); // visible line are above
					pause(sleepTime);
				}
			} else {
				for(int i = 0; i < holdFramesNum; i++) {
					BufferedImage buffer = new BufferedImage(shieldWidth, shieldHeight, TYPE_INT_ARGB);
					Graphics2D gb = (Graphics2D) buffer.getGraphics();
					gb.drawImage(shieldBackGround, 0, 0, null);
					setPaint(gb, i+1+windUpFramesNum, weaponX3B, weaponY3B, weaponDx, weaponDy);
					paintLines(bellowLines, gb);
					int shieldIdx = i+1;
					gb.drawImage(shieldArray[BELLOW][shieldIdx], 0, 0, null);
					paintLines(hitLines, gb); // hitting lines are in-between
					gb.drawImage(shipImg, shipX3B, shipY3B, null);
					gb.drawImage(shieldArray[ABOVE][shieldIdx], 0, 0, null);
					gb.dispose();
	
					setPaint(g, i+1+windUpFramesNum, weaponX, weaponY, weaponDx, weaponDy);
					paintLines(partLines, g); // visible line are bellow
					g.drawImage(buffer, shieldTLx, shieldTLy, null);
					pause(sleepTime);
				}			
			}
		} else {
			if (isAbove) {
				for(int i = 0; i < holdFramesNum; i++) {
					if (ellipse)
						g.drawImage(shieldBackGround, shieldTLx, shieldTLx, null);
					else
						ui.paintImmediately(shieldRec);
					setPaint(g, i+1+windUpFramesNum, weaponX, weaponY, weaponDx, weaponDy);
					int shieldIdx = i+1;
					g.drawImage(shieldArray[BELLOW][shieldIdx], shieldTLx, shieldTLy, null);
					g.drawImage(shipImg, shipTLx, shipTLy, null);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hiddenBeamAlpha));
					paintLines(hitLines, g); // hitting lines are in-between
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
					g.drawImage(shieldArray[ABOVE][shieldIdx], shieldTLx, shieldTLy, null);
		
					paintLines(partLines, g);
					pause(sleepTime);
				}
			} else {
				for(int i = 0; i < holdFramesNum; i++) {
					if (ellipse)
						g.drawImage(shieldBackGround, shieldTLx, shieldTLx, null);
					else
						ui.paintImmediately(shieldRec);
					setPaint(g, i+1+windUpFramesNum, weaponX, weaponY, weaponDx, weaponDy);
					paintLines(partLines, g);
					int shieldIdx = i+1;
					g.drawImage(shieldArray[BELLOW][shieldIdx], shieldTLx, shieldTLy, null);
					paintLines(hitLines, g); // hitting lines are in-between
					g.drawImage(shipImg, shipTLx, shipTLy, null);
					g.drawImage(shieldArray[ABOVE][shieldIdx], shieldTLx, shieldTLy, null);
					pause(sleepTime);
				}			
			}
		}
		
		// Show end of beam
		if (tripleBuffer) {
			if (isAbove) {
				for(int i = 0; i < windUpFramesNum; ++i) {
					BufferedImage buffer = new BufferedImage(shieldWidth, shieldHeight, TYPE_INT_ARGB);
					Graphics2D gb = (Graphics2D) buffer.getGraphics();
					gb.drawImage(shieldBackGround, 0, 0, null);
					setPaint(gb, i+1+windUpFramesNum+holdFramesNum, weaponX3B, weaponY3B, weaponDx, weaponDy);
					int shieldIdx = i+1+holdFramesNum;
					
					gb.drawImage(shieldArray[BELLOW][shieldIdx], 0, 0, null);
					gb.drawImage(shipImg, shipX3B, shipY3B, null);
					gb.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hiddenBeamAlpha));
					paintLines(hitLines, gb); // hitting lines are in-between
					gb.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
					gb.drawImage(shieldArray[ABOVE][shieldIdx], 0, 0, null);
					gb.dispose();
		
					ui.paintCellsImmediately(source.x, target.x, source.y, target.y);
					g.drawImage(buffer, shieldTLx, shieldTLy, null);
					partLines.get(i).clear();
					setPaint(g, i+1+windUpFramesNum+holdFramesNum, weaponX, weaponY, weaponDx, weaponDy);
					paintLines(partLines, g);
					pause(sleepTime);
				}
			} else {
				for(int i = 0; i < windUpFramesNum; ++i) {
					BufferedImage buffer = new BufferedImage(shieldWidth, shieldHeight, TYPE_INT_ARGB);
					Graphics2D gb = (Graphics2D) buffer.getGraphics();
					gb.drawImage(shieldBackGround, 0, 0, null);
					setPaint(gb, i+1+windUpFramesNum+holdFramesNum, weaponX3B, weaponY3B, weaponDx, weaponDy);
					bellowLines.get(i).clear();
					paintLines(bellowLines, gb);
					int shieldIdx = i+1+holdFramesNum;
					gb.drawImage(shieldArray[BELLOW][shieldIdx], 0, 0, null);
					paintLines(hitLines, gb); // hitting lines are in-between
					gb.drawImage(shipImg, shipX3B, shipY3B, null);
					gb.drawImage(shieldArray[ABOVE][shieldIdx], 0, 0, null);
					gb.dispose();

					ui.paintCellsImmediately(source.x, target.x, source.y, target.y);
					partLines.get(i).clear();
					setPaint(g, i+1+windUpFramesNum+holdFramesNum, weaponX, weaponY, weaponDx, weaponDy);
					paintLines(partLines, g); // visible line are bellow
					g.drawImage(buffer, shieldTLx, shieldTLy, null);
					pause(sleepTime);
				}
			}			
		} else {
			if (isAbove) {
				for(int i = 0; i < windUpFramesNum; ++i) {
					ui.paintCellsImmediately(source.x, target.x, source.y, target.y);
					if (ellipse)
						g.drawImage(shieldBackGround, shieldTLx, shieldTLx, null);
					else
						ui.paintImmediately(shieldRec);
					setPaint(g, i+1+windUpFramesNum+holdFramesNum, weaponX, weaponY, weaponDx, weaponDy);
					int shieldIdx = i+1+holdFramesNum;
					
					g.drawImage(shieldArray[BELLOW][shieldIdx], shieldTLx, shieldTLy, null);
					g.drawImage(shipImg, shipTLx, shipTLy, null);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, hiddenBeamAlpha));
					paintLines(hitLines, g); // hitting lines are in-between
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
					g.drawImage(shieldArray[ABOVE][shieldIdx], shieldTLx, shieldTLy, null);
		
					partLines.get(i).clear();
					paintLines(partLines, g);
					pause(sleepTime);
				}
			} else {
				for(int i = 0; i < windUpFramesNum; ++i) {
					ui.paintCellsImmediately(source.x, target.x, source.y, target.y);
					if (ellipse)
						g.drawImage(shieldBackGround, shieldTLx, shieldTLx, null);
					else
						ui.paintImmediately(shieldRec);
					setPaint(g, i+1+windUpFramesNum+holdFramesNum, weaponX, weaponY, weaponDx, weaponDy);
					partLines.get(i).clear();
					paintLines(partLines, g);
					int shieldIdx = i+1+holdFramesNum;
					g.drawImage(shieldArray[BELLOW][shieldIdx], shieldTLx, shieldTLy, null);
					paintLines(hitLines, g); // hitting lines are in-between
					g.drawImage(shipImg, shipTLx, shipTLy, null);
					g.drawImage(shieldArray[ABOVE][shieldIdx], shieldTLx, shieldTLy, null);
					pause(sleepTime);
				}			
			}
		}

		// Show shield fading
		if (tripleBuffer) {
			for(int i = 0; i < fadingFramesNum; ++i) {
				BufferedImage buffer = new BufferedImage(shieldWidth, shieldHeight, TYPE_INT_ARGB);
				Graphics2D gb = (Graphics2D) buffer.getGraphics();
				gb.drawImage(shieldBackGround, 0, 0, null);
				int shieldIdx = i+1+holdFramesNum+windUpFramesNum;
				
				gb.drawImage(shieldArray[BELLOW][shieldIdx], 0, 0, null);
				gb.drawImage(shipImg, shipX3B, shipY3B, null);
				gb.drawImage(shieldArray[ABOVE][shieldIdx], 0, 0, null);
				gb.dispose();

				g.drawImage(buffer, shieldTLx, shieldTLy, null);
				pause(sleepTime);
			}
		} else {
			for(int i = 0; i < fadingFramesNum; ++i) {
				if (ellipse)
					g.drawImage(shieldBackGround, shieldTLx, shieldTLx, null);
				else
					ui.paintImmediately(shieldRec);
				int shieldIdx = i+1+holdFramesNum+windUpFramesNum;
				
				g.drawImage(shieldArray[BELLOW][shieldIdx], shieldTLx, shieldTLy, null);
				g.drawImage(shipImg, shipTLx, shipTLy, null);
				g.drawImage(shieldArray[ABOVE][shieldIdx], shieldTLx, shieldTLy, null);
	
				pause(sleepTime);
			}
		}
		// System.out.println("ui.waitingToShowResult() = " + ui.waitingToShowResult());
		if (ui.waitingToShowResult()) {
			ui.paintAllImmediately();
			sleep(500); // no rush to show results
		}
        ui.animationCompleted();
        ui.paintAllImmediately();
    }
    private void drawNoShieldAttack(CombatStack source, CombatStack target,
    		int weaponX, int weaponY, int impaxtX, int impactY,
    		int wpnNum, float dmg, int count, int boxW, int boxH, float force) {
        ShipBattleUI ui = source.mgr.ui;
        if (!source.mgr.showAnimations()) {
        	ui.animationCompleted();
        	return;
        }

        ShipComponent wpn = source.weapon(wpnNum);
        int wpnCount = count / attacksPerRound / source.num;
        Graphics2D g = (Graphics2D) ui.getGraphics();
        Stroke prev = g.getStroke();
        
        int distFactor = 8*source.movePointsTo(target.x, target.y);

        g.setColor(beamColor);
        if(beamColor2 == null && cycleColor == null)
        	beamColor2 = multColor(beamColor, 0.75f);
        if(beamColor2 != null) {
            GradientPaint gp = new GradientPaint(weaponX,weaponY, beamColor,
            		weaponX+(impaxtX-weaponX)/distFactor,
            		weaponY+(impactY-weaponY)/distFactor, beamColor2, true);
            g.setPaint(gp);
        }

        if ((dashStroke > 0) && (weaponStroke == null)) {
            int w = scaled(3);
            float dash = scaled(w*dashStroke);
            weaponStroke = new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{0, dash}, 0);
        }

        if (weaponStroke != null)
            g.setStroke(weaponStroke);
        else if (wpn.heavy())
            g.setStroke(BasePanel.baseStroke(beamStroke*2+1));
        else
            g.setStroke(BasePanel.baseStroke(beamStroke*2));

        if (options().newWeaponSound())
        	 playAudioClip(newSoundEffect()); // BR:
        else if (attacksPerRound > 1)
            playAudioClip(soundEffectMulti());
        else
            playAudioClip(soundEffect());
       
        windUpFrames = source.mgr.autoComplete ? 1 : WIND_UP_FRAMES;
        holdFrames = source.mgr.autoComplete ? 0 : HOLD_FRAMES;        	

        int sourceSize = 3;
        if (source.design() != null)
        	sourceSize = source.design().size();
        // Full Beam trajectory generation
        ArrayList<Line2D.Double> lines = new ArrayList<>();
        for(int i = 0; i < attacksPerRound; ++i) {
            int xAdj = scaled(roll(-4,4)*2);
            int yAdj = scaled(roll(-4,4)*2);
            if (weaponSpread > 1) {
                int xMod = (source.y == target.y) ? 0 : 1;
                int yMod = (source.x == target.x) ? 0 : 1;
                if ((source.x < target.x) && (source.y < target.y))
                    xMod = -1;
                else if ((source.x > target.x) && (source.y > target.y))
                    xMod = -1;
                for (int n = -1 * weaponSpread; n <= weaponSpread; n++) {
                    if (!source.mgr.showAnimations()) 
                        break;
                    int adj = scaled(n);
                    lines.addAll(addMultiLines(sourceSize, wpnCount, weaponX, weaponY, impaxtX+xAdj+(xMod*adj), impactY+yAdj+(yMod*adj)));
                }
            } else {
                lines.addAll(addMultiLines(sourceSize, wpnCount, weaponX, weaponY, impaxtX+xAdj, impactY+yAdj));
            }
        }
        // Beams Progression generation
        SortedMap<Integer, ArrayList<Line2D.Double>> partLines = new TreeMap<>();
        long sleepTime = 50; // Original = 50;
        for(int i = 0; i < windUpFrames; ++i) {
            ArrayList<Line2D.Double> pl = new ArrayList<>();
            for(Line2D.Double line : lines) {
                double newX1 = line.getX1() + (line.getX2() - line.getX1()) * i / windUpFrames;
                double newY1 = line.getY1() + (line.getY2() - line.getY1()) * i / windUpFrames;
                double newX2 = line.getX1() + (line.getX2() - line.getX1()) * (i + 1) / windUpFrames;
                double newY2 = line.getY1() + (line.getY2() - line.getY1()) * (i + 1) / windUpFrames;
                pl.add(new Line2D.Double(newX1, newY1, newX2, newY2));
            }
            partLines.put(i, pl);
            paintLines(pl, g);
            sleep(sleepTime);
            //ui.paintAllImmediately();
        }
        // Show Continuous
        for(int i = 0; i < holdFrames; i++) {
            if(beamColor2 != null) {
                GradientPaint gp = new GradientPaint(weaponX+i*scaled(10),weaponY+i*scaled(10), beamColor,
                		weaponX+i*scaled(10)+(impaxtX-weaponX)/distFactor,weaponY+i*scaled(10)+(impactY-weaponY)/distFactor, beamColor2, true);
                g.setPaint(gp);
            }
            if(cycleColor != null) {
                if(i%2 == 0) {
                    g.setColor(cycleColor);
                } else if (cycleColor2 != null && i%3 == 0) {
                    g.setColor(cycleColor2);
                } else {
                    g.setColor(beamColor);
                }
            }
            paintLines(partLines, g);
            sleep(sleepTime);
        }
        // Show end of beam
        for(int i = 0; i < windUpFrames; ++i) {
            ui.paintCellsImmediately(source.x, target.x, source.y, target.y);
            partLines.get(i).clear();
            paintLines(partLines, g);
            sleep(sleepTime);
        }
        ui.paintAllImmediately();

        String missLabel = dmg < 0 ? text("SHIP_COMBAT_DEFLECTED") : text("SHIP_COMBAT_MISS");
        target.drawAttackResult(g,impaxtX,impactY,weaponX, dmg,missLabel);   
        g.setStroke(prev);
        ui.animationCompleted();
        ui.paintAllImmediately();
    }
    private void drawOldAttack(CombatStack source, CombatStack target,
    		int weaponX, int weaponY, int impactX, int impactY,
    		int wpnNum, float dmg, int count, int boxW, int boxH, float force) {
        ShipBattleUI ui = source.mgr.ui;
        if (!source.mgr.showAnimations()) {
        	ui.animationCompleted();
        	return;
        }

        // BR: Add Shield effect
        boolean showShield = (dmg != 0) && (target.shieldLevel()>0);
        
        Dimension shieldSize = showShield? target.shieldSize(boxW, boxH) : new Dimension(boxW, boxH);
        int xS = impactX - shieldSize.width/2;
        int yS = impactY - shieldSize.height/2;
       
        ShipComponent wpn = source.weapon(wpnNum);
        int wpnCount = count / attacksPerRound / source.num;
        Graphics2D g = (Graphics2D) ui.getGraphics();
        Stroke prev = g.getStroke();
        
        int distFactor = 8*source.movePointsTo(target.x, target.y);

        g.setColor(beamColor);
        if(beamColor2 == null && cycleColor == null)
        	beamColor2 = multColor(beamColor, 0.75f);
        if(beamColor2 != null) {
            GradientPaint gp = new GradientPaint(weaponX,weaponY, beamColor,
            		weaponX+(impactX-weaponX)/distFactor,
            		weaponY+(impactY-weaponY)/distFactor, beamColor2, true);
            g.setPaint(gp);
        }

        if ((dashStroke > 0) && (weaponStroke == null)) {
            int w = scaled(3);
            float dash = scaled(w*dashStroke);
            weaponStroke = new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL, 0, new float[]{0, dash}, 0);
        }

        if (weaponStroke != null)
            g.setStroke(weaponStroke);
        else if (wpn.heavy())
            g.setStroke(BasePanel.baseStroke(beamStroke*2+1));
        else
            g.setStroke(BasePanel.baseStroke(beamStroke*2));

        if (options().newWeaponSound())
        	 playAudioClip(newSoundEffect()); // BR:
        else if (attacksPerRound > 1)
            playAudioClip(soundEffectMulti());
        else
            playAudioClip(soundEffect());
       
        if (showShield) {
            windUpFrames = WIND_UP_FRAMES;
            holdFrames = HOLD_FRAMES;        	
        } else {
            windUpFrames = source.mgr.autoComplete ? 1 : WIND_UP_FRAMES;
            holdFrames = source.mgr.autoComplete ? 0 : HOLD_FRAMES;        	
        }
        BufferedImage[][] shieldImg = new BufferedImage[attacksPerRound][windUpFrames];

        int sourceSize = 3;
        if (source.design() != null)
        	sourceSize = source.design().size();
        // Full Beam trajectory generation
        ArrayList<Line2D.Double> lines = new ArrayList<>();
        for(int i = 0; i < attacksPerRound; ++i) {
            int xAdj = scaled(roll(-4,4)*2);
            int yAdj = scaled(roll(-4,4)*2);
            if (showShield) {
            	shieldImg[i] = target.shieldImg(windUpFrames, attacksPerRound,
            			shieldSize, weaponX, weaponY, impactX, impactY, xAdj, yAdj, beamColor, force, dmg);
            }
            if (weaponSpread > 1) {
                int xMod = (source.y == target.y) ? 0 : 1;
                int yMod = (source.x == target.x) ? 0 : 1;
                if ((source.x < target.x) && (source.y < target.y))
                    xMod = -1;
                else if ((source.x > target.x) && (source.y > target.y))
                    xMod = -1;
                for (int n = -1 * weaponSpread; n <= weaponSpread; n++) {
                    if (!source.mgr.showAnimations()) 
                        break;
                    int adj = scaled(n);
                    lines.addAll(addMultiLines(sourceSize, wpnCount, weaponX, weaponY, impactX+xAdj+(xMod*adj), impactY+yAdj+(yMod*adj)));
                }
            } else {
                lines.addAll(addMultiLines(sourceSize, wpnCount, weaponX, weaponY, impactX+xAdj, impactY+yAdj));
            }
        }
        // Beams Progression generation
        SortedMap<Integer, ArrayList<Line2D.Double>> partLines = new TreeMap<>();
        long sleepTime = 50; // Original = 50;
        for(int i = 0; i < windUpFrames; ++i) {
            ArrayList<Line2D.Double> pl = new ArrayList<>();
            for(Line2D.Double line : lines) {
                double newX1 = line.getX1() + (line.getX2() - line.getX1()) * i / windUpFrames;
                double newY1 = line.getY1() + (line.getY2() - line.getY1()) * i / windUpFrames;
                double newX2 = line.getX1() + (line.getX2() - line.getX1()) * (i + 1) / windUpFrames;
                double newY2 = line.getY1() + (line.getY2() - line.getY1()) * (i + 1) / windUpFrames;
                pl.add(new Line2D.Double(newX1, newY1, newX2, newY2));
            }
            partLines.put(i, pl);
            paintLines(pl, g);
            sleep(sleepTime);
            //ui.paintAllImmediately();
        }
        // Show Continuous
        for(int i = 0; i < holdFrames; i++) {
            if(beamColor2 != null) {
                GradientPaint gp = new GradientPaint(weaponX+i*scaled(10),weaponY+i*scaled(10), beamColor,
                		weaponX+i*scaled(10)+(impactX-weaponX)/distFactor,weaponY+i*scaled(10)+(impactY-weaponY)/distFactor, beamColor2, true);
                g.setPaint(gp);
            }
            if(cycleColor != null) {
                if(i%2 == 0) {
                    g.setColor(cycleColor);
                } else if (cycleColor2 != null && i%3 == 0) {
                    g.setColor(cycleColor2);
                } else {
                    g.setColor(beamColor);
                }
            }
            paintLines(partLines, g);
            sleep(sleepTime);
        }
        // Show end of beam
        for(int i = 0; i < windUpFrames; ++i) {
            ui.paintCellsImmediately(source.x, target.x, source.y, target.y);
            partLines.get(i).clear();
            paintLines(partLines, g);
            if (showShield) {
    	    	//g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    	    	g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
            	for(int k = 0; k < attacksPerRound; k++) {
            		g.drawImage(shieldImg[k][i], xS, yS, null);
            	}
    	    	g.setComposite(AlphaComposite.Src);
            }
            sleep(sleepTime);
        }
        
        ui.paintAllImmediately();

        String missLabel = dmg < 0 ? text("SHIP_COMBAT_DEFLECTED") : text("SHIP_COMBAT_MISS");
        target.drawAttackResult(g,impactX,impactY,weaponX, dmg,missLabel);   
        g.setStroke(prev);
        ui.animationCompleted();
        ui.paintAllImmediately();
    }
    private ArrayList<Line2D.Double> addMultiLines(int size, int weapons, int sx, int sy, int tx, int ty) {
        int offSet = scaled((size + 1) * 5);
        ArrayList<Line2D.Double> lines = new ArrayList<>();
        if(weapons < 3)
            lines.add(new Line2D.Double(sx, sy, tx, ty));
        else if(weapons < 6) {
            lines.add(new Line2D.Double(sx, sy + offSet, tx, ty));
            lines.add(new Line2D.Double(sx, sy - offSet, tx, ty));
        } else {
            lines.add(new Line2D.Double(sx, sy, tx, ty));
            lines.add(new Line2D.Double(sx, sy + offSet, tx, ty));
            lines.add(new Line2D.Double(sx, sy - offSet, tx, ty));
        }
        return lines;
    }
    private void paintLines(SortedMap<Integer, ArrayList<Line2D.Double>> lines, Graphics2D g) {
        for(ArrayList<Line2D.Double> lineList : lines.values()) {
            for(Line2D.Double line : lineList) {
                g.draw(line);
            }
        }
    }
    private void paintLines(ArrayList<Line2D.Double> lines, Graphics2D g) {
        for(Line2D.Double line : lines) {
            g.draw(line);
        }
    }
    private void drawStack(ShipBattleUI ui, Graphics2D g, CombatStack st, int x, int y, int w, int h) {
        if (st.isShip()) {
            CombatStackShip sh = (CombatStackShip) st;
            sh.drawStack(null, g, 1, x, y, w, h, 1);
        }
        else if (st.isMissile()) {
            CombatStackMissile sh = (CombatStackMissile) st;
            sh.drawStack(null, g, 0, x, y, w, h, 1);
        }
        else if (st.isMonster()) {
            st.drawStack(null, g, 0, x, y, w, h, 1);
        }
        else if (st.isColony()) {
        	int planetR = h*5/6;
        	int planetX = x+((w-planetR)/2);
        	int planetY = y+(h/12);
            CombatStackColony sh = (CombatStackColony) st;
            ui.drawPlanetOnly(g, sh, planetX, planetY, w, h);
        }
    }
	private void setPaint(Graphics2D g, int i, int weaponX, int weaponY, int weaponDx, int weaponDy) {
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
		if(beamColor2 != null) {
			GradientPaint gp = new GradientPaint(weaponX+i*s10, weaponY+i*s10, beamColor,
					weaponX+i*s10+weaponDx, weaponY+i*s10+weaponDy, beamColor2, true);
			g.setPaint(gp);
		} else
			g.setColor(beamColor);
			
		if(cycleColor != null) {
			if(i%2 == 0) {
				g.setColor(cycleColor);
			} else if (cycleColor2 != null && i%3 == 0) {
				g.setColor(cycleColor2);
			} else {
				g.setColor(beamColor);
			}
		}
	}
}
