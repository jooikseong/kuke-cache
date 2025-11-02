package kuke.cache.service.strategy.applicationlevelshardingreplication;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShardedKeyGeneratorTest {
	ShardedKeyGenerator shardedKeyGenerator = new ShardedKeyGenerator();

	@Test
	void genShardedKeys() {
		// given
		String key = "testKey";
		int shardCount = 3;

		// when
		List<String> shardedKeys = shardedKeyGenerator.genShardedKeys(key, shardCount);

		// then
		assertThat(shardedKeys.size()).isEqualTo(shardCount);
		for (int i = 0; i < shardCount; i++) {
			assertThat(shardedKeys.get(i)).isEqualTo(key + ":" + i);
		}
	}

	@Test
	void findRandomShardedKey() {
		// given
		String key = "testKey";
		int shardCount = 3;

		// when, then
		List<String> shardedKeys = shardedKeyGenerator.genShardedKeys(key, shardCount);
		for (int i = 0; i < 10; i++) {
			String shardedKey = shardedKeyGenerator.findRandomShardedKey(key, shardCount);
			assertThat(shardedKey).isIn(shardedKeys);
			System.out.println("shardedKey = " + shardedKey);
		}
	}
}