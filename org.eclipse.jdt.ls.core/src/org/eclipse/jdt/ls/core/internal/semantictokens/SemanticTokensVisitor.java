package org.eclipse.jdt.ls.core.internal.semantictokens;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.ls.core.internal.handlers.JsonRpcHelpers;
import org.eclipse.jface.text.IDocument;

public class SemanticTokensVisitor extends ASTVisitor {
    private ICompilationUnit cu;
    private IDocument document;
    private SemanticTokenManager manager;
    private List<SemanticToken> tokens;
    private SemanticTokensLegend legend;

    public SemanticTokensVisitor(ICompilationUnit cu, IDocument document, SemanticTokenManager manager) {
        this.manager = manager;
        this.cu = cu;
        this.document = document;
        this.legend = manager.getLegend();
        this.tokens = new ArrayList<>();
    }

    public SemanticTokens getSemanticTokens() {
        return new SemanticTokens(encoded());
    }

    private List<Integer> encoded() {
        List<Integer> data = new ArrayList<>();
        int currentLine = 0;
        int currentColumn = 0;
        for (SemanticToken token : this.tokens) {
            int[] lineAndColumn = JsonRpcHelpers.toLine(this.document, token.getOffset());
            int line = lineAndColumn[0];
            int column = lineAndColumn[1];
            int deltaLine = line - currentLine;
            if (deltaLine != 0) {
                currentLine = line;
                currentColumn = 0;
            }
            int deltaColumn = column - currentColumn;
            int tokenTypeIndex = legend.getTokenTypes().indexOf(token.getTokenType().toString());
            ITokenModifier[] modifiers = token.getTokenModifiers();
            int m = 0;
            for (ITokenModifier modifier : modifiers) {
                int bit = manager.getTokenModifiers().indexOf(modifier);
                if (bit >= 0) {
                    m = m | (0b00000001 << bit);
                }
            }
            data.add(deltaLine);
            data.add(deltaColumn);
            data.add(token.getLength());
            data.add(tokenTypeIndex);
            data.add(m);
        }
        return data;
    }

    private void addToken(ASTNode node, TokenType tokenType) {
        addToken(node, tokenType, new ITokenModifier[] {});
    }

    private void addToken(ASTNode node, TokenType tokenType, ITokenModifier[] modifiers) {
        int offset = node.getStartPosition();
        int length = node.getLength();
        SemanticToken token = new SemanticToken(this.document, offset, length, tokenType, modifiers);
        tokens.add(token);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        TokenType tokenType;
        if (node.isInterface()) {
            // interface or annotation
            tokenType = TokenType.INTERFACE;
        } else {
            // class
            tokenType = TokenType.CLASS;
        }
        ASTNode name = node.getName();
        addToken(name, tokenType);
        return super.visit(node);
    }

    @Override
    public boolean visit(NumberLiteral node) {
        addToken(node, TokenType.NUMBER);
        return super.visit(node);
    }

    @Override
    public boolean visit(StringLiteral node) {
        addToken(node, TokenType.STRING);
        return super.visit(node);
    }

    @Override
    public boolean visit(SimpleName node) {

        IBinding binding = node.resolveBinding();

        if (binding != null) {
            TokenType tokenType = null;
            switch (binding.getKind()) {
                case IBinding.VARIABLE: {
                    if (((IVariableBinding) binding).isField()) {
                        tokenType = TokenType.VARIABLE;
                    }
                    break;
                }
                case IBinding.METHOD: {
                    tokenType = TokenType.METHOD;
                    break;
                }
                default:
                    break;
            }

            if (tokenType != null) {
                List<ITokenModifier> modifierList = new ArrayList<>();
                for (ITokenModifier tokenModifier : manager.getTokenModifiers().values()) {
                    if (tokenModifier.applies(binding)) {
                        modifierList.add(tokenModifier);
                    }
                }
                ITokenModifier[] modifiers = new ITokenModifier[modifierList.size()];
                modifierList.toArray(modifiers);
                addToken(node, tokenType, modifiers);
            }
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        return super.visit(node);
    }



}
