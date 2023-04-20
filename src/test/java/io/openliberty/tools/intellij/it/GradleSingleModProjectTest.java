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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests Liberty Tools actions using a Gradle project.
 */
public class GradleSingleModProjectTest extends SingleModProjectTestCommon {

    /**
     * Single module Microprofile project name.
     */
    public static String SM_MP_PROJECT_NAME = "singleModGradleMP";

    /**
     * Single module REST project that lacks the configuration to be recognized by Liberty tools.
     */
    public static String SM_NLT_REST_PROJECT_NAME = "singleModGradleRESTNoLTXmlCfg";

    /**
     * The path to the folder containing the test projects.
     */
    public static String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString();

    /**
     * Project port.
     */
    public static int SM_MP_PROJECT_PORT = 9090;

    /**
     * Project resource URI.
     */
    public static String SM_MP_PROJECT_RES_URI = "api/resource";

    /**
     * Project resource URL.
     */
    public static String SM_MP_PROJECT_BASE_URL = "http://localhost:" + SM_MP_PROJECT_PORT + "/";

    /**
     * Project response.
     */
    public static String SM_MP_PROJECT_OUTPUT = "Hello! Welcome to Open Liberty";

    /**
     * Relative location of the WLP installation.
     */
    public static String WLP_INSTALL_PATH = "build";

    /**
     * The path to the test report.
     */
    public static final Path TEST_REPORT_PATH = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "build", "reports", "tests", "test", "index.html");

    /**
     * Build file name.
     */
    public static final String BUILD_FILE_NAME = "build.gradle";

    /**
     * Action command to open the build file.
     */
    public static final String BUILD_FILE_OPEN_CMD = "Liberty: View Gradle config";

    /**
     * Constructor.
     */
    public GradleSingleModProjectTest() {
        super(PROJECTS_PATH, SM_MP_PROJECT_NAME, SM_NLT_REST_PROJECT_NAME, SM_MP_PROJECT_BASE_URL, SM_MP_PROJECT_OUTPUT);
    }

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECTS_PATH, SM_MP_PROJECT_NAME);
    }


    /**
     * Returns the path where the Liberty server was installed.
     *
     * @return The path where the Liberty server was installed.
     */
    @Override
    public String getWLPInstallPath() {
        return WLP_INSTALL_PATH;
    }

    /**
     * Returns the port number associated with the single module MicroProfile project.
     *
     * @return The port number associated with the single module MicroProfile project.
     */
    @Override
    public int getSmMpProjPort() {
        return SM_MP_PROJECT_PORT;
    }

    /**
     * Return the Resource URI associated with the single module MicroProfile project.
     *
     * @return The Resource URI associated with the single module MicroProfile project.
     */
    @Override
    public String getSmMpProjResURI() {
        return SM_MP_PROJECT_RES_URI;
    }

    /**
     * Returns the name of the build file used by the project.
     *
     * @return The name of the build file used by the project.
     */
    @Override
    public String getBuildFileName() {
        return BUILD_FILE_NAME;
    }

    /**
     * Returns the name of the custom action command used to open the build file.
     *
     * @return The name of the custom action command used to open the build file.
     */
    @Override
    public String getBuildFileOpenCommand() {
        return BUILD_FILE_OPEN_CMD;
    }

    /**
     * Deletes test reports.
     */
    @Override
    public void deleteTestReports() {
        boolean testReportDeleted = TestUtils.deleteFile(TEST_REPORT_PATH.toFile());
        Assertions.assertTrue(testReportDeleted, () -> "Test report file: " + TEST_REPORT_PATH + " was not be deleted.");
    }

    /**
     * Validates that test reports were generated.
     */
    @Override
    public void validateTestReportsExist() {
        TestUtils.validateTestReportExists(TEST_REPORT_PATH);
    }
}