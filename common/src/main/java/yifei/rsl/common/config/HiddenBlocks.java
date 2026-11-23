package yifei.rsl.common.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-side set of block ids that should not be rendered in the block-pool
 * screen. This never touches the server-side random pool.
 */
public final class HiddenBlocks {
    private final LinkedHashSet<String> hidden = new LinkedHashSet<>();

    public int hideAll(Collection<String> ids) {
        if (ids == null) {
            return 0;
        }
        int added = 0;
        for (String id : ids) {
            String value = trim(id);
            if (value != null && hidden.add(value)) {
                added++;
            }
        }
        return added;
    }

    public int showAll(Collection<String> ids) {
        if (ids == null) {
            return 0;
        }
        int removed = 0;
        for (String id : ids) {
            String value = trim(id);
            if (value != null && hidden.remove(value)) {
                removed++;
            }
        }
        return removed;
    }

    public boolean show(String id) {
        String value = trim(id);
        return value != null && hidden.remove(value);
    }

    public boolean isHidden(String id) {
        String value = trim(id);
        return value != null && hidden.contains(value);
    }

    public Set<String> hidden() {
        return Collections.unmodifiableSet(hidden);
    }

    public List<String> list() {
        return new ArrayList<>(hidden);
    }

    public int count() {
        return hidden.size();
    }

    public boolean isEmpty() {
        return hidden.isEmpty();
    }

    public void clear() {
        hidden.clear();
    }

    /**
     * Returns the ids that are not hidden, preserving the input order.
     */
    public List<String> filterVisible(Collection<String> ids) {
        List<String> result = new ArrayList<>();
        if (ids == null) {
            return result;
        }
        for (String id : ids) {
            if (!isHidden(id)) {
                result.add(id);
            }
        }
        return result;
    }

    private static String trim(String id) {
        if (id == null) {
            return null;
        }
        String value = id.trim();
        return value.isEmpty() ? null : value;
    }
}
