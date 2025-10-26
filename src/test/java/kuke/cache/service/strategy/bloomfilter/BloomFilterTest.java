package kuke.cache.service.strategy.bloomfilter;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class BloomFilterTest {

	@Test
	void create() {
		BloomFilter bloomFilter1 = BloomFilter.create("testId1", 1000, 0.01);
		assertThat(bloomFilter1.getId()).isEqualTo("testId1");
		assertThat(bloomFilter1.getDataCount()).isEqualTo(1000);
		assertThat(bloomFilter1.getFalsePositiveRate()).isEqualTo(0.01);
		assertThat(bloomFilter1.getBitSize()).isEqualTo(9586);
		assertThat(bloomFilter1.getHashFunctionCount()).isEqualTo(7);
		System.out.println("bloomFilter1 = " + bloomFilter1);

		BloomFilter bloomFilter2 = BloomFilter.create("testId2", 100_000_000, 0.01);
		assertThat(bloomFilter2.getId()).isEqualTo("testId2");
		assertThat(bloomFilter2.getDataCount()).isEqualTo(100_000_000);
		assertThat(bloomFilter2.getFalsePositiveRate()).isEqualTo(0.01);
		assertThat(bloomFilter2.getBitSize()).isEqualTo(958_505_838);
		assertThat(bloomFilter2.getHashFunctionCount()).isEqualTo(7);
		System.out.println("bloomFilter2 = " + bloomFilter2);
	}

	@Test
	void hash() {
		BloomFilter bloomFilter = BloomFilter.create("testId", 1000, 0.01);
		for (int i = 0; i < 100; i++) {
			List<Long> hashedIndexes = bloomFilter.hash("value" + i);
			assertThat(hashedIndexes.size()).isEqualTo(bloomFilter.getHashFunctionCount());
			for (Long hashedIndex : hashedIndexes) {
				assertThat(hashedIndex).isGreaterThanOrEqualTo(0);
				assertThat(hashedIndex).isLessThan(bloomFilter.getBitSize());
				System.out.println("hashedIndex = " + hashedIndex);
			}
			System.out.println();
		}
	}

}