package rotp.ui.options;

import java.util.Arrays;

import rotp.model.game.SafeListPanel;
import rotp.model.game.SafeListParam;

final class NewOptionsBeta implements IOptionsSubUI {
	static final String OPTION_ID = NEW_OPTIONS_BETA_UI_KEY;

	@Override public String optionId()			{ return OPTION_ID; }
	@Override public boolean isCfgFile()		{ return true; }

	@Override public SafeListPanel optionsMap()	{
		SafeListPanel map = new SafeListPanel(OPTION_ID);
		map.add(new SafeListParam(Arrays.asList(
				rallyCombat,
				rallyCombatLoss,
				debugAutoRun,
				darkGalaxy
				)));
		return map;
	}
	@Override public SafeListParam majorList()	{
		SafeListParam majorList = new SafeListParam(OPTION_ID,
				Arrays.asList(
						debugAutoRun,
						darkGalaxy,
						rallyCombat,
						rallyCombatLoss
						));
		return majorList;
	}
}
