package kuke.cache.service.strategy.bloomfilter;

@FunctionalInterface
public interface BloomFilterHashFunction {
	long hash(String value);
}
