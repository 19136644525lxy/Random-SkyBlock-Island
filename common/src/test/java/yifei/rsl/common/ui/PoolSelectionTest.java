package yifei.rsl.common.ui;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolSelectionTest {
	@Test
	void splitsSelectedPoolIdsIntoRemovalsAndOtherIdsIntoAdditions() {
		Set<String> selected = new LinkedHashSet<>(List.of("minecraft:stone", "mymod:new_block"));
		List<String> pool = List.of("minecraft:stone", "minecraft:dirt");

		PoolSelection.Diff diff = PoolSelection.diff(selected, pool);

		assertEquals(List.of("mymod:new_block"), diff.add());
		assertEquals(List.of("minecraft:stone"), diff.remove());
	}

	@Test
	void preservesSelectionOrder() {
		Set<String> selected = new LinkedHashSet<>(List.of("b", "a", "c"));
		List<String> pool = List.of("a", "c");

		PoolSelection.Diff diff = PoolSelection.diff(selected, pool);

		assertEquals(List.of("b"), diff.add());
		assertEquals(List.of("a", "c"), diff.remove());
	}

	@Test
	void emptySelectionHasNoChanges() {
		PoolSelection.Diff diff = PoolSelection.diff(Set.of(), List.of("a"));

		assertTrue(diff.isEmpty());
		assertTrue(diff.add().isEmpty());
		assertTrue(diff.remove().isEmpty());
	}

	@Test
	void wholePoolRemovalRequiresAllPoolIds() {
		List<String> pool = List.of("a", "b");
		PoolSelection.Diff all = PoolSelection.diff(Set.of("a", "b"), pool);
		PoolSelection.Diff some = PoolSelection.diff(Set.of("a"), pool);

		assertTrue(PoolSelection.removesWholePool(all, pool));
		assertFalse(PoolSelection.removesWholePool(some, pool));
	}

	@Test
	void emptyPoolIsNeverAWholePoolRemoval() {
		PoolSelection.Diff diff = PoolSelection.diff(Set.of(), List.of());

		assertFalse(PoolSelection.removesWholePool(diff, List.of()));
	}

	@Test
	void retainVisibleDropsHiddenSelections() {
		Set<String> selected = new LinkedHashSet<>(List.of("a", "hidden"));

		PoolSelection.retainVisible(selected, List.of("a"));

		assertEquals(Set.of("a"), selected);
	}
}
