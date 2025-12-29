package kuke.cache.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [KukeCacheable]
 * 메서드의 실행 결과를 캐싱하기 위한 커스텀 어노테이션.
 * Spring의 @Cacheable을 사용하지 않고 직접 구현함으로써
 * 프로젝트 특화된 '멀티 저장소 전략'과 '정교한 TTL 관리'를 가능하게 함.
 */
@Target(ElementType.METHOD) // 메서드 단위에 적용
@Retention(RetentionPolicy.RUNTIME) // 런타임 시 AOP(Aspect)가 어노테이션 정보를 참조할 수 있도록 설정
public @interface KukeCacheable {

	/**
	 * 캐시 저장 전략 선택 (예: REDIS, CAFFEINE, LOCAL_MEMORY 등)
	 * KukeCacheAspect에서 적절한 KukeCacheHandler를 찾는 기준이 됨 (전략 패턴의 Key 역할)
	 */
	CacheStrategy cacheStrategy();

	/**
	 * 캐시의 그룹 이름 (Namespace 역할)
	 * 서로 다른 도메인 데이터가 섞이지 않도록 구분하는 논리적 그룹명
	 */
	String cacheName();

	/**
	 * 캐시 키 생성을 위한 식별자
	 * Spring Expression Language(SpEL)와 유사하게 파라미터를 조합하여
	 * 고유한 키를 생성하기 위한 힌트로 사용됨
	 */
	String key();

	/**
	 * 캐시 유지 시간 (Time To Live)
	 * 각 비즈니스 로직의 데이터 특성(휘발성 정도)에 따라
	 * 개별적으로 만료 시간을 설정할 수 있도록 강제함
	 */
	long ttlSeconds();
}