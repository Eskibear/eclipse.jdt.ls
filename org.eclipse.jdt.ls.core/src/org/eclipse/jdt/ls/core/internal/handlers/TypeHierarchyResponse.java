
package org.eclipse.jdt.ls.core.internal.handlers;

import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.jdt.core.ITypeHierarchy;


public class TypeHierarchyResponse {

	private static AtomicLong idSeed = new AtomicLong(0);
	private Long id;
	private ITypeHierarchy hierarchy;

	public TypeHierarchyResponse() {
		id = idSeed.getAndIncrement();
	}

    public Long getId() {
        return id;
    }

    public ITypeHierarchy getHierarchy() {
        return hierarchy;
    }

    public void setHierarchy(ITypeHierarchy hierarchy) {
        this.hierarchy = hierarchy;
    }


}
