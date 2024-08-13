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
package rotp.model.galaxy;

public abstract class GuardianMonsters extends SpaceMonster {
	private static final long serialVersionUID = 1L;
	
	public GuardianMonsters(String name, int empId, Float speed, Float level) {
		super(name, empId, speed, level);
	}

	@Override public void	 plunder()						{ removeGuardian(); }
	@Override public boolean isMonsterGuardian()			{ return true; }
	@Override public boolean isGuardian()					{ return true; }

}
