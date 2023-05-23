/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Yijia Jing
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.it.jsonp;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4jakarta.commons.JakartaDiagnosticsParams;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

public class JakartaJsonpTest extends BaseJakartaTest {

    @Test
    public void invalidPointerTarget() throws Exception {
        Module module = createMavenModule(new File("projects/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "src/main/java/io/openliberty/sample/jakarta/jsonp/CreatePointerInvalidTarget.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
        
        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        
        Diagnostic d1 = JakartaForJavaAssert.d(20, 60, 64,
                "Json.createPointer target must be a sequence of '/' prefixed tokens or an empty String", 
                DiagnosticSeverity.Error, "jakarta-jsonp", "InvalidCreatePointerArg");
        
        Diagnostic d2 = JakartaForJavaAssert.d(21, 62, 70,
                "Json.createPointer target must be a sequence of '/' prefixed tokens or an empty String", 
                DiagnosticSeverity.Error, "jakarta-jsonp", "InvalidCreatePointerArg");
        
        Diagnostic d3 = JakartaForJavaAssert.d(22, 60, 80,
                "Json.createPointer target must be a sequence of '/' prefixed tokens or an empty String", 
                DiagnosticSeverity.Error, "jakarta-jsonp", "InvalidCreatePointerArg");
        
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);
    }
}