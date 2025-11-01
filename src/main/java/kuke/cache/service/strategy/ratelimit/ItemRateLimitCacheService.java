package kuke.cache.service.strategy.ratelimit;

import kuke.cache.common.cache.CacheStrategy;
import kuke.cache.model.ItemCreateRequest;
import kuke.cache.model.ItemUpdateRequest;
import kuke.cache.service.ItemCacheService;
import kuke.cache.service.ItemService;
import kuke.cache.service.response.ItemPageResponse;
import kuke.cache.service.response.ItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemRateLimitCacheService implements ItemCacheService {
	private final ItemService itemService;
	private final RateLimiter rateLimiter;

	private static final String RATE_LIMITER_ID = "itemRead";
	private static final long RATE_LIMIT_COUNT = 100;
	private static final long RATE_LIMIT_PER_SECONDS = 1;

	@Override
	public ItemResponse read(Long itemId) {
		boolean allowed = rateLimiter.isAllowed(RATE_LIMITER_ID, RATE_LIMIT_COUNT, RATE_LIMIT_PER_SECONDS);
		if (allowed) {
			return itemService.read(itemId);
		}
		throw new RuntimeException("item read rate limit exception");
	}

	@Override
	public ItemPageResponse readAll(Long page, Long pageSize) {
		return itemService.readAll(page, pageSize);
	}

	@Override
	public ItemPageResponse readAllInfiniteScroll(Long lastItemId, Long pageSize) {
		return itemService.readAllInfiniteScroll(lastItemId, pageSize);
	}

	@Override
	public ItemResponse create(ItemCreateRequest request) {
		return itemService.create(request);
	}

	@Override
	public ItemResponse update(Long itemId, ItemUpdateRequest request) {
		return itemService.update(itemId, request);
	}

	@Override
	public void delete(Long itemId) {
		itemService.delete(itemId);
	}

	@Override
	public boolean supports(CacheStrategy cacheStrategy) {
		return CacheStrategy.RATE_LIMIT == cacheStrategy;
	}
}
