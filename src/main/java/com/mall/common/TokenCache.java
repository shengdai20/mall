package com.mall.common;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by cq on 2017/11/1.
 */
public class TokenCache {

    //打印日志
    private static Logger logger = LoggerFactory.getLogger(TokenCache.class);

    public static final String TOKEN_PREFIX = "token_";

    //LRU算法
    //initialCapacity(1000)设置cache的初始大小为1000
    //maximumSize(10000)设置缓存个数为10000，当个数超过10000会利用LRU算法删除部分缓存
    //expireAfterAccess(12, TimeUnit.HOURS)设置cache中的数据在写入之后的存活时间为12小时，TimeUnit.HOURS表示12的单位是小时
    //key和value都是String类型
    private static LoadingCache<String, String> localCache = CacheBuilder.newBuilder().initialCapacity(1000).maximumSize(10000).expireAfterAccess(12, TimeUnit.HOURS).build(
            new CacheLoader<String, String>() {
                //默认的数据加载实现，当调用get取值的时候，如果key没有对应的值，就调用这个方法进行加载
                //当本地缓存命没有中时，调用load方法获取结果并将结果缓存
                @Override
                public String load(String s) throws Exception {
                    //因为key和value都是String类型,在下面调用getKey时返回的value值也是String类型，虽然有可能是null值但是也是string类型，所以这里返回"null"而不是null
                    return "null";
                }
            }
    );

    public static void setKey(String key, String value) {
        localCache.put(key, value);
    }

    public static String getKey(String key) {
        String value = null;
        try {
            value = localCache.get(key);
            //这里用"null"字符串来进行判空
            if("null".equals(value)) {
                return null;
            }
            return value;
        } catch (Exception e) {
            logger.error("localCache get error", e);
        }
        return null;
    }
}
