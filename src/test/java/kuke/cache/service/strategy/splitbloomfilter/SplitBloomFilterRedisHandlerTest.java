package kuke.cache.service.strategy.splitbloomfilter;

import kuke.cache.RedisTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SplitBloomFilterRedisHandlerTest extends RedisTestContainerSupport {
	@Autowired
	SplitBloomFilterRedisHandler splitBloomFilterRedisHandler;

	@Test
	void mightContain() {
		// given
		SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1000, 0.01);

		List<String> values = IntStream.range(0, 1000).mapToObj(idx -> "value" + idx).toList();
		for (String value : values) {
			splitBloomFilterRedisHandler.add(splitBloomFilter, value);
		}

		// when, then
		for (String value : values) {
			boolean result = splitBloomFilterRedisHandler.mightContain(splitBloomFilter, value);
			assertThat(result).isTrue();
		}

		for (int i = 0; i < 10000; i++) {
			String value = "notAddedValue" + i;
			boolean result = splitBloomFilterRedisHandler.mightContain(splitBloomFilter, value);
			if (result) {
				// false positive
				System.out.println("value = " + value);
			}
		}
	}

	@Test
	void init() {
		SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1_000L, 0.01);

		for (int splitIndex = 0; splitIndex < splitBloomFilter.getSplitCount(); splitIndex++) {
			String result = redisTemplate.opsForValue().get("split-bloom-filter:%s:split:%s".formatted(splitBloomFilter.getId(), splitIndex));
			assertThat(result).isNull();
		}

		splitBloomFilterRedisHandler.init(splitBloomFilter);

		for (int splitIndex = 0; splitIndex < splitBloomFilter.getSplitCount(); splitIndex++) {
			String result = redisTemplate.opsForValue().get("split-bloom-filter:%s:split:%s".formatted(splitBloomFilter.getId(), splitIndex));
			assertThat(result).isNotNull();
		}
	}

	@Test
	void add() {
		// given
		SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1_000L, 0.01);

		// when
		splitBloomFilterRedisHandler.add(splitBloomFilter, "value");

		// then
		List<Long> hashedIndexes = splitBloomFilter.getBloomFilter().hash("value");
		for (long offset = 0; offset < splitBloomFilter.getBloomFilter().getBitSize(); offset++) {
			long splitIndex = splitBloomFilter.findSplitIndex(offset);
			Boolean result = redisTemplate.opsForValue().getBit("split-bloom-filter:%s:split:%s".formatted(
				splitBloomFilter.getId(), splitIndex), offset % SplitBloomFilter.BIT_SPLIT_UNIT);
			assertThat(result).isEqualTo(hashedIndexes.contains(offset));
		}
	}

	@Test
	void delete() {
		// given
		SplitBloomFilter splitBloomFilter = SplitBloomFilter.create("testId", 1_000L, 0.01);
		splitBloomFilterRedisHandler.add(splitBloomFilter, "value");

		// when
		splitBloomFilterRedisHandler.delete(splitBloomFilter);

		// then
		for (long offset = 0; offset < splitBloomFilter.getBloomFilter().getBitSize(); offset++) {
			long splitIndex = splitBloomFilter.findSplitIndex(offset);
			Boolean result = redisTemplate.opsForValue().getBit("split-bloom-filter:%s:split:%s".formatted(splitBloomFilter.getId(), splitIndex), offset);
			assertThat(result).isFalse();
		}
	}
}