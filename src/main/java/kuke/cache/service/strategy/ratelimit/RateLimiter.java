package kuke.cache.service.strategy.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RateLimiter {
	private final StringRedisTemplate redisTemplate;

	/**
	 * 첫 요청 시점부터 perSeconds 동안 요청량 제한
	 */
	public boolean isAllowed(String id, long limit, long perSeconds) {
		String key = genKey(id);
		Long countResult = redisTemplate.opsForValue().increment(key);
		if (countResult == null) {
			return false;
		}

		long count = countResult;
		if (count == 1) {
			redisTemplate.expire(key, Duration.ofSeconds(perSeconds));
		}

		if (count <= limit) {
			return true;
		}

		// if count == 1에서 expire 실패한 상황일 수 있으므로 최소한의 방어 로직
		if (count % (limit / 10) == 0 && redisTemplate.getExpire(key) == -1) {
			redisTemplate.expire(key, Duration.ofSeconds(perSeconds));
		}

		return false;
	}

	private String genKey(String id) {
		return "rate-limit:" + id;
	}
}
