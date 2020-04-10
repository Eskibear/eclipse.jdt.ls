package org.eclipse.jdt.ls.core.internal.semantictokens;

import java.util.ArrayList;
import java.util.List;

public enum TokenModifier {
    STATIC("static"),
    DEPRECATED("deprecated"),
    READ_ONLY("readonly"),
    ;

    private String literalString;
    TokenModifier(String value) {
        this.literalString = value;
    }

    public String toString() {
        return this.literalString;
    }

    public static List<String> getAll() {
        TokenModifier[] allModifiers = TokenModifier.values();
        List<String> list = new ArrayList<>(allModifiers.length);
        for (TokenModifier modifier : allModifiers) {
            list.add(modifier.toString());
        }
        return list;
    }
}
