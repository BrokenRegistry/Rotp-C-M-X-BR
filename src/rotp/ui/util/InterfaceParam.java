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

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import rotp.model.game.MOO1GameOptions;

public interface InterfaceParam <T> extends InterfaceOptions{
	public T setFromCfgValue(String val);
	public T next();
	public T prev();
	public T toggle(MouseWheelEvent e);
	public T toggle(MouseEvent e);
	public T toggle(MouseEvent e, MouseWheelEvent w);
	public String getCfgValue();
	public String getCfgLabel();
	public String getGuiDisplay();
	public String getGuiDescription();
}
