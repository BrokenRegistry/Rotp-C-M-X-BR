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

import java.util.List;

import rotp.model.colony.Colony;
import rotp.model.combat.CombatStackSpaceAmoeba;
import rotp.model.planet.PlanetType;
import rotp.model.ships.ShipArmor;
import rotp.model.ships.ShipComputer;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipECM;
import rotp.model.ships.ShipEngine;
import rotp.model.ships.ShipManeuver;
import rotp.model.ships.ShipShield;
import rotp.model.ships.ShipSpecialRepair;
import rotp.model.ships.ShipWeaponBeam;
import rotp.model.tech.TechAutomatedRepair;
import rotp.model.tech.TechShipWeapon;

public class SpaceAmoeba extends SpaceMonster {
	private static final long serialVersionUID = 1L;

	public SpaceAmoeba(Float speed, Float level) {
		super("SPACE_AMOEBA", -4, speed, level);
	}

	private int hullHitPoints()		{ return moO1Level (3000, 1000, 200, 0.5f, 0.5f); }
	
	@Override public void initCombat()		{
		super.initCombat();
		addCombatStack(new CombatStackSpaceAmoeba(this, travelSpeed(), stackLevel()));
	}
	@Override public SpaceMonster getCopy() { return new SpaceAmoeba(null, null); }
	@Override public void degradePlanet(StarSystem sys) {
		Colony col = sys.colony();
		if (col != null) {
			float prevFact = col.industry().factories();
			col.industry().factories(prevFact*0.1f);
			sys.empire().lastAttacker(this);
			col.destroy();
		}
		if (options().isMoO1Monster())
			sys.planet().irradiateEnvironment(5 * roll(10/5, 25/5));
		else
			sys.planet().degradeToType(PlanetType.BARREN);
		sys.planet().resetWaste();
		sys.abandoned(false);
	}
	@Override protected ShipDesign designMoO1()	{
		ShipDesignLab lab = empire().shipLab();
		
		// System.out.println();
		// System.out.print("design ");
		ShipDesign design = lab.newBlankDesign(-hullHitPoints());
		
		design.mission	(ShipDesign.DESTROYER);

		// System.out.print("engine ");
		List<ShipEngine> engines = lab.engines();
		design.engine	(engines.get(stackLevel(1, engines.size()-1)));

		// System.out.print("computer ");
		List<ShipComputer> computers = lab.computers();
		design.computer	(computers.get(stackLevel(10, computers.size()-1)));

		// System.out.print("armor ");
		List<ShipArmor> armors = lab.armors();
		design.armor	(armors.get(stackLevel(0, armors.size()-1)));

		// System.out.print("shield ");
		List<ShipShield> shields = lab.shields();
		design.shield	(shields.get(stackLevel(0, shields.size()-1)));

		// System.out.print("ecm ");
		List<ShipECM> ecms = lab.ecms();
		design.ecm		(ecms.get(stackLevel(2, ecms.size()-1)));

		// System.out.print("maneuver ");
		List<ShipManeuver> maneuvers = lab.maneuvers();
		design.maneuver	(maneuvers.get(stackLevel(1, maneuvers.size()-1)));

		// System.out.print("weapon ");
		int wpnAll = max(1, stackLevel(1));
		for (int i=4; i>0; i--) {
			int count = wpnAll/i;
			if (count != 0) {
				// Amoeba stream
				//design.weapon(i-1, new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:22"), false), count);
				design.weapon(i-1, lab.beamWeapon(22, false), count);
				wpnAll -= count;
			}
		}
		design.special(0, lab.specialAdvDamControl());

		// design.special(0, new ShipSpecialRepair((TechAutomatedRepair)tech("AutomatedRepair:1"))); // Advanced Damage Control
		// design.special(0, lab.specialBattleScanner());
		// design.special(1, lab.specialTeleporter());
		// design.special(2, lab.specialCloak());
		// design.name(text(IMAGE_KEY));
		// lab.iconifyDesign(design);
		return design;
	}
	@Override protected ShipDesign designRotP()	{
		ShipDesignLab lab = empire().shipLab();
		
		// System.out.println();
		// System.out.print("design ");
		int hp = monsterLevel == 1f? 3500 : 1500;
		ShipDesign design = lab.newBlankDesign(-hp);
		
		design.mission	(ShipDesign.DESTROYER);

		// System.out.print("engine ");
		List<ShipEngine> engines = lab.engines();
		design.engine	(engines.get(stackLevel(1, engines.size()-1)));

		// System.out.print("computer ");
		List<ShipComputer> computers = lab.computers();
		design.computer	(computers.get(stackLevel(10, computers.size()-1)));

		// System.out.print("armor ");
		List<ShipArmor> armors = lab.armors();
		design.armor	(armors.get(stackLevel(0, armors.size()-1)));

		// System.out.print("shield ");
		List<ShipShield> shields = lab.shields();
		design.shield	(shields.get(stackLevel(0, shields.size()-1)));

		// System.out.print("ecm ");
		List<ShipECM> ecms = lab.ecms();
		design.ecm		(ecms.get(stackLevel(0, ecms.size()-1)));

		// System.out.print("maneuver ");
		List<ShipManeuver> maneuvers = lab.maneuvers();
		design.maneuver	(maneuvers.get(stackLevel(0, maneuvers.size()-1)));

		// TODO BR: create Amoeba specials
		
		design.special(0, lab.specialBattleScanner()); // Limited Damage
		design.special(1, lab.specialBattleScanner()); // Mitosis
		design.special(2, lab.specialAmoebaEatShips()); // Eat
		design.special(0, new ShipSpecialRepair((TechAutomatedRepair)tech("AutomatedRepair:2"))); // Limited Damage
		design.special(1, new ShipSpecialRepair((TechAutomatedRepair)tech("AutomatedRepair:3"))); // Mitosis
//		design.special(2, new ShipSpecialRepair((TechAutomatedRepair)tech("AutomatedRepair:1"))); // Eat
		
		// design.special(0, new ShipSpecialRepair((TechAutomatedRepair)tech("AutomatedRepair:1"))); // Advanced Damage Control
		// design.special(0, lab.specialBattleScanner());
		// design.special(1, lab.specialTeleporter());
		// design.special(2, lab.specialCloak());
		// design.name(text(IMAGE_KEY));
		// lab.iconifyDesign(design);
		return design;
	}
}