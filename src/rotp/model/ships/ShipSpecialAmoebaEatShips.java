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
package rotp.model.ships;

import rotp.model.combat.CombatStack;
import rotp.model.combat.CombatStackColony;
import rotp.model.combat.CombatStackShip;
import rotp.model.combat.ShipCombatManager;
import rotp.model.galaxy.ShipFleet;
import rotp.model.tech.TechAmoebaEatShips;

public final class ShipSpecialAmoebaEatShips extends ShipSpecial {
	private static final long serialVersionUID = 1L;
	public ShipSpecialAmoebaEatShips(TechAmoebaEatShips t) {
		tech(t);
		sequence(t.level + .05f);
	}
	@Override public TechAmoebaEatShips tech()	{ return (TechAmoebaEatShips) super.tech(); }
	@Override public boolean isWeapon()			{ return true; }
	@Override public int	 range()			{ return tech().range; }
	@Override public boolean canAttackPlanets()	{ return true; }
	@Override public boolean canAttackShips()	{ return true; }
	@Override public float	 estimatedKills(CombatStack src, CombatStack tar, int num) { return tar.num; }
	@Override public void	 fireUpon(CombatStack source, CombatStack st, int count, ShipCombatManager mgr) {
		// BR: NOT TESTED The Amoeba captain do the job
		if (st.isShip()) {
			st.mgr.destroyStack(st);
		}
		else if (st.isColony()) {
			CombatStackColony cStack = (CombatStackColony) st;
			st.mgr.destroyStack(st);
			ShipFleet monster = ((CombatStackShip) source).fleet();
			monster.degradePlanet(st.mgr.system());
			cStack.colonyDestroyed = true;
		}
	}
 }
