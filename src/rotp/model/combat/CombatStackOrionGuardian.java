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

import rotp.model.galaxy.SpaceMonster;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipComponent;

public class CombatStackOrionGuardian extends CombatStackMonster {
	public CombatStackOrionGuardian(SpaceMonster fl, String imageKey, Float monsterLevel, int designId) {
		super(fl, imageKey, monsterLevel, designId, false);
	}

	@Override public int optimalFiringRange(CombatStack tgt)	{ return 3; }
	@Override public int maxFiringRange(CombatStack tgt)	{
		if (roundsRemaining[0]>0)
			return 9;
		else if (wpnTurnsToFire[2] < 2)
			return 10;
		else
			return optimalFiringRange(tgt);
	}
	@Override public String	 name()				{ return text(name); }
	@Override public boolean immuneToStasis()	{ return true; }
	@Override public void reloadWeapons()		{
		for (int i=0;i<shotsRemaining.length;i++) 
			shotsRemaining[i] = 1;
		for (ShipComponent c: weapons)
			c.reload(); 
	};
	@Override public Color shieldBaseColor()	{ return Color.blue; }
	@Override public boolean hostileTo(CombatStack st, StarSystem sys)	{ return !st.isMonster(); }
	@Override public boolean selectBestWeapon(CombatStack target)	{
		if (target.destroyed())
			return false;
		if (currentWeaponCanAttack(target))
			return true;

		rotateToUsableWeapon(target);
		return currentWeaponCanAttack(target);
	}

}
