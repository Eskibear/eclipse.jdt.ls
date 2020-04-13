package org.eclipse.jdt.ls.core.internal.commands;

import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.manipulation.CoreASTProvider;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.handlers.JsonRpcHelpers;
import org.eclipse.jdt.ls.core.internal.semantictokens.SemanticTokenManager;
import org.eclipse.jdt.ls.core.internal.semantictokens.SemanticTokens;
import org.eclipse.jdt.ls.core.internal.semantictokens.SemanticTokensLegend;
import org.eclipse.jdt.ls.core.internal.semantictokens.SemanticTokensVisitor;
import org.eclipse.jface.text.IDocument;

public class SemanticTokensCommand {

    public static SemanticTokens provide(String uri) {

        IDocument document = null;

        ICompilationUnit cu = JDTUtils.resolveCompilationUnit(uri);
        //ignoring working copies, they're handled in the DocumentLifecycleHandler
        if (cu != null) {
            try {
                document = JsonRpcHelpers.toDocument(cu.getBuffer());
            } catch (JavaModelException e) {
                JavaLanguageServerPlugin.logException("Failed to provide semantic tokens for " + uri, e);
            }
        }


        SemanticTokensVisitor collector = new SemanticTokensVisitor(cu, document, SemanticTokenManager.getInstance());
        CompilationUnit root = CoreASTProvider.getInstance().getAST(cu, CoreASTProvider.WAIT_YES, new NullProgressMonitor());
        root.accept(collector);
        return collector.getSemanticTokens();
    }

    public static SemanticTokensLegend getLegend() {
        return SemanticTokenManager.getInstance().getLegend();
    }


}
