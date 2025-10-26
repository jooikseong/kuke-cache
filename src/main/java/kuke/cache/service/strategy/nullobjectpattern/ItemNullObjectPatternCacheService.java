package kuke.cache.service.strategy.nullobjectpattern;

import kuke.cache.common.cache.CacheStrategy;
import kuke.cache.model.ItemCreateRequest;
import kuke.cache.model.ItemUpdateRequest;
import kuke.cache.service.ItemCacheService;
import kuke.cache.service.ItemService;
import kuke.cache.service.response.ItemPageResponse;
import kuke.cache.service.response.ItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemNullObjectPatternCacheService implements ItemCacheService {
	private final ItemService itemService;

	/**
	 * 더욱 유연한 구조의 nullObject를 만들 수도 있음
	 * 예시) 데이터가 정말 없는 것인지, 또는 비공개 처리되어서 접근이 안되는 것인지 등의 예외 정보도 함께 캐시해서 분기
	 */
	private static final ItemResponse nullObject = new ItemResponse(null, null);

	@Override
	@Cacheable(cacheNames = "item", key = "#itemId")
	public ItemResponse read(Long itemId) {
		ItemResponse itemResponse = itemService.read(itemId);
		if (itemResponse == null) {
			return nullObject;
		}
		return itemResponse;
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
		return CacheStrategy.NULL_OBJECT_PATTERN == cacheStrategy;
	}
}
