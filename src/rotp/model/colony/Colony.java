/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU GeneraFl Public License, Version 3 (the "License");
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
package rotp.model.colony;

import static rotp.model.colony.ColonySpendingCategory.MAX_TICKS;

import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.List;

import rotp.model.empires.DiplomaticTreaty;
import rotp.model.empires.Empire;
import rotp.model.empires.EmpireView;
import rotp.model.events.SystemAbandonedEvent;
import rotp.model.events.SystemCapturedEvent;
import rotp.model.events.SystemDestroyedEvent;
import rotp.model.events.SystemRandomEvent;
import rotp.model.galaxy.IMappedObject;
import rotp.model.galaxy.ShipFleet;
import rotp.model.galaxy.StarSystem;
import rotp.model.galaxy.Transport;
import rotp.model.game.GovernorOptions;
import rotp.model.incidents.ColonyCapturedIncident;
import rotp.model.incidents.ColonyInvadedIncident;
import rotp.model.planet.Planet;
import rotp.model.ships.Design;
import rotp.model.ships.ShipDesign;
import rotp.model.ships.ShipDesignLab;
import rotp.model.tech.Tech;
import rotp.model.tech.TechMissileWeapon;
import rotp.model.tech.TechTree;
import rotp.ui.RotPUI;
import rotp.ui.notifications.GNNNotification;
import rotp.ui.notifications.InvadersKilledAlert;
import rotp.ui.notifications.TransportsKilledAlert;
import rotp.util.Base;

public final class Colony implements Base, IMappedObject, Serializable {
    private static final long serialVersionUID = 1L;
    private static final int[] validationSeq = { 3, 2, 0, 1, 4 };
    private static final int[] spendingSeq = { 4, 0, 1, 2, 3 };
    // private static final int[] cleanupSeq =  { 2, 4, 1, 0, 3 };
    private static final String[] categoryNames = { "MAIN_COLONY_SHIP", "MAIN_COLONY_DEFENSE", "MAIN_COLONY_INDUSTRY",
                    "MAIN_COLONY_ECOLOGY", "MAIN_COLONY_TECHNOLOGY" };

    public static String categoryName(int i) {
        return categoryNames[i];
    }

    // if these values are changed, the spendingSeq array needs to be changed
    public static final int NUM_CATS = 5;
    public static final int SHIP = 0;
    public static final int DEFENSE = 1;
    public static final int INDUSTRY = 2;
    public static final int ECOLOGY = 3;
    public static final int RESEARCH = 4;

    private static final float TECH_PLUNDER_PCT = 0.02f;
    private static final int MAX_TECHS_CAPTURED = 6;
    private static final int TARGETED_DAMAGE_FOR_POPLOSS = 400;
    private static final int TARGETED_DAMAGE_FOR_FACTLOSS = 100;
    private static final int UNTARGETED_DAMAGE_FOR_POPLOSS = 200;
    private static final int UNTARGETED_DAMAGE_FOR_FACTLOSS = 50;

    public enum Orders {
        NONE(""), SHIELD("TECH_ALLOCATE_SHIELD"), BASES("TECH_ALLOCATE_MISSILE_BASES"), FACTORIES(
                        "TECH_ALLOCATE_FACTORIES"), SOIL("TECH_ALLOCATE_ENRICH_SOIL"), ATMOSPHERE(
                        "TECH_ALLOCATE_ATMOSPHERE"), TERRAFORM("TECH_ALLOCATE_TERRAFORM"), POPULATION("TECH_ALLOCATE_POPULATION");
        private final String label;
        Orders(String s) { label = s; }
        @Override
        public String toString() { return label; }
    }

    private Empire empire;
    private final Planet planet;
    private Transport transport;

    private float population = 0;
    private float previousPopulation = 0;
    private int rebels = 0;
    private float captives = 0;
    private float reserveIncomeBC = 0;
    private boolean rebellion = false;
    private boolean quarantined = false;
    private int fortressNum = 0;
    private final int[] allocation = new int[NUM_CATS];
    private final boolean[] locked = new boolean[NUM_CATS];
    private final EnumMap<Colony.Orders, Float> orders = new EnumMap<>(Orders.class);
    public ColonySpendingCategory[] spending = new ColonySpendingCategory[] { new ColonyShipyard(), new ColonyDefense(),
            new ColonyIndustry(), new ColonyEcology(), new ColonyResearch() };

    private boolean underSiege = false;
    public  boolean keepEcoLockedToClean;
    private boolean transportAutoEco = false;

    private transient boolean hasNewOrders = false;
    private transient int cleanupAllocation = 0;
    private transient boolean recalcSpendingForNewTaxRate;
    public  transient boolean reallocationRequired = false;

    public boolean transportAutoEco()          { return transportAutoEco && !governor; }
    public void    transportAutoEco(boolean b) { transportAutoEco = b; }
    public boolean toggleTransportAutoEco()	   {
    	transportAutoEco(!transportAutoEco());
    	return !governor;
    }
    public void toggleRecalcSpending()         { recalcSpendingForNewTaxRate = true; }
    public boolean underSiege()                { return underSiege; }
    public float reserveIncome()               { return reserveIncomeBC; }
    public void clearReserveIncome()           { reserveIncomeBC = 0; }
    public void adjustReserveIncome(float bc)  { reserveIncomeBC += bc; }
    public boolean quarantined()               { return quarantined; }
    public void becomeQuarantined()            { quarantined = true; }
    public void clearQuarantine()              { quarantined = false; }
    public int fortressNum()                   { return fortressNum; }
    public int allocation(int i)               { return allocation[i]; }
    public void allocation(int i, int val)     { allocation[i] = val; }
    public void addAllocation(int i, int val)  { allocation[i]+= val; }
    public int setAllocation(int i, int val) {
        // attempt to set allocation for category[i] to val
        // do not add more than allocationRemaining()
        // do not reduce current allocation
        int addMax = allocationRemaining();
        int addAmt = max(0, val-allocation(i));
        addAllocation(i,min(addMax, addAmt));
        if(i == ECOLOGY){
            keepEcoLockedToClean = false;
        }
        return addAmt;
    }
    public boolean locked(int i)               { return locked[i]; }
    public void locked(int i, boolean b)       { locked[i] = b; }
    public void toggleLock(int i)              { locked[i] = !locked[i]; }
    public boolean hasNewOrders()              { return hasNewOrders; }
    public void hasNewOrders(boolean b)        { hasNewOrders = b; }
    public float pct(int i)                    { return (float)allocation[i]/MAX_TICKS; }
    public void pct(int i, float d)            { allocation[i]=(int)Math.ceil(d*MAX_TICKS); }
    public void addPct(int i, float d)         { pct(i, pct(i)+d); }

    public boolean warning(int i)              { return category(i).warning(); }
    public boolean canAdjust(int i) {
        if (inRebellion() || locked(i))
            return false;
        else
            return true;
    }
    public float untargetedHitPoints()         { return UNTARGETED_DAMAGE_FOR_POPLOSS * population(); }
    public void clearAllRebellion() {
        rebels = 0;
        rebellion = false;
    }
    public float currentProductionCapacity() {
        // modnar: use direct production capacity formula
        // formula below is not accurate
        return production()/maxProduction();
        /*
        // returns a pct (0 to 1) representing the colony's current
        // production vs its maximum possible formula
        float pop = population();
        float maxPop = planet().maxSizeAfterSoilAtmoTform();
        float maxFactories = maxPop * industry().maxRobotControls();
        float factories = min(maxFactories, industry().factories(), industry().maxUseableFactories());
        
        float workerProd = empire.workerProductivity();
        float maxProd = maxFactories + (maxPop * workerProd);
        float currProd = factories + (pop*workerProd);
        return currProd/maxProd;
        */
    }
    public boolean creatingWaste() {
        int needed = ecology().cleanupAllocationNeeded();
        int curr = ecology().allocation();
        return curr < needed;
    }
    private int cleanupAllocation() {
        if (cleanupAllocation < 0)
            cleanupAllocation = ecology().cleanupAllocationNeeded();
        return cleanupAllocation;
    }
    @Override
    public String toString()                   { return "Colony: " + name();  }

    public Colony(Empire c, Planet p) {
        empire = c;
        planet = p;
        init();
    }
    private void init() {
        buildFortress();
        clearTransport();

        for (int i = 0; i < spending.length; i++)
            spending[i].init(this);

        setPopulation(2);
        shipyard().goToNextDesign();
        defense().updateMissileBase();
        defense().maxBases(empire().defaultMaxBases());
        cleanupAllocation = -1;

        // empires of orbiting fleets should see ownership change
        StarSystem sys = starSystem();
        List<ShipFleet> fleets = sys.orbitingFleets();
        for (ShipFleet fl: fleets) {
            Empire flEmp = fl.empire();
            if (flEmp != empire)
                flEmp.sv.refreshFullScan(sys.id);
        }
		if (empire().isPlayer())
			setDefaultGovernor();
    }
    public boolean isBuildingShip() { return shipyard().design() instanceof ShipDesign; }
    private void buildFortress()    { fortressNum = empire.race().randomFortress(); }
    public boolean isAutopilot()    { return empire.isAIControlled(); }
    // MappedObject overrides
    @Override
    public float x()               { return starSystem().x(); }
    @Override
    public float y()               { return starSystem().y(); }
    public StarSystem starSystem()  { return planet().starSystem(); }
    public Planet planet()          { return planet; }
    public Empire empire()          { return empire; }
    public TechTree tech()          { return empire.tech(); }
    public String name()            { return starSystem().name(); }

    public ColonySpendingCategory category(int i) { return spending[i]; }
    public ColonyShipyard shipyard() { return (ColonyShipyard) spending[SHIP]; }
    public ColonyDefense defense()   { return (ColonyDefense) spending[DEFENSE]; }
    public ColonyIndustry industry() { return (ColonyIndustry) spending[INDUSTRY]; }
    public ColonyEcology ecology()   { return (ColonyEcology) spending[ECOLOGY]; }
    public ColonyResearch research() { return (ColonyResearch) spending[RESEARCH]; }

    public boolean hasStargate()         { return shipyard().hasStargate(); }
    public boolean hasStargate(Empire e) { return (empire == e) && hasStargate(); }
    public void removeStargate()         { shipyard().removeStargate(); }

    public int totalAmountAllocated() {
        int amt = 0;
        for (ColonySpendingCategory cat : spending)
            amt += cat.allocation();

        return amt;
    }
    public int allocationRemaining()              { return MAX_TICKS - totalAmountAllocated(); }
    public float totalPlanetaryResearch()         { 
        float totalBC = research().totalSpending();
        float productAdj = planet().productionAdj();
        if (empire.divertColonyExcessToResearch()) {
            totalBC += shipyard().excessSpending() / productAdj;
            totalBC += defense().excessSpending() / productAdj;
            totalBC += industry().excessSpending() / productAdj;
            totalBC += ecology().excessSpending();
        }        
        float totalRP = totalBC * research().researchBonus();
        return max(0, totalRP-research().projectRemainingBC());
    }
    public float totalPlanetaryResearchSpending() { 
        float totalBC = research().totalSpending(); 
        float productAdj = planet().productionAdj();
        if (empire.divertColonyExcessToResearch()) {
            totalBC += shipyard().excessSpending() / productAdj;
            totalBC += defense().excessSpending() / productAdj;
            totalBC += industry().excessSpending() / productAdj;
            totalBC += ecology().excessSpending();
        }
        return totalBC;
    }  
    public String printString() {
        return empire.sv.name(starSystem().id) + "-- pop:"
                + (float) Math.round(population() * 100) / 100 + " reb:"
                + (float) Math.round(rebels * 100) / 100 + " fac:"
                + (float) Math.round(industry().factories() * 100) / 100 + " was:"
                + (float) Math.round(ecology().waste() * 100) / 100 + " bas:"
                + (float) Math.round(defense().bases() * 100) / 100 + " shd:"
                + (float) Math.round(defense().shield() * 100) / 100;
    }

    public int displayPopulation()          { return population < 1 ? (int) Math.ceil(population) : (int) population; }
    public float population()               { return population; }
    public void setPopulation(float pop)    { population = pop; }
    public void adjustPopulation(float pop) { population += pop; }
    public int rebels()                     { return rebels; }
    public void rebels(int i)               { rebels = i; }
    public int deltaPopulation()            { return (int) population - (int) previousPopulation - (int) inTransport(); }
    public boolean destroyed()              { return population <= 0; }
    public boolean inRebellion()            { return rebellion && (rebels > 0); }
    public float rebellionPct()             { return rebels / population(); }
    public boolean hasOrders()              { return !orders.isEmpty(); }
    public boolean hasOrder(int cat)		{ // BR:
    	switch (cat) {
	    	case DEFENSE:
	    		return orders.containsKey(Orders.SHIELD)
	    				|| orders.containsKey(Orders.BASES);
	    	case INDUSTRY:
	    		return orders.containsKey(Orders.FACTORIES);
	    	case ECOLOGY:
	    		return orders.containsKey(Orders.SOIL)
	    				|| orders.containsKey(Orders.ATMOSPHERE)
	    	    		|| orders.containsKey(Orders.TERRAFORM)
	    				|| orders.containsKey(Orders.POPULATION);
	    	case SHIP:
	    	case RESEARCH:
    		default:
    			return false;
    	}
    }

    public boolean isDeveloped()  {
    	return options().isDeveloped(this);
        // return defense().isCompleted() && industry().isCompleted() && ecology().isCompleted(); 
    }
    public float orderAmount(Colony.Orders order) {
        Colony.Orders priorityOrder = empire.priorityOrders();
        // amount for this order
        float amt = orders.containsKey(order) ? orders.get(order) : 0;
        // if empire has a priority and this is not it, return 0
        if (orders.containsKey(priorityOrder))
            return order == priorityOrder ? amt : 0;
        else
            return amt;
    }
    public void forcePct(int catNum, float d) {
        // occurs when new spending orders are applied
        allocation(catNum, (int) Math.ceil(d*MAX_TICKS));
        realignSpending(spending[catNum]);
        spending[catNum].removeSpendingOrders();
    }
    public void setCleanupPct(float d) {
        // occurs when ai is reseting eco for minimum cleanup
        allocation(ECOLOGY, (int) Math.ceil(d*MAX_TICKS));
        redistributeReducedEcoSpending();
    }
    public boolean increment(int catNum, int amt) {
        if (!canAdjust(catNum))
            return false;

        int newValue = allocation(catNum)+amt;
        if ((newValue < 0)
        || (newValue > MAX_TICKS))
            return false;

        if (catNum == ECOLOGY)
            keepEcoLockedToClean = false;

        allocation(catNum, newValue);
        realignSpending(spending[catNum]);
        spending[catNum].removeSpendingOrders();
        return true;
    }
    public String shipyardProject() {
        if (shipyard().allocation() > 0) {
            int limit = shipyard().buildLimit();
            if (limit == 0)
                return shipyard().design().name();
            else
                return str(limit)+" "+shipyard().design().name();
        }
        return "";
    }
    public void clearSpending() {
        allocation(SHIP, 0);
        allocation(DEFENSE, 0);
        allocation(INDUSTRY, 0);
        allocation(ECOLOGY, 0);
        allocation(RESEARCH, 0);
    }
    public void clearUnlockedSpending() {
        if (!locked(SHIP))
            allocation(SHIP, 0);
        if (!locked(DEFENSE))
            allocation(DEFENSE, 0);
        if (!locked(INDUSTRY))
            allocation(INDUSTRY, 0);
        if (!locked(ECOLOGY))
            allocation(ECOLOGY, 0);
        if (!locked(RESEARCH))
            allocation(RESEARCH, 0);
    }
    public void addColonyOrder(Colony.Orders order, float amt) {
        if (amt == 0)
            return;
        if (planet().isEnvironmentHostile() && order == Orders.SOIL)
            return;
        if (!planet().isEnvironmentHostile() && order == Orders.ATMOSPHERE)
            return;
        if (starSystem().inNebula() && order == Orders.SHIELD)
            return;
        
        float existingAmt = orders.containsKey(order) ? orders.get(order) : 0;

        if (amt <= existingAmt)
            return;

        hasNewOrders = true;
        orders.put(order, amt);
        reallocationRequired = true;
    }
    public void removeColonyOrder(Colony.Orders order) {
        if (orders.containsKey(order)) {
            orders.remove(order);
            reallocationRequired = true;
        }
    }
    public void forceOrder(int cat)		{ // BR:
    	switch (cat) {
	    	case DEFENSE:
	   			addColonyOrder(Orders.BASES, 1);
	    		return;
	    	case INDUSTRY:
	   			addColonyOrder(Orders.FACTORIES, 1);
	    		return;
	    	case ECOLOGY:
	   			addColonyOrder(Orders.POPULATION, 1);
	   			keepEcoLockedToClean = false;
	    		return;
	    	case SHIP:
	    	case RESEARCH:
			default:
    	}
    }
    public void removeOrder(int cat)	{ // BR:
    	switch (cat) {
	    	case DEFENSE:
	   			removeColonyOrder(Orders.BASES);
	   			removeColonyOrder(Orders.SHIELD);
	    		return;
	    	case INDUSTRY:
	   			removeColonyOrder(Orders.FACTORIES);
	    		return;
	    	case ECOLOGY:
	   			removeColonyOrder(Orders.ATMOSPHERE);
	   			removeColonyOrder(Orders.SOIL);
	   			removeColonyOrder(Orders.TERRAFORM);
	   			removeColonyOrder(Orders.POPULATION);
	   			keepEcoLockedToClean = true;
	    		return;
	    	case SHIP:
	    	case RESEARCH:
			default:
		}
    }
    public boolean toggleOrder(int cat)	{ // BR:
	    if (hasOrder(cat)) {
	    	removeOrder(cat);
	    	return false;
	    }
	    else {
	    	forceOrder(cat);
	    	return true;
	    }
    }
    public void smoothMaxSlider(int category) {
    	if(locked(category))
    		return;
    	verifiedSmoothMaxSlider(category, null);
    }
    public void verifiedSmoothMaxSlider(int category, MouseEvent e) {
    	int prevTech = allocation(RESEARCH);
        checkEcoAtClean(); // BR: to avoid wrong setting if not clean!
        int allocationNeeded = category(category).smartAllocationNeeded(e);
        int prevAllocation = allocation(category);
        int incr = allocationNeeded > prevAllocation ? 1 : -1;
        int lim = (allocationNeeded - prevAllocation) * incr;
        for (int i=0; i<lim; i++) {
        	if(!increment(category, incr))
        		break;
        }
        if(category != ECOLOGY)
        	checkEcoAtClean();
        if (category != RESEARCH ) {
        	int deltaTech = allocation(RESEARCH) - prevTech;
        	if (deltaTech > 0) { // Smart distribution of the decremented spending
        		allocation(RESEARCH, prevTech);
        		redistributeSpending(category);
        	}
        }
    }

    // modnar: add challengeMode option from UserPreferences to give AI more initial resources
	private boolean challengeMode = options().selectedChallengeMode();
	
	public void setHomeworldValues() {
        Empire emp = empire();
        ShipDesignLab lab = emp.shipLab();
        ShipDesign scout = lab.scoutDesign();
        ShipDesign colony = lab.colonyDesign();
        
		if (emp.isPlayer())
			setDefaultGovernor();
		// modnar: normal resources for player or non-challengeMode
		if (emp.isPlayer() || !challengeMode) {
	        setPopulation(50);
	        previousPopulation = population();
	        industry().factories(30);
	        industry().previousFactories(30);
	
	        galaxy().ships.buildShips(emp.id, starSystem().id, scout.id(), 2);
	        galaxy().ships.buildShips(emp.id, starSystem().id, colony.id(), 1);
	        lab.recordConstruction(scout, 2);
	        lab.recordConstruction(colony, 1);
		}
		// modnar: add extra starting resources, if challengeMode and AI
		// double initial ships, increase pop/factories to approximately double initial production
		if (emp.isAI() && challengeMode) {
	        setPopulation(80);
	        previousPopulation = population();
	        industry().factories(80);
	        industry().previousFactories(80);
	
	        galaxy().ships.buildShips(emp.id, starSystem().id, scout.id(), 4);
	        galaxy().ships.buildShips(emp.id, starSystem().id, colony.id(), 2);
	        lab.recordConstruction(scout, 4);
	        lab.recordConstruction(colony, 2);
		}
	}
    
    // modnar: add option to start game with additional colonies
    public void setCompanionWorldValues() {
        Empire emp = empire();
		
		if (emp.isPlayer())
			setDefaultGovernor();
		// modnar: normal resources for player or non-challengeMode
		if (emp.isPlayer() || !challengeMode) {
	        setPopulation(30);
	        previousPopulation = population();
	        industry().factories(20);
	        industry().previousFactories(20);
		}
		// modnar: add extra starting resources, if challengeMode and AI
		// increase pop/factories to approximately double initial production
		if (emp.isAI() && challengeMode) {
	        setPopulation(50);
	        previousPopulation = population();
	        industry().factories(50);
	        industry().previousFactories(50);
		}
    }

    public void spreadRebellion() {
        inciteRebels(0.50f, "GNN_PLAYER_REBELLION_SPREAD");
    }
    public int inciteRebels(float pct, String messageKey) {
        // always guaranteed to incite at least 1 pop
        int newRebels = max(1, (int) Math.ceil(pct * population()));
        rebels = (int) min(population(), rebels + newRebels);

        if (rebels >= population() / 2)
            rebel(messageKey);
        return newRebels;
    }
    public void rebel(String messageKey) {
        if (inRebellion())
            return;
        StarSystem sys = starSystem();
        rebellion = true;
        sys.addEvent(new SystemRandomEvent("SYSEVENT_REBELLION"));
        empire.setRecalcDistances();
        if (empire.inRevolt()) {
            empire.overthrowLeader();
            return;
        }
        Empire pl = player();
        String message = null;
        if (empire == pl) {
            message = text(messageKey, sys.name(), rebels);
            message = empire.replaceTokens(message, "rebelling");
            galaxy().giveAdvice("MAIN_ADVISOR_REBELLION", sys.name());
            GNNNotification.notifyRebellion(message, true);
        }
        else if (empire.hasContact(pl) && pl.sv.isScouted(sys.id)) {
            message = text("GNN_ALIEN_REBELLION", pl.sv.name(sys.id), rebels);
            message = empire.replaceTokens(message, "rebelling");
            GNNNotification.notifyRebellion(message, false);
        }
//        if (message != null)
//            GNNNotification.notifyRebellion(message);
    }

    public float orderAdjustment() {
        // if orders for different spending categories exceed 100%
        // we need a modifier to adjust them back down so they don't
        float totalOrders = 0;
        for (int i = 0; i < NUM_CATS; i++) {
            ColonySpendingCategory cat = category(i);
            totalOrders += cat.orderedValue();
        }
        return totalOrders <= 100 ? 1.0f : 100.0f / totalOrders;
    }
    public void validate() {
        int maxTicks = ColonySpendingCategory.MAX_TICKS;

        // bounds all allocations from 0 to max_ticks (in case negative values in save)
        // do them in spending sequence order to ensure ECO spending doesn't get shorted
        for (int i=0;i<allocation.length;i++) {
            int index = validationSeq[i];
            allocation[index] = min(maxTicks, max(0,allocation[index]));
            maxTicks -= allocation[index];
        }
    }
    public void validateOnLoad() {
        if (population < 0)
            previousPopulation = population = 1;
        if (planet.waste() > planet.maxWaste()) {
            planet.resetWaste();
            planet.addWaste(planet.maxWaste());
        }
        cleanupAllocation = -1;
    }

    public void nextTurn() {
        log("Colony: ", empire.sv.name(starSystem().id),  ": NextTurn [" , shipyard().design().name() , "|" ,str(shipyard().allocation()) , "-"
                    , str(defense().allocation()) , "-" , str(industry().allocation()) , "-" , str(ecology().allocation()) , "-"
                    , str(research().allocation()) , "]");
        keepEcoLockedToClean = empire().isPlayerControlled() && (allocation[ECOLOGY] <= cleanupAllocation());
        previousPopulation = population;
        reallocationRequired = false;          
        ensureProperSpendingRates();
        validateOnLoad();

        // if rebelling, nothing happens (only enough prod assumed to clean new
        // waste and maintain existing structures)
        if (inRebellion())
            return;

        // after turn is over, we may need to reset ECO spending to adjust for cleanup
        // make sure that the colony's expenses aren't too high
        empire().governorAI().lowerExpenses(this);

        // if colony is still in the hole, give it up
        float totalProd = totalProductionIncome();
        float reserveIncome = maxReserveIncome();

        if ((totalProd <= 0) && (reserveIncome <= 0))
            return;

        float usedReserve = maxReserveIncome();

        shipyard().nextTurn(totalProd, reserveIncome);
        defense().nextTurn(totalProd, reserveIncome);
        industry().nextTurn(totalProd, reserveIncome);
        ecology().nextTurn(totalProd, reserveIncome);
        research().nextTurn(totalProd, reserveIncome);

        // COMMIT CONTRIBUTIONS
        defense().commitTurn();
        industry().commitTurn();
        ecology().commitTurn();
        research().commitTurn();

        planet().removeExcessWaste();
        adjustReserveIncome(-usedReserve);

        if ((shipyard().allocation() < 0) || (defense().allocation() < 0) || (industry().allocation() < 0)
            || (ecology().allocation() < 0) || (research().allocation() < 0)) {
            err("ERROR: bad allocation for " + starSystem().name() + " ship:" + shipyard().allocation() + " def:"
                    + defense().allocation() + " ind:" + industry().allocation() + " eco:" + ecology().allocation()
                    + " res:" + research().allocation());
            throw new RuntimeException("ERROR: bad allocation for " + starSystem().name() + " ship:"
                    + shipyard().allocation() + " def:" + defense().allocation() + " ind:" + industry().allocation()
                    + " eco:" + ecology().allocation() + " res:" + research().allocation());
        }
    }
    public void assessTurn() {
        underSiege = orbitingEnemyShips();
        shipyard().assessTurn();
        defense().assessTurn();
        industry().assessTurn();
        ecology().assessTurn();
        research().assessTurn();
        checkEcoAtClean();
        
        if (reallocationRequired)
            empire().governorAI().setColonyAllocations(this);

    }
    public void addFollowUpSpendingOrder(float orderAmt) {
        if (orderAmt <= 0)
            return;

        // a spending order has completed, ripple it down to the next priority
        // so the colony progresses to completion
        ColonyEcology eco = ecology();
        ColonyIndustry ind = industry();
        ColonyDefense def = defense();
        if (!eco.terraformCompleted()) 
            addColonyOrder(Colony.Orders.TERRAFORM, orderAmt);
        else if (!ind.isCompleted())
            addColonyOrder(Colony.Orders.FACTORIES, orderAmt);
        else if (!eco.populationGrowthCompleted())
            addColonyOrder(Colony.Orders.POPULATION, orderAmt);
        else if (!def.shieldAtMaxLevel())
            addColonyOrder(Colony.Orders.SHIELD, orderAmt);
        else if (!def.missileBasesCompleted())
            addColonyOrder(Colony.Orders.BASES, orderAmt/5);
    }
    public void checkEcoAtClean() {
        recalcSpendingForNewTaxRate = false;
        if (locked[ECOLOGY]) 
            return;
        
        int cleanAlloc = ecology().cleanupAllocationNeeded();
        if (allocation[ECOLOGY] == cleanAlloc)
            return;
        
        // always ensure we are at least at clean
        if (allocation[ECOLOGY] < cleanAlloc) {
            allocation[ECOLOGY] = cleanupAllocation = cleanAlloc;
            redistributeReducedEcoSpending();
            return;
        }
        
        // if we are over clean but the colony started its turn at clean
        // then lower
        if ((allocation[ECOLOGY] > cleanAlloc) && keepEcoLockedToClean) {
            allocation[ECOLOGY] = cleanupAllocation = cleanAlloc;
            redistributeReducedEcoSpending();
        }
    }
    public void checkEcoAtTerraform() {
        recalcSpendingForNewTaxRate = false;
        if (locked[ECOLOGY]) 
            return;
        int terraformAlloc = ecology().terraformAllocationNeeded();
        if (allocation[ECOLOGY] == terraformAlloc)
            return;
        allocation[ECOLOGY] = cleanupAllocation = terraformAlloc;
        redistributeReducedEcoSpending();
    }
    public void lowerECOToCleanIfEcoComplete() {
        // this will NOT adjust ECO spending if it is locked
        // or manually set to level lower than clean
        if (locked[ECOLOGY])
            return;
        
        int cleanAlloc = ecology().cleanupAllocationNeeded();
        if (allocation[ECOLOGY] < cleanAlloc)
            return;
        
        // if ECO spending is complete, just lower ECO to clean
        // and auto-realign the other categories
//        if (ecology().isCompleted()) {
        if (ecology().isTerraformed()) { // BR: 
            if (allocation[ECOLOGY] != cleanAlloc) {
                allocation(ECOLOGY, cleanAlloc);
                realignSpending(ecology());
            }
            return;
        }
            
        // if ECO is not complete, then if it exceeds
        // maxAlloc, then lower it to that and realign
        int maxAlloc = ecology().maxAllocationNeeded();
        if (allocation[ECOLOGY] > maxAlloc) {
                allocation(ECOLOGY, maxAlloc);
                realignSpending(ecology());
        }
        
    }
    public boolean canLowerMaintenance() { return transporting(); }

    public void lowerMaintenance() {
        if (transporting()) {
            int maxSize = maxTransportsAllowed();
            if (inTransport() > maxSize) {
                // log("reducing transport levels from ", planet.name(), " to ",
                // str(transport().originalSize), " to avoid income loss");
                transport().size(maxSize);
                transport().launchSize(maxSize);
            } else {
                // log("cancelling transports from ", planet.name(), " to
                // avoid income loss");
                clearTransport();
            }
        }
    }
    public void reallocateSpending(int cat, int ticks) {
        if (allocation(cat) >= ticks)
            return;

        int tmp = allocation(cat);
        int delta = ticks - tmp;

        allocation(cat, ticks);

        // deduct from research 1st
        if (cat != RESEARCH) {
            tmp = allocation(RESEARCH);
            if (tmp >= delta) {
                allocation(RESEARCH, allocation(RESEARCH) - delta);
                return;
            } else {
                allocation(RESEARCH, 0);
                delta -= tmp;
            }
        }

        // deduct from ships 2nd
        if (cat != SHIP) {
            tmp = allocation(SHIP);
            if (tmp >= delta) {
                allocation(SHIP, allocation(SHIP) - delta);
                return;
            } else {
                allocation(SHIP, 0);
                delta -= tmp;
            }
        }

        // deduct from defense 3rd
        if (cat != DEFENSE) {
            tmp = allocation(DEFENSE);
            if (tmp >= delta) {
                allocation(DEFENSE, allocation(DEFENSE) - delta);
                return;
            } else {
                allocation(DEFENSE, 0);
                delta -= tmp;
            }
        }

        // deduct from industry 4th
        if (cat != INDUSTRY) {
            tmp = allocation(INDUSTRY);
            if (tmp >= delta) {
                allocation(INDUSTRY, allocation(INDUSTRY) - delta);
                return;
            } else {
                allocation(INDUSTRY, 0);
                delta -= tmp;
            }
        }

        // deduct from ecology last
        if (cat != ECOLOGY) {
            tmp = allocation(ECOLOGY);
            if (tmp >= delta)
                allocation(ECOLOGY, allocation(ECOLOGY) - delta);
            else {
                allocation(ECOLOGY, 0);
                delta -= tmp;
            }
        }
    }
    public void redistributeReducedEcoSpending() {
        int maxAllocation = ColonySpendingCategory.MAX_TICKS;
        // determine how much categories are over/under spent
        int spendingTotal = 0;
        for (int i = 0; i < NUM_CATS; i++) {
        	//ColonySpendingCategory sp = spending[i];
        	spendingTotal += spending[i].allocation();
        }

        int adj = maxAllocation - spendingTotal;
        if (adj == 0)
            return;

        // if we are building ships and doing no research, then assume this is a shipbuilding
        // colony and put the rest of the excess in shipbuilding. Good catch, Xilmi
        if (!locked(SHIP) && (spending[SHIP].allocation() > 0) && (spending[RESEARCH].allocation() == 0) && shipyard().buildLimit() == 0)
            adj -= spending[SHIP].adjustValue(adj);
        
        // funnel excess to industry if it's not completed
        if (!industry().isCompleted() && adj > 0)
            adj -= spending[INDUSTRY].adjustValue(min(industry().maxAllocationNeeded() - industry().allocation(), adj));
        

        if (adj == 0)
            return;
        // put whatever is left or take whatever is missing according to the spending-sequence
        for (int i = 0; i < NUM_CATS; i++) {
            ColonySpendingCategory currCat = spending[spendingSeq[i]];
            if ((spendingSeq[i] != ECOLOGY) && !locked(spendingSeq[i]))
                adj -= currCat.adjustValue(adj);
        }
        if (adj == 0)
            return;
        // All other sliders are locked! Put back to eco!
        spending[ECOLOGY].adjustValue(adj);
    }
    public void realignSpending(ColonySpendingCategory cat) {
        int maxAllocation = ColonySpendingCategory.MAX_TICKS;
        // determine how much categories are over/under spent
        int spendingTotal = 0;
        for (int i = 0; i < NUM_CATS; i++)
            spendingTotal += spending[i].allocation();

        int adj = maxAllocation - spendingTotal;

        for (int i = 0; i < NUM_CATS; i++) {
            ColonySpendingCategory currCat = spending[spendingSeq[i]];
            if ((currCat != cat) && !locked(spendingSeq[i]))
                adj -= currCat.adjustValue(adj);
        }

        // if any adj remaining, send back to original cat
        // this is always player-driver, so remove spending orders
        if (adj != 0) {
            cat.adjustValue(adj);
            cat.removeSpendingOrders();
        }
    }
    // BR: For spending panel UI
    public void redistributeSpending(int category) {
        int maxAllocation = ColonySpendingCategory.MAX_TICKS;

        // determine how much categories are over/under spent
        int spendingTotal = 0;
        for (int i = 0; i < NUM_CATS; i++)
            spendingTotal += spending[i].allocation();
        int adj = maxAllocation - spendingTotal;
        if (adj==0)
    		return;
        int[] sequence = {ECOLOGY, INDUSTRY, SHIP, DEFENSE, RESEARCH };

        // Look for orders
        for (int i : sequence) {
            if ((i != category) && !locked(i) && hasOrder(i)) {
            	ColonySpendingCategory currCat = spending[i];
            	int currentAllocation = currCat.allocation();
            	int allocationNeeded  = currCat.smoothAllocationNeeded();
            	int increment = allocationNeeded - currentAllocation;
            	increment = bounds(0, increment, adj);
            	adj -= currCat.adjustValue(increment);
            	if (adj==0)
            		return;
            }
        }
        // distribute the remaining
        for (int i : sequence) {
            if ((i != category) && !locked(i)) {
            	ColonySpendingCategory currCat = spending[i];
            	int currentAllocation = currCat.allocation();
            	int allocationNeeded  = currCat.smoothAllocationNeeded();
            	int increment = allocationNeeded - currentAllocation;
            	increment = bounds(0, increment, adj);
            	adj -= currCat.adjustValue(increment);
            	if (adj==0)
            		return;
            }
        }

        // if any adj remaining, send back to original cat
        // this is always player-driver, so remove spending orders
        if (adj != 0)
            if (category > 0) {
	        	ColonySpendingCategory cat = category(category);
	            cat.adjustValue(adj);
	            cat.removeSpendingOrders();
	        }
            else
            	redistributeReducedEcoSpending();
    }

    public float transportPriority() {
        float pr;
        if (inRebellion())
            pr = planet.currentSize() + rebels;
        else
            pr = (planet.currentSize() - expectedPopulationLongTerm()) / expectedPopPct();

        pr *= planet.productionAdj();
        if (planet.isOrionArtifact())
            pr *= 3;
        else if (planet.isArtifact())
            pr *= 2; // float for artifacts, triple for super-artifacts

        return pr;
    }
    private boolean orbitingEnemyShips() {
        List<ShipFleet> fleets = starSystem().orbitingFleets();
        for (ShipFleet fleet : fleets) {
            if (fleet.isArmed() && empire.atWarWith(fleet.empId()))
                return true;
        }
        return false;
    }
    public boolean hasInterdiction()  { return tech().subspaceInterdiction();  }
    public boolean isHomeworld()      { return ((empire != null) && (empire.homeSysId() == starSystem().id)); }
    public boolean isCapital()        { return ((empire != null) && (empire.capitalSysId() == starSystem().id)); }
    public float workingPopulation() { return population() - inTransport(); }
    public float usedFactories()     {
        return (int) min(industry().factories(), workingPopulation() * industry().effectiveRobotControls());
    }
    public float production() {
        if (inRebellion())
            return 0.0f;
        
        // modnar: add dynamic difficulty option, change AI colony production
        float dynaMod = 1.0f;
        float scaleMod = 1.0f;
        if (options().selectedDynamicDifficulty() && !(galaxy().currentTurn() < 5)) {
            // scale with relative empire industrialPowerLevel (production*tech) compared with player
            // use custom created nonDynaIndPowerLevel, to avoid infinite recursion
            float empIndPowerLevel = empire().nonDynaIndPowerLevel();
            float playerIndPowerLevel = player().nonDynaIndPowerLevel();
            // r_empInd > 1 means more powerful than player, r_empInd < 1 means less powerful than player
            float r_empInd = empIndPowerLevel / playerIndPowerLevel;
            
            /*
            // relative empire industrialPowerLevel compared with galactic average
            float galIndPowerLevel = 0.0f;
            for (Empire emp: galaxy().empires()) {
                galIndPowerLevel += emp.nonDynaIndPowerLevel(emp);                 
            }
            // multiply numActiveEmpires to assess against balanced power distribution relative to galaxy
            // r_empInd > 1 means more powerful than average, r_empInd < 1 means less powerful than average
            float r_empInd = galaxy().numActiveEmpires() * empIndPowerLevel / galIndPowerLevel;
            */
            
            // scale with turns, between 0 to 1, ramp up at around turn 150
            // (little to no dynamic production change in early turns)
            // turnMod(0)=2.12%, turnMod(50)=3.17%, turnMod(100)=6.28%, turnMod(150)=50.0%, turnMod(200)=93.7%, 
            float turnMod = (float) (0.5f + Math.atan(galaxy().currentTurn()/10 - 15)/Math.PI);
            
            // scaling with base function: f(x) = x^3/(x^2+1), asymptotically approach f(x)=x, with flattening near x=0
            // adjust scaling with base function: g(x) = 1-1/(x+1), to get g(0)=0
            if (r_empInd > 1.0f) {
                scaleMod = (float) ((1 + Math.pow(r_empInd-1, 3) / (Math.pow(r_empInd-1, 2) + 0.25f)) / r_empInd);
            }
            else {
                scaleMod = (float) ((1.01f - 1.01f/(100*r_empInd+1)) * (1 + Math.pow(r_empInd-1, 3) / (Math.pow(r_empInd-1, 2) + 0.25f)) / r_empInd);
            }
            
            // put it all together with: h(x,t) = 1+(scaleMod-1)*turnMod, h(x,t) will then multiply onto mod
            // at turns << 150, turnMod ~ 0, h(x,t) ~ 1
            // at turns >> 150, turnMod ~ 1, h(x,t) ~ scaleMod
            dynaMod = 1.0f + (scaleMod - 1.0f) * turnMod;
        }
        
        float mod = empire().isPlayer() ? 1.0f : (options().aiProductionModifier()*dynaMod);
        
        float workerProd = workingPopulation() * empire.workerProductivity();
        float factoryOutput = mod*(workerProd + usedFactories());
        return factoryOutput - transportCost();
    }
    // modnar: add dynamic difficulty option, change AI colony production
    // create unscaled production, nonDynaProd, to avoid infinite recursion
    public float nonDynaProd() {
        if (inRebellion())
            return 0.0f;
        float mod = empire().isPlayer() ? 1.0f : options().aiProductionModifier();
        float workerProd = workingPopulation() * empire.workerProductivity();
        float factoryOutput = mod*(workerProd + usedFactories());
        return factoryOutput - transportCost();
    }
    public float maxProduction() {
        float workerProd = planet().maxSize() * empire.workerProductivity();
        return workerProd + industry().maxFactories();
    }
    public float maxReserveUseable() {
        return production();
    }
    public float maxReserveIncome() {
        return min(reserveIncome(), maxReserveUseable());
    }
    public float maxReserveNeeded() {
        return max(0, maxReserveUseable() - reserveIncome());
    }
    public boolean embargoed() {
        return underSiege || starSystem().piracy() || quarantined();
    }
    public float actualTradeIncome() {
        if (embargoed())
            return 0;
        else
            return production() * empire.tradeIncomePerBC();
    }
    public float totalIncome() {
        return max(0.1f, totalProductionIncome() + maxReserveIncome());
    }
    public float colonyTaxPct() {
        if (embargoed())
            return 0f;
        // we are taxed at the empire rate if the empire is taxing all colonies, or we are finished developing
        float empireTaxPct = empire.empireTaxPct();
        float colonyTaxPct = 0.0f;
        if (empireTaxPct > 0) { // let's avoid unnecessary calls to isDeveloped()
            if (!empire.empireTaxOnlyDeveloped() || isDeveloped())
                colonyTaxPct = empireTaxPct;
        }
        return colonyTaxPct;
    }
    public void ensureProperSpendingRates() {
        if (recalcSpendingForNewTaxRate) 
        {
            checkEcoAtClean();
        }
    }
    public float totalProductionIncome() {
        if (inRebellion())
            return 0.1f;
        
        ensureProperSpendingRates();

        float prod = production();       
        float reserveCost = prod * colonyTaxPct();
        float securityCost = prod * empire.totalSecurityCostPct();
        float shipCost = prod * empire.shipMaintCostPerBC();
        float stargateCost = prod * empire.stargateCostPerBC();
        float tradeIncome = actualTradeIncome();
        float defenseCost = prod * empire.missileBaseCostPerBC();
        float shipyardCost = shipyard().maintenanceCost();

        return max(0, prod - reserveCost - securityCost - defenseCost - shipyardCost + tradeIncome - shipCost - stargateCost);
    }
    public float expectedPopulationLongTerm() {
        return workingPopulation() + normalPopGrowth() + incomingTransports();
    }
    // GameSession.nextTurnProcess() processes transports before normal population growth.
    public float populationAfterNextTurnTransports() {
    	float pop = population() - inTransport() + incomingTransportsNextTurn();
    	pop = min(pop, planet.currentSize());
        return pop;
    }
    public int incomingTransports() {
        return galaxy().friendlyPopApproachingSystem(starSystem());
    }
    public float populationPct() {
        return (population() / planet.currentSize());
    }
    public float expectedPopPct() {
        return (expectedPopulationLongTerm() / planet.currentSize());
    }
    public int calcPopNeeded(float desiredPct) {
        return (int) ((planet.currentSize() * desiredPct) - expectedPopulationLongTerm());
    }
    public int calcPopToGive(float retainPct) {
        if (!canTransport())
            return 0;
        int p1 = maxTransportsAllowed();
        int p2 = (int) (population() - (retainPct * planet().currentSize()));
        return min(p1,p2);
    }
    public float newWaste() {
        float mod = empire().isPlayer() ? 1.0f : options().aiWasteModifier();
        return max(0, usedFactories() * tech().factoryWasteMod() * mod);
    }
    public float wasteCleanupCost() {
        if (empire.ignoresPlanetEnvironment())
            return 0;
        
        return (min(planet.maxWaste(), planet.waste()) + newWaste()) / tech().wasteElimination();
    }
    public float minimumCleanupCost() {
        return min(wasteCleanupCost(), totalIncome());
    }
    public void ensureMinimumCleanup() {
        float pct = wasteCleanupCost()/totalIncome();
        if (ecology().pct() < pct)
            forcePct(ECOLOGY, pct);
    }
    // BR: ultimate max size, used by system panel.
    // may probably be used instead of maxSize in some other case... 
    public float ultimateMaxSize() { // After Enrichment
        float terraformAdj  = tech().terraformAdj();
        float potentialBaseSize = planet.potentialSize(tech());
        
        if (planet.isEnvironmentHostile() && tech().topAtmoEnrichmentTech() == null)
       		terraformAdj *= options().hostileTerraformingPct();
        return max(planet.currentSize(), potentialBaseSize+terraformAdj);
    }
    public float maxSize() { // After terraform
    	float terraformAdj = tech().terraformAdj();
        if (planet.isEnvironmentHostile())
            terraformAdj *= options().hostileTerraformingPct();
        return max(planet.currentSize(), planet.baseSize()+terraformAdj);
    }
    public float maxUseableFactories() {
        return workingPopulation() * empire().maxRobotControls();
    }
    public float normalPopGrowth() {
        return planet.normalPopGrowth(workingPopulation());
    }
    public float normalPopGrowthAfterNextTurnTransports() {
    	// BR: Do not include incoming troop on normal growth factor.
        // return planet.normalPopGrowth(populationAfterNextTurnTransports());
        return planet.normalPopGrowth(workingPopulation());
    }
    public ShipFleet homeFleet() {
        return starSystem().orbitingFleetForEmpire(empire());
    }
    public float defenderCombatAdj() {
        return tech().troopCombatAdj(true);
    }
    public Transport transport() {
        if (transport == null)
            transport = new Transport(starSystem());
        return transport;
    }
    public StarSystem transportDestination() {
        if ((transport == null) || !transport.isActive())
            return null;
        return transport.destination();
    }
    public boolean transporting() {
        return (transport().isActive());
    }
    public boolean canTransport() {
        return (!inRebellion() && !transporting());
    }
    public float inTransport() {
        return transporting() ? transport().size() : 0;
    }
    public float transportCost() {
        return inTransport();
    }
    public void clearTransport() {
        starSystem().clearTransportSprite();
        StarSystem oldDest = transport().destination();
        transport().reset(empire);
        // recalculate destination and this colony if applicable
        if (oldDest != null) {
            oldDest.colony().governIfNeeded();
        }
        if (empire.isPlayerControlled() && transportAutoEco()) {
        	smoothMaxSlider(ECOLOGY);
        	redistributeReducedEcoSpending();
        }
        
        governIfNeeded();
    }
    public int maxTransportsAllowed() {
        if (quarantined())
            return 0;
        else
            return (int) (population() / 2);
    }
    public void launchTransports() {
        if (transport().isActive()) {
            transport().launch();
            if (transport().size() >= (int)population()) {
                abandon();
                return;
            }
            setPopulation(population() - transport().size());
            transport = new Transport(starSystem());
            if (empire.isPlayerControlled())
                starSystem().transportSprite().launch();
        }
    }
    public void scheduleTransportsToSystem(StarSystem dest, int pop, float travelTime) {
        scheduleTransportsToSystem(dest, pop);
        if (dest != starSystem()) {
            float dist = starSystem().distanceTo(dest);
            transport().travelSpeed(dist/travelTime);
        }
    }
    public void scheduleTransportsToSystem(StarSystem dest, int pop) {

    	if (dest == null) {
    		// BR: Should not happen! but it happens!
    		// little fix while searching for the initial bug to be fixed! 
    		 clearTransport();
    		 return;
    	}
    		
        // adjust pop to max allowed... But still send original pop to allow abandon!
        int xPop = min(pop, maxTransportsAllowed());
        log("Scheduling " + xPop + " transports from: " + starSystem().name() + "  to: " + dest.name());

        // if zero or to this system, then clear
        if ((dest == starSystem()) || (xPop == 0))
            clearTransport();
        else {
            StarSystem oldDest = transport().destination();
            transport().size(pop);
            transport().setDest(dest);
            transport().setDefaultTravelSpeed();
            if (oldDest != null && oldDest != dest) {
                oldDest.colony().governIfNeeded();
            }
        }
        empire().flagColoniesToRecalcSpending();
        checkEcoAtClean();
        // reset ship views
        if (empire.isPlayerControlled()) {
        	empire.setVisibleShips();
        	if (transportAutoEco())
        		smoothMaxSlider(ECOLOGY);
        }
        // recalculate governor if transports are sent
        governIfNeeded();

        // recalculate destination colony
        dest.colony().governIfNeeded();
    }
    public float fleetDamagePerRoundToArrivingTransports(int empId) {
        float defenderDmg = 0;
        List<ShipFleet> fleets = starSystem().orbitingFleets();
        // add firepower for each allied ship in orbit
            // modnar: use firepowerAntiShip to only count ship weapons that can hit ships
            // to prevent ground bombs from being able to damage transports
        for (ShipFleet fl : fleets) {
            if (fl.empire().aggressiveWith(empId))
                defenderDmg += fl.firepowerAntiShip(0);
        }
        return defenderDmg;
    }
    public void acceptTransport(Transport t) {
        if (!t.empire().canColonize(starSystem())) {
            // no appropriate alert message for this transport loss. This is an edge case anyway
            // as it occurs only when the destination system has been rendered inhabitable by a
            // random event while the transport was in transits
            t.size(0);
            return;
        }
        // Xilmi: when landing on our own planet we also can be shot down by orbiting enemies
        float defenderDmg = fleetDamagePerRoundToArrivingTransports(t.empId());
        int passed = 0;
        int lost = 0;
        int num = t.size();
        // run the gauntlet
        for (int j = 0; j < t.gauntletRounds(); j++)
            lost += (int) (defenderDmg / t.hitPoints());
        passed += max(0, (num - lost));
        lost = min(lost, num);
        t.size(passed);
        if (lost > 0) {
            log(concat(str(t.launchSize()), " ", t.empire().raceName(), " transports perished at ", name()));
            if (t.empire().isPlayerControlled()) 
                TransportsKilledAlert.create(empire(), starSystem(), lost);
            else if (empire().isPlayerControlled()) 
                InvadersKilledAlert.create(t.empire(), starSystem(), lost);
            if(t.size() == 0)
                return;
        }
        setPopulation(min(planet.currentSize(), (population() + t.size())));
        log("Accepting ", str(t.size()), " transports at: ", starSystem().name(), ". New pop:", fmt(population(), 2));
        t.size(0);
    }
    public float maxTransportsToReceive() {
        return planet.currentSize() - workingPopulation();
    }
    public void resistTransportWithRebels(Transport tr) {
        log(str(rebels), " ", empire().raceName(), " rebels at ", starSystem().name(), " resisting ",
                    str(tr.size()), " ", tr.empire().raceName(), " transports");

        // Xilmi: when landing on our own planet we also can be shot down by orbiting enemies
        float defenderDmg = fleetDamagePerRoundToArrivingTransports(tr.empId());
        int passed = 0;
        int lost = 0;
        int num = tr.size();
        // run the gauntlet
        for (int j = 0; j < tr.gauntletRounds(); j++)
            lost += (int) (defenderDmg / tr.hitPoints());
        passed += max(0, (num - lost));
        lost = min(lost, num);
        tr.size(passed);
        if (lost > 0) {
            log(concat(str(tr.launchSize()), " ", tr.empire().raceName(), " transports perished at ", name()));
            if (tr.empire().isPlayerControlled()) 
                TransportsKilledAlert.create(empire(), starSystem(), lost);
            else if (empire().isPlayerControlled()) 
                InvadersKilledAlert.create(tr.empire(), starSystem(), lost);
            if(tr.size() == 0)
                return;
        }
        
        captives = population() - rebels;
        setPopulation(rebels);

        if (population() > 0) {
            if (empire.isPlayerControlled() || tr.empire().isPlayerControlled())
                RotPUI.instance().selectGroundBattlePanel(this, tr);
            else
                completeDefenseAgainstTransports(tr);
        }

        rebels = (int) population();
        setPopulation(rebels + captives);
        captives = 0;

        // are there rebels left?
        if (rebels > 0)
            return;

        empire.setRecalcDistances();
        rebellion = false;
        setPopulation(max(1, tr.size() + population));
        tr.size(0);
    }
    public void resistTransport(Transport tr) {
        log(empire().raceName() + " colony at " + starSystem().name() + " resisting " + tr.size() + " "
                        + tr.empire().raceName() + " transports");

        if (!tr.empire().canColonize(starSystem())) {
            if (tr.empire().isPlayerControlled())
                TransportsKilledAlert.create(empire(), starSystem(), tr.launchSize());
            else if (empire().isPlayerControlled())
                InvadersKilledAlert.create(tr.empire(), starSystem(), tr.launchSize());
            tr.size(0);
            return;
        }

        int passed = 0;
        int num = tr.size();
        float pct = tr.combatTransportPct();

        EmpireView ev = tr.empire().viewForEmpire(empire);

        if (ev != null) {
            if (ev.embassy().unity())
                return;
            // don't cause war if treaty signed since launch
            if (!ev.embassy().anyWar() && (ev.embassy().treatyDate() >= tr.launchTime()))
                return;
            // don't cause war if planet now occupied by another race
            if (!ev.embassy().anyWar() && (empire != tr.targetCiv()))
                return;
            if (!ev.embassy().anyWar())
                ev.embassy().declareWar();
        }

        if (tech().subspaceInterdiction())
            pct /= 2;

        // check for automatic passing if combat transporters
        if (pct > 0) {
            for (int i = 0; i < num; i++) {
                if (random() <= pct)
                    passed++;
            }
        }

        num -= passed;

        // choose most effective missile dmg
        int missileDmg = 0;
        MissileBase base = defense().missileBase();
        TechMissileWeapon scatter = base.scatterPack() == null ? null : base.scatterPack().tech();
        TechMissileWeapon missile = defense().missileBase().missile().tech();
        if (scatter != null)
            missileDmg = 3*max(missile.damage(), scatter.damage() * scatter.scatterAttacks());
        else 
            missileDmg = 3*missile.damage();

        int lost = 0;

        // start with base missile damage
        float defenderDmg = defense().missileBases() * missileDmg;

        // add firepower for each allied ship in orbit
            // modnar: use firepowerAntiShip to only count ship weapons that can hit ships
            // to prevent ground bombs from being able to damage transports
	defenderDmg += fleetDamagePerRoundToArrivingTransports(tr.empId());

        // run the gauntlet
        for (int j = 0; j < tr.gauntletRounds(); j++)
            lost += (int) (defenderDmg / tr.hitPoints());

        passed += max(0, (num - lost));
        lost = min(lost, num);

        tr.size(passed);

        // if gauntlet not passed, stop and inform player (if player)
        // neither of these incidents are added to the embassies. They are for
        // player notification only.
        if (lost > 0) {
            log(concat(str(tr.launchSize()), " ", tr.empire().raceName(), " transports perished at ", name()));
            if (tr.empire().isPlayerControlled()) 
                TransportsKilledAlert.create(empire(), starSystem(), lost);
            else if (empire().isPlayerControlled()) 
                InvadersKilledAlert.create(tr.empire(), starSystem(), lost);
            if(tr.size() == 0)
                return;
        }

        float startingPop = population();
        if (population() > 0) {
            if (empire.isPlayerControlled() || tr.empire().isPlayerControlled())
                RotPUI.instance().selectGroundBattlePanel(this, tr);
            else
                completeDefenseAgainstTransports(tr);
        }

        float pctLost = min(1, ((startingPop - population()) / startingPop));
        int popLost = (int) startingPop -  (int) population();
        int rebelsLost = (int) Math.ceil(pctLost*rebels);
        rebels = rebels - rebelsLost;
        
        DiplomaticTreaty treaty = empire().treaty(tr.empire());
        if (treaty != null) {
            treaty.losePopulation(empire(), startingPop-population());
            treaty.losePopulation(tr.empire(), tr.launchSize()-tr.size());
        }

        // did planet ownership change?
        if (tr.size() > 0) {
            ColonyCapturedIncident.create(tr.empire(), empire(), starSystem(), popLost);
            capturedByTransport(tr);
        } else
            ColonyInvadedIncident.create(tr.empire(), empire(), starSystem(), popLost);
    }
    public void completeDefenseAgainstTransports(Transport tr) {
        while ((tr.size() > 0) && (defense().troops() > 0))
            singleCombatAgainstTransports(tr);
    }
    public boolean singleCombatAgainstTransports(Transport tr) {
        float attRoll = random(100) + tr.combatAdj();
        float defRoll = random(100) + defenderCombatAdj();

        if (attRoll < defRoll)
            tr.size(tr.size() - 1);
        else
            setPopulation(population() - 1);

        if (population() <= 0)
            setPopulation(0);

        // true: attacker defeated
        // false: defender defeated
        return attRoll < defRoll;
    }
    private void capturedByTransport(Transport tr) {
        Empire loser = empire();
        if (isCapital())
            loser.chooseNewCapital();
        
        loser.lastAttacker(tr.empire());
        starSystem().addEvent(new SystemCapturedEvent(tr.empId()));
        tr.empire().lastAttacker(loser);

        Empire pl = player();
        if (tr.empire().isPlayerControlled()) {
            allocation(SHIP, 0);
            allocation(DEFENSE,0);
            allocation(INDUSTRY,0);
            allocation(ECOLOGY,0);
            allocation(RESEARCH,0);
            String str1 = text("MAIN_ALLOCATE_COLONY_CAPTURED", pl.sv.name(starSystem().id), pl.raceName());
            str1 = pl.replaceTokens(str1, "spy");
            session().addSystemToAllocate(starSystem(), str1);
        }
        // list of possible techs that could be recovered from factories
        List<Tech> possibleTechs = empire().tech().techsUnknownTo(tr.empire(), true);
        int techsCaptured = 0;
        // each factory is 2% chance to plunder an unknown tech
        for (int i = 0; i < (int) industry().factories(); i++) {
            if (techsCaptured >= MAX_TECHS_CAPTURED)
                break;
            if (!possibleTechs.isEmpty() && (random() <= TECH_PLUNDER_PCT)) {
                Tech t = random(possibleTechs);
                possibleTechs.remove(t);
                tr.empire().plunderTech(t, starSystem(), empire());
                techsCaptured++;
            }
        }

        setPopulation(min(planet.currentSize(),tr.size()));
        tr.size(0);
        shipyard().capturedBy(tr.empire());
        industry().capturedBy(tr.empire());
        defense().capturedBy(tr.empire());
        ecology().capturedBy(tr.empire());
        research().capturedBy(tr.empire());

        StarSystem sys = starSystem();
        empire.removeColonizedSystem(sys);
        empire.stopRalliesWithSystem(sys);
        tr.empire().addColonizedSystem(sys);

        empire = tr.empire();
        defense().maxBases(empire.defaultMaxBases());
        buildFortress();
        shipyard().goToNextDesign();

        rebels = 0;
        rebellion = false;
        clearReserveIncome();
        clearTransport();
        loser.sv.refreshFullScan(sys.id);
        empire.sv.refreshFullScan(sys.id);
        
        // empires of orbiting fleets should see ownership change
        List<ShipFleet> fleets = sys.orbitingFleets();
        for (ShipFleet fl: fleets) {
            Empire flEmp = fl.empire();
            if ((flEmp != loser) && (flEmp != empire))
                flEmp.sv.refreshFullScan(sys.id);
        }

        // if system was captured, clear shipbuilding, we don't want systems just captured building ships
        // Do that if governor is on by default, otherwise stick to default behaviour
        if (tr.empire().isPlayerControlled()) {
        	// BR: Set the new governor
        	setGovernor(govOptions().isGovernorOnByDefault());
            if (isGovernor()) {
	            //System.out.println("System captured "+name()+", clearing shipbuilding");
	            locked(SHIP, false);
	            locked(INDUSTRY, false);
	            setAllocation(SHIP, 0);
	        }
        }

        if (loser.numColonies() == 0)
            loser.goExtinct();
    }
    public void takeCollateralDamage(float damage) {
        if (destroyed())
            return;

        if (defense().bases() < 1)
            takeUntargetedCollateralDamage(damage);
        else
            takeTargetedCollateralDamage(damage);
    }
    public void takeTargetedCollateralDamage(float damage) {
        float newPop = max(0, population() - (damage / TARGETED_DAMAGE_FOR_POPLOSS));
        float newFact = max(0, industry().factories() - (damage / TARGETED_DAMAGE_FOR_FACTLOSS));

        setPopulation(newPop);
        industry().factories(newFact);

        if (population() <= 0)
            destroy();
    }
    public void takeUntargetedCollateralDamage(float damage) {
        float newPop = max(0, population() - (damage / UNTARGETED_DAMAGE_FOR_POPLOSS));
        float newFact = max(0, industry().factories() - (damage / UNTARGETED_DAMAGE_FOR_FACTLOSS));

        setPopulation(newPop);
        industry().factories(newFact);

        if (population() <= 0)
            destroy();
    }
    public void takeBioweaponDamage(float damage) {
        float popLost = damage;

        setPopulation(max(0, population() - popLost));

        float newWaste = popLost * 10;
        ecology().addWaste(newWaste);
        planet().removeExcessWaste();

        if (population() <= 0)
            destroy();
    }
    public void abandon() {
        if (isCapital())
            empire.chooseNewCapital();
        
        StarSystem sys = starSystem();
        sys.addEvent(new SystemAbandonedEvent(empire.id));
        sys.abandoned(true);

        setPopulation(0);
        rebels = 0;
        captives = 0;
        rebellion = false;
        planet.addAlienFactories(empire.id, (int) industry().factories());

        transport = null;
        clearReserveIncome();
        empire.removeColonizedSystem(sys);
        empire.stopRalliesWithSystem(sys);
        planet.setColony(null);
        // update system views of civs that would notice
        empire.sv.refreshFullScan(sys.id);
        List<ShipFleet> fleets = sys.orbitingFleets();
        for (ShipFleet fl : fleets) 
            fl.empire().sv.refreshFullScan(sys.id);
        
        for (Empire emp: galaxy().empires()) {
            if (emp.knowsOf(empire) && !emp.sv.name(sys.id).isEmpty()) 
                emp.sv.view(sys.id).setEmpire();                   
        }
    }
    public void destroy() {
        if (isCapital())
            empire.chooseNewCapital();
        
        StarSystem sys = starSystem();
        sys.addEvent(new SystemDestroyedEvent(empire.lastAttacker()));

        setPopulation(0);
        rebels = 0;
        captives = 0;
        rebellion = false;
        planet.addAlienFactories(empire.id, (int) industry().factories());

        transport = null;
        clearReserveIncome();
        empire.removeColonizedSystem(sys);
        empire.stopRalliesWithSystem(sys);
        planet.setColony(null);
        // update system views of civs that would notice
        empire.sv.refreshFullScan(sys.id);
        List<ShipFleet> fleets = sys.orbitingFleets();
        for (ShipFleet fl : fleets) 
            fl.empire().sv.refreshFullScan(sys.id);
        
        for (Empire emp: galaxy().empires()) {
            if (emp.knowsOf(empire) && !emp.sv.name(sys.id).isEmpty()) 
                emp.sv.view(sys.id).setEmpire();                   
        }
    }

    private boolean governor = govOptions().isGovernorOnByDefault();
//  TODO: For future use, flag allowing this colony to autobuild ships
    private boolean autoShips = govOptions().isAutoShipsByDefault();

    public boolean isGovernor() { return governor; }
    public void setDefaultGovernor() { setGovernor(govOptions().isGovernorOnByDefault()); }
    public void setGovernor(boolean governor) {
        this.governor = governor;
        //removing locks after disabling governor:
        if(!isGovernor())
        {
            for (int i = 0; i <= 4; i++) {
                locked(i, false);
            }
        }
    }

    public boolean isAutoShips() { return autoShips; }
    public void setAutoShips(boolean autoShips) {
        this.autoShips = autoShips;
    }

    /**
     * Increment slider. Stop moving when results no longer contains "stopWhenDisappears".
     * Stop when results contain "stopWhenAppears".
     * If moving slider doesn't change production, stop as well.
    private void moveSlider(int category, String stopWhenDisappears, String stopWhenAppears) {
        ColonySpendingCategory cat = category(category);
        int previousAllocaton = -1;
        for (;;) {
            String result = cat.upcomingResult();
            if (stopWhenDisappears != null && !result.contains(stopWhenDisappears)) {
                break;
            }
            if (stopWhenAppears != null) {
                if (result.contains(stopWhenAppears)) {
                    break;
                }
            }
            increment(category, 1);
            if (previousAllocaton == cat.allocation()) {
                break;
            }
            previousAllocaton = cat.allocation();
        }
    }
     */

    public void governIfNeeded() {
        if (!this.isAutopilot() && this.isGovernor()) {
            govern();
        }
    }
    /**
     * Govern the colony.
     * - First, send auto transports if enabled
     * - Then balance ecology and industry spending for maximum production
     * - Then set defense to maximum.
     * - Then build a stargate if applicable
     *
     * This is quite crude- works by moving slider by 1 tick until desired results happen.
     * Better way would be to calculate and set each slider directly to the right percentage.
     *
     */
    public void govern() {
        // don't govern if it hasn't been fully initialized
        // I added this due to adding governor logic in clearTransports which is called
        // during the initialization process
        for (int i = 0; i < spending.length; i++)
            if (spending[i] == null || spending[i].colony() == null)
                return;
        // Set max missile bases if minimum is set
        if (session().getGovernorOptions().getMinimumMissileBases() > 0) {
            if (defense().maxBases() < session().getGovernorOptions().getMinimumMissileBases()) {
                defense().maxBases(session().getGovernorOptions().getMinimumMissileBases());
            }
        }
        // unlock all sliders
        for (int i = 0; i <= 4; i++) {
            locked(i, false);
        }
        // remember if the planet was building a stargate (might have been manually started by the player)
        boolean buildingStargate = allocation[SHIP] > 0 &&
                shipyard().design().equals(empire.shipLab().stargateDesign()) &&
                !shipyard().stargateCompleted();
        // remember if this planet was building ships. Stargate doesn't count
        // if we just finished building a stargate, we're not building ships
        boolean buildingShips = (allocation[SHIP] > 0 || shipyard().buildLimit() > 0) &&
                !shipyard().design().equals(empire.shipLab().stargateDesign()) &&
                !buildingStargate;

        int shipAllocNeeded = shipyard().maxAllocationNeeded();        
        // start from scratch
        clearSpending();
        /**
         * 2022-02-20 Obsolete, back to old transport logic. 10bc transport cost was never implemented by Ray.
         *
         * I took this out with the new autotransport logic.  If you want to slow down autotransports,
         * then change it back to something like this that will leave 2 population to grow naturally.
         * Leaving it at always balanceEcoAndInd(1) will enable rapid growth and much more spending on
         * population growth.
        */
//        balanceEcoAndInd(1);

//        if (session().getGovernorOptions().isAutotransport())
//            balanceEcoAndInd(1 - Math.max(normalPopGrowth(), 3) / maxSize(), buildingShips, true);
//        else
//            balanceEcoAndInd(1, buildingShips, true);

        // Leave some room for normal population growth if we're auto transporting
        if (session().getGovernorOptions().isAutotransport())
            balanceEcoAndInd(1 - Math.max(normalPopGrowth(), 3) / maxSize(), buildingShips, false);
        else
            balanceEcoAndInd(1, buildingShips, false);
        // unlock all sliders except for ECO. Thanks DM666a
        for (int i = 0; i <= 4; i++) {
            locked(i, false);
        }
        // add maximum defense
        // don't allocate just for "upgrades" if there are no bases or if there are more bases than we want
        if (!defense().isCompleted() && (defense().maxBases() > 0 && defense().maxBases() >= defense().bases()) || session().getGovernorOptions().getShieldWithoutBases()) {
            int allocationAvailableForDefense = allocation(RESEARCH) + allocationRemaining();
            if(allocation(SHIP) > 1)
                allocationAvailableForDefense += allocation(SHIP) - 1;
            increment(DEFENSE, min(defense().maxAllocationNeeded(), allocationAvailableForDefense));
            locked(DEFENSE, true);
        }

        // Build gate if tech is available. Also add a system property to turn it off.
        // Don't build gate if shipbuilding on governor is enabled, and planet is already building ships
        if (!buildingShips || !session().getGovernorOptions().isShipbuilding()) {
            buildStargate(buildingStargate);
        }

        // put rest into research.
        allocation(RESEARCH, 0); // BR: Clear the research before computing the remaining available
        if(allocationRemaining() > 0)
            allocation(RESEARCH, allocationRemaining());

        // if we were building ships, or a stargate, keep 1 tick in shipbuilding
        if ((buildingShips && session().getGovernorOptions().isShipbuilding()) ||
            (buildingStargate && session().getGovernorOptions().getGates() != GovernorOptions.GatesGovernor.None)) {
            if(allocation(SHIP) < 1 && shipAllocNeeded < 1) //only do it if we aren't already spending into ship as we otherwise could get waste
                increment(SHIP, 1);
        }
        if ((buildingStargate || buildingShips)
                && session().getGovernorOptions().isShipbuilding() && allocation[RESEARCH] > 0) {
            // if we were building ships, push all research into shipbuilding.
            locked(Colony.SHIP, false);
            int allocForShips = allocation(RESEARCH);
            if(shipAllocNeeded > 0)
                allocForShips = min(allocForShips, shipAllocNeeded);
            increment(Colony.SHIP, allocForShips);
        }
        locked(Colony.ECOLOGY, true);
        /*System.out.println(galaxy().currentTurn()+" "+empire.name()+" "+name()+" After Govern:");
        System.out.println(galaxy().currentTurn()+" "+empire.name()+" Ship: "+allocation(SHIP));
        System.out.println(galaxy().currentTurn()+" "+empire.name()+" Def : "+allocation(DEFENSE));
        System.out.println(galaxy().currentTurn()+" "+empire.name()+" Ind : "+allocation(INDUSTRY));
        System.out.println(galaxy().currentTurn()+" "+empire.name()+" Eco : "+allocation(ECOLOGY));
        System.out.println(galaxy().currentTurn()+" "+empire.name()+" Res : "+allocation(RESEARCH));*/
    }

    /**
     * Balances ECO and IND spending for maximum production next turn.
     * - targetPopPercent is between 0 and 1 for target population size by spending on ECO.
     *   So, you can say 0.99 to leave a little population for natural growth, or use 0.5 to
     *   maximize growth rate, 1 to always grow to max size, or 0 to never spend on pop growth.
     * - It will also not spend to grow larger than max size less all incoming transports.
     *   So if you have 10 transports arriving in 5 turns, it won't spend ECO to grow the last 10,
     *   but it will spend IND to build factories for them.
     *
     * - first allocate minimum ECO spend to prevent waste
     * - then prioritize IND for refitting / converting factories
     * - then prioritize IND up to maximum controlled by next turn's population
     * - then spend ECO to improve environment (atmosphere, soil, terraforming)
     * - then spend ECO to grow population enough to control existing factories
     * - then spend ECO/IND proportionally to build factories at the rate the new pop can control
     * - then build any remaining factories that can be built (if target population percent is not 1)
     * - then make sure we don't allocate more than needed for either ECO or IND (can happen due to
     *   rounding or situations like terraforming where the factories for the new population can't be
     *   built until the next turn).  If there is extra, assign it to the other category if it can be used
     * - finally make sure that we aren't over MAX_TICKS for both ECO and IND (can happen due to rounding).
     *   If we are, prioritize IND making sure to keep ECO at least at minimum spend to prevent waste
     */
    public void balanceEcoAndInd(float targetPopPercent, boolean buildingShip, boolean test) {
    	GovernorOptions gov = govOptions();
    	ColonyEcology   eco = ecology();
    	float   earlyFactor     = gov.terraformEarly()/100f;
    	boolean terraformEarly  = earlyFactor>0;
    	float maxSize = terraformEarly? ultimateMaxSize() : maxSize();
    	float currentSize = planet.currentSize();
        targetPopPercent = Math.min(Math.max(targetPopPercent, 0), 1);
        // new pop next turn before spending
        float baseNewPop = Math.min(currentSize, workingPopulation() + normalPopGrowth() + incomingTransportsNextTurn());
        // transports coming after next turn; use to limit pop growth spending
        float additionalTransports = Math.max(incomingTransports() - incomingTransportsNextTurn(), 0);
        // population target for growth spending
        float popTarget = Math.min(maxSize * targetPopPercent, maxSize - additionalTransports);

        float totalBC = totalIncome();
        float cleanupCost = minimumCleanupCost();
        int minEcoAll = eco.cleanupAllocationNeeded();
        // ECO allocation to clean up + everything else
        // I compute it here instead of adding maxSpendingAllocation + cleanupAllocationNeeded because that could produce
        // a result too high by 1 due to rounding.
        int maxEcoAll = eco.maxAllocationNeeded();
        int maxIndAll = industry().maxAllocationNeeded();
        // Factor for industry spending based on planet adjustment (rich, poor, etc.).  Reserve
        // spending doesn't get adjusted, so we calculate an overall factor here.
        float indFactor = totalIncome() / (maxReserveIncome() + totalProductionIncome() * planet.productionAdj());
        float maxIndBC = industry().maxSpendingNeeded();
        // check if it's adjusted or not, then fix if not
        if (Math.ceil(Math.min(1, maxIndBC / totalBC) * MAX_TICKS) != maxIndAll)
            maxIndBC *= indFactor;
        float remainingBC = Math.max(totalBC - cleanupCost, 0);  // BC remaining to allocate
        float popCost = tech().populationCost();
        float factoryCost = industry().newFactoryCost() * indFactor;
        float robotControls = industry().effectiveRobotControls();
        if(empire().ignoresFactoryRefit())
            robotControls = industry().maxRobotControls();
        float factories = industry().factories();
        float canBeUsed = baseNewPop * robotControls;
        // limit growth based on target population
        float maxGrowth = Math.max(popTarget - baseNewPop, 0);
        float ecoBC = cleanupCost;
        float indBC = 0;
        int ecoAll = 0;
        int indAll = 0;
        boolean refit = industry().effectiveRobotControls() < empire().maxRobotControls() && !empire.ignoresFactoryRefit();

        float[] planetBoostCost = eco.planetBoostCost();
        float   totalBoostCost  = planetBoostCost[4];
        boolean needTerraform   = totalBoostCost > 0;

        /*
        System.out.println("balance "+this.name()+" popTarget "+popTarget);
        System.out.println("balance "+this.name()+" baseNewPop "+baseNewPop);
        System.out.println("balance "+this.name()+" totalBC "+totalBC);
        System.out.println("balance "+this.name()+" cleanupCost "+cleanupCost);
        System.out.println("balance "+this.name()+" minEcoAll "+minEcoAll);
        System.out.println("balance "+this.name()+" maxEcoAll "+maxEcoAll);
        System.out.println("balance "+this.name()+" maxIndAll "+maxIndAll);
        System.out.println("balance "+this.name()+" maxIndBC "+maxIndBC);
        System.out.println("balance "+this.name()+" indFactor "+indFactor);
        System.out.println("balance "+this.name()+" popCost "+popCost);
        System.out.println("balance "+this.name()+" factoryCost "+factoryCost);
        System.out.println("balance "+this.name()+" robotControls "+robotControls);
        System.out.println("balance "+this.name()+" factories "+factories);
        System.out.println("balance "+this.name()+" canBeUsed "+canBeUsed);
        System.out.println("balance "+this.name()+" maxGrowth "+maxGrowth);
        System.out.println("balance "+this.name()+" remainingBC "+remainingBC);
        System.out.println("balance "+this.name()+" maxControls "+empire().maxRobotControls());
        System.out.println("balance "+this.name()+" industry complete "+industry().isCompleted());
        System.out.println("balance "+this.name()+" refit "+refit);
        System.out.println("balance "+this.name()+" needTerraform "+needTerraform);
        System.out.println("balance "+this.name()+" totalBoostCost "+totalBoostCost);
        System.out.println("balance "+this.name()+" planetBoostCost "+planetBoostCost);
        */

        // If there's alien factories or refitting, allocate ECO to clean and then the rest (up to max needed) to industry.
        // Any leftovers go to ECO up to max.
        // To properly balance this would require changes to ColonyIndustry to expose some internal numbers.

        boolean boostedAction = false;
        if ( !industry().isCompleted() && refit && !needTerraform
        		&& baseNewPop >= factories / empire().maxRobotControls() ) {
            //System.out.println("balance "+this.name()+" has alien factories or refit");
            indAll = Math.min(MAX_TICKS - ecoAll, industry().maxAllocationNeeded());
            indBC = indAll * totalBC / MAX_TICKS;
            remainingBC = Math.max(remainingBC - indBC, 0);
            // max out pop growth if there is some leftover production
            ecoBC += Math.min(eco.maxSpendingNeeded(), remainingBC);
            ecoAll = Math.max((int) Math.ceil(ecoBC / totalBC * MAX_TICKS), minEcoAll);
        } else {
            // For all other situations (no refit; no alien factories), max out IND first to usefulness, then
            // balance ECO and IND spend to get max production next turn

        	// Check for planet boost priority
        	boostedAction = needTerraform && terraformEarly;
        	if (boostedAction && !test) {
        		// Do instant Buy
        		float originalBC = remainingBC;
        		int   mistId = 0;
        		for (int i=0; i<4; i++) {
        			float cost = planetBoostCost[i];
        			if (cost <= remainingBC) {
        				ecoBC += cost;
        				remainingBC -= cost;
        				planetBoostCost[i] = 0;
        				totalBoostCost -= cost;
        			}
        			else {
        				mistId = i;
        				break;
        			}
        		}
         		needTerraform = totalBoostCost > 0;

         		// Test for partial buy
        		if (needTerraform) {
					float buyFactor = earlyFactor; // TODO BR: Tune Governor terraformEarly factors
					float limSize	= 20.0f;
					if(originalBC == remainingBC) { // There was no instant buy
        				switch (mistId) {
	        				case 0: // Hostile, very good improvement value -> max buy
	        					// buyFactor *= 1.0f;
	        					break;
	        				case 1: // Normal, good improvement value
	        				case 2: // fertile, good improvement value
	        					if (currentSize > limSize)
	        						buyFactor *= bounds(0.5f, baseNewPop/(planet.currentSize()-limSize), 1.0f);
	        					break;
	        				case 3: // Terraform, keep some bc for normal process
	        					if (currentSize > limSize)
	        						buyFactor *= bounds(0.25f, baseNewPop/(planet.currentSize()-20.0f), 1.0f);
        				}
        			}
        			else { // already bought something
        				if (currentSize > limSize)
        					buyFactor = max(0.25f, baseNewPop/currentSize);
        			}
					float partialBuy = remainingBC * bounds(0, buyFactor, 1.0f);
    				ecoBC += partialBuy;
    				remainingBC -= partialBuy; // Do not take all
    				planetBoostCost[mistId] -= partialBuy;
    				totalBoostCost -= partialBuy;
                    //System.out.println("balance "+name()+" buyFactor "	+buyFactor);
        		}
                //System.out.println("balance "+name()+" originalBC "	+originalBC);
                //System.out.println("balance "+name()+" ecoOverClean " +(ecoBC - cleanupCost));        	
        	}

            // Build factories to max out population use of factories
            indBC = Math.min(Math.max(canBeUsed - factories, 0) * factoryCost, remainingBC);
            remainingBC = Math.max(remainingBC - indBC, 0);
            //System.out.println("balance "+this.name()+" newFactories "+newFactories);
            //System.out.println("balance "+this.name()+" indBC "+indBC);
            //System.out.println("balance "+this.name()+" remainingBC "+remainingBC);

            // Check for terraforming / atmosphere / soil enrichment
            // Since 2.05 or so eco.terraformSpendingNeeded() includes cleanup cost
            float terraformBC = Math.min(totalBoostCost, remainingBC);
            ecoBC += terraformBC;
            remainingBC = Math.max(remainingBC - terraformBC, 0);
            //System.out.println("balance "+this.name()+" terraformBC "+terraformBC);
            //System.out.println("balance "+this.name()+" remainingBC "+remainingBC);

            // Grow population to use existing factories
            float popGrowth = Math.min(Math.max(factories - canBeUsed, 0) / robotControls, maxGrowth);
            float popBC = Math.min(popGrowth * popCost, remainingBC);
            ecoBC += popBC;
            remainingBC = Math.max(remainingBC - popBC, 0);
            //System.out.println("balance "+this.name()+" popGrowth "+popGrowth);
            //System.out.println("balance "+this.name()+" popBC "+popBC);
            //System.out.println("balance "+this.name()+" remainingBC "+remainingBC);

            // room left we can grow
            maxGrowth = Math.max(maxGrowth - popGrowth, 0);
            float workerROI = empire.tech().populationCost() / empire.workerProductivity();
            float popGrowthROI = Float.MAX_VALUE;
            if (normalPopGrowth() > 0)
                popGrowthROI = empire.tech().populationCost() / normalPopGrowth();
            maxGrowth = min(0, maxGrowth, industry().factories() / empire.maxRobotControls() - workingPopulation() - normalPopGrowth());
            if(popGrowthROI > workerROI 
            		|| gov.legacyGrowthMode() || terraformEarly
            		|| (!buildingShip && empire.tech().researchCompleted()))
                maxGrowth = maxSize - workingPopulation();
            
            maxGrowth -= additionalTransports;
            maxGrowth = max(0, maxGrowth);
            //System.out.println("balance "+this.name()+" maxGrowth "+maxGrowth);

            // Allocate remaining BC between ECO and IND to get max production benefit
            float factoryCostPerPop = factoryCost * robotControls;
            float totalCostPerPop = popCost + factoryCostPerPop;
            popGrowth = Math.min(remainingBC / totalCostPerPop, maxGrowth);
            indBC += popGrowth * factoryCostPerPop;
            ecoBC += popGrowth * popCost;
            remainingBC = Math.max(remainingBC - popGrowth * totalCostPerPop, 0);
            //System.out.println("balance "+this.name()+" popGrowth "+popGrowth);
            //System.out.println("balance "+this.name()+" added ind BC "+popGrowth * factoryCostPerPop);
            //System.out.println("balance "+this.name()+" added eco BC "+popGrowth * popCost);
            //System.out.println("balance "+this.name()+" remainingBC "+remainingBC);

            // If we're not growing some pop, still build out those factories for when natural growth/transports need them
            if (popTarget < maxSize && indBC < maxIndBC) {
                float extraIndBC = Math.min(maxIndBC - indBC, remainingBC);
                indBC += extraIndBC;
                //System.out.println("balance "+this.name()+" extraIndBC "+extraIndBC);
                //System.out.println("balance "+this.name()+" remainingBC "+remainingBC);
            }

            ecoAll = (int) Math.ceil(ecoBC / totalBC * MAX_TICKS);
            indAll = (int) Math.ceil(indBC / totalBC * MAX_TICKS);
        }
        //System.out.println("balance "+this.name()+" indAll before min/max checks "+indAll);
        //System.out.println("balance "+this.name()+" ecoAll before min/max checks "+ecoAll);

        // we over allocated industry (can be due to terraforming or industry reserve)
        if (indAll > maxIndAll) {
            // assign excess to ECO
            ecoAll = Math.min(ecoAll + indAll - maxIndAll, maxEcoAll);
            indAll = maxIndAll;
        }
        // we over allocated ecology, so reduce to max needed
        if (ecoAll > maxEcoAll) {
            // assign excess to IND
            indAll = Math.min(indAll + ecoAll - maxEcoAll, maxIndAll);
            ecoAll = maxEcoAll;
        }

        // adjust allocations to maximum total if we exceeded due to rounding
        if (indAll + ecoAll > MAX_TICKS) {
            // favor IND over ECO, but don't reduce ECO below minimum
            ecoAll = Math.max(MAX_TICKS - indAll, minEcoAll);
            indAll = Math.max(MAX_TICKS - ecoAll, 0);
        }
        //System.out.println("balance "+this.name()+" indAll final "+indAll+" maxIndAll "+maxIndAll);
        //System.out.println("balance "+this.name()+" ecoAll final "+ecoAll+" maxEcoAll "+maxEcoAll);
        //xilmi: We need to reset ecology-spending because totalIncome conditionally calls a function that sets eco to clean, if we don't we can end up having twice the eco-spending we want

/*        if (boostedAction) {
           // System.out.println("balance "+name()+" ->remainingBC "	+remainingBC);
           // System.out.println("balance "+name()+" ->ecoBC "		+ecoBC);        	
            System.out.println("balance "+name()+" ->ecoOverClean "	+(ecoBC - cleanupCost));        	
        }*/

        if (test)
        	return;
        	
        allocation(ECOLOGY, 0);
        locked(Colony.ECOLOGY, false);
        allocation(Colony.ECOLOGY, ecoAll);
        locked(Colony.ECOLOGY, true);
        allocation(Colony.INDUSTRY, indAll);
        locked(Colony.INDUSTRY, true);
    }
    public float unrestrictedPopGrowth() {
        // calculate growth rate based on current pop, environment & race
        float baseGrowthRate = max(0, (1 - (workingPopulation() / planet.currentSize())) / 10);
        baseGrowthRate *= empire.growthRateMod();
        if (!empire.ignoresPlanetEnvironment())
            baseGrowthRate *= planet.growthAdj();

        // always at least .1 base growth in pop
        float newGrownPopulation = max(.1f, workingPopulation() * baseGrowthRate);
        return newGrownPopulation;
    }
    private int upcomingPopGrowth(int toSend) {
    	float oldPop	 = population;
    	int oldTransport = transport().size();
    	int oldEco		 = allocation(ECOLOGY);
        allocation(ECOLOGY, 50);
        transport().size(0);
        population -= toSend;
        int expectedPopGrowth = ecology().upcomingPopGrowth();
        transport().size(oldTransport);
        population = oldPop;
        allocation(ECOLOGY, oldEco);
        return expectedPopGrowth;
    }
    private boolean willBeFilled(int toSend) {
    	float expectedPop = population - toSend + upcomingPopGrowth(toSend);
    	return planet.currentSize() <= expectedPop;
    }
    private boolean willNotDecline(int toSend) {
    	float expectedPop = population - toSend + upcomingPopGrowth(toSend);
    	return population <= expectedPop;
    }
    public int maxTransportToFill() {
    	int lim = maxTransportsAllowed();
    	int maxTransport = 0;
    	for (int i=0; i<=lim; i++) {
    		if (willBeFilled(i))
    			maxTransport = i;
    		else
    			return maxTransport;
    	}
    	return maxTransport;
    }
    public int maxTransportNoLoss() {
    	int lim = maxTransportsAllowed();
    	int maxTransport = 0;
    	for (int i=0; i<=lim; i++) {
    		if (willNotDecline(i))
    			maxTransport = i;
    		else
    			return maxTransport;
    	}
    	return maxTransport;
    }
    public boolean showTransports() {
        if (isPlayer(empire())) {
        	int friendPop = playerPopApproachingSystem();
            int enemyPop  = enemyPopApproachingPlayerSystem();
    		return (friendPop>0 || enemyPop>0);
    	}
        else {
        	int playerPop  = playerPopApproachingSystem();
        	return (playerPop>0);
        }
    }
    public int plannedTransport(int destId) {
    	StarSystem sys = starSystem();
    	if ( sys.transportSprite != null) {
    		StarSystem destSys = sys.transportSprite.clickedDest();
    	   	if ( destSys != null && destSys.id == destId) {
    	   		return sys.transportAmt;
    	   	}
    	   	destSys = sys.transportSprite.hoveringDest();
        	if ( destSys != null && destSys.id == destId) {
        		return 0;
    	   	}
    	}
    	if (transporting() && transport().destSysId() == destId)
    		return (int) inTransport();
    	return 0;
    }
    // Try to transport extra population to other plants.
    // Since 1.9 minimum cost to transport population is 10 BC which means
    // we have to transport in bunches of ~10 (configurable).
    // This introduces some restrictions:
    // We only transport when planet is fully built (eco, industry, etc).
    // (I won't wait for defenses)
    // We only transport limited distance.
    // Should we transport from hostile planets (?)
    // We chose targets more carefully.
    // Autotransport was Moved to Empire.autotransport()

    public int enemyPopApproachingPlayerSystem() {
        return galaxy().enemyPopApproachingPlayerSystem(starSystem());
    }
    public int playerPopApproachingSystem() {
        return galaxy().playerPopApproachingSystem(starSystem());
    }
    private int incomingTransportsNextTurn() {
        return galaxy().friendlyPopApproachingSystemNextTurn(starSystem());
    }

    private void buildStargate(final boolean wasPreviouslyBuildingStargate) {
        if (!this.shipyard().canBuildStargate()) {
            return;
        }
        if (session().getGovernorOptions().getGates() == GovernorOptions.GatesGovernor.None) {
            return;
        }
        // if the stargate build was already started (whether by governor or by the player),
        // then continue building it
        if(!wasPreviouslyBuildingStargate) {
            if (session().getGovernorOptions().getGates() == GovernorOptions.GatesGovernor.Rich) {
                if (!planet().isResourceRich() && !planet.isResourceUltraRich()) {
                    return;
                }
            }
        }
        // don't build gate if planet production is below 300
        // Not sure about this one, now that maintenance is taken from global pool
//        if (production() < 300) {
//            return;
//        }
        Design first = this.shipyard().design();
        Design current = this.shipyard().design();
        while (!this.empire.shipLab().stargateDesign().equals(current)) {
            this.shipyard().goToNextDesign();
            current = this.shipyard().design();
            if (current.equals(first)) {
                System.out.println("unable to cycle to Shargate design");
                break;
            }
        }
        if (this.empire.shipLab().stargateDesign().equals(current)) {
            locked(Colony.SHIP, false);
            int needed = shipyard().maxAllocationNeeded();
            needed = min(needed, allocationRemaining() + allocation(RESEARCH));
            allocation(SHIP, needed);
        }
    }

}
