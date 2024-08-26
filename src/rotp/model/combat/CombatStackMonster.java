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
import rotp.model.ai.interfaces.ShipCaptain;
import rotp.model.empires.Empire;
import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.model.game.GameSession;
import rotp.model.ships.ShipDesign;

public class CombatStackMonster extends CombatStackShip {
	//private final SpaceMonster monster;
	private final boolean fusionGuardian;
	private final boolean moo1Monster;
	private final boolean rotPMonster;
	private final boolean fusionMonster;
	private final Color	  shieldColor;
	private final String  monsterKey;

	public CombatStackMonster(SpaceMonster m, String key, Float level, int desId, boolean fusion, Color shieldC) {
		super(m, desId, GameSession.instance().galaxy().shipCombat());
		fusionGuardian	= m.isFusionGuardian();
		moo1Monster		= options().isMoO1Monster();
		fusionMonster	= fusion;
		rotPMonster		= !(fusionMonster || moo1Monster);
		shieldColor		= shieldC;
		monsterKey  	= key;
		image			= image(monsterKey);
	}
	@Override protected ShipDesign getDesign(int id)	{ return fleet().design(id); }
	@Override protected ShipCaptain getCaptain()		{ return new MonsterShipCaptain(spaceMonster()); }
	@Override public boolean isShip()					{ return false; }
	@Override public boolean isMonster()				{ return true; }
	@Override public boolean isArmed()					{ return true; }
	@Override public String	 name()						{
		if (fusionGuardian)
			return text("PLANET_" + monsterKey);
		else
			return text(monsterKey);
	}
	@Override public Color shieldBaseColor()			{ return shieldColor; }
	@Override public void recordKills(int num)			{  }
	@Override public void endTurn()						{
		if (!destroyed())
			finishMissileRemainingMoves();
		List<CombatStackMissile> missiles = new ArrayList<>(targetingMissiles);
		for (CombatStackMissile miss : missiles)
			miss.endTurn();
	}
	@Override public float initiative()					{
		if (rotPMonster)
			return 1;
		else
			return super.initiative();
	}
	@Override public boolean canRetreat()				{ return false; }
	@Override public boolean retreatToSystem(StarSystem s)				{ return false; }
	@Override public boolean canPotentiallyAttack(CombatStack target)	{
		Empire emp = target.empire();
		return emp != null; // You won't them attacking other wandering monsters!
	}
	@Override public boolean hostileTo(CombatStack st, StarSystem sys)	{ return !st.isMonster(); }
	@Override public boolean selectBestWeapon(CombatStack target)		{
		if (rotPMonster)
			return false;
		else
			return super.selectBestWeapon(target);

	}
	@Override public int optimalFiringRange(CombatStack tgt)			{
		return max(1, super.optimalFiringRange(tgt));
	}

	public final Color shieldColor()			{ return shieldColor; }
	public final SpaceMonster spaceMonster()	{ return (SpaceMonster) fleet(); }
	public final String monsterKey()			{ return monsterKey; }
}
