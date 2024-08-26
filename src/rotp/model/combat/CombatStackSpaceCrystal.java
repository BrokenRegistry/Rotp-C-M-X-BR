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
package rotp.model.combat;

import java.awt.Color;

import rotp.model.ai.CrystalShipCaptain;
import rotp.model.galaxy.SpaceCrystal;
import rotp.model.galaxy.StarSystem;
import rotp.model.ships.ShipWeaponMissileType;

public class CombatStackSpaceCrystal extends CombatStackMonster {
    private static final int MAX_WEAPON_DAMAGE = 1000;
    private boolean weaponUsed = false;
    public SpaceCrystal monster;
    public float monsterLevel;
    public CombatStackSpaceCrystal(SpaceCrystal crystal, String imageKey, Float level, int desId, Color shieldC) {
    	super(crystal, imageKey, level, desId, false, shieldC);
    	monster = crystal;
        if (level == null)
        	monsterLevel = options().monstersLevel();
        else
        	monsterLevel = level;
        num = 1;
        maxStackHits(7000 * monsterLevel);
        hits(maxStackHits());
        streamProjectorHits(0); // BR:
        canTeleport = true;
        beamDefense = 1;
        missileDefense = 1;
        maxShield = shield = 5.0f;
        captain = new CrystalShipCaptain(monster);
        image = image(imageKey);
    }    
    @Override
	public float missileInterceptPct(ShipWeaponMissileType wpn)   {
        return max(0, 0.75f - (0.01f * wpn.tech().level));
    }
    @Override
    public boolean isMonster()          { return true; }
    @Override
    public boolean isArmed()            { return true; }
    @Override
    public boolean immuneToStasis()     { return true; }
    @Override
    public void reloadWeapons()         { weaponUsed = false; };
    @Override
    public boolean hostileTo(CombatStack st, StarSystem sys)  { return true; }
    @Override
    public boolean selectBestWeapon(CombatStack target)       { return !weaponUsed; }
    @Override
    public void fireWeapon(CombatStack target)  { 
        weaponUsed = true;
        int maxWeaponDamage = (int) (MAX_WEAPON_DAMAGE * monsterLevel);
        float dam = roll(1, maxWeaponDamage);

        drawAttack();

        String attackText = text("SHIP_COMBAT_MISS");
        for (int x0=x-1; x0<=x+1; x0++) {
            for (int y0=y-1; y0<=y+1; y0++) {
                CombatStack st = mgr.stackAt(x0,y0);
                if ((st != null) && (st != this)) {
                    if (st.isShip() || st.isMonster() || st.isColony()) {
                        st.drawDamageTaken(dam, attackText);
                        st.takePulsarDamage(dam, 1);
                        st.attackLevel = max(0, st.attackLevel-1);
                        st.repairPct = max(0, st.repairPct-0.05f);
                    }
                    if (st.isColony() && st.destroyed()) {
                        CombatStackColony cStack = (CombatStackColony) st;
                        st.mgr.destroyStack(st);
                        monster.degradePlanet(st.mgr.system());                   
                        cStack.colonyDestroyed = true;
                    }
                }
            }
        }
    }
    @Override
    public float initiativeRank() {
        return 100;
    }
    public void drawAttack() { 
        if (!mgr.showAnimations())
            return;
        
        brighten = 1.0f;
        for (int i=0;i<2;i++) {
            scale += 1.5;
            transparency -= 0.45;
            brighten -= .005;
            long t0 = System.currentTimeMillis();
            mgr.ui.paintAllImmediately();
            long t1 = System.currentTimeMillis() - t0;
            if (t1 < 50)
                sleep(50-t1);
        }
        for (int i=0;i<12;i++) {
            scale -= 0.25;
            transparency += 0.075;
            brighten -= 0.075f;
            long t0 = System.currentTimeMillis();
            mgr.ui.paintAllImmediately();
            long t1 = System.currentTimeMillis() - t0;
            if (t1 < 50)
                sleep(50-t1);
        }
        brighten = 0;
        mgr.ui.paintAllImmediately();
    }
}
        