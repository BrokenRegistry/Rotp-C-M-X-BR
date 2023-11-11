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

import java.util.List;

import rotp.model.colony.Colony;
import rotp.model.empires.Empire;
import rotp.model.galaxy.SpaceAmoeba;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.model.game.IGameOptions;
import rotp.ui.notifications.GNNNotification;
import rotp.ui.util.ParamInteger;

public class RandomEventSpaceAmoeba extends AbstractRandomEvent {
	private static final long serialVersionUID = 1L;
	private static final String NEXT_ALLOWED_TURN = "AMOEBA_NEXT_ALLOWED_TURN";
	public static final String TRIGGER_TECH		= "Cloning:1";
	public static final String TRIGGER_GNN_KEY	= "EVENT_SPACE_AMOEBA_TRIG";
	public static final String GNN_EVENT		= "GNN_Event_Amoeba";
	public static Empire triggerEmpire;
	private int empId;
	private int sysId;
	private int turnCount = 0;
	private SpaceAmoeba monster;
	
	@Override public SpaceMonster monster()	  {
		if (monster == null)
			initMonster();
		return monster;
	}
	@Override public boolean techDiscovered() { return triggerEmpire != null; }
	@Override ParamInteger delayTurn()		  { return IGameOptions.amoebaDelayTurn; }
	@Override ParamInteger returnTurn()		  { return IGameOptions.amoebaReturnTurn; }
	@Override public String statusMessage()	  { return text("SYSTEMS_STATUS_SPACE_AMOEBA"); }
	@Override public String systemKey()		  { return "MAIN_PLANET_EVENT_AMOEBA"; }
	@Override public boolean goodEvent()	  { return false; }
	@Override public boolean monsterEvent()	  { return true; }
	@Override int nextAllowedTurn() { // for backward compatibility
		return (Integer) galaxy().dynamicOptions().getInteger(NEXT_ALLOWED_TURN, -1);
	}
	@Override
	public String notificationText()	{
		String s1 = text("EVENT_SPACE_AMOEBA");
		Empire emp = galaxy().empire(empId);
		s1 = s1.replace("[system]", emp.sv.name(sysId));
		s1 = emp.replaceTokens(s1, "victim");
		return s1;
	}
	@Override
	public void trigger(Empire emp) {
		if (emp != null)
			log("Starting Amoeba event against: "+emp.raceName());
		//System.out.println("Starting Amoeba event against: "+emp.raceName());
		if (emp == null || emp.extinct()) {
			empId = emp.id;
			sysId = emp.homeSysId(); // Former home of extinct empire
		}
		else {
			List<StarSystem> allSystems = emp.allColonizedSystems();
			StarSystem targetSystem = random(allSystems);
			targetSystem.eventKey(systemKey());

			empId = emp.id;
			sysId = targetSystem.id;
		}
		turnCount = 3;
		galaxy().events().addActiveEvent(this);
	}
	@Override
	public void nextTurn() {
		if (isEventDisabled()) {
			terminateEvent(this);
			return;
		}
		if (monster == null)
			initMonster();

		if (turnCount == 3) 
			approachSystem();	 
		else if (turnCount == 0) 
			enterSystem();
		turnCount--;
	}
	private boolean nextSystemAllowed() { // BR: To allow disappearance
		int maxSystem = options().selectedAmoebaMaxSystems();
		return maxSystem == 0 || maxSystem > monster.vistedSystemsCount();
	}
	private void initMonster() {
		monster = new SpaceAmoeba();
		StarSystem targetSystem = galaxy().system(sysId);
		double alpha = random()*Math.PI*2;
		float speed	= monster.travelSpeed();
		double dist	= 2.9 * speed;
		double dx	= dist*Math.cos(alpha);
		double dy	= dist*Math.sin(alpha);
		float x		= (float) (targetSystem.x() + dx);
		float y		= (float) (targetSystem.y() + dy);
		monster.setXY(x, y);
		monster.destSysId(sysId);
		monster.launch(x, y);
	}
	private void enterSystem() {
//		System.out.println("Amoeba enter system");
		monster.visitSystem(sysId);
		monster.initCombat();
		StarSystem targetSystem = galaxy().system(sysId);
		targetSystem.clearEvent();
		Colony col = targetSystem.colony();
		if (!targetSystem.orbitingFleets().isEmpty())
			startCombat();
		else if ((col != null) && col.defense().isArmed())
			startCombat();
		
		if (monster.alive()) {
			degradePlanet(targetSystem);
			if (nextSystemAllowed())
				moveToNextSystem();
			else
				monsterVanished();
		}
		else 
			amoebaDestroyed();		 
	}
	private void startCombat() {
		StarSystem targetSystem = galaxy().system(sysId);
		galaxy().shipCombat().battle(targetSystem, monster);
	}
	private void approachSystem() {
		StarSystem targetSystem = galaxy().system(sysId);
		targetSystem.eventKey(systemKey());
		Empire pl = player();
		if (targetSystem.isColonized()) { 
			if (pl.knowsOf(targetSystem.empire()) || !pl.sv.name(sysId).isEmpty())
				GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_AMOEBA", targetSystem.empire(), null), GNN_EVENT);
		}
		else if (!pl.sv.name(sysId).isEmpty())
			GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_AMOEBA_1", null, null), GNN_EVENT);   
	}
	private void degradePlanet(StarSystem targetSystem) {
		Empire emp = targetSystem.empire();
		// colony may have already been destroyed in combat
		if (targetSystem.isColonized() || targetSystem.abandoned())
			monster.degradePlanet(targetSystem);
		
		if (emp == null)
			return;
		Empire pl = player();
		if (pl.knowsOf(emp) || !pl.sv.name(sysId).isEmpty())
			GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_AMOEBA_2", emp, null), GNN_EVENT);
	}
	private void amoebaDestroyed() {
		//galaxy().events().removeActiveEvent(this);
		terminateEvent(this);
		monster.plunder();
		Empire emp = monster.lastAttacker();
		String notifKey = "EVENT_SPACE_AMOEBA_3";
		Integer saleAmount = null;
		if (options().monstersGiveLoot()) {
			notifKey = "EVENT_SPACE_AMOEBA_PLUNDER";
			saleAmount = galaxy().currentTurn();
			// Studying amoeba remains help completing the current research
			if (emp.tech().planetology().completeResearch())
				saleAmount *= 10;
			else
				saleAmount *= 25; // if no research then more gold
			// Selling the amoeba flesh gives reserve BC, scaling with turn number
			emp.addToTreasury(saleAmount);
		}
		if (player().knowsOf(empId)|| !player().sv.name(sysId).isEmpty())
		   	GNNNotification.notifyRandomEvent(notificationText(notifKey, emp, saleAmount), GNN_EVENT);
		monster = null;
	}
	private void monsterVanished() { // BR: To allow disappearance
		terminateEvent(this);
		if (player().knowsOf(galaxy().empire(empId)) || !player().sv.name(sysId).isEmpty())
			GNNNotification.notifyRandomEvent(notificationText("EVENT_SPACE_AMOEBA_4", monster.lastAttacker(), null), GNN_EVENT);
		monster = null;
	}
	private void moveToNextSystem() {
		monster.sysId(sysId); // be sure the monster is a t the planet
		StarSystem targetSystem = galaxy().system(sysId);
		// next system is one of the 10 nearest systems
		// more likely to go to new system (25%) than visited system (5%)
		int[] near = targetSystem.nearbySystems();
		boolean stopLooking = false;
		
		int nextSysId = -1;
		int loops = 0;
		if (near.length > 0) {
			while (!stopLooking) {
				loops++;
				for (int i=0;i<near.length;i++) {
					float chance = monster.vistedSystems().contains(near[i]) ? 0.05f : 0.25f;
					if (random() < chance) {
						nextSysId = near[i];
						stopLooking = true;
						break;
					}
				}
				if (loops > 10) 
					stopLooking = true;
			}
		}
		
		if (nextSysId < 0) {
			log("ERR: Could not find next system. Space Amoeba removed.");
			//System.out.println("ERR: Could not find next system. Space Amoeba removed.");
			// galaxy().events().removeActiveEvent(this);
			terminateEvent(this);
			return;
		}
		log("Space Amoeba moving to system: "+nextSysId);

		sysId = nextSysId;	
		monster.destSysId(sysId);
		monster.launch();
		turnCount = monster.travelTurnsRemaining();
		if (turnCount <= 3)
			approachSystem();	 
	}
	private String notificationText(String key, Empire emp, Integer amount)	{
		String s1 = text(key);
		if (emp != null) {
			s1 = s1.replace("[system]", emp.sv.name(sysId));
			s1 = emp.replaceTokens(s1, "victim");
		}
		else 
			s1 = s1.replace("[system]", player().sv.name(sysId));
		if (amount != null)
			s1 = s1.replace("[amt]", amount.toString());
		return s1;
	}
}
