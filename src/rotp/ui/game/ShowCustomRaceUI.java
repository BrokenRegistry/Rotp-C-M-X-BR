/*
 * Copyright 2015-2020 Ray Fowler
 * 
 * Licensed under the GNU General Public License, Version 3 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *	 https://www.gnu.org/licenses/gpl-3.0.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rotp.ui.game;

import static rotp.model.empires.CustomAbilitiesFactory.ROOT;
import static rotp.ui.UserPreferences.customPlayerRace;
import static rotp.ui.util.AbstractOptionsUI.exitButtonKey;
import static rotp.ui.util.AbstractOptionsUI.exitButtonWidth;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.util.LinkedList;

import rotp.model.empires.CustomRaceDefinitions;
import rotp.model.game.MOO1GameOptions;
import rotp.ui.BasePanel;
import rotp.ui.BaseText;
import rotp.ui.main.SystemPanel;
import rotp.ui.races.RacesUI;
import rotp.ui.util.AbstractOptionsUI;
import rotp.ui.util.InterfaceOptions;
import rotp.ui.util.Modifier2KeysState;
import rotp.ui.util.SettingBase;

// modnar: add UI panel for modnar MOD game options, based on StartOptionsUI.java
public abstract class ShowCustomRaceUI extends BasePanel implements MouseListener, MouseMotionListener {
	private static final long serialVersionUID	= 1L;
	private static final Color  backgroundHaze	= new Color(0,0,0,160);
	private static final String totalCostKey	= "CUSTOM_RACE_GUI_COST";
	private static final String initialRace	= MOO1GameOptions.baseRaceOptions().getFirst();
	
	protected static final Color textC		= SystemPanel.whiteText;
	protected		   final Font buttonFont	= narrowFont(20);
	protected static final int buttonH		= s30;
	protected static final int buttonMargin	= AbstractOptionsUI.smallButtonM;
	protected static final int buttonPad		= s15;
	protected static final int xButtonOffset	= s30;
	protected static final int yButtonOffset	= s40;
	protected static final Color labelC		= SystemPanel.orangeText;
	protected	static final int labelFontSize	= 14;
	protected static final int labelH			= s16;
	protected static final int labelPad		= s8;

	protected static final Color costC		= SystemPanel.blackText;
	protected static final int costFontSize	= 18;
	private		   final Font titleFont		= narrowFont(30);
	private static final int titleOffset	= s30; // Offset from Margin
	private static final int costOffset		= s25; // Offset from title
	private static final int titlePad		= s75;
	private static final int bottomPad		= s40;
	private static final int columnPad		= s20;
	
	private static final Color frameC		= SystemPanel.blackText; // Setting frame color
	private static final Color settingPosC	= SystemPanel.limeText; // Setting name color
	private static final Color settingNegC	= SystemPanel.redText; // Setting name color
	private static final Color settingC		= SystemPanel.whiteText; // Setting name color
	private static final int settingFont	= 16;
	private static final int settingH		= s16;
	private static final int spacerH		= s10;
	private static final int settingHPad	= s4;
	private static final int frameShift		= s5;
	private static final int frameTopPad	= 0;
	private static final int frameSizePad	= s10;
	private static final int frameEndPad	= s4;
	private static final int settingIndent	= s10;
	private static final int wSetting		= s100+s100+s20;

	private static final Color optionC		= SystemPanel.blackText; // Unselected option Color
	private static final Color selectC		= SystemPanel.whiteText;  // Selected option color
	private static final int optionFont		= 13;
	private static final int optionH		= s15;
	private static final int optionIndent	= s15;

	protected LinkedList<Integer> colSettingsCount;
	private	  LinkedList<Integer> spacerList;
	private   LinkedList<Integer> columnList;
	protected LinkedList<SettingBase<?>> settingList;
	public    LinkedList<SettingBase<?>> commonList;
	protected String guiTitleID;
	public final CustomRaceDefinitions cr = new CustomRaceDefinitions();

	protected int numColumns	= 0;
	protected int columnsMaxH	= 0;

	protected int xButton, yButton;
	protected int yTitle;
	protected int xCost, yCost;
	protected int w, wBG, h, hBG;
	protected int columnH		= 0;
	protected int numSettings	= 0;
	protected int settingSize;
	protected int settingBoxH;
	protected int leftM, topM, yTop;
	protected int xLine, yLine; // settings var

	protected BasePanel parent;
	protected Rectangle hoverBox;
	protected final Rectangle exitBox = new Rectangle();
	protected BaseText totalCostText;
	private RacesUI	raceUI; // Parent panel
	protected int maxLeftM;
	
	// ========== Constructors and initializers ==========
	//
	public ShowCustomRaceUI() {
		maxLeftM	= scaled(100);
		guiTitleID	= ROOT + "SHOW_TITLE";
		init_0();
	}
	private void init_0() {
		setOpaque(false);
	    totalCostText = new BaseText(this, false, costFontSize, 0, 0, 
	    		costC, costC, hoverC, depressedC, costC, 0, 0, 0);

	    // Call for filling the settings
	    if (colSettingsCount == null)
	    	initGUI();

	    commonList = settingList;
	    cr.setRace(MOO1GameOptions.baseRaceOptions().getFirst());
	    cr.pullSettings();
		addMouseListener(this);
		addMouseMotionListener(this);
	}
	public void loadRace() { // For Race Diplomatic UI Panel
		cr.initShowRace(raceUI.selectedEmpire().abilitiesKey());
	}
	public void init(RacesUI p) { // For Race Diplomatic UI Panel
		raceUI     = p;
		cr.initShowRace(initialRace);
	}
	protected void initGUI() {
		columnList	= cr.columnList();
		spacerList	= cr.spacerList();
		settingList	= cr.settingList();
		settingSize	= settingList.size();
		
		for (int i=0; i<settingSize; i++) {
			if (spacerList.contains(i))
				columnH += settingH;
			if (columnList.contains(i))
				endOfColumn();
			columnH += settingHPad;
			initSetting(settingList.get(i));
			numSettings++;
		}
		endOfColumn();
	}
	private void initSetting(SettingBase<?> setting) {
		if (setting.isBullet()) {
			int optionCount = setting.boxSize(); // +1 for the setting
			int paramIdx	= setting.index();
			setting.settingText(settingBT());
			columnH += settingH;
			columnH += frameTopPad;
			for (int optionIdx=0; optionIdx < optionCount; optionIdx++) {
				setting.optionText(optionBT(), optionIdx);
				setting.optionText(optionIdx).disabled(optionIdx == paramIdx);
				columnH	+= optionH;
			}
			columnH += frameEndPad;
		} else {
			setting.settingText(settingBT());			
			columnH += settingH;
		}		
	}
	public void open(BasePanel p) {
		parent = p;
		init();
		enableGlassPane(this);
		repaint();
	}
	protected void init() {
		if (cr.race() == null) {
			cr.setRace(newGameOptions().selectedPlayerRace());
			cr.pullSettings();
		}
		for (SettingBase<?> setting : commonList) { // Loop thru the setting list
			if (setting.isBullet()) {
				setting.settingText().displayText(setting.guiSettingDisplayStr()); // The setting
				int optionCount = setting.boxSize();
				for (int optionIdx=0; optionIdx <  optionCount; optionIdx++) {
					setting.optionText(optionIdx).displayText(setting.guiCostOptionStr(optionIdx)); // The options
				}
			} else {
				setting.settingText().displayText(setting.guiSettingDisplayStr());			
			}
		}
		totalCostText.displayText(totalCostStr());
		totalCostText.disabled(true);
	}
	// ========== Other Methods ==========
	//
	protected  String totalCostStr() {
		return text(totalCostKey, Math.round(cr.getTotalCost()));
	}
	protected  BaseText settingBT() {
		return new BaseText(this, false, settingFont, 0, 0,
				settingC, settingNegC, hoverC, depressedC, textC, 0, 0, 0);
	}
	protected  BaseText optionBT() {
		return new BaseText(this, false, optionFont, 0, 0,
				optionC, selectC, hoverC, depressedC, textC, 0, 0, 0);
	}
	protected void endOfColumn() {
		columnsMaxH = max(columnsMaxH, columnH);
		colSettingsCount.add(numSettings);
		numColumns++;
		numSettings	= 0;
		columnH		= 0;
	}
	protected void close() {
		disableGlassPane();
	}
//	public void saveOptions(MOO1GameOptions destination) {
//		for (InterfaceOptions param : commonList)
//			param.setOptions(destination);
//		customPlayerRace.setOptions(destination);
//	}
	public void getOptions(MOO1GameOptions source) {
		for (InterfaceOptions param : commonList)
			param.setFromOptions(source);
		customPlayerRace.setFromOptions(source);
		init();
	}
	private void doExitBoxAction() {
		close();			
	}
//	private void doSelectBoxAction() {
//		cr.pushSettings();
//		customPlayerRace.set(true);
//		saveLastOptions();
//		close();
//	}
//	private void doDefaultBoxAction() {
//		switch (Modifier2KeysState.get()) {
//		case CTRL:
//		case CTRL_SHIFT: // set to last
//			getOptions(MOO1GameOptions.loadLastOptions());
//			break;
//		case SHIFT: // set to last game options
//			if (options() != null)
//				getOptions(MOO1GameOptions.loadGameOptions());			
//			break;
//		default: // set to default
//			setToDefault();
//			break; 
//		}
//		repaint();
//	}
//	private void doUserBoxAction() {
//		switch (Modifier2KeysState.get()) {
//		case CTRL:
//		case CTRL_SHIFT: // Save
//			saveUserOptions();
//			break;
//		default: // Set
//			MOO1GameOptions fileOptions = MOO1GameOptions.loadUserOptions();
//			getOptions(fileOptions);
//			repaint();
//		}
//	}	
//	public void setToDefault() {
//		for (InterfaceOptions param : commonList)
//			param.setFromDefault();
//		init();
//	}
//	private void saveUserOptions() {
//		MOO1GameOptions fileOptions = MOO1GameOptions.loadUserOptions();
//		saveOptions(fileOptions);
//		MOO1GameOptions.saveUserOptions(fileOptions);
//	}
//	private void saveLastOptions() {
//		MOO1GameOptions fileOptions = MOO1GameOptions.loadLastOptions();
//		saveOptions(fileOptions);
//		MOO1GameOptions.saveLastOptions(fileOptions);
//	}
	protected void checkModifierKey(InputEvent e) {
		if (Modifier2KeysState.checkForChange(e)) {
			repaint();
		}
	}
	protected void paintSetting(Graphics2D g, SettingBase<?> setting) {
		int sizePad	= frameSizePad;
		int endPad 	= frameEndPad;
		int optNum	= setting.boxSize();;
		float cost 	= setting.settingCost();
		BaseText bt	= setting.settingText();
		int paramId	= setting.index();

		if (optNum == 0) {
			endPad	= 0;
			sizePad	= 0;
		}
		settingBoxH	= optNum * optionH + sizePad;
		// frame
		g.setColor(frameC);
		g.drawRect(xLine, yLine - frameShift, wSetting, settingBoxH);
		g.setPaint(GameUI.settingsSetupBackground(w));
		bt.displayText(setting.guiSettingDisplayStr());
		g.fillRect(xLine + settingIndent/2, yLine -s12 + frameShift,
				bt.stringWidth(g) + settingIndent, s12);
		if (cost == 0) 
			bt.enabledC(settingC);
		else if (cost > 0)
			bt.enabledC(settingPosC);
		else
			bt.enabledC(settingNegC);		
		bt.setScaledXY(xLine + settingIndent, yLine);
		bt.draw(g);
		yLine += settingH;
		yLine += frameTopPad;
		// Options
		for (int optionId=0; optionId < optNum; optionId++) {
			bt = setting.optionText(optionId);
			bt.disabled(optionId == paramId);
			bt.displayText(setting.guiCostOptionStr(optionId));
			bt.setScaledXY(xLine + optionIndent, yLine);
			bt.draw(g);
			yLine += optionH;
		}				
		yLine += endPad;
	}
	protected void updateBulletSetting(SettingBase<?> setting) {
		setting.guiSelect();
		totalCostText.repaint(totalCostStr());
	}
	protected void mouseCommon(boolean up, boolean mid, boolean shiftPressed, boolean ctrlPressed
			, MouseEvent e, MouseWheelEvent w) {
		for (int settingIdx=0; settingIdx < commonList.size(); settingIdx++) {
			SettingBase<?> setting = commonList.get(settingIdx);
			if (setting.isBullet()) {
				if (hoverBox == setting.settingText().bounds()) { // Check Setting
					setting.toggle(e, w);
					updateBulletSetting(setting);
					return;
				} else { // Check options
					int optionCount	= setting.boxSize(); // 1 for the setting
					for (int optionIdx=0; optionIdx < optionCount; optionIdx++) {
						if (hoverBox == setting.optionText(optionIdx).bounds()) {
							setting.index(optionIdx);
							updateBulletSetting(setting);
							return;
						}
					}
				}
			} else if (hoverBox == setting.settingText().bounds()) {
				setting.toggle(e, w);
				setting.settingText().repaint();
				totalCostText.repaint(totalCostStr());
				return;
			}
		}
	}
	// ========== Overriders ==========
	//
	@Override public void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0;
		w	  = getWidth();
		h	  = getHeight();
		hBG	  = titlePad + columnsMaxH + bottomPad;
		topM  = (h - hBG)/2;
		yTop  = topM + titlePad; // First setting top position
		wBG	  = (wSetting + columnPad) * numColumns;
		leftM = Math.min((w - wBG)/2, maxLeftM);
		yTitle	= topM + titleOffset;
		yButton	= topM + hBG - yButtonOffset;
		yCost	= yTitle + costOffset;
		xCost	= leftM + columnPad/2;
		xLine	= leftM + columnPad/2;
		yLine	= yTop;

		// draw background "haze"
		g.setColor(backgroundHaze);
		g.fillRect(0, 0, w, h);
		
		g.setPaint(GameUI.settingsSetupBackground(w));
		g.fillRect(leftM, topM, wBG, hBG);
		g.setFont(titleFont);
		String title = text(guiTitleID);

		int sw = g.getFontMetrics().stringWidth(title);
		int xTitle = leftM +(wBG-sw)/2;
		drawBorderedString(g, title, 1, xTitle, yTitle, Color.black, Color.white);
		
		totalCostText.displayText(totalCostStr());
		totalCostText.setScaledXY(xCost, yCost);
		totalCostText.draw(g);
		
		// Loop thru the parameters
		xLine = leftM+s10;
		yLine = yTop;
		// First column (left)
		// Loop thru parameters

		Stroke prev = g.getStroke();
		g.setStroke(stroke2);
		
		for (int i=0; i<settingSize; i++) {
			if (spacerList.contains(i))
				yLine += spacerH;
			if (columnList.contains(i)) {
				xLine = xLine + wSetting + columnPad;
				yLine = yTop;
			}
			paintSetting(g, settingList.get(i));
			yLine += settingHPad;
		}
		g.setStroke(prev);

		int cnr = s5;
		g.setFont(buttonFont);
		// Exit Button
		String text = text(exitButtonKey());
		sw = g.getFontMetrics().stringWidth(text);
		int buttonW	= exitButtonWidth(g);
		xButton = leftM + wBG - buttonW - xButtonOffset;
		exitBox.setBounds(xButton, yButton, buttonW, buttonH);
		g.setColor(GameUI.buttonBackgroundColor());
		g.fillRoundRect(exitBox.x, exitBox.y, buttonW, buttonH, cnr, cnr);
		int xT = exitBox.x+((exitBox.width-sw)/2);
		int yT = exitBox.y+exitBox.height-s8;
		Color cB = hoverBox == exitBox ? Color.yellow : GameUI.borderBrightColor();
		drawShadowedString(g, text, 2, xT, yT, GameUI.borderDarkColor(), cB);
		prev = g.getStroke();
		g.setStroke(stroke1);
		g.drawRoundRect(exitBox.x, exitBox.y, exitBox.width, exitBox.height, cnr, cnr);
		g.setStroke(prev);

	}
	@Override public void keyReleased(KeyEvent e) {
		checkModifierKey(e);
	}
	@Override public void keyPressed(KeyEvent e) {
		checkModifierKey(e);
		int k = e.getKeyCode();  // BR:
		switch(k) {
			case KeyEvent.VK_ESCAPE:
				doExitBoxAction();
				return;
			case KeyEvent.VK_SPACE:
			case KeyEvent.VK_ENTER:
				parent.advanceHelp();
				return;
			default:
				return;
		}
	}
	@Override public void mouseDragged(MouseEvent e) {  }
	@Override public void mouseMoved(MouseEvent e) {
		checkModifierKey(e);
		int x = e.getX();
		int y = e.getY();
		Rectangle prevHover = hoverBox;
		hoverBox = null;
		if (exitBox.contains(x,y))
			hoverBox = exitBox;
		if (hoverBox != prevHover) {
			if (prevHover != null) repaint(prevHover);
			if (hoverBox != null)  repaint(hoverBox);
		}
	}
	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mousePressed(MouseEvent e) { }
	@Override public void mouseReleased(MouseEvent e) {
		if (e.getButton() > 3)
			return;
		if (hoverBox == null)
			return;
		if (hoverBox == exitBox) {
			doExitBoxAction();
			return;
		}
	}
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) {
		if (hoverBox != null) {
			hoverBox = null;
			repaint();
		}
	}
}
