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

import java.util.List;

import rotp.model.colony.Colony;
import rotp.model.combat.CombatStackSpaceCrystal;
import rotp.model.planet.PlanetType;
import rotp.model.ships.ShipArmor;
import rotp.model.ships.ShipComputer;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipECM;
import rotp.model.ships.ShipEngine;
import rotp.model.ships.ShipManeuver;
import rotp.model.ships.ShipShield;
import rotp.model.ships.ShipSpecialBlackHole;
import rotp.model.ships.ShipSpecialMissileShield;
import rotp.model.ships.ShipSpecialPulsar;
import rotp.model.ships.ShipSpecialRepair;
import rotp.model.ships.ShipSpecialShipNullifier;
import rotp.model.ships.ShipWeaponBeam;
import rotp.model.tech.TechAutomatedRepair;
import rotp.model.tech.TechBlackHole;
import rotp.model.tech.TechEnergyPulsar;
import rotp.model.tech.TechMissileShield;
import rotp.model.tech.TechShipNullifier;
import rotp.model.tech.TechShipWeapon;

public class SpaceCrystal extends SpaceMonster {
    private static final long serialVersionUID = 1L;
    public SpaceCrystal(Float speed, Float level) {
        super("SPACE_CRYSTAL", -3, speed, level);
    }
    @Override  public void initCombat() {
    	super.initCombat();
//		if (options().isMoO1Monster())
//			addCombatStack(new CombatStackMoO1SpaceCrystal(this, "SPACE_CRYSTAL", 1f, 0));
//		else
			addCombatStack(new CombatStackSpaceCrystal(this, travelSpeed(), stackLevel()));	   
    }
    @Override public SpaceMonster getCopy()		{ return new SpaceCrystal(null, null); }
    @Override public void degradePlanet(StarSystem sys) {
        Colony col = sys.colony();
        if (col != null) {
            sys.empire().lastAttacker(this);
            col.destroy();  
        }
        if (!options().isMoO1Monster())
        	sys.planet().degradeToType(PlanetType.DEAD);
        float maxWaste = sys.planet().maxWaste();
        sys.planet().addWaste(maxWaste);
        sys.planet().removeExcessWaste();
        sys.abandoned(false);
    }
	private int hullHitPoints()		{ return moO1Level (5000, 1000, 500, 0.5f, 0.25f); }
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
		design.shield	(shields.get(stackLevel(5, shields.size()-1)));

		// System.out.print("ecm ");
		List<ShipECM> ecms = lab.ecms();
		design.ecm		(ecms.get(stackLevel(2, ecms.size()-1)));

		// System.out.print("maneuver ");
		List<ShipManeuver> maneuvers = lab.maneuvers();
		design.maneuver	(maneuvers.get(stackLevel(1, maneuvers.size()-1)));

		// System.out.print("weapon ");
		int wpnAll = max(1, stackLevel(10));
		for (int i=4; i>0; i--) {
			int count = wpnAll/i;
			if (count != 0) {
				// Crystal ray
				design.weapon(i-1, new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:23"), false), count);
				wpnAll -= count;
			}
		}
		design.special(0, new ShipSpecialMissileShield((TechMissileShield)tech("MissileShield:3"))); // Lightning Shield
		design.special(1, new ShipSpecialRepair((TechAutomatedRepair)tech("AutomatedRepair:1"))); // Advanced Damage Control
		design.special(2, new ShipSpecialBlackHole((TechBlackHole)tech("EnergyPulsar:0"))); // Black Hole Generator
		// design.special(0, lab.specialBattleScanner());
		// design.special(1, lab.specialTeleporter());
		// design.special(2, lab.specialCloak());
		// design.name(text(IMAGE_KEY));
		// lab.iconifyDesign(design);
		return design;
	}
	@Override protected ShipDesign designRotP()	{ // TODO BR: Crystal RotP design
		ShipDesignLab lab = empire().shipLab();
		
		int hp = (int) (7000 * monsterLevel);
		ShipDesign design = lab.newBlankDesign(-hp);
		
		design.mission	(ShipDesign.DESTROYER);

		List<ShipEngine> engines = lab.engines();
		design.engine	(engines.get(stackLevel(0, engines.size()-1)));

		List<ShipComputer> computers = lab.computers();
		design.computer	(computers.get(stackLevel(10, computers.size()-1)));

		List<ShipArmor> armors = lab.armors();
		design.armor	(armors.get(stackLevel(0, armors.size()-1)));

		List<ShipShield> shields = lab.shields();
		design.shield	(shields.get(stackLevel(5, shields.size()-1)));

		List<ShipECM> ecms = lab.ecms();
		design.ecm		(ecms.get(stackLevel(0, ecms.size()-1)));

		List<ShipManeuver> maneuvers = lab.maneuvers();
		design.maneuver	(maneuvers.get(stackLevel(0, maneuvers.size()-1)));

		design.special(0, lab.specialTeleporter());
		design.special(1, new ShipSpecialPulsar((TechEnergyPulsar)tech("EnergyPulsar:2"))); // Crystal Pulsar
		design.special(2, new ShipSpecialShipNullifier((TechShipNullifier)tech("ShipNullifier:2"))); // Crystal Nullifier
		
		// design.special(0, new ShipSpecialRepair((TechAutomatedRepair)tech("AutomatedRepair:1"))); // Advanced Damage Control
		// design.special(0, lab.specialBattleScanner());
		// design.special(2, lab.specialCloak());
		// design.name(text(IMAGE_KEY));
		// lab.iconifyDesign(design);
		return design;
	}
}
