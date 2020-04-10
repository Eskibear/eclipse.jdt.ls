package org.eclipse.jdt.ls.core.internal.semantictokens;

import java.util.List;

import org.eclipse.lsp4j.util.Preconditions;

public class SemanticTokens {
    private String resultId;
    private List<Integer> data;

    public SemanticTokens(List<Integer> data) {
        this(data, null);
    }

    public SemanticTokens(List<Integer> data, String resultId) {
        this.data = Preconditions.<List<Integer>>checkNotNull(data, "data");
        this.resultId = resultId;
    }


}
