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
package rotp.model.empires;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import rotp.model.ships.Design;
import rotp.model.ships.ShipArmor;
import rotp.model.ships.ShipComputer;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipECM;
import rotp.model.ships.ShipEngine;
import rotp.model.ships.ShipManeuver;
import rotp.model.ships.ShipShield;
import rotp.model.ships.ShipSpecial;
import rotp.model.ships.ShipWeapon;
import rotp.util.Base;

public class ShipView implements Base,Serializable {
    private static final long serialVersionUID = 1L;
    private final static int UNARMED = -1;
    private final static int UNKNOWN = 0;
    private final static int ARMED = 1;
    // owner is the *viewing* empire.
    private final Empire owner;
    // empire is the owner of the actual ship.
    private final Empire empire;
    private final ShipDesign design;
    private boolean detected = false;
    private boolean encountered = false;
    private boolean scanned = false;
    private int armedFlag = UNKNOWN;

    private int hits;
    private int attackLevel, combatSpeed, beamDefense, missileDefense;
    private ShipComputer computer;
    private ShipShield shield;
    private ShipECM ecm;
    private ShipArmor armor;
    private ShipEngine engine;
    private ShipManeuver maneuver;
    private boolean computerKnown, shieldKnown, ecmKnown, armorKnown, hitsKnown, engineKnown, maneuverKnown;
    private boolean attackLevelKnown, combatSpeedKnown, missileDefenseKnown, beamDefenseKnown;

    private final ShipWeapon[] weapon = new ShipWeapon[ShipDesign.maxWeapons()];
    private final boolean[] wpnKnown = new boolean[ShipDesign.maxWeapons()];
    private final int[] wpnCount = new int[ShipDesign.maxWeapons()];
//    private final ShipSpecial[] special = new ShipSpecial[ShipDesign.maxSpecials()];
//    private final boolean[] spcKnown = new boolean[ShipDesign.maxSpecials()];
    private final ShipSpecial[] special;
    private final boolean[] spcKnown;

    // historical data
    private int firstViewDate = 0;
    private int lastViewDate = 0;
    private int totalKilled = 0;
    private int totalKills = 0;

    public Empire empire()                { return empire; }
    public ShipDesign design()            { return design; }
    public int firstViewDate()            { return firstViewDate; }
    public Integer lastViewDate()         { return lastViewDate; }
    public int totalKills()               { return totalKills; }
    public void addTotalKills(int i)      { totalKills += i; }
    public int totalKilled()              { return totalKilled; }
    public void addTotalKilled(int i)     { totalKilled += i; }

    // A ShipView is always tied to exactly one ShipDesign.
    // In the case of un-encountered empires, when only ship size is known, no ShipView is created.
    // A public static function that always returns true is obviously completely vacuous,
    // but serves as a hook where we could potentially have other kinds of views for un-encountered empires.
    // (And links IDEs to where the fact that the design is distinguishable is *used* in the RacesMilitaryUI.)
    public static boolean canDistinguishFromOtherDesigns() { return true; }
    // The size is always known and displayed.
    public static boolean sizeKnown() { return true; }
    // The warp speed is always known and displayed.
    // (I guess this follows from canDistinguishFromOtherDesigns(), as scanners track a ship over the course of a year?)
    public static boolean warpSpeedKnown() { return true; }
    public float maxPossibleWarpSpeed() {
        if (warpSpeedKnown())
            return design.warpSpeed();
        else
            return 9;
    }
    public float minPossibleWarpSpeed() {
        if (warpSpeedKnown())
            return design.warpSpeed();
        else
            return 1;
    }

    public boolean hasComputer()        { return computer != null; }
    public boolean computerKnown()      { return computerKnown; }
    public boolean hasShield()          { return shield != null; }
    public boolean shieldKnown()        { return shieldKnown; }
    public boolean hasECM()             { return ecm != null; }
    public boolean ecmKnown()           { return ecmKnown; }
    public boolean hitsKnown()          { return hitsKnown; }
    public boolean hasArmor()           { return armor != null;	}
    public boolean armorKnown()         { return armorKnown; }
    public boolean hasEngine()          { return engine != null; }
    public boolean engineKnown()        { return engineKnown; }
    public boolean hasManeuver()        { return maneuver != null; }
    public boolean maneuverKnown()      { return maneuverKnown; }
    public boolean hasWeapon(int i)     { return (weapon[i] != null) && !weapon[i].noWeapon(); }
    public ShipWeapon weapon(int i)     { return weapon[i]; }
    public boolean weaponKnown(int i)   { return wpnKnown[i]; }
    public boolean hasSpecial(int i)    { return (special[i] != null) && !special[i].isNone(); }
    public boolean specialKnown(int i)  { return spcKnown[i]; }
    public boolean missileDefenseKnown() { return missileDefenseKnown; }
    public boolean beamDefenseKnown()   { return beamDefenseKnown; }
    public boolean attackLevelKnown()   { return attackLevelKnown; }
    public boolean combatSpeedKnown()   { return combatSpeedKnown; }

    public boolean isPotentiallyArmed() { return armedFlag != UNARMED; }

    public void setViewDate() {
        if (firstViewDate == 0)
            firstViewDate = galaxy().currentYear();
        lastViewDate = galaxy().currentYear();
    }

    public static Comparator<ShipView> VIEW_DATE = (ShipView o1, ShipView o2) -> o2.lastViewDate().compareTo(o1.lastViewDate());
    public static Comparator<ShipView> VIEW_ACTIVE = new ViewActiveComparator();
    public ShipView(Empire o, ShipDesign d) {
        special = new ShipSpecial[d.maxSpecials()];
        spcKnown = new boolean[d.maxSpecials()];
        owner = o;
        design = d;
        empire = d.empire();
    }
    public ShipView(ShipDesign d) { // BR: For Monsters
        special	 = new ShipSpecial[d.maxSpecials()];
        spcKnown = new boolean[d.maxSpecials()];
        empire	 = d.empire();
        owner	 = empire;
        design	 = d;
        scan();
    }
    public boolean matches(Design d) { return design == d;  }
    // detect is for long-range scanning
    public void detect() {
        lastViewDate = galaxy().currentYear();
        if (!detected)
            firstViewDate = galaxy().currentYear();

        detected = true;
    }
    // encounter is for same orbit
    public void encounter() {
        lastViewDate = galaxy().currentYear();

        if (encountered) return;

        encountered = true;
        detect();

        armedFlag = design.isArmed() ? ARMED : UNARMED;
        scanManeuver();
        scanEngine();
        scanArmor();
        owner.makeContact(empire);
    }
    // scanned is when battle scanned
    public void scan() {
        lastViewDate = galaxy().currentYear();
        scanned = true;

        encounter();
        scanComputer();
        scanShield();
        scanECM();
        scanWeapons();
        scanSpecials();
        owner.makeContact(empire);
    }
    public boolean scanned()    { return scanned; }
    public float visibleFirepower(int shieldLevel) {
        if (scanned)
            return design.firepower(shieldLevel);
        else
            return owner.estimatedShipFirepower(empire, design.size(), shieldLevel);
    }
    public float visibleFirepowerAntiShip(int shieldLevel) {
        if (scanned)
            return design.firepowerAntiShip(shieldLevel);
        else
            return owner.estimatedShipFirepower(empire, design.size(), shieldLevel);
    }
    private void scanComputer() {
        computer = design.computer();
        attackLevel = (int) design.attackLevel()+empire.shipAttackBonus();
        computerKnown = true;
        attackLevelKnown = true;
    }
    private void scanShield() {
        shield = design.shield();
        beamDefense = design.beamDefense()+empire.shipDefenseBonus();
        missileDefense = design.missileDefense()+empire.shipDefenseBonus();
        shieldKnown = true;
        beamDefenseKnown = true;
        missileDefenseKnown = true;
    }
    private void scanECM() {
        ecm = design.ecm();
        ecmKnown = true;
    }
    private void scanArmor() {
        armor = design.armor();
        hits = (int) design.hits();
        armorKnown = true;
        hitsKnown = true;
    }
    private void scanEngine() {
        engine = design.engine();
        engineKnown = true;
    }
    private void scanManeuver() {
        maneuver = design.maneuver();
        maneuverKnown = true;
        combatSpeed = design.combatSpeed();
        combatSpeedKnown = true;
    }
    private void scanWeapons() {
        for (int i=0;i<ShipDesign.maxWeapons();i++) {
            wpnKnown[i] = true;
            if ((design.weapon(i) == null) || design.weapon(i).noWeapon()) {
                wpnCount[i] = 0;
                weapon[i] = null;
            }
            else {
                wpnCount[i] = design.wpnCount(i);
                weapon[i] = design.weapon(i);
            }
        }
    }
    private void scanSpecials() {
        for (int i=0; i<special.length; i++) {
            spcKnown[i] = true;
            if ((design.special(i) == null) || design.special(i).isNone())
                special[i] = null;
            else
                special[i] = design.special(i);
        }
    }
    public List<ShipWeapon> weapons() {
        List<ShipWeapon> wpns = new ArrayList<>();
        for (int i=0;i<wpnCount.length;i++) {
            if (wpnCount[i] > 0)
                wpns.add(weapon[i]);
        }
        return wpns;
    }
    public List<ShipSpecial> specials() {
        List<ShipSpecial> spec = new ArrayList<>();
        for (int i=0;i<special.length;i++) {
            if (special[i] != null)
                spec.add(special[i]);
        }
        return spec;
    }
    // For the reporting name given by the viewing empire, we use the actual name of the design.
    // (The player will never have access to the "real" name, so there's no need for the reporting name to be different.)
    public String reportingName()    { return design.name(); }
    public int hits()                { return hits; }
    public ShipShield shield()       { return shield; }
    public ShipComputer computer()   { return computer; }
    public int attackLevel()         { return attackLevel; }
    public int missileDefense()      { return missileDefense; }
    public int beamDefense()         { return beamDefense; }
    public int combatSpeed()         { return combatSpeed; }
    public int wpnCount(int i)       { return wpnCount[i]; }

    private static class ViewActiveComparator implements Comparator<ShipView> {
    	@Override public int compare(ShipView o1, ShipView o2) {
            ShipDesign d1 = o1.design;
            ShipDesign d2 = o2.design;
            if (d1.active() && !d2.active())
                return -1;
            else if (d2.active() && !d1.active())
                return 1;
            else
                return d1.name().compareTo(d2.name());
        }
    }
}
