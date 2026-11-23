package yifei.rsl.common.world;

import org.junit.jupiter.api.Test;
import yifei.rsl.common.config.AreaSpec;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FillCursorTest {
	@Test
	void visitsEveryPositionExactlyOnceInLegacyOrder() {
		AreaSpec area = new AreaSpec("x", 0, 0, 0, 1, 1, 1);
		FillCursor cursor = new FillCursor(area, -64, 320);
		List<String> visited = new ArrayList<>();
		while (cursor.hasNext()) {
			visited.add(cursor.x() + "," + cursor.y() + "," + cursor.z());
			cursor.advance();
		}
		assertEquals(List.of(
				"0,0,0", "0,0,1",
				"0,1,0", "0,1,1",
				"1,0,0", "1,0,1",
				"1,1,0", "1,1,1"), visited);
		assertEquals(8L, cursor.total());
		assertEquals(8L, cursor.done());
		assertEquals(0L, cursor.remaining());
		assertFalse(cursor.hasNext());
	}

	@Test
	void clampsToWorldHeight() {
		AreaSpec area = new AreaSpec("x", 0, -10, 0, 0, 10, 0);
		FillCursor cursor = new FillCursor(area, 0, 5);
		assertEquals(5L, cursor.total());
		List<Integer> ys = new ArrayList<>();
		while (cursor.hasNext()) {
			ys.add(cursor.y());
			cursor.advance();
		}
		assertEquals(List.of(0, 1, 2, 3, 4), ys);
	}

	@Test
	void emptyWhenAreaIsOutsideWorld() {
		AreaSpec area = new AreaSpec("x", 0, 10, 0, 0, 20, 0);
		FillCursor cursor = new FillCursor(area, 0, 5);
		assertTrue(cursor.isEmpty());
		assertFalse(cursor.hasNext());
		assertEquals(0L, cursor.total());
	}

	@Test
	void handlesMinGreaterThanMax() {
		AreaSpec area = new AreaSpec("x", 1, 1, 1, 0, 0, 0);
		FillCursor cursor = new FillCursor(area, -64, 320);
		assertEquals(8L, cursor.total());
		assertTrue(cursor.hasNext());
	}
}
