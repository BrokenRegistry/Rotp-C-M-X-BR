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
package rotp.model.events;

import java.util.ArrayList;
import java.util.List;

import rotp.model.colony.Colony;
import rotp.model.colony.ColonyResearchProject;
import rotp.model.empires.Empire;
import rotp.model.galaxy.StarSystem;
import rotp.model.game.IGameOptions;
import rotp.ui.notifications.GNNNotification;
import rotp.ui.util.ParamInteger;

public class RandomEventSupernova extends AbstractRandomEvent implements ColonyResearchProject {
    private static final long serialVersionUID = 1L;
    private int empId;
    private int sysId;
    private float researchNeeded = 0;
    private int turnsNeeded = 0;
    private int turnCount = 0;
    private float researchRemaining = 0;
    @Override public String statusMessage()	{ return text("SYSTEMS_STATUS_SUPERNOVA",str(turnsNeeded-turnCount), str((int)Math.ceil(researchRemaining))); }
    @Override public String systemKey()		{ return "MAIN_PLANET_EVENT_SUPERNOVA"; }
    @Override ParamInteger delayTurn()		{ return IGameOptions.supernovaDelayTurn; }
    @Override ParamInteger returnTurn()		{ return IGameOptions.supernovaReturnTurn; }
    @Override public boolean goodEvent()	{ return false; }
    @Override
    public String notificationText()    {
        String s1 = text("EVENT_SUPERNOVA");
        if (empId > Empire.NULL_ID )
            s1 = s1.replace("[system]", galaxy().empire(empId).sv.name(sysId));
        s1 = s1.replace("[years]", str((int)Math.ceil(1+turnsNeeded-turnCount)));
        if (empId > Empire.NULL_ID )
        	s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    @Override
    public String projectKey()          { return text("MAIN_COLONY_SPENDING_SUPERNOVA"); }
    @Override
    public float remainingResearchBC()   { return researchRemaining; }
    @Override
    public void addResearchBC(float amt) {
        researchRemaining -= amt;
        if (researchRemaining <= 0)
            solveSupernova();
    }
    @Override
    public void trigger(Empire emp) {
    	if (emp == null || emp.extinct())
    		return;

        // find a random colony that has at least 30 population that does not already have a research project, not a homeworld
        // allowing these combinations would complicate a lot of code in other places, so not allowed by fiat
        List<StarSystem> systems = new ArrayList<>();
        for (StarSystem sys : emp.allColonizedSystems()) {
            Colony col = sys.colony();
            if ((col.population() >= 30) && !col.research().hasProject() && !col.isCapital())
                systems.add(sys);
        }
        if (systems.isEmpty())
            return;

        turnCount = 0;
        turnsNeeded = roll(5,15);

        StarSystem targetSystem = random(systems);
        empId = emp.id;
        sysId = targetSystem.id;

        targetSystem.eventKey(systemKey());
        researchNeeded = turnsNeeded * targetSystem.colony().totalProductionIncome();
        researchRemaining = researchNeeded;
        if (player().knowsOf(empId)
        && !player().sv.name(sysId).isEmpty())
            GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Supernova");

        affectColony();
        galaxy().events().addActiveEvent(this);
    }
    @Override
    public void nextTurn() {
        affectColony();
        turnCount++;

        if ((turnCount % 5 == 0) && (player().id == empId))
            GNNNotification.notifyRandomEvent(continuingText(), "GNN_Event_Supernova");
    }
    private void affectColony() {
        StarSystem sys = galaxy().system(sysId);
        // reduce population by 5-10%
        Colony targetColony = sys.colony();
        
        // research ends if colony is destroyed
        if (targetColony != null)
            targetColony.research().project(this);
        
        if (turnCount == turnsNeeded)
            goSupernova();
        
        // if colony changed hands instead
        // transfer project to  new empire
        Empire sysEmp = sys.empire();
        int sysEmpId = id(sysEmp);
        if (sysEmpId != empId) {
            empId = sysEmpId;
            researchRemaining = researchNeeded;
            if (sysEmp == player())
                GNNNotification.notifyRandomEvent(notificationText(), "GNN_Event_Supernova");
        }
    }
    private String continuingText() {
        String s1 = text("EVENT_SUPERNOVA_2");
        s1 = s1.replace("[system]", player().sv.name(sysId));
        s1 = s1.replace("[amt]", str((int)Math.ceil(researchRemaining)));
        s1 = s1.replace("[years]", str((int)Math.ceil(1+turnsNeeded-turnCount)));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    private String goodEndText() {
        String s1 = text("EVENT_SUPERNOVA_3");
        s1 = s1.replace("[system]", player().sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    private String badEndText() {
        String s1 = text("EVENT_SUPERNOVA_4");
        s1 = s1.replace("[system]", player().sv.name(sysId));
        s1 = galaxy().empire(empId).replaceTokens(s1, "target");
        return s1;
    }
    private void solveSupernova() {
        StarSystem targetSystem = galaxy().system(sysId);
        terminateEvent(this);
        targetSystem.clearEvent();
        Colony col = targetSystem.colony();
        
        session().removePendingNotification("GNN_Event_Supernova");
        // possible colony is destroyed before supernova
        if (col != null) {
            col.research().endProject();
            if (player().knowsOf(empId)
            && !player().sv.name(sysId).isEmpty())
                GNNNotification.notifyRandomEvent(goodEndText(), "GNN_Event_Supernova");
        }
    }
    private void goSupernova() {
        StarSystem targetSystem = galaxy().system(sysId);
        terminateEvent(this);
        targetSystem.clearEvent();
        targetSystem.planet().baseSize(roll(11,20)); // reset size first... irradiate will reset pop
        targetSystem.planet().irradiateEnvironment(0);
        targetSystem.abandoned(false);
        targetSystem.addEvent(new SystemRandomEvent("SYSEVENT_SUPERNOVA"));

        Colony col = targetSystem.colony();
        
        // possible colony is destroyed before supernova
        if (col != null) {
            col.research().endProject();
            if (player().knowsOf(empId)
            && !player().sv.name(sysId).isEmpty())
                GNNNotification.notifyRandomEvent(badEndText(), "GNN_Event_Supernova");
        }
    }
}
