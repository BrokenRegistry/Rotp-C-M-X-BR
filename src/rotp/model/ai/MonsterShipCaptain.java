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
package rotp.model.ai;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.combat.CombatStack;
import rotp.model.combat.CombatStackColony;
import rotp.model.combat.CombatStackShip;
import rotp.model.combat.FlightPath;
import rotp.model.combat.ShipCombatManager;
import rotp.model.empires.Empire;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipDesign;
import rotp.util.Base;

public class MonsterShipCaptain implements Base, ShipCaptain {
	private final Empire empire;
	private SpaceMonster monster; // for potential later use

	public MonsterShipCaptain (SpaceMonster spaceMonster)	{
		monster = spaceMonster;
		empire  = monster.empire();
	}

	private ShipCombatManager combat()	{ return galaxy().shipCombat(); }

	@Override public void performTurn(CombatStack stack)		{
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

		// check for retreating
		if (wantToRetreat(stack)) {
			CombatStackShip shipStack = (CombatStackShip) stack;
			StarSystem dest = retreatSystem(shipStack.mgr.system());
			if (dest != null) {
				mgr.retreatStack(shipStack, dest);
				return;
			}
		}
		
		CombatStack prevTarget = null;
		
		boolean turnActive = true;
		while (turnActive) {
			float prevMove = stack.move;
			prevTarget = stack.target;
			FlightPath bestPathToTarget = chooseTarget(stack);
			// if we need to move towards target, do it now
			if (stack.target != null) {
				if (stack.mgr.autoResolve) {
					Point destPt = findClosestPoint(stack, stack.target);
					if (destPt != null)
						mgr.performMoveStackToPoint(stack, destPt.x, destPt.y);
				}
				else if ((bestPathToTarget != null) && (bestPathToTarget.size() > 0)) {
					mgr.performMoveStackAlongPath(stack, bestPathToTarget);
				}
			}

			// if can attack target this turn, fire when ready
			if (stack.canAttack(stack.target)) 
				mgr.performAttackTarget(stack);
		 
			// SANITY CHECK:
			// make sure we fall out if we haven't moved 
			// and we are still picking the same target
			if ((prevMove == stack.move) && (prevTarget == stack.target)) {
				turnActive = false;
			}
		}
		mgr.turnDone(stack);
	}
	@Override public boolean wantToRetreat(CombatStack stack) 	{ return false; }
	@Override public StarSystem retreatSystem(StarSystem sys)	{ return null; }
	@Override public FlightPath pathTo(CombatStack st, int x1, int y1)	{
		List<FlightPath> validPaths = allValidPathsTo(st, x1, y1);
		if (validPaths.isEmpty())
			return null;

		Collections.sort(validPaths, FlightPath.SORT);
		return validPaths.get(0);
	}
	private FlightPath chooseTarget(CombatStack stack)	{
		if (!stack.canChangeTarget())
			return null;

		List<CombatStack> potentialTargets = new ArrayList<>();
		List<CombatStack> activeStacks = new ArrayList<>(combat().activeStacks());

		for (CombatStack st: activeStacks) {
			if (stack.hostileTo(st, st.mgr.system()) && !st.inStasis)
				potentialTargets.add(st);
		}
		FlightPath bestPath = null;
		CombatStack bestTarget = null;
		float maxDesirability = -1;
		float threatLevel = 0;
		boolean currentCanBomb = false;
		for (CombatStack target : potentialTargets) {
			// pct of target that this stack thinks it can kill
			float killPct = max(stack.estimatedKillPct(target, false), expectedPopLossPct(stack, target)); 
			// threat level target poses to this stack (or its ward if applicable)
			CombatStack ward = stack.hasWard() ? stack.ward() : stack;
			// want to adjust threat upward as target gets closer to ward
			int distAfterMove = target.canTeleport() ? 1 : (int) max(1,target.movePointsTo(ward)-target.maxMove());
			// treat those who can move to bombing range (distAfterMove == 1) as maximum threats
			if (ward.isColony()) {
				CombatStackColony colony = (CombatStackColony) ward;
				float popLossPct  =  expectedPopLossPct(target, colony); 
				float baseLossPct = target.estimatedKillPct(colony, false);
				float maxLossPct = max(popLossPct,baseLossPct);
				// if this is the first potential target that can reach and damage the colony, 
				// ignore any previous selected targets
				if ((!currentCanBomb) && (distAfterMove <= 1) && (maxLossPct > 0.05f)) {
					threatLevel = maxLossPct;					 
					currentCanBomb = true;
					bestTarget = null;
					maxDesirability = -1;
				}
				// if we have a target that can actually bomb us, ignore any future
				// targets that cannot yet
				else if (currentCanBomb && (distAfterMove > 1)) {
					threatLevel = 0;
				}
				// this and no previous targets can yet bomb our colony, so evaluate
				// based on threat and distance
				else {
					float rangeAdj = 10.0f/distAfterMove;
					threatLevel = rangeAdj * maxLossPct;					 
				} 
			}
			else {
				float rangeAdj = 10.0f/distAfterMove;
				threatLevel = rangeAdj * target.estimatedKillPct(ward, false);  
			}
			if (killPct > 0) {
				killPct = min(1,killPct);
				float desirability = max((10000* threatLevel * threatLevel * killPct), .01f);
				if (desirability > maxDesirability) {  // this might be a better target, adjust desirability for pathing
					if (stack.mgr.autoResolve) {
						bestTarget = target;
						maxDesirability = desirability;
					}
					else if (stack.isColony()) {
						bestTarget = target;
						maxDesirability = desirability;
					}
					else {
						FlightPath path = findBestPathToAttack(stack, target);
						if (path != null) {  // can we even path to this target?
							int turnsToReachTarget = stack.canTeleport ? 1 : (int) Math.ceil(path.size() / stack.maxMove());
							if (turnsToReachTarget > 0)
								desirability = desirability / turnsToReachTarget; // lower-value targets that can be attacked right away may be more desirable
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
		stack.target = bestTarget;
		return bestPath;
	}
	private Point findClosestPoint(CombatStack st, CombatStack tgt)	{
		if (!st.canMove())
			return null;

		int targetDist = st.optimalFiringRange(tgt);
		if (tgt.isColony() && st.hasBombs())
			targetDist = 1;

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
	private static FlightPath findBestPathToAttack(CombatStack st, CombatStack tgt)	{
		if (!st.isArmed())
			return null;
		int r = st.optimalFiringRange(tgt);
		return findBestPathToAttack(st, tgt, r);
	}
	private static FlightPath findBestPathToAttack(CombatStack st, CombatStack tgt, int range)	{
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
			// are we within max firing range? if so, go with that
			if (st.movePointsTo(tgt) <= st.maxFiringRange(tgt)) 
				return new FlightPath();		  
			return null;
		}  

		Collections.sort(validPaths,FlightPath.SORT);
		//System.out.println("Paths found: "+validPaths.size());
		return validPaths.get(0);
	}
	private List<FlightPath> allValidPathsTo(CombatStack st, int x1, int y1)	{
		List<FlightPath> validPaths = new ArrayList<>();
		allValidPaths(st.x, st.y, x1, y1, (int)st.maxMove, st, validPaths, null);
		return validPaths;
	}
	private static FlightPath allValidPaths(int x0, int y0, int x1, int y1, int moves,
				CombatStack stack, List<FlightPath> validPaths, FlightPath bestPath) {
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
	private static int pathSize(FlightPath fp)			{ return fp == null ? 999 : fp.size(); }
	private static int[] bestPathDeltas(int c0, int c1)	{
		int w = FlightPath.mapW;
		return bestPathDeltas(c0%w, c0/w, c1%w, c1/w);
	}
	private static int[] bestPathDeltas(int x0, int y0, int x1, int y1)	{
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
	private static FlightPath loadValidPaths(int curr, int end, boolean[] valid, int moves,
			List<FlightPath> paths, List<Integer> currPath, int[] deltas, int gridW, FlightPath bestPath)	{
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
	private static int moveDistance(int pt0, int pt1, int w)	{
		int x0 = pt0 % w;
		int y0 = pt0 / w;
		int x1 = pt1 % w;
		int y1 = pt1 / w;
		return Math.max(Math.abs(x0-x1), Math.abs(y0-y1));
	}
	private float expectedBombardDamage(CombatStackShip ship, CombatStackColony colony)		{
		int num = ship.num;
		float damage = 0.0f;

		ShipDesign d = ship.design();
		for (int j=0;j<ShipDesign.maxWeapons();j++)
			damage += (num * d.wpnCount(j) * d.weapon(j).estimatedBombardDamage(d, colony));
		for (int j=0;j<d.maxSpecials();j++)
			damage += d.special(j).estimatedBombardDamage(d, colony);
		return damage;
	}
	private float expectedBioweaponDamage(CombatStackShip ship, CombatStackColony colony)	{
		int num = ship.num;
		float popLoss = 0.0f;

		ShipDesign d = ship.design();
		for (int j=0;j<ShipDesign.maxWeapons();j++)
			popLoss += (num * d.wpnCount(j) * d.weapon(j).estimatedBioweaponDamage(ship, colony));
		return popLoss;
	}
	private float expectedPopulationLoss(CombatStackShip ship, CombatStackColony colony)	{
		float popLost = 0;
		float bombDamage = expectedBombardDamage(ship, colony);
		if (colony.num == 0)
			popLost = bombDamage / 200;
		else
			popLost = bombDamage / 400;
		
		float bioDamage = expectedBioweaponDamage(ship, colony);

		return popLost+bioDamage;
	}
	private float expectedPopLossPct(CombatStack source, CombatStack target)				{
		if (!(source.isMonster() || source.isShip()))
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
}
