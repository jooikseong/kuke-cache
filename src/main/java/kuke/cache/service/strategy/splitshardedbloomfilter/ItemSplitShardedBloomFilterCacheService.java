package kuke.cache.service.strategy.splitshardedbloomfilter;

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
public class ItemSplitShardedBloomFilterCacheService implements ItemCacheService {
	private final ItemService itemService;
	private final SplitShardedBloomFilterRedisHandler splitShardedBloomFilterRedisHandler;

	private static final SplitShardedBloomFilter bloomFilter = SplitShardedBloomFilter.create(
		"item-bloom-filter",
		1000,
		0.01,
		4
	);

	@Override
	public ItemResponse read(Long itemId) {
		boolean result = splitShardedBloomFilterRedisHandler.mightContain(bloomFilter, String.valueOf(itemId));
		if (!result) {
			return null;
		}
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
		ItemResponse itemResponse = itemService.create(request);
		splitShardedBloomFilterRedisHandler.add(bloomFilter, String.valueOf(itemResponse.itemId()));
		return itemResponse;
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
		return CacheStrategy.SPLIT_SHARDED_BLOOM_FILTER == cacheStrategy;
	}
}
