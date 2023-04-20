/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.it;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.utils.Keyboard;
import com.intellij.remoterobot.utils.RepeatUtilsKt;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import io.openliberty.tools.intellij.it.fixtures.DialogFixture;
import io.openliberty.tools.intellij.it.fixtures.ProjectFrameFixture;
import io.openliberty.tools.intellij.it.fixtures.WelcomeFrameFixture;
import org.assertj.swing.core.MouseButton;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;

import static com.intellij.remoterobot.fixtures.dataExtractor.TextDataPredicatesKt.contains;
import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static java.awt.event.KeyEvent.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * UI helper function.
 */
public class UIBotTestUtils {

    /**
     * Print destination types.
     */
    public enum PrintTo {
        STDOUT, FILE
    }

    /**
     * server.xml Language server insertion types.
     */
    public enum InsertionType {
        FEATURE, ELEMENT
    }

    /**
     * UI Frames.
     */
    public enum Frame {
        WELCOME, PROJECT
    }

    /**
     * Execution mode.
     */
    public enum ExecMode {
        DEBUG, RUN
    }

    /**
     * Imports a project using the UI.
     *
     * @param remoteRobot  The RemoteRobot instance.
     * @param projectsPath The absolute path to the directory containing the projects.
     */
    public static void importProject(RemoteRobot remoteRobot, String projectsPath, String projectName) {
        // Trigger the open project dialog.
        CommonContainerFixture commonFixture = null;
        Frame currentFrame = getCurrentFrame(remoteRobot);
        if (currentFrame == null) {
            fail("Unable to identify the current window frame (i.e. welcome/project)");
        }

        if (currentFrame == Frame.WELCOME) {
            // From the welcome dialog.
            WelcomeFrameFixture welcomePage = remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(10));
            commonFixture = welcomePage;
            ComponentFixture cf = welcomePage.getOpenProjectComponentFixture("Open");
            cf.click();
        } else if (currentFrame == Frame.PROJECT) {
            // From the project frame.
            ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
            commonFixture = projectFrame;
            ComponentFixture fileMenuEntry = projectFrame.getActionMenu("File");
            fileMenuEntry.click();
            ComponentFixture openFixture = projectFrame.getActionMenuItem("Open...");
            openFixture.click(new Point());
        }

        // Specify the project's path. The text field is pre-populated by default.
        DialogFixture newProjectDialog = commonFixture.find(DialogFixture.class, DialogFixture.byTitle("Open File or Project"), Duration.ofSeconds(10));
        JTextFieldFixture textField = newProjectDialog.getBorderLessTextField();
        JButtonFixture okButton = newProjectDialog.getButton("OK");

        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the OK button on the open project dialog to be enabled",
                "The OK button on the open project dialog was not enabled",
                okButton::isEnabled);

        String projectFullPath = Paths.get(projectsPath, projectName).toString();
        textField.setText(projectFullPath);
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the text box on the Open \"File or Project\" dialog to be populated with the given value",
                "The text box on the Open \"File or Project\" dialog was not populated with the given value",
                () -> textField.getText().equals(projectFullPath));

        ComponentFixture projectTree = newProjectDialog.getTree();
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for project tree on the Open \"File or Project\" dialog to show the set project",
                "The project tree on the \"File or Project\" dialog did not show the set project",
                () -> projectTree.getData().hasText(projectName));

        // Click OK.
        okButton.click();

        // If in a project frame, choose where to open the project.
        if (currentFrame == Frame.PROJECT) {
            DialogFixture openProjectDialog = getOpenProjectLocationDialog(commonFixture);
            JButtonFixture thisWinButton = openProjectDialog.getButton("This Window");
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(1),
                    "Waiting for The \"This window\" button on the \"Open Project\" dialog to be enabled",
                    "The \"This window\" button on the \"Open Project\" dialog was not enable",
                    thisWinButton::isEnabled);
            thisWinButton.click();
        }

        // Need a buffer here.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the dialog that queries the user for the location where the project is to be opened.
     *
     * @return The dialog that queries the user for the location where the project is to be opened.
     */
    public static DialogFixture getOpenProjectLocationDialog(CommonContainerFixture baseFixture) {
        DialogFixture openProjectDialog;
        try {
            openProjectDialog = baseFixture.find(DialogFixture.class, DialogFixture.byTitle("New Project"), Duration.ofSeconds(5));
        } catch (WaitForConditionTimeoutException e) {
            openProjectDialog = baseFixture.find(DialogFixture.class, DialogFixture.byTitle("Open Project"), Duration.ofSeconds(5));
        }

        return openProjectDialog;
    }

    /**
     * Closes the project frame.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void closeProjectFrame(RemoteRobot remoteRobot) {
        // Click on File on the Menu bar.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        ComponentFixture fileMenuEntry = projectFrame.getActionMenu("File");
        fileMenuEntry.click();

        // Click on Close Project in the menu.
        ComponentFixture closeFixture = projectFrame.getActionMenuItem("Close Project");
        closeFixture.click();
    }

    /**
     * Runs a Liberty tool window action using the drop-down tree view.
     *
     * @param remoteRobot   The RemoteRobot instance.
     * @param action        The action to run
     * @param usePlayButton The indicator that specifies if play button should be used to run the action or not.
     */
    public static void runLibertyTWActionFromDropDownView(RemoteRobot remoteRobot, String action, boolean usePlayButton) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        // Click on the Liberty toolbar to give it focus.
        ComponentFixture libertyTWBar = projectFrame.getBaseLabel("Liberty", "10");
        libertyTWBar.click();

        // Check if the project tree was expanded and the action is showing.
        ComponentFixture treeFixture = projectFrame.getTree("LibertyTree", action, "60");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(2),
                "Waiting for " + action + " in tree fixture to show and come into focus",
                "Action " + action + " in tree fixture is not showing or not in focus",
                treeFixture::isShowing);

        // Run the action.
        List<RemoteText> rts = treeFixture.findAllText();
        for (RemoteText rt : rts) {
            if (action.equals(rt.getText())) {
                int maxRetries = 3;
                Exception error = null;
                for (int i = 0; i < maxRetries; i++) {
                    try {
                        error = null;
                        if (usePlayButton) {
                            rt.click();
                            clickOnLibertyTWToolbarPlayButton(remoteRobot);
                        } else {
                            rt.doubleClick();
                        }
                        break;
                    } catch (Exception e) {
                        // The content of the Liberty tool window may blink in and out of existence. Retry.
                        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                                "Double click or play button click on Liberty tool window drop down action failed (" + e.getMessage() + "). Retrying...");
                        TestUtils.sleepAndIgnoreException(1);
                        error = e;
                    }
                }

                // Report the last error if there is one.
                if (error != null) {
                    throw new RuntimeException("Unable to run action from dropdown view.", error);
                }

                break;
            }
        }
    }

    /**
     * Runs a Liberty tool window action using the pop-up action menu.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project.
     * @param action      The action to run.
     */
    public static void runLibertyTWActionFromMenuView(RemoteRobot remoteRobot, String projectName, String action) {
        RemoteText project = findProjectInLibertyToolWindow(remoteRobot, projectName, "60");
        project.rightClick();
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture menuAction = projectFrame.getActionMenuItem(action);
        menuAction.click();
    }

    /**
     * Returns the RemoteText object representing the project in the Liberty tool window.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project.
     * @return The RemoteText object representing the project in the Liberty tools window.
     */
    public static RemoteText findProjectInLibertyToolWindow(RemoteRobot remoteRobot, String projectName, String secsToWait) {
        RemoteText projectRootNode = null;
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture treeFixture = projectFrame.getTree("LibertyTree", projectName, secsToWait);
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(2),
                "Waiting for project " + projectName + " to appear in the Liberty tool window",
                "Project " + projectName + " was not found in the Liberty tool window",
                treeFixture::isShowing);

        List<RemoteText> rts = treeFixture.findAllText();
        for (RemoteText rt : rts) {
            if (projectName.equals(rt.getText())) {
                projectRootNode = rt;
                break;
            }
        }

        if (projectRootNode == null) {
            fail("Project " + projectName + " was not found in Liberty tool window.");
        }

        return projectRootNode;
    }

    /**
     * Waits for the specified project tree item to appear in the Liberty tool window.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param treeItem    The project name to wait for.
     */
    public static void validateLibertyTWProjectTreeItemIsShowing(RemoteRobot remoteRobot, String treeItem) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        // There is a window between which the Liberty tool window content may come and
        // go when intellij detects indexing starts and ends. Try to handle it.
        try {
            projectFrame.getTree("LibertyTree", treeItem, "60");
        } catch (Exception e) {
            // Do nothing.
        }

        // Wait a bit and re-try. Indexing is a long process right now.
        TestUtils.sleepAndIgnoreException(10);
        ComponentFixture treeFixture = projectFrame.getTree("LibertyTree", treeItem, "360");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(2),
                "Waiting for Tree fixture to show",
                "Tree fixture is not showing",
                treeFixture::isShowing);
    }

    /**
     * Waits for the Welcome page, which is shown when the project frame is closed.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void validateProjectFrameClosed(RemoteRobot remoteRobot) {
        remoteRobot.find(WelcomeFrameFixture.class, Duration.ofMinutes(2));
    }

    /**
     * Opens the Liberty tool window if it is not already open.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void openLibertyToolWindow(RemoteRobot remoteRobot) {
        int maxRetries = 6;
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                error = null;
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
                projectFrame.getBaseLabel("Liberty", "5");
                break;
            } catch (WaitForConditionTimeoutException wfcte) {
                // The Liberty tool window is closed. Open it.
                clickOnWindowPaneStripeButton(remoteRobot, "Liberty");
                break;
            } catch (Exception e) {
                // The project frame may hang for a bit while loading/processing work. Retry.
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                        "Unable to open the Liberty tool window (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(10);
                error = e;
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to open the Liberty tool window.", error);
        }
    }

    /**
     * Closes the Liberty tool window if it is not already closed.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void closeLibertyToolWindow(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        try {
            projectFrame.getBaseLabel("Liberty", "2");
            clickOnWindowPaneStripeButton(remoteRobot, "Liberty");
        } catch (WaitForConditionTimeoutException e) {
            // The Liberty tool window is already closed. Nothing to do.
        }
    }

    /**
     * Opens the project tree view if it is not already opened.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void openProjectView(RemoteRobot remoteRobot) {
        int maxRetries = 6;
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                error = null;
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
                projectFrame.getContentComboLabel("Project", "5");
                break;
            } catch (WaitForConditionTimeoutException wfcte) {
                // The project view is closed. Open it.
                clickOnWindowPaneStripeButton(remoteRobot, "Project");
                break;
            } catch (Exception e) {
                // The project frame may hang for a bit while loading/processing work. Retry.
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                        "Unable to open the project tool window (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(10);
                error = e;
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to open the project tool window.", error);
        }
    }

    /**
     * Closes the project tree view if it is not already closed.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void closeProjectView(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        try {
            projectFrame.getContentComboLabel("Project", "2");
            clickOnWindowPaneStripeButton(remoteRobot, "Project");
        } catch (WaitForConditionTimeoutException e) {
            // Project view is already closed. Nothing to do.
        }
    }

    /**
     * Opens the terminal window if it is not already open.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void openTerminalWindow(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(5));
        try {
            projectFrame.getBaseLabel("Terminal", "5");
        } catch (WaitForConditionTimeoutException e) {
            // The Liberty tool window is closed. Open it.
            clickOnWindowPaneStripeButton(remoteRobot, "Terminal");
        }
    }

    /**
     * Clicks on the specified tool window pane stripe.
     *
     * @param remoteRobot      The RemoteRobot instance.
     * @param StripeButtonName The name of the window pane stripe button.
     */
    public static void clickOnWindowPaneStripeButton(RemoteRobot remoteRobot, String StripeButtonName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture wpStripe = projectFrame.getStripeButton(StripeButtonName);
        wpStripe.click();
    }

    /**
     * Clicks on the expand action button on the Liberty tool window.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void expandLibertyToolWindowProjectTree(RemoteRobot remoteRobot, String projectName) {
        int maxRetries = 6;
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
                error = null;

                // Click on the Liberty tool window toolbar to give it focus.
                ComponentFixture LibertyTWBar = projectFrame.getBaseLabel("Liberty", "10");
                LibertyTWBar.click();

                // Expand the project tree to show the available actions.
                String xPath = "//div[@class='LibertyExplorer']//div[@class='ActionButton' and contains(@myaction.key, 'action.ExpandAll.text')]";
                ComponentFixture actionButton = projectFrame.getActionButton(xPath, "10");
                actionButton.click();

                // Click on the project node to give it focus. This action opens the editor tab showing
                // the build file.
                RemoteText projectRootNode = findProjectInLibertyToolWindow(remoteRobot, projectName, "10");
                projectRootNode.click();
                break;
            } catch (Exception e) {
                // The Liberty tool window content may blink in and out or hang. Retry.
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                        "Unable to expand the Liberty tool window project tree (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(10);
                error = e;
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to expand the Liberty tool window project tree.", error);
        }
    }

    /**
     * Closes the Project Tree for a given appName
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param appName     The Name of the application in tree to close
     */
    public static void closeProjectViewTree(RemoteRobot remoteRobot, String appName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        ComponentFixture appNameEntry = projectFrame.getProjectViewTree(appName);
        appNameEntry.findText(appName).doubleClick();
    }

    public static void openConfigFile(RemoteRobot remoteRobot, String projectName, String fileName) {
        // Click on File on the Menu bar.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));

        // hide the terminal window for now
        UIBotTestUtils.hideTerminalWindow(remoteRobot);

        // get a keyboard - should maybe make this class wide
        Keyboard keyboard = new Keyboard(remoteRobot);

        // get a JTreeFixture reference to the file project viewer entry
        JTreeFixture projTree = projectFrame.getProjectViewJTree(projectName);
        projTree.expand(projectName, "src", "main", "liberty", "config");

        projTree.findText(fileName).doubleClick();

    }

    public static void hideTerminalWindow(RemoteRobot remoteRobot) {
        try {
            ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(2));
            String xPath = "//div[@class='ToolWindowHeader'][.//div[@myaction.key='action.NewPredefinedSession.label']]//div[@myaction.key='tool.window.hide.action.name']";
            ComponentFixture hideActionButton = projectFrame.getActionButton(xPath, "10");
            hideActionButton.click();
        } catch (WaitForConditionTimeoutException e) {
            // not open, nothing to do, so proceed
        }
    }

    /**
     * Returns the editor tab close button for the specified editor tab name or null if one is not found within the
     * specified wait time.
     *
     * @param remoteRobot   The RemoteRobot instance.
     * @param editorTabName The name of the editor tab to close.
     * @param timeToWait    The time to wait for the editor tab close button.
     * @return Returns the editor tab close button for the specified editor tab name or null if one is not found within the
     * * specified wait time.
     */
    public static ComponentFixture getEditorTabCloseButton(RemoteRobot remoteRobot, String editorTabName, String timeToWait) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture editorTabCloseButton = null;
        try {
            editorTabCloseButton = projectFrame.getInplaceButton(editorTabName, timeToWait);
        } catch (WaitForConditionTimeoutException e) {
            // The editor is most likely closed.
        }

        return editorTabCloseButton;
    }

    /**
     * Closes a file that is open in the editor pane.
     *
     * @param remoteRobot   The RemoteRobot instance.
     * @param editorTabName The name of the editor tab to close.
     * @param timeToWait    The time to wait for the editor tab close button.
     */
    public static void closeFileEditorTab(RemoteRobot remoteRobot, String editorTabName, String timeToWait) {
        ComponentFixture editorTabCloseButton = getEditorTabCloseButton(remoteRobot, editorTabName, timeToWait);
        if (editorTabCloseButton != null) {
            editorTabCloseButton.click();

            // Wait until the tab closes.
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(1),
                    "Waiting Editor tab " + editorTabName + " to close.",
                    "Editor tab " + editorTabName + " did not close.",
                    () -> getEditorTabCloseButton(remoteRobot, editorTabName, "1") == null);
        }
    }

    /**
     * Closes all opened editor tabs.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void closeAllEditorTabs(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture windowMenuEntry = projectFrame.getActionMenu("Window");
        windowMenuEntry.click();

        // Click on Editor Tabs in the menu.
        ComponentFixture editorTabsFixture = projectFrame.getChildActionMenu("Window", "Editor Tabs");
        editorTabsFixture.click();

        // Click on Close Project in the menu.
        ComponentFixture closeAllTabsFixture = projectFrame.getChildActionMenuItem("Window", "Close All Tabs");
        if (closeAllTabsFixture.callJs("component.isEnabled();", false)) {
            closeAllTabsFixture.click();
        }
    }

    /**
     * Click on a editor file tab for a file that is open in the editor pane to gain focus to that file
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param fileName    The string file name
     */

    public static void clickOnFileTab(RemoteRobot remoteRobot, String fileName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        try {
            String xPath = "//div[@accessiblename='" + fileName + "' and @class='SimpleColoredComponent']";
            ComponentFixture actionButton = projectFrame.getActionButton(xPath, "10");
            actionButton.click();

        } catch (WaitForConditionTimeoutException e) {
            // file not open, nothing to do
        }
    }


    /**
     * Moves the mouse cursor to a specific string target in server.xml
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param hoverTarget The string to hover over in server.xml
     */
    public static void hoverInAppServerCfgFile(RemoteRobot remoteRobot, String hoverTarget, String hoverFile) {

        Keyboard keyboard = new Keyboard(remoteRobot);

        Locator locator = byXpath("//div[@class='EditorWindowTopComponent']//div[@class='EditorComponentImpl']");
        clickOnFileTab(remoteRobot, hoverFile);
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, locator, Duration.ofSeconds(20));

        Exception error = null;
        for (int i = 0; i < 3; i++) {
            error = null;
            try {
                // move the cursor to the origin of the editor
                goToLineAndColumn(remoteRobot, keyboard, 1, 1);

                // Find the target text on the editor and move the move to it.
                editorNew.findText(contains(hoverTarget)).moveMouse();
                // clear and "lightbulb" icons?
                keyboard.hotKey(VK_ESCAPE);

                // jitter the cursor
                Point p = editorNew.findText(contains(hoverTarget)).getPoint();

                // provoke the hint popup with a cursor jitter
                jitterCursor(editorNew, p.x, p.y);

                // first get the contents of the popup - put in a String
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
                ContainerFixture popup = projectFrame.getDocumentationHintEditorPane();
                List<RemoteText> rts = popup.findAllText();
                StringBuilder remoteString = new StringBuilder();
                for (RemoteText rt : rts) {
                    remoteString.append(rt.getText());
                }

                // Check for "Fetching Documentation" message indicating there is a delay in getting hint
                // allow some time for the LS hint to appear in the popup
                if (remoteString.toString().contains("Fetching Documentation")) {
                    TestUtils.sleepAndIgnoreException(2);
                }

                break;
            } catch (WaitForConditionTimeoutException wftoe) {
                error = wftoe;
                TestUtils.sleepAndIgnoreException(2);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Hover on text: " + hoverTarget + " did not trigger a pop-up window to open", error);
        }
    }

    public static void jitterCursor(EditorFixture editor, int pointX, int pointY) {

        String jitterScript = "const x = %d;" +
                "const y = %d;" +
                "java.util.List.of(5, 20, 5, 15).forEach((i)=> {" +
                "const point = new Point(x + i, y);" +
                "robot.moveMouse(component, point);})";

        // run the jitter mouse script remotely in the idea
        editor.runJs(String.format(jitterScript, pointX, pointY));
    }

    public static void insertConfigIntoConfigFile(RemoteRobot remoteRobot, String projectName, String fileName, String envCfgNameSnippet, String envCfgNameChooserSnippet, String envCfgValueSnippet) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        clickOnFileTab(remoteRobot, fileName);
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, EditorFixture.Companion.getLocator());
        editorNew.click();

        Keyboard keyboard = new Keyboard(remoteRobot);
        // find the location in the file to begin the stanza insertion
        // since we know this is a new empty file, go to position 1,1
        goToLineAndColumn(remoteRobot, keyboard, 1, 1);
        keyboard.enter();
        goToLineAndColumn(remoteRobot, keyboard, 1, 1);

        keyboard.enterText(envCfgNameSnippet);

        // Select the appropriate completion suggestion in the pop-up window that is automatically
        // opened as text is typed. Avoid hitting ctrl + space as it has the side effect of selecting
        // and entry automatically if the completion suggestion windows has one entry only.
        ComponentFixture namePopupWindow = projectFrame.getLookupList();
        RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                Duration.ofSeconds(1),
                "Waiting for text " + envCfgNameSnippet + " to appear in the completion suggestion pop-up window",
                "Text " + envCfgNameSnippet + " did not appear in the completion suggestion pop-up window",
                () -> namePopupWindow.hasText(envCfgNameSnippet));

        namePopupWindow.findText(envCfgNameSnippet).doubleClick();

        keyboard.hotKey(VK_END);
        keyboard.enterText("=");
        keyboard.hotKey(VK_END);

        keyboard.enterText(envCfgValueSnippet);

        // Select the appropriate completion suggestion in the pop-up window that is automatically
        // opened as text is typed. Avoid hitting ctrl + space as it has the side effect of selecting
        // and entry automatically if the completion suggestion windows has one entry only.
        ComponentFixture valuePopupWindow = projectFrame.getLookupList();
        RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                Duration.ofSeconds(1),
                "Waiting for text " + envCfgNameChooserSnippet + " to appear in the completion suggestion pop-up window",
                "Text " + envCfgNameSnippet + " did not appear in the completion suggestion pop-up window",
                () -> valuePopupWindow.hasText(envCfgValueSnippet));

        valuePopupWindow.findText(envCfgValueSnippet).doubleClick();

        // let the auto-save function of intellij save the file before testing it
        if (remoteRobot.isMac()) {
            keyboard.hotKey(VK_META, VK_S);
        } else {
            // linux + windows
            keyboard.hotKey(VK_CONTROL, VK_S);
        }
    }

    /**
     * Inserts content to server.xml. Callers are required to have done a UI copy of the server.xml
     * content prior to calling this method.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void insertStanzaInAppServerXML(RemoteRobot remoteRobot, String stanzaSnippet, int line, int col, InsertionType type) {

        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        clickOnFileTab(remoteRobot, "server.xml");
        Locator locator = byXpath("//div[@class='EditorWindowTopComponent']//div[@class='EditorComponentImpl']");
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, locator, Duration.ofSeconds(20));
        editorNew.click();

        Keyboard keyboard = new Keyboard(remoteRobot);
        Exception error = null;

        for (int i = 0; i < 3; i++) {
            error = null;
            try {
                // find the location in the file to begin the stanza insertion
                goToLineAndColumn(remoteRobot, keyboard, line, col);

                if (type.name().equals("FEATURE")) {
                    String textToFind = "feature";
                    // if this is a feature stanza, hit enter to place it on the next line
                    // in the featureManager Block
                    keyboard.hotKey(VK_ENTER);

                    // Add the feature element using completion.
                    goToLineAndColumn(remoteRobot, keyboard, line + 1, col);
                    keyboard.hotKey(VK_CONTROL, VK_SPACE);

                    ContainerFixture popupWindow = projectFrame.getLookupList();
                    RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                            Duration.ofSeconds(1),
                            "Waiting for text " + textToFind + " to appear in the completion suggestion pop-up window",
                            "Text " + textToFind + " did not appear in the completion suggestion pop-up window",
                            () -> popupWindow.hasText(textToFind));

                    popupWindow.findText(textToFind).doubleClick();

                    // get the full text of the editor contents
                    String editorText = editorNew.getText();

                    // find the location to click on for feature name entry
                    // this should put the entry cursor directly between <feature> and <feature/>
                    int offset = editorText.indexOf("<feature></feature>") + "<feature>".length();

                    editorNew.clickOnOffset(offset, MouseButton.LEFT_BUTTON, 1);

                    // small delay to allow the button to be fully clicked before typing
                    TestUtils.sleepAndIgnoreException(1);
                }

                // For either a FEATURE or a CONFIG stanza, insert where the cursor is currently located.
                keyboard.enterText(stanzaSnippet);
                TestUtils.sleepAndIgnoreException(2);

                // Select the appropriate completion suggestion in the pop-up window that is automatically
                // opened as text is typed. Avoid hitting ctrl + space as it has the side effect of selecting
                // and entry automatically if the completion suggestion windows has one entry only.
                ComponentFixture completionPopupWindow = projectFrame.getLookupList();
                RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                        Duration.ofSeconds(1),
                        "Waiting for text " + stanzaSnippet + " to appear in the completion suggestion pop-up window",
                        "Text " + stanzaSnippet + " did not appear in the completion suggestion pop-up window",
                        () -> completionPopupWindow.hasText(stanzaSnippet));

                completionPopupWindow.findText(stanzaSnippet).doubleClick();

                // Save the file.
                if (remoteRobot.isMac()) {
                    keyboard.hotKey(VK_META, VK_S);
                } else {
                    // linux + windows
                    keyboard.hotKey(VK_CONTROL, VK_S);
                }

                break;
            } catch (WaitForConditionTimeoutException wftoe) {
                error = wftoe;

                // The server.xml content may have been corrupted in the process. Replace it before re-trying it.
                UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
                TestUtils.sleepAndIgnoreException(2);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to insert entry in server.xml using text: " + stanzaSnippet, error);
        }

        TestUtils.sleepAndIgnoreException(5);
    }

    /**
     * Places the cursor at an exact location for text entry in file
     *
     * @param remoteRobot the remote robot instance
     * @param keyboard    keyboard to interact with
     * @param line        target line number
     * @param column      target column number
     */
    public static void goToLineAndColumn(RemoteRobot remoteRobot, Keyboard keyboard, int line, int column) {
        // trigger the line:col popup window to place cursor at exact location in file
        if (remoteRobot.isMac())
            keyboard.hotKey(KeyEvent.VK_META, KeyEvent.VK_L);
        else
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_G);
        keyboard.enterText(line + ":" + column);
        keyboard.enter();
    }

    /**
     * Validates the expected hover string message was raised in popup.
     *
     * @param remoteRobot the remote robot instance
     */
    public static String getHoverStringData(RemoteRobot remoteRobot) {
        // get the text from the LS diagnostic hint popup
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        ContainerFixture popup = projectFrame.getDocumentationHintEditorPane();
        List<RemoteText> rts = popup.findAllText();

        // print out the string data found in the popup window - for debugging
        popup.findAllText().forEach((it) -> System.out.println(it.getText()));

        StringBuilder popupString = new StringBuilder();
        for (RemoteText rt : rts) {
            popupString.append(rt.getText());
        }

        return popupString.toString();
    }

    /**
     * Copies the contents from the currently active window.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void copyWindowContent(RemoteRobot remoteRobot) {
        // Select the content.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        ComponentFixture editMenuEntry = projectFrame.getActionMenu("Edit");
        editMenuEntry.click();
        ComponentFixture slectAllEntry = projectFrame.getActionMenuItem("Select All");
        slectAllEntry.click();

        // Copy the content.
        editMenuEntry.click();
        ComponentFixture copyEntry = projectFrame.getActionMenuItem("Copy");
        copyEntry.click();
    }

    /**
     * Pastes previously copied content on the currently active window.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void pasteOnActiveWindow(RemoteRobot remoteRobot) {
        // Select the content.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        ComponentFixture editMenuEntry = projectFrame.getActionMenu("Edit");
        editMenuEntry.click();
        ComponentFixture selectAllEntry = projectFrame.getActionMenuItem("Select All");
        selectAllEntry.click();

        // Paste the content.
        editMenuEntry = projectFrame.getActionMenu("Edit");
        editMenuEntry.click();
        ComponentFixture pasteFixture = projectFrame.getChildActionMenu("Edit", "Paste");
        pasteFixture.click();
        ComponentFixture pasteChildEntry = projectFrame.getChildActionMenuItem("Edit", "Paste");
        pasteChildEntry.click();

        // Save.
        ComponentFixture fileMenuEntry = projectFrame.getActionMenu("File");
        fileMenuEntry.click();
        ComponentFixture saveAllEntry = projectFrame.getActionMenuItem("Save All");
        saveAllEntry.click();

    }

    /**
     * Runs the start parameters run configuration dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param startParams The parameters to set in the configuration dialog.
     */
    public static void runStartParamsConfigDialog(RemoteRobot remoteRobot, String startParams) {
        DialogFixture dialog = remoteRobot.find(DialogFixture.class, Duration.ofSeconds(10));
        if (startParams != null) {
            // Update the parameter editor box.
            // TODO: Investigate this further:
            // Currently blocked by the dialog editor box behind the start parameter text box
            // holding the editor's write lock.
            // One can only write when the dialog is being closed because the write lock
            // is released at that point.

            // Save the changes made.
            JButtonFixture applyButton = dialog.getButton("Apply");
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(1),
                    "Waiting for the Apply button on the open project dialog to be enabled",
                    "The Apply button on the open project dialog to be enabled",
                    applyButton::isEnabled);
            applyButton.click();
        }

        // Run the configuration.
        JButtonFixture runButton = dialog.getButton("Run");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the Run button on the open project dialog to be enabled",
                "The run button on the open project dialog to be enabled",
                runButton::isEnabled);
        runButton.click();
    }

    /**
     * Returns the name of the opened Liberty configuration.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @return The name of the opened Liberty configuration.
     */
    public static final String getLibertyConfigName(RemoteRobot remoteRobot) {
        // Get a hold of the Liberty Edit Configurations window.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        DialogFixture startParmsDialog = projectFrame.find(DialogFixture.class, DialogFixture.byTitle("Edit Configuration"), Duration.ofSeconds(10));
        Locator locator = byXpath("//div[@class='JTextField']");

        // Get the name of the configuration.
        JTextFieldFixture nameTextField = startParmsDialog.textField(locator, Duration.ofSeconds(10));
        RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                Duration.ofSeconds(1),
                "Waiting for the name text field on the Liberty config dialog to be enabled or populated by default",
                "The name text field on the Liberty config dialog was not enabled or populated by default",
                () -> nameTextField.isEnabled() && (nameTextField.getText().length() != 0));
        String configName = nameTextField.getText();

        // Close the configuration.
        JButtonFixture cancelButton = startParmsDialog.getButton("Cancel");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the cancel button on the Liberty config dialog to be enabled",
                "The cancel button on the Liberty config dialog was not enabled",
                cancelButton::isEnabled);
        cancelButton.click();

        return configName;
    }

    /**
     * Clicks on the play action button located on the Liberty tool window toolbar.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void clickOnLibertyTWToolbarPlayButton(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        // Click on the play button.
        String xPath = "//div[@class='LibertyExplorer']//div[@class='ActionButton' and @tooltiptext.key='action.io.openliberty.tools.intellij.actions.RunLibertyDevTask.text']";
        ComponentFixture actionButton = projectFrame.getActionButton(xPath, "10");
        actionButton.click();
    }

    /**
     * Prints the UI Component hierarchy to the specified destination.
     *
     * @param printTo       The indicator that specifies where the output should go: FILE or STDOUT.
     * @param secondsToWait The seconds to wait before output is collected.
     */
    public static void printUIComponentHierarchy(PrintTo printTo, int secondsToWait) {
        try {
            if (secondsToWait > 0) {
                System.out.println("!!! MARKER: The output will be collected in " + secondsToWait + " seconds. !!!");
                Thread.sleep(secondsToWait * 1000L);
            }

            URL url = new URL(MavenSingleModProjectTest.REMOTE_BOT_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                try (FileWriter fw = new FileWriter("botCompHierarchy.html")) {
                    String inputLine;
                    while ((inputLine = br.readLine()) != null) {
                        switch (printTo) {
                            case STDOUT -> System.out.println(inputLine);
                            case FILE -> {
                                fw.write(inputLine);
                                fw.write("\n");
                            }
                            default -> Assert.fail("Invalid format to write : ");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Failed to collect UI Component Hierarchy information: " + e.getCause());
        }
    }

    /**
     * Opens the search everywhere dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void runActionFromSearchEverywherePanel(RemoteRobot remoteRobot, String action) {
        // Click on Navigate on the Menu bar.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        ComponentFixture navigateMenuEntry = projectFrame.getActionMenu("Navigate");
        navigateMenuEntry.click();

        // Click on Search Everywhere in the menu.
        ComponentFixture searchFixture = projectFrame.getActionMenuItem("Search Everywhere");
        searchFixture.click();

        // Click on the Actions tab
        ComponentFixture actionsTabFixture = projectFrame.getSETabLabel("Actions");
        actionsTabFixture.click();

        // Type the search string in the search dialog box.
        JTextFieldFixture searchField = projectFrame.textField(JTextFieldFixture.Companion.byType(), Duration.ofSeconds(10));
        searchField.click();
        searchField.setText(action);

        // Wait for the desired action to show in the search output frame and click on it.
        RepeatUtilsKt.waitFor(Duration.ofSeconds(20),
                Duration.ofSeconds(1),
                "Waiting for the search to filter and show " + action + " in search output",
                "The search did not filter or show " + action + " in the search output",
                () -> findTextInListOutputPanel(projectFrame, action) != null);

        RemoteText foundAction = findTextInListOutputPanel(projectFrame, action);
        if (foundAction != null) {
            foundAction.click();
        } else {
            fail("Search everywhere found " + action + ", but it can no longer be found after a subsequent attempt to find it.");
        }
    }

    /**
     * Selects the specified project from the list of detected projects shown in the 'Add Liberty project'
     * dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project to select.
     */
    public static void selectProjectFromAddLibertyProjectDialog(RemoteRobot remoteRobot, String projectName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        DialogFixture addProjectDialog = projectFrame.find(DialogFixture.class,
                DialogFixture.byTitle("Add Liberty project"),
                Duration.ofSeconds(10));
        JButtonFixture jbf = addProjectDialog.getBasicArrowButton();
        jbf.click();

        RemoteText remoteProject = findTextInListOutputPanel(addProjectDialog, projectName);
        if (remoteProject != null) {
            remoteProject.click();
        } else {
            fail("Unable to find " + projectName + " in the output list of the Add Liberty project dialog.");
        }

        JButtonFixture okButton = addProjectDialog.getButton("OK");
        okButton.click();
    }

    /**
     * Selects the specified project from the list of detected projects shown in the 'Remove Liberty project'
     * dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project to select.
     */
    public static void selectProjectFromRemoveLibertyProjectDialog(RemoteRobot remoteRobot, String projectName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        DialogFixture removeProjectDialog = projectFrame.find(DialogFixture.class,
                DialogFixture.byTitle("Remove Liberty project"),
                Duration.ofSeconds(10));
        removeProjectDialog.getBasicArrowButton().click();

        RemoteText remoteProject = findTextInListOutputPanel(removeProjectDialog, projectName);
        if (remoteProject != null) {
            remoteProject.click();
        } else {
            fail("Unable to find " + projectName + " in the output list of the Remove Liberty project dialog.");
        }

        JButtonFixture okButton = removeProjectDialog.getButton("OK");
        okButton.click();
    }

    /**
     * Waits the "No Liberty Maven or Liberty Gradle projects detected in this workspace" text
     * in the Liberty tool window. This text comes from Liberty Tools.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param waitTime    The time to wait for the required message. In seconds.
     */
    public static void waitForLTWNoProjectDetectedMsg(RemoteRobot remoteRobot, String waitTime) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        String text = "No Liberty Maven or Liberty Gradle projects detected in this workspace.";
        ComponentFixture textArea = projectFrame.getTextArea(waitTime);

        RepeatUtilsKt.waitFor(Duration.ofSeconds(60),
                Duration.ofSeconds(1),
                "Waiting for text " + text + " to appear in Liberty tool window",
                "Text " + text + " did not appear in Liberty tool window",
                () -> readAllText(textArea).equals(text));
    }

    /**
     * Returns a concatenated string of all text found in a ComponentFixture object.
     *
     * @param componentFixture The ComponentFixture object.
     * @return A concatenated string of all text found in a ComponentFixture object.
     */
    public static String readAllText(ComponentFixture componentFixture) {
        List<RemoteText> lines = componentFixture.findAllText();
        StringBuilder fullText = new StringBuilder();
        for (RemoteText line : lines) {
            fullText.append(line.getText());
        }

        return fullText.toString();
    }

    /**
     * Responds to the `Remove Liberty project` dialog query asking if the project should be deleted.
     * The response is in the affirmative.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void respondToRemoveProjectQueryDialog(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        DialogFixture removeProjectDialog = projectFrame.find(DialogFixture.class,
                DialogFixture.byTitle("Remove Liberty project"),
                Duration.ofSeconds(10));
        JButtonFixture okButton = removeProjectDialog.getButton("Yes");
        okButton.click();
    }

    /**
     * Creates a new Liberty configuration.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param cfgName     The name of the new configuration.
     */
    public static void createLibertyConfiguration(RemoteRobot remoteRobot, String cfgName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture runMenu = projectFrame.getActionMenu("Run");
        runMenu.click();
        ComponentFixture editCfgsMenuEntry = projectFrame.getActionMenuItem("Edit Configurations...");
        editCfgsMenuEntry.click();

        // Find the Run/Debug Configurations dialog.
        DialogFixture addProjectDialog = projectFrame.find(DialogFixture.class,
                DialogFixture.byTitle("Run/Debug Configurations"),
                Duration.ofSeconds(10));

        // Click on the Add Configuration action button (+) to open the Add New Configuration window.
        Locator addButtonLocator = byXpath("//div[@tooltiptext.key='add.new.run.configuration.action2.name']");
        ActionButtonFixture addCfgButton = addProjectDialog.actionButton(addButtonLocator);
        addCfgButton.click();

        // Look for the Liberty entry in the Add New configuration window and  create a new configuration.
        ComponentFixture pluginCfgTree = addProjectDialog.getMyTree();
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(2),
                "Waiting for plugin config tree to show on the screen",
                "Plugin config tree is not showing on the screen",
                () -> pluginCfgTree.isShowing());

        List<RemoteText> rts = pluginCfgTree.findAllText();
        for (RemoteText rt : rts) {
            if (rt.getText().equals("Liberty")) {
                rt.click();
                break;
            }
        }

        // The Run/Debug Configurations dialog should now contain the Liberty configuration that was
        // just created. Refresh it.
        addProjectDialog = projectFrame.find(DialogFixture.class,
                DialogFixture.byTitle("Run/Debug Configurations"),
                Duration.ofSeconds(10));

        // Find the new configuration's name text field and give it a name.
        Locator locator = byXpath("//div[@class='JTextField']");
        JTextFieldFixture nameTextField = addProjectDialog.textField(locator, Duration.ofSeconds(10));
        RepeatUtilsKt.waitFor(Duration.ofSeconds(30),
                Duration.ofSeconds(1),
                "Waiting for the name text field to be enabled or populated by default",
                "The name text field was not enabled or populated by default",
                () -> nameTextField.isEnabled() && (nameTextField.getText().length() != 0));

        nameTextField.click();
        nameTextField.setText(cfgName);

        RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                Duration.ofSeconds(1),
                "Waiting for the name of the config to appear in the text box",
                "The name of the config did not appear in the text box ",
                () -> nameTextField.getText().equals(cfgName));

        // Save the new configuration by clicking on the Apply button.
        JButtonFixture applyButton = addProjectDialog.getButton("Apply");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the Apply button on the add config project dialog to be enabled",
                "The Apply button on the add config dialog was not enabled",
                applyButton::isEnabled);
        applyButton.click();

        // Exit the Run/Debug Configurations dialog.
        JButtonFixture okButton = addProjectDialog.getButton("OK");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the OK button on the config dialog to be enabled",
                "The OK button on the config dialog was not enabled",
                okButton::isEnabled);
        okButton.click();
    }

    /**
     * Selects the liberty configuration using the Run/Debug configuration selection box on
     * the project frame toolbar.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param cfgName     The configuration name to select.
     */
    public static void selectConfigUsingToolbar(RemoteRobot remoteRobot, String cfgName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture cfgSelectBox = projectFrame.getRunConfigurationsComboBoxButton();
        cfgSelectBox.click();
        ComponentFixture cfgSelectPaneList = projectFrame.getMyList();
        List<RemoteText> configs = cfgSelectPaneList.getData().getAll();
        for (RemoteText cfg : configs) {
            if (cfg.getText().equals(cfgName)) {
                cfg.click();
                break;
            }
        }
    }

    /**
     * Selects the specified configuration using the Run->Run.../Debug... menu options.
     * The configuration is chosen from panel that lists the available configurations.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param cfgName     The configuration name
     * @param execMode    The execution mode (RUN/DEBUG).
     */
    public static void selectConfigUsingMenu(RemoteRobot remoteRobot, String cfgName, ExecMode execMode) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture menuOption = projectFrame.getActionMenu("Run");
        menuOption.click();
        ComponentFixture menuCfgExecOption = projectFrame.getActionMenuItem("Run...");
        if (execMode == ExecMode.DEBUG) {
            menuCfgExecOption = projectFrame.getActionMenuItem("Debug...");
        }

        menuCfgExecOption.click();

        // Retrieve the list of configs from the config list window.
        ComponentFixture cfgSelectWindow = projectFrame.getMyList();
        List<RemoteText> configs = cfgSelectWindow.getData().getAll();

        // Open the specified configuration.
        for (RemoteText cfg : configs) {
            if (cfg.getText().equals(cfgName)) {
                cfg.click();
                break;
            }
        }
    }

    /**
     * Executes the configurations using the icon on the project frame toolbar.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param execMode    The execution mode (RUN/DEBUG).
     */
    public static void runConfigUsingIconOnToolbar(RemoteRobot remoteRobot, ExecMode execMode) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        Locator locator = byXpath("//div[contains(@myaction.key, 'group.RunMenu.text')]");
        if (execMode == ExecMode.DEBUG) {
            locator = byXpath("//div[@myicon='startDebugger.svg']");
        }

        ActionButtonFixture iconButton = projectFrame.actionButton(locator, Duration.ofSeconds(10));
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the start debugger button on the project frame toolbar to be enabled",
                "The start debugger button on the project frame toolbar was not enabled",
                iconButton::isEnabled);

        iconButton.click();
    }

    /**
     * Deletes all Liberty tool plugin configurations using the Run->EditConfigurations... dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void deleteLibertyRunConfigurations(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture runMenu = projectFrame.getActionMenu("Run");
        runMenu.click();
        ComponentFixture editCfgsMenuEntry = projectFrame.getActionMenuItem("Edit Configurations...");
        editCfgsMenuEntry.click();

        // Find the Run/Debug Configurations dialog.
        DialogFixture rdConfigDialog = projectFrame.find(DialogFixture.class,
                DialogFixture.byTitle("Run/Debug Configurations"),
                Duration.ofSeconds(10));

        // Expand and retrieve configuration tree content.
        JTreeFixture configTree = rdConfigDialog.jTree();
        configTree.expandAll();
        List<RemoteText> treeEntries = configTree.findAllText();

        // Find the Liberty tree node and delete all child configurations.
        boolean processEntries = false;
        boolean firstEntryClicked = false;
        Locator locator = byXpath("//div[@tooltiptext.key='remove.run.configuration.action.name']");
        ActionButtonFixture removeCfgButton = rdConfigDialog.actionButton(locator);

        for (RemoteText treeEntry : treeEntries) {
            if (treeEntry.getText().equals("Liberty")) {
                processEntries = true;
                continue;
            }
            if (processEntries) {
                // Click on the first configuration in the tree only.
                if (!firstEntryClicked) {
                    treeEntry.click();
                    firstEntryClicked = true;
                }

                // Delete the configuration.
                try {
                    RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                            Duration.ofSeconds(1),
                            "Waiting for the remove config button on the config dialog to be enabled",
                            "The remove config button on the config dialog was not enabled",
                            removeCfgButton::isEnabled);
                    removeCfgButton.click();
                } catch (WaitForConditionTimeoutException wfcte) {
                    // We have reached the end of the Liberty entries.
                    break;
                }
            }
        }

        // Press Save the changes.
        JButtonFixture okButton = rdConfigDialog.getButton("OK");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the OK button on the config dialog to be enabled",
                "The OK button on the config dialog was not enabled",
                okButton::isEnabled);
        okButton.click();
    }

    /**
     * Stops the debugger if it is running.
     *
     * @param remoteRobot
     */
    public static void stopDebugger(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        try {
            projectFrame.getBaseLabel("Debug:", "5");
        } catch (WaitForConditionTimeoutException wfcte) {
            // The debug tab is not opened for some reason. Open it.
            ComponentFixture debugStripe = projectFrame.getStripeButton("Debug");
            debugStripe.click();
        }

        Locator locator = byXpath("//div[contains(@myvisibleactions, 'Get')]//div[contains(@myaction.key, 'action.Stop.text')]");
        ActionButtonFixture stopButton = projectFrame.actionButton(locator, Duration.ofSeconds(5));
        stopButton.click();
    }

    /**
     * Returns the RemoteText object representing the text found in the list panel, or
     * null if the text was not found.
     *
     * @param fixture The CommonContainerFixture under which the search is taking place.
     * @param text    The text to search.
     * @return The RemoteText object representing the text found in the list panel, or
     * null if the text was not found.
     */
    public static RemoteText findTextInListOutputPanel(CommonContainerFixture fixture, String text) {
        RemoteText foundText = null;

        List<JListFixture> searchLists = fixture.jLists(JListFixture.Companion.byType());
        if (!searchLists.isEmpty()) {
            JListFixture searchList = searchLists.get(0);
            try {
                List<RemoteText> entries = searchList.findAllText();
                for (RemoteText entry : entries) {
                    if (entry.getText().equals(text)) {
                        foundText = entry;
                    }
                }
            } catch (NoSuchElementException nsee) {
                // The list is empty.
                return null;
            }
        }

        return foundText;
    }

    /**
     * Stops the Liberty server by running the action from the Liberty tool window dropdown action list.
     *
     * @param remoteRobot     The RemoteRobot instance.
     * @param testName        The name of the test calling this method.
     * @param absoluteWLPPath The absolute path of the Liberty installation.
     * @param maxRetries      The maximum amount of attempts to try to stop the server.
     */
    public static void stopLibertyServerUsingLTWDropdownActions(RemoteRobot remoteRobot, String testName, String absoluteWLPPath, int maxRetries) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                // Stop dev mode.
                UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Stop", false);

                // Validate that the server stopped. Fail the test if the last iteration fails.
                boolean failOnError = (i == (maxRetries - 1));
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath, 12, failOnError);

                break;
            } catch (Exception e) {
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "Retrying server stop. Cause: " + e.getMessage());
            }
        }
    }

    /**
     * Returns the frame type that is currently active.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @return The frame type that is currently active.
     */
    public static Frame getCurrentFrame(RemoteRobot remoteRobot) {
        Frame frame = null;
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            if (inWelcomeFrame(remoteRobot)) {
                frame = Frame.WELCOME;
                break;
            } else if (inProjectFrame(remoteRobot)) {
                frame = Frame.PROJECT;
                break;
            }
        }

        return frame;
    }

    /**
     * Returns true if the frame currently active is the welcome frame. False, otherwise.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @return True if the frame currently active is the welcome frame. False, otherwise.
     */
    public static boolean inWelcomeFrame(RemoteRobot remoteRobot) {
        boolean inWelcomeFrame = false;
        try {
            remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(2));
            inWelcomeFrame = true;
        } catch (Exception e) {
            // Not in welcome frame.
        }

        return inWelcomeFrame;
    }

    /**
     * Returns true if the frame currently active is the project frame. False, otherwise.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @return True if the frame currently active is the project frame. False, otherwise.
     */
    public static boolean inProjectFrame(RemoteRobot remoteRobot) {
        boolean inProjectFrame = false;
        try {
            remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(2));
            inProjectFrame = true;
        } catch (Exception e) {
            // Not in project frame.
        }

        return inProjectFrame;
    }

    /**
     * Removes the specified tool window from view.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param label       The tool window label.
     */
    public static void removeToolWindow(RemoteRobot remoteRobot, String label) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        try {
            ComponentFixture toolWindowLabel = projectFrame.getBaseLabel(label, "5");
            toolWindowLabel.rightClick();
            ComponentFixture removeSideBardAction = projectFrame.getActionMenuItem("Remove from Sidebar");
            removeSideBardAction.click();
        } catch (WaitForConditionTimeoutException e) {
            // The tool window is not active.
        }
    }
}