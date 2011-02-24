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
package net.sourceforge.marathon.api;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

public class PlaybackResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private Collection<Failure> failures = new LinkedList<Failure>();

    public void addFailure(String message, SourceLine[] traceback) {
        failures.add(new Failure(message, traceback));
    }

    public Failure[] failures() {
        return (Failure[]) failures.toArray(new Failure[failures.size()]);
    }

    public int failureCount() {
        return failures.size();
    }

    public boolean hasFailure() {
        return failureCount() > 0;
    }

    public void addFailure(Failure[] f) {
        failures.addAll(Arrays.asList(f));
    }
}
