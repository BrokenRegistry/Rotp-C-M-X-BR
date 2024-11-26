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
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import rotp.model.game.IGameOptions;
import rotp.ui.BasePanel;
import rotp.ui.main.GalaxyMapPanel;

public class ShipDisplaySprite extends MapControlSprite  {
	static Color yellowC = Color.yellow;
	static Color redC = Color.red;
	static Color greenC = Color.green;
	static Color darkGreenC = new Color(0,128,0);
	static FlightPathSprite sprite = new FlightPathSprite();
	Color extColor, normColor;

	public ShipDisplaySprite(int xOff, int yOff, int w, int h) {
		xOffset = scaled(xOff);
		yOffset = scaled(yOff);
		width = scaled(w);
		height = scaled(h);
		extColor =  newColor(0,0,192,64);
		normColor = newColor(32,32,192,128);
	}
	@Override
	public void click(GalaxyMapPanel map, int count, boolean rightClick, boolean click, boolean middleClick, MouseEvent e) {
		map.toggleShipDisplay(rightClick);
	}
	@Override
	public void draw(GalaxyMapPanel map, Graphics2D g2) {
		int w = width;
		String detail ="";
		int fontSize = 13;
		int lineH	 = BasePanel.s14;
		List<String> detailLines = null;

		if (hovering) {
			g2.setFont(narrowFont(fontSize));
			detail = IGameOptions.shipDisplay.getLangLabel("_EXT");
			int detailW = g2.getFontMetrics().stringWidth(detail);
			int w0 = detailW * 3/5;
			detailLines = wrappedLines(g2, detail, w0);
			while (detailLines.size() > 2) {
				w0 += detailW/10;
				detailLines = wrappedLines(g2, detail, w0);
			}
			w = width + BasePanel.s15 + w0;
		}
		drawBackground(map, g2, w);

		int cnr = BasePanel.s12;		
		g2.setColor(player().scoutBorderColor());
		g2.fillRoundRect(startX, startY, width, height, cnr, cnr);

		g2.setClip(startX, startY, width, height);

		if (map.showArmedShips()) {
			BufferedImage img = player().shipImage();
			g2.drawImage(img, startX+BasePanel.s4, startY+BasePanel.s10, map);
		}
		if (map.showFriendlyTransports()) {
			BufferedImage img = player().transportImage();
			g2.drawImage(img, startX+BasePanel.s12, startY+BasePanel.s5, map);
		}
		if (map.showUnarmedShips()) {
			BufferedImage img = player().scoutImage();
			g2.drawImage(img, startX+BasePanel.s12, startY+BasePanel.s18, map);
		}

		g2.setClip(null);

		if (hovering) {
			g2.setColor(Color.lightGray);
			g2.setFont(narrowFont(fontSize));
			int y1 = startY + height - BasePanel.s17;
			int x1 = startX + width + BasePanel.s10;
			if (detailLines.size() == 1)
				y1 += BasePanel.s8;
			for (String line: detailLines) {
				drawString(g2, line, x1, y1);
				y1 += lineH;
			}
		}
		drawBorder(map, g2, w, map.parent().shadeC(), false);
	}
}
