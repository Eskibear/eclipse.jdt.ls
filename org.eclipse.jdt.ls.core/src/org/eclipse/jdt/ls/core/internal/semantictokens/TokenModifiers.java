package org.eclipse.jdt.ls.core.internal.semantictokens;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.Modifier;

public class TokenModifiers {
    private ITokenModifier[] mods;
    private Map<ITokenModifier, Integer> modifierIndices;

    public TokenModifiers() {
        mods = new ITokenModifier[] { new StaticModifier(), new FinalModifier(), new DeprecatedModifier(), };
        modifierIndices = new HashMap<>();
        for (int i = 0; i < mods.length; i++) {
            modifierIndices.putIfAbsent(mods[i], i);
        }
    }

    public Set<ITokenModifier> values() {
        return modifierIndices.keySet();
    }

    public List<ITokenModifier> list() {
        return Arrays.asList(mods);
    }

    public int indexOf(ITokenModifier modifier) {
        return modifierIndices.getOrDefault(modifier, -1);
    }



    class StaticModifier implements ITokenModifier {
        @Override
        public boolean applies(IBinding binding) {
            int flags = binding.getModifiers();
            if ((flags & Modifier.STATIC) != 0) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "static";
        }
    }

    class FinalModifier implements ITokenModifier {
        @Override
        public boolean applies(IBinding binding) {
            int flags = binding.getModifiers();
            if ((flags & Modifier.FINAL) != 0) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return "final";
        }
    }

    class DeprecatedModifier implements ITokenModifier {
        @Override
        public boolean applies(IBinding binding) {
            if (binding != null) {
                return binding.isDeprecated();
            }
            return false;
        }

        @Override
        public String toString() {
            return "deprecated";
        }
    }

}
