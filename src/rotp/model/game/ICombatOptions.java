package rotp.model.game;

import java.util.Arrays;

import rotp.ui.util.ParamBoolean;
import rotp.ui.util.ParamInteger;
import rotp.ui.util.ParamIntegerSound;
import rotp.ui.util.ParamList;
import rotp.ui.util.ParamSubUI;
import rotp.ui.util.ParamTitle;

public interface ICombatOptions extends IBaseOptsTools {
	String ZOOM_GUI_ID		= "ZOOM_OPTIONS";

	ParamBoolean finalReplayZoomOut	 = new ParamBoolean(MOD_UI, "ZOOM_FINAL_REPLAY", false)
			.isCfgFile(true);
	default boolean finalReplayZoomOut()	{ return finalReplayZoomOut.get(); }

	ParamBoolean empireReplayZoomOut = new ParamBoolean(MOD_UI, "ZOOM_EMPIRE_REPLAY", false)
			.isCfgFile(true);
	default boolean empireReplayZoomOut()	{ return empireReplayZoomOut.get(); }

	ParamInteger replayTurnPace		 = new ParamInteger(MOD_UI, "REPLAY_TURN_PACE" , 1)
			.setLimits(1, 100)
			.setIncrements(1, 5, 20)
			.isCfgFile(true);
	default int replayTurnPace()			{ return replayTurnPace.get(); }
	default void replayTurnPace(int div)	{ replayTurnPace.set(div); }

	ParamBoolean newWeaponSound		= new ParamBoolean(MOD_UI, "NEW_WEAPON_SOUND", true)
			.isCfgFile(true);
	default boolean newWeaponSound() 		{ return newWeaponSound.get(); }

	ParamBoolean playerSoundEcho		= new ParamBoolean(MOD_UI, "PLAYER_SOUND_ECHO", true)
			.isCfgFile(true);
	default boolean playerSoundEcho() 		{ return playerSoundEcho.get(); }

	ParamBoolean alwaysShowsShield	= new ParamBoolean(MOD_UI, "ALWAYS_SHOWS_SHIELD", false)
			.isCfgFile(true);
	default boolean alwaysShowsShield()		{ return alwaysShowsShield.get(); }

	ParamInteger beamWindupFrames	= new ParamInteger(MOD_UI, "BEAM_WINDUP_FRAMES" , 6)
			.setLimits(1, 20)
			.setIncrements(1, 5, 20)
			.isCfgFile(true);
	default int beamWindupFrames()			{ return beamWindupFrames.get(); }

	ParamInteger beamHoldFrames		= new ParamInteger(MOD_UI, "BEAM_HOLD_FRAMES" , 0)
			.setLimits(-6, 20)
			.setIncrements(1, 5, 20)
			.isCfgFile(true);
	default int beamHoldFrames()			{ return beamHoldFrames.get(); }
	
	ParamInteger heavyBeamHoldFrames= new ParamInteger(MOD_UI, "HEAVY_BEAM_HOLD_FRAMES" , 6)
			.setLimits(0, 20)
			.setIncrements(1, 5, 20)
			.isCfgFile(true);
	default int heavyBeamHoldFrames()		{ return heavyBeamHoldFrames.get(); }
	
	ParamBoolean shieldFadingFrames	= new ParamBoolean(MOD_UI, "SHIELD_FADING_FRAMES", true)
			.isCfgFile(true);
	default boolean shieldFadingFrames()	{ return shieldFadingFrames.get(); }

	ParamBoolean shieldEnveloping	= new ParamBoolean(MOD_UI, "SHIELD_ENVELOPING", false)
			.isCfgFile(true);
	default boolean shieldEnveloping()		{ return shieldEnveloping.get(); }

	ParamInteger beamAnimationFPS	= new ParamInteger(MOD_UI, "BEAM_ANIMATION_FPS" , 15)
			.setLimits(5, 100)
			.setIncrements(1, 5, 20)
			.isCfgFile(true);
	default int beamAnimationFPS()			{ return beamAnimationFPS.get(); }
	default int beamAnimationDelay()		{ return 1000/beamAnimationFPS.get(); }

	ParamInteger showResultDelay	= new ParamInteger(MOD_UI, "SHOW_RESULT_DELAY" , 2000)
			.setLimits(0, 5000)
			.setIncrements(100, 500, 2000)
			.isCfgFile(true);
	default int showResultDelay()			{ return showResultDelay.get(); }

	ParamInteger shieldNoisePct		= new ParamInteger(MOD_UI, "SHIELD_NOISE_PCT" , 30)
			.setLimits(0, 200)
			.setIncrements(1, 5, 20)
			.isCfgFile(true);
	default int shieldNoisePct()			{ return shieldNoisePct.get(); }

	ParamInteger shieldTransparency	= new ParamInteger(MOD_UI, "SHIELD_TRANSPARENCY" , 20)
			.setLimits(0, 100)
			.setIncrements(1, 5, 20)
			.isCfgFile(true);
	default int shieldTransparency()		{ return shieldTransparency.get(); }

	ParamInteger shieldFlickering	= new ParamInteger(MOD_UI, "SHIELD_FLICKERING" , 20)
			.setLimits(0, 100)
			.setIncrements(1, 5, 20)
			.isCfgFile(true);
	default int shieldFlickering()			{ return shieldFlickering.get(); }

	ParamInteger shieldBorder		= new ParamInteger(MOD_UI, "SHIELD_BORDER" , 0)
			.setLimits(-1, 5)
			.setIncrements(1, 1, 1)
			.isCfgFile(true)
			.loop(true)
			.specialZero(MOD_UI + "SHIELD_BORDER_SIZE")
			.specialNegative(MOD_UI + "SHIELD_BORDER_SIZE_2");
	default int shieldBorder()				{ return shieldBorder.get(); }
	default int shieldBorder(int hullSize)	{
		if (shieldBorder.isSpecialZero())
			return hullSize+1;
		if (shieldBorder.isSpecialNegative())
			return 2* (hullSize+1);
		return shieldBorder.get();
	}
	ParamInteger weaponZposition	= new ParamInteger(MOD_UI, "WEAPON_Z_POS" , 100)
			.setLimits(-1000, 1000)
			.setIncrements(10, 50, 200)
			.isCfgFile(true);
	default int weaponZposition()			{ return weaponZposition.get(); }

	ParamInteger weaponZRandom		= new ParamInteger(MOD_UI, "WEAPON_Z_RANDOM" , 50)
			.setLimits(0, 500)
			.setIncrements(5, 20, 100)
			.isCfgFile(true);
	default int weaponZRandom()				{ return weaponZRandom.get(); }
	
	ParamBoolean startShieldDemo	= new StartShieldDemo();
	class StartShieldDemo extends ParamBoolean {
		StartShieldDemo() {
			super(MOD_UI, "START_SHIELD_DEMO", false);
		}
		@Override public Boolean set(Boolean val) {
			if (val)
				javax.swing.SwingUtilities.invokeLater(demo());
			return super.set(false);
		}
		private Runnable demo() {
	        return () -> {
	        	rotp.model.combat.DemoShields.main(null);
	        };
		}
	}
	default boolean startShieldDemo()		{ return startShieldDemo.get(); }
	
	ParamIntegerSound echoSoundDelay		= new ParamIntegerSound(MOD_UI, "ECHO_SOUND_DELAY" , 90, 10, 500, 5, 20, 50);
	default int echoSoundDelay()			{ return echoSoundDelay.get(); }

	ParamIntegerSound echoSoundHullDelay	= new ParamIntegerSound(MOD_UI, "ECHO_SOUND_HULL_DELAY" , 30, 0, 100, 1, 5, 20);
	default int echoSoundHullDelay()		{ return echoSoundHullDelay.get(); }

	ParamIntegerSound echoSoundDecay		= new ParamIntegerSound(MOD_UI, "ECHO_SOUND_DECAY" , 50, 0, 95, 1, 5, 20);
	default float echoSoundDecay()			{ return echoSoundDecay.get()/100f; }

	ParamBoolean former2DShield		= new ParamBoolean(MOD_UI, "FORMER_2D_SHIELD", true)
			.isCfgFile(true);
	default boolean former2DShield()		{ return former2DShield.get(); }

	ParamList shieldType			= new ParamList( MOD_UI, "NEW_WEAPON_ANIMATION", "Yes")
			.isCfgFile(true)
			.showFullGuide(true)
			.put("No",	MOD_UI + "SHIELD_TYPE_NONE") // for compatibility with former boolean
			.put("Yes",	MOD_UI + "SHIELD_TYPE_3D")   // for compatibility with former boolean
			.put("2D",	MOD_UI + "SHIELD_TYPE_2D")
			.put("3B",	MOD_UI + "SHIELD_TYPE_3_BUFFERS");
	default boolean shieldType3D()			{ return shieldType.get().equalsIgnoreCase("Yes"); }
	default boolean shieldType2D()			{ return shieldType.get().equalsIgnoreCase("2D"); }
	default boolean shieldType3Buffer()		{ return shieldType.get().equalsIgnoreCase("3B"); }
	
	ParamInteger playerAttackConfidence  = new ParamInteger(MOD_UI, "PLAYER_ATTACK_CONFIDENCE", 100)
			.setLimits(100, 500)
			.setIncrements(1, 5, 20);
	ParamInteger playerDefenseConfidence = new ParamInteger(MOD_UI, "PLAYER_DEFENSE_CONFIDENCE", 100)
			.setLimits(100, 500)
			.setIncrements(1, 5, 20);
	ParamInteger aiAttackConfidence		 = new ParamInteger(MOD_UI, "AI_ATTACK_CONFIDENCE", 100)
			.setLimits(100, 500)
			.setIncrements(1, 5, 20);
	ParamInteger aiDefenseConfidence	 = new ParamInteger(MOD_UI, "AI_DEFENSE_CONFIDENCE", 100)
			.setLimits(100, 500)
			.setIncrements(1, 5, 20);
	default float playerAttackConfidence()	{ return playerAttackConfidence.get()/100f; }
	default float playerDefenseConfidence()	{ return playerDefenseConfidence.get()/100f; }
	default float aiAttackConfidence()		{ return aiAttackConfidence.get()/100f; }
	default float aiDefenseConfidence()		{ return aiDefenseConfidence.get()/100f; }

	ParamInteger retreatRestrictionTurns	= new ParamInteger(MOD_UI, "RETREAT_RESTRICTION_TURNS", 100)
			.setDefaultValue(MOO1_DEFAULT, 1)
			.setLimits(0, 100)
			.setIncrements(1, 5, 20);
	default int selectedRetreatRestrictionTurns()	{ return retreatRestrictionTurns.get(); }

	ParamList retreatRestrictions			= new ParamList(MOD_UI, "RETREAT_RESTRICTIONS", "None")
			.setDefaultValue(MOO1_DEFAULT, "Both")
			.showFullGuide(true)
			.put("None",	MOD_UI + "RETREAT_NONE")
			.put("AI",		MOD_UI + "RETREAT_AI")
			.put("Player",	MOD_UI + "RETREAT_PLAYER")
			.put("Both",	MOD_UI + "RETREAT_BOTH");
	default int selectedRetreatRestrictions()	{ return retreatRestrictions.getIndex(); }

	ParamInteger maxCombatTurns		= new ParamInteger(MOD_UI, "MAX_COMBAT_TURNS", 100)
			.setDefaultValue(MOO1_DEFAULT, 50)
			.setLimits(10, 1000)
			.setIncrements(1, 5, 20);
	default int maxCombatTurns()				{ return maxCombatTurns.get(); }

	ParamBoolean asteroidsVanish			= new ParamBoolean(MOD_UI, "ASTEROIDS_VANISH", true)
			.setDefaultValue(MOO1_DEFAULT, false)
			.isValueInit(false);
	ParamBoolean moo1PlanetLocation			= new ParamBoolean(MOD_UI, "MOO1_PLANET_LOCATION", false)
			.setDefaultValue(MOO1_DEFAULT, true)
			.isValueInit(false);
	ParamBoolean moo1AsteroidsLocation		= new ParamBoolean(MOD_UI, "MOO1_ASTEROIDS_LOCATION", false)
			.setDefaultValue(MOO1_DEFAULT, true)
			.isValueInit(false);
	ParamBoolean moo1AsteroidsProperties	= new ParamBoolean(MOD_UI, "MOO1_ASTEROIDS_PROPERTIES", false)
			.setDefaultValue(MOO1_DEFAULT, true)
			.isValueInit(false);
	default boolean asteroidsVanish()			{ return asteroidsVanish.get(); }
	default boolean moo1PlanetLocation()		{ return moo1PlanetLocation.get(); }
	default boolean moo1AsteroidsLocation()		{ return moo1AsteroidsLocation.get(); }
	default boolean moo1AsteroidsProperties()	{ return moo1AsteroidsProperties.get(); }
	
	ParamInteger baseNoAsteroidsProbPct		= new ParamInteger(MOD_UI, "BASE_NO_ASTEROIDS", 46)
			.setLimits(0, 100)
			.setIncrements(1, 5, 20);
	ParamInteger stepNoAsteroidsProbPct		= new ParamInteger(MOD_UI, "STEP_NO_ASTEROIDS", 1)
			.setLimits(0, 100)
			.setIncrements(1, 5, 20);
	default int baseNoAsteroidsProbPct()		{ return baseNoAsteroidsProbPct.get(); }
	default int stepNoAsteroidsProbPct()		{ return stepNoAsteroidsProbPct.get(); }

	ParamInteger baseLowAsteroidsProbPct	= new ParamInteger(MOD_UI, "BASE_LOW_ASTEROIDS", 25)
			.setLimits(0, 100)
			.setIncrements(1, 5, 20);
	ParamInteger stepLowAsteroidsProbPct	= new ParamInteger(MOD_UI, "STEP_LOW_ASTEROIDS", 0)
			.setLimits(0, 100)
			.setIncrements(1, 5, 20);
	default int baseLowAsteroidsProbPct()		{ return baseLowAsteroidsProbPct.get(); }
	default int stepLowAsteroidsProbPct()		{ return stepLowAsteroidsProbPct.get(); }

	ParamInteger richNoAsteroidsModPct		= new ParamInteger(MOD_UI, "RICH_NO_ASTEROIDS", -10)
			.setLimits(-100, 100)
			.setIncrements(1, 5, 20);
	ParamInteger ultraRichNoAsteroidsModPct	= new ParamInteger(MOD_UI, "U_RICH_NO_ASTEROIDS", -20)
			.setLimits(-100, 100)
			.setIncrements(1, 5, 20);
	default int richNoAsteroidsModPct()			{ return richNoAsteroidsModPct.get(); }
	default int ultraRichNoAsteroidsModPct()	{ return ultraRichNoAsteroidsModPct.get(); }

	ParamInteger minLowAsteroids			= new ParamInteger(MOD_UI, "MIN_LOW_ASTEROIDS", 1)
			.setLimits(0, 20)
			.setIncrements(1, 2, 5);
	ParamInteger maxLowAsteroids			= new ParamInteger(MOD_UI, "MAX_LOW_ASTEROIDS", 5)
			.setLimits(0, 20)
			.setIncrements(1, 2, 5);
	default int minLowAsteroids()				{ return minLowAsteroids.get(); }
	default int maxLowAsteroids()				{ return maxLowAsteroids.get(); }

	ParamInteger minHighAsteroids			= new ParamInteger(MOD_UI, "MIN_HIGH_ASTEROIDS", 3)
			.setLimits(0, 20)
			.setIncrements(1, 2, 5);
	ParamInteger maxHighAsteroids			= new ParamInteger(MOD_UI, "MAX_HIGH_ASTEROIDS", 7)
			.setLimits(0, 20)
			.setIncrements(1, 2, 5);
	default int minHighAsteroids()				{ return minHighAsteroids.get(); }
	default int maxHighAsteroids()				{ return maxHighAsteroids.get(); }


	// ==================== GUI List Declarations ====================
	//
	ParamSubUI combatOptionsUI = combatOptionsUI();

	static SafeListPanel combatOptionsMap() {
		SafeListPanel map = new SafeListPanel();
		map.add(new SafeListParam(Arrays.asList(
				new ParamTitle("WEAPON_ANIMATIONS"),
				showResultDelay,
				newWeaponSound, playerSoundEcho,
				echoSoundDecay, echoSoundDelay, echoSoundHullDelay,

				headerSpacer,
				beamWindupFrames, beamHoldFrames,
				heavyBeamHoldFrames, shieldFadingFrames
				)));
		map.add(new SafeListParam(Arrays.asList(
				new ParamTitle("SHIELD_ANIMATIONS"),
				shieldType, alwaysShowsShield, 
	
				headerSpacer,
				beamAnimationFPS, shieldEnveloping, shieldBorder,
				shieldTransparency, shieldFlickering, shieldNoisePct,
				weaponZposition, weaponZRandom,
	
				headerSpacer,
				startShieldDemo
				)));
		map.add(new SafeListParam(Arrays.asList(
				new ParamTitle("ASTEROIDS"),
				moo1PlanetLocation, moo1AsteroidsLocation,
				asteroidsVanish, moo1AsteroidsProperties,
				headerSpacer,
				minLowAsteroids, maxLowAsteroids,
				minHighAsteroids, maxHighAsteroids,
				headerSpacer,
				baseNoAsteroidsProbPct, stepNoAsteroidsProbPct,
				baseLowAsteroidsProbPct, stepLowAsteroidsProbPct,
				richNoAsteroidsModPct, ultraRichNoAsteroidsModPct
				)));
		map.add(new SafeListParam(Arrays.asList(
				new ParamTitle("XILMI_AI_OPTIONS"),
				playerAttackConfidence, playerDefenseConfidence,
				aiAttackConfidence, aiDefenseConfidence,

				headerSpacer,
				new ParamTitle("GAME_OTHER"),
				maxCombatTurns,
				retreatRestrictions, retreatRestrictionTurns
				)));
		return map;
	};
	static ParamSubUI combatOptionsUI() {
		return new ParamSubUI( MOD_UI, "ZOOM_OPTIONS_UI", combatOptionsMap(),
				"ZOOM_OPTIONS_TITLE", ZOOM_GUI_ID).isCfgFile(true);
	}
}
