/*******************************************************************************
 *  
 *  Copyright (C) 2010 Jalian Systems Private Ltd.
 *  Copyright (C) 2010 Contributors to Marathon OSS Project
 * 
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public
 *  License as published by the Free Software Foundation; either
 *  version 2 of the License, or (at your option) any later version.
 * 
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Library General Public License for more details.
 * 
 *  You should have received a copy of the GNU Library General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 *  Project website: http://www.marathontesting.com
 *  Help: Marathon help forum @ http://groups.google.com/group/marathon-testing
 * 
 *******************************************************************************/
package net.sourceforge.marathon.action;

import java.util.regex.Pattern;

import net.sourceforge.marathon.api.ComponentId;
import net.sourceforge.marathon.api.IScriptModelServerPart;
import net.sourceforge.marathon.component.ComponentFinder;
import net.sourceforge.marathon.component.MComponent;
import net.sourceforge.marathon.recorder.WindowMonitor;
import net.sourceforge.marathon.util.Retry;
import net.sourceforge.marathon.util.Retry.Attempt;

public class WaitPropertyAction extends AbstractMarathonAction {
    private static final long serialVersionUID = 1L;
    private String property;
    private String value;

    public WaitPropertyAction(ComponentId componentId, String property, String value, IScriptModelServerPart scriptModel,
            WindowMonitor windowMonitor) {
        super(componentId, scriptModel, windowMonitor);
        this.property = property;
        this.value = value;
    }

    public void play(final ComponentFinder resolver) {
        new Retry("Wait for change in property timed out", 500, 120, new Attempt() {
            public void perform() {
                if (!isValueEqual(resolver))
                    retry();
            }
        });
    }

    private boolean isValueEqual(ComponentFinder resolver) {
        MComponent component = resolver.getMComponentById(getComponentId());
        Object actual;
        Object expected;
        if (property.equals("Text")) {
            actual = component.getComparableObject();
            expected = component.getComparableObject(value);
        } else {
            actual = component.getProperty(property);
            expected = value;
        }
        if (actual == null && expected == null)
            return true;
        if (actual == null || expected == null)
            return false;
        String expectedString = expected.toString();
        if (isRegex(expectedString) && Pattern.matches(expectedString.substring(1), actual.toString())) {
            return true;
        }
        if ((expectedString.startsWith("//") && expectedString.substring(1).equals(actual.toString()))
                || expectedString.equals(actual.toString()))
            return true;
        return false;
    }

    private boolean isRegex(String expectedString) {
        return expectedString.startsWith("/") && !expectedString.startsWith("//");
    }

    public String toScriptCode() {
        return scriptModel.getScriptCodeForWaitProperty(getComponentId(), property, value);
    }
}
