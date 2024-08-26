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
package rotp.model.combat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import rotp.model.ai.SpacePiratesCaptain;
import rotp.model.empires.Empire;
import rotp.model.galaxy.*;
import rotp.model.ships.*;
import rotp.model.tech.*;

// modnar: add Space Pirates random event
// modnar: Space Pirates combat stack, use CombatStackOrionGuardian.java as template
public class CombatStackSpacePirates extends CombatStack {
    public final List<ShipComponent> weapons = new ArrayList<>();
    public int[] weaponCount = new int[4];
    //public final ShipSpecial[] specials = new ShipSpecial[3];
	public final List<ShipSpecial> specials = new ArrayList<>(); // modnar: switch to List<ShipSpecial> for easier ShipBattleUI display
    public int[] roundsRemaining = new int[4]; // how many rounds you can fire (i.e. missiles)
    public int[] shotsRemaining = new int[4]; // how many uses (shots) left on the current turn
    public int[] baseTurnsToFire = new int[4];    // how many turns to wait before you can fire again
    public int[] wpnTurnsToFire = new int[4];    // how many turns to wait before you can fire again
    public ShipComponent selectedWeapon;
    public SpacePirates monster;
    public float speed;
    public float monsterLevel;
	
    public CombatStackSpacePirates(SpacePirates pirates, Float speed, Float level) {
		captain = new SpacePiratesCaptain(monster);
        image = image("SPACE_PIRATES");
        if (level == null)
        	monsterLevel = options().monstersLevel();
        else
        	monsterLevel = level;
        float wsLevel	= monsterLevel;
        float numLevel	= monsterLevel;
        float hitLevel	= monsterLevel;
		
		// modnar: adjust Space Pirate ship stack stats based on galaxy empire development
		// based on highest average tech level, difficulty, and galaxy size
		int totalStars = galaxy().numStarSystems();
		// log10 for slow growing function
		// for totalStars =  33   100   200   500   1000   5000
		//     stackScale ~ 0.5   1.0   1.3   1.7    2.0    2.7
		float stackScale = (float)(Math.max(0.1f, Math.log10(totalStars/10)));
		// difficulty multiplier = aiProductionModifier()
		// so hardest will be 2.0 while easiest will be 0.5
		stackScale *= options().aiProductionModifier();
		
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
		//maxTechLvl = (float)(Math.random() * 59); // testing
		//stackScale = (float)(Math.random() * 3); // testing
		
		// modnar: customize different number of Space Pirate ship stacks for different tech levels
		// also gradually scale number of weapons, attackLevel, and defenses within each design tier, 75% to 100%
		float weaponScale = 1.0f;
        streamProjectorHits(0); // BR:
		if (maxTechLvl <= 5) {
			weaponScale = wsLevel * (maxTechLvl + 15.0f)/20.0f;// gradually scale weapon count
			num = (int) (5 * numLevel);
			maxStackHits(18);
			hits(maxStackHits());
			maxMove = move = 1;
			maneuverability = 1;
			attackLevel = (int)Math.round(weaponScale*2);
			beamDefense = (int)Math.round(weaponScale*2);
			missileDefense = (int)Math.round(weaponScale*3);
			maxShield = shield = 1.0f;
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:0"), false)); // laser
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:0"), true)); // heavy laser
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:0"), false, 5, 4, 2.0f)); // nuclear missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:0"))); // nuclear bombs

			weaponCount[0] = (int)Math.round(weaponScale*3);
			weaponCount[1] = (int)Math.round(weaponScale*5);
			weaponCount[2] = (int)Math.round(weaponScale*3);
			weaponCount[3] = 2;
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 1;
			shotsRemaining[1] = 1;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		else if ((maxTechLvl > 5) && (maxTechLvl <= 10)) {
			weaponScale = wsLevel * (maxTechLvl + 10.0f)/20.0f;// gradually scale weapon count
			num = (int) Math.ceil(numLevel*stackScale*maxTechLvl);
			maxStackHits(18);
			hits(maxStackHits());
			maxMove = move = 2;
			maneuverability = 2;
			attackLevel = (int)Math.round(weaponScale*3);
			beamDefense = (int)Math.round(weaponScale*3);
			missileDefense = (int)Math.round(weaponScale*4);
			maxShield = shield = 2.0f;
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:1"), false)); // gatling laser
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:2"), false)); // neutron pellet gun
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:1"), false, 5, 5, 2.5f)); // hyper-v missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:1"))); // fusion bombs
			weaponCount[0] = (int)Math.round(weaponScale*3);
			weaponCount[1] = (int)Math.round(weaponScale*6);
			weaponCount[2] = (int)Math.round(weaponScale*3);
			weaponCount[3] = 2;
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 4;
			shotsRemaining[1] = 1;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		else if ((maxTechLvl > 10) && (maxTechLvl <= 15)) {
			weaponScale = wsLevel * (maxTechLvl + 5.0f)/20.0f;// gradually scale weapon count
			num = (int) Math.ceil(numLevel*stackScale*maxTechLvl);
			maxStackHits(27);
			hits(maxStackHits());
			maxMove = move = 2;
			maneuverability = 3;
			attackLevel = (int)Math.round(weaponScale*4);
			beamDefense = (int)Math.round(weaponScale*4);
			missileDefense = (int)Math.round(weaponScale*5);
			maxShield = shield = 3.0f;
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:3"), false)); // ion cannon
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:3"), true)); // heavy ion cannon
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:2"), false, 5, 5, 2.5f)); // hyper-x missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:1"))); // fusion bombs
			weaponCount[0] = (int)Math.round(weaponScale*4);
			weaponCount[1] = (int)Math.round(weaponScale*7);
			weaponCount[2] = (int)Math.round(weaponScale*4);
			weaponCount[3] = (int)Math.round(weaponScale*3);
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 1;
			shotsRemaining[1] = 1;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		else if ((maxTechLvl > 15) && (maxTechLvl <= 20)) {
			weaponScale = wsLevel * (maxTechLvl + 0.0f)/20.0f;// gradually scale weapon count
			num = (int) Math.ceil(numLevel*stackScale*maxTechLvl);
			maxStackHits(36);
			hits(maxStackHits());
			maxMove = move = 3;
			maneuverability = 4;
			attackLevel = (int)Math.round(weaponScale*5);
			beamDefense = (int)Math.round(weaponScale*6);
			missileDefense = (int)Math.round(weaponScale*8);
			maxShield = shield = 4.0f;
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			specials.add(new ShipSpecialInertial((TechShipInertial)tech("ShipInertial:0"))); // inertial stabilizer
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:5"), false)); // neutron blaster
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:5"), true)); // heavy neutron blaster
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:4"), false, 5, 6, 3.0f)); // merculite missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:2"))); // anti-matter bombs
			weaponCount[0] = (int)Math.round(weaponScale*4);
			weaponCount[1] = (int)Math.round(weaponScale*7);
			weaponCount[2] = (int)Math.round(weaponScale*4);
			weaponCount[3] = (int)Math.round(weaponScale*4);
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 1;
			shotsRemaining[1] = 1;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		else if ((maxTechLvl > 20) && (maxTechLvl <= 25)) {
			weaponScale = wsLevel * (maxTechLvl - 5.0f)/20.0f;// gradually scale weapon count
			num = (int) Math.ceil(numLevel*stackScale*maxTechLvl);
			maxStackHits(36);
			hits(maxStackHits());
			maxMove = move = 3;
			maneuverability = 4;
			attackLevel = (int)Math.round(weaponScale*6);
			beamDefense = (int)Math.round(weaponScale*7);
			missileDefense = (int)Math.round(weaponScale*10);
			maxShield = shield = 5.0f;
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			specials.add(new ShipSpecialInertial((TechShipInertial)tech("ShipInertial:0"))); // inertial stabilizer
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:7"), false)); // hard beam
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:8"), true)); // heavy fusion beam
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:5"), false, 5, 7, 3.5f)); // stinger missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:3"))); // omega-v bombs
			weaponCount[0] = (int)Math.round(weaponScale*5);
			weaponCount[1] = (int)Math.round(weaponScale*8);
			weaponCount[2] = (int)Math.round(weaponScale*5);
			weaponCount[3] = (int)Math.round(weaponScale*4);
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 1;
			shotsRemaining[1] = 1;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		else if ((maxTechLvl > 25) && (maxTechLvl <= 30)) {
			weaponScale = wsLevel * (maxTechLvl - 10.0f)/20.0f;// gradually scale weapon count
			num = (int) Math.ceil(numLevel*stackScale*maxTechLvl);
			maxStackHits(45);
			hits(maxStackHits());
			maxMove = move = 4;
			maneuverability = 5;
			attackLevel = (int)Math.round(weaponScale*7);
			beamDefense = (int)Math.round(weaponScale*8);
			missileDefense = (int)Math.round(weaponScale*12);
			maxShield = shield = 6.0f;
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			specials.add(new ShipSpecialInertial((TechShipInertial)tech("ShipInertial:0"))); // inertial stabilizer
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:10"), true)); // heavy phasor
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:11"), false)); // auto-blaster
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:6"), false, 5, 6, 3.0f)); // scatter pack-vii missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:3"))); // omega-v  bombs
			weaponCount[0] = (int)Math.round(weaponScale*6);
			weaponCount[1] = (int)Math.round(weaponScale*8);
			weaponCount[2] = (int)Math.round(weaponScale*5);
			weaponCount[3] = (int)Math.round(weaponScale*4);
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 1;
			shotsRemaining[1] = 3;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		else if ((maxTechLvl > 30) && (maxTechLvl <= 35)) {
			weaponScale = wsLevel * (maxTechLvl - 15.0f)/20.0f;// gradually scale weapon count
			num = (int) Math.ceil(numLevel*stackScale*maxTechLvl);
			maxStackHits(54);
			hits(maxStackHits());
			maxMove = move = 4;
			maneuverability = 6;
			attackLevel = (int)Math.round(weaponScale*8);
			beamDefense = (int)Math.round(weaponScale*9);
			missileDefense = (int)Math.round(weaponScale*14);
			maxShield = shield = 7.0f;
			beamRangeBonus = 3; // high energy focus
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			specials.add(new ShipSpecialInertial((TechShipInertial)tech("ShipInertial:0"))); // inertial stabilizer
			specials.add(new ShipSpecialBeamFocus((TechBeamFocus)tech("BeamFocus:0"))); // high energy focus
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:11"), false)); // auto-blaster
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:13"), false)); // gauss auto-cannon
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:7"), false, 5, 8, 4.0f)); // pulson missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:3"))); // omega-v bombs
			weaponCount[0] = (int)Math.round(weaponScale*7);
			weaponCount[1] = (int)Math.round(weaponScale*8);
			weaponCount[2] = (int)Math.round(weaponScale*6);
			weaponCount[3] = (int)Math.round(weaponScale*4);
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 3;
			shotsRemaining[1] = 4;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		else if ((maxTechLvl > 35) && (maxTechLvl <= 40)) {
			weaponScale = wsLevel * (maxTechLvl - 20.0f)/20.0f;// gradually scale weapon count
			num = (int) Math.ceil(numLevel*stackScale*maxTechLvl);
			maxStackHits(54);
			hits(maxStackHits());
			maxMove = move = 5;
			maneuverability = 7;
			attackLevel = (int)Math.round(weaponScale*9);
			beamDefense = (int)Math.round(weaponScale*10);
			missileDefense = (int)Math.round(weaponScale*15);
			maxShield = shield = 9.0f;
			beamRangeBonus = 3; // high energy focus
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			specials.add(new ShipSpecialInertial((TechShipInertial)tech("ShipInertial:0"))); // inertial stabilizer
			specials.add(new ShipSpecialBeamFocus((TechBeamFocus)tech("BeamFocus:0"))); // high energy focus
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:13"), false)); // Gauss auto-cannon
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:17"), false)); // disruptor
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:8"), false, 5, 9, 4.5f)); // Hercular missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:4"))); // neutronium bombs
			weaponCount[0] = (int)Math.round(weaponScale*8);
			weaponCount[1] = (int)Math.round(weaponScale*10);
			weaponCount[2] = (int)Math.round(weaponScale*8);
			weaponCount[3] = (int)Math.round(weaponScale*5);
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 4;
			shotsRemaining[1] = 1;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		else if ((maxTechLvl > 40) && (maxTechLvl <= 45)) {
			weaponScale = wsLevel * (maxTechLvl - 25.0f)/20.0f;// gradually scale weapon count
			num = (int) Math.ceil(numLevel*stackScale*maxTechLvl);
			maxStackHits(63);
			hits(maxStackHits());
			maxMove = move = 5;
			maneuverability = 8;
			attackLevel = (int)Math.round(weaponScale*10);
			beamDefense = (int)Math.round(weaponScale*11);
			missileDefense = (int)Math.round(weaponScale*16);
			maxShield = shield = 11.0f;
			beamRangeBonus = 3; // high energy focus
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			specials.add(new ShipSpecialInertial((TechShipInertial)tech("ShipInertial:0"))); // inertial stabilizer
			specials.add(new ShipSpecialBeamFocus((TechBeamFocus)tech("BeamFocus:0"))); // high energy focus
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:13"), false)); // gauss auto-cannon
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:17"), false)); // disruptor
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:9"), false, 5, 10, 5.0f)); // zeon missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:4"))); // neutronium bombs
			weaponCount[0] = (int)Math.round(weaponScale*10);
			weaponCount[1] = (int)Math.round(weaponScale*12);
			weaponCount[2] = (int)Math.round(weaponScale*10);
			weaponCount[3] = (int)Math.round(weaponScale*6);
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 4;
			shotsRemaining[1] = 1;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		else if ((maxTechLvl > 45) && (maxTechLvl <= 50)) {
			weaponScale = wsLevel * (maxTechLvl - 30.0f)/20.0f;// gradually scale weapon count
			num = (int) Math.ceil(numLevel*stackScale*maxTechLvl);
			maxStackHits(63);
			hits(maxStackHits());
			maxMove = move = 6;
			maneuverability = 8;
			attackLevel = (int)Math.round(weaponScale*11);
			beamDefense = (int)Math.round(weaponScale*13);
			missileDefense = (int)Math.round(weaponScale*18);
			maxShield = shield = 13.0f;
			beamRangeBonus = 3; // high energy focus
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			specials.add(new ShipSpecialInertial((TechShipInertial)tech("ShipInertial:1"))); // inertial nullifier
			specials.add(new ShipSpecialBeamFocus((TechBeamFocus)tech("BeamFocus:0"))); // high energy focus
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:20"), false)); // stellar converter
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:21"), false)); // mauler device
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:10"), false, 5, 7, 3.5f)); // scatter pack X missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:4"))); // neutronium bombs
			weaponCount[0] = (int)Math.round(weaponScale*12);
			weaponCount[1] = (int)Math.round(weaponScale*15);
			weaponCount[2] = (int)Math.round(weaponScale*12);
			weaponCount[3] = (int)Math.round(weaponScale*8);
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 4;
			shotsRemaining[1] = 1;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		else if ((maxTechLvl > 50) && (maxTechLvl <= 55)) {
			weaponScale = wsLevel * (maxTechLvl - 35.0f)/20.0f;// gradually scale weapon count
			num = (int) Math.ceil(numLevel*stackScale*maxTechLvl);
			maxStackHits(72);
			hits(maxStackHits());
			maxMove = move = 7;
			maneuverability = 9;
			attackLevel = (int)Math.round(weaponScale*12);
			beamDefense = (int)Math.round(weaponScale*14);
			missileDefense = (int)Math.round(weaponScale*20);
			maxShield = shield = 15.0f;
			beamRangeBonus = 3; // high energy focus
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			specials.add(new ShipSpecialInertial((TechShipInertial)tech("ShipInertial:1"))); // inertial nullifier
			specials.add(new ShipSpecialBeamFocus((TechBeamFocus)tech("BeamFocus:0"))); // high energy focus
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:20"), false)); // stellar converter
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:21"), false)); // mauler device
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:10"), false, 5, 7, 3.5f)); // scatter pack X missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:4"))); // neutronium bombs
			weaponCount[0] = (int)Math.round(weaponScale*15);
			weaponCount[1] = (int)Math.round(weaponScale*20);
			weaponCount[2] = (int)Math.round(weaponScale*15);
			weaponCount[3] = (int)Math.round(weaponScale*10);
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 4;
			shotsRemaining[1] = 1;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		else {
			num = (int) (numLevel * Math.ceil(stackScale*maxTechLvl));
			maxStackHits((int)(hitLevel * 9*Math.ceil(maxTechLvl/5)));
			hits(maxStackHits());
			maxMove = move = 7;
			maneuverability = 9;
			attackLevel = 12;
			beamDefense = 14;
			missileDefense = 22;
			maxShield = shield = 15.0f;
			beamRangeBonus = 3; // high energy focus
			specials.add(new ShipSpecialScanner((TechScanner)tech("Scanner:0"))); // battle scanner
			specials.add(new ShipSpecialInertial((TechShipInertial)tech("ShipInertial:1"))); // inertial nullifier
			specials.add(new ShipSpecialBeamFocus((TechBeamFocus)tech("BeamFocus:0"))); // high energy focus
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:20"), false)); // stellar converter
			weapons.add(new ShipWeaponBeam((TechShipWeapon) tech("ShipWeapon:21"), false)); // mauler device
			weapons.add(new ShipWeaponMissile((TechMissileWeapon) tech("MissileWeapon:10"), false, 5, 7, 3.5f)); // scatter pack X missiles
			weapons.add(new ShipWeaponBomb((TechBombWeapon) tech("BombWeapon:4"))); // neutronium bombs
			// scale weapon count when in Future Tech
			weaponCount[0] = (int)Math.ceil(maxTechLvl/3);
			weaponCount[1] = (int)Math.ceil(maxTechLvl/2);
			weaponCount[2] = (int)Math.ceil(maxTechLvl/3);
			weaponCount[3] = (int)Math.ceil(maxTechLvl/4);
			roundsRemaining[0] = 1;
			roundsRemaining[1] = 1;
			roundsRemaining[2] = 5;
			roundsRemaining[3] = 10;
			shotsRemaining[0] = 4;
			shotsRemaining[1] = 1;
			shotsRemaining[2] = 1;
			shotsRemaining[3] = 1;
		}
		
		baseTurnsToFire[0] = 1;
		baseTurnsToFire[1] = 1;
		baseTurnsToFire[2] = 1;
		baseTurnsToFire[3] = 1;
		wpnTurnsToFire[0] = 1;
		wpnTurnsToFire[1] = 1;
		wpnTurnsToFire[2] = 1;
		wpnTurnsToFire[3] = 1;
		
        if (weapons.size() > 0)
            selectedWeapon = weapons.get(0);
    }    
    @Override
    public String name()                { return concat(str(num), " ", text("SPACE_PIRATES")); } // modnar: combine num to get correct display on tactical combat screen
    @Override
    public boolean isMonster()          { return true; }
	@Override
	public boolean isNeutralShip()      { return true; } // modnar: add new type, for ship scan display
	@Override
    public boolean isArmed()            { return true; }
    @Override
    public boolean immuneToStasis()     { return false; } // modnar: not immune to stasis
    @Override
    public int numWeapons()               { return weapons.size(); }
    @Override
    public ShipComponent weapon(int i)    { return weapons.get(i); }
    @Override
    public void reloadWeapons()         {
        for (int i=0;i<shotsRemaining.length;i++) 
            shotsRemaining[i] = 1;
        for (ShipComponent c: weapons)
            c.reload(); 
    };
    @Override
    public int shotsRemaining(int i) { return shotsRemaining[i]; }
    @Override
    public boolean hostileTo(CombatStack st, StarSystem sys)  { return true; }
    @Override
    public boolean selectBestWeapon(CombatStack target) {
        if (target.destroyed())
            return false;
        if (currentWeaponCanAttack(target))
            return true;

        rotateToUsableWeapon(target);
        return currentWeaponCanAttack(target);
    }
    @Override
    public void endTurn() {
        super.endTurn();
        for (int i=0;i<shotsRemaining.length;i++) 
            wpnTurnsToFire[i] = shotsRemaining[i] == 0 ? baseTurnsToFire[i] : wpnTurnsToFire[i]-1;          
    }
    @Override
    public void rotateToUsableWeapon(CombatStack target) {
        if (selectedWeapon == null)
            return;
        int i = weapons.indexOf(selectedWeapon);
        int j = i;
        boolean looking = true;

        while (looking) {
            j++;
            if (j == weapons.size())
                j = 0;
            selectedWeapon = weapons.get(j);
            if ((j == i) || currentWeaponCanAttack(target))
                looking = false;
        }
    }
    @Override
    public boolean canAttack(CombatStack st) {
        if (st == null)
            return false;
        if (st.inStasis)
            return false;
        for (int i=0;i<weapons.size();i++) {
            if (shipComponentCanAttack(st, i))
                return true;
        }
        return false;
    }
    @Override
    public boolean currentWeaponCanAttack(CombatStack target) {
        if (selectedWeapon() == null)
            return false;

        int wpn = weapons.indexOf(selectedWeapon());

        return shipComponentCanAttack(target, wpn);
    }
    private boolean shipComponentCanAttack(CombatStack target, int index) {
        if (target == null)
            return false;

        if (roundsRemaining[index]< 1)
            return false;

        if (shotsRemaining[index] < 1)
            return false;
        
        if (wpnTurnsToFire[index] > 1)
            return false;

        if (target.inStasis || target.isMissile())
            return false;

        ShipComponent shipWeapon = weapons.get(index);

        if (!shipWeapon.isWeapon())
            return false;

        if (shipWeapon.isLimitedShotWeapon() && (roundsRemaining[index] < 1))
            return false;

        if (shipWeapon.groundAttacksOnly() && !target.isColony())
            return false;

        int minMove = movePointsTo(target);
        if (weaponRange(shipWeapon) < minMove)
            return false;

        return true;
    }
    @Override
    public int weaponNum(ShipComponent comp) {
        return weapons.indexOf(comp);
    }
    @Override
    public int weaponIndex() {
        return weapons.indexOf(selectedWeapon);
    }
    @Override
    public void fireWeapon(CombatStack targetStack) {
        fireWeapon(targetStack, weaponIndex());
    }
    @Override
    public void fireWeapon(CombatStack targetStack, int index)  { 
         if (targetStack == null)
            return;

        target = targetStack;
        target.damageSustained = 0;
        // only fire if we have shots remaining... this is a missile concern
        if ((roundsRemaining[index] > 0)) {
            selectedWeapon = weapons.get(index);
            // some weapons (beams) can fire multiple per round
            int shots = (int) selectedWeapon.attacksPerRound();
            int count = num*shots*weaponCount[index];
            if (selectedWeapon.isMissileWeapon()) {
                CombatStackMissile missile = new CombatStackMissile(this, (ShipWeaponMissileType) selectedWeapon, count);
                log(fullName(), " launching ", missile.fullName(), " at ", targetStack.fullName());
                mgr.addStackToCombat(missile);
            }
            else {
                log(fullName(), " firing ", str(count), " ", selectedWeapon.name(), " at ", targetStack.fullName());
                selectedWeapon.fireUpon(this, target, count, mgr);
            }
            if (target == null) 
                log("TARGET IS NULL AFTER BEING FIRED UPON!");
            shotsRemaining[index] = max(0, shotsRemaining[index]-1);
            if (selectedWeapon.isLimitedShotWeapon())
                roundsRemaining[index] = max(0, roundsRemaining[index]-1);
            if (target.damageSustained > 0)
                log("weapon damage: ", str(target.damageSustained));
        }
        rotateToUsableWeapon(targetStack);
        target.damageSustained = 0;
    }
    @Override
    public float initiativeRank() {
        return (maneuverability);
    }
    @Override
    public ShipComponent selectedWeapon() { return selectedWeapon; }
    public void drawAttack() { 
        return;
    }
    @Override public Color shieldBaseColor() { return Color.magenta; }
}
        