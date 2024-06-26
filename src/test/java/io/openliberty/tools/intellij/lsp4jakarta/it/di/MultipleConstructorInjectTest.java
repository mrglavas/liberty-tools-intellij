/*******************************************************************************
* Copyright (c) 2021, 2024 IBM Corporation.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Ananya Rao
*******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.it.di;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

@RunWith(JUnit4.class)
public class MultipleConstructorInjectTest extends BaseJakartaTest {

    @Test
    public void multipleInject() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/di/MultipleConstructorWithInject.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        

        // test expected diagnostic
        Diagnostic d1 = JakartaForJavaAssert.d(22, 11, 40,
                "The @Inject annotation must not define more than one constructor.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInject");
        
        Diagnostic d2 = JakartaForJavaAssert.d(26, 11, 40,
                "The @Inject annotation must not define more than one constructor.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInject");
        
        Diagnostic d3 = JakartaForJavaAssert.d(31, 14, 43,
                "The @Inject annotation must not define more than one constructor.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInject");
        
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1,d2,d3);
        
            // test expected quick-fix
        String newText = "/*******************************************************************************\n" +
                "* Copyright (c) 2021 IBM Corporation.\n*\n* This program and the accompanying materials are made available under the\n" +
                "* terms of the Eclipse Public License v. 2.0 which is available at\n* http://www.eclipse.org/legal/epl-2.0.\n*\n" +
                "* SPDX-License-Identifier: EPL-2.0\n*\n* Contributors:\n*     Ananya Rao\n" +
                "*******************************************************************************/\n\n" +
                "package io.openliberty.sample.jakarta.di;\n\nimport jakarta.inject.Inject;\n\n" +
                "public class MultipleConstructorWithInject{\n    private int productNum;\n    private String productDesc;\n	\n" +
                "    public MultipleConstructorWithInject(int productNum) {\n        this.productNum = productNum;\n	}\n" +
                "    @Inject\n    public MultipleConstructorWithInject(String productDesc) {\n" +
                "        this.productDesc = productDesc;\n	}\n\n    @Inject\n" +
                "    protected MultipleConstructorWithInject(int productNum, String productDesc) {\n" +
                "        this.productNum = productNum;\n        this.productDesc = productDesc;\n	}\n}\n\n";

        JakartaJavaCodeActionParams codeActionParams1 = JakartaForJavaAssert.createCodeActionParams(uri, d1);
        TextEdit te = JakartaForJavaAssert.te(0, 0, 37, 0, newText);
        CodeAction ca = JakartaForJavaAssert.ca(uri, "Remove @Inject", d1, te);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams1, utils, ca);

        String newText1 = "/*******************************************************************************\n" +
                "* Copyright (c) 2021 IBM Corporation.\n*\n* This program and the accompanying materials are made available under the\n" +
                "* terms of the Eclipse Public License v. 2.0 which is available at\n* http://www.eclipse.org/legal/epl-2.0.\n*\n" +
                "* SPDX-License-Identifier: EPL-2.0\n*\n* Contributors:\n" +
                "*     Ananya Rao\n*******************************************************************************/\n\n" +
                "package io.openliberty.sample.jakarta.di;\n\nimport jakarta.inject.Inject;\n\n" +
                "public class MultipleConstructorWithInject{\n    private int productNum;\n    private String productDesc;\n	\n" +
                "    @Inject\n    public MultipleConstructorWithInject(int productNum) {\n        this.productNum = productNum;\n	}\n" +
                "    @Inject\n    public MultipleConstructorWithInject(String productDesc) {\n        this.productDesc = productDesc;\n	}\n\n" +
                "    protected MultipleConstructorWithInject(int productNum, String productDesc) {\n        this.productNum = productNum;\n" +
                "        this.productDesc = productDesc;\n	}\n}\n\n";

        JakartaJavaCodeActionParams codeActionParams2 = JakartaForJavaAssert.createCodeActionParams(uri, d3);
        TextEdit te2 = JakartaForJavaAssert.te(0, 0, 37, 0, newText1);
        CodeAction ca2 = JakartaForJavaAssert.ca(uri, "Remove @Inject", d3, te2);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams2, utils, ca2);
    }

}
