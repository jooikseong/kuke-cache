package kuke.cache.common.distributedlock;

import kuke.cache.RedisTestContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DistributedLockProviderTest extends RedisTestContainerSupport {
	@Autowired
	DistributedLockProvider distributedLockProvider;

	@Test
	void lock() throws InterruptedException {
		assertThat(distributedLockProvider.lock("testId", Duration.ofSeconds(1))).isTrue();
		assertThat(distributedLockProvider.lock("testId", Duration.ofSeconds(1))).isFalse();
		assertThat(distributedLockProvider.lock("testId", Duration.ofSeconds(1))).isFalse();

		TimeUnit.SECONDS.sleep(2);
		assertThat(distributedLockProvider.lock("testId", Duration.ofSeconds(1))).isTrue();
	}

	@Test
	void lock_shouldAcquireOnlyOnce_whenMultiThread() throws InterruptedException {
		ExecutorService executorService = Executors.newFixedThreadPool(10);
		CountDownLatch latch = new CountDownLatch(10);
		AtomicInteger acquiredCount = new AtomicInteger(0);
		for (int i = 0; i < 10; i++) {
			executorService.execute(() -> {
				boolean locked = distributedLockProvider.lock("testId", Duration.ofSeconds(10));
				if (locked) {
					acquiredCount.incrementAndGet();
				}
				latch.countDown();
			});
		}

		latch.await();

		// then
		assertThat(acquiredCount.get()).isEqualTo(1);
	}

	@Test
	void unlock() {
		// given
		distributedLockProvider.lock("testId", Duration.ofSeconds(1));

		// when
		distributedLockProvider.unlock("testId");

		// then
		boolean locked = distributedLockProvider.lock("testId", Duration.ofSeconds(1));
		assertThat(locked).isTrue();
	}
}