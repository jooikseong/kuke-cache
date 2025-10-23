package kuke.cache.common.cache;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class KukeCacheKeyGeneratorTest {
	KukeCacheKeyGenerator kukeCacheKeyGenerator = new KukeCacheKeyGenerator();

	@Test
	void genKey_shouldEvaluateSpELWithMethodArgs() {
		// given
		JoinPoint joinPoint = mock(JoinPoint.class);
		MethodSignature methodSignature = mock(MethodSignature.class);

		given(joinPoint.getSignature()).willReturn(methodSignature);
		given(methodSignature.getParameterNames()).willReturn(new String[]{"itemId", "userId"});
		given(joinPoint.getArgs()).willReturn(new Object[]{42L, 99L});

		CacheStrategy strategy = CacheStrategy.NONE;
		String cacheName = "item";
		String keySpel = "#itemId + ':' + #userId"; // SpEL

		// when
		String key = kukeCacheKeyGenerator.genKey(joinPoint, strategy, cacheName, keySpel);

		// then
		assertThat(key).isEqualTo("NONE:item:42:99");
	}
}