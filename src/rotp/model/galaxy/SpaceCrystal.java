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
import java.util.List;

import rotp.model.colony.Colony;
import rotp.model.combat.CombatStackMonster;
import rotp.model.combat.CombatStackSpaceCrystal;
import rotp.model.planet.PlanetType;
import rotp.model.ships.ShipArmor;
import rotp.model.ships.ShipComputer;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipECM;
import rotp.model.ships.ShipEngine;
import rotp.model.ships.ShipShield;

public class SpaceCrystal extends SpaceMonster {
    private static final long serialVersionUID = 1L;
    private static final Color shieldColor	= Color.cyan;
    private static final String imageKey	= "SPACE_CRYSTAL";
    private static final boolean isFusion	= false;

    public SpaceCrystal(Float speed, Float level) { super(imageKey, -3, speed, level); }

    @Override  public void initCombat() {
    	super.initCombat();
		if (options().isMoO1Monster())
			addCombatStack(new CombatStackMonster(this, imageKey, stackLevel(), 0, isFusion, shieldColor));
		else
			addCombatStack(new CombatStackSpaceCrystal(this, imageKey, stackLevel(), 0, shieldColor));
    }
    @Override public SpaceMonster getCopy()		{ return new SpaceCrystal(null, null); }
    @Override protected int otherSpecialCount() { return options().isMoO1Monster()? 1:2; }
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
		ShipDesign design = lab.newBlankDesign(4, hullHitPoints());
		design.mission	(ShipDesign.DESTROYER);

		List<ShipEngine> engines = lab.engines();
		design.engine	(engines.get(stackLevel(1, engines.size()-1)));

		List<ShipComputer> computers = lab.computers();
		int attackLevel = stackLevel(10);
		design.computer	(computers.get(min(attackLevel, computers.size()-1)));

		List<ShipArmor> armors = lab.armors();
		design.armor	(armors.get(stackLevel(0, armors.size()-1)));

		List<ShipShield> shields = lab.shields();
		design.shield	(shields.get(stackLevel(5, shields.size()-1)));

		List<ShipECM> ecms = lab.ecms();
		design.ecm		(ecms.get(stackLevel(2, ecms.size()-1)));

		int maneuver = max(2, stackLevel(2));
		design.maneuver(lab.maneuver(maneuver));
		design.monsterManeuver(maneuver);
		design.monsterAttackLevel(attackLevel);
		design.monsterBeamDefense(1);
		design.monsterEcmDefense(1);
		design.monsterInitiative(100);

		int wpnAll = max(1, stackLevel(10));
		for (int i=4; i>0; i--) {
			int count = wpnAll/i;
			if (count != 0) {
				// Crystal ray
				design.weapon(i-1, lab.crystalRay(), count);
				wpnAll -= count;
			}
		}
		design.special(0, lab.specialLightningShield());
		design.special(1, lab.specialAdvDamControl());		// Advanced Damage control
		design.special(2, lab.specialBlackHole());			// Black Hole Generator
		design.special(3, lab.specialResistStasis());		// Immune to Stasis
		return design;
	}
	@Override protected ShipDesign designRotP()	{
		ShipDesignLab lab = empire().shipLab();
		int hp = (int) (stackLevel(7000));
		ShipDesign design = lab.newBlankDesign(5, hp);
		
		design.mission	(ShipDesign.DESTROYER);

		List<ShipEngine> engines = lab.engines();
		design.engine	(engines.get(stackLevel(0, engines.size()-1)));

		List<ShipComputer> computers = lab.computers();
		int computerLevel = stackLevel(10);
		design.computer	(computers.get(min(computerLevel, computers.size()-1)));

		List<ShipArmor> armors = lab.armors();
		design.armor	(armors.get(stackLevel(0, armors.size()-1)));

		List<ShipShield> shields = lab.shields();
		design.shield	(shields.get(stackLevel(5, shields.size()-1)));

		List<ShipECM> ecms = lab.ecms();
		design.ecm		(ecms.get(stackLevel(0, ecms.size()-1)));

		int maneuver = max(2, stackLevel(2));
		design.maneuver(lab.maneuver(maneuver));
		design.monsterManeuver(maneuver);
		design.monsterAttackLevel(20); // Always hit
		design.monsterBeamDefense(1);
		design.monsterEcmDefense(1);
		design.monsterInitiative(100);

		design.special(0, lab.specialZyroShield());
		design.special(1, lab.specialTeleporter());
		design.special(2, lab.specialCrystalPulsar());
		design.special(3, lab.specialCrystalNullifier());
		design.special(4, lab.specialResistStasis());
		return design;
	}
}
