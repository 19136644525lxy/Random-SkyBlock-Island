package yifei.rsl.common.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure selection-diff logic shared by the Fabric and Forge block-pool screens.
 *
 * <p>Selections are interpreted relative to the current pool: an id that is
 * already in the pool is a removal, anything else is an addition. This is what
 * makes the "view current pool" mode editable: clicking a pool block marks it
 * for removal.</p>
 */
public final class PoolSelection {
	private PoolSelection() {
	}

	public record Diff(List<String> add, List<String> remove) {
		public Diff {
			add = add == null ? List.of() : List.copyOf(add);
			remove = remove == null ? List.of() : List.copyOf(remove);
		}

		public boolean isEmpty() {
			return add.isEmpty() && remove.isEmpty();
		}
	}

	public static Diff diff(Collection<String> selected, Collection<String> poolIds) {
		List<String> add = new ArrayList<>();
		List<String> remove = new ArrayList<>();
		if (selected == null || selected.isEmpty()) {
			return new Diff(add, remove);
		}
		Set<String> pool = poolIds == null ? Set.of() : new HashSet<>(poolIds);
		for (String id : selected) {
			if (id == null) {
				continue;
			}
			if (pool.contains(id)) {
				remove.add(id);
			} else {
				add.add(id);
			}
		}
		return new Diff(add, remove);
	}

	/**
	 * @return true when the diff removes every id currently in the pool; the UI
	 *         uses this to ask for a second confirmation before emptying the pool
	 */
	public static boolean removesWholePool(Diff diff, Collection<String> poolIds) {
		return diff != null
				&& poolIds != null
				&& !poolIds.isEmpty()
				&& diff.remove().size() == poolIds.size();
	}

	/**
	 * Keeps only ids that are still visible in the active view. This prevents a
	 * hidden addition (selected in "all blocks" view) from being applied while
	 * the user is looking at "current pool".
	 */
	public static void retainVisible(Set<String> selected, Collection<String> visible) {
		if (selected == null) {
			return;
		}
		if (visible == null) {
			selected.clear();
			return;
		}
		selected.retainAll(visible);
	}
}
