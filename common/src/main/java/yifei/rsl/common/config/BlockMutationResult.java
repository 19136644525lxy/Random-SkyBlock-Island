package yifei.rsl.common.config;

/**
 * Outcome of mutating the global block pool. The platform layer maps each value to a
 * localized command message.
 */
public enum BlockMutationResult {
	OK,
	INVALID_ID,
	UNKNOWN_BLOCK,
	AIR_BLOCK,
	DUPLICATE,
	POOL_FULL,
	WOULD_EMPTY_RUNNING,
	NOT_FOUND,
	IO_ERROR
}
