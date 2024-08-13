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
package rotp.model.ships;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rotp.model.empires.Empire;
import rotp.model.empires.ShipView;
import rotp.model.tech.Tech;
import rotp.model.tech.TechEngineWarp;
import rotp.util.Base;
import rotp.util.ImageColorizer;

public class ShipDesignLab implements Base, Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MAX_DESIGNS = 6;

    private Empire empire;
    private final ShipDesign[] designs = new ShipDesign[ShipDesignLab.MAX_DESIGNS];
    private final DesignStargate stargateDesign = new DesignStargate();

    private final List<ShipView> designHistory = new ArrayList<>();
    private int shipStyleIndex;

    private final List<ShipComputer> computer = new ArrayList<>();
    private final List<ShipShield> shield = new ArrayList<>();
    private final List<ShipECM> ecm = new ArrayList<>();
    private final List<ShipArmor> armor = new ArrayList<>();
    private final List<ShipEngine> engine = new ArrayList<>();
    private final List<ShipManeuver> maneuver = new ArrayList<>();
    private final List<ShipWeapon> weapon = new ArrayList<>();
    private final List<ShipSpecial> special = new ArrayList<>();

    private int scoutDesignId, bomberDesignId, fighterDesignId, colonyDesignId, destroyerDesignId;

    private float bestEnemyShieldLevel = 0;
    private float bestEnemyPlanetaryShieldLevel = 0;
    public boolean needColonyShips = false;
    public boolean needExtendedColonyShips = false;
    public boolean needScouts = true;

    private ShipDesign prototypeDesign;
    private transient List<ShipDesign> outdatedDesigns;

    public ShipDesign[] designs()                 { return designs; }
    public ShipDesign design(int i)               { return designs[i]; }
    public DesignStargate stargateDesign()        { return stargateDesign; }
    public Empire empire()                        { return empire; }
    public List<ShipView> designHistory()         { return designHistory; }
    public List<ShipComputer> computers()         { return computer; }
    public List<ShipShield> shields()             { return shield; }
    public List<ShipECM> ecms()                   { return ecm; }
    public List<ShipArmor> armors()               { return armor; }
    public List<ShipEngine> engines()             { return engine; }
    public List<ShipManeuver> maneuvers()         { return maneuver; }
    public List<ShipWeapon> weapons()             { return weapon; }
    public List<ShipSpecial> specials()           { return special; }
    public ShipDesign scoutDesign()               { return designs[scoutDesignId]; }
    public ShipDesign bomberDesign()              { return designs[bomberDesignId]; }
    public ShipDesign fighterDesign()             { return designs[fighterDesignId]; }
    public ShipDesign colonyDesign()              { return designs[colonyDesignId]; }
    public ShipDesign destroyerDesign()           { return designs[destroyerDesignId]; }
    public float bestEnemyShieldLevel()           { return bestEnemyShieldLevel; }
    public float bestEnemyPlanetaryShieldLevel()  { return bestEnemyPlanetaryShieldLevel; }
    public int shipStyleIndex()                   { return shipStyleIndex; }

    public ShipWeapon noWeapon()                  { return weapons().get(0); }
    public ShipSpecial noSpecial()                { return specials().get(0); }
    
    public ShipDesign prototypeDesign() {
        if (prototypeDesign == null)
            prototypeDesign = newBlankDesign(ShipDesign.SMALL);
        return prototypeDesign;
    }  
    public boolean slotInUse(int slot) {
        return (slot == scoutDesignId) || (slot == bomberDesignId) || (slot == fighterDesignId)
                || (slot == colonyDesignId) || (slot == destroyerDesignId);
    }

    public List<ShipDesign> outdatedDesigns() {
        if (outdatedDesigns == null)
            outdatedDesigns = new ArrayList<>();
        return outdatedDesigns;
    }
    public void init(Empire c)  {
        empire = c;
        // BR: Add Ship Set for custom races
        if (empire.isPlayer())
        	shipStyleIndex = options().selectedPlayerShipSetId();
//        else if (ShipLibrary.current().styles.contains(empire.race().preferredShipSet))
//            shipStyleIndex = ShipLibrary.current().styles.indexOf(empire.race().preferredShipSet);
        else if (ShipLibrary.current().styles.contains(empire.preferredShipSet()))
            shipStyleIndex = ShipLibrary.current().styles.indexOf(empire.preferredShipSet());
        else
            shipStyleIndex = ShipLibrary.current().selectRandomUnchosenSet();

        // add default ship options that are NOT provided by starting techs
        // shield, armor, engine added by starting techs
        addWeapon(new ShipWeapon());
        addSpecial(new ShipSpecial());
        
        loadInitialDesigns();
        
        // modnar: add battleScout option to give player super Scout design
        if ( c.isPlayerControlled() && options().selectedBattleScout() ) { 
            ShipDesign design;
            design = battleScoutDesign();
            setFighterDesign(design, 5);
        }
    }
    public boolean isCurrentLabDesign(int id) {
        return (id == scoutDesignId) || (id == fighterDesignId) || (id == destroyerDesignId)
                || (id == bomberDesignId) || (id == colonyDesignId);
    }
    public int numDesigns() {
        int i = 0;
        for (ShipDesign d : designs) {
            if (d.active())
                i++;
        }
        return i;
    }
    public boolean canScrapADesign() {
        int numActive = 0;
        for (ShipDesign d : designs) {
            if (d.active())
                numActive++;
        }
        return numActive > 1;
    }
    private void loadInitialDesigns() {
        for (int i=0;i<designs.length;i++) {
            if (designs[i] == null) {
                designs[i] = newBlankDesign(ShipDesign.SMALL);
                designs[i].id(i);
            }
        }
        scoutDesignId = 0;
        fighterDesignId = 1;
        bomberDesignId = 2;
        destroyerDesignId = 3;
        colonyDesignId = 4;
        
        if (empire.id < 0) // BR: Monster Empire
        	return;
        
        ShipDesign design;
        rotp.model.ai.xilmi.NewShipTemplate nst = new rotp.model.ai.xilmi.NewShipTemplate();

        design = startingScoutDesign();
        setScoutDesign(design, 0);

        design = nst.autoDestroyerDesign(empire.shipDesignerAI(), 0);
        if (empire.isPlayer())
            design.name(text("SHIP_DESIGN_1ST_FIGHTER_NAME"));
        if(design.isArmed())
            setFighterDesign(design, 1);

        design = nst.autoBomberDesign(empire.shipDesignerAI(), 1);
        if (empire.isPlayer())
            design.name(text("SHIP_DESIGN_1ST_BOMBER_NAME"));
        setBomberDesign(design, 2);

        design = nst.autoDestroyerDesign(empire.shipDesignerAI(), 1);
        if (empire.isPlayer())
            design.name(text("SHIP_DESIGN_1ST_DESTROYER_NAME"));
        setDestroyerDesign(design, 3);

        design = startingColonyDesign();
        setColonyDesign(design, 4);
    }
    public void nextTurn() {
        for (int i=0;i<designs.length;i++) {
            if (designs[i].id() != i)
                System.err.println("Empire: "+empire.name()+"  design slot "+i+" has id:"+designs[i].id());
        }
        // update opp shield level (for fighter designs)
        bestEnemyShieldLevel = empire.bestEnemyShieldLevel();
        bestEnemyPlanetaryShieldLevel = empire.bestEnemyPlanetaryShieldLevel();
        for (ShipDesign d : designs) {
            d.recalculateCost();
        }
    }
    public void recordConstruction(Design d, int num) {
        ShipView sv = shipViewFor((ShipDesign)d);
        sv.design().addTotalBuilt(num);
        sv.setViewDate();
    }
    public ShipView shipViewFor(ShipDesign d) {
        // get shipView from design history. If none, add one.
        for (ShipView sv1 : designHistory) {
            if (sv1.matches(d))
                return sv1;
        }

        ShipView sv = new ShipView(empire, d);
        sv.scan();
        designHistory.add(sv);
        return sv;
    }
    public void recordScrap(Design d, int num)       { d.addTotalScrapped(num); }
    public void recordDestruction(Design d, int num) { d.addTotalDestroyed(num); }
    public void recordUse(Design d, int num)         { d.addTotalUsed(num); }
    public void recordKills(Design d, int num) {
        ShipDesign sd = (ShipDesign) d;
        ShipView sv = shipViewFor(sd);
        sv.addTotalKills(num);
    }
    public int availableDesignSlot() {
        for (int i=0;i<designs.length;i++) {
            if (!designs[i].active())
                return i;
        }
        return -1;
    }
    public void setScoutDesign(ShipDesign d, int slot) {
        d.mission(ShipDesign.SCOUT);
        empire().swapShipConstruction(scoutDesign(), d);
        designs[slot] = d;
        d.active(true);
        d.id(slot);
        d.seq(slot); 
        d.shipColor(empire().defaultShipTint());
        scoutDesignId = slot;
        log("Empire: "+empire.name()+" creates scout design: "+d.name()+"  slot:"+slot);
    }
    public void setColonyDesign(ShipDesign d, int slot) {
        d.mission(ShipDesign.COLONY);
        empire().swapShipConstruction(colonyDesign(), d);
        designs[slot] = d;
        d.active(true);
        d.id(slot);
        d.seq(slot); 
        d.shipColor(empire().defaultShipTint());
        colonyDesignId = slot;
        log("Empire: "+empire.name()+" creates colony design: "+d.name()+"  slot:"+slot);
    }
    public void setFighterDesign(ShipDesign d, int slot) {
        d.mission(ShipDesign.FIGHTER);
        designs[slot] = d;
        d.active(true);
        d.id(slot);
        d.seq(slot); 
        d.shipColor(empire().defaultShipTint());
        empire().swapShipConstruction(fighterDesign(), d);
        fighterDesignId = slot;
        log("Empire: "+empire.name()+" creates fighter design: "+d.name()+"  slot:"+slot);
    }
    public void setBomberDesign(ShipDesign d, int slot) {
        d.mission(ShipDesign.BOMBER);
        empire().swapShipConstruction(bomberDesign(), d);
        designs[slot] = d;
        d.active(true);
        d.id(slot);
        d.seq(slot); 
        d.shipColor(empire().defaultShipTint());
        bomberDesignId = slot;
        log("Empire: "+empire.name()+" creates bomber design: "+d.name()+"  slot:"+slot);
    }
    public void setDestroyerDesign(ShipDesign d, int slot) {
        d.mission(ShipDesign.DESTROYER);
        empire().swapShipConstruction(destroyerDesign(), d);
        designs[slot] = d;
        d.active(true);
        d.id(slot);
        d.seq(slot); 
        d.shipColor(empire().defaultShipTint());
        destroyerDesignId = slot;
        log("Empire: "+empire.name()+" creates destroyer design: "+d.name()+"  slot:"+slot);
    }
    public ShipDesign startingScoutDesign() {
        ShipDesign design = newBlankDesign(ShipDesign.SMALL);
        design.special(0, specialReserveFuel());
        design.mission(ShipDesign.SCOUT);
        if (empire.isAI())
            nameDesign(design);
        else
            design.name(text("SHIP_DESIGN_1ST_SCOUT_NAME"));
        iconifyDesign(design);
        return design;
    }
    public ShipDesign startingFighterDesign() {
        ShipDesign design = newBlankDesign(ShipDesign.SMALL);
        design.engine(engines().get(0));
        design.addWeapon(beamWeapon(0, false), 1);
        design.mission(ShipDesign.FIGHTER);
        if (empire.isAI())
            nameDesign(design);
        else
            design.name(text("SHIP_DESIGN_1ST_FIGHTER_NAME"));
        iconifyDesign(design);
        return design;
    }
    public ShipDesign startingBomberDesign() {
        ShipDesign design = newBlankDesign(ShipDesign.MEDIUM);
        design.computer(computers().get(1));
        design.addWeapon(bombWeapon(0), 3);
        design.mission(ShipDesign.BOMBER);
        if (empire.isAI())
            nameDesign(design);
        else
            design.name(text("SHIP_DESIGN_1ST_BOMBER_NAME"));
        iconifyDesign(design);
        return design;
    }
    public ShipDesign startingDestroyerDesign() {
        ShipDesign design = newBlankDesign(ShipDesign.MEDIUM);
        design.mission(ShipDesign.DESTROYER);
        design.addWeapon(beamWeapon(0, false), 4);
        design.computer(computers().get(1));
        design.shield(shields().get(1));
        if (empire.isAI())
            nameDesign(design);
        else
            design.name(text("SHIP_DESIGN_1ST_DESTROYER_NAME"));
        iconifyDesign(design);
        return design;
    }
    public ShipDesign startingColonyDesign() {
        ShipDesign design = newBlankDesign(ShipDesign.LARGE);
        design.mission(ShipDesign.COLONY);
        design.special(0, empire.shipDesignerAI().bestColonySpecial());
        if (empire.isAI())
            nameDesign(design);
        else
            design.name(text("SHIP_DESIGN_1ST_COLONY_NAME"));
        iconifyDesign(design);
        return design;
    }
    // modnar: add battleScout option to give player super Scout design
    public ShipDesign battleScoutDesign() {
        ShipDesign design = newBlankDesign(ShipDesign.MEDIUM);
        design.mission(ShipDesign.FIGHTER);
        design.computer(computers().get(1));
        design.shield(shields().get(1));
        design.addWeapon(beamWeapon(0, true), 1); // HEAVY LASER
        ShipSpecial spBattleScanner = specialBattleScanner();
        design.special(0, spBattleScanner);
        design.special(1, specialReserveFuel());
        design.name(text("Battle Scout"));
        iconifyDesign(design);
        return design;
    }
    public void nameDesign(ShipDesign d) {
        List<String> shipNames = empire.race().shipNames(d.size());
        if ((shipNames == null) || shipNames.isEmpty()) {
            d.name(concat("Ship", str(roll(100,999))));
            return;
        }
        List<String> remainingNames = new ArrayList<>();
        for (String name : shipNames) {
            if (designNamed(name) == null)
                remainingNames.add(name);
        }
        if (remainingNames.isEmpty()) {
            d.name(concat("Ship", str(roll(100,999))));
            return;
        }
        int index = min(0,remainingNames.size()-1);
        d.name(remainingNames.get(index));
    }
    public void iconifyDesign(ShipDesign newDesign) {
        // get all valid icons for this size and remove ones already being used
        List<String> validIconKeys = ShipLibrary.current().validIconKeys(shipStyleIndex, newDesign.size());
        for (ShipDesign d : designs()) {
            if (d.active() && (d.size() == newDesign.size()))
                validIconKeys.remove(d.iconKey());
        }

        if (validIconKeys.isEmpty()) {
            int newNum = roll(0,ShipLibrary.designsPerSize-1);
            newDesign.iconKey(ShipLibrary.current().shipKey(shipStyleIndex, newDesign.size(), newNum));
        }
        else
            newDesign.iconKey(random(validIconKeys));
    }
    public Design prevDesignFrom(Design d, boolean stargateBuilt) {
        int index;
        if ((d == null) || (d == stargateDesign))
            index = MAX_DESIGNS;
        else
            index = d.id();

        while (true) {
            if (index == 0) {
                if (empire.tech().canBuildStargate() && !stargateBuilt)
                    return stargateDesign;
                else
                    index = MAX_DESIGNS;
            }
            index--;
            ShipDesign design = design(index);
            if (design.active())
                return design;
        }
    }
    public Design nextDesignFrom(Design d, boolean stargateBuilt) {
        int index;
        if ((d == null) || (d == stargateDesign))
            index = -1;
        else
            index = d.id();

        while (true) {
            if (index == (MAX_DESIGNS-1)) {
                if (empire.tech().canBuildStargate() && !stargateBuilt)
                    return stargateDesign;
               else
                    index = -1;
            }
            index++;
            ShipDesign design = design(index);
            if (design.active())
                return design;
        }
    }
    public ShipDesign newBlankDesign(int size) {
        ShipDesign design = new ShipDesign(size);
        design.active(false);
        design.lab(this);
        design.computer(computers().get(0));
        design.shield(shields().get(0));
        design.ecm(ecms().get(0));
        design.armor(armors().get(0));
        design.engine(engines().get(0));
        design.maneuver(maneuvers().get(0));
        for (int i=0;i<ShipDesign.maxWeapons();i++)
            design.weapon(i, weapons().get(0));
        for (int i=0;i<ShipDesign.maxSpecials();i++)
            design.special(i, specials().get(0));
        return design;
    }
    public void clearDesign(ShipDesign design, boolean onlyWeapons) {
        for (int i=0;i<ShipDesign.maxWeapons();i++) {
            design.weapon(i, weapons().get(0));
            design.wpnCount(i, 0);
        }
        if(onlyWeapons)
            return;
        design.computer(computers().get(0));
        design.shield(shields().get(0));
        design.ecm(ecms().get(0));
        design.armor(armors().get(0));
        design.engine(engines().get(0));
        design.maneuver(maneuvers().get(0));
        design.shipColor(0);
        for (int i=0;i<ShipDesign.maxSpecials();i++)
            design.special(i, specials().get(0));
    }
    public void scrapDesign(ShipDesign d) {
        int designId = d.id();

        // remove from existing fleets
//        int scrappedCount = galaxy().ships.scrapDesign(empire.id, designId);
        int[] counts = galaxy().ships.scrapDesign(empire.id, designId);
        log("Empire: "+empire.name()+"  Scrapping design: ",
        		d.name(), "  id: "+d.id()+"  count:", str(counts[0]));

        d.scrapped(true);
        d.active(false);
//        d.addTotalScrapped(scrappedCount);
        d.addTotalScrapped(counts[0]);

        // reimburse civ reserve for 1/2 of ship's cost (halved when added to reserve)
        switch (options().selectedScrapRefundOption()) {
	        case "Never":
	        	break;
	        case "Ally":
	        	empire().addReserve(d.scrapValue(counts[1]+counts[2])*2);
	        	break;
	        case "Empire":
	        	empire().addReserve(d.scrapValue(counts[1])*2);
	        	break;
	        case "All":
	        default:
	        	empire().addReserve(d.scrapValue(counts[0])*2);
        }
//        empire().addReserve(d.scrapValue(scrappedCount)*2);
        empire().swapShipConstruction(d);
        
        // remove scrapped design from list of designs and replace with new, inactive design
        designs[designId] = newBlankDesign(ShipDesign.SMALL);
        designs[designId].id(designId);
        designs[designId].copyFrom(d);
    }
    public String nextAvailableIconKey(int size, String currIconKey) {
        int newNum;
        String iconKey;

        // get all valid icons for this size and remove ones already being used
        // null-check necessary for design because this can be called up on init when not all designs are present
        List<String> validIconKeys = ShipLibrary.current().validIconKeys(shipStyleIndex, size);
        for (ShipDesign d : designs()) {
            if (d.active() && (d.size() == size))
                validIconKeys.remove(d.iconKey());
        }

        if (validIconKeys.isEmpty()) {
            newNum = roll(0,ShipLibrary.designsPerSize-1);
            iconKey = ShipLibrary.current().shipKey(shipStyleIndex, size, newNum);
        }
        else {
            newNum = validIconKeys.indexOf(currIconKey);
            if ((newNum + 1) >= validIconKeys.size())
                newNum = 0;
            else
                newNum++;
            iconKey = validIconKeys.get(newNum);
        }
        return iconKey;
    }
    public void addComputer(ShipComputer c) {
        computers().add(c);
        Collections.sort(computers(),ShipComponent.SELECTION_ORDER);
    }
    public void addShield(ShipShield c) {
        shields().add(c);
        Collections.sort(shields(),ShipComponent.SELECTION_ORDER);
    }
    public void addECM(ShipECM c) {
        ecms().add(c);
        Collections.sort(ecms(),ShipComponent.SELECTION_ORDER);
    }
    public void addArmor(ShipArmor c) {
        armors().add(c);
        Collections.sort(armors(),ShipComponent.SELECTION_ORDER);
}
    public void addEngine(ShipEngine c) {
        engines().add(c);
        Collections.sort(engines(),ShipComponent.SELECTION_ORDER);
    }
    public void addManeuver(ShipManeuver c) {
        maneuvers().add(c);
        Collections.sort(maneuvers(),ShipComponent.SELECTION_ORDER);
    }
    public boolean hasManeuverForTech(TechEngineWarp tech) {
        for (ShipManeuver manv: maneuvers()) {
            if (manv.tech() == tech)
                return true;
        }
        return false;
    }
    public List<ShipManeuver> availableManeuversForDesign(ShipDesign d) {
        List<ShipManeuver> manvList = new ArrayList<>();
        int engWarpLevel = d.engine().baseWarp();
        for (ShipManeuver manv: maneuver) {
            if (manv.level() <= engWarpLevel)
                manvList.add(manv);
        }
        return manvList;
    }
    public void addWeapon(ShipWeapon c) {
    	weapons().add(c);
        Collections.sort(weapons(),ShipComponent.SELECTION_ORDER);
    }
    public void addSpecial(ShipSpecial c) {
    	specials().add(c);
        Collections.sort(specials(),ShipComponent.SELECTION_ORDER);
    }
    public ShipWeapon beamWeapon(int seq, boolean heavy) {
        for (ShipWeapon wpn : weapons()) {
            if (wpn.isBeamWeapon()) {
                ShipWeaponBeam wpn2 = (ShipWeaponBeam) wpn;
                System.out.println("wpn2.tech().sequence = " + wpn2.tech().sequence);
                if ((wpn2.tech().sequence == seq) && (wpn2.heavy() == heavy))
                    return wpn;
            }
        }
        return null;
    }
    public ShipWeapon missileWeapon(int seq, int shots) {
        for (ShipWeapon wpn : weapons()) {
            if (wpn.isMissileWeapon()) {
                ShipWeaponMissile wpn2 = (ShipWeaponMissile) wpn;
                if ((wpn2.tech().sequence == seq) && (wpn2.shots() == shots))
                    return wpn;
            }
        }
        return null;
    }
    public ShipWeapon bombWeapon(int seq) {
        for (ShipWeapon wpn : weapons()) {
            if (wpn.groundAttacksOnly()) {
                if (((ShipWeaponBomb) wpn).tech().sequence == seq)
                    return wpn;
            }
        }
        return null;
    }
    public ShipSpecial specialNamed(String s) {
        for (ShipSpecial spec : specials()) {
            if (spec.name().equals(s))
                return spec;
        }
        return null;
    }
    public ShipSpecial getSpecial(String s, int seq) {
        for (ShipSpecial special : specials()) {
            if (special.name().equals(s) && special.sequence()==seq)
                return special;
        }
        return null;
    }
    public ShipSpecial specialSquidInk()        { return getSpecial("Squid Ink", 0); }
    public ShipSpecial specialAmoebaEatShips()  { return getSpecial("Eat Ships", 0); }
    public ShipSpecial specialAutomatedRepair() { return getSpecial("AutomatedRepair", 0); }
    public ShipSpecial specialAdvDamControl()   { return getSpecial("AutomatedRepair", 1); }

    public ShipSpecial specialReserveFuel()     {
        for (ShipSpecial spec : specials()) {
            if (spec.isFuelRange())
                return spec;
        }
        return null;
    }
    public ShipSpecial specialBattleScanner()   {
        for (ShipSpecial spec : specials()) {
            if (spec.allowsScanning())
                return spec;
        }
        return null;
    }
    public ShipSpecial specialTeleporter() {
        for (ShipSpecial spec : specials()) {
            if (spec.allowsTeleporting())
                return spec;
        }
        return null;
    }
    public ShipSpecial specialCloak() {
        for (ShipSpecial spec : specials()) {
            if (spec.allowsCloaking())
                return spec;
        }
        return null;
    }
    public ShipDesign designNamed(String s) {
        for (ShipDesign des : designs()) {
            if (des.active() && des.name().equals(s))
                return des;
        }
        return null;
    }
    public ShipEngine fastestEngine() {
        ShipEngine fastestEngine = engines().get(0);
        for (ShipEngine eng : engines()) {
            if (eng.warp() > fastestEngine.warp())
                fastestEngine = eng;
        }
        return fastestEngine;
    }
    public ShipArmor bestArmor() {
        ShipArmor bestArmor = armors().get(0);
        for (ShipArmor arm : armors()) {
            if (!arm.reinforced() && (arm.sequence() > bestArmor.sequence()))
                bestArmor = arm;
        }
        return bestArmor;
    }
    public ShipComputer nextBestComputer(ShipDesign d, float spacePct) {
        float space = d.availableSpace() * spacePct;
        for (ShipComputer comp: computers()) {
            if ((comp.level() > d.computer().level()) && (comp.space(d) < space))
                return comp;
        }
        return null;
    }
    public ShipManeuver nextBestManeuver(ShipDesign d, float spacePct) {
        float space = d.availableSpace() * spacePct;
        for (ShipManeuver manv: maneuvers()) {
            if ((manv.level() > d.maneuver().level()) && (manv.space(d) < space))
                return manv;
        }
        return null;
    }
    public ShipShield nextBestShield(ShipDesign d, float spacePct) {
        float space = d.availableSpace() * spacePct;
        for (ShipShield shld: shields()) {
            if ((shld.level() > d.shield().level()) && (shld.space(d) < space))
                return shld;
        }
        return null;
    }
    public ShipECM nextBestECM(ShipDesign d, float spacePct) {
        float space = d.availableSpace() * spacePct;
        for (ShipECM ecm: ecms()) {
            if ((ecm.level() > d.ecm().level()) && (ecm.space(d) < space))
                return ecm;
        }
        return null;
    }
    public ShipWeapon bestWeapon(ShipDesign d, float spacePct) {
        float shieldLevel = bestEnemyShieldLevel;
        ShipWeapon bestWpn = null;
        float bestDamage = -1;
        int numWeapons;
        float wpnDamage, adjDamage;
        float space = d.availableSpace() * spacePct;

        for (ShipWeapon wpn : weapons()) {
            if (!wpn.noWeapon() && !wpn.groundAttacksOnly()) {
                numWeapons = (int) (space/wpn.space(d));
                wpnDamage = numWeapons * wpn.firepower(shieldLevel);
                if (wpn.isLimitedShotWeapon()) {
                    float base = pow(2,wpn.shots());
                    adjDamage = (base -1)/base * wpnDamage;
                }
                else
                        adjDamage = wpnDamage * sqrt(wpn.range());
                if (adjDamage > bestDamage) {
                    bestWpn = wpn;
                    bestDamage = adjDamage;
                }
            }
        }
        return bestWpn;
    }
    public ShipWeapon bestPlanetWeapon(ShipDesign d, float spacePct) {
        float shieldLevel = bestEnemyPlanetaryShieldLevel;
        ShipWeapon bestWpn = null;
        float bestDamage = -1;
        int numWeapons;
        float wpnDamage, adjDamage;
        float space = d.availableSpace() * spacePct;

        for (ShipWeapon wpn : weapons()) {
            if (!wpn.noWeapon()) {
                numWeapons = (int) (space/wpn.space(d));
				
				// modnar: accounting for planetDamageMod()
				// correctly calculate damage estimate for attacking colony (in round-about way)
				// beams and torpedoes do half damage against colonies, planetDamageMod() = 0.5f
				// other weapons have planetDamageMod() = 1.0f, so this correction would have no effect for them
				// average(beamMax/2-shield, beamMin/2-shield)  // correct formula
				// = average(beamMax-2*shield, beamMin-2*shield)/2  // equivalent formula used here
                wpnDamage = numWeapons * wpn.firepower(shieldLevel/wpn.planetDamageMod()) * wpn.planetDamageMod();
                if (wpn.isLimitedShotWeapon()) {
                    float base = pow(2,wpn.shots());
                    adjDamage = (base -1)/base * wpnDamage;
                }
                else
                    adjDamage = wpnDamage * sqrt(wpn.range());
                if (adjDamage > bestDamage) {
                    bestWpn = wpn;
                    bestDamage = adjDamage;
                }
            }
        }
        return bestWpn;
    }
    public ShipWeapon bestUnlimitedShotWeapon(ShipDesign d, float spacePct) {
        float shieldLevel = bestEnemyShieldLevel;
        ShipWeapon bestWpn = null;
        float bestDamage = -1;
        int numWeapons;
        float wpnDamage, adjDamage;
        float space = d.availableSpace() * spacePct;

        for (ShipWeapon wpn : weapons()) {
            if (!wpn.noWeapon() && !wpn.groundAttacksOnly() && !wpn.isLimitedShotWeapon()) {
                numWeapons = (int) (space/wpn.space(d));
                wpnDamage = numWeapons * wpn.firepower(shieldLevel);
                adjDamage = wpnDamage * sqrt(wpn.range());
                if (adjDamage > bestDamage) {
                    bestWpn = wpn;
                    bestDamage = adjDamage;
                }
            }
        }
        return bestWpn;
    }
    public BufferedImage missileImage(Tech tech, int length, int height) {
    	String key = tech.imageKey();
    	int size = ShipDesign.SMALL;
    	int rawModel = 1;
    	int shipColor = ImageColorizer.RED;
    	switch (key) {
	    	case "MISSILE_SCATTER_PACK_V":
	    	case "MISSILE_SCATTER_PACK_VII":
	    	case "MISSILE_SCATTER_PACK_X":
	    		size = ShipDesign.SMALL;
	    		rawModel = ShipLibrary.current().scatterDesign.get(shipStyleIndex());
	    		shipColor = ImageColorizer.BLUE;
	    		break;    		

	    	case "TORPEDO_ANTI_MATTER":
	    	case "TORPEDO_HELLFIRE":
	    	case "TORPEDO_PROTON":
	    	case "TORPEDO_PLASMA":
	    		size = ShipDesign.SMALL;
	    		rawModel = ShipLibrary.current().torpedoDesign.get(shipStyleIndex());
	    		shipColor = ImageColorizer.GREEN;
	    		break;

	    	default:
	    		size = ShipDesign.SMALL;
	    		rawModel = ShipLibrary.current().missileDesign.get(shipStyleIndex());
	    		shipColor = ImageColorizer.RED;
    	}
    	boolean flip = rawModel<0;
    	int model;
		if (flip)
			model = 1+rawModel;
		else
			model = rawModel-1;

    	// current missile image size
    	int mW = 173;
    	int mH = 54;
    	// current missile location
    	int mX1 = 13;
    	int mY1 = 3;
    	int mX2 = 155;
    	int mY2 = 52;
    	
        // get source image to be transformed
        ShipImage src = ShipLibrary.current().shipImage(shipStyleIndex(), size, model);
        Image image = newBufferedImage(icon(src.baseIcon()).getImage());
        int imgW = image.getWidth(null);
        int imgH = image.getHeight(null);
        BufferedImage srcImg = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) srcImg.getGraphics();
        if (flip)
            g.drawImage(image, 0, 0, imgW, imgH, imgW, imgH, 0, 0, null);
        else
        	g.drawImage(image, 0, 0, imgW, imgH, 0, 0, imgW, imgH, null);
        g.dispose();
        //image = null;
        BufferedImage clippedImg = clippedImage(srcImg);
        clippedImg = colorizer.makeColor(shipColor, clippedImg);
        imgW = clippedImg.getWidth();
        imgH = clippedImg.getHeight();
 
        // image to build
        int imgWidth = length;
        int imgX1, imgX2, imgY1, imgY2, imgHeight;
        if (height<=0) {
            imgX1	  = mX1 * imgWidth / mW;
            imgX2	  = mX2 * imgWidth / mW;
            imgY1	  = mY1 * imgWidth / mW;
            imgY2	  = mY2 * imgWidth / mW;
            imgHeight = mH  * imgWidth / mW;
        }
        else {
            imgHeight = height;
            imgX1	  = 0;
            imgX2	  = imgWidth;
            imgY1	  = 0;
            imgY2	  = imgHeight;
        }

        BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_ARGB);
        g = (Graphics2D) img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY); 
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.drawImage(clippedImg, imgX1, imgY1, imgX2, imgY2, 0, 0, imgW, imgH, null);
        g.dispose();
        return img;
    }
    private BufferedImage clippedImage(BufferedImage srcImg) {
        int imgW = srcImg.getWidth();
        int imgH = srcImg.getHeight();
        int yMin = Integer.MAX_VALUE;
        int xMin = Integer.MAX_VALUE;
        int yMax = -1;
        int xMax = -1;
        for(int x=0; x<imgW; x++) {
        	 for(int y=0; y<imgH; y++) {
        		 int color = srcImg.getRGB(x, y);
        		 int alpha = (color >> 24) & 0xff;
        		 if (alpha > 32) {
           			 if (y<yMin)
        				 yMin = y;
           			 else if (y>yMax)
        				 yMax = y;
           			 if (x<xMin)
           				xMin = x;
           			 else if (x>xMax)
           				xMax = x;
        		}
        	}
        }
        return srcImg.getSubimage(xMin, yMin, xMax-xMin+1, yMax-yMin+1);
    }
}
