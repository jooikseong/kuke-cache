package kuke.cache.service.strategy.bloomfilter;

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
public class ItemBloomFilterCacheService implements ItemCacheService {
	private final ItemService itemService;
	private final BloomFilterRedisHandler bloomFilterRedisHandler;

	private static final BloomFilter bloomFilter = BloomFilter.create(
		"item-bloom-filter",
		1000,
		0.01
	);

	@Override
	public ItemResponse read(Long itemId) {
		boolean result = bloomFilterRedisHandler.mightContain(bloomFilter, String.valueOf(itemId));
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
		bloomFilterRedisHandler.add(bloomFilter, String.valueOf(itemResponse.itemId()));
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
		return CacheStrategy.BLOOM_FILTER == cacheStrategy;
	}
}
