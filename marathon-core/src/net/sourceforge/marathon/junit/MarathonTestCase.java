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
package net.sourceforge.marathon.junit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import junit.framework.Test;
import junit.framework.TestCase;
import net.sourceforge.marathon.Constants;
import net.sourceforge.marathon.api.IConsole;
import net.sourceforge.marathon.api.IMarathonRuntime;
import net.sourceforge.marathon.api.IPlaybackListener;
import net.sourceforge.marathon.api.IPlayer;
import net.sourceforge.marathon.api.IScript;
import net.sourceforge.marathon.api.PlaybackResult;
import net.sourceforge.marathon.api.ScriptModelClientPart;
import net.sourceforge.marathon.api.SourceLine;
import net.sourceforge.marathon.checklist.CheckList;
import net.sourceforge.marathon.checklist.CheckListDialog;
import net.sourceforge.marathon.checklist.CheckListForm;
import net.sourceforge.marathon.checklist.CheckListForm.Mode;
import net.sourceforge.marathon.runtime.JavaRuntimeFactory;
import net.sourceforge.marathon.runtime.JavaRuntimeProfile;
import net.sourceforge.marathon.screencapture.AnnotateScreenCapture;

public class MarathonTestCase extends TestCase implements IPlaybackListener, Test {
    private File file;
    private IMarathonRuntime runtime = null;
    private final Object waitLock = new Object();
    private PlaybackResult result;
    private String suffix;
    private IScript script;
    private ArrayList<CheckList> checkLists = new ArrayList<CheckList>();
    private ArrayList<File> screenCaptures = new ArrayList<File>();
    private boolean acceptChecklist;
    private IConsole console;
    private Properties dataVariables;
    private String nameSuffix = "";

    public MarathonTestCase(File file, boolean acceptChecklist, IConsole console) {
        this(file, null);
        this.acceptChecklist = acceptChecklist;
        this.console = console;
    }

    MarathonTestCase(File file, IMarathonRuntime runtime) {
        this.file = file;
        this.runtime = runtime;
        suffix = ScriptModelClientPart.getModel().getSuffix();
        this.acceptChecklist = false;
    }

    public MarathonTestCase(File file, boolean acceptChecklist, IConsole console, Properties dataVariables, String name) {
        this(file, acceptChecklist, console);
        this.dataVariables = dataVariables;
        this.nameSuffix = name;
    }

    public String getName() {
        String name = file.getName();

        if (name.endsWith(suffix)) {
            name = name.substring(0, name.length() - suffix.length());
        }
        return name + nameSuffix;
    }

    protected synchronized void runTest() throws Throwable {
        checkLists.clear();
        screenCaptures.clear();
        try {
            if (runtime == null) {
                runtime = new JavaRuntimeFactory().createRuntime(new JavaRuntimeProfile(), console);
            }
            script = runtime.createScript(getScriptContents(), file.getAbsolutePath(), false, true);
            if (dataVariables != null)
                script.setDataVariables(dataVariables);
            IPlayer player = script.getPlayer(MarathonTestCase.this, new PlaybackResult());
            player.setAcceptCheckList(acceptChecklist);
            synchronized (waitLock) {
                player.play(true);
                waitLock.wait();
            }
            confirmResult();
        } finally {
            if (runtime != null)
                runtime.destroy();
            runtime = null;
        }
    }

    private String getScriptContents() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        StringWriter sWriter = new StringWriter(8192);
        PrintWriter writer = new PrintWriter(sWriter);
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                writer.println(line);
            }
        } finally {
            writer.close();
            reader.close();
        }
        return sWriter.toString();
    }

    public void playbackFinished(PlaybackResult result, boolean shutdown) {
        this.result = result;
        synchronized (waitLock) {
            waitLock.notify();
        }
    }

    public int lineReached(SourceLine line) {
        return CONTINUE;
    }

    private void confirmResult() {
        if (result.failureCount() == 0)
            return;
        String dirName = System.getProperty(Constants.PROP_IMAGE_CAPTURE_DIR);
        if (dirName != null) {
            File dir = new File(dirName);
            String testPath = dirName + File.separator + this.getName();
            File[] files = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.matches("error[0-9]*.png");
                }
            });
            for (int i = 0; i < files.length; i++) {
                String name = files[i].getName();
                File newFile = new File(testPath + "-" + name);
                files[i].renameTo(newFile);
                addErrorScreenCapture(newFile);
            }
        }
        MarathonAssertion e = new MarathonAssertion(result.failures(), this.getName());
        throw e;
    }

    private void addErrorScreenCapture(File file) {
        screenCaptures.add(file);
    }

    public File getFile() {
        return file;
    }

    public int methodReturned(SourceLine line) {
        return CONTINUE;
    }

    public int methodCalled(SourceLine line) {
        return CONTINUE;
    }

    public int acceptChecklist(final String fileName) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                File file = new File(System.getProperty(Constants.PROP_CHECKLIST_DIR), fileName);
                showAndEnterChecklist(file, runtime, null);
                script.getDebugger().resume();
            }
        });
        return 0;
    }

    public int showChecklist(final String fileName) {
        return 0;
    }

    public ArrayList<CheckList> getChecklists() {
        return checkLists;
    }

    public void addChecklist(CheckList checkList) {
        checkLists.add(checkList);
    }

    public File[] getScreenCaptures() {
        return screenCaptures.toArray(new File[screenCaptures.size()]);
    }

    public void addScreenCapture(File newFile) {
        screenCaptures.add(newFile);
    }

    public CheckList showAndEnterChecklist(File file, final IMarathonRuntime runtime, final JFrame instance) {
        final CheckList checklist;
        try {
            checklist = CheckList.read(new FileInputStream(file));
            CheckListForm checklistForm = new CheckListForm(checklist, Mode.ENTER);
            final CheckListDialog dialog = new CheckListDialog((JFrame) null, checklistForm);

            JButton screenCapture = new JButton("Screen Capture");
            screenCapture.addActionListener(new ActionListener() {
                File captureFile = null;

                public void actionPerformed(ActionEvent e) {
                    boolean iconify = false;
                    int state = -1;
                    try {
                        if (instance != null && instance.getState() != JFrame.ICONIFIED) {
                            iconify = true;
                            state = instance.getState();
                        }
                        if (iconify)
                            instance.setState(JFrame.ICONIFIED);
                        dialog.setVisible(false);
                        if (captureFile == null)
                            captureFile = runtime.getScreenCapture();
                        if (captureFile == null) {
                            JOptionPane.showMessageDialog(null, "Could not create a screen capture");
                            return;
                        }
                        try {
                            AnnotateScreenCapture annotate = new AnnotateScreenCapture(captureFile, true);
                            if (annotate.showDialog() == AnnotateScreenCapture.APPROVE_OPTION) {
                                annotate.saveToFile(captureFile);
                                checklist.setCaptureFile(captureFile.getName());
                            }
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    } finally {
                        if (iconify)
                            instance.setState(state);
                        dialog.setVisible(true);
                    }
                }
            });
            JButton saveButton = new JButton("Save");
            saveButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });
            dialog.setActionButtons(new JButton[] { screenCapture, saveButton });
            dialog.setVisible(true);
        } catch (Exception e1) {
            JOptionPane.showMessageDialog(null, "Unable to read the checklist file");
            return null;
        }
        addChecklist(checklist);
        return checklist;
    }

    @Override public String toString() {
        return getName();
    }

    public void setDataVariables(Properties dataVariables) {
        this.dataVariables = dataVariables;
    }
}
