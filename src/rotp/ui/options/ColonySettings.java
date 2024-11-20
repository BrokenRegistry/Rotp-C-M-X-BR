package rotp.ui.options;

import java.util.Arrays;

import rotp.model.game.SafeListPanel;
import rotp.model.game.SafeListParam;

final class ColonySettings extends AbstractOptionsSubUI {
	static final String OPTION_ID = COLONY_SETTINGS_UI_KEY;

	@Override public String optionId()			{ return OPTION_ID; }

	@Override public SafeListPanel optionsMap()	{
		SafeListPanel map = new SafeListPanel(OPTION_ID);
		map.add(new SafeListParam(Arrays.asList(
				developedDefinition,
				maxMissingPopulation,
				maxMissingFactories,
				LINE_SPACER_25,
				defaultMaxBases
				)));
		return map;
	}
	@Override public SafeListParam majorList()	{
		SafeListParam majorList = new SafeListParam(uiMajorKey(),
				Arrays.asList(
						developedDefinition,
						maxMissingPopulation,
						maxMissingFactories
						));
		return majorList;
	}
}
