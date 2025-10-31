package kuke.cache.service.strategy.per;

import kuke.cache.common.cache.CacheStrategy;
import kuke.cache.common.cache.KukeCacheEvict;
import kuke.cache.common.cache.KukeCachePut;
import kuke.cache.common.cache.KukeCacheable;
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
public class ItemProbabilisticEarlyRecomputationCacheService implements ItemCacheService {
	private final ItemService itemService;

	@Override
	@KukeCacheable(
		cacheStrategy = CacheStrategy.PROBABILISTIC_EARLY_RECOMPUTATION,
		cacheName = "item",
		key = "#itemId",
		ttlSeconds = 5
	)
	public ItemResponse read(Long itemId) {
		return itemService.read(itemId);
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
	@KukeCachePut(
		cacheStrategy = CacheStrategy.PROBABILISTIC_EARLY_RECOMPUTATION,
		cacheName = "item",
		key = "#itemId",
		ttlSeconds = 5
	)
	public ItemResponse update(Long itemId, ItemUpdateRequest request) {
		return itemService.update(itemId, request);
	}

	@Override
	@KukeCacheEvict(
		cacheStrategy = CacheStrategy.PROBABILISTIC_EARLY_RECOMPUTATION,
		cacheName = "item",
		key = "#itemId"
	)
	public void delete(Long itemId) {
		itemService.delete(itemId);
	}

	@Override
	public boolean supports(CacheStrategy cacheStrategy) {
		return CacheStrategy.PROBABILISTIC_EARLY_RECOMPUTATION == cacheStrategy;
	}
}
