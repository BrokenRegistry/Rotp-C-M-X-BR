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

import static rotp.ui.UserPreferences.loadWithNewOptions;

import java.awt.geom.Point2D.Float;
import java.util.List;

import rotp.model.empires.Empire;
import rotp.model.game.GameSession;
import rotp.model.game.IGameOptions;
import rotp.ui.game.SetupRaceUI;

// BR: Added symmetric galaxies functionalities
// moved modnar companion worlds here with some more random positioning
// added some comments to help my understanding of this class
public class GalaxyCopy extends GalaxyShape {
	private static final long serialVersionUID = 1L;
	private IGameOptions options;
	private float maxScaleAdj;
	private float nebulaSizeMult;
	private int numStarSystems;
	private String selectedGalaxySize;
	private List<Nebula> nebulas;
	private Empire[] empires;
	private StarSystem[] starSystem;

	// ========== Constructors and Initializers ==========
	//
	public GalaxyCopy (IGameOptions newO) {
		options	= newO;
	}
	public void copy (GameSession s) {
		IGameOptions oldO = s.options();
		String newRace	= options.selectedPlayerRace();
		String oldRace	= oldO.selectedPlayerRace();
		numOpponents	= oldO.selectedNumberOpponents();
		numEmpires		= numOpponents + 1;
		nebulaSizeMult	= oldO.nebulaSizeMult();

		if (loadWithNewOptions.get()) {
			initForNewOptions(s);
		} else {
			initForOldOptions(s);
		}
		selectedGalaxySize = s.options().selectedGalaxySize();
		copyGalaxy(s.galaxy());
		// Check total number of player race
		if (countRace(newRace) >= SetupRaceUI.MAX_RACES) {
			// replace first occurrence with old player race
			swapRaces(newRace, oldRace);
		}
	}
	private void initForOldOptions(GameSession s) {
		// Copy what's needed from new options
		s.options().selectedPlayer().race = options.selectedPlayerRace();
		s.options().selectedHomeWorldName(options.selectedHomeWorldName());
		s.options().selectedLeaderName	 (options.selectedLeaderName());
		options = s.options();
	}
	private void initForNewOptions(GameSession s) {
		// Copy what's needed from old options
		IGameOptions oldO = s.options();
		options.copyForRestart(oldO);
		copyRaces(oldO);
	}
	// ========== Public Getters ==========
	//
	@Override public float maxScaleAdj()	{ return maxScaleAdj; }
	@Override public int totalStarSystems()	{ return numStarSystems; }
	@Override public IGameOptions options() { return options; }

	public List<Nebula> nebulas()		{ return nebulas; }
	public Empire[] empires()			{ return empires; }
	public Empire empires(int i)		{ return empires[i]; }
	public StarSystem[] starSystem()	{ return starSystem; }
	public StarSystem starSystem(int i)	{ return starSystem[i]; }
	public String selectedGalaxySize()	{ return selectedGalaxySize; }
	public float nebulaSizeMult()		{ return nebulaSizeMult; }
	public int numNearBySystem()		{ return 2; }
	
	// ========== Private Methods ==========
	private void copyGalaxy(Galaxy g) {
		width	= g.width();
		height	= g.height();
		
		maxScaleAdj		= g.maxScaleAdj();
		numStarSystems	= g.numStarSystems();
		nebulas			= g.nebulas();
		empires			= g.empires();
		starSystem		= g.starSystems();
	}
	private void copyRaces (IGameOptions oldO) {
		int i=0;
		while (oldO.selectedOpponentRace(i) != null) {
			options.selectedOpponentRace(i, oldO.selectedOpponentRace(i));
			i++;
		}		
	}
	private int countRace (String race) {
		int count = 0;
		for (String r : options().selectedOpponentRaces()) {
			if (r != null && r.equalsIgnoreCase(race)) count++;
		}
		return count;
	}
	private void swapRaces (String rSearch, String rReplace) {
		String[] races = options().selectedOpponentRaces();
		for (int i=0; i<races.length; i++) {
			if (races[i] != null && races[i].equalsIgnoreCase(rSearch)) {
				races[i] = rReplace;
				return;
			}
		}
	}
	
	@Override public void setSpecific(Float p)	{}
	@Override protected int galaxyWidthLY()		{ return 0; }
	@Override protected int galaxyHeightLY()	{ return 0; }
	@Override public void setRandom(Float p)	{}
	@Override public boolean valid(float x, float y)	{ return false; }
	@Override protected float sizeFactor(String size)	{ return 0; }

}