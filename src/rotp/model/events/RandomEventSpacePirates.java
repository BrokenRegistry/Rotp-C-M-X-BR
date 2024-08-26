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
package rotp.model.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rotp.model.empires.Empire;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.SpacePirates;
import rotp.model.galaxy.StarSystem;
import rotp.model.game.IGameOptions;
import rotp.model.tech.TechCategory;
import rotp.ui.util.ParamInteger;

// modnar: add Space Pirates random event
public class RandomEventSpacePirates extends RandomEventMonsters {
	private static final long serialVersionUID = 1L;
	// Static parameters for Tech Triggered Events
	public static final String TRIGGER_TECH		= "EngineWarp:8";
	public static final String TRIGGER_GNN_KEY	= "EVENT_SPACE_PIRATES_TRIG";
	public static final String GNN_EVENT		= "GNN_Event_Pirates";
	public static Empire triggerEmpire;
	private int empId; // Not to be set: kept for backward compatibility
	private int sysId; // Not to be set: kept for backward compatibility
	private int turnCount; // Not to be set: kept for backward compatibility
	
	@Override protected SpaceMonster newMonster(Float speed, Float level) {
		return new SpacePirates(speed, level);
	}
	@Override public boolean techDiscovered()	{ return triggerEmpire != null; }
	@Override protected String name()			{ return "PIRATES"; }
	@Override ParamInteger delayTurn()			{ return IGameOptions.piratesDelayTurn; }
	@Override ParamInteger returnTurn()			{ return IGameOptions.piratesReturnTurn; }
	@Override protected Integer lootMonster(boolean lootMode)	{
		Empire heroEmp = monster.lastAttacker();
		int spoilsBC;
		int lootBC = bcLootAmount();
		if (lootMode) {
			// Studying Damaged Pirate ships help completing two current research
			List<TechCategory> catList = new ArrayList<>();
			catList.add(heroEmp.tech().weapon());
			catList.add(heroEmp.tech().construction());
			catList.add(heroEmp.tech().computer());
			catList.add(heroEmp.tech().forceField());
			Collections.shuffle(catList);
			
			float rProb	= researchLootProbability();
			int rBC		= researchLootAmount();
			boolean completeAllowed = (rProb >= 1) && !repeatable();
			int maxTech = 1;
			if (random() < rProb)
				maxTech = 2;
			int techLearned = 0;
			for (TechCategory cat:catList) {
				if (completeAllowed)
				{
					if (cat.completeResearch())
					{
						techLearned++;
						completeAllowed = false;
					}
				}
				else
				{
					if (cat.contributeToResearch(rBC))
						techLearned++;
				}
				if (techLearned >= maxTech)
					break;
			}
			// destroying the space pirates gives reserve BC, scaling with turn number
			spoilsBC = lootBC * (1+maxTech-techLearned);
		}
		else {
			// destroying the space pirates gives reserve BC, scaling with turn number
			spoilsBC = galaxy().currentTurn() * 25;
		}
		return spoilsBC;
	}
	@Override protected int getNextSystem()		{
		StarSystem targetSystem = galaxy().system(targetSysId);
		// next system is one of the 10 nearest systems
		// more likely to go to new system (25%) than visited system (5%)
		// more likely to go to colony with a lot of factories (factories/2000 = additional chance)
		// increase chance with more loops (essentially try to force a choice before loops>10)
		int[] near = targetSystem.nearbySystems();
		boolean stopLooking = false;		
		int nextSysId = -1;
		int loops = 0;
		if (near.length > 0) {
			while (!stopLooking) {
				loops++;
				for (int i=0;i<near.length;i++) {
					if (galaxy().system(near[i]).isColonized()) { // check for colony
						float chance = monster.vistedSystems().contains(near[i]) ? 0.05f : 0.25f;
						// more likely to go to colony with a lot of factories
						chance += galaxy().system(near[i]).colony().industry().factories()/2000.0f;
						// increase chance with more loops
						chance += (float)(loops/20);
						if (random() < chance) {
							nextSysId = near[i];
							stopLooking = true;
							break;
						}
					}
				}
				if (loops > 10)
					stopLooking = true;
			}
		}
		return nextSysId;
	}
	// Don't use! For backward compatibility only, when a monster was already launched
	@Override protected int oldEmpId()			{ return empId; }
	@Override protected int oldSysId()			{ return sysId; }
	@Override protected int oldTurnCount()		{ return turnCount; }
}
