package org.eclipse.jdt.ls.core.internal.semantictokens;

import java.util.List;

public class SemanticTokensLegend {
    private List<String> tokenTypes;
    private List<String> tokenModifiers;

    public SemanticTokensLegend(List<String> tokenTypes, List<String> tokenModifiers){
        this.tokenTypes = tokenTypes;
        this.tokenModifiers = tokenModifiers;
    };

    public List<String> getTokenTypes() {
        return this.tokenTypes;
    }

    public List<String> getTokenModifiers() {
        return this.tokenModifiers;
    }
}
