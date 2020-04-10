package org.eclipse.jdt.ls.core.internal.semantictokens;

import java.util.ArrayList;
import java.util.List;

public enum TokenType {
    CLASS("class"),
    INTERFACE("interface"),
    ENUM("enum"),
    TYPE_PARAMETER("typeParameter"),

    NUMBER("number"),
    STRING("string"),
    VARIABLE("variable"),
    METHOD("method"),
    ;

    private String literalString;
    TokenType(String tokenTypeString) {
        this.literalString = tokenTypeString;
    }

    public String toString() {
        return this.literalString;
    }

    public static List<String> getAll() {
        TokenType[] allTypes = TokenType.values();
        List<String> list = new ArrayList<>(allTypes.length);
        for (TokenType tokenType : allTypes) {
            list.add(tokenType.toString());
        }
        return list;
    }
}
