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

/**
 * [KukeCacheAspect]
 * 공통 캐시 처리를 위한 AOP 클래스.
 * 비즈니스 로직과 캐시 인프라 로직을 분리하여 유지보수성을 극대화함.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class KukeCacheAspect {
	// 전략 패턴: 다양한 캐시 저장소(Redis, Local 등)를 유연하게 처리하기 위한 핸들러 리스트
	private final List<KukeCacheHandler> kukeCacheHandlers;
	// 단일 책임 원칙: 캐시 키 생성 로직을 별도로 분리하여 관리
	private final KukeCacheKeyGenerator kukeCacheKeyGenerator;

	/**
	 * 조회용 캐시 처리 (Read-Through 패턴 기반)
	 */
	@Around("@annotation(kukeCacheable)")
	public Object handleCacheable(ProceedingJoinPoint joinPoint, KukeCacheable kukeCacheable) {
		CacheStrategy cacheStrategy = kukeCacheable.cacheStrategy();
		// 런타임에 적절한 캐시 엔진(핸들러)을 선택 (OCP 준수)
		KukeCacheHandler cacheHandler = findCacheHandler(cacheStrategy);

		// 어노테이션에 정의된 정보를 바탕으로 고유한 캐시 키 생성
		String key = kukeCacheKeyGenerator.genKey(joinPoint, cacheStrategy, kukeCacheable.cacheName(), kukeCacheable.key());
		Duration ttl = Duration.ofSeconds(kukeCacheable.ttlSeconds());

		// 핵심: 지연 실행(Lazy Execution)을 위해 실제 DB 조회 로직을 Supplier로 감쌈
		Supplier<Object> dataSourceSupplier = createDataSourceSupplier(joinPoint);
		Class returnType = findReturnType(joinPoint);

		try {
			log.info("[KukeCacheAspect.handleCacheable] key={}", key);

			/**
			 * cacheHandler.fetch 내부 로직 예상:
			 * 1. 캐시에서 데이터 조회 (Hit 시 바로 반환)
			 * 2. Miss 시 dataSourceSupplier.get() 호출하여 DB 조회
			 * 3. 조회 결과를 캐시에 저장 후 반환
			 */
			return cacheHandler.fetch(
					key,
					ttl,
					dataSourceSupplier,
					returnType
			);
		} catch (Exception e) {
			// 장애 전파 방지: 캐시 서버 장애 시에도 원본 데이터(DB) 조회가 가능하도록 예외 처리(Fall-back)
			log.error("[KukeCacheAspect.handleCacheable] key={}", key, e);
			return dataSourceSupplier.get();
		}
	}

	/**
	 * 특정 전략(Redis, Caffeine 등)을 지원하는 핸들러를 검색
	 */
	private KukeCacheHandler findCacheHandler(CacheStrategy cacheStrategy) {
		return kukeCacheHandlers.stream()
				.filter(handler -> handler.supports(cacheStrategy))
				.findFirst()
				.orElseThrow();
	}

	/**
	 * 실제 메서드 실행(DB 조회 등)을 캡슐화하여 캐시 엔진에 전달
	 * 호출 시점에만 실행되므로 불필요한 연산을 방지함
	 */
	private Supplier<Object> createDataSourceSupplier(ProceedingJoinPoint joinPoint) {
		return () -> {
			try {
				return joinPoint.proceed(); // 실제 타겟 메서드 실행
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		};
	}

	/**
	 * 리플렉션을 통해 리턴 타입을 추출하여 역직렬화(Deserialization) 시 활용
	 */
	private Class findReturnType(JoinPoint joinPoint) {
		Signature signature = joinPoint.getSignature();
		MethodSignature methodSignature = (MethodSignature) signature;
		return methodSignature.getReturnType();
	}

	/**
	 * 캐시 강제 갱신 (Write-Through 또는 단순 업데이트)
	 * 메서드가 성공적으로 실행된 후 결과를 캐시에 반영
	 */
	@AfterReturning(pointcut = "@annotation(kukeCachePut)", returning = "result")
	public void handleCachePut(JoinPoint joinPoint, KukeCachePut kukeCachePut, Object result) {
		CacheStrategy cacheStrategy = kukeCachePut.cacheStrategy();
		KukeCacheHandler cacheHandler = findCacheHandler(cacheStrategy);
		String key = kukeCacheKeyGenerator.genKey(joinPoint, cacheStrategy, kukeCachePut.cacheName(), kukeCachePut.key());

		log.info("[KukeCacheAspect.handleCachePut] key={}", key);
		cacheHandler.put(key, Duration.ofSeconds(kukeCachePut.ttlSeconds()), result);
	}

	/**
	 * 캐시 삭제 (Cache Eviction)
	 * 데이터 수정/삭제 시 연관된 캐시를 무효화하여 데이터 정합성 유지
	 */
	@AfterReturning(pointcut = "@annotation(kukeCacheEvict)")
	public void handleCacheEvict(JoinPoint joinPoint, KukeCacheEvict kukeCacheEvict) {
		CacheStrategy cacheStrategy = kukeCacheEvict.cacheStrategy();
		KukeCacheHandler cacheHandler = findCacheHandler(cacheStrategy);
		String key = kukeCacheKeyGenerator.genKey(joinPoint, cacheStrategy, kukeCacheEvict.cacheName(), kukeCacheEvict.key());

		log.info("[KukeCacheAspect.handleCacheEvict] key={}", key);
		cacheHandler.evict(key);
	}
}