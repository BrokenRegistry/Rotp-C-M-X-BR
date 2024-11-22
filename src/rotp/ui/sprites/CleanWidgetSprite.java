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
package rotp.ui.sprites;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.List;

import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;

public class CleanWidgetSprite extends MapControlSprite {
	private Image image;
	private int yOrigin, yShift;
	private boolean unlockedClean = true;
	private boolean lockedClean   = true;
	private boolean clean   = true;
	private int dirtyCount  = 0;
	private int lockedCount = 0;
	
	public CleanWidgetSprite(int xOff, int yOff, int w, int h, int shift) {
		xOffset = scaled(xOff);
		yOffset = scaled(yOff);
		width   = scaled(w);
		height  = scaled(h);
		yShift  = scaled(yOff+shift);
		yOrigin = yOffset;
	}
	private Image image()	{
		if (image == null)
			image = scaledImageW("CLEAN_BUTTON", width-BasePanel.s4);
		return image;
	}
	public void checkForEcoClean ()	{
		int[] needCleaning = player().needCleaning();
		lockedCount	  = needCleaning[1];
		lockedClean   = lockedCount == 0;
		unlockedClean = needCleaning[0] == 0;
		dirtyCount 	  = needCleaning[0] + lockedCount;
		clean = unlockedClean && lockedClean;
	}
	public void updateLocation()	{
		if (session().aFewMoreTurns())
			yOffset = yOrigin;
		else
			yOffset = yShift;
		checkForEcoClean ();
	}
	public void setCleanValue(boolean b) { clean = b; }
	@Override
	public boolean acceptDoubleClicks()	 { return false; }
	@Override
	public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click, boolean middleClick) {
		player().checkEcoAtClean(rightClick);
	}
	@Override
	public void draw(GalaxyMapPanel map, Graphics2D g2) {
		int w = width;
		int s2 = BasePanel.s2;
		String label;
		int fontSize = 13;
		int labelW;
		List<String> detailLines = null;
		
		if (hovering) {
			checkForEcoClean ();
			clean = unlockedClean && lockedClean;
			g2.setFont(narrowFont(fontSize));
			int w0;
			if (clean) {
				label = text("MOD_MAIN_COLONIES_ARE_CLEAN");
				labelW = g2.getFontMetrics().stringWidth(label);
				w0 = labelW*3/5;
			}
			else {
				if (dirtyCount == 1)
					label = text("MOD_MAIN_COLONIES_ARE_DIRTY_ALL1");
				else
					label = text("MOD_MAIN_COLONIES_ARE_DIRTY_ALL", dirtyCount);
				labelW = g2.getFontMetrics().stringWidth(label);
				w0 = labelW*3/5;
				if (!lockedClean) {
					w0 = labelW;
					if (dirtyCount == 1)
						label += text("MOD_MAIN_COLONIES_ARE_DIRTY_LOCK1");
					else
						label += text("MOD_MAIN_COLONIES_ARE_DIRTY_LOCK", lockedCount);
				}
			}
			labelW = g2.getFontMetrics().stringWidth(label);
			detailLines = wrappedLines(g2, label, w0);
			while (detailLines.size() > 2) {
				w0 += labelW/10;
				detailLines = wrappedLines(g2, label, w0);
			}
			w = width + BasePanel.s15 + w0;
		}
		drawBackground(map, g2, w);
		if (!clean)
			drawRedBackground(map, g2);

		g2.drawImage(image(), startX+s2, startY+s2, map);

		if (hovering) {
			g2.setColor(Color.lightGray);
			g2.setFont(narrowFont(fontSize));
			int y1 = startY + height - BasePanel.s17;
			int x1 = startX + width + BasePanel.s10;
			if (detailLines.size() == 1)
				y1 += BasePanel.s8;
			for (String line: detailLines) {
				drawString(g2, line, x1, y1);
				y1 += BasePanel.s14;
			}			
		}
		drawBorder(map, g2, w, map.parent().shadeC(), false);
	}
	public void drawRedBackground(GalaxyMapPanel map, Graphics2D g2) {
		int cnr = BasePanel.s12;
		startX = xOffset >= 0 ? xOffset : map.getWidth()+xOffset;
		startY = yOffset >= 0 ? yOffset : map.getHeight()+yOffset;
		g2.setColor(Color.RED);
		g2.fillRoundRect(startX, startY, width, height, cnr, cnr);
	}
	@Override public void drawBackground(GalaxyMapPanel map, Graphics2D g2, int w) {
		int cnr = BasePanel.s12;
		int brdr = BasePanel.s1;
		startX = xOffset >= 0 ? xOffset : map.getWidth()+xOffset;
		startY = yOffset >= 0 ? yOffset : map.getHeight()+yOffset;
		g2.setColor(map.parent().shadeC());
		g2.fillRoundRect(startX-brdr, startY-brdr, w+brdr+brdr, height+brdr+brdr, cnr, cnr);
	}

}
