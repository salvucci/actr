package actr.model;

import java.util.HashSet;
import java.util.Set;

public class ChunkType {
	private final Symbol name;
	private final Set<ChunkType> parents;

	ChunkType(Symbol name) {
		this.name = name;
		parents = new HashSet<>();
	}

	Symbol getName() {
		return name;
	}

	void addParent(ChunkType parent) {
		parents.add(parent);
	}

	boolean isa(ChunkType type) {
		if (name == type.getName())
			return true;
		for (ChunkType parent : parents)
			if (parent.isa(type))
				return true;
		return false;
	}
}
