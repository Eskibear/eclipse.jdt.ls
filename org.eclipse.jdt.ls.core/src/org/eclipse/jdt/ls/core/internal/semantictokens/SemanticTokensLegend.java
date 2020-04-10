package org.eclipse.jdt.ls.core.internal.semantictokens;

import java.util.List;

public class SemanticTokensLegend {
    private List<String> tokenTypes;
    private List<String> tokenModifiers;

    private SemanticTokensLegend(){
        this.tokenTypes = TokenType.getAll();
        this.tokenModifiers = TokenModifier.getAll();
    };

    private static class SingletonHelper{
        private static final SemanticTokensLegend INSTANCE = new SemanticTokensLegend();
    }

    public static SemanticTokensLegend getInstance(){
        return SingletonHelper.INSTANCE;
    }

    public List<String> getTokenTypes() {
        return this.tokenTypes;
    }

    public List<String> getTokenModifiers() {
        return this.tokenModifiers;
    }
}
