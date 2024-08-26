package rotp.ui.design;

import static rotp.model.ships.ShipDesign.MAX_SIZE;
import static rotp.model.ships.ShipDesign.maxSpecials;
import static rotp.model.ships.ShipDesign.maxWeapons;
import static rotp.model.ships.ShipDesignLab.MAX_DESIGNS;

import rotp.model.ships.ShipComponent;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipEngine;
import rotp.ui.RotPUI;
import rotp.ui.design.DesignUI.DesignConfigPanel;
import rotp.ui.vipconsole.IVIPConsole;
import rotp.ui.vipconsole.VIPConsole.Command;
import rotp.ui.vipconsole.VIPConsole.CommandMenu;

public class VIPDesignView implements IVIPConsole {
	private static final String DESIGN_INFO			= "I";
	private static final String DESIGN_SELECT		= "D";
	private static final String DESIGN_HULL			= "H";
	private static final String DESIGN_ENGINE		= "E";
	private static final String DESIGN_COMPUTER		= "C";
	private static final String DESIGN_ARMOR		= "A";
	private static final String DESIGN_SHIELD		= "SH";
	private static final String DESIGN_ECM			= "ECM";
	private static final String DESIGN_MANEUVER		= "CS"; // Combat Speed
	private static final String DESIGN_WEAPON_SLOT	= "WS";
	private static final String DESIGN_WEAPON_TYPE	= "WT";
	private static final String DESIGN_WEAPON_COUNT	= "WC";
	private static final String DESIGN_SPECIAL_SLOT	= "SDS"; // Special Devices
	private static final String DESIGN_SPECIAL_TYPE	= "SDT"; // Special Devices
	private static final String DESIGN_COPY			= "COPY";
	private static final String DESIGN_SCRAP		= "SCRAP";
	private static final String DESIGN_AUTO_SCOUT	= "AS";
	private static final String DESIGN_AUTO_ATTACK	= "AA";
	private static final String DESIGN_AUTO_COLONY	= "AC";
	private static final String DESIGN_AUTO_BUILD	= "SUGGEST";
	private static final String DESIGN_FIGHTER		= "FIGHTER";
	private static final String DESIGN_COLONY		= "COLONY";
	private static final String DESIGN_SCOUT		= "SCOUT";
	private static final String DESIGN_BOMBER		= "BOMBER";
	private static final String DESIGN_CLEAR		= "CLEAR";
	private static final String CLEAR_WEAPONS_ONLY	= "W";
	private static final String DESIGN_DEPLOY		= "DEPLOY";
	
	private int selectedWeaponSlot	= 0;
	private int selectedSpecialSlot	= 0;
	private Boolean verboseLocal	= null;
	@Override public Boolean verboseLocal()	{ return verboseLocal; }

	//private int selectedSlot;
	// Access to DesignUI
	private DesignUI designUI()				{ return DesignUI.instance; }
	private DesignConfigPanel configPanel()	{ return designUI().configPanel; }
	private ShipDesign shipDesign()			{ return configPanel().shipDesign(); }
	private void selectSlot(int id)			{ designUI().selectSlot(id); }
	private int selectedSlot()				{ return designUI().selectedSlot(); }
	private void selectHull(int id)			{ configPanel().selectHull(id); }

	private int shipCount(int id)			{ return designUI().shipCount(id); }
	private int orbitCount(int id)			{ return designUI().orbitCount(id); }
	private int inTransitCount(int id)		{ return designUI().inTransitCount(id); }
	private int constructionCount(int id)	{ return designUI().constructionCount(id); }
	
	private ShipDesign slotDesign(int slot)	{
		if (slot<0)
			return player().shipLab().prototypeDesign();
		else
			return player().shipLab().design(slot);
	}

	// ##### DesignView Command
	public CommandMenu initDesignMenu(CommandMenu parent)	{
		return new DesignMenu("Ship Design Menu", parent);
	}

	private String processAutoScout(Entries param)		{
		if (!param.isEmpty())
			return "error: No parameters expected." + param.toString();
		ShipDesign shipDesign = shipDesign();
		if (!shipDesign.active())
			return "error: Only active design can be set to auto Scout.";

		shipDesign.setAutoScout(!shipDesign.isAutoScout());
		designUI().repaint();
		String out = "Auto Scout has been set to " + shipDesign.isAutoScout();
		return out;
	}
	private String processAutoColonize(Entries param)	{
		if (!param.isEmpty())
			return "error: No parameters expected." + param.toString();
		ShipDesign shipDesign = shipDesign();
		if (!shipDesign.active())
			return "error: Only active design can be set to auto Colonize.";

		if (shipDesign.isAutoColonize()) {
			shipDesign.setAutoColonize(false);
			designUI().repaint();
			return "Auto Colonize has been disabled";
		}
		else {
			// don't set autocolonize to true for non-colony ships
			if (shipDesign.hasColonySpecial()) {
				shipDesign.setAutoColonize(true);
				designUI().repaint();
				return "Auto Colonize has been enabled";
			}
			else
				return "This ship has no colony ability";
		}
	}
	private String processAutoAttack(Entries param)	{
		if (!param.isEmpty())
			return "error: No parameters expected." + param.toString();
		ShipDesign shipDesign = shipDesign();
		if (!shipDesign.active())
			return "error: Only active design can be set to auto Attack.";

		if (shipDesign.isAutoAttack()) {
			shipDesign.setAutoAttack(false);
			designUI().repaint();
			return "Auto Attack has been disabled";
		}
		else {
			// don't set autoattack to true for unarmed
			if (shipDesign.hasColonySpecial()) {
				shipDesign.setAutoAttack(true);
				designUI().repaint();
				return "Auto Attack has been enabled";
			}
			else
				return "This ship is unarmed";
		}
	}
	private String processAutoBuild(Entries param)	{
		if (!param.isEmpty())
			return "error: No parameters expected." + param.toString();
		configPanel().autoDesign();
		designUI().repaint();
		String out = "New suitable design";
		out += NEWLINE + designComponents();
		out += NEWLINE + summaryInfo();
		return out;
	}
	private String processFighter(Entries param)	{
		if (!param.isEmpty())
			return "error: No parameters expected." + param.toString();
		configPanel().autoDesignFighter();
		designUI().repaint();
		String out = "New Fighter design";
		out += NEWLINE + designComponents();
		out += NEWLINE + summaryInfo();
		return out;
	}
	private String processColony(Entries param)		{
		if (!param.isEmpty())
			return "error: No parameters expected." + param.toString();
		configPanel().autoDesignColony();
		designUI().repaint();
		String out = "New Colony design";
		out += NEWLINE + designComponents();
		out += NEWLINE + summaryInfo();
		return out;
	}
	private String processScout(Entries param)		{
		if (!param.isEmpty())
			return "error: No parameters expected." + param.toString();
		configPanel().autoDesignScout();
		designUI().repaint();
		String out = "New Scout design";
		out += NEWLINE + designComponents();
		out += NEWLINE + summaryInfo();
		return out;
	}
	private String processBomber(Entries param)		{
		if (!param.isEmpty())
			return "error: No parameters expected." + param.toString();
		configPanel().autoDesignBomber();
		designUI().repaint();
		String out = "New Bomber design";
		out += NEWLINE + designComponents();
		out += NEWLINE + summaryInfo();
		return out;
	}

	private String processDeploy(Entries param)		{
		ShipDesign design = shipDesign();
		if (design.active())
			return "Error: Design is already deployed";

		if (param.isEmpty())
			return "Error: Ship Name is mandatory";
		if (!design.validConfiguration())
			return "Error: Invalid Configuration";
		if (selectedSlot() < 0) {
			int firstAvailable = -1;
			for (int i=0; i<MAX_DESIGNS; i++) {
				ShipDesign des1 = player().shipLab().design(i);
				if (!des1.active()) {
					firstAvailable = i;
					break;
				}
			}
			if (firstAvailable<0)
				return "Error: " + text("SHIP_DESIGN_PROTOTYPE_DESC");
			selectSlot(firstAvailable);
			design.copyFrom(player().shipLab().prototypeDesign());
		}
		String name = param.remains();
		ShipDesign targetDesign = design;
		targetDesign.active(true);
		targetDesign.setIconKey();
		targetDesign.name(name);
		targetDesign.clearEmptyWeapons();
		designUI().repaint();

		String out = name + " has been deployed";
		return out;
	}
	private String processCopy(Entries param)		{
		String out = "";
		if (param.isEmpty())
			return "Missing parameter: Source design";
		ShipDesign design = shipDesign();
		if (design.active())
			return "Deployed design can not be copied over";

		String str	= param.remove(0).toUpperCase();
		Integer tag	= getInteger(str);
		if (tag == null)
			return "Error: Wrong or misplaced parameter " + str;
		if (isValidDesignTag(tag))
			if (tag == selectedSlot()-1)
				return "error: can not copy the design over itself";
			else {
				design.copyFrom(player().shipLab().design(tag-1));
				out += "Copied from design " + tag;
			}
		else
			return "Error: Invalid copy design index " + tag;
		designUI().repaint();
		out += NEWLINE + designComponents();
		out += NEWLINE + summaryInfo();
		return out;
	}
	private String processClear(Entries param)		{
		ShipDesign design = shipDesign();
		if (design.active())
			return "Deployed design can not be cleared";
		if (param.isEmpty()) {
			player().shipLab().clearDesign(design, false);
			designUI().repaint();
			return "Currently selected design has been cleared";
		}
		String str = param.remove(0).toUpperCase();
		if (str.equalsIgnoreCase(CLEAR_WEAPONS_ONLY)) {
			player().shipLab().clearDesign(design, true);
			String out = "Currently selected design weapons have been cleared";
			out += NEWLINE + costAndSpaceInfo();
			designUI().repaint();
			return out;
		}
		else
			return "error: invalid parameter " + str;
	}
	private String processScrap(Entries param)		{
		if (!param.isEmpty()) {
			return "error: No parameters expected." + param.toString();
		}
		ShipDesign design = shipDesign();
		if (!design.active())
			return "Only Deployed design can be scraped";
		player().shipLab().scrapDesign(design);
		player().recalcPlanetaryProduction();
		designUI().init();
		designUI().repaint();
		return "Currently selected design has been scraped";
	}

	private String componentInfo(ShipComponent shipComp, String header)	{
		ShipDesign des = shipDesign();
		String size  = text("SHIP_DESIGN_SIZE_LABEL");
		String power = text("SHIP_DESIGN_POWER_LABEL");
		String cost  = text("SHIP_DESIGN_COST_LABEL");
		String out   = header;
		if (shipComp.name().isEmpty())
			out += EQUAL_SEP + text("SHIP_DESIGN_COMPONENT_NONE");
		else
			out += EQUAL_SEP + shipComp.name();
		out += SPACER + shipComp.desc(des);
		if (verbose()) {
			out += SPACER + size  + EQUAL_SEP + fmt(shipComp.size(des), 1);
			out += SPACER + power + EQUAL_SEP + fmt(shipComp.power(des), 1);
			out += SPACER + cost  + EQUAL_SEP + fmt(shipComp.cost(des), 1);			
		}
		return out;
	}
	
	private String processDesign(Entries param)	{
		if (param.isEmpty())
			return designSlotsOptions();

		if (!param.isEmpty()) {
			String str = param.remove(0).toUpperCase();
			Integer tag = getInteger(str);
			if (tag == null)
				return "Error: Wrong or misplaced parameter " + str;
			if (isValidDesignTag(tag))
				setDesign(tag);
			else
				return "Error: Invalid design index " + tag;
		}
		return designData();
	}
	private boolean isValidDesignTag(int tag)	{ return tag>=0 && tag<=MAX_DESIGNS; }
	private void setDesign(int tag)				{ selectSlot(tag-1); }
	private String designData()					{
		String out = designComponents();
		out += NEWLINE + summaryInfo();
		return out;		
	}
/*	private String designData()					{
		ShipDesign design = shipDesign();
		String out = "";
		out += summaryInfo();
		out += NEWLINE + engineInfo();
		
		out += NEWLINE + computerInfo(design);
		out += NEWLINE + armorInfo(design);
		out += NEWLINE + shieldInfo(design);
		out += NEWLINE + ecmInfo(design);
		out += NEWLINE + maneuverInfo(design);

		out += NEWLINE + weaponsInfo();
		out += NEWLINE + specialInfo();
		return out;		
	}
*/
	private String summaryInfo()				{ return summaryInfo(shipDesign()); }
	private String summaryInfo(ShipDesign des)	{
		String out = "";
		String name;
		if (selectedSlot() < 0) 
			name = text("SHIP_DESIGN_PROTOTYPE_TITLE");
		else if (des.active())
			name = des.name();
		else
			name = text("SHIP_DESIGN_NEW");
		out += name;
		out += NEWLINE	 + text("SHIP_DESIGN_COMBAT_STATS_TITLE");

		out += NEWLINE	 + text("SHIP_DESIGN_SIZE_LABEL");
		out += EQUAL_SEP + des.sizeDesc();
		out += NEWLINE	 + text("SHIP_DESIGN_RANGE_LABEL");
		if (player().tech().topFuelRangeTech().unlimited)
			out += EQUAL_SEP + text("SHIP_DESIGN_RANGE_UNLIMITED");
		else
			out += EQUAL_SEP + text("SHIP_DESIGN_RANGE_VALUE", (int)des.range());
		out += NEWLINE	 + text("SHIP_DESIGN_SPEED_LABEL");
		out += EQUAL_SEP + text("SHIP_DESIGN_SPEED_VALUE", (int)des.warpSpeed());
		des.recalculateCost();
		out += NEWLINE	 + text("SHIP_DESIGN_COST_LABEL");
		out += EQUAL_SEP + text("SHIP_DESIGN_COST_VALUE", (int)des.cost());
		out += NEWLINE	 + text("SHIP_DESIGN_TOTAL_SPACE_LABEL");
		out += EQUAL_SEP + (int)des.spaceUsed();
		out += NEWLINE	 + text("SHIP_DESIGN_AVAIL_SPACE_LABEL");
		out += EQUAL_SEP + (int)des.availableSpace();

		out += NEWLINE	 + text("SHIP_DESIGN_HIT_POINTS_LABEL");
		out += EQUAL_SEP + (int)des.hits();
		out += NEWLINE	 + text("SHIP_DESIGN_MISSILE_DEF_LABEL");
		out += EQUAL_SEP + (des.missileDefense() + des.empire().shipDefenseBonus());
		out += NEWLINE	 + text("SHIP_DESIGN_BEAM_DEF_LABEL");
		out += EQUAL_SEP + (des.beamDefense() + des.empire().shipDefenseBonus());
		out += NEWLINE	 + text("SHIP_DESIGN_ATTACK_LEVEL_LABEL");
		out += EQUAL_SEP + (int)(des.attackLevel()+des.empire().shipAttackBonus());
		out += NEWLINE	 + text("SHIP_DESIGN_COMBAT_SPEED_LABEL");
		out += EQUAL_SEP + des.combatSpeed();
		
		out += NEWLINE	 + text("SHIP_DESIGN_AUTO_SCOUT");
		out += EQUAL_SEP + des.isAutoScout();
		out += NEWLINE	 + text("SHIP_DESIGN_AUTO_ATTACK");
		out += EQUAL_SEP + des.isAutoAttack();
		out += NEWLINE	 + text("SHIP_DESIGN_AUTO_COLONIZE");
		out += EQUAL_SEP + des.isAutoColonize();

		return out;		
	}
	private String designLabel(int idx)			{
		ShipDesign design = slotDesign(idx);
		String out = bracketed(DESIGN_SELECT, idx + 1);

		String name;
		if (selectedSlot() < 0) 
			name = text("SHIP_DESIGN_PROTOTYPE_TITLE");
		else if (design.active())
			name = design.name();
		else
			name = text("SHIP_DESIGN_NEW");

		out += name;
//		if (design.active())
//			out += " " + design.name();
//		else
//			out += " " + text("SHIP_DESIGN_AVAILABLE");
		return out;
	}
	private String costAndSpaceInfo()			{
		ShipDesign des = shipDesign();
		String out = "";
		out += NEWLINE	 + text("SHIP_DESIGN_COST_LABEL");
		out += EQUAL_SEP + text("SHIP_DESIGN_COST_VALUE", (int)des.cost());
		out += SPACER	 + text("SHIP_DESIGN_AVAIL_SPACE_LABEL");
		out += EQUAL_SEP + (int)des.availableSpace();
		return out;
	}
	private String designSlotsOptions()			{
		String out = "The design selection options are:";
		out += NEWLINE + bracketed(DESIGN_SELECT, 0);
		out += " Prototype design";
		for (int idx=0; idx<MAX_DESIGNS; idx++)
			out += NEWLINE + designSlotInfo(idx);
		return out;
	}
	private String designSlotInfo(int desNum)	{
		ShipDesign des = slotDesign(desNum);
		String out = designLabel(desNum);
		if (desNum == selectedSlot())
			out += SPACER + "Selected Design ";
		if (!des.active()) {
			if (desNum == selectedSlot())
				out += NEWLINE + text("SHIP_DESIGN_AVAILABLE_DESC2");
			else
				out += NEWLINE + text("SHIP_DESIGN_AVAILABLE_DESC");
		}
		else {
			out += NEWLINE + text("SHIP_DESIGN_SLOT_DESC");
			out += EQUAL_SEP + shipCount(des.id());
			out += NEWLINE + text("SHIP_DESIGN_SLOT_DESC2");
			out += EQUAL_SEP + orbitCount(des.id());
			out += NEWLINE + text("SHIP_DESIGN_SLOT_DESC3");
			out += EQUAL_SEP + inTransitCount(des.id());
			if (constructionCount(des.id()) > 0) {
				out += NEWLINE + text("SHIP_DESIGN_SLOT_DESC4");
				out += EQUAL_SEP + constructionCount(des.id());				
			}
		}
		return out;
	}
	private String designSlotInfo()				{ return designSlotInfo(selectedSlot()); }
	private String designComponents(int desNum)	{
		ShipDesign design = slotDesign(desNum);
		String out = designLabel(desNum);
//		String name;
//		if (selectedSlot() < 0) 
//			name = text("SHIP_DESIGN_PROTOTYPE_TITLE");
//		else if (design.active())
//			name = design.name();
//		else
//			name = text("SHIP_DESIGN_NEW");

		if (desNum == selectedSlot())
			out += SPACER + "Selected Design ";
		if (design.active())
			out += SPACER + "Active";
		else
			out += SPACER + "not deployed";
		Boolean tmpVerbose = verboseLocal;
		verboseLocal = false;
		out += NEWLINE + hullComponent(design);
		out += NEWLINE + engineComponent(design);
		out += NEWLINE + maneuverComponent(design);
		out += NEWLINE + computerComponent(design);
		out += NEWLINE + armorComponent(design);
		out += NEWLINE + shieldComponent(design);
		out += NEWLINE + ecmComponent(design);
		out += NEWLINE + weaponsComponents(design);
		out += NEWLINE + specialsComponents(design);
		verboseLocal = tmpVerbose;
		return out;
	}
	private String designComponents()			{ return designComponents(selectedSlot()); }

	private String processHull(Entries param)		{
		String out = "Selected ";
		if (param.isEmpty()) {
			out += hullInfo();
			out += NEWLINE + hullOptions();
			return out;
		}
		
		String str = param.remove(0).toUpperCase();
		switch (str) {
		default:
			Integer tag = getInteger(str);
			if (tag == null)
				return "Error: Wrong or misplaced parameter " + str;
			if (isValidHullTag(tag))
				setHull(tag);
			else
				return "Error: Invalid hull index " + tag;
		}
		out += hullInfo();
		out += NEWLINE + costAndSpaceInfo();
		return out;
	}
	private String hullInfo(int sizeId)			{
		ShipDesign des = shipDesign();
		String size	= text("SHIP_DESIGN_SIZE_LABEL");
		String hit	= "Base " + text("SHIP_DESIGN_HIT_POINTS_LABEL");
		String cost	= text("SHIP_DESIGN_COST_LABEL");
		String out	= text("RACES_MILITARY_HULL");
		out += EQUAL_SEP + des.sizeDesc(sizeId);
		if (verbose()) {
			out += SPACER + size + EQUAL_SEP + (int) des.totalSpace(sizeId);
			out += SPACER + hit	 + EQUAL_SEP + des.baseHits(sizeId);
			out += SPACER + cost + EQUAL_SEP + des.baseCost(sizeId);			
		}
		return out;
	}
	private String hullOptions()				{
		Boolean tempVerbose = verboseLocal;
		verboseLocal = true;
		String out = "Hull options are:";
		for (int idx=0; idx<=MAX_SIZE; idx++) {
			out += NEWLINE + bracketed(DESIGN_HULL, idx + 1);
			out += " " + hullInfo(idx);
		}
		verboseLocal = tempVerbose;
		return out;
	}
	private String hullInfo()					{ return hullInfo(shipDesign().size()); }
	private boolean isValidHullTag(int tag)		{ return tag>0 && tag<=MAX_SIZE+1; }
	private void setHull(int tag)				{ selectHull(tag-1); }
	private String hullComponent(ShipDesign design)	{ return hullInfo(design.size()); }

	private String processEngine(Entries param)	{
		String out = "";
		if (param.isEmpty())
			return engineOptions();
		
		while (!param.isEmpty()) {
			String str = param.remove(0).toUpperCase();
			switch (str) {
			default:
				Integer tag = getInteger(str);
				if (tag == null)
					return out + NEWLINE + "Error: Wrong or misplaced parameter " + str;
				if (isValidEngineTag(tag))
					setEngine(tag);
				else
					return out + NEWLINE + "Error: Invalid Engine index " + tag;
			}
		}
		out += NEWLINE + engineInfo();
		return out;
	}	
	private String engineInfo()					{
		ShipDesign des = shipDesign();
		ShipEngine engine = des.engine();
		float engRequired = des.enginesRequired();
		float engSize	  = engine.size(des);
		float engPower	  = engine.powerOutput();
		float engCost	  = engine.cost(des);
		String out = text("SHIP_DESIGN_ENGINES_TITLE");
		if (verbose())
			out += NEWLINE	 + text("SHIP_DESIGN_ENGINES_DESC");
		out += NEWLINE	 + text("SHIP_DESIGN_ENGINE_TYPE");
		out += EQUAL_SEP + engine.name();
		out += NEWLINE	 + text("SHIP_DESIGN_ENGINE_SPEED");
		out += EQUAL_SEP + text("SHIP_DESIGN_SPEED_VALUE", (int)des.warpSpeed());
		out += NEWLINE	 + text("SHIP_DESIGN_ENGINE_COST1");
		out += EQUAL_SEP + fmt(engCost, 1);
		out += NEWLINE	 + text("SHIP_DESIGN_ENGINE_SIZE1");
		out += EQUAL_SEP + fmt(engSize, 1);
		out += NEWLINE	 + text("SHIP_DESIGN_ENGINE_POWER1");
		out += EQUAL_SEP +fmt(engPower, 1);
		out += NEWLINE	 + text("SHIP_DESIGN_POWER_REQUIREMENTS");
		out += EQUAL_SEP + fmt(engRequired * engPower, 1);
		out += NEWLINE	 + text("SHIP_DESIGN_ENGINES_REQUIRED");
		out += EQUAL_SEP + fmt(engRequired, 1);
		out += NEWLINE	 + text("SHIP_DESIGN_ENGINES_SIZE");
		out += EQUAL_SEP + (int) (engRequired * engSize);
		out += NEWLINE	 + text("SHIP_DESIGN_ENGINES_COST");
		out += EQUAL_SEP + (int) (engRequired * engCost);

		return out;		
	}
	private String engineOptions()				{
		DesignEngineSelectionUI selectionUI = designUI().engineSelectionUI;
		selectionUI.selectedDesign = shipDesign();
		String out = selectionUI.getOptions(bracketed(DESIGN_ENGINE, "#"), "#");
		return out;
	}
	private boolean isValidEngineTag(int tag)	{
		return tag>0 && tag<=designUI().engineSelectionUI.numComponents();
	}
	private void setEngine(int tag)				{
		designUI().engineSelectionUI.select(tag-1);
		designUI().repaint();
	}
	private String engineComponent(ShipDesign design)	{
		String out = text("SHIP_DESIGN_ENGINE_TYPE");
		out += EQUAL_SEP + design.engine().name();
		out += SPACER	 + text("SHIP_DESIGN_ENGINE_SPEED");
		out += EQUAL_SEP + text("SHIP_DESIGN_SPEED_VALUE", (int) design.warpSpeed());
		return out;
	}

	private String processWeaponSlot(Entries param)	{
		String out = "";
		if (param.isEmpty()) {
			out += weaponsInfo();
			out += NEWLINE + weaponsSlotOptions();
			return out;
		}

		String str = param.remove(0).toUpperCase();
		Integer tag = getInteger(str);
		if (tag == null)
			return "Error: Wrong or misplaced parameter " + str;
		if (isValidWeaponsSlot(tag))
			out += NEWLINE + setWeaponSlot(tag);
		else
			return "Error: Invalid Weapon slot " + tag;

		out += NEWLINE + weaponsInfo();
		out += NEWLINE + costAndSpaceInfo();
		return out;
	}
	private String processWeaponType(Entries param)	{
		String out = "";
		if (param.isEmpty()) {
			out += weaponsInfo();
			out += NEWLINE + costAndSpaceInfo();
			out += NEWLINE + weaponsTypeOptions();
			return out;
		}

		String str = param.remove(0).toUpperCase();
		Integer tag = getInteger(str);

		if (tag == null)
			return out + NEWLINE + "Error: Wrong or misplaced parameter " + str;
		if (isValidWeaponsType(tag))
			out += NEWLINE + setWeaponType(tag);
		else
			return out + NEWLINE + "Error: Invalid Weapon type " + tag;

		out += weaponsInfo();
		out += NEWLINE + costAndSpaceInfo();
		return out;
	}
	private String processWeaponCount(Entries param)	{
		String out = "";
		if (param.isEmpty()) {
			out += weaponsInfo();
			return out;
		}

		String str = param.remove(0).toUpperCase();
		Integer tag = getInteger(str);
		if (tag == null)
			return "Error: Wrong or misplaced parameter " + str;
		if (isValidWeaponsSlot(tag))
			out += NEWLINE + setWeaponCount(tag);
		else
			return "Error: Invalid Weapon Count " + tag;

		out += NEWLINE + weaponsInfo();
		out += NEWLINE + costAndSpaceInfo();
		return out;
	}
	private String weaponsInfo(ShipDesign des)	{
		return componentInfo(des.weapon(selectedWeaponSlot), text("SHIP_DESIGN_WEAPON_TITLE"));
	}
	private String weaponsInfo()				{ return weaponsInfo(shipDesign()); }
	private String weaponsSlotOptions()			{
		DesignWeaponSelectionUI selectionUI = designUI().weaponSelectionUI;
		selectionUI.selectedDesign = shipDesign();
		String out = selectionUI.getOptions(bracketed(DESIGN_WEAPON_SLOT, "#"), "#");
		return out;
	}
	private String weaponsTypeOptions()			{
		DesignWeaponSelectionUI selectionUI = designUI().weaponSelectionUI;
		selectionUI.selectedDesign = shipDesign();
		String out = selectionUI.getOptions(bracketed(DESIGN_WEAPON_TYPE, "#"), "#");
		return out;
	}
	private boolean isValidWeaponsSlot(int tag)	{ return tag>0 && tag<=maxWeapons; }
	private boolean isValidWeaponsType(int tag)	{
		return tag>0 && tag<=designUI().weaponSelectionUI.numComponents();
	}
	private String setWeaponSlot(int tag)		{
		selectedWeaponSlot = tag -1;
		designUI().weaponSelectionUI.select(selectedWeaponSlot);
		return "Selected weapon slot" + EQUAL_SEP + tag;
	}
	private String setWeaponType(int tag)		{
		designUI().weaponSelectionUI.select(selectedWeaponSlot);
		designUI().weaponSelectionUI.bank(tag-1);
		designUI().repaint();
		return "Selected weapon type"+ EQUAL_SEP + tag;
	}
	private String setWeaponCount(int count)	{
		designUI().computerSelectionUI.select(count);
		designUI().repaint();
		return "Selected weapon count" + EQUAL_SEP + count;
	}
	private String weaponsComponents(ShipDesign design)	{
		String out = "";
		String sep = "";
		String label = "Weapon ";
		for (int slot=0; slot<maxWeapons; slot++) {
			out += sep + componentInfo(design.weapon(slot), label + (slot+1));
			sep = NEWLINE;
		}
		return out;
	}

	private String processSpecialSlot(Entries param)	{
		String out = "";
		if (param.isEmpty()) {
			out += specialInfo();
			out += NEWLINE + specialSlotOptions();
			return out;
		}

		String str = param.remove(0).toUpperCase();
		Integer tag = getInteger(str);
		if (tag == null)
			return "Error: Wrong or misplaced parameter " + str;
		if (isValidSpecialSlot(tag))
			out += NEWLINE + setSpecialSlot(tag);
		else
			return "Error: Invalid Special Device slot " + tag;

		out += NEWLINE + specialInfo();
		out += NEWLINE + costAndSpaceInfo();
		return out;
	}
	private String processSpecialType(Entries param)	{
		String out = "";
		if (param.isEmpty()) {
			out += specialInfo();
			out += NEWLINE + costAndSpaceInfo();
			out += NEWLINE + specialTypeOptions();
			return out;
		}

		String str = param.remove(0).toUpperCase();
		Integer tag = getInteger(str);
		if (tag == null)
			return out + NEWLINE + "Error: Wrong or misplaced parameter " + str;
		if (isValidSpecialType(tag))
			out += NEWLINE + setSpecialType(tag);
		else
			return out + NEWLINE + "Error: Invalid Special Device type " + tag;

		out += specialInfo();
		out += NEWLINE + costAndSpaceInfo();
		return out;
	}
	private String specialInfo(ShipDesign des)	{
		return componentInfo(des.special(selectedSpecialSlot), text("SHIP_DESIGN_SPECIAL_TITLE"));
	}
	private String specialInfo()				{ return specialInfo(shipDesign()); }
	private String specialSlotOptions()			{
		DesignSpecialSelectionUI selectionUI = designUI().specialSelectionUI;
		selectionUI.selectedDesign = shipDesign();
		String out = selectionUI.getOptions(bracketed(DESIGN_SPECIAL_SLOT, "#"), "#");
		return out;
	}
	private String specialTypeOptions()			{
		DesignSpecialSelectionUI selectionUI = designUI().specialSelectionUI;
		selectionUI.selectedDesign = shipDesign();
		String out = selectionUI.getOptions(bracketed(DESIGN_SPECIAL_TYPE, "#"), "#");
		return out;
	}
	private boolean isValidSpecialSlot(int tag)	{ return tag>0 && tag<=maxSpecials; }
	private boolean isValidSpecialType(int tag)	{
		return tag>0 && tag<=designUI().specialSelectionUI.numComponents();
	}
	private String setSpecialSlot(int tag)		{
		designUI().specialSelectionUI.select(tag-1);
		designUI().repaint();
		return "Selected special device slot"+ EQUAL_SEP + tag;
	}
	private String setSpecialType(int tag)		{
		designUI().specialSelectionUI.select(tag-1);
		designUI().repaint();
		return "Selected special device type"+ EQUAL_SEP + tag;
	}
	private String specialsComponents(ShipDesign design)	{
		String out = "";
		String sep = "";
		String label = "Special Device ";
		for (int slot=0; slot<maxSpecials; slot++) {
			out += sep + componentInfo(design.special(slot), label + (slot+1));
			sep = NEWLINE;
		}
		return out;
	}

	private String processComputer(Entries param)	{
		String out = "";
		if (param.isEmpty())
			return computerOptions();
		
		while (!param.isEmpty()) {
			String str = param.remove(0).toUpperCase();
			switch (str) {
			default:
				Integer tag = getInteger(str);
				if (tag == null)
					return out + NEWLINE + "Error: Wrong or misplaced parameter " + str;
				if (isValidComputerTag(tag))
					setComputer(tag);
				else
					return out + NEWLINE + "Error: Invalid Computer index " + tag;
			}
		}
		out += computerInfo();
		out += NEWLINE + costAndSpaceInfo();
		return out;
	}
	private String computerInfo(ShipDesign des)	{
		return componentInfo(des.computer(), text("SHIP_DESIGN_COMPUTER_TITLE"));
	}
	private String computerInfo()				{ return computerInfo(shipDesign()); }
	private String computerOptions()			{
		DesignComputerSelectionUI selectionUI = designUI().computerSelectionUI;
		selectionUI.selectedDesign = shipDesign();
		String out = selectionUI.getOptions(bracketed(DESIGN_COMPUTER, "#"), "#");
		return out;
	}
	private boolean isValidComputerTag(int tag)	{
		return tag>0 && tag<=designUI().computerSelectionUI.numComponents();
	}
	private void setComputer(int tag)			{
		designUI().computerSelectionUI.select(tag-1);
		designUI().repaint();
	}
	private String computerComponent(ShipDesign design)	{ return computerInfo(design); }

	private String processArmor(Entries param)		{
		String out = "";
		if (param.isEmpty())
			return armorOptions();
		
		while (!param.isEmpty()) {
			String str = param.remove(0).toUpperCase();
			switch (str) {
			default:
				Integer tag = getInteger(str);
				if (tag == null)
					return out + NEWLINE + "Error: Wrong or misplaced parameter " + str;
				if (isValidArmorTag(tag))
					setArmor(tag);
				else
					return out + NEWLINE + "Error: Invalid Armor index " + tag;
			}
		}
		out += armorInfo();
		out += NEWLINE + costAndSpaceInfo();
		return out;
	}
	private String armorInfo(ShipDesign des)	{
		return componentInfo(des.armor(), text("SHIP_DESIGN_ARMOR_TITLE"));
	}
	private String armorInfo()					{ return armorInfo(shipDesign()); }
	private String armorOptions()				{
		DesignArmorSelectionUI selectionUI = designUI().armorSelectionUI;
		selectionUI.selectedDesign = shipDesign();
		String out = selectionUI.getOptions(bracketed(DESIGN_ARMOR, "#"), "#");
		return out;
	}
	private boolean isValidArmorTag(int tag)	{
		return tag>0 && tag<=designUI().armorSelectionUI.numComponents();
	}
	private void setArmor(int tag)				{
		designUI().armorSelectionUI.select(tag-1);
		designUI().repaint();
	}
	private String armorComponent(ShipDesign design)	{ return armorInfo(design); }

	private String processShield(Entries param)	{
		String out = "";
		if (param.isEmpty())
			return shieldOptions();
		
		while (!param.isEmpty()) {
			String str = param.remove(0).toUpperCase();
			switch (str) {
			default:
				Integer tag = getInteger(str);
				if (tag == null)
					return out + NEWLINE + "Error: Wrong or misplaced parameter " + str;
				if (isValidShieldTag(tag))
					setShield(tag);
				else
					return out + NEWLINE + "Error: Invalid Shield index " + tag;
			}
		}
		out += shieldInfo();
		out += NEWLINE + costAndSpaceInfo();
		return out;
	}
	private String shieldInfo(ShipDesign des)	{
		return componentInfo(des.shield(), text("SHIP_DESIGN_SHIELD_TITLE"));
	}
	private String shieldInfo()					{ return shieldInfo(shipDesign()); }
	private String shieldOptions()				{
		DesignShieldSelectionUI selectionUI = designUI().shieldSelectionUI;
		selectionUI.selectedDesign = shipDesign();
		String out = selectionUI.getOptions(bracketed(DESIGN_SHIELD, "#"), "#");
		return out;
	}
	private boolean isValidShieldTag(int tag)	{
		return tag>0 && tag<=designUI().shieldSelectionUI.numComponents();
	}
	private void setShield(int tag)				{
		designUI().shieldSelectionUI.select(tag-1);
		designUI().repaint();
	}
	private String shieldComponent(ShipDesign design)	{ return shieldInfo(design); }

	private String processEcm(Entries param)		{
		String out = "";
		if (param.isEmpty())
			return ecmOptions();
		
		while (!param.isEmpty()) {
			String str = param.remove(0).toUpperCase();
			switch (str) {
			default:
				Integer tag = getInteger(str);
				if (tag == null)
					return out + NEWLINE + "Error: Wrong or misplaced parameter " + str;
				if (isValidEcmTag(tag))
					setEcm(tag);
				else
					return out + NEWLINE + "Error: Invalid Ecm index " + tag;
			}
		}
		out += ecmInfo();
		out += NEWLINE + costAndSpaceInfo();
		return out;
	}
	private String ecmInfo(ShipDesign des)		{
		return componentInfo(des.ecm(), text("SHIP_DESIGN_ECM_TITLE"));
	}
	private String ecmInfo()					{ return ecmInfo(shipDesign()); }
	private String ecmOptions()					{
		DesignEcmSelectionUI selectionUI = designUI().ecmSelectionUI;
		selectionUI.selectedDesign = shipDesign();
		String out = selectionUI.getOptions(bracketed(DESIGN_ECM, "#"), "#");
		return out;
	}
	private boolean isValidEcmTag(int tag)		{
		return tag>0 && tag<=designUI().ecmSelectionUI.numComponents();
	}
	private void setEcm(int tag)				{
		designUI().ecmSelectionUI.select(tag-1);
		designUI().repaint();
	}
	private String ecmComponent(ShipDesign design)	{ return ecmInfo(design); }

	private String processManeuver(Entries param)	{
		String out = "";
		if (param.isEmpty())
			return maneuverOptions();
		
		while (!param.isEmpty()) {
			String str = param.remove(0).toUpperCase();
			switch (str) {
			default:
				Integer tag = getInteger(str);
				if (tag == null)
					return out + NEWLINE + "Error: Wrong or misplaced parameter " + str;
				if (isValidManeuverTag(tag))
					setManeuver(tag);
				else
					return out + NEWLINE + "Error: Invalid Maneuver index " + tag;
			}
		}
		out += maneuverInfo();
		out += NEWLINE + costAndSpaceInfo();
		return out;
	}
	private String maneuverInfo(ShipDesign des)	{
		return componentInfo(des.maneuver(), text("SHIP_DESIGN_MANEUVER_TITLE"));
	}
	private String maneuverInfo()				{ return maneuverInfo(shipDesign()); }
	private String maneuverOptions()			{
		DesignManeuverSelectionUI selectionUI = designUI().maneuverSelectionUI;
		selectionUI.selectedDesign = shipDesign();
		String out = selectionUI.getOptions(bracketed(DESIGN_MANEUVER, "#"), "#");
		return out;
	}
	private boolean isValidManeuverTag(int tag)	{
		return tag>0 && tag<=designUI().maneuverSelectionUI.numComponents();
	}
	private void setManeuver(int tag)			{
		designUI().maneuverSelectionUI.select(tag-1);
		designUI().repaint();
	}
	private String maneuverComponent(ShipDesign design)	{ return maneuverInfo(design); }

	// ################### SUB COMMAND CLASSES ######################
	private class CmdAutoScout extends Command {
		CmdAutoScout  (String descr, String... keys) {
			super(descr, keys);
			cmdHelp(text("GOVERNOR_AUTO_SCOUT_DESC"));
		}
		@Override protected String execute(Entries param) { return processAutoScout(param); }
	}
	private class CmdAutoAttack extends Command {
		CmdAutoAttack  (String descr, String... keys) {
			super(descr, keys);
			cmdHelp(text("GOVERNOR_AUTO_SCOUT_DESC"));
		}
		@Override protected String execute(Entries param) { return processAutoAttack(param); }
	}
	private class CmdAutoColonize extends Command {
		CmdAutoColonize  (String descr, String... keys) {
			super(descr, keys);
			cmdHelp(text("GOVERNOR_AUTO_SCOUT_DESC"));
		}
		@Override protected String execute(Entries param) { return processAutoColonize(param); }
	}
	private class CmdAutoBuild extends Command {
		CmdAutoBuild  (String descr, String... keys) {
			super(descr, keys);
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processAutoBuild(param); }
	}
	private class CmdDesignFighter extends Command {
		CmdDesignFighter  (String descr, String... keys) {
			super(descr, keys);
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processFighter(param); }
	}
	private class CmdDesignColony extends Command {
		CmdDesignColony  (String descr, String... keys) {
			super(descr, keys);
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processColony(param); }
	}
	private class CmdDesignScout extends Command {
		CmdDesignScout  (String descr, String... keys) {
			super(descr, keys);
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processScout(param); }
	}
	private class CmdDesignBomber extends Command {
		CmdDesignBomber  (String descr, String... keys) {
			super(descr, keys);
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processBomber(param); }
	}
	private class CmdSelectSpecialSlot extends Command {
		CmdSelectSpecialSlot  (String descr, String... keys) {
			super(descr, keys);
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processSpecialSlot(param); }
	}
	private class CmdSelectSpecialType extends Command {
		CmdSelectSpecialType  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" TypeId");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processSpecialType(param); }
	}
	private class CmdSelectWeaponSlot extends Command {
		CmdSelectWeaponSlot  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" SlotId");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processWeaponSlot(param); }
	}
	private class CmdSelectWeaponType extends Command {
		CmdSelectWeaponType  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" TypeId");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processWeaponType(param); }
	}
	private class CmdSelectWeaponCount extends Command {
		CmdSelectWeaponCount  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" Count");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processWeaponCount(param); }
	}
	private class CmdSelectEcm extends Command {
		CmdSelectEcm  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" Index");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processEcm(param); }
	}
	private class CmdSelectShield extends Command {
		CmdSelectShield  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" Index");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processShield(param); }
	}
	private class CmdSelectArmor extends Command {
		CmdSelectArmor  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" Index");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processArmor(param); }
	}
	private class CmdSelectComputer extends Command {
		CmdSelectComputer  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" Index");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processComputer(param); }
	}
	private class CmdSelectManeuver extends Command {
		CmdSelectManeuver  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" Index");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processManeuver(param); }
	}
	private class CmdSelectEngine extends Command {
		CmdSelectEngine  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" Index");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processEngine(param); }
	}
	private class CmdSelectHull extends Command {
		CmdSelectHull  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" Index");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processHull(param); }
	}
	private class CmdCopyDesign extends Command {
		CmdCopyDesign  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" DesignId");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processCopy(param); }
	}
	private class CmdScrapDesign extends Command {
		CmdScrapDesign  (String descr, String... keys) {
			super(descr, keys);
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processScrap(param); }
	}
	private class CmdClearDesign extends Command {
		CmdClearDesign  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(optional(CLEAR_WEAPONS_ONLY));
			cmdHelp("Resets all of the selected components, for undeployed design only"
					+ NEWLINE + optional(CLEAR_WEAPONS_ONLY) + " : Resets Weapons only");
		}
		@Override protected String execute(Entries param) { return processClear(param); }
	}
	private class CmdDeployDesign extends Command {
		CmdDeployDesign  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" ShipName");
			cmdHelp(text("SHIP_DESIGN_HELP_4G")
					+ NEWLINE + "ShipName" + " : Mandatory parameter");
		}
		@Override protected String execute(Entries param) { return processDeploy(param); }
	}
	private class CmdSelectDesign extends Command {
		CmdSelectDesign  (String descr, String... keys) {
			super(descr, keys);
			cmdParam(" Index");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return processDesign(param); }
	}
	private class CmdGlobalInfo extends Command {
		CmdGlobalInfo  (String descr, String... keys) {
			super(descr, keys);
			//cmdParam(" Index");
			cmdHelp("No secondary options");
		}
		@Override protected String execute(Entries param) { return designSlotsOptions(); }
	}
	// ################### SUB COMMAND MENU CLASSES ######################
	private class DesignMenu extends CommandMenu {
		DesignMenu  (String name, CommandMenu parent) {
			super(name, parent);
			addCmd(new CmdGlobalInfo("Show Global Info", DESIGN_INFO));
			addCmd(new CmdSelectDesign("Select Design", DESIGN_SELECT));
			addCmd(new CmdSelectHull("Select Hull size", DESIGN_HULL));
			addCmd(new CmdSelectEngine("Select Engine", DESIGN_ENGINE));
			addCmd(new CmdSelectComputer("Select Computer", DESIGN_COMPUTER));
			addCmd(new CmdSelectManeuver("Select Combat Speed", DESIGN_MANEUVER));
			addCmd(new CmdSelectArmor("Select Design", DESIGN_ARMOR));
			addCmd(new CmdSelectShield("Select Shield", DESIGN_SHIELD));
			addCmd(new CmdSelectEcm("Select Ecm", DESIGN_ECM));
			addCmd(new CmdSelectWeaponSlot("Select Weapon Slot", DESIGN_WEAPON_SLOT));
			addCmd(new CmdSelectWeaponType("Select Weapon", DESIGN_WEAPON_TYPE));
			addCmd(new CmdSelectWeaponCount("Select Weapon", DESIGN_WEAPON_COUNT));
			addCmd(new CmdSelectSpecialSlot("Select Special Device Slot", DESIGN_SPECIAL_SLOT));
			addCmd(new CmdSelectSpecialType("Select Special Device Type", DESIGN_SPECIAL_TYPE));
			addCmd(new CmdDeployDesign("Deploy Design", DESIGN_DEPLOY));
			addCmd(new CmdCopyDesign("Copy Design", DESIGN_COPY));
			addCmd(new CmdClearDesign("Copy Design", DESIGN_CLEAR));
			addCmd(new CmdScrapDesign("Scrap Design", DESIGN_SCRAP));
			addCmd(new CmdAutoBuild("Automatically design the most suitable ship", DESIGN_AUTO_BUILD));
			addCmd(new CmdDesignFighter("Automatically design a Fighter ship", DESIGN_FIGHTER));
			addCmd(new CmdDesignColony("Automatically design a Colony ship", DESIGN_COLONY));
			addCmd(new CmdDesignScout("Automatically design a Scout ship", DESIGN_SCOUT));
			addCmd(new CmdDesignBomber("Automatically design a Bomber ship", DESIGN_BOMBER));
			addCmd(new CmdAutoScout("Toggle auto Scout", DESIGN_AUTO_SCOUT));
			addCmd(new CmdAutoColonize("Toggle auto Colonize", DESIGN_AUTO_COLONY));
			addCmd(new CmdAutoAttack("Toggle auto Attack", DESIGN_AUTO_ATTACK));
		}
		@Override public String open(String out)	{
			out = super.open(out);
			RotPUI.instance().selectDesignPanel();
			out += designSlotInfo();
			return out;
		}
		@Override public String exitPanel()	{
			designUI().exit(false);
			return super.exitPanel();
		}
	}
}
