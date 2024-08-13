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
package rotp.model.galaxy;

import rotp.model.colony.Colony;
import rotp.model.combat.CombatStackSpacePirates;
import rotp.model.ships.ShipDesign;

// modnar: add Space Pirates random event
public class SpacePirates extends SpaceMonster {
    private static final long serialVersionUID = 1L;
    public SpacePirates(Float speed, Float level) {
        super("SPACE_PIRATES", -5, speed, level);
    }
    @Override
    public void initCombat() {
    	super.initCombat();
        addCombatStack(new CombatStackSpacePirates(this, travelSpeed(), stackLevel()));       
    }
	// modnar: pirates pillage colonies rather than destroy
	// half population, remove all factories, produce max waste
	// ?? add to combat stack numbers from factories pillaged ??
    @Override public void degradePlanet(StarSystem sys) { // was pillageColony
        Colony col = sys.colony();
        if (col != null) {
            sys.empire().lastAttacker(this);
			float prevPop = col.population();
            col.setPopulation(prevPop*0.5f); // half population
            col.industry().factories(0.0f); // remove all factories
            float maxWaste = sys.planet().maxWaste(); // produce max waste
            sys.planet().addWaste(maxWaste);
            sys.planet().removeExcessWaste();
        }        
    }
    // ShipMonster overriders
	@Override protected ShipDesign designRotP() {
		// TODO BR: SpacePirates Fake Design
		return null;
	}
}
