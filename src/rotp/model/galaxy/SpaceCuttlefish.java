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

import rotp.model.combat.CombatStackSpaceCuttlefish;
import rotp.model.ships.ShipArmor;
import rotp.model.ships.ShipComputer;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipECM;
import rotp.model.ships.ShipEngine;
import rotp.model.ships.ShipManeuver;
import rotp.model.ships.ShipShield;

public class SpaceCuttlefish extends GuardianMonsters {
	private static final long serialVersionUID = 1L;
	private static final String IMAGE_KEY = "SPACE_CUTTLEFISH";
	public SpaceCuttlefish(Float speed, Float level) {
		super(IMAGE_KEY, -2, speed, level);
		num(0, 1); // Number of monsters
	}
	@Override public void initCombat()	{
		super.initCombat();
		addCombatStack(new CombatStackSpaceCuttlefish(this, IMAGE_KEY, stackLevel(), 0));	   
	}
	@Override public SpaceMonster getCopy() 	{ return new SpaceCuttlefish(null, null); }
	@Override public float bcValue()			{ return 100 * stackLevel(); }
	@Override protected int otherSpecialCount() { return 2; } // change if needed
	@Override protected float firepowerAntiMonster(float shield, float defense, 
			float missileDefense, int speed, int beamRange) { return 100 * stackLevel(); }
	@Override protected ShipDesign designRotP()	 {
		ShipDesignLab lab = empire().shipLab();
		
		// System.out.println();
		// System.out.print("design ");
		ShipDesign design = lab.newBlankDesign(Math.max(ShipDesign.MEDIUM,
								stackLevel(ShipDesign.LARGE, ShipDesign.HUGE)));
		design.mission	(ShipDesign.DESTROYER);

		// System.out.print("engine ");
		List<ShipEngine> engines = lab.engines();
		design.engine	(engines.get(stackLevel(0, engines.size()-1)));

		// System.out.print("computer ");
		List<ShipComputer> computers = lab.computers();
		design.computer	(computers.get(stackLevel(2, computers.size()-1)));

		// System.out.print("armor ");
		List<ShipArmor> armors = lab.armors();
		design.armor	(armors.get(stackLevel(2, armors.size()-1)));

		// System.out.print("shield ");
		List<ShipShield> shields = lab.shields();
		design.shield	(shields.get(stackLevel(2, shields.size()-1)));

		// System.out.print("ecm ");
		List<ShipECM> ecms = lab.ecms();
		design.ecm		(ecms.get(stackLevel(2, ecms.size()-1)));

		// System.out.print("maneuver ");
		List<ShipManeuver> maneuvers = lab.maneuvers();
		design.maneuver	(maneuvers.get(stackLevel(0, maneuvers.size()-1)));

		// System.out.print("weapon ");
		design.special(0, lab.specialBattleScanner());
		design.special(1, lab.specialSquidInk());
//		design.special(1, lab.getSpecial("Squid Ink", 0));
		
		// design.special(1, lab.specialTeleporter());
		// design.special(2, lab.specialCloak());
		// design.name(text(IMAGE_KEY));
		// lab.iconifyDesign(design);
		return design;
	}

}
