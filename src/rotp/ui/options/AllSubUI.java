package rotp.ui.options;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import rotp.model.game.SafeListParam;
import rotp.ui.util.IParam;
import rotp.ui.util.ParamSubUI;

public final class AllSubUI {
	private	static AllSubUI instance;
	public	static final String ALL_MOD_OPTIONS			= "ALL_MOD_OPTIONS";
	public	static final String ALL_CFG_OPTIONS			= "ALL_CFG_OPTIONS";
	public	static final String ALL_DUPLICATE_OPTIONS	= "ALL_DUPLICATE_OPTIONS";
	public	static final String ALL_NOT_CFG_OPTIONS		= "ALL_NOT_CFG_OPTIONS";
	private	final Map<String, AbstractOptionsSubUI> uiMap = new HashMap<>();
	private static SafeListParam allModOptions;

	public static AllSubUI instance()			{
		if (instance == null)
			instance = new AllSubUI();
		return instance;
	}
	private AllSubUI()							{ init(); }
	private void put(AbstractOptionsSubUI ui)	{ uiMap.put(ui.optionId(), ui); }
	private void init()							{
		// Level 0 Panels
		put(new BackupOptions());
		put(new ColonyRules());
		put(new ColonySettings());
		put(new CombatAsteroids());
		put(new CombatTiming());
		put(new CombatXilmiAI());
		put(new CouncilOptions());
		put(new DiplomacyOptions());
		put(new FlagOptions());
		put(new GalaxyRules());
		put(new GameDifficulty());
		put(new GameMenuPreferences());
		put(new GNNandPopupFilter());
		put(new HelpAndAdvice());
		put(new IronmanLittle());
		put(new NewOptionsBeta());
		put(new NewRulesBeta());
		put(new SettingMenuPreferences());
		put(new SetupGalaxyOptions());
		put(new SetupHomeworldOptions());
		put(new SetupNebulaOptions());
		put(new SetupRandomOpponents());
		put(new SetupRestartOptions());
		put(new SetupTechOptions());
		put(new ShieldAnimations());
		put(new ShipRules());
		put(new SpyOptions());
		put(new VisualOptions());
		put(new WeaponAnimation());
		put(new ZoomOptions());

		// Level 1 Panels
		put(new ComputerOptions());
		put(new DiplomacyOptions());
		put(new GameAutomation());
		put(new ShipCombatRules());
		put(new ShipCombatSettings());

		put(new CombatOptions());
		put(new CommonOptions());
		put(new DebugOptions());
		put(new GalaxyMenuOptions());
		put(new GovOptions());
		put(new InGameOptions());
		put(new IronmanOptions());
		put(new MainOptions());
		put(new NameOptions());
		put(new NameFrOptions());
		put(new PreGameOptions());
		put(new RaceMenuOptions());
		put(new RandomEventsOptions());
		put(new SystemsOptions());
		put(new RulesOptions());
		put(new SettingsOptions());
		put(new SetupParameters());
	}

	public static SafeListParam allModOptions(boolean refresh)			{
		if (refresh || allModOptions == null) {
			// Start with a set to filter duplicates
			LinkedHashSet<IParam> allOptions = new LinkedHashSet<>();
			for (AbstractOptionsSubUI ui : instance().uiMap.values())
				allOptions.addAll(ui.getListNoSpacer());
			// Remove the line separators
			allOptions.remove(null);
			// Then create the final list 
			allModOptions = new SafeListParam(ALL_MOD_OPTIONS);
			allModOptions.add(rotp.model.game.IMainOptions.showGuide);
			allModOptions.addAll(allOptions);
		}
		return allModOptions;
	}
	public static SafeListParam allCfgOptions(boolean refresh)			{
		SafeListParam list = new SafeListParam(ALL_CFG_OPTIONS);
		for(IParam param : allModOptions(refresh))
			if (param.isCfgFile())
				list.add(param);
		return list;
	}
	public static SafeListParam allDuplicateOptions(boolean refresh)	{
		SafeListParam list = new SafeListParam(ALL_DUPLICATE_OPTIONS);
		for(IParam param:allModOptions(refresh))
			if (param.isCfgFile() && param.isDuplicate())
				list.add(param);
		return list;
	}
	public static SafeListParam allNotCfgOptions(boolean refresh)		{
		SafeListParam list = new SafeListParam(ALL_NOT_CFG_OPTIONS);
		for(IParam param:allModOptions(refresh))
			if (!param.isCfgFile())
				list.add(param);
		return list;
	}

	public static AbstractOptionsSubUI getHandle(String name)	{ return instance().uiMap.get(name); }
	public static ParamSubUI	getUI(String name)		{ return getHandle(name).getUI(); }
	// Minimal Panels
	public static ParamSubUI shieldAnimSubUI()		{ return getUI(ShieldAnimations.OPTION_ID); }
	public static ParamSubUI weaponAnimSubUI()		{ return getUI(WeaponAnimation.OPTION_ID); }
	
	// Panels Level 1

	public static ParamSubUI combatSubUI()			{ return getUI(CombatOptions.OPTION_ID); }
	public static ParamSubUI commonSubUI()			{ return getUI(CommonOptions.OPTION_ID); }
	public static ParamSubUI debugSubUI()			{ return getUI(DebugOptions.OPTION_ID); }
	public static ParamSubUI flagSubUI()			{ return getUI(FlagOptions.OPTION_ID); }
	public static ParamSubUI galaxySubUI()			{ return getUI(GalaxyMenuOptions.OPTION_ID); } // May be never used
	public static ParamSubUI governorSubUI()		{ return getUI(GovOptions.OPTION_ID); }
	public static ParamSubUI inGameSubUI()			{ return getUI(InGameOptions.OPTION_ID); }
	public static ParamSubUI ironmanSubUI()			{ return getUI(IronmanOptions.OPTION_ID); }
	public static ParamSubUI mainSubUI()			{ return getUI(MainOptions.OPTION_ID); }
	public static ParamSubUI nameSubUI()			{ return getUI(NameOptions.OPTION_ID); }
	public static ParamSubUI nameFrSubUI()			{ return getUI(NameFrOptions.OPTION_ID); }
	public static ParamSubUI preGameSubUI()			{ return getUI(PreGameOptions.OPTION_ID); }
	public static ParamSubUI raceSubUI()			{ return getUI(RaceMenuOptions.OPTION_ID); } // May be never used
	public static ParamSubUI randomEventsSubUI()	{ return getUI(RandomEventsOptions.OPTION_ID); }
	public static ParamSubUI systemSubUI()			{ return getUI(SystemsOptions.OPTION_ID); }

	public static ParamSubUI rulesSubUI()			{return getUI(RulesOptions.OPTION_ID); }
	public static ParamSubUI settingsSubUI()		{ return getUI(SettingsOptions.OPTION_ID); }
//	public static ParamSubUI rulesSubUI()			{  // TO DO BR: Validate Rotp.isUnderTest()
//		if (Rotp.isUnderTest())
//			return getUI(RulesOptions.OPTION_ID);
//		else
//			return getUI(InGameOptions.OPTION_ID);
//	}
//	public static ParamSubUI settingsSubUI()		{  // TO DO BR: Validate  Rotp.isUnderTest()
//		if (Rotp.isUnderTest())
//			return getUI(SettingsOptions.OPTION_ID);
//		else
//			return getUI(InGameOptions.OPTION_ID);
//	}

	public static SafeListParam optionsGalaxy() 	{ return GalaxyMenuOptions.optionsGalaxy(); }
	public static SafeListParam optionsCustomRace() { return RaceMenuOptions.optionsCustomRace(); }
	public static SafeListParam optionsRace() 		{ return RaceMenuOptions.optionsRace(); }
	
	// TODO BR: May be Not!!!
	public List<ParamSubUI> subPanelList()			{
		List<ParamSubUI> list = new ArrayList<>();
		for (AbstractOptionsSubUI ui : uiMap.values())
			list.add(ui.getUI());
		return list;
	}
	public SafeListParam getParamList(String name)	{ return uiMap.get(name).getListNoSpacer(); }

}
