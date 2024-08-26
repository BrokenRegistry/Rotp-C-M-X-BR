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

import static rotp.model.ships.ShipDesign.maxSpecials;

import java.awt.Color;
import java.util.List;

import rotp.model.colony.Colony;
import rotp.model.combat.CombatStackMonster;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipArmor;
import rotp.model.ships.ShipComputer;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.ships.ShipEngine;
import rotp.model.ships.ShipShield;

// modnar: add Space Pirates random event
public class SpacePirates extends SpaceMonster {
	private static final long serialVersionUID = 1L;
    private static final Color shieldColor	= Color.magenta;
    private static final String imageKey	= "SPACE_PIRATES";
    private static final boolean isFusion	= true;

	public SpacePirates(Float speed, Float level) {
		super(imageKey, -5, speed, level);
	}
	@Override public void initCombat() {
		super.initCombat();
		addCombatStack(new CombatStackMonster(this, imageKey, stackLevel(), 0, isFusion, shieldColor));	   
//		addCombatStack(new CombatStackSpacePirates(this, travelSpeed(), stackLevel()));	   
	}
	// modnar: pirates pillage colonies rather than destroy
	// half population, remove all factories, produce max waste
	// ?? add to combat stack numbers from factories pillaged ??
	@Override public void degradePlanet(StarSystem sys) { // was pillageColony
		Colony col = sys.colony();
		if (col != null) {
			sys.empire().lastAttacker(this);
			float prevPop = col.population();
			col.setPopulation(prevPop*0.5f); // half population
			col.industry().factories(0.0f); // remove all factories
			float maxWaste = sys.planet().maxWaste(); // produce max waste
			sys.planet().addWaste(maxWaste);
			sys.planet().removeExcessWaste();
		}		
	}
	@Override protected void initDesigns()		{
		super.initDesigns();
		float numLevel	 = stackLevel();
		float maxTechLvl = maxTechLvl();
		float stackScale = stackScale();
		int pirateLevel	 = pirateLevel(maxTechLvl);
		int num = 1;
		if (pirateLevel == 0)
			num = (int) (5 * numLevel);
		else
			num = (int) Math.ceil(numLevel * stackScale * maxTechLvl);
		num(0, num);
	}
	@Override protected ShipDesign designRotP() {
		ShipDesignLab lab = empire().shipLab();
		float maxTechLvl  = maxTechLvl();
		float weaponScale = 1.0f;
		int pirateLevel	  = pirateLevel(maxTechLvl);

		List<ShipEngine>	engines		= lab.engines();
		List<ShipComputer>	computers	= lab.computers();
		List<ShipArmor>		armors		= lab.armors();
		List<ShipShield>	shields		= lab.shields();

		ShipDesign design;
		int attackLevel = 1;
		int beamDefense = 1;
		int missileDefense = 1;

		switch (pirateLevel) {
			case 0:
				weaponScale		= stackLevel() * (maxTechLvl + 15.0f)/20.0f;// gradually scale weapon count
				attackLevel		= round(weaponScale * 2);
				beamDefense		= round(weaponScale * 2);
				missileDefense	= round(weaponScale * 3);

				design = lab.newBlankDesign(maxSpecials, 18);
				design.engine	(engines.get(0));
				design.maneuver	(lab.maneuver(1));
				design.special	(0, lab.specialBattleScanner());
				design.weapon	(0, lab.laserBeam(LIGHT),	 round(weaponScale*3));
				design.weapon	(1, lab.laserBeam(HEAVY),	 round(weaponScale*5));
				design.weapon	(2, lab.nuclearMissiles(x5), round(weaponScale*3));
				design.weapon	(3, lab.nuclearBomb(), 		 2);
				break;

			case 1:
				weaponScale		= stackLevel() * (maxTechLvl + 10.0f)/20.0f;// gradually scale weapon count
				attackLevel		= round(weaponScale * 3);
				beamDefense		= round(weaponScale * 3);
				missileDefense	= round(weaponScale * 4);

				design = lab.newBlankDesign(maxSpecials, 18);
				design.engine	(engines.get(1));
				design.maneuver	(lab.maneuver(2));

				design.special	(0, lab.specialBattleScanner());
				design.weapon	(0, lab.gatlingLaser(),		round(weaponScale * 3));
				design.weapon	(1, lab.netronPelletGun(),	round(weaponScale * 6));
				design.weapon	(2, lab.hyperVRockets(x5),	round(weaponScale * 3));
				design.weapon	(3, lab.fusionBomb(), 		2);
				break;

			case 2:
				weaponScale		= stackLevel() * (maxTechLvl + 10.0f)/20.0f;// gradually scale weapon count
				attackLevel		= round(weaponScale * 4);
				beamDefense		= round(weaponScale * 4);
				missileDefense	= round(weaponScale * 5);

				design = lab.newBlankDesign(maxSpecials, 27);
				design.engine	(engines.get(1));
				design.maneuver	(lab.maneuver(3));

				design.special	(0, lab.specialBattleScanner());
				design.weapon	(0, lab.ionCannon(LIGHT),	round(weaponScale * 4));
				design.weapon	(1, lab.ionCannon(HEAVY),	round(weaponScale * 7));
				design.weapon	(2, lab.hyperXRockets(x5),	round(weaponScale * 4));
				design.weapon	(3, lab.fusionBomb(), 		round(weaponScale * 3));
				break;

			case 3:
				weaponScale		= stackLevel() * (maxTechLvl + 10.0f)/20.0f;// gradually scale weapon count
				attackLevel		= round(weaponScale * 5);
				beamDefense		= round(weaponScale * 6);
				missileDefense	= round(weaponScale * 8);

				design = lab.newBlankDesign(maxSpecials, 36);
				design.engine	(engines.get(2));
				design.maneuver	(lab.maneuver(3));

				design.special	(0, lab.specialBattleScanner());
				design.special	(1, lab.specialInertialStabilizer());
				design.weapon	(0, lab.neutronBlaster(LIGHT),	round(weaponScale * 4));
				design.weapon	(1, lab.neutronBlaster(HEAVY),	round(weaponScale * 7));
				design.weapon	(2, lab.merculiteMissiles(x5),	round(weaponScale * 4));
				design.weapon	(3, lab.antiMatterBomb(), 		round(weaponScale * 4));
				break;

			case 4:
				weaponScale		= stackLevel() * (maxTechLvl + 10.0f)/20.0f;// gradually scale weapon count
				attackLevel		= round(weaponScale * 6);
				beamDefense		= round(weaponScale * 7);
				missileDefense	= round(weaponScale * 10);

				design = lab.newBlankDesign(maxSpecials, 36);
				design.engine	(engines.get(2));
				design.maneuver	(lab.maneuver(4));

				design.special	(0, lab.specialBattleScanner());
				design.special	(1, lab.specialInertialStabilizer());
				design.weapon	(0, lab.hardBeam(),				round(weaponScale * 5));
				design.weapon	(1, lab.fusionBeam(HEAVY),		round(weaponScale * 8));
				design.weapon	(2, lab.stingerMissiles(x5),	round(weaponScale * 5));
				design.weapon	(3, lab.omegaVBomb(), 			round(weaponScale * 4));
				break;

			case 5:
				weaponScale		= stackLevel() * (maxTechLvl + 10.0f)/20.0f;// gradually scale weapon count
				attackLevel		= round(weaponScale * 7);
				beamDefense		= round(weaponScale * 8);
				missileDefense	= round(weaponScale * 12);

				design = lab.newBlankDesign(maxSpecials, 45);
				design.engine	(engines.get(3));
				design.maneuver	(lab.maneuver(5));

				design.special	(0, lab.specialBattleScanner());
				design.special	(1, lab.specialInertialStabilizer());
				design.weapon	(0, lab.phasorBeam(HEAVY),			round(weaponScale * 6));
				design.weapon	(1, lab.autoBlaster(),				round(weaponScale * 8));
				design.weapon	(2, lab.scatterPackVIIMissiles(x5),	round(weaponScale * 5));
				design.weapon	(3, lab.omegaVBomb(), 				round(weaponScale * 4));
				break;

			case 6:
				weaponScale		= stackLevel() * (maxTechLvl + 10.0f)/20.0f;// gradually scale weapon count
				attackLevel		= round(weaponScale * 8);
				beamDefense		= round(weaponScale * 9);
				missileDefense	= round(weaponScale * 14);

				design = lab.newBlankDesign(maxSpecials, 54);
				design.engine	(engines.get(3));
				design.maneuver	(lab.maneuver(6));

				design.special	(0, lab.specialBattleScanner());
				design.special	(1, lab.specialInertialStabilizer());
				design.special	(2, lab.specialHighEnergyFocus());
				design.weapon	(0, lab.autoBlaster(),		round(weaponScale * 7));
				design.weapon	(1, lab.gaussAutoCannon(),	round(weaponScale * 8));
				design.weapon	(2, lab.pulsonMissiles(x5),	round(weaponScale * 6));
				design.weapon	(3, lab.omegaVBomb(), 		round(weaponScale * 4));
				break;

			case 7:
				weaponScale		= stackLevel() * (maxTechLvl + 10.0f)/20.0f;// gradually scale weapon count
				attackLevel		= round(weaponScale * 9);
				beamDefense		= round(weaponScale * 10);
				missileDefense	= round(weaponScale * 15);

				design = lab.newBlankDesign(maxSpecials, 54);
				design.engine	(engines.get(4));
				design.maneuver	(lab.maneuver(7));

				design.special	(0, lab.specialBattleScanner());
				design.special	(1, lab.specialInertialStabilizer());
				design.special	(2, lab.specialHighEnergyFocus());
				design.weapon	(0, lab.gaussAutoCannon(),		round(weaponScale * 8));
				design.weapon	(1, lab.disruptor(),			round(weaponScale * 10));
				design.weapon	(2, lab.hercularMissiles(x5),	round(weaponScale * 8));
				design.weapon	(3, lab.neutroniumBomb(), 		round(weaponScale * 5));
				break;

			case 8:
				weaponScale		= stackLevel() * (maxTechLvl + 10.0f)/20.0f;// gradually scale weapon count
				attackLevel		= round(weaponScale * 10);
				beamDefense		= round(weaponScale * 11);
				missileDefense	= round(weaponScale * 16);

				design = lab.newBlankDesign(maxSpecials, 63);
				design.engine	(engines.get(4));
				design.maneuver	(lab.maneuver(8));

				design.special	(0, lab.specialBattleScanner());
				design.special	(1, lab.specialInertialStabilizer());
				design.special	(2, lab.specialHighEnergyFocus());
				design.weapon	(0, lab.gaussAutoCannon(),	round(weaponScale * 10));
				design.weapon	(1, lab.disruptor(),		round(weaponScale * 12));
				design.weapon	(2, lab.zeonMissiles(x5),	round(weaponScale * 10));
				design.weapon	(3, lab.neutroniumBomb(), 	round(weaponScale * 6));
				break;

			case 9:
				weaponScale		= stackLevel() * (maxTechLvl + 10.0f)/20.0f;// gradually scale weapon count
				attackLevel		= round(weaponScale * 11);
				beamDefense		= round(weaponScale * 13);
				missileDefense	= round(weaponScale * 18);

				design = lab.newBlankDesign(maxSpecials, 63);
				design.engine	(engines.get(5));
				design.maneuver	(lab.maneuver(8));

				design.special	(0, lab.specialBattleScanner());
				design.special	(1, lab.specialInertialNullifier());
				design.special	(2, lab.specialHighEnergyFocus());
				design.weapon	(0, lab.stellarConverter(),		  round(weaponScale * 12));
				design.weapon	(1, lab.maulerDevice(),			  round(weaponScale * 15));
				design.weapon	(2, lab.scatterPackXMissiles(x5), round(weaponScale * 12));
				design.weapon	(3, lab.neutroniumBomb(), 		  round(weaponScale * 8));
				break;

			case 10:
				weaponScale		= stackLevel() * (maxTechLvl + 10.0f)/20.0f;// gradually scale weapon count
				attackLevel		= 12;
				beamDefense		= 14;
				missileDefense	= 22;

				design = lab.newBlankDesign(maxSpecials, 72);
				design.engine	(engines.get(6));
				design.maneuver	(lab.maneuver(9));

				design.special	(0, lab.specialBattleScanner());
				design.special	(1, lab.specialInertialNullifier());
				design.special	(2, lab.specialHighEnergyFocus());
				design.weapon	(0, lab.stellarConverter(),		  round(weaponScale * 15));
				design.weapon	(1, lab.maulerDevice(),			  round(weaponScale * 20));
				design.weapon	(2, lab.scatterPackXMissiles(x5), round(weaponScale * 15));
				design.weapon	(3, lab.neutroniumBomb(), 		  round(weaponScale * 10));
				break;

			default:
				attackLevel		= round(weaponScale * 12);
				beamDefense		= round(weaponScale * 14);
				missileDefense	= round(weaponScale * 20);

				int hp = (int)(stackLevel() * 9 * Math.ceil(maxTechLvl/5));
				design = lab.newBlankDesign(maxSpecials, hp);
				design.engine	(engines.get(6));
				design.maneuver	(lab.maneuver(9));

				design.special	(0, lab.specialBattleScanner());
				design.special	(1, lab.specialInertialNullifier());
				design.special	(2, lab.specialHighEnergyFocus());
				design.weapon	(0, lab.stellarConverter(),		  ceil(maxTechLvl/3));
				design.weapon	(1, lab.maulerDevice(),			  ceil(maxTechLvl/2));
				design.weapon	(2, lab.scatterPackXMissiles(x5), ceil(maxTechLvl/3));
				design.weapon	(3, lab.neutroniumBomb(), 		  ceil(maxTechLvl/4));
		}
		
		design.mission(ShipDesign.DESTROYER);
		design.computer(computers.get(bounds(0, attackLevel-1, computers.size()-1)));
		design.monsterAttackLevel(attackLevel);
		design.monsterBeamDefense(beamDefense);
		design.monsterEcmDefense(missileDefense);

		design.armor(armors.get(0));

		int shieldId = bounds(0, pirateLevel+1, shields.size()-1);
		design.shield(shields.get(shieldId));

		return design;
	}
	private float maxTechLvl()	{
		// find highest average tech level among all active empires
		float maxTechLvl = 1.0f;
		float empTechLvl = 1.0f;
		for (Empire e: galaxy().activeEmpires()) {
			empTechLvl = e.tech().avgTechLevel();
			if (empTechLvl > maxTechLvl)
				maxTechLvl = empTechLvl;
		}
		// reduce maxTechLvl to fine-tune Space Pirate strength
		maxTechLvl = (float)Math.max(1.0f, maxTechLvl - 2.0f);
		return maxTechLvl;
	}
	private float stackScale()	{
		// modnar: adjust Space Pirate ship stack stats based on galaxy empire development
		// based on highest average tech level, difficulty, and galaxy size
		int totalStars = galaxy().numStarSystems();
		// log10 for slow growing function
		// for totalStars =  33   100   200   500   1000   5000
		//	 stackScale ~ 0.5   1.0   1.3   1.7	2.0	2.7
		float stackScale = (float)(Math.max(0.1f, Math.log10(totalStars/10)));
		// difficulty multiplier = aiProductionModifier()
		// so hardest will be 2.0 while easiest will be 0.5
		stackScale *= options().aiProductionModifier();
		return stackScale;
	}
	private int pirateLevel(float maxTechLvl)	{ return (int) ((maxTechLvl-1)/5); }
}
