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
package rotp.model.tech;

import rotp.model.combat.CombatStack;
import rotp.model.empires.Empire;
import rotp.model.ships.ShipSpecialAmoebaEatShips;
import rotp.ui.combat.ShipBattleUI;

public final class TechAmoebaEatShips extends Tech {
	public int range = 1;

	public TechAmoebaEatShips(String typeId, int lv, int seq, boolean b, TechCategory c) {
		id(typeId, seq);
		typeSeq = seq;
		level = lv;
		cat = c;
		free = b;
		restricted = true;
		init();
	}
	@Override public void init()	{
		super.init();
		techType = Tech.MONSTER_SPECIAL;
		switch(typeSeq) {
			case 0:
				cost = 1000;
				size = 700;
				power = 1000;
				range = 1;
				break;
		}
	}
	@Override public boolean isMonsterTech()			{ return true; }
	@Override public float baseValue(Empire c)			{
		return c.ai().scientist().baseValue((TechBlackHole)TechLibrary.current().tech("BlackHole:0"));
	}
	@Override public boolean providesShipComponent()	{ return true; }
	@Override public void provideBenefits(Empire c)		{
		super.provideBenefits(c);
		ShipSpecialAmoebaEatShips sh = new ShipSpecialAmoebaEatShips(this);
		c.shipLab().addSpecial(sh);
	}
	@Override public void drawSpecialAttack(CombatStack source, CombatStack target, int wpnNum, float dmg) {
		ShipBattleUI ui = source.mgr.ui;
		if (ui == null)
			return;

		if (!source.mgr.showAnimations())
			return;

		if (target == null)
			return;

		if (!target.isShip() && !target.isColony())
			return;

		// only eats ships animations
		if (target.isShip()) {
			target.drawFadeOut(.025f);
			ui.paintAllImmediately();
			sleep(250);
		}

		source.mgr.performingStackTurn = false;
	}
}
