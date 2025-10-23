package kuke.cache.service;

import kuke.cache.common.cache.CacheStrategy;
import kuke.cache.model.ItemCreateRequest;
import kuke.cache.model.ItemUpdateRequest;
import kuke.cache.service.response.ItemPageResponse;
import kuke.cache.service.response.ItemResponse;

public interface ItemCacheService {
	ItemResponse read(Long itemId);

	ItemPageResponse readAll(Long page, Long pageSize);

	ItemPageResponse readAllInfiniteScroll(Long lastItemId, Long pageSize);

	ItemResponse create(ItemCreateRequest request);

	ItemResponse update(Long itemId, ItemUpdateRequest request);

	void delete(Long itemId);

	boolean supports(CacheStrategy cacheStrategy);
}
