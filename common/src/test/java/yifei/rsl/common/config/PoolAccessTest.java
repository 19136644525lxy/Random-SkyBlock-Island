package yifei.rsl.common.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoolAccessTest {
	private static final UUID ALICE = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID BOB = UUID.fromString("22222222-2222-2222-2222-222222222222");

	@Test
	void grantsEditorsAndUpgradesToOwner() {
		List<PoolAccessEntry> entries = new ArrayList<>();
		assertFalse(PoolAccess.canUse(entries, ALICE));

		assertTrue(PoolAccess.grant(entries, ALICE, "Alice", false));
		assertTrue(PoolAccess.isEditor(entries, ALICE));
		assertTrue(PoolAccess.canUse(entries, ALICE));
		assertFalse(PoolAccess.isOwner(entries, ALICE));

		assertFalse(PoolAccess.grant(entries, ALICE, "Alice", false));
		assertTrue(PoolAccess.grant(entries, ALICE, "Alice", true));
		assertTrue(PoolAccess.isOwner(entries, ALICE));

		// An owner is never silently downgraded by a later editor grant.
		assertFalse(PoolAccess.grant(entries, ALICE, "Alice", false));
		assertTrue(PoolAccess.isOwner(entries, ALICE));
	}

	@Test
	void revokesBothRoles() {
		List<PoolAccessEntry> entries = new ArrayList<>();
		PoolAccess.grant(entries, ALICE, "Alice", true);
		PoolAccess.grant(entries, BOB, "Bob", false);

		assertTrue(PoolAccess.revoke(entries, ALICE));
		assertNull(PoolAccess.find(entries, ALICE));
		assertTrue(PoolAccess.canUse(entries, BOB));
		assertFalse(PoolAccess.revoke(entries, ALICE));
	}

	@Test
	void sanitizeNormalizesAndDeduplicatesByUuid() {
		List<PoolAccessEntry> entries = new ArrayList<>();
		entries.add(new PoolAccessEntry(ALICE.toString().toUpperCase(), "Alice", PoolAccessEntry.Role.EDITOR));
		entries.add(new PoolAccessEntry(ALICE.toString(), "Alice2", PoolAccessEntry.Role.OWNER));
		entries.add(new PoolAccessEntry("not-a-uuid", "Bad", PoolAccessEntry.Role.OWNER));
		entries.add(new PoolAccessEntry(BOB.toString(), "Bob", null));

		PoolAccess.sanitize(entries);

		assertEquals(1, entries.size());
		assertEquals(ALICE.toString(), entries.get(0).uuid);
		assertEquals(PoolAccessEntry.Role.OWNER, entries.get(0).role);
		assertEquals("Alice2", entries.get(0).name);
	}

	@Test
	void missingOrMalformedEntriesAreIgnored() {
		List<PoolAccessEntry> entries = new ArrayList<>();
		assertFalse(PoolAccess.canUse(entries, ALICE));
		assertNull(PoolAccess.find(entries, null));
		assertFalse(PoolAccess.grant(entries, null, "nobody", true));
		assertFalse(PoolAccess.revoke(entries, null));
	}
}
