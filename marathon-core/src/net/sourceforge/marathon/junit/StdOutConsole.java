/*******************************************************************************
 * 
 * Copyright (C) 2010 Jalian Systems Private Ltd. Copyright (C) 2010 Contributors to Marathon OSS
 * Project
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Library General Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 * 
 * Project website: http://www.marathontesting.com Help: Marathon help forum @
 * http://groups.google.com/group/marathon-testing
 * 
 *******************************************************************************/
package net.sourceforge.marathon.junit;

import net.sourceforge.marathon.api.AbstractFileConsole;

public class StdOutConsole extends AbstractFileConsole {

    public void write(char cbuf[], int off, int len) {
        byte[] buf = new byte[len];
        for (int i = off; i < off + len; i++) {
            buf[i - off] = (byte) cbuf[i];
        }
        System.out.write(buf, 0, len);
        writeToFile(String.valueOf(cbuf, off, len));
    }

    public void writeStdOut(char cbuf[], int off, int len) {
        write(cbuf, 0, len);
    }

    public void writeStdErr(char cbuf[], int off, int len) {
        write(cbuf, 0, len);
    }

    public void writeScriptOut(char cbuf[], int off, int len) {
        write(cbuf, 0, len);
    }

    public void writeScriptErr(char cbuf[], int off, int len) {
        write(cbuf, 0, len);
    }

    public void clear() {
    }

}
