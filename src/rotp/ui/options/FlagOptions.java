package rotp.ui.options;

import java.util.Arrays;

import rotp.model.game.SafeListPanel;
import rotp.model.game.SafeListParam;
import rotp.ui.util.ParamTitle;

final class FlagOptions implements IOptionsSubUI {
	static final String OPTION_ID = FLAG_OPTIONS_UI_KEY;

	@Override public String optionId()			{ return OPTION_ID; }

	@Override public SafeListPanel optionsMap()	{
		SafeListPanel map = new SafeListPanel(OPTION_ID);
		map.add(new SafeListParam(Arrays.asList(
				new ParamTitle("AUTO_FLAG_ID_SELECTION"),
				autoFlagAssignation1, autoFlagAssignation2,
				autoFlagAssignation3, autoFlagAssignation4,

				headerSpacer50,
				new ParamTitle("AUTO_FLAG_IN_NEBULA"),
				flagInNebulaColor, flagNotNebulaColor,

				headerSpacer50,
				new ParamTitle("AUTO_FLAG_COLONY_TECH"),
				flagTechGaiaColor, flagTechFertileColor, flagTechGoodColor,
				flagTechStandardColor, flagTechBarrenColor, flagTechDeadColor,
				flagTechToxicColor, flagTechRadiatedColor, flagTechNoneColor
				)));
		map.add(new SafeListParam(Arrays.asList(
				new ParamTitle("AUTO_FLAG_VESTIGES"),
				flagRuinsOrionColor, flagRuinsAntaranColor, flagRuinsNoneColor,

				headerSpacer50,
				new ParamTitle("AUTO_FLAG_RESOURCES"),
				flagOrionColor, flagAntaranColor,
				flagUltraRichColor, flagRichColor, flagAssetNormalColor,
				flagPoorColor, flagUltraPoorColor, flagNoneColor,
				
				headerSpacer50,
				new ParamTitle("AUTO_FLAG_ENVIRONMENT"),
				flagEnvGaiaColor, flagEnvFertileColor,
				flagEnvNormalColor,	flagEnvHostileColor, flagEnvNoneColor
				)));
		map.add(new SafeListParam(Arrays.asList(
				new ParamTitle("AUTO_FLAG_TYPE"),
				flagTerranColor, flagJungleColor, flagOceanColor,
				flagAridColor, flagSteppeColor, flagDesertColor, flagMinimalColor,
				flagBarrenColor, flagTundraColor, flagDeadColor,
				flagInfernoColor, flagToxicColor, flagRadiatedColor,
				flagAsteroidColor
				)));
		return map;
	}
	@Override public SafeListParam majorList()	{
		SafeListParam majorList = new SafeListParam(uiMajorKey(),
				Arrays.asList(
						flagColorCount,
						autoFlagAssignation1,
						autoFlagAssignation2,
						autoFlagAssignation3,
						autoFlagAssignation4
						));
		return majorList;
	}
	@Override public SafeListParam minorList()	{
		SafeListParam minorList = new SafeListParam(uiMinorKey(),
				Arrays.asList(
						flagColorCount
						));
		return minorList;
	}

}
