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

//BR: Do not use: For Compatibility with the first GuardianJellyfish release
//Now named SpaceJellyfish
public class CombatStackGuardianJellyfish extends CombatStackMonster {

	public CombatStackGuardianJellyfish(SpaceMonster fl, String imageKey, Float monsterLevel, int designId, Color shieldC) {
		super(fl, imageKey, monsterLevel, designId, true, shieldC);
	}
	@Override public Color	 shieldBaseColor()					{ return Color.blue; }
}
