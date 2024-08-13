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
package rotp.model.planet;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rotp.model.empires.Empire;
import rotp.model.game.IGameOptions;
import rotp.ui.util.planets.Sphere2D;
import rotp.util.Base;
import rotp.util.ColorMap;

public class PlanetType implements Base {
    private static final int MAX_SHIPS = 4;
    public static final int TERRAIN_MAX = 10000000; // number of possible terrains

    private static final HashMap<String, PlanetType> typeMap = new HashMap<>();
    public static PlanetType keyed(String s)       { return typeMap.get(s); }
    public static Collection<PlanetType> allTypes(){ return typeMap.values(); }
    public static void addType(PlanetType r)       { typeMap.put(r.key(), r); }

    public static final String NONE = "PLANET_NONE";
    public static final String RADIATED = "PLANET_RADIATED";
    public static final String TOXIC = "PLANET_TOXIC";
    public static final String INFERNO = "PLANET_INFERNO";
    public static final String DEAD = "PLANET_DEAD";
    public static final String TUNDRA = "PLANET_TUNDRA";
    public static final String BARREN = "PLANET_BARREN";
    public static final String MINIMAL = "PLANET_MINIMAL";
    public static final String DESERT = "PLANET_DESERT";
    public static final String STEPPE = "PLANET_STEPPE";
    public static final String ARID = "PLANET_ARID";
    public static final String OCEAN = "PLANET_OCEAN";
    public static final String JUNGLE = "PLANET_JUNGLE";
    public static final String TERRAN = "PLANET_TERRAN";

    public static final int HOSTILITY_NONE = 99;
    public static final int HOSTILITY_RADIATED = 12;
    public static final int HOSTILITY_TOXIC = 11;
    public static final int HOSTILITY_INFERNO = 10;
    public static final int HOSTILITY_DEAD = 9;
    public static final int HOSTILITY_TUNDRA = 8;
    public static final int HOSTILITY_BARREN = 7;
    public static final int HOSTILITY_MINIMAL = 6;
    public static final int HOSTILITY_DESERT = 5;
    public static final int HOSTILITY_STEPPE = 4;
    public static final int HOSTILITY_ARID = 3;
    public static final int HOSTILITY_OCEAN = 2;
    public static final int HOSTILITY_JUNGLE = 1;
    public static final int HOSTILITY_TERRAN = 0;

    public static final List<String> planetTypes = Arrays.asList(new String[] {
    			NONE,
    			RADIATED,
    			TOXIC,
    			INFERNO,
    			DEAD,
    			TUNDRA,
    			BARREN,
    			MINIMAL,
    			DESERT,
    			STEPPE,
    			ARID,
    			OCEAN,
    			JUNGLE,
    			TERRAN
    			});

    private String key;
    private String descBiological;
    private String descSilicoid;
    private String terrainKey;
    private String panoramaKey;
    private final List<Integer> terrainSeeds = new ArrayList<>();
    private final int[] shipX = new int[MAX_SHIPS];
    private final int[] shipY = new int[MAX_SHIPS];
    private final int[] shipW = new int[MAX_SHIPS];

    private final List<String> landscapeKeys  = new ArrayList<>();
    private final List<String> cloudKeys      = new ArrayList<>();
    private final List<String> atmosphereKeys = new ArrayList<>();
    private int hostility;
    private int minSize;
    private int maxSize;

    private transient ColorMap colorMap;
    private transient BufferedImage terrainImage;
    private transient BufferedImage panoramaImage;
    private transient Map<Integer, Sphere2D>smallSpheres;
    private transient Map<Integer, Integer> sphereResolution;

    private  Map<Integer,Sphere2D> smallSpheres() {
        if (smallSpheres == null)
            smallSpheres = new HashMap<>();
        return smallSpheres;
    }
    private  Map<Integer,Integer> sphereResolution() {
        if (sphereResolution == null)
            sphereResolution = new HashMap<>();
        return sphereResolution;
    }
    public Sphere2D smallSphere(Planet p)               { return smallSpheres().get(p.terrainSeed());  }
    public void smallSphere(Sphere2D sph, Planet p)     { smallSpheres().put(p.terrainSeed(), sph); }
    public int sphereResolution(Planet p)         {
        if (sphereResolution().containsKey(p.terrainSeed()))
            return sphereResolution().get(p.terrainSeed());
        else
            return 0;
    }

    public PlanetType() {
        terrainSeeds.add(roll(1,TERRAIN_MAX-1));
    }
    @Override
    public String toString()                  { return concat("PlanetType: ", key); }

    public String key()                       { return key; }
    public void key(String s)                 { key = s; }
    public String descBiological()            { return descBiological; }
    public void descBiological(String s)      { descBiological = s; }
    public String descSilicoid()              { return descSilicoid; }
    public void descSilicoid(String s)        { descSilicoid = s; }
    public int hostility()                    { return hostility; }
    public void hostility(int i)              { hostility = i; }
    public String terrainKey()                { return terrainKey; }
    public void terrainKey(String s)          { terrainKey = s; }
    public String panoramaKey()               { return panoramaKey; }
    public void panoramaKey(String s)         { panoramaKey = s; }
    public int minSize()                      { return minSize; }
    public void minSize(int i)                { minSize = i; }
    public int maxSize()                      { return maxSize; }
    public void maxSize(int i)                { maxSize = i; }
    public void landscapeKeys(String s)       { landscapeKeys.addAll(substrings(s, ',')); }
    public List<String> atmosphereKeys()      { return atmosphereKeys; }
    public List<String> cloudKeys()           { return cloudKeys; }

    public int shipX(int i)                   { return shipX[i]; }
    public void shipX(int i, int val)         { shipX[i] = val; }
    public int shipY(int i)                   { return shipY[i]; }
    public void shipY(int i, int val)         { shipY[i] = val; }
    public int shipW(int i)                   { return shipW[i]; }
    public void shipW(int i, int val)         { shipW[i] = val; }

    public void cloudKeys(String s) {
        cloudKeys.clear();
        if (!s.trim().isEmpty())
            cloudKeys.addAll(substrings(s, ','));
    }
    public void atmosphereKeys(String s) {
        atmosphereKeys.clear();
        if (!s.trim().isEmpty())
            atmosphereKeys.addAll(substrings(s, ','));
    }

    public String name()                      { return text(key); }
    public boolean hostileToTerrans()         { return hostility >= HOSTILITY_BARREN; }
    public int randomTerrainSeed()            { return random(terrainSeeds); }

    public boolean isAsteroids()              { return key.equals(NONE); }

    public String description(Empire emp) {
        if (emp.ignoresPlanetEnvironment())
            return descSilicoid();
        else
            return descBiological();
    }
    public BufferedImage terrainImage()           {
        if (terrainImage == null)
            terrainImage = newBufferedImage(currentFrame(terrainKey));
        return terrainImage;
    }
    public BufferedImage panoramaImage()           {
        if (panoramaKey == null)
            return null;
        if (panoramaImage == null)
            panoramaImage = newBufferedImage(currentFrame(panoramaKey));
        return panoramaImage;
    }
    public String randomLandscapeKey() {
        return random(landscapeKeys);
    }
    public Image randomCloudImage() {
        return cloudKeys.isEmpty() ? null : image(random(cloudKeys));
    }
    public Image atmosphereImage() {
        return atmosphereKeys.isEmpty() ? null : image(atmosphereKeys.get(0));
    }
    public int randomSize() {
        return 5* roll(minSize()/5, maxSize()/5);
    }
    public ColorMap colorMap()  {
        if (colorMap == null)
            colorMap = new ColorMap();
        return colorMap;
    }
    public float randomOceanPct() {
        switch(key()) {
            case OCEAN:
                return random(0.8f,1.0f);
            case JUNGLE:
                return random(0.6f,0.8f);
            case TERRAN:
                return random(0.55f,0.7f);
            case STEPPE:
                return random(0.25f,0.55f);
            case ARID:
                return random(0.1f,0.25f);
            case DESERT:
                return random(0.05f,0.1f);
            case MINIMAL:
                return 0.05f;
            case TOXIC:
                return random(0.2f,0.6f);
            case BARREN:
            case TUNDRA:
            case DEAD:
            case INFERNO:
            case RADIATED:
            default:
                return 0.0f;
        }
    }
    public int randomIceLevel() {
        switch(key()) {
            case OCEAN:
                return roll(10,30);
            case JUNGLE:
                return 0;
            case TERRAN:
                return roll(10,60);
            case STEPPE:
                return roll(10,80);
            case ARID:
                return roll(10,60);
            case DESERT:
                return roll(10,50);
            case MINIMAL:
                return roll(0,40);
            case BARREN:
                return roll(0,20);
            case TUNDRA:
                return 0;
            case DEAD:
                return roll(30,100);
            case INFERNO:
                return 0;
            case TOXIC:
                return roll(0,30);
            case RADIATED:
            default:
                return  roll(0,20);
        }
    }
    public int randomCloudThickness() {
        // inferno 600+
        // none = 0
        // wisps = 300-350
        // thin 350-400
        // terran 400-450
        // heavy 450-500
        switch(key()) {
            case OCEAN:
            case JUNGLE:
            case TERRAN:
            case STEPPE:
                return roll(400,450);
            case ARID:
            case DESERT:
            case TUNDRA:
                return roll(350,400);
            case MINIMAL:
            case BARREN:
                return roll(300,350);
            case INFERNO:
                return 700;
            case TOXIC:
                return roll(300,500);
            case DEAD:
            case RADIATED:
            default:
                return  0;
        }
    }
    public int[] asteroidProbability(String type) {
    	int typeIdx  = planetTypes.indexOf(type);
    	if (typeIdx == 0)
    		return new int[] {0, 0, 100};
    	IGameOptions opts = options();
    	int noneBase = opts.baseNoAsteroidsProbPct();
    	int noneStep = opts.stepNoAsteroidsProbPct();
    	int lowBase  = opts.baseLowAsteroidsProbPct();
    	int lowStep  = opts.stepLowAsteroidsProbPct();

    	int none = noneBase + (typeIdx-1) * noneStep;
    	int low  = lowBase  + (typeIdx-1) * lowStep;
    	return new int[] {none, low};
//    	switch (type) {
//	        case RADIATED:	return new int[] {46, 25, 29};
//	        case TOXIC:		return new int[] {47, 25, 28};
//	        case INFERNO:	return new int[] {48, 25, 27};
//	        case DEAD:		return new int[] {49, 25, 26};
//	        case TUNDRA:	return new int[] {50, 25, 25};
//	        case BARREN:	return new int[] {51, 25, 24};
//	        case MINIMAL:	return new int[] {52, 25, 23};
//	        case DESERT:	return new int[] {53, 25, 22};
//	        case STEPPE:	return new int[] {54, 25, 21};
//	        case ARID:		return new int[] {55, 25, 20};
//	        case OCEAN:		return new int[] {56, 25, 19};
//	        case JUNGLE:	return new int[] {57, 25, 18};
//	        case TERRAN:	return new int[] {58, 25, 17};
//    	}
//    	return new int[] {0, 0, 100};
    }
}
