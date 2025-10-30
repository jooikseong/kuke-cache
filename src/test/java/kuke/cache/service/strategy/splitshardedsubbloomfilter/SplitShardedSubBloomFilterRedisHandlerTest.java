package kuke.cache.service.strategy.splitshardedsubbloomfilter;

import kuke.cache.RedisTestContainerSupport;
import kuke.cache.service.strategy.splitshardedbloomfilter.SplitShardedBloomFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SplitShardedSubBloomFilterRedisHandlerTest extends RedisTestContainerSupport {
	@Autowired
	SplitShardedSubBloomFilterRedisHandler handler;

	@Test
	void add() {
		// given
		SplitShardedSubBloomFilter splitShardedSubBloomFilter = SplitShardedSubBloomFilter.create(
			"testId", 1000, 0.01, 4
		);

		// when
		handler.add(splitShardedSubBloomFilter, "value");

		// then
		assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(0);
		assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(0)))
			.isEqualTo(1);
	}

	@Test
	void add_shouldAddSubFilter_whenFilterIsFull() {
		// given
		SplitShardedSubBloomFilter splitShardedSubBloomFilter = SplitShardedSubBloomFilter.create(
			"testId", 1000, 0.01, 4
		);

		int count = 1000 - 1;
		for (int i = 0; i < count; i++) {
			handler.add(splitShardedSubBloomFilter, "value" + i);
		}
		assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(0);
		assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(0))).isEqualTo(999);

		// when
		handler.add(splitShardedSubBloomFilter, "value" + 1000);

		// then
		assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(1);
		assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(1))).isEqualTo(0);
	}

	@Test
	void add_shouldNotAddSubFilter_whenSubFilterCountReachesMaxLimit() {
		// given
		SplitShardedSubBloomFilter splitShardedSubBloomFilter = SplitShardedSubBloomFilter.create(
			"testId", 1000, 0.01, 4
		);

		int count = 1000 + 2000 + 4000 - 1;
		for (int i = 0; i < count; i++) {
			handler.add(splitShardedSubBloomFilter, "value" + i);
		}
		assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(2);
		assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(2))).isEqualTo(3999);

		// when
		handler.add(splitShardedSubBloomFilter, "new value");

		// then
		assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(2);
		assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(2))).isEqualTo(4000);
	}

	@Test
	void mightContain() {
		// given
		SplitShardedSubBloomFilter splitShardedSubBloomFilter = SplitShardedSubBloomFilter.create(
			"testId", 1000, 0.01, 4
		);

		List<String> values = IntStream.range(0, 1000 + 2000).mapToObj(idx -> "value" + idx).toList();
		for (String value : values) {
			handler.add(splitShardedSubBloomFilter, value);
		}

		// when, then
		for (String value : values) {
			boolean result = handler.mightContain(splitShardedSubBloomFilter, value);
			assertThat(result).isTrue();
		}

		for (int i = 0; i < 1000; i++) {
			String value = "notAddedValue" + i;
			boolean result = handler.mightContain(splitShardedSubBloomFilter, value);
			if (result) {
				// false positive
				System.out.println("value = " + value);
			}
		}
	}

	@Test
	void init() {
		SplitShardedSubBloomFilter splitShardedSubBloomFilter = SplitShardedSubBloomFilter.create(
			"testId", 1_000, 0.01, 4
		);

		handler.init(splitShardedSubBloomFilter);
	}

	@Test
	void delete() {
		// given
		SplitShardedSubBloomFilter splitShardedSubBloomFilter = SplitShardedSubBloomFilter.create(
			"testId", 1_000, 0.01, 4
		);
		handler.add(splitShardedSubBloomFilter, "value");

		// when
		handler.delete(splitShardedSubBloomFilter);

		// then
		assertThat(getSubFilterCount(splitShardedSubBloomFilter)).isEqualTo(0);
		assertThat(getDataCount(splitShardedSubBloomFilter.findActivatedFilter(0))).isEqualTo(0);
	}

	private long getDataCount(SplitShardedBloomFilter splitShardedBloomFilter) {
		String result = redisTemplate.opsForValue().get(
			"split-sharded-sub-bloom-filter:data-count:%s".formatted(splitShardedBloomFilter.getId())
		);
		return result == null ? 0 : Long.parseLong(result);
	}

	private int getSubFilterCount(SplitShardedSubBloomFilter splitShardedSubBloomFilter) {
		String result = redisTemplate.opsForValue().get(
			"split-sharded-sub-bloom-filter:sub-filter-count:%s".formatted(splitShardedSubBloomFilter.getId())
		);
		return result == null ? 0 : Integer.parseInt(result);
	}
}