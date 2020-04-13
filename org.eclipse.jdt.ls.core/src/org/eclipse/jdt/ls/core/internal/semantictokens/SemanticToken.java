package org.eclipse.jdt.ls.core.internal.semantictokens;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class SemanticToken {
    private TokenType tokenType;
    private ITokenModifier[] tokenModifiers;
    private int offset;
    private int length;
    private IDocument document;
    private String text;// TODO:???

    public SemanticToken(IDocument document, int offset, int length, TokenType tokenType, ITokenModifier[] tokenModifiers) {
        this.document = document;
        this.offset = offset;
        this.length = length;
        this.tokenType = tokenType;
        this.tokenModifiers = tokenModifiers;
        try {
            this.text = document.get(offset, length);
        } catch (BadLocationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public ITokenModifier[] getTokenModifiers() {
        return tokenModifiers;
    }

    public void setTokenModifiers(ITokenModifier[] tokenModifiers) {
        this.tokenModifiers = tokenModifiers;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public IDocument getDocument() {
        return document;
    }

    public void setDocument(IDocument document) {
        this.document = document;
    }

}
