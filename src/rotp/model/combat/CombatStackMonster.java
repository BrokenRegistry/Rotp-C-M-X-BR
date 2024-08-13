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
import java.util.ArrayList;
import java.util.List;

import rotp.model.ai.MonsterShipCaptain;
import rotp.model.empires.Empire;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.model.game.GameSession;
import rotp.model.ships.ShipComponent;

public class CombatStackMonster extends CombatStackShip {
	private final SpaceMonster monster;
	private final boolean orionGuardian;
	private final boolean fusionGuardian;
	private final boolean moo1Monster;
	private final boolean rotPMonster;
	private final boolean fusionMonster;
	
	public final List<ShipComponent> weapons = new ArrayList<>();

	protected String name;

	public CombatStackMonster(SpaceMonster m, String key, Float level, int desId, boolean fusion) {
		super(m, desId, GameSession.instance().galaxy().shipCombat());
		orionGuardian	= m.isGuardian();
		fusionGuardian	= m.isMonsterGuardian();
		moo1Monster		= options().isMoO1Monster();
		fusionMonster	= fusion;
		rotPMonster		= !(fusionMonster || moo1Monster);
		monster	= m;
		name  	= key;
		image	= image(name);
		captain = new MonsterShipCaptain(monster);
	}
	@Override protected void initDesign(int id)	{ design = fleet.design(id);}
	@Override public boolean isShip()			{ return false; }
	@Override public boolean isMonster()		{ return true; }
	@Override public boolean isArmed()			{ return true; }
	@Override public String	 name()				{
		if (fusionGuardian)
			return text("PLANET_" + name);
		else
			return text(name);
	}
	@Override public Color shieldBaseColor()	{ return Color.red; }
	@Override public void recordKills(int num)	{  }
	@Override public void becomeDestroyed()		{ destroyed = true; num = 0;}
	@Override public void endTurn()				{
		if (rotPMonster) {
			if (!destroyed())
				finishMissileRemainingMoves();
			List<CombatStackMissile> missiles = new ArrayList<>(targetingMissiles);
			for (CombatStackMissile miss : missiles)
				miss.endTurn();
		}
		else
			super.endTurn();
	}
	@Override public float initiative()			{
		if (rotPMonster)
			return 1;
		else
			return super.initiative();
	}
	@Override public void loseShip()			{
		if (rotPMonster) {
			int lost = maxStackHits() > 0 ? 1 : num;
			hits(maxStackHits());
			shield = maxShield;
			num = max(0, num - lost);
			if (destroyed() && (mgr != null))
				mgr.destroyStack(this);
		}
		super.loseShip();
	}
	@Override public boolean canRetreat()		{ return false; }
	@Override public boolean retreatToSystem(StarSystem s)				{ return false; }
	@Override public boolean canPotentiallyAttack(CombatStack target)	{
		Empire emp = target.empire;
		return emp != null; // You won't them attacking other wandering monsters!
	}
	@Override public boolean hostileTo(CombatStack st, StarSystem sys)	{ return !st.isMonster(); }
	@Override public boolean selectBestWeapon(CombatStack target)		{
		if (rotPMonster)
			return false;
		else
			return super.selectBestWeapon(target);

	}

	public SpaceMonster spaceMonster()	{ return monster; }
}
