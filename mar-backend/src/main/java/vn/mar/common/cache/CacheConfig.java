package vn.mar.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(PermissionProfileCacheProperties.class)
public class CacheConfig {

    @Bean
    CacheManager cacheManager(PermissionProfileCacheProperties permissionProfileCacheProperties) {
        CaffeineCacheManager manager = new CaffeineCacheManager(CacheNames.PERMISSION_PROFILE);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(permissionProfileCacheProperties.maximumSize())
                .expireAfterWrite(permissionProfileCacheProperties.ttl())
                .recordStats());
        return manager;
    }
}
