package org.eclipse.jdt.ls.core.internal.handlers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache of {@link CompletionResponse}s.
 *
 * @author Fred Bricon
 */
public final class TypeHierarchyResponses {

	private TypeHierarchyResponses(){
		//Don't instantiate
	}

	private static final Map<Long, TypeHierarchyResponse> TYPE_HIERARCHIES = new ConcurrentHashMap<>();

	public static TypeHierarchyResponse get(Long id) {
		return TYPE_HIERARCHIES.get(id);
	}

	public static void store(TypeHierarchyResponse response) {
		if (response != null) {
			TYPE_HIERARCHIES.put(response.getId(), response);
		}
	}

	public static void delete(TypeHierarchyResponse response) {
		if (response != null) {
			TYPE_HIERARCHIES.remove(response.getId());
		}
	}

	public static void clear() {
		TYPE_HIERARCHIES.clear();
	}
}
