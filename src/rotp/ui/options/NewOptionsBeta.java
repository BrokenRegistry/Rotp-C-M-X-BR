package rotp.ui.options;

import java.util.Arrays;

import rotp.model.game.SafeListPanel;
import rotp.model.game.SafeListParam;

final class NewOptionsBeta extends AbstractOptionsSubUI {
	static final String OPTION_ID = NEW_OPTIONS_BETA_UI_KEY;

	@Override public String optionId()			{ return OPTION_ID; }
	@Override public boolean isCfgFile()		{ return true; }

	@Override public SafeListPanel optionsMap()	{
		SafeListPanel map = new SafeListPanel(OPTION_ID);
		map.add(new SafeListParam(Arrays.asList(
				rallyCombat
				)));
		map.add(new SafeListParam(Arrays.asList(
				debugAutoRun
				)));
		map.add(new SafeListParam(Arrays.asList(
				optionPanelAlignment
				)));
		return map;
	}
	@Override public SafeListParam majorList()	{
		SafeListParam majorList = new SafeListParam(uiMajorKey(),
				Arrays.asList(
						debugAutoRun,
						rallyCombatLoss,
						optionPanelAlignment
						));
		return majorList;
	}
}
