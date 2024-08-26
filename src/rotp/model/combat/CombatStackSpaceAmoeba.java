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
package rotp.model.combat;

import java.awt.Color;

import rotp.model.ai.AmoebaShipCaptain;
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.galaxy.SpaceAmoeba;

public class CombatStackSpaceAmoeba extends CombatStackMonster {
	private static final int DAMAGE_FOR_SPLIT = 500;
	public float speed;
	public float monsterLevel;
	public CombatStackSpaceAmoeba(SpaceAmoeba amoeba, String imageKey, Float level, int desId, Color shieldC) {
		super(amoeba, imageKey, level, desId, false, shieldC);
		if (level == null)
			monsterLevel = options().monstersLevel();
		else
			monsterLevel = level;
		num = 1;
		if (monsterLevel == 1f)
			maxStackHits(3500);
		else
			maxStackHits(1500);			
		hits(maxStackHits());
		streamProjectorHits(0); // BR:
		if (options().isMoO1Monster()) {
			maxMove = move = 2;
			beamDefense = 1;
			missileDefense = 1;	 
		}
		else {
			maxMove = move = 2;
			beamDefense = 1;
			missileDefense = 1;	 
		}

		reversed = random() < .5;
		image = image(imageKey);
		scale = 1.5f;
	}

	@Override protected ShipCaptain getCaptain()		{
		if (options().isMoO1Monster())
			return super.getCaptain();
		else
			return new AmoebaShipCaptain((SpaceAmoeba) fleet());
	}
	@Override public void beginTurn() {
		super.beginTurn();
		if (options().isMoO1Monster())
			return;
		// ok, we are splitting
		if ((maxStackHits() - hits()) >= DAMAGE_FOR_SPLIT) {
			float newMaxHits = (maxStackHits() - DAMAGE_FOR_SPLIT) / 2;
			maxStackHits(newMaxHits);
			hits(newMaxHits);
			((AmoebaShipCaptain)captain).splitAmoeba(this);
		}
	}
	@Override public boolean canEat(CombatStack st)	{
		return (st instanceof CombatStackShip) || (st instanceof CombatStackColony);
	}
	@Override public boolean ignoreRepulsors()	{ return true; }
	@Override public boolean canAttack(CombatStack target)  { 
		if (target.destroyed()) 
			return false;
		if (target.isColony() && !target.isArmed())
			return false;
		return (x == target.x) && (y == target.y);
	}
	@Override public boolean selectBestWeapon(CombatStack target)	{ return canAttack(target); }
	@Override public void fireWeapon(CombatStack target)	{
		if (options().isMoO1Monster())
			super.fireWeapon(target);
		else
			if ((x == target.x) && (y == target.y)) 
				eatShips(target);
	}
	@Override public boolean moveTo(int x1, int y1)	{
		CombatStack potentialFood = mgr.stackAt(x1, y1);
		boolean stillAlive = super.moveTo(x1, y1);
		
		// if we made it successfully to the new dest
		// and there happens to be a ship here, eat it
		if (stillAlive) 
			eatShips(potentialFood);
		return stillAlive;
	}
	@Override protected float takeDamage(float damage, float shieldAdj)	{
		if (inStasis)
			return 0;
		attacked = true;

		// max damage that will trigger a split
		float maxDamage = hits()+DAMAGE_FOR_SPLIT-maxStackHits();
		float actualDamage = min(maxDamage, damage);
		hits(hits() - actualDamage);
		
		// if we are on smallest form and are reduced < 0, we are dead
		if (hits() <= 0) {
			num = 0;
			mgr.destroyStack(this);
			return damage;
		}
		
		return actualDamage; 
	}
	@Override public Color shieldBaseColor()	{ return Color.yellow; }
	@Override public void becomeDestroyed()		{ destroyed = true; num = 0;}

	public void eatShips(CombatStack st) {
		if (st == null)
			return;
		if (!st.isShip() && !st.isColony())
			return;
		
		// only eats ships
		if (st.isShip()) {
			st.drawFadeOut(.025f);
			st.mgr.destroyStack(st);
		}
		else if (st.isColony()) {
			CombatStackColony cStack = (CombatStackColony) st;
			st.mgr.destroyStack(st);
			spaceMonster().degradePlanet(st.mgr.system());
			cStack.colonyDestroyed = true;
		}

		st.num = 0;

		// stop and enjoy the meal
		move = 0;
		if (st.mgr.showAnimations())
			st.mgr.ui.paintAllImmediately();
	}
}