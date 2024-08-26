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

import java.awt.*;
import java.util.*;
import java.util.List;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.combat.*;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipDesign;
import rotp.model.tech.Tech;
import rotp.util.Base;

public class AIShipCaptain implements Base, ShipCaptain {
    private final Empire empire;
    private transient List<CombatStack> allies = new ArrayList<>();
    private transient List<CombatStack> enemies = new ArrayList<>();
    private CombatStack currentTarget = null;
    private boolean retreatImmediately = false;
    private boolean kiteMissiles = false;

    public List<CombatStack> allies() {
        if (allies == null)
            allies = new ArrayList<>();
        return allies;
    }
    public List<CombatStack> enemies() {
        if (enemies == null)
            enemies = new ArrayList<>();
        return enemies;
    }
    public AIShipCaptain (Empire c) {
        empire = c;
    }
    private ShipCombatManager combat()    { return galaxy().shipCombat(); }
    @Override
    public void performTurn(CombatStack stack) {
        ShipCombatManager mgr = galaxy().shipCombat();
        // missiles move during their target's turn
        // check if stack is still alive!
        if (stack.destroyed()) {
            mgr.turnDone(stack);
            return;
        }

        if (stack.isMissile()) {
            mgr.turnDone(stack);
            return;
        }

        if (stack.inStasis) {
            mgr.turnDone(stack);
            return;
        }

        if (empire.isPlayerControlled() && !combat().autoComplete) {
            mgr.turnDone(stack);
            return;
        }
        
        CombatStack prevTarget = null;
        retreatImmediately = false;
        kiteMissiles = false;
        
        boolean turnActive = true;
        while (turnActive) {
            float prevMove = stack.move;
            prevTarget = currentTarget;
            //ail: for moving we pick the target that is overall the most suitable, so that bombers move towards planet
            FlightPath bestPathToTarget;
            //ail: defend-stuff is problematic as stacks can be drawn out
            /*if((currentTarget == null || stack.movePointsTo(currentTarget) - stack.move > stack.maxFiringRange(currentTarget)) && stack.hasWard())
                bestPathToTarget = defendWardPath(stack, stack.ward());
            else*/
            bestPathToTarget = chooseTarget(stack, false, false);
            CombatStack tgtBeforeClose = currentTarget;
            //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+stack.fullName()+" performTurn.");
            if (stack.isColony() && stack.canAttack(currentTarget)) 
            {
                //System.out.print("\n"+galaxy().currentTurn()+" "+empire.name()+" "+stack.fullName()+" supposed to fire at: "+currentTarget.fullName());
                stack.target = currentTarget;
                mgr.performAttackTarget(stack);
                mgr.turnDone(stack);
                return;
            }
            //ail: if our target to move to is not the same as the target we can currently shoot at, we shoot before moving
            // check for retreating
            if(tgtBeforeClose != null && stack.movePointsTo(tgtBeforeClose) > stack.move + stack.optimalFiringRange(tgtBeforeClose))
            {  
                chooseTarget(stack, true, false);
                if (stack.canAttack(currentTarget)) 
                    performSmartAttackTarget(stack, currentTarget);
                currentTarget = tgtBeforeClose;
            }
            boolean shouldPerformKiting = false;
            if(stack.isShip())
            {
                for (int i=0;i<stack.numWeapons(); i++) {
                    if(stack.weapon(i).isMissileWeapon())
                        shouldPerformKiting = true;
                    if(tgtBeforeClose != null && stack.optimalFiringRange(tgtBeforeClose) > 1)
                        shouldPerformKiting = true;
                }
            }
            if(stack.repulsorRange() > 0)
                shouldPerformKiting = true;
            
            //check for immediate retreat
            if (wantToRetreat(stack) && stack.canRetreat()) {
                if(retreatImmediately)
                {
                    CombatStackShip shipStack = (CombatStackShip) stack;
                    StarSystem dest = retreatSystem(shipStack.mgr.system());
                    if (dest != null) {
                        mgr.retreatStack(shipStack, dest);
                        //System.out.print("\n"+stack.fullName()+" target: "+currentTarget.fullName()+" retreat because it wants to.");
                        return;
                    }
                }
            }
            if(kiteMissiles)
            {
                //System.out.print("\n"+stack.fullName()+" should be kiting now. Destination: "+findSafestPoint(stack));
                if (stack.mgr.autoResolve) {
                    Point destPt = findSafestPoint(stack);
                    if (destPt != null)
                        mgr.performMoveStackToPoint(stack, destPt.x, destPt.y);
                }
                else
                {
                    FlightPath bestPathToSaveSpot = findSafestPath(stack);
                    if(bestPathToSaveSpot != null)
                        mgr.performMoveStackAlongPath(stack, bestPathToSaveSpot);
                    //System.out.print("\n"+stack.fullName()+" Kiting performed: "+(bestPathToSaveSpot != null));
                }
            }
            
            //When we are defending and can't get into attack-range of the enemy, we let them come to us
            /*if(currentTarget != null)
                System.out.println(stack.fullName()+" target: "+currentTarget.fullName()+" distaftermove: "+(stack.movePointsTo(currentTarget) - stack.move)+" DistToBeAt: "+DistanceToBeAt(stack, currentTarget));*/
            // if we need to move towards target, do it now
            if(currentTarget != null && stack.movePointsTo(currentTarget) >= stack.move + stack.optimalFiringRange(currentTarget))
            {  
                if (wantToRetreat(stack) && stack.canRetreat()) {
                    CombatStackShip shipStack = (CombatStackShip) stack;
                    StarSystem dest = retreatSystem(shipStack.mgr.system());
                    if (dest != null) {
                        mgr.retreatStack(shipStack, dest);
                        //System.out.print("\n"+stack.fullName()+" target: "+currentTarget.fullName()+" retreat because it wants to.");
                        return;
                    }
                }
            }
            boolean moved = false;            
            if (currentTarget != null && !kiteMissiles) {
                boolean repulsorDefender = stack.repulsorRange() >= 1 && currentTarget.maxFiringRange(stack) <= stack.repulsorRange() && stack.hasWard() && !currentTarget.canCloak && !currentTarget.canTeleport();
                if(repulsorDefender)
                {
                    int y = stack.ward().y;
                    int x = stack.ward().x + 1;
                    if(x > 8)
                        x = stack.ward().x - 1;
                    if (stack.mgr.autoResolve) {
                        mgr.performMoveStackToPoint(stack, x, y);
                        moved = true;
                    }
                    else {
                        FlightPath bestPath = null;
                        List<FlightPath> validPaths = new ArrayList<>();
                        allValidPaths(stack.x,stack.y,x,y,9,stack, validPaths, bestPath);
                        if(!validPaths.isEmpty())
                        {
                            Collections.sort(validPaths,FlightPath.SORT);
                            mgr.performMoveStackAlongPath(stack, validPaths.get(0));
                            moved = true;
                        }
                    }
                    if(stack.x == x && stack.y == y)
                        moved = true;
                }
                if(!moved) {
                    if (stack.mgr.autoResolve) {
                        Point destPt = findClosestPoint(stack, currentTarget);
                        if (destPt != null)
                            mgr.performMoveStackToPoint(stack, destPt.x, destPt.y);
                    }
                    else if ((bestPathToTarget != null) && (bestPathToTarget.size() > 0)) {
                        mgr.performMoveStackAlongPath(stack, bestPathToTarget);
                    }
                }
            }
            
            // if can attack target this turn, fire when ready
            //ail: first look for ships as targets as we can fire our beams/missiles at them and then still drop bombs afterwards
            if(currentTarget != null && currentTarget.isColony())
            {
                chooseTarget(stack, false, true);
                if (stack.canAttack(currentTarget)) 
                    performSmartAttackTarget(stack, currentTarget);
                //now chhose our previous target again
                chooseTarget(stack, false, false);
            }
            if (stack.canAttack(currentTarget)) 
                performSmartAttackTarget(stack, currentTarget);
            else
            {
                //ail: if we couldn't attack our move-to-target, we try and see if anything else can be attacked from where we are
                chooseTarget(stack, true, false);
                if (stack.canAttack(currentTarget)) 
                    performSmartAttackTarget(stack, currentTarget);
            }
            
            if(currentTarget != null)
            {
                if(stack.movePointsTo(currentTarget) + stack.maxMove > currentTarget.optimalFiringRange(stack) + currentTarget.maxMove)
                    shouldPerformKiting = true;
                if(!currentTarget.canPotentiallyAttack(stack))
                    shouldPerformKiting = false;
                if(currentTarget.repairPct > 0)
                    shouldPerformKiting = false;
            }
         
            boolean enemyColonyPresent = false;
            if (stack.mgr.results().colonyStack != null && stack.mgr.results().colonyStack.colony.empire() != empire && stack.mgr.results().colonyStack.isArmed())
                enemyColonyPresent = true;
            
            //ail: only move away if I have fired at our best target and am a missile-user or have repulsors
            boolean atLeastOneWeaponCanStillFire = false;
            boolean allWeaponsCanStillFire = true;
            if(stack.isShip())
            {
                CombatStackShip shipStack = (CombatStackShip)stack;
                for (int i=0;i<stack.numWeapons(); i++) {
                    if(stack.weapon(i).groundAttacksOnly() && !enemyColonyPresent)
                        continue;
                    if(stack.weapon(i).isSpecial())
                        continue;
                    if(stack.shotsRemaining(i) < shipStack.weaponAttacks(i) || stack.weapon(i).isLimitedShotWeapon() && shipStack.roundsRemaining(i) < 1)
                    {
                        allWeaponsCanStillFire = false;
                    }
                    else
                    {
                        atLeastOneWeaponCanStillFire = true;
                    }
                }
            }
            
            //System.out.print("\n"+stack.fullName()+" shouldPerformKiting: "+shouldPerformKiting+" atLeastOneWeaponCanStillFire: "+atLeastOneWeaponCanStillFire);
            
            if (wantToRetreat(stack) && stack.canRetreat()) {
                CombatStackShip shipStack = (CombatStackShip) stack;
                StarSystem dest = retreatSystem(shipStack.mgr.system());
                if (dest != null) {
                    mgr.retreatStack(shipStack, dest);
                    //System.out.println(stack.fullName()+" retreat because it wants to after moving.");
                    return;
                }
            }
            
            if(shouldPerformKiting && !atLeastOneWeaponCanStillFire && !moved)
            {
                if (stack.mgr.autoResolve) {
                    Point destPt = findSafestPoint(stack);
                    if (destPt != null)
                        mgr.performMoveStackToPoint(stack, destPt.x, destPt.y);
                }
                else
                {
                    FlightPath bestPathToSaveSpot = findSafestPath(stack);
                    if(bestPathToSaveSpot != null)
                        mgr.performMoveStackAlongPath(stack, bestPathToSaveSpot);
                    //System.out.print("\n"+stack.fullName()+" Kiting performed: "+(bestPathToSaveSpot != null));
                }
                //turnActive = false;
            }
            // SANITY CHECK:
            // make sure we fall out if we haven't moved 
            // and we are still picking the same target
            if ((prevMove == stack.move) && (prevTarget == currentTarget)) {
                turnActive = false;
            }
            //ail: no more handling retreat from here, only kiting
            if(stack.maxMove == stack.move && allWeaponsCanStillFire && stack.isShip() && !moved)
            {
                if(currentTarget == null)
                {
                    if (stack.mgr.autoResolve) {
                        Point destPt = findSafestPoint(stack);
                        if (destPt != null)
                            mgr.performMoveStackToPoint(stack, destPt.x, destPt.y);
                    }
                    else
                    {
                        FlightPath bestPathToSaveSpot = findSafestPath(stack);
                        if(bestPathToSaveSpot != null)
                            mgr.performMoveStackAlongPath(stack, bestPathToSaveSpot);
                        //System.out.print("\n"+stack.fullName()+" No target-kite performed: "+(bestPathToSaveSpot != null));
                    }
                }
            }
        }
        mgr.turnDone(stack);
    }
   
    private boolean performSmartAttackTarget(CombatStack stack, CombatStack target)
    {
        boolean performedAttack = false;
        boolean initialTargetWasShip = false;
        if(target == null)
            return false;
        if(target.isShip())
            initialTargetWasShip = true;
        //1st run: fire only specials which are not repulsor or stasis-field
        for (int i=0;i<stack.numWeapons(); i++) {
            if(target == null)
                continue;
            if(!stack.weapon(i).isSpecial()
                    || !((CombatStackShip)stack).shipComponentCanAttack(target, i)
                    || stack.weapon(i).tech().isType(Tech.REPULSOR)
                    || stack.weapon(i).tech().isType(Tech.STASIS_FIELD))
                continue;
            if(((CombatStackShip)stack).shipComponentCanAttack(target, i))
            {
                stack.fireWeapon(target, i, true);
                chooseTarget(stack, true, initialTargetWasShip);
                target = currentTarget;
                performedAttack = true;
            }
        }
        //2nd run: fire non-special-weapons
        for (int i=0;i<stack.numWeapons(); i++) {
            if(target == null)
                continue;
            if(stack.weapon(i).isSpecial()
                    || !((CombatStackShip)stack).shipComponentCanAttack(target, i)
                    || (stack.weapon(i).isMissileWeapon() && stack.movePointsTo(target) > stack.maxFiringRange(target)))
                continue;
            if(stack.weapon(i).isMissileWeapon() && target.isShip() && stack.shotsRemaining(i) <= 2 && stack.movePointsTo(target) > stack.optimalFiringRange(target))
                continue;
            if(((CombatStackShip)stack).shipComponentCanAttack(target, i))
            {
                stack.fireWeapon(target, i, true);
                chooseTarget(stack, true, initialTargetWasShip);
                target = currentTarget;
                performedAttack = true;
            }
        }
        //3rd run: fire whatever is left, except missiles if we are too far
        for (int i=0;i<stack.numWeapons(); i++) {
            if(target == null)
                continue;
            if(stack.weapon(i).isMissileWeapon() && stack.movePointsTo(target) > stack.maxFiringRange(target))
                continue;
            if(stack.weapon(i).isMissileWeapon() && target.isShip() && stack.shotsRemaining(i) <= 2 && stack.movePointsTo(target) > stack.optimalFiringRange(target))
                continue;
            if(((CombatStackShip)stack).shipComponentCanAttack(target, i))
            {
                stack.fireWeapon(target, i, true);
                chooseTarget(stack, true, initialTargetWasShip);
                target = currentTarget;
                performedAttack = true;
            }
        }
        return performedAttack;
    }
    private  FlightPath chooseTarget(CombatStack stack, boolean onlyInAttackRange, boolean onlyShips) {
        if (!stack.canChangeTarget())
            return null;

        List<CombatStack> potentialTargets = new ArrayList<>();
        List<CombatStack> activeStacks = new ArrayList<>(combat().activeStacks());
        
        boolean allTargetsCloaked = true;
        for (CombatStack st: activeStacks) {
            if (stack.hostileTo(st, st.mgr.system()) && !st.inStasis)
            {
                potentialTargets.add(st);
                if(!st.cloaked)
                {
                    if(!st.isColony())
                        allTargetsCloaked = false;
                    else if(st.num > 0)
                        allTargetsCloaked = false;
                }
            }
        }
        FlightPath bestPath = null;
        CombatStack bestTarget = null;
        float maxDesirability = -1;
        for (CombatStack target : potentialTargets) {
            if(onlyInAttackRange && !stack.canAttack(target))
            {
                continue;
            }
            if(onlyShips && target.isColony())
            {
                continue;
            }
            if(target.inStasis)
                continue;
            if(target.cloaked && (!allTargetsCloaked || stack.hasWard()) && stack.isShip())
                continue;
            // pct of target that this stack thinks it can kill
            float killPct = max(stack.estimatedKillPct(target, false), expectedPopLossPct(stack, target)); 
            //reduce attractiveness of target depending on how much damage it already has incoming from missiles
            // threat level target poses to this stack (or its ward if applicable)
            CombatStack ward = stack.hasWard() ? stack.ward() : stack;
            // want to adjust threat upward as target gets closer to ward
            int distAfterMove = target.canTeleport() ? 1 : (int) max(1,target.movePointsTo(ward)-target.maxMove());
            //ail: We run best-target twice: Once to see where to move toward by ignoring distance, so we just assume we can reach it, and once after moving so we can see what we can actually shoot
            if(!onlyInAttackRange)
                distAfterMove = 1;
            float rangeAdj = 10.0f/distAfterMove;
            if(stack.isShip())
            {
                if(target.isShip())
                {
                    boolean canStillFireShipWeapon = false;
                    for (int i=0;i<stack.numWeapons(); i++) {
                        if(!stack.weapon(i).groundAttacksOnly() && stack.shotsRemaining(i) > 0)
                        {
                            canStillFireShipWeapon = true;
                        }
                    }
                    if(!canStillFireShipWeapon)
                        killPct = 0;
                }
                //no point in targetting the colony if we can't attack it either
                if(target.isColony())
                {
                    boolean canStillFireShipWeapon = false;
                    for (int i=0;i<stack.numWeapons(); i++) {
                        if(stack.shotsRemaining(i) > 0)
                        {
                            canStillFireShipWeapon = true;
                        }
                    }
                    if(!canStillFireShipWeapon)
                        killPct = 0;
                }
            }
            //System.out.print("\n"+stack.fullName()+" onlyships: "+onlyShips+" onlyInAttackRange: "+onlyInAttackRange+" looking at "+target.fullName()+" killPct: "+killPct+" rangeAdj: "+rangeAdj+" cnt: "+target.num+" target.designCost(): "+target.designCost());
            if (killPct > 0) {
                killPct = min(1,killPct);
                float adjustedKillPct = killPct;
                if(missileKillPct(target) >= 1.0f / target.num)
                    adjustedKillPct -= missileKillPct(target);
                float desirability = 0;
                float valueMod = 0;
                if(target.isMonster())
                    valueMod = 1; //Real value of monster doesn't matter. It just mustn't be 0!
                else if(target.isShip())
                    valueMod = target.designCost();
                else if(target.num > 0)
                    valueMod = target.designCost();
                else if(target.isColony())
                {
                    CombatStackColony csCol = (CombatStackColony) target;
                    valueMod = csCol.colony.population() * csCol.colony.empire().tech().populationCost() + csCol.colony.industry().factories() * csCol.colony.empire().tech().bestFactoryCost();
                    if(killPct > 0.9f && !empire.generalAI().allowedToBomb(target.mgr.system()) && !facingOverwhelmingForce(stack, true))
                        valueMod = 0;
                }
                if(adjustedKillPct > 0)
                    desirability = adjustedKillPct * max(1, target.num) * valueMod * rangeAdj;
                else
                    desirability = killPct * max(1, target.num) * valueMod * rangeAdj / 100;
                if(stack.isColony())
                    desirability *= 1 + target.estimatedKillPct(stack, false) * stack.designCost();
                if(target.totalHits() > 0 && killPct < target.hits() / target.totalHits())
                    desirability /= target.hits();
                //System.out.print("\n"+stack.fullName()+" onlyships: "+onlyShips+" onlyInAttackRange: "+onlyInAttackRange+" looking at "+target.fullName()+" killPct: "+killPct+" target.hits / target.totalHits(): "+target.hits() / target.totalHits()+" rangeAdj: "+rangeAdj+" cnt: "+target.num+" target.designCost(): "+target.designCost()+" desirability: "+desirability);
                if(target.isShip() && stack.isShip())
                {
                    boolean shouldPerformKiting = false;
                    for (int i=0;i<stack.numWeapons(); i++) {
                        if(stack.weapon(i).isMissileWeapon())
                            shouldPerformKiting = true;
                    }
                    if(stack.repulsorRange() > 0)
                        shouldPerformKiting = true;
                    if(stack.movePointsTo(target) + stack.maxMove > target.optimalFiringRange(stack) + target.maxMove)
                        shouldPerformKiting = true;
                    if(!target.canPotentiallyAttack(stack))
                        shouldPerformKiting = false;
                    if(shouldPerformKiting || preferClosestTarget(stack))
                        desirability /= stack.movePointsTo(target);
                }
                if(!target.canPotentiallyAttack(stack) && stack.isColony())
                {
                    if(!target.isColony() || onlyShips)
                        desirability /= 100;
                }
                if(empire.shipDesignerAI().bombingAdapted(stack.design()) < 0.5 && target.isColony() && !target.isArmed())
                    desirability = Float.MIN_VALUE;
                if(!stack.ignoreRepulsors() && stack.maxFiringRange(target) <= target.repulsorRange() && stack.movePointsTo(target) > 1)
                    desirability = -1;
                if (desirability > maxDesirability && valueMod > 0) {  // this might be a better target, adjust desirability for pathing
                    if (stack.mgr.autoResolve || onlyInAttackRange) {
                        bestTarget = target;
                        maxDesirability = desirability;
                    }
                    else if(!onlyInAttackRange) {
                        FlightPath path = findBestPathToAttack(stack, target);
                        if (path != null) {  // can we even path to this target?
                            int turnsToReachTarget = stack.canTeleport ? 1 : (int) Math.ceil(path.size() / stack.maxMove());
                            if (turnsToReachTarget > 0 && onlyInAttackRange)
                                desirability = desirability / turnsToReachTarget; // lower-value targets that can be attacked right away may be more desirable
                            desirability *= checkPathSafety(stack, path);
                            if (desirability > maxDesirability) {
                                bestPath = path;
                                bestTarget = target;
                                maxDesirability = desirability;
                            }
                        }
                    }
                }
            }
        }
        currentTarget = bestTarget;
        return bestPath;
    }
    public float checkPathSafety(CombatStack stack, FlightPath path)
    {
        if(path == null)
            return 0;
        float pathSafety = 1;
        //float threatValue = 0;
        //float totalValue = 0;
        for (int i=0;i<path.size();i++) 
        {
            Point coord = new Point(path.mapX(i), path.mapY(i));
            for (CombatStack st : combat().activeStacks()) {
                if (st.isMonster() || stack.empire().aggressiveWith(st.empire(), combat().system())) 
                {
                    if(st.movePointsTo(coord.x, coord.y) <= st.maxFiringRange(stack))
                    {
                        pathSafety -= st.estimatedKillPct(stack, true);
                    }
                }
            }
        }
        return pathSafety;
    }
    public Point findClosestPoint(CombatStack st, CombatStack tgt) {
        if (!st.canMove())
            return null;

        //ail: We will always want to go as close as possible because this increases hit-chance
        int targetDist = DistanceToBeAt(st, tgt);
        
        float maxDist = st.movePointsTo(tgt.x,tgt.y);
        if (maxDist <= targetDist)
            return null;

        int r = (int) st.move;
        if (st.canTeleport)
            r = 100;

        Point pt = new Point(st.x, st.y);

        int minMove = 0;
        for (int x1=st.x-r; x1<=st.x+r; x1++) {
            for (int y1=st.y-r; y1<=st.y+r; y1++) {
                if (combat().canMoveTo(st, x1, y1)) {
                    float dist = st.movePointsTo(tgt.x,tgt.y,x1,y1);
                    int move = st.movePointsTo(x1,y1);
                    if ((maxDist > targetDist) && (dist < maxDist)) {
                        maxDist = dist;
                        minMove = move;
                        pt.x = x1;
                        pt.y = y1;
                    }
                    else if ((dist <= targetDist)
                        && ((dist > maxDist)
                            || ((dist == maxDist) && (move < minMove)))) {
                        maxDist = dist;
                        minMove = move;
                        pt.x = x1;
                        pt.y = y1;
                    }
                }
            }
        }
        return pt;
    }
    public Point findSafestPoint(CombatStack st) {
        int bestX = st.x;
        int bestY = st.y;
        float safestScore = 0;
        for(int x = 0; x <= ShipCombatManager.maxX; ++x)
        {
            for(int y = 0; y <= ShipCombatManager.maxY; ++y)
            {
                float currentScore = 0;
                if(!st.mgr.validSquare(x,y))
                    continue;
                if(!st.canTeleport && !st.canMoveTo(x, y))
                    continue;
                boolean blocked = false;
                for(CombatStack other : st.mgr.activeStacks())
                {
                    if(other.x == x && other.y == y && other != st)
                    {
                        blocked = true;
                        continue;
                    }
                    if(other.canPotentiallyAttack(st) || !st.missiles().isEmpty())
                    {
                        currentScore += other.distanceTo(x, y);
                    }
                }
                if(blocked)
                    continue;
                /*float distFromCenter = sqrt(((x-4.5f)*(x-4.5f)) + ((y-3.5f)*(y-3.5f)));
                currentScore /= (5.7f + distFromCenter);*/
                if(currentScore > safestScore)
                {
                    safestScore = currentScore;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        //System.out.print("\nSafest space for "+st.fullName()+" x: "+bestX+" y: "+bestY+" score: "+safestScore);
        Point pt = new Point(st.x, st.y);
        pt.x = bestX;
        pt.y = bestY;
        return pt;
    }
    public FlightPath findSafestPath(CombatStack st) {
        FlightPath bestPath = null;
        Point pt = findSafestPoint(st);
        //System.out.println("Safest space for "+st.fullName()+" x: "+pt.x+" y: "+pt.y);
        List<FlightPath> validPaths = new ArrayList<>();
        allValidPaths(st.x,st.y,pt.x,pt.y,9,st, validPaths, bestPath);
        if(validPaths.isEmpty())
            return bestPath;
        Collections.sort(validPaths,FlightPath.SORT);
        //System.out.println("Paths found: "+validPaths.size());
        return validPaths.get(0);
    }
    public FlightPath defendWardPath(CombatStack st, CombatStack tgt)
    {
        List<FlightPath> validPaths = new ArrayList<>();
        FlightPath bestPath = null;
        int bestX = st.x;
        int bestY = st.y;
        float bestScore = Float.MAX_VALUE;
        for(int x = tgt.x-1; x <= tgt.x+1; ++x)
        {
            for(int y = tgt.y-1; y <= tgt.y+1; ++y)
            {
                float currentScore = Float.MAX_VALUE;
                if(!st.mgr.validSquare(x,y))
                    continue;
                boolean blocked = false;
                for(CombatStack other : st.mgr.activeStacks())
                {
                    if(other.x == x && other.y == y && other != st)
                    {
                        blocked = true;
                        continue;
                    }
                    if(other.hostileTo(st, StarSystem.TARGET_SYSTEM))
                    {
                        currentScore = other.distanceTo(x, y);
                    }
                }
                if(blocked)
                    continue;
                if(currentScore < bestScore)
                {
                    bestScore = currentScore;
                    bestX = x;
                    bestY = y;
                }
            }
        }
        //System.out.println("Best Square for "+st.fullName()+" x: "+bestX+" y: "+bestY+" wardScore: "+bestScore);
        allValidPaths(st.x,st.y,bestX,bestY,14,st, validPaths, bestPath);
        if(validPaths.isEmpty())
            return bestPath;
        Collections.sort(validPaths,FlightPath.SORT);
        return validPaths.get(0);
    }
    public FlightPath findBestPathToAttack(CombatStack st, CombatStack tgt) {
        if (!st.isArmed())
            return null;
        //we start at r = 1 and increase up to our optimal firing-range
        FlightPath bestPath = null;
        int distanceToBeAt = DistanceToBeAt(st, tgt);
        int distanceToBeAtLimit = distanceToBeAt;
        while(bestPath == null && distanceToBeAt <= max(st.maxFiringRange(tgt),distanceToBeAtLimit))
        {
            bestPath = findBestPathToAttack(st, tgt, distanceToBeAt);
            distanceToBeAt++;
        }
        return bestPath;
    }
    public int DistanceToBeAt(CombatStack st, CombatStack tgt)
    {
        int distanceToBeAt = st.optimalFiringRange(tgt);
        if(st.repulsorRange() > 0 && st.optimalFiringRange(tgt) > 1 && tgt.optimalFiringRange(st) < st.optimalFiringRange(tgt) && !tgt.ignoreRepulsors())
            distanceToBeAt = max(distanceToBeAt, 2);
        if(tgt.repulsorRange() > 0 && !st.ignoreRepulsors())
            distanceToBeAt = max(distanceToBeAt, 2);
        boolean shallGoForFirstStrike = true;
        if(galaxy().shipCombat().results().damageSustained(st.empire()) > 0
                || galaxy().shipCombat().results().damageSustained(tgt.empire()) > 0)
            shallGoForFirstStrike = false;
        boolean enemyCanAttackAnythingFromMe = false;
        for(CombatStack enemy : galaxy().shipCombat().activeStacks())
        {
            if(enemy.empire() != empire)
                continue;
            for(CombatStack mine : galaxy().shipCombat().activeStacks())
            {
                if(mine.empire() != empire)
                    continue;
                if(enemy.isArmed())
                {
                    if(enemy.maxFiringRange(mine) + enemy.maxMove <= enemy.distanceTo(mine.x(), mine.y()))
                    {
                        enemyCanAttackAnythingFromMe = true;
                        break;
                    }
                }
            }
        }
        if(!enemyCanAttackAnythingFromMe)
            shallGoForFirstStrike = true;
        if(st.maxMove <= tgt.maxMove || st.canTeleport)
            shallGoForFirstStrike = false;
        if(st.move < st.movePointsTo(tgt) - st.optimalFiringRange(tgt) && shallGoForFirstStrike)
        {
            int rangeToAssume = (int) (tgt.optimalFiringRange(st) + tgt.maxMove + 1);
            if(rangeToAssume <= st.movePointsTo(tgt))
            {
                distanceToBeAt = max(distanceToBeAt, rangeToAssume);
            }
        }
        return distanceToBeAt;
    }
    public static FlightPath findBestPathToAttack(CombatStack st, CombatStack tgt, int range) {
        if (st.movePointsTo(tgt) <= range) {
            return new FlightPath();
        }        
        int r = range;
        if (tgt.isColony() && st.hasBombs())
            r = 1;

        List<FlightPath> validPaths = new ArrayList<>();
        FlightPath bestPath = null;
        
        if (st.x > tgt.x) {
            if (st.y > tgt.y) {
                for (int x1=tgt.x+r; x1>=tgt.x-r; x1--) {
                    for (int y1=tgt.y+r; y1>=tgt.y-r; y1--) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            } 
            else {
                for (int x1=tgt.x+r; x1>=tgt.x-r; x1--) {
                    for (int y1=tgt.y-r; y1<=tgt.y+r; y1++) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            }
        } 
        else {
            if (st.y > tgt.y) {
                for (int x1=tgt.x-r; x1<=tgt.x+r; x1++) {
                    for (int y1=tgt.y+r; y1>=tgt.y-r; y1--) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            } 
            else {
                for (int x1=tgt.x-r; x1<=tgt.x+r; x1++) {
                    for (int y1=tgt.y-r; y1<=tgt.y+r; y1++) {
                        if (st.mgr.validSquare(x1,y1))
                            bestPath = allValidPaths(st.x,st.y,x1,y1,14,st, validPaths, bestPath); // get all valid paths to this point
                    }
                }
            }
        }
            
         // there is no path to get in optimal firing range of target!
        if (validPaths.isEmpty()) {
            // ail: no longer being content when we are within max-firing-range, we'll run a loop with slowly increasing range instead
            return null;
        }  

        Collections.sort(validPaths,FlightPath.SORT);
        //System.out.println("\n"+st.fullName()+" Paths found to "+tgt.fullName()+": "+validPaths.size());
        return validPaths.get(0);
    }
    @Override
    public boolean wantToRetreat(CombatStack currStack) {
        CombatStackColony col = combat().results().colonyStack;
        EmpireView colView = (col == null) ? null : currStack.empire().viewForEmpire(col.empire());
        boolean inPact = (colView != null) && colView.embassy().pact();
            
        if (currStack == col)
            return false;
        
        // PLAYER STACKS
        // 
        // when auto-resolving, retreat player stacks ONLY when not
        // retreating would violate a pact, or when the stack is unarmed
        // armed stacks will otherwise fight to the death, per player expectations
        if (!currStack.usingAI() && !currStack.mgr.allowRetreat)
        {
            boolean atLeastOneStackStillArmed = false;
            for(CombatStack cst : currStack.mgr.allStacks())
            {
                if(cst.empire() == empire)
                {
                    if(cst.isArmed())
                        atLeastOneStackStillArmed = true;
                }
                else if(!cst.missiles().isEmpty())
                    atLeastOneStackStillArmed = true;
            }
            return inPact || !atLeastOneStackStillArmed;
        }
     
        // AI STACKS
        //System.out.print("\n"+currStack.fullName()+" canRetreat: "+currStack.canRetreat()+" maneuverability: "+currStack.maneuverability);
        if (!currStack.canRetreat()) 
            return false;
        
        if (!currStack.canMove()) 
            return false;
        
        // threatened to be completely disabled by warp-dissipater
        /*System.out.print("\n"+currStack.fullName()+" currStack.maxMove(): "+currStack.maxMove()+" currStack.maneuverablity()"+currStack.maneuverablity()+" currStack.design().combatSpeed(): "+currStack.design().combatSpeed());
        if(currStack.maxMove() <= 1 && currStack.design().combatSpeed() > currStack.maxMove())
            return true;*/

        // don't retreat if we still have missiles in flight
        float killPct = 0;
        float maxHit = 0;
        List<CombatStack> activeStacks = new ArrayList<>(currStack.mgr.activeStacks());
        for (CombatStack st: activeStacks) {
            for (CombatStackMissile miss: st.missiles()) {
                if (miss.target == currStack && (st.isShip() || st.isMonster()))
                {
                    float hitPct;
                    hitPct = (5 + miss.attackLevel - miss.target.missileDefense()) / 10;
                    hitPct = max(.05f, hitPct);
                    hitPct = min(hitPct, 1.0f);
                    killPct += ((miss.maxDamage()-miss.target.shieldLevel())*miss.num*hitPct)/(miss.target.maxStackHits()*miss.target.num);
                    maxHit += (miss.maxDamage() - currStack.shieldLevel()) * miss.num; //don't use hitPct for max-hit as we have to expect the worst in this case
                    //System.out.println(currStack.fullName()+" will be hit by missiles for approx "+killPct+" dmg: "+maxHit+" hp: "+currStack.hits()+" threshold: "+(1.0f / miss.missile.shots()));
                    if((killPct > 1.0f / miss.missile.shots() && maxHit >= currStack.hits()) || (currStack.num == 1 && maxHit >= currStack.hits()))
                    {
                        Point safestPoint = findSafestPoint(currStack);
                        if(miss.maxMove * Math.max(1.0, miss.moveRate) + 0.7 < miss.distanceTo((float)safestPoint.x, (float)safestPoint.y))
                        {
                            kiteMissiles = true;
                            //System.out.println(currStack.fullName()+" should kite missiles because "+(miss.maxMove*miss.moveRate+0.7)+" at x:"+miss.x()+" y:"+miss.y()+" < "+miss.distanceTo((float)safestPoint.x, (float)safestPoint.y)+" to x:"+safestPoint.x+" y: "+safestPoint.y+" Moverate: "+miss.moveRate );
                        }
                        else
                        {
                            retreatImmediately = true; //when we have incoming missiles we can't do damage first
                            return true;
                        }
                    }
                }
                // If our missiles are about to kill something, we must not retreat!
                if (miss.owner == currStack)
                {
                    maxHit += (miss.maxDamage() - currStack.shieldLevel()) * miss.num;
                    if(maxHit > miss.target.hits())
                        return false;
                }
            }
        }
        
        // if stack is pacted with colony and doesn't want war, then retreat
        // ail: Whether I want a war or not depends on whether the other faction is an enemy, not on relation!
        if ((colView != null) && !empire.enemies().contains(colView.empire()))  
            return true;
        
        // don't retreat if all enemies can only target planets
        boolean canBeTargeted = false;
        boolean canTarget = false;
        for (CombatStack st: activeStacks) {
            if (st.canPotentiallyAttack(currStack))
                canBeTargeted = true;
            if(currStack.canPotentiallyAttack(st))
                canTarget = true;
        }
        //System.out.print("\n"+currStack.mgr.system().name()+" "+currStack.fullName()+" canBeTargeted: "+canBeTargeted);
        if (!canBeTargeted)
            return false;
        if(!canTarget)
            return true;
        
        if (facingOverwhelmingForce(currStack, false)) {
            log(currStack.toString()+" retreating from overwhelming force");
            return true;
        }

        return false;
    }
    public boolean facingOverwhelmingForce(CombatStack stack, boolean skipColonyBombCheck) {
        // build list of allies & enemies
        allies().clear(); enemies().clear();
        for (CombatStack st : combat().activeStacks()) {
            if (st.isMonster()) 
                enemies.add(st);
            else {
                if (stack.empire().alliedWith(id(st.empire())))
                {
                    allies().add(st);
                }
                else if (stack.empire().aggressiveWith(st.empire(), combat().system()))
                    enemies().add(st);
            }
        }
        // calculate ally kills & deaths
        float allyKillTime = 0;
        float enemyKillTime = 0;
        float enemyKillTimeWithoutHeal = Float.MAX_VALUE;
        float bomberKillTime = 0;
        
        float dpsOnColony = 0;
        boolean enemyHasRepulsor = false;
        boolean weCounterRepulsor = false;
        boolean enemyHasWarpDissipator = false;
        
        List<CombatStack> friends = new ArrayList<>();
        List<CombatStack> temp = new ArrayList<>(allies());
        for (CombatStack ally: temp) {
            if (ally!=null && ally.isArmed())
                friends.add(ally);
        }
        if (!friends.contains(stack))
            friends.add(stack);
        List<CombatStack> foes = new ArrayList<>();
        temp = new ArrayList<>(enemies());
        for (CombatStack enemy: temp) {
            if (enemy!=null && enemy.isArmed())
                foes.add(enemy);
            if(enemy.isColony())
            {
                for(CombatStack friend : friends)
                {
                    if(empire.shipDesignerAI().bombingAdapted(friend.design()) < 0.5)
                        continue;
                    float currentDamage = min(1.0f, expectedPopLossPct(friend, enemy));
                    dpsOnColony += currentDamage;
                    if(currentDamage > 0 && (friend.canCloak || friend.canTeleport()))
                        weCounterRepulsor = true;
                }
            }
        }

        int foesBlockPlanet = 0;
        CombatStackColony col = combat().results().colonyStack;
        
        for (CombatStack st1 : foes) {
            if(st1.repulsorRange() > 0)
                enemyHasRepulsor = true;
            if(st1.isShip() && st1.design().hasWarpDissipator())
                enemyHasWarpDissipator = true;
            if(col != null && col.empire() == st1.empire() && col.mgr.currentStack() != null)
                if(col.movePointsTo(st1) == 1)
                    foesBlockPlanet++;
            if(st1.inStasis || (st1.maneuverablity() == 0 && st1.isShip()))
                continue;
            boolean previousCloakingState = st1.cloaked;
            st1.cloaked = false; //decloack in our mind for estimates
            float pctOfMaxHP = 0;
            pctOfMaxHP = ((st1.num-1) * st1.maxStackHits() + st1.hits()) / (st1.num * st1.maxStackHits());
            float damagePerTurn = 0;
            for (CombatStack st2: friends) {
                if(st2.inStasis)
                    continue;
                float killPct = max(st2.estimatedKillPct(st1, false), expectedPopLossPct(st2, st1));
                if(st2.maxFiringRange(st1) <= st1.repulsorRange() && st1.maxFiringRange(st2) > 1 && !st2.canCloak && !st2.canTeleport())
                {
                    killPct = 0;
                }
                if(st2.maxMove < st1.maxMove && (st2.isShip() || st2.isMonster()))
                {
                    float mySpeedRange = st2.maxMove + maxFiringRange(st2, st1, true);
                    float enemySpeedRange = st1.maxMove + maxFiringRange(st1, st2, true);
                    if(st2.initiative() <= st1.initiative() || maxFiringRange(st2, st1, true) < maxFiringRange(st1, st2, true))
                        killPct *= max(0, (2 * mySpeedRange - enemySpeedRange) / mySpeedRange);
                }
                damagePerTurn += killPct;
                if(st1.maxFiringRange(st2) <= st2.repulsorRange() && st2.maxFiringRange(st1) > 1 && !st1.canCloak && !st1.canTeleport())
                {
                    pctOfMaxHP = 0;
                }
            }
            damagePerTurn += missileKillPct(st1);
            float healPerTurn = 0;
            if(st1.isShip())
            {
                CombatStackShip ship = (CombatStackShip)st1;
                healPerTurn = ship.designShipRepairPct() / st1.num;
            } 
            else if (st1.isMonster())
            {
                healPerTurn = st1.repairPct;
            }
            damagePerTurn -= healPerTurn;
            //System.out.println(stack.mgr.system().name()+" "+st1.fullName()+" takes "+damagePerTurn+" damage per turn with heal. heal per turn: "+healPerTurn);
            if(damagePerTurn > 0)
                allyKillTime += pctOfMaxHP / min(damagePerTurn, 1.0f);
            else
            {
                allyKillTime = Float.MAX_VALUE;
                break;
            }
            st1.cloaked = previousCloakingState;
        }
        
        CombatStack invulnerableFriend = null;
        
        for (CombatStack st1 : friends) {
            if(st1.inStasis || (st1.maneuverablity() == 0 && st1.isShip()))
                continue;
            boolean previousCloakingState = st1.cloaked;
            st1.cloaked = false;
            float pctOfMaxHP = ((st1.num-1) * st1.maxStackHits() + st1.hits()) / (st1.num * st1.maxStackHits());
            float topShipHP = st1.hits() / st1.maxStackHits() / st1.num;
            float damagePerTurn = 0;
            float damagePerTurnWithoutHeal = 0;
            for (CombatStack st2: foes) {
                if(st2.inStasis)
                    continue;
                float killPct = max(st2.estimatedKillPct(st1, st2.isShip()), expectedPopLossPct(st2, st1));
                //System.out.println(stack.mgr.system().name()+" "+stack.fullName()+" raw killpct: "+killPct);
                if(st2.maxFiringRange(st1) <= st1.repulsorRange() && st1.maxFiringRange(st2) > 1 && !st2.canCloak && !st2.canTeleport())
                {
                    killPct = 0;
                }
                if(st2.maxMove < st1.maxMove && (st2.isShip() || st2.isMonster()))
                {
                    float mySpeedRange = st2.maxMove + maxFiringRange(st2, st1, true);
                    float enemySpeedRange = st1.maxMove + maxFiringRange(st1, st2, true);
                    if(st2.initiative() <= st1.initiative() || maxFiringRange(st2, st1, true) < maxFiringRange(st1, st2, true))
                        killPct *= max(0, (2 * mySpeedRange - enemySpeedRange) / mySpeedRange);
                }
                //System.out.println(stack.mgr.system().name()+" "+stack.fullName()+" kite-adapted killpct: "+killPct);
                damagePerTurn += killPct;
                if(st1.maxFiringRange(st2) <= st2.repulsorRange() && st2.maxFiringRange(st1) > 1 && !st1.canCloak && !st1.canTeleport())
                {
                    pctOfMaxHP = 0;
                }
            }
            damagePerTurn += missileKillPct(st1);
            float healPerTurn = 0;
            if(st1.isShip())
            {
                CombatStackShip ship = (CombatStackShip)st1;
                healPerTurn = ship.designShipRepairPct() / st1.num;
            }
            else if (st1.isMonster())
            {
                healPerTurn = st1.repairPct;
            }
            damagePerTurnWithoutHeal = damagePerTurn;
            damagePerTurn -= healPerTurn;
            //System.out.println(stack.mgr.system().name()+" "+st1.fullName()+" takes "+damagePerTurn+" damage per turn with heal. heal per turn: "+healPerTurn);
            if(stack == st1 && damagePerTurnWithoutHeal > 0)
                enemyKillTimeWithoutHeal = min(enemyKillTimeWithoutHeal, topShipHP / min(damagePerTurnWithoutHeal, 1.0f));
            if(damagePerTurn > 0)
            {
                enemyKillTime += pctOfMaxHP / min(damagePerTurn, 1.0f);
                if(empire.shipDesignerAI().bombingAdapted(st1.design()) >= 0.5)
                    bomberKillTime += pctOfMaxHP / min(damagePerTurn, 1.0f);
            }
            else
            {
                if(st1.isColony())
                    invulnerableFriend = st1;
                enemyKillTime = Float.MAX_VALUE;
                if(empire.shipDesignerAI().bombingAdapted(st1.design()) >= 0.5)
                    bomberKillTime = Float.MAX_VALUE;
                break;
            }
            st1.cloaked = previousCloakingState;
        }
        //If we have an invulnerable friend, we should retreat and let him do the work. Due to the rule-change we will even stay where we are.
        if(invulnerableFriend != null && invulnerableFriend != stack && friends.size() > 2)
            return true;
        
        /*if(dpsOnColony > 0)
            System.out.print("\n"+stack.mgr.system().name()+" "+stack.fullName()+" allyKillTime: "+allyKillTime+" enemyKillTime: "+enemyKillTime+" bomberKillTime: "+bomberKillTime+" dpsOnColony: "+dpsOnColony+" col dies in: "+1 / dpsOnColony+" foesBlockPlanet: "+foesBlockPlanet);*/
        if(!skipColonyBombCheck)
            if(dpsOnColony * bomberKillTime > 1 && (!enemyHasRepulsor || weCounterRepulsor) && foesBlockPlanet < 5)
                return false;
        
        //System.out.println(stack.mgr.system().name()+" "+stack.fullName()+" allyKillTime: "+allyKillTime+" enemyKillTime: "+enemyKillTime+" enemyKillTimeWithoutHeal: "+enemyKillTimeWithoutHeal);
        if (enemyKillTime == allyKillTime)
        {
            if(col != null && col.empire() == empire)
                return false;
            else
                return true;
        }
        if(stack.empire().isPlayer())
        {
            if(col != null && col.empire() == empire)
            {
                enemyKillTime *= options().playerDefenseConfidence();
                enemyKillTimeWithoutHeal *= options().playerDefenseConfidence();
            }
            else
            {
                enemyKillTime *= options().playerAttackConfidence();
                enemyKillTimeWithoutHeal *= options().playerAttackConfidence();
            }
        }
        else
        {
            if(col != null && col.empire() == empire)
            {
                enemyKillTime *= options().aiDefenseConfidence();
                enemyKillTimeWithoutHeal *= options().aiDefenseConfidence();
            }
            else
            {
                enemyKillTime *= options().aiAttackConfidence();
                enemyKillTimeWithoutHeal *= options().aiAttackConfidence();
            }
        }
        if (!enemyHasWarpDissipator)
            if(enemyKillTimeWithoutHeal < 2)
                return allyKillTime > enemyKillTime;
            else
                return false;
        else
            return allyKillTime > enemyKillTime;
    }
    @Override
    public StarSystem retreatSystem(StarSystem sys) {
        float speed = empire.tech().topSpeed();
        //ail: first try to use the staging-point for the system we are currently retreating from so we don't retreat to system with enemies
        int sysId = empire.optimalStagingPoint(sys, 1);
        //ail: only if that fails take overall closest system
        if(sysId == StarSystem.NULL_ID)
            sysId = empire.alliedColonyNearestToSystem(sys, speed);
        return galaxy().system(sysId);
    }
    @Override
    public FlightPath pathTo(CombatStack st, int x1, int y1) {
        List<FlightPath> validPaths = allValidPathsTo(st,x1,y1);
        if (validPaths.isEmpty())
            return null;

        Collections.sort(validPaths,FlightPath.SORT);
        return validPaths.get(0);
    }
    public List<FlightPath> allValidPathsTo(CombatStack st, int x1, int y1) {
        List<FlightPath> validPaths = new ArrayList<>();
        allValidPaths(st.x, st.y, x1, y1, (int)st.maxMove, st, validPaths, null);
        return validPaths;
    }
    public static FlightPath allValidPaths(int x0, int y0, int x1, int y1, int moves, CombatStack stack, List<FlightPath> validPaths, FlightPath bestPath) {
        FlightPath updatedBestPath = bestPath;
        ShipCombatManager mgr = stack.mgr;
        int gridW = ShipCombatManager.maxX+3;

        // all squares containing ships, asteroids, etc or non-traversable
        // can also check for enemy repulsor beam effects
        boolean[] valid = mgr.validMoveMap(stack);

        int startX = x0 + 1;
        int startY = y0 + 1;
        int endX = x1 + 1;
        int endY = y1 + 1;

        // based on general direction to travel, find most straightforward path priority
        int[] pathDeltas = bestPathDeltas(startX, startY, endX, endY);

        int start = (startY*gridW)+startX;
        int end = (endY*gridW)+endX;

        List<Integer> path = new ArrayList<>();

        loadValidPaths(start, end, valid, moves, validPaths, path, pathDeltas, gridW, updatedBestPath);
        return updatedBestPath;
    }
    private static int[] bestPathDeltas(int c0, int c1) {
        int w = FlightPath.mapW;
        return bestPathDeltas(c0%w, c0/w, c1%w, c1/w);
    }
    private static int[] bestPathDeltas(int x0, int y0, int x1, int y1) {
        if (x1 < x0) {
            if (y1 < y0)
                return FlightPath.nwPathPriority;
            else if (y1 > y0)
                return FlightPath.swPathPriority;
            else
                return FlightPath.wPathPriority;
        }
        else if (x1 > x0) {
            if (y1 < y0)
                return FlightPath.nePathPriority;
            else if (y1 > y0)
                return FlightPath.sePathPriority;
            else
                return FlightPath.ePathPriority;
        }
        else {
            if (y1 < y0)
                return FlightPath.nPathPriority;
            else
                return FlightPath.sPathPriority;
        }
    }
    private static FlightPath loadValidPaths(int curr, int end, boolean[] valid, int moves, List<FlightPath> paths, List<Integer> currPath, int[] deltas, int gridW, FlightPath bestPath) {
        FlightPath updatedBestPath = bestPath;
        if (curr == end) {
            if (currPath.size() <= pathSize(bestPath)) {
                FlightPath newPath = new FlightPath(currPath, gridW);
                paths.add(newPath);
                updatedBestPath = newPath;
            }
            return updatedBestPath;
        }
        int[] basePaths = FlightPath.basePathPriority;

        int remainingMoves = moves - 1;
        for (int dir=0;dir<deltas.length;dir++) {
            int next = curr+deltas[dir];

            if (valid[next]) {
                // are we at the end? if so create FP and fall out
                if (next == end) {
                    currPath.add(next);
                    if (currPath.size() <= pathSize(bestPath)) {
                        FlightPath newPath = new FlightPath(currPath, gridW);
                        paths.add(newPath);
                        updatedBestPath = newPath;
                    }
                }
                else if (remainingMoves > 0) {
                    int minMovesReq = moveDistance(next,end,gridW);
                    int minPossibleMoves = minMovesReq + currPath.size() + 1;
                    int bestPathSize = pathSize(updatedBestPath);
                    if ((minPossibleMoves < bestPathSize) && (minMovesReq <= remainingMoves)) {
                        int baseDir = 0;
                        for (int i=0; i<basePaths.length;i++) {
                            if (basePaths[i] == deltas[dir]) {
                                baseDir = i; 
                                break;
                            }
                        }
                        List<Integer> nextPath = new ArrayList<>(currPath);
                        nextPath.add(next);
                        boolean[] nextValid = Arrays.copyOf(valid, valid.length);
                        nextValid[curr] = false;
                        nextValid[curr + basePaths[(baseDir+1)%8]] = false;
                        nextValid[curr + basePaths[(baseDir+7)%8]] = false;
                        if (baseDir %2 == 0) {
                            nextValid[curr + basePaths[(baseDir+6)%8]] = false;
                            nextValid[curr + basePaths[(baseDir+2)%8]] = false;
                        }
                        int [] pathDeltas = bestPathDeltas(next, end);
                        updatedBestPath = loadValidPaths(next, end, nextValid, remainingMoves, paths, nextPath, pathDeltas, gridW, updatedBestPath);
                    }
                }
            }
        }
        return updatedBestPath;
    }
    private static int pathSize(FlightPath fp) {
        return fp == null ? 999 : fp.size();
    }
    private static int moveDistance(int pt0, int pt1, int w) {
        int x0 = pt0 % w;
        int y0 = pt0 / w;
        int x1 = pt1 % w;
        int y1 = pt1 / w;
        return Math.max(Math.abs(x0-x1), Math.abs(y0-y1));
    }
    public float expectedBombardDamage(CombatStackShip ship, CombatStackColony colony) {
        int num = ship.num;
        float damage = 0.0f;

        ShipDesign d = ship.design();
        for (int j=0;j<ShipDesign.maxWeapons();j++)
        {
            if(d.weapon(j).noWeapon())
                continue;
            float dmg = num * d.wpnCount(j) * d.weapon(j).estimatedBombardDamage(d, colony);
            if(ship.roundsRemaining(j) > 0)
                dmg /= d.weapon(j).bombardAttacks();
            damage += dmg;
        }
        for (int j=0;j<d.maxSpecials();j++)
            damage += d.special(j).estimatedBombardDamage(d, colony);
        return damage;
    }
    public float expectedBioweaponDamage(CombatStackShip ship, CombatStackColony colony) {
        int num = ship.num;
        float popLoss = 0.0f;

        ShipDesign d = ship.design();
        for (int j=0;j<ShipDesign.maxWeapons();j++) {
            if(d.weapon(j).noWeapon())
                continue;
            if(ship.roundsRemaining(j) > 0)
                popLoss += (num * d.wpnCount(j) * d.weapon(j).estimatedBioweaponDamage(ship, colony) / d.weapon(j).bombardAttacks()); //divide by bombard-attacks as the return-value is for orbital-bombard, not during-combat-bombard
        }
        return popLoss;
    }
    public float expectedPopulationLoss(CombatStackShip ship, CombatStackColony colony) {
        float popLost = 0;
        float bombDamage = expectedBombardDamage(ship, colony);
        if (colony.num == 0)
            popLost = bombDamage / 200;
        else
            popLost = bombDamage / 400;
        popLost += expectedBioweaponDamage(ship, colony);
        float pct = (5 + ship.attackLevel() - colony.bombDefense()) / 10;
        pct = max(.05f, pct);
        return popLost * pct;
    }
    public float expectedPopLossPct(CombatStack source, CombatStack target) {
        if (!source.isShip())
            return 0;
        if (!target.isColony())
            return 0;
        
        CombatStackShip ship = (CombatStackShip) source;
        CombatStackColony colony = (CombatStackColony) target;
        
        if (colony.destroyed())
            return 0;
        
        float popLoss = expectedPopulationLoss(ship, colony);
        return popLoss/colony.colony.population();
    }
    @Override
    public boolean useSmartRangeForBeams()
    {
        return true;
    }
    public int maxFiringRange(CombatStack attacker, CombatStack defender, boolean ignoreWeaponsWithAmmo) {
        int maxRange = 0;
        for (int i=0;i<attacker.numWeapons();i++) {
            ShipComponent wpn = attacker.weapon(i);
            if (wpn.groundAttacksOnly() && !defender.isColony())
                continue;
            if(ignoreWeaponsWithAmmo && wpn.isLimitedShotWeapon())
                continue;
            if (!attacker.shipComponentIsOutOfMissiles(i))
                maxRange = max(maxRange,wpn.range());
        }
        if(maxRange <= defender.repulsorRange())
            maxRange = 0;
        return maxRange;
    }
    //If the enemy has repulsors that we can't counter, they could mess with us so we'd rather just go straight for the closest target
    public boolean preferClosestTarget(CombatStack st)
    {
        boolean enemyHasRepulsor = false;
        for(CombatStack enemy : enemies())
        {
            if(enemy.repulsorRange() > 0)
                enemyHasRepulsor = true;
        }
        boolean canByPassRepulsor = false;
        if(st.cloaked || st.canTeleport())
            canByPassRepulsor = true;
        return enemyHasRepulsor && !canByPassRepulsor;
    }
    
    public float missileKillPct(CombatStack target)
    {
        if(target.missiles().isEmpty())
            return 0;
        float killPct = 0;
        for (CombatStackMissile miss: target.missiles()) {
            float hitPct;
            hitPct = (5 + miss.attackLevel - miss.target.missileDefense()) / 10;
            hitPct = max(.05f, hitPct);
            hitPct = min(hitPct, 1.0f);
            killPct += ((miss.maxDamage()-miss.target.shieldLevel())*miss.num*hitPct)/(miss.target.maxStackHits()*miss.target.num);
        }
        return min(1.0f, killPct);
    }
}
