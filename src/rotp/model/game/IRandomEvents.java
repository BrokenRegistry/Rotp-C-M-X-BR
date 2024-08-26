package rotp.model.game;

import static rotp.model.game.IPreGameOptions.guardianMonstersLevel;

import java.util.Arrays;

import rotp.ui.util.ParamBoolean;
import rotp.ui.util.ParamInteger;
import rotp.ui.util.ParamList;
import rotp.ui.util.ParamSubUI;
import rotp.ui.util.ParamTitle;

public interface IRandomEvents extends IBaseOptsTools {

	String RANDOM_EVENTS_GUI_ID	= "RANDOM_EVENTS";
	String SPECIAL_MULTIPLE		= MOD_UI + "RANDOM_EVENT_MULTIPLE";
	String SPECIAL_DISABLED		= MOD_UI + "RANDOM_EVENT_DISABLED";
	String SPECIAL_UNIQUE		= MOD_UI + "RANDOM_EVENT_UNIQUE";
	String SPECIAL_UNLIMITED	= MOD_UI + "RANDOM_EVENT_UNLIMITED";

	Integer MAX_DELAY_TURN	= 9999; 
	Integer MAX_RETURN_TURN	= 9999; 
	Integer MAX_SYSTEMS		= 9999; 

	// ========================================================================
	// BR: RANDOM EVENT GLOBAL PARAMETERS
	ParamInteger eventsStartTurn	= new ParamInteger(MOD_UI, "EVENTS_START_TURN", 50)
			.setLimits(1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20);

	ParamInteger eventsPace			= new ParamInteger(MOD_UI, "EVENTS_PACE", 100)
			.setLimits(20, 500)
			.setIncrements(1, 5, 20);
	default float selectedEventsPace()			{ return (float)eventsPace.get()/100f; }

	ParamBoolean eventsFavorWeak	= new ParamBoolean(MOD_UI, "EVENTS_FAVOR_WEAK", true);
	default boolean selectedEventsFavorWeak()	{ return eventsFavorWeak.get(); }
	
	ParamBoolean fixedEventsMode	= new ParamBoolean(MOD_UI, "FIXED_EVENTS_MODE", false);
	default boolean selectedFixedEventsMode()	{ return fixedEventsMode.get(); }
	
	ParamList    monstersGiveLoots	= new ParamList( MOD_UI, "MONSTERS_GIVE_LOOTS", "No")
		.showFullGuide(true)
		.put("No", 	MOD_UI + "MONSTERS_GIVE_LOOTS_NO")
		.put("Yes",	MOD_UI + "MONSTERS_GIVE_LOOTS_YES");
	default boolean monstersGiveLoot()			{ return monstersGiveLoots.get().equalsIgnoreCase("Yes"); }

	String MONSTER_EASY		= "Easy";
	String MONSTER_NORMAL	= "Normal";
	String MONSTER_HARD		= "Hard";
	String MONSTER_CUSTOM	= "Custom";
	ParamList    monstersLevel	= new ParamList( MOD_UI, "MONSTERS_LEVEL", "Normal")
		.setDefaultValue(MOO1_DEFAULT, "MoO1")
		.showFullGuide(true)
		.put(MONSTER_EASY,		MOD_UI + "MONSTERS_LEVEL_EASY")
		.put(MONSTER_NORMAL, 	MOD_UI + "MONSTERS_LEVEL_NORMAL")
		.put(MONSTER_HARD,		MOD_UI + "MONSTERS_LEVEL_HARD")
		.put(MONSTER_CUSTOM,	MOD_UI + "MONSTERS_LEVEL_CUSTOM");
	
		//.put("MoO1",	MOD_UI + "MONSTERS_LEVEL_MOO1");
	default String	monstersLevelKey()			{ return monstersLevel.get(); }
	default float	monstersLevel()				{
		switch (monstersLevel.get()) {
			case MONSTER_EASY:		return 0.7f;
			case MONSTER_NORMAL:	return 1.0f;
			case MONSTER_HARD:		return 1.4f;
			case MONSTER_CUSTOM:
				return (float) Math.pow(guardianMonstersLevel.get()/100f, 0.5);
			default:				return 1.0f;
		}
	}
	ParamBoolean isMoO1Monster				= new ParamBoolean(MOD_UI, "IS_MOO1_MONSTER", false)
			.setDefaultValue(MOO1_DEFAULT, true);
	default Boolean	isMoO1Monster()				{ return isMoO1Monster.get(); }

	ParamList    monstersGNNNotification	= new ParamList( MOD_UI, "MONSTERS_GNN", "All")
		.showFullGuide(true)
		.put("All", 	MOD_UI + "MONSTERS_GNN_ALL")
		.put("New",		MOD_UI + "MONSTERS_GNN_NEW")
		.put("First",	MOD_UI + "MONSTERS_GNN_FIRST");
	default String	monstersGNNNotification()	{ return monstersGNNNotification.get(); }
	
	// ========================================================================
	// BR: RANDOM EVENT MONSTERS PARAMETERS
	ParamInteger piratesDelayTurn	= new ParamInteger(MOD_UI, "PIRATES_DELAY_TURN",  25)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger amoebaDelayTurn	= new ParamInteger(MOD_UI, "AMOEBA_DELAY_TURN",  100)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger crystalDelayTurn	= new ParamInteger(MOD_UI, "CRYSTAL_DELAY_TURN", 100)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);

	ParamInteger piratesReturnTurn	= new ParamInteger(MOD_UI, "PIRATES_RETURN_TURN", 0)
			.setLimits(-1, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE)
			.specialNegative(SPECIAL_MULTIPLE);
	ParamInteger amoebaReturnTurn	= new ParamInteger(MOD_UI, "AMOEBA_RETURN_TURN",  0)
			.setLimits(-1, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE)
			.specialNegative(SPECIAL_MULTIPLE);
	ParamInteger crystalReturnTurn	= new ParamInteger(MOD_UI, "CRYSTAL_RETURN_TURN", 0)
			.setLimits(-1, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE)
			.specialNegative(SPECIAL_MULTIPLE);

	ParamInteger piratesMaxSystems	= new ParamInteger(MOD_UI, "PIRATES_MAX_SYSTEMS", 0)
			.setDefaultValue(MOO1_DEFAULT, 5) // Not MoO1 Monster but the spirit is there.
			.setLimits(0, MAX_SYSTEMS)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNLIMITED);
	default int selectedPiratesMaxSystems()		{ return piratesMaxSystems.get(); }
	ParamInteger amoebaMaxSystems	= new ParamInteger(MOD_UI, "AMOEBA_MAX_SYSTEMS",  0)
			.setDefaultValue(MOO1_DEFAULT, 5)
			.setLimits(0, MAX_SYSTEMS)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNLIMITED);
	default int selectedAmoebaMaxSystems()		{ return amoebaMaxSystems.get(); }
	ParamInteger crystalMaxSystems	= new ParamInteger(MOD_UI, "CRYSTAL_MAX_SYSTEMS", 0)
			.setDefaultValue(MOO1_DEFAULT, 5)
			.setLimits(0, MAX_SYSTEMS)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNLIMITED);
	default int selectedCrystalMaxSystems()		{ return crystalMaxSystems.get(); }

	// ========================================================================
	// BR: RANDOM EVENT OTHER DELAY PARAMETERS
	ParamInteger donationDelayTurn	= new ParamInteger(MOD_UI, "DONATION_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger depletedDelayTurn	= new ParamInteger(MOD_UI, "DEPLETED_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger enrichedDelayTurn	= new ParamInteger(MOD_UI, "ENRICHED_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger fertileDelayTurn	= new ParamInteger(MOD_UI, "FERTILE_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger virusDelayTurn		= new ParamInteger(MOD_UI, "VIRUS_DELAY_TURN",		0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger earthquakeDelayTurn= new ParamInteger(MOD_UI, "EARTHQUAKE_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger accidentDelayTurn	= new ParamInteger(MOD_UI, "ACCIDENT_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger rebellionDelayTurn	= new ParamInteger(MOD_UI, "REBELLION_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger derelictDelayTurn	= new ParamInteger(MOD_UI, "DERELICT_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger assassinDelayTurn	= new ParamInteger(MOD_UI, "ASSASSIN_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger plagueDelayTurn	= new ParamInteger(MOD_UI, "PLAGUE_DELAY_TURN",		0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger supernovaDelayTurn	= new ParamInteger(MOD_UI, "SUPERNOVA_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger piracyDelayTurn	= new ParamInteger(MOD_UI, "PIRACY_DELAY_TURN",		0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger cometDelayTurn		= new ParamInteger(MOD_UI, "COMET_DELAY_TURN",		0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger relicDelayTurn		= new ParamInteger(MOD_UI, "RELIC_DELAY_TURN",		0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger sizeBoostDelayTurn	= new ParamInteger(MOD_UI, "SIZEBOOST_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);
	ParamInteger gauntletDelayTurn	= new ParamInteger(MOD_UI, "GAUNTLET_DELAY_TURN",	0)
			.setLimits(-1, MAX_DELAY_TURN)
			.setIncrements(1, 5, 20)
			.specialNegative(SPECIAL_DISABLED);

	// ========================================================================
	// BR: RANDOM EVENT OTHER RETURN PARAMETERS
	ParamInteger donationReturnTurn	= new ParamInteger(MOD_UI, "DONATION_RETURN_TURN",	1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger depletedReturnTurn	= new ParamInteger(MOD_UI, "DEPLETED_RETURN_TURN",	1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger enrichedReturnTurn	= new ParamInteger(MOD_UI, "ENRICHED_RETURN_TURN",	1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger fertileReturnTurn	= new ParamInteger(MOD_UI, "FERTILE_RETURN_TURN",	1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger virusReturnTurn	= new ParamInteger(MOD_UI, "VIRUS_RETURN_TURN",		1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger earthquakeReturnTurn= new ParamInteger(MOD_UI, "EARTHQUAKE_RETURN_TURN", 1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger accidentReturnTurn	= new ParamInteger(MOD_UI, "ACCIDENT_RETURN_TURN",	1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger rebellionReturnTurn= new ParamInteger(MOD_UI, "REBELLION_RETURN_TURN",	1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger derelictReturnTurn	= new ParamInteger(MOD_UI, "DERELICT_RETURN_TURN",	0)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger assassinReturnTurn	= new ParamInteger(MOD_UI, "ASSASSIN_RETURN_TURN",	1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger plagueReturnTurn	= new ParamInteger(MOD_UI, "PLAGUE_RETURN_TURN",	1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger supernovaReturnTurn= new ParamInteger(MOD_UI, "SUPERNOVA_RETURN_TURN",	0)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger piracyReturnTurn	= new ParamInteger(MOD_UI, "PIRACY_RETURN_TURN",	1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger cometReturnTurn	= new ParamInteger(MOD_UI, "COMET_RETURN_TURN",		1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger relicReturnTurn 	= new ParamInteger(MOD_UI, "RELIC_RETURN_TURN",		0)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger sizeBoostReturnTurn= new ParamInteger(MOD_UI, "SIZEBOOST_RETURN_TURN",	1)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	ParamInteger gauntletReturnTurn	= new ParamInteger(MOD_UI, "GAUNTLET_RETURN_TURN",	0)
			.setLimits(0, MAX_RETURN_TURN)
			.setIncrements(1, 5, 20)
			.specialZero(SPECIAL_UNIQUE);
	
	// ==================== GUI List Declarations ====================
	//
	ParamSubUI customRandomEventUI = customRandomEventUI();

	static SafeListPanel customRandomEventMap() {
		SafeListPanel map = new SafeListPanel();
		map.add(new SafeListParam(Arrays.asList(
				new ParamTitle("RANDOM_EVENTS_GLOBAL"),
				IAdvOptions.randomEvents,
				eventsStartTurn, eventsPace,
				eventsFavorWeak, fixedEventsMode,
				monstersGiveLoots, monstersLevel,
				monstersGNNNotification,

				headerSpacer,
				guardianMonstersLevel,
				isMoO1Monster,
				
				headerSpacer,
				new ParamTitle("RANDOM_EVENTS_MONSTERS"),
				piratesDelayTurn, piratesReturnTurn, piratesMaxSystems,
				headerSpacer,
				amoebaDelayTurn, amoebaReturnTurn, amoebaMaxSystems,
				headerSpacer,
				crystalDelayTurn, crystalReturnTurn, crystalMaxSystems
				)));
		map.add(new SafeListParam(Arrays.asList(
				new ParamTitle("RANDOM_EVENTS_DELAYS"),
				donationDelayTurn,
				depletedDelayTurn,
				enrichedDelayTurn,
				fertileDelayTurn,
				virusDelayTurn,
				earthquakeDelayTurn,
				accidentDelayTurn,
				rebellionDelayTurn,
				derelictDelayTurn,
				assassinDelayTurn,
				plagueDelayTurn,
				supernovaDelayTurn,
				piracyDelayTurn,
				cometDelayTurn,
				relicDelayTurn,
				sizeBoostDelayTurn,
				gauntletDelayTurn
				)));
		map.add(new SafeListParam(Arrays.asList(
				new ParamTitle("RANDOM_EVENTS_RETURNS"),
				donationReturnTurn,
				depletedReturnTurn,
				enrichedReturnTurn,
				fertileReturnTurn,
				virusReturnTurn,
				earthquakeReturnTurn,
				accidentReturnTurn,
				rebellionReturnTurn,
				derelictReturnTurn,
				assassinReturnTurn,
				plagueReturnTurn,
				supernovaReturnTurn,
				piracyReturnTurn,
				cometReturnTurn,
				relicReturnTurn,
				sizeBoostReturnTurn,
				gauntletReturnTurn
				)));
		return map;
	};
	static ParamSubUI customRandomEventUI() {
		return new ParamSubUI(MOD_UI, "RANDOM_EVENTS_UI", customRandomEventMap(),
				"RANDOM_EVENTS_TITLE", RANDOM_EVENTS_GUI_ID);
	}
	static SafeListParam customRandomEventOptions() { return customRandomEventMap().getList(); }
}
