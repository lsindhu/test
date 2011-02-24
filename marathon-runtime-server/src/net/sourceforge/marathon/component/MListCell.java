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
package net.sourceforge.marathon.component;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Properties;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

import net.sourceforge.marathon.action.ClickAction;
import net.sourceforge.marathon.recorder.WindowMonitor;

/**
 * This is the list cell component. It currently don't support editing.
 */
public class MListCell extends MCellComponent {
    private int index;
    private String text;

    public MListCell(JList list, String name, Object pointOrInfo, ComponentFinder finder, WindowMonitor windowMonitor) {
        super(list, name, finder, windowMonitor);
        // If the point is being pass in, it's created from recording of gui
        // If the info(String) is being pass in, it's created from playing the
        // script
        if (pointOrInfo instanceof Point) {
            Point point = (Point) pointOrInfo;
            index = eventQueueRunner.invokeInteger(list, "locationToIndex", new Object[] { point }, new Class[] { Point.class });
            text = getTextFromIndex();
        } else {
            String info = (String) pointOrInfo;
            Properties props = parseProperties(info, new String[][] { { "Index", "Text" }, { "Text" } });
            if (props == null) {
                parseCellSpec(info);
                text = getTextFromIndex();
                return;
            }
            index = -1;
            String indexString = props.getProperty("Index");
            if (indexString == null) {
                MListCell item = (MListCell) getCollectionComponent().findMatchingComponent(props);
                if (item == null)
                    throw new ComponentException("Could not find list cell component matching given property list: " + props,
                            finder.getScriptModel(), windowMonitor);
                if (item != null)
                    index = item.getIndex();
            } else {
                index = Integer.parseInt(indexString);
            }
            if (index < 0 || index >= list.getModel().getSize()) {
                throw new ComponentException("Invalid property list " + info + " for List(" + getMComponentName() + ")",
                        finder.getScriptModel(), windowMonitor);
            }
            text = getTextFromIndex();
        }
    }

    MListCell(JList list, String name, int index, ComponentFinder finder, WindowMonitor windowMonitor) {
        super(list, name, finder, windowMonitor);
        this.index = index;
        text = getTextFromIndex();
    }

    public String getComponentInfo() {
        if (getCollectionComponent().hasDuplicates())
            return createPropertyMapString(new String[] { "Index", "Text" });
        else
            return createPropertyMapString(new String[] { "Text" });
    }

    private void parseCellSpec(String cellSpec) {
        try {
            index = Integer.parseInt(cellSpec);
        } catch (Exception e) {
            throw new ComponentException("Cell specification invalid : " + cellSpec, finder.getScriptModel(), windowMonitor);
        }
    }

    public void click(int numberOfClicks, int modifiers, Point position) {
        if (position == null) {
            Rectangle rect = (Rectangle) eventQueueRunner.invoke(getListComponent(), "getCellBounds", new Object[] {
                    new Integer(index), new Integer(index) }, new Class[] { Integer.TYPE, Integer.TYPE });
            position = rect.getLocation();
        }
        super.click(numberOfClicks, modifiers, position);
    }

    /**
     * Get text from renderer
     * 
     * @return String in that list cell component
     */
    public String getTextFromIndex() {
        MComponent renderer = getRenderer();
        // this is put in to avoid some possible invalid state of the
        // application's List implementation
        if (renderer == null) {
            ListModel listModel = (ListModel) eventQueueRunner.invoke(getListComponent(), "getModel");
            return (index < 0) ? null : listModel.getElementAt(index).toString();
        } else {
            return renderer.getText();
        }
    }

    public String getText() {
        return text;
    }

    public MComponent getRenderer() {
        ListCellRenderer renderer = (ListCellRenderer) eventQueueRunner.invoke(getListComponent(), "getCellRenderer");
        boolean isSelected = isSelected();
        ListModel listModel = (ListModel) eventQueueRunner.invoke(getListComponent(), "getModel");
        Component rendererComponent = renderer.getListCellRendererComponent(getListComponent(), listModel.getElementAt(index),
                index, isSelected, isSelected);
        MComponent mcomponent = rendererComponent == null ? null : finder.getMComponentByComponent(rendererComponent, getMComponentName() + "," + index,
                null);
        return mcomponent;
    }

    @Override public int clickNeeded(MouseEvent e) {
        MComponent renderer = getRenderer();
        if (renderer == null || renderer instanceof MUnknownComponent)
            return ClickAction.RECORD_EX;
        if (e.getClickCount() > 1 || e.isPopupTrigger())
            return ClickAction.RECORD_CLICK;
        return ClickAction.RECORD_NONE;
    }

    private boolean isSelected() {
        return eventQueueRunner.invokeBoolean(getListComponent(), "isSelectedIndex", new Object[] { new Integer(index) },
                new Class[] { Integer.TYPE });
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MListCell))
            return false;
        if (!super.equals(o))
            return false;
        final MListCell mListCell = (MListCell) o;
        if (index != mListCell.index)
            return false;
        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 29 * result + index;
        return result;
    }

    private JList getListComponent() {
        return (JList) getComponent();
    }

    public String toString() {
        return super.toString() + "[" + index + "]";
    }

    public MCollectionComponent getCollectionComponent() {
        return new MList(getListComponent(), getMComponentName(), finder, windowMonitor);
    }

    public void setCurrentSelection() {
        eventQueueRunner.invoke(getListComponent(), "setSelectedIndex", new Object[] { new Integer(index) },
                new Class[] { Integer.TYPE });
    }

    public boolean keyNeeded(KeyEvent e) {
        return getCollectionComponent().keyNeeded(e);
    }

    protected Class<?>[] getPropertyAccessMethodParameters(String property) {
        if (property.equals("Text"))
            return new Class[] {};
        else
            return new Class[] { Integer.TYPE };
    }

    protected Object[] getPropertyAccessMethodArguments(String property) {
        if (property.equals("Text"))
            return new Object[] {};
        else
            return new Object[] { new Integer(index) };
    }

    public Point getLocation() {
        Rectangle bounds = (Rectangle) eventQueueRunner.invoke(getListComponent(), "getCellBounds", new Object[] {
                new Integer(index), new Integer(index) }, new Class[] { Integer.TYPE, Integer.TYPE });
        return bounds.getLocation();
    }

    public Dimension getSize() {
        Rectangle bounds = (Rectangle) eventQueueRunner.invoke(getListComponent(), "getCellBounds", new Object[] {
                new Integer(index), new Integer(index) }, new Class[] { Integer.TYPE, Integer.TYPE });
        return bounds.getSize();
    }

    protected String getCollectionComponentAccessMethodName() {
        return "getList";
    }

    public MCollectionComponent getList() {
        return getCollectionComponentWithWindowID();
    }

    public int getIndex() {
        return index;
    }

    protected boolean oldFormat(String info) {
        try {
            Integer.parseInt(info);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}
