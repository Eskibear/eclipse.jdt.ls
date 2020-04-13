package org.eclipse.jdt.ls.core.internal.semantictokens;

import org.eclipse.jdt.core.dom.IBinding;

public interface ITokenModifier {
    public boolean applies(IBinding binding);
    public String toString();
}
