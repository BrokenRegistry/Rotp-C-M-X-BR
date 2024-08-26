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

import java.awt.Color;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import rotp.model.combat.CombatStackMonster;
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
import rotp.ui.main.GalaxyMapPanel;

public class OrionGuardianShip extends GuardianMonsters {
    private static final long serialVersionUID = 1L;
    private static final Color shieldColor	= Color.blue;
    private static final String imageKey	= "ORION_GUARDIAN";
    private static final boolean isFusion	= false;
    private final List<String> techs = new ArrayList<>();

    public OrionGuardianShip(Float speed, Float level)	{
		super(imageKey, -2, speed, level);
		num(0, 1); // Number of monsters
		if (!options().isMoO1Monster())
        techs.add("ShipWeapon:16");  // death ray
    }
	@Override public void initCombat()		{
        combatStacks().clear();
		if (options().isMoO1Monster())
			addCombatStack(new CombatStackMonster(this, imageKey, stackLevel(), 0, isFusion, shieldColor));
		else
			addCombatStack(new CombatStackOrionGuardian(this, imageKey, stackLevel(), 0, shieldColor));
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
	@Override public boolean isOrionGuardian()	{ return true; }
	@Override public boolean isFusionGuardian()	{ return false; }
	@Override protected DiplomaticIncident killIncident(Empire emp)	{
		return KillGuardianIncident.create(emp.id, lastAttackerId, nameKey);
	}

	// BR: Redundant for backward compatibility
	@Override public Image image()	{ return image(imageKey); }

	@Override protected int otherSpecialCount() { return 1; } // change if needed
	private int hullHitPoints()		{ return moO1Level (8000, 1000, 2000, 0.4f, 0.15f); }
	private int defenseValue()		{ return moO1Level ( 7,  1, 1, 0.3f, 0.15f); }
	private int rocketsCount()		{ return moO1Level (45, 20, 1, 0.3f, 0.15f); }
	private int convertersCount()	{ return moO1Level (25, 10, 1, 0.3f, 0.15f); }
	private int torpedoesCount()	{ return moO1Level (12,  3, 1, 0.3f, 0.15f); }
	@Override protected ShipDesign designMoO1()	{
		ShipDesignLab lab = empire().shipLab();
		
		ShipDesign design = lab.newBlankDesign(ShipDesign.maxSpecials, stackLevel(hullHitPoints()));
		design.mission	(ShipDesign.DESTROYER);

		List<ShipEngine> engines = lab.engines();
		design.engine	(engines.get(stackLevel(1, engines.size()-1)));

		List<ShipComputer> computers = lab.computers();
		int attackLevel = stackLevel(10);
		design.computer	(computers.get(min(attackLevel, computers.size()-1)));

		List<ShipArmor> armors = lab.armors();
		design.armor	(armors.get(stackLevel(0, armors.size()-1)));

		int defense = defenseValue();
		List<ShipShield> shields = lab.shields();
		design.shield	(shields.get(stackLevel(defense-1, shields.size()-1)));

		List<ShipECM> ecms = lab.ecms();
		design.ecm		(ecms.get(stackLevel(defense-1, ecms.size()-1)));

		List<ShipManeuver> maneuvers = lab.maneuvers();
		design.maneuver	(maneuvers.get(stackLevel(1, maneuvers.size()-1)));

		design.weapon(0, lab.scatterPackXMissiles(x5), rocketsCount());
		design.weapon(1, lab.stellarConverter(), convertersCount());
		design.weapon(2, lab.plasmaTorpedoes(), torpedoesCount());
		design.weapon(3, lab.deathRay(), 1);
		
		design.special(0, lab.specialLightningShield());		// Lightning Shield
		float pFactor = options().aiProductionModifier();
		if (pFactor > 1.4f)
			design.special(1, lab.specialAdvDamControl());		// Advanced Damage control
		else if (pFactor > 1.2f)
			design.special(1, lab.specialAutomatedRepair());	// Automated Repair System
		design.special(2, lab.specialResistStasis());			// Immune to Stasis
		
		int maneuver = max(2, stackLevel(2));
		design.maneuver(lab.maneuver(maneuver));
		design.monsterManeuver(maneuver);
		design.monsterAttackLevel(attackLevel);
		design.monsterBeamDefense(defense);
		design.monsterEcmDefense(defense);
		design.monsterInitiative(100);
		
		return design;
	}
	@Override protected ShipDesign designRotP()	{
		ShipDesignLab lab = empire().shipLab();
		ShipDesign design = lab.newBlankDesign(4, stackLevel(10000));
		design.mission	(ShipDesign.DESTROYER);

		List<ShipEngine> engines = lab.engines();
		design.engine	(engines.get(stackLevel(1, engines.size()-1)));

		List<ShipComputer> computers = lab.computers();
		int attackLevel = stackLevel(10);
		design.computer	(computers.get(min(attackLevel, computers.size()-1)));

		List<ShipArmor> armors = lab.armors();
		design.armor	(armors.get(stackLevel(0, armors.size()-1)));

		List<ShipShield> shields = lab.shields();
		design.shield	(shields.get(stackLevel(9, shields.size()-1)));

		List<ShipECM> ecms = lab.ecms();
		int defense = stackLevel(9, ecms.size()-1);
		design.ecm	(ecms.get(defense));

		int maneuver = max(2, stackLevel(2));
		design.maneuver(lab.maneuver(maneuver));
		design.monsterManeuver(maneuver);
		design.monsterAttackLevel(attackLevel);
		design.monsterBeamDefense(defense);
		design.monsterEcmDefense(defense);
		design.monsterInitiative(100);

		// BR: do not change this sequence... CombatStackOrionGuardian needs it this way
		design.weapon(0, lab.scatterPackXMissiles(x5), stackLevel(85));
		design.weapon(1, lab.stellarConverter(), stackLevel(45));
		design.weapon(2, lab.plasmaTorpedoes(), stackLevel(18));
		design.weapon(3, lab.deathRay(), 1);
		
		design.special(0, lab.specialZyroShield());
		design.special(1, lab.specialHighEnergyFocus());	// High Energy Focus
		design.special(2, lab.specialAdvDamControl());		// Advanced Damage control
		design.special(3, lab.specialResistStasis());		// Immune to Stasis
		
		return design;
	}
}
