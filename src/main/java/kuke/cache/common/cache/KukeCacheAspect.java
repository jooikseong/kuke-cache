package kuke.cache.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class KukeCacheAspect {
	private final List<KukeCacheHandler> kukeCacheHandlers;
	private final KukeCacheKeyGenerator kukeCacheKeyGenerator;

	@Around("@annotation(kukeCacheable)")
	public Object handleCacheable(ProceedingJoinPoint joinPoint, KukeCacheable kukeCacheable) {
		CacheStrategy cacheStrategy = kukeCacheable.cacheStrategy();
		KukeCacheHandler cacheHandler = findCacheHandler(cacheStrategy);

		String key = kukeCacheKeyGenerator.genKey(joinPoint, cacheStrategy, kukeCacheable.cacheName(), kukeCacheable.key());
		Duration ttl = Duration.ofSeconds(kukeCacheable.ttlSeconds());
		Supplier<Object> dataSourceSupplier = createDataSourceSupplier(joinPoint);
		Class returnType = findReturnType(joinPoint);

		try {
			log.info("[KukeCacheAspect.handleCacheable] key={}", key);
			return cacheHandler.fetch(
				key,
				ttl,
				dataSourceSupplier,
				returnType
			);
		} catch (Exception e) {
			log.error("[KukeCacheAspect.handleCacheable] key={}", key, e);
			return dataSourceSupplier.get();
		}
	}

	private KukeCacheHandler findCacheHandler(CacheStrategy cacheStrategy) {
		return kukeCacheHandlers.stream()
			.filter(handler -> handler.supports(cacheStrategy))
			.findFirst()
			.orElseThrow();
	}

	private Supplier<Object> createDataSourceSupplier(ProceedingJoinPoint joinPoint) {
		return () -> {
			try {
				return joinPoint.proceed();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		};
	}

	private Class findReturnType(JoinPoint joinPoint) {
		Signature signature = joinPoint.getSignature();
		MethodSignature methodSignature = (MethodSignature) signature;
		return methodSignature.getReturnType();
	}

	@AfterReturning(pointcut = "@annotation(kukeCachePut)", returning = "result")
	public void handleCachePut(JoinPoint joinPoint, KukeCachePut kukeCachePut, Object result) {
		CacheStrategy cacheStrategy = kukeCachePut.cacheStrategy();
		KukeCacheHandler cacheHandler = findCacheHandler(cacheStrategy);
		String key = kukeCacheKeyGenerator.genKey(joinPoint, cacheStrategy, kukeCachePut.cacheName(), kukeCachePut.key());
		log.info("[KukeCacheAspect.handleCachePut] key={}", key);
		cacheHandler.put(key, Duration.ofSeconds(kukeCachePut.ttlSeconds()), result);
	}

	@AfterReturning(pointcut = "@annotation(kukeCacheEvict)")
	public void handleCacheEvict(JoinPoint joinPoint, KukeCacheEvict kukeCacheEvict) {
		CacheStrategy cacheStrategy = kukeCacheEvict.cacheStrategy();
		KukeCacheHandler cacheHandler = findCacheHandler(cacheStrategy);
		String key = kukeCacheKeyGenerator.genKey(joinPoint, cacheStrategy, kukeCacheEvict.cacheName(), kukeCacheEvict.key());
		log.info("[KukeCacheAspect.handleCacheEvict] key={}", key);
		cacheHandler.evict(key);
	}
}
