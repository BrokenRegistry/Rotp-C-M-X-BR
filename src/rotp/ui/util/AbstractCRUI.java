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
package rotp.ui.util;

import static rotp.model.empires.CustomRace.ROOT;
import static rotp.ui.UserPreferences.customPlayerRace;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.text.DecimalFormat;
import java.util.LinkedList;

import javax.swing.SwingUtilities;

import rotp.mod.br.profiles.Profiles;
import rotp.model.empires.CustomRace;
import rotp.ui.BasePanel;
import rotp.ui.BaseText;
import rotp.ui.game.GameUI;
import rotp.ui.main.SystemPanel;

// modnar: add UI panel for modnar MOD game options, based on StartOptionsUI.java
public abstract class AbstractCRUI extends BasePanel implements MouseListener, MouseMotionListener, MouseWheelListener {
	private static final long serialVersionUID	= 1L;
	private static final Color backgroundHaze	= new Color(0,0,0,160);
	private static final String totalCostKey	= "CUSTOM_RACE_GUI_COST";
	private static final String exitKey			= "SETTINGS_EXIT";
	private static final String selectKey		= "CUSTOM_RACE_GUI_SELECT";
	private static final String randomKey		= "CUSTOM_RACE_GUI_RANDOM";
	private static final LinkedList<Integer> colSettingsCount   = new LinkedList<>();
	private static final LinkedList<SettingBase<?>> settingList = new LinkedList<>();
	private static final LinkedList<SettingBase<?>> guiList		= new LinkedList<>();
	public	static final CustomRace cr = new CustomRace();
	
	private static final Color textC		= SystemPanel.whiteText;
	private		   final Font buttonFont	= narrowFont(20);
	private static final int buttonW		= s100+s80;
	private static final int buttonH		= s30;
	private static final int buttonPad		= s20;
	private static final int xButtonOffset	= s30;
	private static final int yButtonOffset	= s40;
	private static final Color labelC		= SystemPanel.orangeText;
	private	static final int labelFontSize	= 14;
	private static final int labelH			= s16;
	private static final int labelPad		= s8;

	private static final Color costC		= SystemPanel.blackText;
	private		   final String guiTitleID;
	private		   final Font titleFont		= narrowFont(30);
	private	static final int costFontSize	= 18;
	private static final int titleOffset	= s30; // Offset from Margin
	private static final int costOffset		= s25; // Offset from title
	private static final int titlePad		= s75;
	private static final int bottomPad		= s40;
	private static final int columnPad		= s20;
	
	private static final Color frameC		= SystemPanel.blackText; // Setting frame color
	private static final Color settingPosC	= SystemPanel.redText; // Setting name color
	private static final Color settingNegC	= SystemPanel.limeText; // Setting name color
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

//	private static final SettingInteger randomMean = new SettingInteger(ROOT, "RANDOM_MEAN",
//			0, null, null, 5, 20, 100);
//	private static final SettingInteger randomStDev = new SettingInteger(ROOT, "RANDOM_STDEV",
//			0, 0, null, 1, 5, 20);
	private static final SettingInteger randomMax = new SettingInteger(ROOT, "RANDOM_MAX",
			0, -100, 100, 1, 5, 20);
	private static final SettingInteger randomMin = new SettingInteger(ROOT, "RANDOM_MIN",
			0, -100, 100, 1, 5, 20);
	private static final SettingBoolean randomSmoothEdges = new SettingBoolean(ROOT, "RANDOM_EDGES", false);

	private static int numColumns	= 0;
	private static int columnsMaxH	= 0;

	private int xButton, yButton;
	private int yTitle;
	private int xCost, yCost;
	private int w, wBG, h, hBG;
	private int columnH		= 0;
	private int numSettings	= 0;
	private int settingBoxH;
	private int leftM, topM, yTop;
	private int xLine, yLine; // settings var
	private int columnIndex, rowIndex;

	private BasePanel parent;
	private Rectangle hoverBox;
	private Rectangle okBox 	= new Rectangle();
	private Rectangle selectBox	= new Rectangle();
	private Rectangle randomBox	= new Rectangle();
	private BaseText totalCostText;
	
	// ========== Constructors and initializers ==========
	//
	public AbstractCRUI(String guiTitle_ID) {
		guiTitleID = guiTitle_ID;
		init_0();
	}
	private void init_0() {
		setOpaque(false);
		cr.init(settingList);
	    totalCostText = new BaseText(this, false, costFontSize, 0, 0, 
	    		costC, costC, hoverC, depressedC, costC, 0, 0, 0);

	    // Call for filling the settings
	    if (settingList.size() == 0)
	    	init0();

	    guiList.add(randomMax);
	    guiList.add(randomMin);
	    guiList.add(randomSmoothEdges);
	    
	    for(SettingBase<?> setting : guiList) {
	    	setting.saveAllowed(false);
	    	setting.hasNoCost(true);
	    	setting.settingText(new BaseText(this, false, labelFontSize, 0, 0,
					labelC, labelC, hoverC, depressedC, textC, 0, 0, 0));
	    	
	    }
//		randomMean.saveAllowed(false);
//		randomMean.hasNoCost(true);
//		randomMean.settingText(new BaseText(this, false, labelFontSize, 0, 0,
//				labelC, labelC, hoverC, depressedC, textC, 0, 0, 0));
//		randomStDev.saveAllowed(false);
//		randomStDev.hasNoCost(true);
//		randomStDev.settingText(new BaseText(this, false, labelFontSize, 0, 0,
//				labelC, labelC, hoverC, depressedC, textC, 0, 0, 0));

		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
	}
	private void init() {
		if (cr.getRace() == null) {
			cr.setRace(newGameOptions().selectedPlayerRace());
			cr.pullSettings();
		}
		// Display text Initialization
		for (SettingBase<?> setting : settingList) { // Loop thru the setting list
			if (setting.isSpacer())
				continue;
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
	// ========== Abstract Methods Request ==========
	//
	protected abstract void init0();
	// ========== Other Methods ==========
	//
	private  String totalCostStr() {
		return text(totalCostKey, new DecimalFormat("0.0").format(cr.getTotalCost()));
	}
	private  BaseText settingBT() {
		return new BaseText(this, false, settingFont, 0, 0,
				settingC, settingNegC, hoverC, depressedC, textC, 0, 0, 0);
	}
	private  BaseText optionBT() {
		return new BaseText(this, false, optionFont, 0, 0,
				optionC, selectC, hoverC, depressedC, textC, 0, 0, 0);
	}
	protected void newSetting(SettingBase<?> setting) {
		columnH += settingHPad;
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
			numSettings++;
			columnH += frameEndPad;
		} else {
			setting.settingText(settingBT());			
			numSettings++;
			columnH += settingH;
		}
		settingList.add(setting);
	}
	protected void endOfColumn() {
		columnsMaxH = max(columnsMaxH, columnH);
		colSettingsCount.add(numSettings);
		numColumns++;
		numSettings	= 0;
		columnH		= 0;
	}
	public void open(BasePanel p) {
		parent = p;
		init();
		enableGlassPane(this);
	}
	private void close() {
		disableGlassPane();
	}
	private void selectRace() {
		cr.pushSettings();
		customPlayerRace.set(true);
		close();
	}
	private void randomizeRace() {
		cr.randomizeRace(randomMin.settingValue(),
				randomMax.settingValue(), randomSmoothEdges.settingValue());
		totalCostText.repaint(totalCostStr());
	}
	private void paintSetting(Graphics2D g, SettingBase<?> setting) {
		if (setting.isSpacer()) {
			yLine += spacerH;
			return;
		}
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
	private void goToNextSetting() {
		rowIndex++;
		if (rowIndex >= colSettingsCount.get(columnIndex)) {
			rowIndex = 0;
			columnIndex++;
			xLine = xLine + wSetting + columnPad;
			yLine = yTop;
		} else
			yLine += settingHPad;
	}
	private void updateBulletSetting(SettingBase<?> setting) {
		setting.guiSelect();
		totalCostText.repaint(totalCostStr());
	}
	private void mouseCommon(boolean up, boolean mid, boolean shiftPressed, boolean ctrlPressed
			, MouseEvent e, MouseWheelEvent w) {
		for (int settingIdx=0; settingIdx < settingList.size(); settingIdx++) {
			SettingBase<?> setting = settingList.get(settingIdx);
			if (setting.isSpacer())
				continue;
			if (setting.isBullet()) {
				if (hoverBox == setting.settingText().bounds()) { // Check Setting
					setting.toggle(e, w);
					updateBulletSetting(setting);
					return;
				} else { // Check options
					int optionCount	= setting.boxSize(); // 1 for the setting
					for (int optionIdx=0; optionIdx < optionCount; optionIdx++) {
						if (hoverBox == setting.optionText(optionIdx).bounds()) {
							setting.setFromIndexAndSave(optionIdx);
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
		for (SettingBase<?> setting : guiList) {
			if (hoverBox == setting.settingText().bounds()) {
				setting.toggle(e, w);
				setting.settingText().repaint();
				return;
			}
		}
	}
	// ========== Overriders ==========
	//
	@Override public void paintComponent(Graphics g0) {
		super.paintComponent(g0);
		Graphics2D g = (Graphics2D) g0;
		w		= getWidth();
		h		= getHeight();
		hBG		= titlePad + columnsMaxH + bottomPad;
		topM	= (h - hBG)/2;
		yTop	= topM + titlePad; // First setting top position
		wBG		= (wSetting + columnPad) * numColumns;
		leftM	= (w - wBG)/2;
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
		int xTitle = (w-sw)/2;
		drawBorderedString(g, title, 1, xTitle, yTitle, Color.black, Color.white);
		
		totalCostText.setScaledXY(xCost, yCost);
		totalCostText.draw(g);
		
		// Loop thru the parameters
		columnIndex	 = 0;
		rowIndex	 = 0;
		xLine = leftM+s10;
		yLine = yTop;
		// First column (left)
		// Loop thru parameters

		Stroke prev = g.getStroke();
		g.setStroke(stroke2);
		for (SettingBase<?> setting : settingList) {
			paintSetting(g, setting);
			goToNextSetting();
		}
		g.setStroke(prev);

		int cnr = s5;
		// Exit Button
		xButton = leftM + wBG - buttonW - xButtonOffset;
		okBox.setBounds(xButton, yButton, buttonW, buttonH);
		g.setColor(GameUI.buttonBackgroundColor());
		g.fillRoundRect(okBox.x, okBox.y, buttonW, buttonH, cnr, cnr);
		g.setFont(buttonFont);
		String text = text(exitKey);
		sw = g.getFontMetrics().stringWidth(text);
		int xT = okBox.x+((okBox.width-sw)/2);
		int yT = okBox.y+okBox.height-s8;
		Color cB = hoverBox == okBox ? Color.yellow : GameUI.borderBrightColor();
		drawShadowedString(g, text, 2, xT, yT, GameUI.borderDarkColor(), cB);
		prev = g.getStroke();
		g.setStroke(stroke1);
		g.drawRoundRect(okBox.x, okBox.y, okBox.width, okBox.height, cnr, cnr);
		g.setStroke(prev);

		// Select Button
		text = text(selectKey);
		xButton -= (buttonW + buttonPad);
		sw = g.getFontMetrics().stringWidth(text);
		selectBox.setBounds(xButton, yButton, buttonW, buttonH);
		g.setColor(GameUI.buttonBackgroundColor());
		g.fillRoundRect(selectBox.x, selectBox.y, buttonW, buttonH, cnr, cnr);
		g.setFont(buttonFont);
		xT = selectBox.x+((selectBox.width-sw)/2);
		yT = selectBox.y+selectBox.height-s8;
		cB = hoverBox == selectBox ? Color.yellow : GameUI.borderBrightColor();
		drawShadowedString(g, text, 2, xT, yT, GameUI.borderDarkColor(), cB);
		prev = g.getStroke();
		g.setStroke(stroke1);
		g.drawRoundRect(selectBox.x, selectBox.y, selectBox.width, selectBox.height, cnr, cnr);
		g.setStroke(prev);

		// Randomize Button
		text = text(randomKey);
		xButton = leftM + buttonPad;
		sw = g.getFontMetrics().stringWidth(text);
		randomBox.setBounds(xButton, yButton, buttonW, buttonH);
		g.setColor(GameUI.buttonBackgroundColor());
		g.fillRoundRect(randomBox.x, randomBox.y, buttonW, buttonH, cnr, cnr);
		g.setFont(buttonFont);
		xT = randomBox.x+((randomBox.width-sw)/2);
		yT = randomBox.y+randomBox.height-s8;
		cB = hoverBox == randomBox ? Color.yellow : GameUI.borderBrightColor();
		drawShadowedString(g, text, 2, xT, yT, GameUI.borderDarkColor(), cB);
		prev = g.getStroke();
		g.setStroke(stroke1);
		g.drawRoundRect(randomBox.x, randomBox.y, randomBox.width, randomBox.height, cnr, cnr);
		g.setStroke(prev);
		
		// Randomize Options
		xLine = xButton + labelPad;
		yLine = yButton - labelPad;
		BaseText bt = randomSmoothEdges.settingText();
		bt.displayText(randomSmoothEdges.guiSettingDisplayStr());
		bt.setScaledXY(xLine, yLine);
		bt.draw(g);
		
		yLine -= labelH;
		bt = randomMin.settingText();
		bt.displayText(randomMin.guiSettingDisplayStr());
		bt.setScaledXY(xLine, yLine);
		bt.draw(g);

		yLine -= labelH;
		bt = randomMax.settingText();
		bt.displayText(randomMax.guiSettingDisplayStr());
		bt.setScaledXY(xLine, yLine);
		bt.draw(g);
	}
	@Override public void keyPressed(KeyEvent e) {
		int k = e.getKeyCode();  // BR:
		switch(k) {
			case KeyEvent.VK_ESCAPE:
				close();
				break;
			case KeyEvent.VK_SPACE:
			case KeyEvent.VK_ENTER:
				parent.advanceHelp();
				break;
			default: // BR:
				if(Profiles.processKey(k, e.isShiftDown(), guiTitleID, newGameOptions())) {
				};
				// Needs to be done twice for the case both Galaxy size
				// and the number of opponents were changed !?
				if(Profiles.processKey(k, e.isShiftDown(), guiTitleID, newGameOptions())) {
//					for (int i=0; i<paramList.size(); i++) {
//						btList.get(i).repaint(paramList.get(i).getGuiDisplay());
//					} TODO BR: processKey
				};
				return;
		}
	}
	@Override public void mouseDragged(MouseEvent e) {  }
	@Override public void mouseMoved(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		Rectangle prevHover = hoverBox;
		hoverBox = null;
		if (okBox.contains(x,y))
			hoverBox = okBox;
		else if (selectBox.contains(x,y))
			hoverBox = selectBox;
		else if (randomBox.contains(x,y))
			hoverBox = randomBox;
		else {
			for (SettingBase<?> setting : guiList)
				if (setting.settingText().contains(x,y))
					hoverBox = setting.settingText().bounds();
			outerLoop1:
			for ( SettingBase<?> setting : settingList) {
				if (setting.isSpacer())
					continue;
				if (setting.settingText().contains(x,y)) {
					hoverBox = setting.settingText().bounds();
					break outerLoop1;
				}
				if (setting.isBullet()) {					
					for (BaseText txt : setting.optionsText()) {
						if (txt.contains(x,y)) {
							hoverBox = txt.bounds();
							break outerLoop1;
						}
					}
				}
			}
		}
		if (hoverBox != prevHover) {
			for (SettingBase<?> setting : guiList)
				if (prevHover == setting.settingText().bounds())
					setting.settingText().mouseExit();
			outerLoop2:
			for ( SettingBase<?> setting : settingList) {
				if (setting.isSpacer())
					continue;
				if (prevHover == setting.settingText().bounds()) {
					setting.settingText().mouseExit();
					break outerLoop2;
				}
				if (setting.isBullet()) {					
					for (BaseText txt : setting.optionsText()) {
						if (prevHover == txt.bounds()) {
							txt.mouseExit();
							break outerLoop2;
						}
					}
				}
			}
			for (SettingBase<?> setting : guiList)
				if (hoverBox == setting.settingText().bounds())
					setting.settingText().mouseEnter();
			outerLoop3:
			for ( SettingBase<?> setting : settingList) {
				if (setting.isSpacer())
					continue;
				if (hoverBox == setting.settingText().bounds()) {
					setting.settingText().mouseEnter();
					break outerLoop3;
				}
				if (setting.isBullet()) {					
					for (BaseText txt : setting.optionsText()) {
						if (hoverBox == txt.bounds()) {
							txt.mouseEnter();
							break outerLoop3;
						}
					}
				}
			}
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
		boolean up	= !SwingUtilities.isRightMouseButton(e); // BR: added bidirectional
		boolean mid	= !SwingUtilities.isMiddleMouseButton(e); // BR: added reset click
		boolean shiftPressed = e.isShiftDown();
		boolean ctrlPressed = e.isControlDown();
		mouseCommon(up, mid, shiftPressed, ctrlPressed, e, null);
		if (hoverBox == okBox)
			close();
		else if (hoverBox == selectBox)
			selectRace();
		else if (hoverBox == randomBox)
			randomizeRace();			
	}
	@Override public void mouseEntered(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) {
		if (hoverBox != null) {
			hoverBox = null;
			repaint();
		}
	}
	@Override public void mouseWheelMoved(MouseWheelEvent e) {
		boolean shiftPressed = e.isShiftDown();
		boolean ctrlPressed = e.isControlDown();
		boolean up = e.getWheelRotation() < 0;
		mouseCommon(up, false, shiftPressed, ctrlPressed, null, e);
	}
}