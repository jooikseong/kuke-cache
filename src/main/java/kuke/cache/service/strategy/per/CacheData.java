package kuke.cache.service.strategy.per;

import kuke.cache.common.serde.DataSerializer;
import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;
import java.util.random.RandomGenerator;

@Getter
@ToString
public class CacheData {
	private String data;
	private long computationTimeMillis; // delta
	private long expiredAtMillis; // expiry

	public static CacheData of(Object data, long computationTimeMillis, Duration ttl) {
		CacheData cacheData = new CacheData();
		cacheData.data = DataSerializer.serializeOrException(data);
		cacheData.computationTimeMillis = computationTimeMillis;
		cacheData.expiredAtMillis = Instant.now().plus(ttl).toEpochMilli();
		return cacheData;
	}

	public <T> T parseData(Class<T> dataType) {
		return DataSerializer.deserializeOrNull(data, dataType);
	}

	public boolean shouldRecompute(double beta) {
		long nowMillis = Instant.now().toEpochMilli();
		double rand = RandomGenerator.getDefault().nextDouble();
		return nowMillis - computationTimeMillis * beta * Math.log(rand) >= expiredAtMillis;
	}
}
