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
package rotp.model.galaxy;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import rotp.model.combat.CombatStackOrionGuardian;
import rotp.model.empires.Empire;
import rotp.model.incidents.DiplomaticIncident;
import rotp.model.incidents.KillGuardianIncident;
import rotp.model.ships.ShipArmor;
import rotp.model.ships.ShipComputer;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipECM;
import rotp.model.ships.ShipEngine;
import rotp.model.ships.ShipManeuver;
import rotp.model.ships.ShipShield;
import rotp.model.ships.ShipSpecialBeamFocus;
import rotp.model.ships.ShipSpecialMissileShield;
import rotp.model.ships.ShipSpecialRepair;
import rotp.model.ships.ShipWeaponBeam;
import rotp.model.ships.ShipWeaponMissile;
import rotp.model.ships.ShipWeaponTorpedo;
import rotp.model.tech.TechAutomatedRepair;
import rotp.model.tech.TechBeamFocus;
import rotp.model.tech.TechMissileShield;
import rotp.model.tech.TechMissileWeapon;
import rotp.model.tech.TechShipWeapon;
import rotp.model.tech.TechTorpedoWeapon;
import rotp.ui.main.GalaxyMapPanel;

public class OrionGuardianShip extends GuardianMonsters {
    private static final long serialVersionUID = 1L;
	private static final String IMAGE_KEY = "ORION_GUARDIAN";
    private final List<String> techs = new ArrayList<>();
	public OrionGuardianShip(Float speed, Float level)	{
		super(IMAGE_KEY, -2, speed, level);
		num(0, 1); // Number of monsters
		if (!options().isMoO1Monster())
        techs.add("ShipWeapon:16");  // death ray
    }
	@Override public void initCombat()	{
        combatStacks().clear();
		if (options().isMoO1Monster())
			addCombatStack(new CombatStackOrionGuardian(this, IMAGE_KEY, 1f, 0));
		else
			addCombatStack(new CombatStackOrionGuardian(this, IMAGE_KEY, options().guardianMonstersLevel(), 0));	   
    }
	@Override public SpaceMonster getCopy() { return new OrionGuardianShip(null, null); }
	@Override public int maxMapScale()		{ return GalaxyMapPanel.MAX_FLEET_HUGE_SCALE; }
	@Override public void plunder()			{ 
        super.plunder();
        Empire emp = this.lastAttacker();
        for (String techId: techs)
            emp.plunderShipTech(tech(techId), -2); 

        removeGuardian();
    } 
	@Override public boolean isGuardian()	{ return true; }
	@Override public boolean isMonsterGuardian()	{ return false; }
	@Override protected DiplomaticIncident killIncident(Empire emp)	{ return KillGuardianIncident.create(emp.id, lastAttackerId, nameKey); }

	// BR: Redundant for backward compatibility
	@Override public Image image()	{ return image(IMAGE_KEY); }

	// @Override protected int otherSpecialCount() { return 0; } // change if needed
	private int hullHitPoints()		{ return moO1Level (8000, 1000, 2000, 0.4f, 0.15f); }
	private int defenseValue()		{ return moO1Level ( 7,  1, 1, 0.3f, 0.15f); }
	private int rocketsCount()		{ return moO1Level (45, 20, 1, 0.3f, 0.15f); }
	private int convertersCount()	{ return moO1Level (25, 10, 1, 0.3f, 0.15f); }
	private int torpedoesCount()	{ return moO1Level (12,  3, 1, 0.3f, 0.15f); }
	@Override protected ShipDesign designMoO1()	{
		ShipDesignLab lab = empire().shipLab();
		
		ShipDesign design = lab.newBlankDesign(-hullHitPoints());
		design.mission	(ShipDesign.DESTROYER);

		List<ShipEngine> engines = lab.engines();
		design.engine	(engines.get(stackLevel(1, engines.size()-1)));

		List<ShipComputer> computers = lab.computers();
		design.computer	(computers.get(stackLevel(10, computers.size()-1)));

		List<ShipArmor> armors = lab.armors();
		design.armor	(armors.get(stackLevel(0, armors.size()-1)));

		int defense = defenseValue();
		List<ShipShield> shields = lab.shields();
		design.shield	(shields.get(stackLevel(defense-1, shields.size()-1)));

		List<ShipECM> ecms = lab.ecms();
		design.ecm		(ecms.get(stackLevel(defense-1, ecms.size()-1)));

		List<ShipManeuver> maneuvers = lab.maneuvers();
		design.maneuver	(maneuvers.get(stackLevel(1, maneuvers.size()-1)));
		// Scatter Pack X Rockets
		design.weapon(0, new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:10"), false, 5, 7, 3.5f), rocketsCount());
		// Stellar Converters
		design.weapon(1, new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:20"), false), convertersCount());
		// Plasma Torpedoes
		design.weapon(2, new ShipWeaponTorpedo((TechTorpedoWeapon) tech("TorpedoWeapon:3")), torpedoesCount());
		// Death rays
		design.weapon(3, new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:16"), false), 1);
		
		design.special(0, new ShipSpecialMissileShield((TechMissileShield)tech("MissileShield:3"))); // Lightning Shield
		float pFactor = options().aiProductionModifier();
		if (pFactor > 1.4f)
			design.special(1, new ShipSpecialRepair((TechAutomatedRepair)tech("AutomatedRepair:1"))); // Advanced Damage Control
		else if (pFactor > 1.2f)
			design.special(1, new ShipSpecialRepair((TechAutomatedRepair)tech("AutomatedRepair:0"))); // Automated Repair System

		// design.special(2, new ShipSpecialBlackHole((TechBlackHole)tech("BlackHole:0"))); // Black Hole Generator
		// design.special(0, lab.specialBattleScanner());
		// design.special(1, lab.specialTeleporter());
		// design.special(2, lab.specialCloak());
		// design.name(text(IMAGE_KEY));
		// lab.iconifyDesign(design);
		return design;
	}
	@Override protected ShipDesign designRotP()	{
		ShipDesignLab lab = empire().shipLab();
		
		ShipDesign design = lab.newBlankDesign(-10000);
		design.mission	(ShipDesign.DESTROYER);

		List<ShipEngine> engines = lab.engines();
		design.engine	(engines.get(stackLevel(1, engines.size()-1)));

		List<ShipComputer> computers = lab.computers();
		design.computer	(computers.get(stackLevel(10, computers.size()-1)));

		List<ShipArmor> armors = lab.armors();
		design.armor	(armors.get(stackLevel(0, armors.size()-1)));

		List<ShipShield> shields = lab.shields();
		design.shield	(shields.get(stackLevel(9, shields.size()-1)));

		List<ShipECM> ecms = lab.ecms();
		design.ecm		(ecms.get(stackLevel(9, ecms.size()-1)));

		List<ShipManeuver> maneuvers = lab.maneuvers();
		design.maneuver	(maneuvers.get(stackLevel(1, maneuvers.size()-1)));
		// Scatter Pack X Rockets
		design.weapon(0, new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:10"), false, 5, 7, 3.5f), 85);
		// Stellar Converters
		design.weapon(1, new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:20"), false), 45);
		// Plasma Torpedoes
		design.weapon(2, new ShipWeaponTorpedo((TechTorpedoWeapon) tech("TorpedoWeapon:3")), 18);
		// Death rays
		design.weapon(3, new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:16"), false), 1);
		
		design.special(0, new ShipSpecialBeamFocus((TechBeamFocus)tech("BeamFocus:0"))); // High Energy Focus
		design.special(1, new ShipSpecialRepair((TechAutomatedRepair)tech("AutomatedRepair:1"))); // Advanced Damage Control

		// design.special(2, new ShipSpecialBlackHole((TechBlackHole)tech("BlackHole:0"))); // Black Hole Generator
		// design.special(0, lab.specialBattleScanner());
		// design.special(1, lab.specialTeleporter());
		// design.special(2, lab.specialCloak());
		// design.name(text(IMAGE_KEY));
		// lab.iconifyDesign(design);
		return design;
	}
}
