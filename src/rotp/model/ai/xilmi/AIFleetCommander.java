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
package rotp.model.ai.xilmi;

import rotp.model.ai.FleetStats;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import rotp.model.ai.interfaces.FleetCommander;
import rotp.model.ai.FleetOrders;
import rotp.model.ai.FleetPlan;
import rotp.model.ai.ShipDecision;
import rotp.model.ai.ShipPlan;
import rotp.model.combat.CombatStack;
import rotp.model.combat.CombatStackColony;
import rotp.model.combat.ShipCombatManager;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.Galaxy;
import rotp.model.galaxy.Location;
import rotp.model.galaxy.Ship;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.model.ships.ShipDesign;
import static rotp.model.ships.ShipDesign.COLONY;
import rotp.model.ships.ShipDesignLab;
import rotp.ui.NoticeMessage;
import rotp.util.Base;

class AISystemInfo {
    AISystemInfo() {
        enemyFleetStats = new FleetStats();
    }
    float enemyFightingPower;
    float enemyBombardDamage;
    float enemyIncomingTransports;
    FleetStats enemyFleetStats;
    float myBombardDamage;
    float myIncomingTransports;
    float myTotalPower;
    float myRetreatingPower;
    int additionalSystemsInRangeWhenColonized;
    boolean inScannerRange;
    int colonizersEnroute;
}

public class AIFleetCommander implements Base, FleetCommander {
    private static final int DEFAULT_SIZE = 25;
    private final Empire empire;

    private final List<FleetPlan> fleetPlans;
    private final List<Integer> systems;
    private final List<Integer> systemsCommitted;
    private final Map<Integer, AISystemInfo> systemInfoBuffer;
    private final Map<Integer, Float> bridgeHeadConfidenceBuffer;
    private transient boolean canBuildShips = true;
    private transient float maxMaintenance = -1;
    private float myTotalPower = -1;    
    private Location threatCenter = new Location(0,0);

    private List<FleetPlan> fleetPlans()      { return fleetPlans; }
    private List<Integer> systems()           { return systems;  }
    private List<Integer> systemsCommitted()  {return systemsCommitted;  }
    public AIFleetCommander (Empire c) {
        empire = c;
        fleetPlans = new ArrayList<>(DEFAULT_SIZE);
        systems = new ArrayList<>(DEFAULT_SIZE);
        systemsCommitted = new ArrayList<>(DEFAULT_SIZE);
        systemInfoBuffer = new HashMap<>();
        bridgeHeadConfidenceBuffer = new HashMap<>();
    }
    @Override
    public String toString()   { return concat("FleetCommander: ", empire.raceName()); }
    public Location getThreatCenter()
    {
        if(threatCenter.distanceTo(0, 0) != 0)
            return threatCenter;
        threatCenter = empire.generalAI().colonyCenter(empire.generalAI().biggestThreat());
        return threatCenter;
    }
    public boolean minimalTechForRush()
    {
        return (empire.tech().topShipWeaponTech().quintile() > 1 
                || empire.tech().topBaseMissileTech().quintile() > 1 
                || empire.tech().topBaseScatterPackTech() != null) 
                && empire.tech().topSpeed() > 1;
    }
    @Override
    public float maxShipMaintainance() {
        if (maxMaintenance < 0) 
        {
            if(empire.tech().researchCompleted())
                maxMaintenance = 0.8f;
            else if(minimalTechForRush() && (empire.contactedEmpires().size() < 2 || empire.generalAI().techLevelRank() == 1))
                maxMaintenance = min(max(empire.generalAI().gameProgress(), enemyMaintenance()), 0.8f);
            else
                maxMaintenance = min(empire.generalAI().gameProgress(), enemyMaintenance(), 0.8f);
            //System.out.println(galaxy().currentTurn()+" "+empire.name()+" maxMaintenance: "+maxMaintenance+ " enemyMaintenance(): "+enemyMaintenance()+" progress: "+empire.generalAI().gameProgress()+" incomingInvasion: "+incomingInvasion()+" underSiege: "+underSiege()+" minimalTechForRush: "+minimalTechForRush()+" techLevelRank: "+empire.generalAI().techLevelRank()+" enemies: "+empire.enemies().size());
        }
        return maxMaintenance;
    }
    @Override
    public void nextTurn() {
        if (empire.isAIControlled()) {
            log(toString(), ": nextTurn");
            empire.shipLab().needColonyShips = false;
            empire.shipLab().needExtendedColonyShips = false;
            systemInfoBuffer.clear();
            bridgeHeadConfidenceBuffer.clear();
            maxMaintenance = -1;
            myTotalPower = -1;
            threatCenter = new Location(0,0);
            canBuildShips = true; //since we build only colonizers and scouts here, this should always be possible
            NoticeMessage.setSubstatus(text("TURN_FLEET_PLANS"));
            handleTransports();
            handleMilitary();
            buildFleetPlans();
            fillFleetPlans();
            empire.generalAI().nextTurn();
        }
    }
    public void UpdateSystemInfo(int id)
    {
        StarSystem current = galaxy().system(id);
        Empire currentOwner = empire;
        if(current.empire() != null)
            currentOwner = current.empire();
        if(!systemInfoBuffer.containsKey(id))
        {
            AISystemInfo buffy = new AISystemInfo();
            if(!empire.tech().hyperspaceCommunications())
            {
                for(StarSystem sys : galaxy().systemsInRange(current, empire.shipRange()))
                {
                    if(!empire.sv.inShipRange(sys.id))
                    {
                        if(empire.canColonize(sys.id)
                                || empire.unexploredSystems().contains(sys))
                        {
                            buffy.additionalSystemsInRangeWhenColonized++;
                        }
                    }
                }
            }
            for(ShipFleet retreating : galaxy().ships.fleeingFleets(empire.id, id))
            {
                buffy.myRetreatingPower += combatPower(retreating, currentOwner);
                //System.out.print("\n"+empire.name()+"  recorded "+retreating.bcValue()+" worth of ships retreating from "+current.name());
            }
            for(ShipFleet incoming : current.incomingFleets())
            {
                if(incoming.empire().aggressiveWith(empire.id))
                {
                    if(!empire.visibleShips().contains(incoming))
                        continue;
                    buffy.enemyBombardDamage += empire.governorAI().expectedBombardDamageAsIfBasesWereThere(incoming, current, 0);
                    buffy.enemyFleetStats.merge(getFleetStats(incoming));
                    if(incoming.isArmed())
                        buffy.enemyFightingPower += combatPower(incoming, empire);
                }
                if(incoming.empire() == empire)
                {
                    buffy.myBombardDamage += empire.governorAI().expectedBombardDamageAsIfBasesWereThere(incoming, current, 0);
                    if(incoming.canColonizeSystem(current))
                        buffy.colonizersEnroute++;
                    buffy.myTotalPower += combatPower(incoming, incoming);
                }
            }
            for(ShipFleet orbiting : current.orbitingFleets())
            {
                if(orbiting.retreating())
                    continue;
                if(orbiting.empire().aggressiveWith(empire.id))
                {
                    if(!empire.visibleShips().contains(orbiting))
                        continue;
                    buffy.enemyBombardDamage += empire.governorAI().expectedBombardDamageAsIfBasesWereThere(orbiting, orbiting.system(), 0);
                    buffy.enemyFleetStats.merge(getFleetStats(orbiting));
                    if(orbiting.isArmed())
                        buffy.enemyFightingPower += combatPower(orbiting, empire);
                }
                if(orbiting.empire() == empire)
                {
                    buffy.myBombardDamage += empire.governorAI().expectedBombardDamageAsIfBasesWereThere(orbiting, orbiting.system(), 0);
                    if(orbiting.canColonizeSystem(current))
                        buffy.colonizersEnroute++;
                    buffy.myTotalPower += combatPower(orbiting, orbiting);
                }
            }
            if(current.colony() != null)
            {
                buffy.enemyIncomingTransports += empire.unfriendlyTransportsInTransit(current);
                buffy.myIncomingTransports += empire.transportsInTransit(current);
            }
            buffy.inScannerRange = empire.canScanTo(current);
            systemInfoBuffer.put(id, buffy);
        }
    }
    @Override
    public boolean inExpansionMode() {
        for(EmpireView contact : empire.contacts())
        {
            if(empire.inShipRange(contact.empId()))
            {
                return false;
            }
        }
        if((empire.tech().planetology().techLevel() > 19 || empire.ignoresPlanetEnvironment())
            && empire.shipLab().needScouts == false)
        {
            return false;
        }
        return true;
    }
    @Override
    public float transportPriority(StarSystem sv) {
        int id = sv.id;
        float pr = sv.colony().transportPriority();

        if (empire.sv.colony(id).inRebellion())
            return pr * 5;
        else if (empire.sv.isBorderSystem(id))
            return pr * 2;
        else if (empire.sv.isInnerSystem(id))
            return pr / 2;
        else
            return pr;
    }
    //ail: When we didn't find an attack-target we reposition our fleets strategically instead of leaving them where they are
    private StarSystem findBestGatherpoint(ShipFleet fleet)
    {
        Galaxy gal = galaxy();
        StarSystem best = null;
        float bestScore = 0.0f;
        for (int id=0;id<empire.sv.count();id++)
        {
            StarSystem current = gal.system(id);
            Empire currEmp = empire.sv.system(current.id).empire();
            float currentScore = 0.0f;
            if(!fleet.canReach(current))
                continue;
            if(currEmp != null && !currEmp.alliedWith(empire.id) && !empire.enemies().contains(currEmp))
                continue;
            if(current.monster() != null)
                continue;
            if(current.enemyShipsInOrbit(empire))
                continue;
            //skip empty systems when we already are at war. This usually means we are outranged and thus shall defend our border-colonies
            if(currEmp == null && empire.atWar())
                continue;
            UpdateSystemInfo(id);
            if(!systemInfoBuffer.get(id).inScannerRange)
                continue;
            currentScore = 1 / getThreatCenter().distanceTo(current);
            //distance to our fleet also plays a role but it's importance is heavily scince we are at peace and have time to travel
            //currentScore /= max(1, fleet.travelTurns(current));
            if(fleet.system() != current && fleet.destination() != current)
                currentScore *= 1 - (systemInfoBuffer.get(id).myTotalPower / myTotalPower());
            //System.out.print("\n"+fleet.empire().name()+" "+empire.sv.name(fleet.system().id)+" score to gather at: "+empire.sv.name(current.id)+" score: "+currentScore);
            if(currentScore > bestScore)
            {
                bestScore = currentScore;
                best = current;
            }
        }
        /*if(best != null)
            System.out.print("\n"+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" gathers at "+empire.sv.name(best.id)+" score: "+bestScore);*/
        return best;
    }
    private StarSystem smartPath(ShipFleet fleet, StarSystem target)
    {
        if(target != null && !empire.tech().hyperspaceCommunications())
        {
            Galaxy gal = galaxy();
            float ourBombing = fleet.expectedBombardDamage(target, false);
            float ourKilling = empire.governorAI().expectedBombardDamageAsIfBasesWereThere(fleet, target, 0) / 200;
            //We smart-path towards the gather-point to be more flexible
            //for that we seek the closest system from where we currently are, that is closer to the gather-point, if none is found, we go to the gather-point
            StarSystem pathNode = null;
            float smallestDistance = Float.MAX_VALUE;
            for (int id=0;id<empire.sv.count();id++)
            {
                StarSystem current = gal.system(id);
                Empire currEmp = empire.sv.system(id).empire();
                if(!fleet.canReach(current))
                    continue;
                if(currEmp != null && !currEmp.alliedWith(empire.id) && !empire.warEnemies().contains(currEmp))
                    continue;
                if(current.monster() != null)
                    continue;
                if(fleet.distanceTo(target) < fleet.distanceTo(current))
                    continue;
                if(current.distanceTo(target) + fleet.distanceTo(target) / 3 >= fleet.distanceTo(target))
                    continue;
                if(current.distanceTo(target) + fleet.distanceTo(current) > 1.5 * fleet.distanceTo(target))
                    continue;
                float enemyFightingPower = 0.0f;
                float enemyMissileHP = 0.0f;
                float ourFightingPower = 0;
                UpdateSystemInfo(id);
                if(systemInfoBuffer.containsKey(id))
                {
                    ourFightingPower = combatPower(fleet, systemInfoBuffer.get(target.id).enemyFleetStats);
                    enemyFightingPower = systemInfoBuffer.get(id).enemyFightingPower;
                    if(currEmp != null && empire.aggressiveWith(currEmp.id))
                        enemyMissileHP += empire.sv.bases(current.id)*currEmp.tech().newMissileBase().maxHits();
                    //prevent trickling
                    if((currEmp != null && empire.warEnemies().contains(currEmp)) || enemyFightingPower > 0)
                        enemyFightingPower += ourFightingPower;
                }
                if(enemyFightingPower * 2 > ourFightingPower)
                    continue;
                if(enemyMissileHP * 2 > ourBombing && empire.sv.population(current.id) * 2 > ourKilling)
                    continue;
                if(fleet.distanceTo(current) < smallestDistance)
                {
                    pathNode = current;
                    smallestDistance = fleet.distanceTo(current);
                }
            }
            if(pathNode != null)
                target = pathNode;
        }
        return target;
    }
    //ail: using completely different approach for handling my attack-fleets
    private StarSystem findBestTarget(ShipFleet fleet, boolean onlyBomberTargets, boolean onlyColonizerTargets)
    {
        Galaxy gal = galaxy();
        StarSystem best = null;
        float bestScore = 0.0f;
        for (int id=0;id<empire.sv.count();id++) 
        {
            StarSystem current = gal.system(id);
            Empire currEmp = empire.sv.system(id).empire();
            if(!fleet.canReach(current))
            {
                if(onlyColonizerTargets 
                        && fleet.newestOfType(COLONY) != null 
                        && fleet.newestOfType(COLONY).range() > empire.shipRange())
                {
                    if(!empire.sv.inScoutRange(id))
                        continue;
                }
                else
                    continue;
            }
            boolean handleEvent = false;
            
            if(current.monster() != null) {
                //System.out.println(galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" => "+empire.sv.name(current.id)+" monsterpower: "+combatPower(current.monster(), fleet)+" fleetpower: "+combatPower(fleet, getMonsterStats(current.monster().combatStacks())));
                if(combatPower(current.monster(), fleet) > combatPower(fleet, getMonsterStats(current.monster().combatStacks())))
                {
                    //System.out.println(galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" => "+empire.sv.name(current.id)+" should be skipped!");
                    continue;
                }
            }
            float score = 0.0f;
            float transports = 0.0f;
            float myTransports = 0.0f;
            float enemyFightingPower = 0.0f;
            float enemyBombardDamage = 0.0f;
            float bombardDamage = 0.0f;
            float myPower = 0.0f;
            float myRetreatingPower = 0.0f;
            int colonizationBonus = 0;
            int colonizerEnroute = 0;
            boolean canScanTo = false;
            UpdateSystemInfo(id);
            if(systemInfoBuffer.containsKey(id))
            {
                enemyFightingPower = systemInfoBuffer.get(id).enemyFightingPower;
                enemyBombardDamage = systemInfoBuffer.get(id).enemyBombardDamage;
                transports = systemInfoBuffer.get(id).enemyIncomingTransports;
                bombardDamage = systemInfoBuffer.get(id).myBombardDamage;
                myPower = systemInfoBuffer.get(id).myTotalPower;
                myTransports = systemInfoBuffer.get(id).myIncomingTransports;
                colonizationBonus = systemInfoBuffer.get(id).additionalSystemsInRangeWhenColonized;
                colonizerEnroute = systemInfoBuffer.get(id).colonizersEnroute;
                canScanTo = systemInfoBuffer.get(id).inScannerRange;
                myRetreatingPower = systemInfoBuffer.get(id).myRetreatingPower;
            }
            //When it is ourselves who are en-route, don't let that reduce the score
            //If we already sent a fleet to an enemy system out of our scanner-range we don't send more there to reinforce as long as we don't get better information
            if((myPower > 0 || bombardDamage > 0 || myRetreatingPower > 0) && !canScanTo && empire.aggressiveWith(empire.sv.empId(id)))
            {
                //System.out.print("\n"+fleet.empire().name()+" check if I can attack "+empire.sv.name(current.id)+" out of range expected bombard: "+fleet.expectedBombardDamage(empire.sv.system(id))+" HP: "+empire.sv.system(id).colony().untargetedHitPoints());
                if(empire.sv.system(id).colony() != null && empire.governorAI().expectedBombardDamageAsIfBasesWereThere(fleet, empire.sv.system(id), 0) < empire.sv.system(id).colony().untargetedHitPoints())
                    continue;
            }

            //ail: incase we have hyperspace-communications and are headed to current, we have to substract ourself from the values
            //This needs to happen always, not just when we are about to buffer it
            //System.out.print("\n"+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" => "+empire.sv.name(current.id)+" bc: "+bc+" bomb: "+bombardDamage);
            /*if(!fleet.isArmed() && enemyFightingBc > 0)
                continue;*/
            if(fleet.inTransit() && fleet.destination() == current)
            {
                myPower -= combatPower(fleet, currEmp);
                if(fleet.hasColonyShip())
                    colonizerEnroute--;
                bombardDamage -= empire.governorAI().expectedBombardDamageAsIfBasesWereThere(fleet, current, 0);
                //System.out.print("\n"+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" => "+empire.sv.name(current.id)+" bc: "+bc+" bomb: "+bombardDamage);
            }
            if(empire.sv.isColonized(id))
            {
                score = 10;
                float baseBc = empire.sv.bases(current.id)*currEmp.tech().newMissileBaseCost();
                float popBc = empire.sv.population(current.id) * empire.sv.empire(current.id).tech().populationCost();
                score /= (baseBc + popBc) / popBc;
                //System.out.print("\n"+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" => "+empire.sv.name(current.id)+" score: "+score);
                if(onlyColonizerTargets && !(empire.generalAI().allowedToBomb(current) && bombardDamage > 0))
                {
                    continue;
                }
            }
            //if we don't have scouts anymore, we still need a way to uncover new systems
            else if((!empire.sv.isScouted(id) || current.monster() != null) && myPower == 0 )
            {
                score = 5.0f;
                if(current.starType().key().equals("YELLOW"))
                    score *= 2;
                else if(current.starType().key().equals("ORANGE") || current.starType().key().equals("RED"))
                    score *= 1.5;
                if(onlyColonizerTargets || fleet.hasColonyShip())
                {
                    score += colonizationBonus * 5;
                }
            }
            else if(fleet.canColonizeSystem(current))
            {
                score = 20.0f;
                float bonusScore = 20 * current.planet().maxSize() / 100.0f;
                bonusScore *= (1+empire.sv.artifactLevel(id));
                if (empire.sv.isUltraRich(id))
                    bonusScore *= 3;
                else if (empire.sv.isRich(id))
                    bonusScore *= 2;
                else if (empire.sv.isPoor(id))
                    bonusScore /= 2;
                else if (empire.sv.isUltraPoor(id))
                    bonusScore /= 3;
                bonusScore += colonizationBonus * 5;
                score += bonusScore;
                if(enemyFightingPower > 0)
                    score /= enemyFightingPower;
            }
            if(empire.alliedWith(empire.sv.empId(id)))
            {
                //attacking is a lot better than defending, so defending should have a lower score in general. Unless there's incoming transports, that is.
                if(transports == 0)
                    score *= bcValue(fleet, false, true, false, false) / fleet.bcValue();
                else
                    score *= 1 + transports / current.population();
                if (currEmp == empire && current.hasEvent()) {
                    if (current.eventKey().equals("MAIN_PLANET_EVENT_PIRACY")) {
                        handleEvent = true;
                    }
                    if (current.eventKey().equals("MAIN_PLANET_EVENT_COMET")) {
                        handleEvent = true;
                    }
                }
                if(current.colony() != null && enemyBombardDamage > current.colony().untargetedHitPoints() && fleet.travelTurnsAdjusted(current) > 1)
                {
                    score = 1.0f;
                }
                if(enemyBombardDamage == 0 && transports == 0 && !handleEvent)
                {
                    continue;
                }
                if(onlyBomberTargets && !handleEvent)
                {
                    continue;
                }
                if(myPower > enemyFightingPower && !handleEvent && transports == 0)
                    continue;
            }
            else
            {
                if(empire.sv.isColonized(id))
                {
                    if(!empire.enemies().contains(currEmp))
                    {
                        continue;
                    }
                    if(!empire.warEnemies().contains(currEmp) && !empire.generalAI().strongEnoughToAttack())
                        continue;
                    //Skip systems that are already attacked by competitor
                    if(canScanTo) {
                        boolean wantToSkip = false;
                        for(ShipFleet fl : current.orbitingFleets()) {
                            if(fl.empire().aggressiveWith(current.empId()) && empire.aggressiveWith(fl.empId()) && combatPower(fl, fleet) > combatPower(fleet, fl)) {
                                wantToSkip = true;
                                break;
                            }
                        }
                        if(wantToSkip)
                            continue;
                    }
                }
            }
            if(bombardDamage > 0 && fleet.system() != current)
            {
                if(empire.sv.system(current.id).colony() != null)
                    score *= Math.min(empire.sv.system(current.id).colony().untargetedHitPoints() / bombardDamage, 1.0f);
            } 
            else if(bombardDamage > 0 && fleet.system() == current)
                score = 0; //score will be 0 and the amount of ships that stay there will be handled via keepBC
            if (myPower > enemyFightingPower && transports == 0 && fleet.sysId() != current.id && (currEmp == null || empire.alliedWith(empire.sv.empId(id))))
            {
                if(!(currEmp == null && fleet.canColonizeSystem(current) && colonizerEnroute == 0))
                    score /= myPower;
            }
            if(handleEvent)
            {
                score = 10;
            }
            boolean ignoreTravelTime = false;
            if(fleet.canColonizeSystem(current))
            {
                if(colonizerEnroute > 0)
                    score /= (systemInfoBuffer.get(id).colonizersEnroute * 10) + 1;
                if(empire.shipLab().colonyDesign().size() > 2)
                    ignoreTravelTime = true;
            }
            if(!ignoreTravelTime)
            {
                score /= pow(max(1, fleet.travelTurnsAdjusted(current)), 2) + 1;
            }
            /*if(score > 0)
                System.out.println(galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" => "+empire.sv.name(current.id)+" score: "+score+" enemy-transports: "+transports+" colonizerEnroute: "+colonizerEnroute);*/
            if(score > bestScore)
            {
                bestScore = score;
                best = current;
            }
        }
        /*if(best != null)
            System.out.println(fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" x: "+fleet.x()+" y: "+fleet.y()+" => "+empire.sv.name(best.id)+" score: "+bestScore+" colonizersEnroute: "+systemInfoBuffer.get(best.id).colonizersEnroute);*/
        return best;
    }
    
    private void buildFleetPlans() {
        // clear existing fleet plans & planetary build queues
        fleetPlans().clear();
        systems().clear();
        systemsCommitted().clear();
        for (int id=0; id<empire.sv.count();id++) {
            empire.sv.clearFleetPlan(id);
            if (empire.sv.empire(id) == empire) {
                empire.sv.colony(id).shipyard().resetQueueData();
                if (canBuildShips)
                    systems.add(id);
            }
        }

        // make fleet plans for each unplanned system
        for (int id=0; id<empire.sv.count();id++) {
            reviseFleetPlan(id);
            if (empire.sv.hasFleetPlan(id) && !empire.sv.fleetPlan(id).isClear()) 
                fleetPlans.add(empire.sv.fleetPlan(id));
        }

        // fleets that are somehow trapped beyond scout range need to retreat
        setDistantFleetsToRetreat();
        
        // sort fleet plans by priority
        Collections.sort(fleetPlans, FleetPlan.PRIORITY);
    }
    private void fillFleetPlans() {
        if (empire.tech().topFuelRangeTech().range() > 8)
            empire.shipLab().needScouts = false;
        else if (empire.scanPlanets())
            empire.shipLab().needScouts = false;
        else if (empire.shipLab().colonyDesign().size() <= 2 
                && empire.shipLab().colonyDesign().range() >= empire.scoutRange())
            empire.shipLab().needScouts = false;
        else if (empire.atWar())
            empire.shipLab().needScouts = false;
            
        NoticeMessage.setSubstatus(text("TURN_DEPLOY_FLEETS"));
        // get fleet orders for each fleet
        List<ShipFleet> fleets = galaxy().ships.allFleets(empire.id);
        List<FleetOrders> fleetOrders = new ArrayList<>(fleets.size());
        for (ShipFleet fleet : fleets) {
            if (fleet.hasShips())
                fleetOrders.add(fleet.newOrders());
        }

        int numPlans = fleetPlans.size();
        int numShips = 0;
        for (FleetPlan fp: fleetPlans)
            numShips += fp.numNeededShips();

        int numComplete = 0;

        log("Fleet Plans to fill:  ", str(numPlans), "  ships: ", str(numShips));
        //for (FleetPlan fp: fleetPlans) 
        //    log(fp.fullName());          
        
        List<FleetPlan> retreatPlans = new ArrayList<>();
        for (FleetPlan fPlan: fleetPlans) {
            NoticeMessage.setSubstatus(text("TURN_DEPLOY_FLEET_X", str(numComplete), str(numPlans)));
            if (fPlan.priority() == FleetPlan.RETREAT) 
                retreatPlans.add(fPlan);
            else 
                assignShipsToFleetPlans(fPlan, fleetOrders);
            numComplete += 1;
        }

        //  if we have retreat plans (unlikely), process them
        if (!retreatPlans.isEmpty()) {
            NoticeMessage.setSubstatus(text("TURN_RETREAT_FLEETS"));
            for (FleetPlan fp: retreatPlans) {
                ShipFleet fleet = empire.sv.orbitingFleet(fp.destId);
                if (fleet != null) {
                    StarSystem dest = galaxy().system(fp.destId);
                    StarSystem safeSystem = RetreatSystem(fleet);
                    if(safeSystem == null)
                        safeSystem = empire.shipCaptainAI().retreatSystem(dest);
                    log("Withdrawing fleet: ", fleet.toString(), " from: ", str(fp.destId), "  to: ", safeSystem.toString());
                    galaxy().ships.retreatFleet(fleet, safeSystem.id);
                }
            }
        }
    }
    private void assignShipsToFleetPlans(FleetPlan fPlan, List<FleetOrders> fleetOrders) {
        // remove all plans of this priority from future loops
        //  log(str(plansToMeet.size()), " fleets:", str(plansToMeet.get(0).priority()));
        // 1. for each plan, fill from fleets already at the dest
        ShipFleet destFleet = empire.sv.orbitingFleet(fPlan.destId);
        // subtract from plan the ships already at the dest
        if (destFleet != null)
            fPlan.subtract(destFleet.orders());
        // if this is a staged plan and is unfilled by fleet at staging point, all future ships go to staging point
        if (fPlan.isStaged() && fPlan.needsShips()) {
            ShipFleet stagingFleet = empire.sv.orbitingFleet(fPlan.stagingPointId);
            if (fPlan.canBeFilledBy(stagingFleet)) 
                fPlan.subtract(stagingFleet.orders());
            else
                fPlan.switchToStagingPoint();
        }

        // 2. for each plans, deduct any existing fleet orders that are currently in transit to the dest
        for (FleetOrders orders : fleetOrders) {
            if (orders.destSysId == fPlan.currentDestId())
                fPlan.eliminateCommonShips(orders);
        }

        // 3. for unmet fleet plans, build list of remaining ship plans to meet
        List<ShipPlan> shipPlans = fPlan.shipPlans();

        // loop through ship design plans & orders, looking for the optimal match
        for (ShipPlan sPlan: shipPlans) {
            boolean finished = false;
            while (!finished) {
                // find the best decision for this plan
                ShipDecision bestDecision = shipPlans.get(0).decide(fleetOrders, systems, empire);
                // implement the best decision
                float prevNum = sPlan.num;
                bestDecision.implement(fleetOrders, systemsCommitted());
                // we are finished with this plan if all of its ship needs are met (num == 0)
                // or we are no longer able to meet any more needs (num == prevNum)
                finished = (sPlan.num == 0) || (sPlan.num == prevNum);
            }
        }
    }
    private void reviseFleetPlan(int sysId) {        
        float scoutRange = empire.scoutRange();
        // if out of scout range, forget it
        if (!empire.sv.withinRange(sysId, scoutRange)) 
            return;
        // if a fleet plan has already been assigned (by AIGeneral), then skip
        if (empire.sv.fleetPlan(sysId).needsShips()) 
            return;
        // if guarded, forget it for now
        if (empire.sv.isGuarded(sysId)) 
            return;

        // if not scouted and owner still using scouts, send a scout
        // if it has no known missile bases or if it is an ally 
        if (!empire.sv.isScouted(sysId) && empire.shipDesignerAI().BestDesignToScout().range() >= empire.tech().scoutRange()) { //once we no longer use scouts, we do this in another way
            if (empire.alliedWith(empire.sv.empId(sysId))
            || (empire.sv.bases(sysId) == 0)) 
                setScoutFleetPlan(sysId);
        }
    }
    private void setDistantFleetsToRetreat() {
        List<ShipFleet> fleets = galaxy().ships.notInTransitFleets(empire.id);
        float range = empire.tech().scoutRange();
        for (ShipFleet fl: fleets) {
            int sysId = fl.sysId();
            if (!empire.sv.withinRange(sysId, range))  {
                //ail: no retreating, if I can still bomb the enemy
                if(empire.enemies().contains(fl.system().empire()) && fl.expectedBombardDamage(false) > 0)
                {
                    continue;
                }
                setRetreatFleetPlan(sysId);
                fleetPlans.add(empire.sv.fleetPlan(sysId));
            }
            //we also want to retreat fleets that are trespassing to avoid prevention-wars
            if(fl.system().empire()!= null 
                    && !empire.enemies().contains(fl.system().empire())
                    && !empire.allies().contains(fl.system().empire())
                    && empire != fl.system().empire())
            {
                setRetreatFleetPlan(sysId);
                fleetPlans.add(empire.sv.fleetPlan(sysId));
            }
        }
    }
    private void setRetreatFleetPlan(int id) {
        empire.sv.fleetPlan(id).priority = FleetPlan.RETREAT;
    }
    private float getClosestDistanceToShip(int id, ShipDesign des) {
        float closestDist = Float.MAX_VALUE;
        StarSystem sys = empire.sv.system(id);
        for(ShipFleet fleet:empire.allFleets())
        {
            if(!fleet.hasShip(des))
                continue;
            if(!fleet.canSend() || fleet.deployed() || fleet.retreating())
                continue;
            float currentDist = sys.distanceTo(fleet);
            if(currentDist < closestDist)
                closestDist = currentDist;
        }
        if(closestDist == Float.MAX_VALUE)
            return empire.distanceTo(sys);
        return closestDist;
    }
    private void setScoutFleetPlan (int id) {
        if(systemInfoBuffer.containsKey(id) && systemInfoBuffer.get(id).colonizersEnroute > 0)
            return;
        FleetPlan plan = empire.sv.fleetPlan(id);
        if (empire.sv.isScouted(id))
            plan.priority = FleetPlan.SCOUT_TO_EXPLORED;
        else {
            float closeRangeBonus = 100 - getClosestDistanceToShip(id, empire.shipDesignerAI().BestDesignToScout())/10;
            //float closeRangeBonus = 100 - empire.sv.distance(id)/10;
            plan.priority = FleetPlan.SCOUT_TO_UNEXPLORED + closeRangeBonus;
        }
        if (empire.shipDesignerAI().BestDesignToScout().range() >= empire.tech().scoutRange())
            plan.addShips(empire.shipDesignerAI().BestDesignToScout(), 1);
    }
    private void handleTransports()
    {
        for(Transport trn : empire.transports())
        {
            if(empire.enemies().contains(trn.destination().empire()))
            {
                if(trn.surrenderOnArrival())
                    trn.toggleSurrenderOnArrival();
            }
            else if(trn.destination().empire() != empire)
            {
                if(!trn.surrenderOnArrival())
                    trn.toggleSurrenderOnArrival();
            }
        }
    }
    //ail: Entirely new way of handling the military
    private void handleMilitary()
    {
        //ail: when we have colonizers but don't know we need any, we send them with our attacks, so they can colonize the bombed system also this should allow to scout with initial colonizer
        //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" firepower: "+totalFirePower()+" firepower needed: "+firePowerNeededForAttack()+" def-budget: "+stationaryDefenseBudget());
        for(ShipFleet fleet:empire.allFleets())
        {
            //If we have made peace and war again, we disable potential retreatOnArrival
            if(fleet.destination() != null)
            {
                if(fleet.retreatOnArrival() && empire.enemies().contains(fleet.destination().empire()))
                    fleet.toggleRetreatOnArrival();
            }
            //Improve retreat-target for retreating fleets that are still at the system they retreat from
            //this cannot be done from ship-captain as at that point it isn't known how big the retreating fleet will become when it retreats partially
            if(fleet.retreating() && fleet.system() != null && fleet.distanceTo(fleet.system()) == 0)
            {
                if(RetreatSystem(fleet) != null)
                {
                    //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" fleet at "+fleet.system().name()+" rerouted from "+fleet.destination().name()+" to "+RetreatSystem(fleet).name());
                    attackWithFleet(fleet, RetreatSystem(fleet), 1.0f, 1.0f, true, true, true, true, 0, true);
                }
            }
            if(!fleet.canSend() || fleet.deployed() || fleet.retreating())
            {
                continue;
            }
            else
            {
                boolean canStillSend = true;
                float keepPower = 0;
                StarSystem previousBest = null;
                StarSystem previousAttacked = null;
                while(canStillSend)
                {
                    boolean allowFighters = true;
                    boolean allowBombers = true;
                    boolean allowColonizers = true;
                    float sendAmount = 1.0f;
                    float sendBombAmount = 1.0f;
                    float keepAmount = 0.0f;
                    boolean onlyColonizerTargets = false;
                    boolean targetIsGatherPoint = false;
                    boolean onlyAllowRealTarget = false;
                    boolean targetIsPreviousBest = false;
                    
                    if(fleet.numFighters() == 0 && fleet.numBombers() == 0)
                        onlyColonizerTargets = true;
                    
                    StarSystem target = findBestTarget(fleet, false, onlyColonizerTargets);
                    if(previousBest == target)
                       targetIsPreviousBest = true;
                    previousBest = target;
                    if(empire.enemies().contains(fleet.system().empire()))
                    {
                        float requiredBombardDamage = fleet.system().population() * 200;
                        if(empire.transportsInTransit(fleet.system()) > 0)
                        {
                            requiredBombardDamage *= 0.9f;
                        }
                        float expectedBombardDamage = fleet.expectedBombardDamage(false);
                        //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" raw keepAmount: "+requiredBombardDamage / expectedBombardDamage+" expected: "+expectedBombardDamage+" required: "+requiredBombardDamage);
                        if(expectedBombardDamage > 0)
                            keepAmount = min(1, requiredBombardDamage / expectedBombardDamage);
                        if(keepAmount < 1)
                            onlyAllowRealTarget = true;
                    }
                   
                    //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" keep: "+keepAmount);
                    if(keepAmount >= 1)
                        break;

                    if(target == null && !onlyAllowRealTarget)
                    {
                        //System.out.print("\n"+galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" didn't find a target at first.");
                        if(onlyColonizerTargets == false && fleet.hasColonyShip())
                        {
                            onlyColonizerTargets = true;
                            target = findBestTarget(fleet, false, onlyColonizerTargets);
                        }
                        if(target == null)
                        {
                            target = findBestGatherpoint(fleet);
                            targetIsGatherPoint = true;
                            //System.out.print("\n"+galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" didn't get a regular target.");
                        }
                    }
                    if(target != null)
                    {
                        int travelTurns = fleet.travelTurnsAdjusted(target);                    
                        //System.out.println(galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.system().id)+" wants to go for "+empire.sv.name(target.id)+" id: "+target.id);
                        float bombardDamage = fleet.expectedBombardDamage(target, false);
                        float killPower = empire.governorAI().expectedBombardDamageAsIfBasesWereThere(fleet, target, 0) / 200;
                        float combatPower = combatPower(fleet);
                        UpdateSystemInfo(fleet.sysId());
                        Empire tgtEmpire = empire.sv.empire(target.id);
                        float stayToKillTransports = 0;
                        float transportsToDealWith = 0;
                        if(fleet.system().empire() == empire || empire.enemies().contains(fleet.system().empire()))
                            transportsToDealWith = systemInfoBuffer.get(fleet.sysId()).enemyIncomingTransports;
                        if(empire.enemies().contains(fleet.system().empire())) 
                            transportsToDealWith = max(transportsToDealWith, systemInfoBuffer.get(fleet.sysId()).myIncomingTransports);
                        if(transportsToDealWith > 0)
                        {
                            float TransportKills = fleet.firepowerAntiShip(0) * transportGauntletRounds(max(1, empire.tech().topEngineWarpTech().baseWarp() - 1)) / empire.tech().topArmorTech().transportHP;
                            transportsToDealWith *= 1 - empire.combatTransportPct();
                            stayToKillTransports = combatPower * min(1, transportsToDealWith / TransportKills);
                            //System.out.print("\n"+galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+fleet.system().name()+" should be able to kill "+TransportKills+"/"+transportsToDealWith+" transports. Need to keep: "+stayToKillTransports+" of "+fleet.bcValue());
                        }
                        keepPower = max(keepPower, systemInfoBuffer.get(fleet.sysId()).enemyFightingPower * 2, stayToKillTransports);
                        if(systemInfoBuffer.get(fleet.sysId()).enemyBombardDamage > 0)
                            keepPower = max(keepPower, 1);
                        //System.out.println(galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+fleet.system().name()+" enemy-power: "+systemInfoBuffer.get(fleet.sysId()).enemyFightingPower+" my power: "+combatPower(fleet, systemInfoBuffer.get(fleet.sysId()).enemyFleetStats));
                        if(systemInfoBuffer.get(fleet.sysId()).enemyFightingPower > combatPower(fleet, systemInfoBuffer.get(fleet.sysId()).enemyFleetStats))
                            keepPower = 0;
                        keepPower = min(keepPower, combatPower);
                        boolean targetHasEvent = false;
                        boolean currentHasEvent = false;
                        if (target.empire() == empire && target.hasEvent()) {
                            if (target.eventKey().equals("MAIN_PLANET_EVENT_PIRACY")) {
                                targetHasEvent = true;
                            }
                            if (target.eventKey().equals("MAIN_PLANET_EVENT_COMET")) {
                                targetHasEvent = true;
                            }
                        }
                        if (fleet.system().empire() == empire && fleet.system().hasEvent()) {
                            if (target.eventKey().equals("MAIN_PLANET_EVENT_PIRACY")) {
                                currentHasEvent = true;
                            }
                            if (target.eventKey().equals("MAIN_PLANET_EVENT_COMET")) {
                                currentHasEvent = true;
                            }
                        }
                        if(currentHasEvent)
                            keepPower = combatPower;
                        //convert keepPower to keepAmount
                        if(combatPower > 0)
                            keepAmount = keepPower / combatPower;
                        //System.out.println(galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+fleet.system().name()+" keepPower: "+keepPower);
                        if(targetIsGatherPoint)
                        {
                            target = smartPath(fleet, target);
                            //System.out.print("\n"+galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+fleet.system().name()+" gathers at: "+target.name());
                            attackWithFleet(fleet, target, sendAmount - keepAmount, sendBombAmount - keepAmount, false, true, true, true, keepAmount, true);
                            break;
                        }
                        StarSystem stagingPoint = StageSystem(fleet, target);
                        float enemyFleetPower = 0.0f;
                        float enemyBaseHP = 0.0f;
                        float enemyBaseDamage = 0.0f;
                        float enemyPop = 0.0f;
                        FleetStats enemyStats = new FleetStats();
                        for(ShipFleet orbiting : target.orbitingFleets())
                        {
                            if(orbiting.retreating())
                                continue;
                            if(orbiting.empire().aggressiveWith(fleet.empId()))
                            {
                                if(!empire.visibleShips().contains(orbiting))
                                {
                                    continue;
                                }
                                EmpireView ev = empire.viewForEmpire(orbiting.empId());
                                if(orbiting.isArmed()) {
                                    enemyStats.merge(getFleetStats(orbiting));
                                    enemyFleetPower += combatPower(orbiting, fleet);
                                }
                            }
                        }
                        if(target.monster() != null)
                        {
                            enemyFleetPower += combatPower(target.monster(), fleet);
                            enemyStats.merge(getMonsterStats(target.monster().combatStacks()));
                        }
                        for(ShipFleet incoming : target.incomingFleets())
                        {
                            if(incoming.empire().aggressiveWith(empire.id))
                            {
                                if(!empire.visibleShips().contains(incoming))
                                    continue;
                                if(incoming.travelTurnsRemainingAdjusted() > travelTurns)
                                    continue;
                                EmpireView ev = empire.viewForEmpire(incoming.empId());
                                if(incoming.isArmed()){
                                    enemyStats.merge(getFleetStats(incoming));
                                    enemyFleetPower += combatPower(incoming, fleet);
                                }
                            }
                        }
                        float ourFleetPower = combatPower(fleet, enemyStats);
                        if(tgtEmpire != null)
                        {
                            //experimental: Prevent "trickling in" by adding what we already sent to enemyFightingBC
                            //System.out.println(galaxy().currentTurn()+" "+fleet.empire().name()+" bridgeHeadConfidence for "+target.name()+": "+bridgeHeadConfidence(target));
                            if(!empire.tech().hyperspaceCommunications() && !targetHasEvent && bridgeHeadConfidence(target) < 1)
                                enemyFleetPower += systemInfoBuffer.get(target.id).myTotalPower + systemInfoBuffer.get(target.id).myRetreatingPower;
                            //System.out.print("\n"+galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+target.name()+" gets boosted by my own fightingpower of "+(systemInfoBuffer.get(target.id).myTotalPower + systemInfoBuffer.get(target.id).myRetreatingPower)+" to: "+enemyFleetPower);
                            float TransportKillPowerNeeded = 0;
                            if(empire.alliedWith(tgtEmpire.id) && (enemyFleetPower > 0 || empire.unfriendlyTransportsInTransit(target) > 0))
                            {
                                allowBombers = false;
                                allowColonizers = false;
                                float incomingTransports = empire.unfriendlyTransportsInTransit(target);
                                if(incomingTransports > 0)
                                {
                                    float TransportKills = fleet.firepowerAntiShip(0) * transportGauntletRounds(max(1, empire.tech().topEngineWarpTech().baseWarp() - 1)) / empire.tech().topArmorTech().transportHP;
                                    incomingTransports *= 1 - empire.combatTransportPct();
                                    TransportKillPowerNeeded = ourFleetPower * min(1, incomingTransports / TransportKills);
                                    //System.out.print("\n"+galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+fleet.system().name()+" should be able to kill "+TransportKills+"/"+transportsToDealWith+" transports. Need to keep: "+stayToKillTransports+" of "+fleet.bcValue());
                                }
                                sendBombAmount = 0;
                                if (targetHasEvent) {
                                    sendAmount = 1.0f;
                                    sendBombAmount = 1.0f;
                                    allowBombers = true;
                                }
                            }
                            else
                            {
                                EmpireView ev = empire.viewForEmpire(empire.sv.empId(target.id));
                                if(ev != null)
                                {
                                    enemyBaseHP = empire.sv.bases(target.id)*ev.empire().tech().newMissileBase().maxHits();
                                    enemyPop = empire.sv.population(target.id);
                                    float ourShield = avgFleetShield(fleet);
                                    float timeToReachColony = (float) Math.ceil(8.0 / getFleetStats(fleet).avgCombatSpeed);
                                    float timeToKillBases = (float) Math.ceil(enemyBaseHP / bombardDamage); 
                                    float timeToKillPop = (float) Math.ceil(enemyPop / killPower);
                                    if(target.inNebula())
                                        ourShield = 0;
                                    enemyBaseDamage = empire.sv.bases(target.id)*ev.empire().tech().newMissileBase().firepower(ourShield) * (timeToReachColony + Math.min(timeToKillBases, timeToKillPop));
                                    //System.out.println(galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+fleet.system().name()+" => "+empire.sv.name(target.id)+" avgShield: "+avgFleetShield(fleet)+" missile-bases at target deal damage: "+enemyBaseDamage+" Fleet-Health: "+totalFleetHealth(fleet)+ " Time to kill bases: "+(timeToReachColony + timeToKillBases)+" move: "+timeToReachColony+" kill: "+timeToKillBases);
                                }
                            }
                            //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" => "+target.name()+" ourEffectiveBC: "+ourEffectiveBC+" ourEffectiveBombBC: "+ourEffectiveBombBC+" ourColonizerBC: "+ourColonizerBC+" keepBc: "+keepBc+" col-adpt: "+empire.shipDesignerAI().fightingAdapted(empire.shipLab().colonyDesign()));
                            //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" thinks "+target.name()+" has "+enemyFightingBC+" our effective: "+(ourEffectiveBC - keepBc));
                            if(killPower > 0 && enemyPop > 0)
                                sendBombAmount = min(sendBombAmount, enemyPop / killPower);
                            if(enemyBaseDamage > 0 && bombardDamage > 0)
                                sendBombAmount = max(sendBombAmount, enemyBaseDamage / totalFleetHealth(fleet), enemyBaseHP / bombardDamage);
                            sendBombAmount = min(1, sendBombAmount);
                            //System.out.println(galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.sysId())+" => "+empire.sv.name(target.id)+" sendAmount before fleetpower: "+sendAmount);
                            sendAmount = max(sendBombAmount, min(1.0f, max(TransportKillPowerNeeded,  enemyFleetPower * 2) / ourFleetPower));
                            sendAmount = min(sendAmount, 1 - keepAmount);
                            sendBombAmount = min(sendBombAmount, 1 - keepAmount);
                            //System.out.println(galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.sysId())+" => "+empire.sv.name(target.id)+" sendAmount after fleetpower: "+sendAmount);
                        }
                        else
                        {
                            if(fleet.canColonizeSystem(target) && target.monster() == null)
                            {
                                allowColonizers = true;
                                allowBombers = false;
                                if(enemyFleetPower == 0)
                                {
                                    sendAmount = 0.01f;
                                    sendBombAmount = 0;
                                    allowFighters = false;
                                }
                            }
                        }
                        if(!empire.sv.isScouted(target.id) && !empire.sv.isColonized(target.id) && target.monster() == null)
                        {
                            sendAmount = 0.01f;
                            sendBombAmount = 0.01f;
                        }
                        if(target.monster() != null)
                        {
                            allowBombers = false;
                        }
                        //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" should attack "+empire.sv.name(target.id)+" "+ourFleetPower * sendAmount+":"+enemyFleetPower+" sendAmount: "+sendAmount+" sendBombAmount: "+sendBombAmount+" killPower: "+killPower+" enemyPop: "+enemyPop+" enemyBaseDamage: "+enemyBaseDamage+":"+totalFleetHealth(fleet));
                        //ail: if we have Hyperspace-communications, we can't split
                        if(targetIsPreviousBest)
                        {
                            sendAmount = 1.0f - keepAmount;
                            sendBombAmount = 1.0f - keepAmount;
                        }
                        if(fleet.inTransit())
                        {
                            sendAmount = 1.0f;
                            sendBombAmount = 1.0f;
                            allowFighters = true;
                            allowBombers = true;
                            allowColonizers = true;
                        }
                        //System.out.println(galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+empire.sv.name(fleet.sysId())+" => "+empire.sv.name(target.id)+" "+ourFleetPower+":"+enemyFleetPower+" bombardDamage: "+bombardDamage+" enemyBaseHP: "+enemyBaseHP+" killPower: "+killPower+" pop: "+enemyPop+" sendAmount: "+sendAmount+" sendBombAmount: "+sendBombAmount+" targetIsPreviousBest: "+targetIsPreviousBest+" KeepPower: "+keepPower+" KeepAmount: "+ keepAmount);
                        /*if(stagingPoint != null)
                            System.out.println(fleet.empire().name()+" Fleet at "+fleet.system().name()+" => "+empire.sv.name(target.id)+" should stage at "+empire.sv.name(stagingPoint.id));*/
                        if((ourFleetPower * sendAmount >= enemyFleetPower
                                && ((bombardDamage > enemyBaseHP || killPower > enemyPop) && enemyBaseDamage < totalFleetHealth(fleet)) || !empire.sv.isColonized(target.id))
                                || (previousAttacked == target))
                        {
                            StarSystem targetBeforeSmartPath = target;
                            /*if(!(fleet.canColonizeSystem(target) && tgtEmpire == null))
                                target = smartPath(fleet, target);*/
                            if(sendAmount > 0.01 || sendBombAmount > 0.01)
                                target = smartPath(fleet, target);
                            boolean allowSplitBySpeed = true;
                            if(targetBeforeSmartPath == target)
                                allowSplitBySpeed = false;
                            if(fleet.canSendTo(target.id))
                            {
                                int numBeforeSend=fleet.numShips();
                                //ail: first send everything except fighters
                                attackWithFleet(fleet, target, sendAmount, sendBombAmount, false, allowFighters, allowBombers, allowColonizers, keepAmount, allowSplitBySpeed);
                                previousAttacked = target;
                                if((sendAmount >= 1.0f && sendBombAmount >= 1.0f) || numBeforeSend == fleet.numShips())
                                {
                                    //System.out.println(fleet.empire().name()+" Fleet at "+fleet.system().name()+" should attack "+target.name()+" allowBombers: "+allowBombers);
                                    canStillSend = false;
                                }
                                //System.out.println(fleet.empire().name()+" Fleet at "+fleet.system().name()+" has been sent "+target.name()+" sent: "+sendAmount);
                            }
                            else
                            {
                                if(empire.sv.inScoutRange(target.id) 
                                        && fleet.newestOfType(COLONY) != null
                                        && fleet.newestOfType(COLONY).range() > empire.shipRange())
                                {
                                    int numBeforeSend=fleet.numShips();
                                    attackWithFleet(fleet, target, sendAmount, sendBombAmount, false, allowFighters, allowBombers, allowColonizers, keepAmount, false);
                                    previousAttacked = target;
                                    //System.out.print("\n"+galaxy().currentTurn()+" "+fleet.empire().name()+" Ranged Colonizers at "+fleet.system().name()+" going to: "+target);
                                    if((sendAmount >= 1.0f && sendBombAmount >= 1.0f) || numBeforeSend == fleet.numShips())
                                    {
                                        //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" should attack "+target.name()+" allowBombers: "+allowBombers);
                                        canStillSend = false;
                                    }
                                }
                                else
                                    canStillSend = false;
                            }
                        }
                        else if(stagingPoint != null
                            && fleet.system() != stagingPoint
                            && !onlyAllowRealTarget)
                        {
                            stagingPoint = smartPath(fleet, stagingPoint);
                            int numBeforeSend=fleet.numShips();
                            attackWithFleet(fleet, stagingPoint, sendAmount, sendBombAmount, false, allowFighters, allowBombers, allowColonizers, keepAmount, true);
                            previousAttacked = stagingPoint;
                            //System.out.print("\n"+galaxy().currentTurn()+" "+fleet.empire().name()+" Fleet at "+fleet.system().name()+" wanting to attack "+target.name()+" stages at: "+stagingPoint.name());
                            if((sendAmount >= 1.0f && sendBombAmount >= 1.0f) || numBeforeSend == fleet.numShips())
                            {
                                //System.out.print("\n"+fleet.empire().name()+" Fleet at "+fleet.system().name()+" should attack "+target.name()+" allowBombers: "+allowBombers);
                                canStillSend = false;
                            }
                        }
                        else
                        {
                            canStillSend = false;
                        }
                    }
                    else
                    {
                        canStillSend = false;
                    }
                    if(!fleet.hasShips())
                    {
                        canStillSend = false;
                    }
                }
            }
        }
    }
    
    public void attackWithFleet(ShipFleet fl, StarSystem target, float amount, float bombAmount, boolean includeScouts, boolean includeFighters, boolean includeBombers, boolean includeColonizer, float needToKeep, boolean splitBySpeed)
    {
        /*if(fl.system() != null)
            System.out.println(empire.name()+" fleet at "+fl.system().name()+" sent to "+target.name()+" amount: "+amount+" bomb-amount: "+bombAmount+" keepBc: "+needToKeep);*/
        if(fl.system() == target)
            return;
        ShipDesignLab lab = empire.shipLab();
    
        float totalVal = 0;
        float topSpeedVal = 0;
        float fullPowerValue = combatPower(fl, target.empire());
        
        for (int i=0;i<fl.num.length;i++) {
            int num = fl.num(i);
            ShipDesign d = lab.design(i); 
            totalVal += num * d.cost();
            if(d.warpSpeed() == empire.tech().topSpeed())
                topSpeedVal += num * d.cost();
        }
        
        if(topSpeedVal / totalVal > 2.0 / 3.0)
            splitBySpeed = true;
        
        if(fl.isInTransit())
            splitBySpeed = false;
        
        ShipDesign Repeller = null;
        
        if(empire.generalAI().needScoutRepellers(false))
            Repeller = empire.shipDesignerAI().BestDesignToRepell();
        //when the system is colonizable we'll also leave at least one ship that can fight behind
        //No we don't. We need our ships to apply pressure.
        /*if(!fl.isInTransit() && !fl.system().isColonized() && empire.canColonize(fl.system()))
            needToKeep = max(needToKeep, 1);*/
        
        boolean handleEvent = false;
        if(!fl.isInTransit())
        {
            StarSystem current = fl.system();
            if (current.empire() == empire && current.hasEvent()) {
                if (current.eventKey().equals("MAIN_PLANET_EVENT_PIRACY")) {
                    handleEvent = true;
                }
                if (current.eventKey().equals("MAIN_PLANET_EVENT_COMET")) {
                    handleEvent = true;
                }
            }
        }
        
        for (int speed=(int)fl.slowestStackSpeed();speed<=(int)empire.tech().topSpeed();speed++)
        {
            boolean haveToDeploy = false;
            int[] counts = new int[ShipDesignLab.MAX_DESIGNS];
            for (int i=0;i<fl.num.length;i++) {
                int num = fl.num(i);
                ShipDesign d = lab.design(i); 
                if(d.warpSpeed()!=speed && splitBySpeed)
                    continue;
                if(!d.isArmed() && !d.hasColonySpecial() && !includeScouts)
                {
                    continue;
                }
                if(d == Repeller && num > 0)
                    num--;
                if(d.hasColonySpecial() && !includeColonizer)
                {
                    continue;
                }
                if(empire.shipDesignerAI().bombingAdapted(d) >= 0.75f && !includeBombers && !d.isColonyShip())
                {
                    continue;
                }
                if(empire.shipDesignerAI().fightingAdapted(d) > 0.75f && !includeFighters && !d.isColonyShip())
                {
                    continue;
                }
                if(!empire.sv.inShipRange(target.id) && d.range() < empire.scoutRange())
                {
                    continue;
                }
                if(!d.hasColonySpecial())
                    if(empire.shipDesignerAI().fightingAdapted(d) > 0 && empire.shipDesignerAI().bombingAdapted(d) == 0)
                        counts[i] = (int)Math.ceil(num * amount);
                    else if(empire.shipDesignerAI().fightingAdapted(d) > 0 && empire.shipDesignerAI().bombingAdapted(d) > 0)
                        counts[i] = max((int)Math.ceil(num * amount), (int)Math.ceil(num * bombAmount));
                    else
                        counts[i] = (int)Math.ceil(num * bombAmount);
                else
                    counts[i] = (int)Math.ceil(num * amount);
                if(needToKeep > 0 && ( (empire.shipDesignerAI().fightingAdapted(d) >= 0.5 && !d.isColonyShip()) || handleEvent) )
                {
                    int toKeep = (int)Math.ceil(needToKeep * num);
                    counts[i] -= toKeep;
                }   
                if(counts[i] > 0)
                {
                    haveToDeploy = true;
                    //System.out.println(empire.name()+" deploy "+counts[i]+" "+d.name()+" speed "+speed+" to "+target.name()+" splitBySpeed: "+splitBySpeed);
                    systemInfoBuffer.get(target.id).myBombardDamage += counts[i] * designBombardDamage(d, target);
                    systemInfoBuffer.get(target.id).myTotalPower += fullPowerValue * counts[i] / num;
                    if(d.hasColonySpecial())
                        systemInfoBuffer.get(target.id).colonizersEnroute++;
                    if(fl.destination() != null)
                    {
                        UpdateSystemInfo(fl.destination().id);
                        systemInfoBuffer.get(fl.destination().id).myBombardDamage -= counts[i] * designBombardDamage(d, fl.destination());
                        systemInfoBuffer.get(fl.destination().id).myTotalPower -= fullPowerValue * counts[i] / num;
                        if(d.hasColonySpecial())
                            systemInfoBuffer.get(fl.destination().id).colonizersEnroute--;
                    }
                }
            }
            if(haveToDeploy)
            {
                galaxy().ships.deploySubfleet(fl, counts, target.id);
            }
            if(!splitBySpeed)
                break;
        }
    }
    @Override
    public float bcValue(ShipFleet fl, boolean countScouts, boolean countFighters, boolean countBombers, boolean countColonizers) {
        float bc = 0;
        ShipDesignLab lab = fl.empire().shipLab();
        for (int i=0;i<fl.num.length;i++) {
            int num = fl.num(i);
            if (num > 0) {
                ShipDesign des = lab.design(i);
                float bcValueFactor = 1;
                if(des == null)
                    continue;
                if(des.range() == des.empire().scoutRange() && !des.hasColonySpecial() && countScouts)
                {
                    bc += (num * des.cost() * bcValueFactor);
                }
                if(countColonizers && des.hasColonySpecial())
                {
                    bc += (num * des.cost() * bcValueFactor);
                }
                if(countBombers)
                {
                    bcValueFactor = empire.shipDesignerAI().bombingAdapted(des);
                    bc += (num * des.cost() * bcValueFactor);
                }
                if(countFighters)
                {
                    bcValueFactor = empire.shipDesignerAI().fightingAdapted(des);
                    bc += (num * des.cost() * bcValueFactor);
                }
            }
        }
        //System.out.print("\n"+fl.empire().name()+" Fleet at "+fl.system().name()+" has BC: "+bc);
        return bc;
    }
    public float designBombardDamage(ShipDesign d, StarSystem sys) {
        if (!empire.sv.isColonized(sys.id))
            return 0;

        float damage = 0;
        ShipCombatManager mgr = galaxy().shipCombat();
        CombatStackColony planetStack = new CombatStackColony(sys.colony(), mgr);
        planetStack.num = 0; //set missile-bases to 0 because we are interested in killing power in this regard

        for (int j=0;j<ShipDesign.maxWeapons();j++)
            damage += d.wpnCount(j) * d.weapon(j).estimatedBombardDamage(d, planetStack);
        for (int j=0;j<d.maxSpecials();j++)
            damage += d.special(j).estimatedBombardDamage(d, planetStack);
        return damage;
    }
    public float totalFirePower()
    {
        float bombardPower = 0;
        for(ShipFleet fleet:empire.allFleets())
        {
            bombardPower+=fleet.expectedBombardDamage(galaxy().system(empire.homeSysId()), false);
        }
        return bombardPower;
    }
    public float firePowerNeededForAttack()
    {
        float firePowerNeeded = 0;
        for(Empire emp:empire.enemies())
        {
            firePowerNeeded += empire.generalAI().totalEmpirePopulationCapacity(emp) * 200;
        }
        return firePowerNeeded;
    }
    public float stationaryDefenseBudget()
    {
        float totalDefenseBC = empire.totalFleetCost() * (totalFirePower() - firePowerNeededForAttack()) / totalFirePower();
        if(firePowerNeededForAttack() == 0)
            totalDefenseBC = 0;
        return max(0, totalDefenseBC);
    }
    public float defenseBudgetForSystem(StarSystem sys, float totalBudget)
    {
        if(sys.empire() != empire)
            return 0;
        return totalBudget * sys.population() / empire.totalPlanetaryPopulation();
    }
    public int transportGauntletRounds(float speed) {
        switch((int)speed) {
            case 0: case 1: case 2: case 3: case 4:
                return 4;
            case 5: case 6:
                return 3;
            case 7: case 8:
                return 2;
            case 9: default:
                return 1;
        }
    }
    public StarSystem RetreatSystem(ShipFleet fl) {
        float shortestDistance = Float.MAX_VALUE;
        StarSystem best = null;
        for(StarSystem sys : empire.allySystems())
        {
            UpdateSystemInfo(sys.id);
            if(systemInfoBuffer.get(sys.id).enemyFightingPower > combatPower(fl, sys.empire()) + systemInfoBuffer.get(sys.id).myTotalPower)
                continue;
            if(fl.distanceTo(sys) < shortestDistance)
            {
                shortestDistance = fl.distanceTo(sys);
                best = sys;
            }
        }
        return best;
    }
    public StarSystem StageSystem(ShipFleet fl, StarSystem target) {
        float shortestDistance = Float.MAX_VALUE;
        StarSystem best = null;
        for (int id=0;id<empire.sv.count();id++)
        {
            StarSystem current = galaxy().system(id);
            Empire currEmp = current.empire();
            if(!fl.canReach(current))
                continue;
            if(currEmp != null && !currEmp.alliedWith(empire.id) && !empire.warEnemies().contains(currEmp))
                continue;
            if(current.monster() != null)
                continue;
            if(empire.enemies().contains(currEmp) && bridgeHeadConfidence(target) < 1)
                continue;
            UpdateSystemInfo(id);
            if(systemInfoBuffer.get(id).enemyFightingPower > combatPower(fl, target.empire()) + systemInfoBuffer.get(id).myTotalPower)
                continue;
            float distToTarget = target.distanceTo(current);
            //Foreign-systems need to be significantly closer to become a valid staging-point
            if(currEmp != empire)
                distToTarget *= 2;
            if(distToTarget < shortestDistance)
            {
                shortestDistance = distToTarget;
                best = current;
            }
        }
        return best;
    }
    public float bridgeHeadPower(StarSystem sys)
    {
        float biggestFleetPower = 0;
        float enemyBaseHP = 0;
        if(empire.sv.empire(sys.id) != null)
            enemyBaseHP = max(1, empire.sv.bases(sys.id))*empire.sv.empire(sys.id).tech().newMissileBase().maxHits();
        for(ShipFleet orbiting : sys.orbitingFleets())
        {
            if(orbiting.empire() == empire)
            {
                float ourEffectiveBombDamage = empire.governorAI().expectedBombardDamageAsIfBasesWereThere(orbiting, sys, 1);
                //System.out.println(galaxy().currentTurn()+" "+empire.name()+" "+sys.name()+" bombard-Damage: "+ourEffectiveBombDamage+" base-HP: "+enemyBaseHP);
                if(ourEffectiveBombDamage >= enemyBaseHP)
                {
                    float myFightingPower = combatPower(orbiting, sys.empire());
                    if(myFightingPower > biggestFleetPower)
                        biggestFleetPower = myFightingPower;
                }
            }
        }
        for(ShipFleet incoming : sys.incomingFleets())
        {
            if(incoming.empire() == empire)
            {
                float ourEffectiveBombDamage = empire.governorAI().expectedBombardDamageAsIfBasesWereThere(incoming, sys, 1);
                //System.out.println(galaxy().currentTurn()+" "+empire.name()+" "+sys.name()+" bombard-Damage: "+ourEffectiveBombDamage+" base-HP: "+enemyBaseHP);
                if(ourEffectiveBombDamage >= enemyBaseHP)
                {
                    float myFightingPower = combatPower(incoming, sys.empire());
                    if(myFightingPower > biggestFleetPower)
                        biggestFleetPower = myFightingPower;
                }
            }
        }
        return biggestFleetPower;
    }
    @Override
    public float bridgeHeadConfidence(StarSystem sys) {
        if(bridgeHeadConfidenceBuffer.containsKey(sys.id))
            return bridgeHeadConfidenceBuffer.get(sys.id);
        UpdateSystemInfo(sys.id);
        float knownSituation = bridgeHeadPower(sys) - systemInfoBuffer.get(sys.id).enemyFightingPower;
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" "+sys.name()+" bridgeHeadPower: "+bridgeHeadPower(sys)+" enemy: "+systemInfoBuffer.get(sys.id).enemyFightingBc);
        if(knownSituation <= 0)
            return 0;
        float totalEnemyFleet = 0;
        if(empire.sv.empire(sys.id) != null)
        {
            List<ShipFleet> fleets = galaxy().ships.allFleets(empire.sv.empire(sys.id).id);
            for (ShipFleet fl: fleets)
                totalEnemyFleet += combatPower(fl, empire);
        }
        for(ShipFleet fl:empire.enemyFleets())
        {
            if(fl.empire() == empire.sv.empire(sys.id))
            {
                //It is orbiting one of my systems, so it's unlikely it'll go back
                if(fl.inOrbit() && fl.system().empire() == empire)
                    totalEnemyFleet -= combatPower(fl, empire);
                //It is en route to one of my systems, so it's unlikely it'll go back
                if(fl.destination() != null && !fl.empire().tech().hyperspaceCommunications() && fl.destination().empire() == empire)
                    totalEnemyFleet -= combatPower(fl, empire);
            }
        }
        if(totalEnemyFleet <= 0)
            return 1;
        float uniqueTargets = 0;
        if(empire.sv.empire(sys.id) != null)
        {
            for(StarSystem enemysys : empire.sv.empire(sys.id).allColonizedSystems())
            {
                if(enemysys.colony() == null)
                    continue;
                boolean alreadyCounted = false;
                for(ShipFleet fl : enemysys.orbitingFleets())
                {
                    if(fl.empire() == empire)
                    {
                        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" has orbiting fleet at "+enemysys.name());
                        uniqueTargets++;
                        alreadyCounted = true;
                        break;
                    }
                }
                if(alreadyCounted)
                    continue;
                for(ShipFleet fl : enemysys.incomingFleets())
                {
                    if(fl.empire() == empire)
                    {
                        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" has incoming fleet at "+enemysys.name());
                        uniqueTargets++;
                        break;
                    }
                }   
            }
        }
        //count other enemies of them as more targets to split their attention between
        float myPower = empire.generalAI().smartPowerLevel();
        float allTheirEnemiesPower = myPower;
        for(Empire eno : empire.sv.empire(sys.id).warEnemies())
        {
            if(eno != empire)
            allTheirEnemiesPower += eno.militaryPowerLevel();
        }
        if(myPower > 0)
            uniqueTargets *= allTheirEnemiesPower / myPower;
        float confidence = min(1, knownSituation / (totalEnemyFleet / uniqueTargets));
        //System.out.println(galaxy().currentTurn()+" "+empire.name()+" "+sys.name()+" uniqueTargets: "+uniqueTargets+" knownSituation: "+knownSituation+" totalEnemyFleet: "+totalEnemyFleet+" confidence: "+confidence);
        bridgeHeadConfidenceBuffer.put(sys.id, confidence);
        return confidence;
    }
    @Override
    public boolean incomingInvasion()
    {
        for(Ship sh : empire.visibleShips())
        {
            if(empire.aggressiveWith(sh.empId()))
                if(!sh.nullDest() && galaxy().system(sh.destSysId()).empire() == empire)
                    if(sh.isTransport())
                        return true;
        }
        return false;
    }
    @Override
    public boolean underSiege()
    {
        boolean underSiege = false;
        for(StarSystem sys : empire.allColonizedSystems())
        {
            if(sys.colony() == null)
                continue;
            if(sys.enemyShipsInOrbit(empire))
            {
                underSiege = true;
                break;
            }
        }
        return underSiege;
    }
    float enemyMaintenance()
    {
        float highest = 0;
        for(Empire enemy : empire.contactedEmpires())
        {
            if(!empire.inShipRange(enemy.id))
                continue;
            if(empire.alliedWith(enemy.id))
                continue;
            if(enemy.militaryPowerLevel() <= empire.generalAI().smartPowerLevel())
                continue;
            if(enemy.shipMaintCostPerBC() > highest)
                highest = enemy.shipMaintCostPerBC();
        }
        return highest;
    }
    float avgFleetShield(ShipFleet fl) {
        float totalShield = 0;
        float totalVal = 0;
        for (int i=0;i<fl.num.length;i++) {
            int num = fl.num(i);
            if (num > 0) {
                ShipDesign des = empire.shipLab().design(i);
                if(des == null)
                    continue;
                totalVal += num * des.cost();
                totalShield += num * des.shieldLevel() * des.cost();
            }
        }
        if(totalVal > 0)
            return totalShield / totalVal;
        return 0;
    }
    float totalFleetHealth(ShipFleet fl) {
        float totalHP = 0;
        for (int i=0;i<fl.num.length;i++) {
            int num = fl.num(i);
            if (num > 0) {
                ShipDesign des = empire.shipLab().design(i);
                if(des == null)
                    continue;
                totalHP += num * des.hits();
            }
        }
        return totalHP;
    }
    @Override
    public FleetStats getFleetStats(ShipFleet fl) {
        FleetStats stats = new FleetStats();
        float totalShield = 0;
        float totalDefense = 0;
        float totalMissileDefense = 0;
        float totalSpecials = 0;
        float totalHP = 0;
        float totalCombatSpeed = 0;
        float totalWeighedHP = 0;
        float totalArmor = 0;
        for (int i=0;i<fl.num.length;i++) {
            int num = fl.num(i);
            if (num > 0) {
                ShipDesign des = empire.shipLab().design(i);
                if(des == null)
                    continue;
                totalHP += num * des.hits();
                totalShield += num * des.shieldLevel() * des.hits();
                totalDefense += num * (des.beamDefense() + des.empire().shipDefenseBonus()) * des.hits();
                totalMissileDefense += num * (des.missileDefense() + des.empire().shipDefenseBonus()) * des.hits();
                totalSpecials += num * des.getSpecialCount(true) * des.hits();
                totalCombatSpeed += num * des.combatSpeed() * des.hits();
                totalWeighedHP += num * des.hits() * des.hits();
                totalArmor += num * des.armor().tech().hitsAdj * des.hits();
            }
        }
        if(totalHP > 0)
        {
            stats.avgShield = totalShield / totalHP;
            stats.avgDefense = totalDefense / totalHP;
            stats.avgMissileDefense = totalMissileDefense / totalHP;
            stats.avgSpecials = totalSpecials / totalHP;
            stats.avgCombatSpeed = totalCombatSpeed / totalHP;
            stats.totalHP = totalHP;
            stats.avgHP = totalWeighedHP / totalHP;
            stats.avgArmor = totalArmor / totalHP;
        }
        return stats;
    }
    FleetStats getMonsterStats(List<CombatStack> monsters) {
        FleetStats stats = new FleetStats();
        float totalShield = 0;
        float totalDefense = 0;
        float totalMissileDefense = 0;
        float totalSpecials = 0;
        float totalHP = 0;
        float totalVal = 0;
        float totalCombatSpeed = 0;
        float totalWeighedHP = 0;
        for (CombatStack monster : monsters) {
            int num = monster.num;
            if (num > 0) {
                totalHP += num * monster.hits();
                totalVal += num * monster.hits();
                totalShield += num * monster.shieldLevel() * monster.hits();
                totalDefense += num * monster.beamDefense() * monster.hits();
                totalMissileDefense += num * monster.missileDefense() * monster.hits();
                totalSpecials += num * 2 * monster.hits();
                totalCombatSpeed += monster.maxMove();
                totalWeighedHP += num * monster.hits() * monster.hits();
            }
        }
        if(totalVal > 0)
        {
            stats.avgShield = totalShield / totalVal;
            stats.avgDefense = totalDefense / totalVal;
            stats.avgMissileDefense = totalMissileDefense / totalVal;
            stats.avgSpecials = totalSpecials / totalVal;
            stats.avgCombatSpeed = totalCombatSpeed / totalVal;
            stats.totalHP = totalHP;
            stats.avgHP = totalWeighedHP / totalVal;
            stats.avgArmor = totalHP / 600;
        }
        return stats;
    }
    float monsterFirePower(List<CombatStack> monsters, float shield, float defense, float missileDefense) {
        float total = 0;
        for (CombatStack monster : monsters) {
            total += monster.firePower(shield, defense, missileDefense);
        }
        return total;
    }
    float combatPower(ShipFleet attacker) {
        FleetStats undefended = new FleetStats();
        return combatPower(attacker, undefended);
    }
    float combatPower(ShipFleet attacker, Empire emp) {
        if(emp == null)
            emp = empire;
        return combatPower(attacker, empire.generalAI().getFleetStatsForEmpire(emp));
    }
    float combatPower(ShipFleet attacker, FleetStats defender) {
        FleetStats defenderStats = defender;
        FleetStats attackerStats = getFleetStats(attacker);
        float power = attacker.firepowerAntiShip(defenderStats.avgShield, defenderStats.avgDefense, defenderStats.avgMissileDefense);
        power *= Math.pow(1.26, attackerStats.avgSpecials);
        float shipsOfAvgPower = attackerStats.totalHP / attackerStats.avgHP;
        power *= (shipsOfAvgPower + 1) / (2 * shipsOfAvgPower);
        power *= attackerStats.avgArmor;
        return power;
    }
    public float combatPower(ShipFleet attacker, ShipFleet defender) {
        return combatPower(attacker, getFleetStats(defender));
    }
    float combatPower(SpaceMonster monster , ShipFleet defender) {
        FleetStats defenderStats = getFleetStats(defender);
        monster.initCombat();
        FleetStats monsterStats = getMonsterStats(monster.combatStacks());
        float power = monsterFirePower(monster.combatStacks(), defenderStats.avgShield, defenderStats.avgDefense, defenderStats.avgMissileDefense);
        power *= Math.pow(1.26, monsterStats.avgSpecials);
        float shipsOfAvgPower = monsterStats.totalHP / monsterStats.avgHP;
        power *= (shipsOfAvgPower + 1) / (2 * shipsOfAvgPower);
        power *= monsterStats.avgArmor;
        return power;
    }
    public float myTotalPower() {
        if(myTotalPower > -1)
            return myTotalPower;
        float power = 0;
        for(ShipFleet fl : empire.allFleets())
            power += combatPower(fl, fl);
        myTotalPower = power;
        return myTotalPower;
    }
}
