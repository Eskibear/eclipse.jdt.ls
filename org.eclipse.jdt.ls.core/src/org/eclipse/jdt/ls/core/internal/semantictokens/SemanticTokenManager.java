package org.eclipse.jdt.ls.core.internal.semantictokens;

import java.util.List;
import java.util.stream.Collectors;

public class SemanticTokenManager {
    private TokenModifiers tokenModifiers;
    private List<String> tokenTypes; // TODO: use TokenTypes
    private SemanticTokensLegend legend;

    private SemanticTokenManager() {
        this.tokenModifiers = new TokenModifiers();
        this.tokenTypes = TokenType.getAll();
        List<String> modifiersString = tokenModifiers.list().stream().map(mod -> mod.toString()).collect(Collectors.toList());
        this.legend = new SemanticTokensLegend(tokenTypes, modifiersString);
    }

    private static class SingletonHelper{
        private static final SemanticTokenManager INSTANCE = new SemanticTokenManager();
    }

    public static SemanticTokenManager getInstance(){
        return SingletonHelper.INSTANCE;
    }

    public SemanticTokensLegend getLegend() {
        return this.legend;
    }

    public TokenModifiers getTokenModifiers() {
        return tokenModifiers;
    }

    public List<String> getTokenTypes() {
        return tokenTypes;
    }

}
