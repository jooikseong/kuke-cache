package kuke.cache.service.strategy.splitshardedbloomfilter;

import kuke.cache.RedisTestContainerSupport;
import kuke.cache.service.strategy.splitbloomfilter.SplitBloomFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SplitShardedBloomFilterRedisHandlerTest extends RedisTestContainerSupport {
	@Autowired
	SplitShardedBloomFilterRedisHandler splitShardedBloomFilterRedisHandler;

	@Test
	void mightContain() {
		// given
		SplitShardedBloomFilter splitShardedBloomFilter = SplitShardedBloomFilter.create(
			"testId", 1000, 0.01, 4
		);

		List<String> values = IntStream.range(0, 1000).mapToObj(idx -> "value" + idx).toList();
		for (String value : values) {
			splitShardedBloomFilterRedisHandler.add(splitShardedBloomFilter, value);
		}

		// when, then
		for (String value : values) {
			boolean result = splitShardedBloomFilterRedisHandler.mightContain(splitShardedBloomFilter, value);
			assertThat(result).isTrue();
		}

		for (int i = 0; i < 10000; i++) {
			String value = "notAddedValue" + i;
			boolean result = splitShardedBloomFilterRedisHandler.mightContain(splitShardedBloomFilter, value);
			if (result) {
				// false positive
				System.out.println("value = " + value);
			}
		}
	}


	@Test
	void init() {
		SplitShardedBloomFilter splitShardedBloomFilter = SplitShardedBloomFilter.create("testId", 1_000, 0.01, 4);

		for (int shardIndex = 0; shardIndex < splitShardedBloomFilter.getShardCount(); shardIndex++) {
			SplitBloomFilter splitBloomFilter = splitShardedBloomFilter.getShards().get(shardIndex);
			for (int splitIndex = 0; splitIndex < splitBloomFilter.getSplitCount(); splitIndex++) {
				String result = redisTemplate.opsForValue().get("split-bloom-filter:%s:split:%s".formatted(splitBloomFilter.getId(), splitIndex));
				assertThat(result).isNull();
			}
		}

		splitShardedBloomFilterRedisHandler.init(splitShardedBloomFilter);

		for (int shardIndex = 0; shardIndex < splitShardedBloomFilter.getShardCount(); shardIndex++) {
			SplitBloomFilter splitBloomFilter = splitShardedBloomFilter.getShards().get(shardIndex);
			for (int splitIndex = 0; splitIndex < splitBloomFilter.getSplitCount(); splitIndex++) {
				String result = redisTemplate.opsForValue().get("split-bloom-filter:%s:split:%s".formatted(splitBloomFilter.getId(), splitIndex));
				assertThat(result).isNotNull();
			}
		}
	}

	@Test
	void add() {
		// given
		SplitShardedBloomFilter splitShardedBloomFilter = SplitShardedBloomFilter.create("testId", 1_000, 0.01, 4);

		// when
		splitShardedBloomFilterRedisHandler.add(splitShardedBloomFilter, "value");

		// then
		SplitBloomFilter shard = splitShardedBloomFilter.findShard("value");
		List<Long> hashedIndexes = shard.getBloomFilter().hash("value");
		for (long offset = 0; offset < shard.getBloomFilter().getBitSize(); offset++) {
			long splitIndex = shard.findSplitIndex(offset);
			Boolean result = redisTemplate.opsForValue().getBit("split-bloom-filter:%s:split:%s".formatted(shard.getId(), splitIndex), offset % SplitBloomFilter.BIT_SPLIT_UNIT);
			assertThat(result).isEqualTo(hashedIndexes.contains(offset));
		}
	}

	@Test
	void delete() {
		// given
		SplitShardedBloomFilter splitShardedBloomFilter = SplitShardedBloomFilter.create("testId", 1_000, 0.01, 4);
		splitShardedBloomFilterRedisHandler.add(splitShardedBloomFilter, "value");

		// when
		splitShardedBloomFilterRedisHandler.delete(splitShardedBloomFilter);

		// then
		List<SplitBloomFilter> shards = splitShardedBloomFilter.getShards();
		for (SplitBloomFilter splitBloomFilter : shards) {
			for (long offset = 0; offset < splitBloomFilter.getBloomFilter().getBitSize(); offset++) {
				long splitIndex = splitBloomFilter.findSplitIndex(offset);
				Boolean result = redisTemplate.opsForValue().getBit("split-bloom-filter:%s:split:%s".formatted(splitBloomFilter.getId(), splitIndex), offset);
				assertThat(result).isFalse();
			}
		}
	}
}