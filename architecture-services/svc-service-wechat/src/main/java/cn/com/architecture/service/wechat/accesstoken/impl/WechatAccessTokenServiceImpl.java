package cn.com.architecture.service.wechat.accesstoken.impl;

import cn.com.architecture.config.wechat.WechatConfigSecret;
import cn.com.architecture.constants.wechat.WechatConfigParams;
import cn.com.architecture.entity.wechat.acesstoken.WechatAccessToken;
import cn.com.architecture.entity.wechat.callbackip.WechatCallBackIp;
import cn.com.architecture.service.wechat.accesstoken.WechatAccessTokenService;
import cn.com.architecture.service.wechat.callbackip.WechatCallBackIpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import utils.okhttputil.OkHttpUtils;

import java.io.IOException;
import java.util.UUID;

@Service("wechatAccessTokenService")
public class WechatAccessTokenServiceImpl implements WechatAccessTokenService {
	@Autowired
	private JedisPool jedisPool;

	private static Logger logger = LoggerFactory.getLogger(WechatAccessTokenServiceImpl.class);

	private static final ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private WechatCallBackIpService wechatCallBackIpService;

	@Override
	public WechatAccessToken getAccessToken() {
		WechatAccessToken wechatAccessToken = null;
		Jedis jedis = jedisPool.getResource();
		if (jedis == null) {
			logger.error("Redis is not reachable");
		}
		logger.debug("start to get accessToken");
		try {
			do {
				Long accessTokenExpired = jedis.ttl(WechatConfigParams.ACCESS_TOKEN_KEY);
				String accessToken = jedis.get(WechatConfigParams.ACCESS_TOKEN_KEY);

				if (accessToken != null && accessTokenExpired > 0) {
					wechatAccessToken =  new WechatAccessToken(accessToken, accessTokenExpired);
					logger.debug("get access token from redis success, access token:{} expired:{}",accessToken,accessTokenExpired);
					//wechatAccessToken = judgeAccessTokenActive(wechatAccessToken, jedis);

				} else {
					//logger.debug("Redis is not exists access token , start get lock to get access token from cn.com.dingduoduo.api.admin.wechat.");
					String mutexKeyValue = UUID.randomUUID().toString();
					if (isLeader(jedis, mutexKeyValue)) {
						wechatAccessToken = getAccessTokenFromWechat(jedis);
					}
				}
			} while (wechatAccessToken == null);
		} finally {
			jedis.close();
		}
		return wechatAccessToken;
	}
	private Boolean isLeader(Jedis jedis, String mutexKeyValue) {
		Long mutexKeyExpired = 10L;
		String existsMutexKeyValue = jedis.get(WechatConfigParams.ACCESS_TOKEN_KEY_MUTEX);

		//防止某线程或进程第一次获得leader但获取或设置accessToken失败而进行第二次设置
		if (existsMutexKeyValue != null && mutexKeyValue.equals(existsMutexKeyValue)) {
			logger.debug("set access token not need lock");
			return true;
		} else if (existsMutexKeyValue != null){
			logger.debug("get leader failed");
			return false;
		}

		String result = jedis.set(WechatConfigParams.ACCESS_TOKEN_KEY_MUTEX, mutexKeyValue, "NX", "EX", mutexKeyExpired);
		if (result != null) {
			logger.debug("get leader success");
			return true;
		} else {
			logger.debug("get leader failed");
		}
		return false;
	}

	private WechatAccessToken getAccessTokenFromWechat(Jedis jedis)  {
		String url = WechatConfigParams.WECHAT_GET_ACCESSTOKEN_URL.replace("APPID", WechatConfigSecret.getWechatAppId())
				.replace("APPSECRET", WechatConfigSecret.getWechatSecret());

		Response response = null;
		WechatAccessToken wechatAccessToken = null;
		logger.debug("get access token from wechat start:{}", url);
		try {
			response = OkHttpUtils.get().url(url)
					.build().execute();

			String result = response.body().string();
			wechatAccessToken = mapper.readValue(result, WechatAccessToken.class);
			logger.debug("get access token from wechat success:{}", wechatAccessToken);
			if (wechatAccessToken.getAccessToken() == null) {
				logger.error("get access token from wechat error, errcode:{},errmsg:{}", wechatAccessToken.getErrcode(),wechatAccessToken.getErrmsg());
				return null;
			}

			wechatAccessToken.setExpiresIn(
					wechatAccessToken.getExpiresIn());
			logger.debug("get wechat access token success, accessToken:{}, accessToken expire time: {}", wechatAccessToken.getAccessToken(), wechatAccessToken.getExpiresIn());
			setAccessTokenToRedis(wechatAccessToken, jedis);
		} catch (IOException e) {
			logger.error("visit wechat get access token failed:{}", e);
		}


		return wechatAccessToken;
	}

	private void setAccessTokenToRedis(WechatAccessToken accessToken, Jedis jedis) throws IOException {
		logger.debug("set access token to redis start.");
		Long accessTokenExpiredIn = accessToken.getExpiresIn() - 60L;
		if (accessTokenExpiredIn < 0) {
			return;
		}
		jedis.del(WechatConfigParams.ACCESS_TOKEN_KEY);
		String result = jedis.set(WechatConfigParams.ACCESS_TOKEN_KEY, accessToken.getAccessToken(), "NX", "EX", accessTokenExpiredIn);
		logger.debug("result:{}",result);
		if (result != null) {
			logger.debug("set access token to redis success, accessToken:{}, expired: {}.", accessToken.getAccessToken(),accessTokenExpiredIn);
			jedis.del(WechatConfigParams.ACCESS_TOKEN_KEY_MUTEX);
		} else {
			logger.debug("access token had saved.");
		}
	}

	/**
	 * 若授权微信权限给第三方, 需在此调用一个无限制次数的接口判断accessToken是否有效
	 * @return
	 */
	private WechatAccessToken judgeAccessTokenActive(WechatAccessToken wechatAccessToken, Jedis jedis){
		WechatCallBackIp wechatCallBackIp = null;
		try {
			wechatCallBackIp = wechatCallBackIpService.getWechatCallBackIp(wechatAccessToken);
		} catch (IOException e) {
			logger.error("error :{}", e);
		}
		if (wechatCallBackIp.getIpList() == null) {
			jedis.del(WechatConfigParams.ACCESS_TOKEN_KEY);
			logger.debug("wechat access token is not active, try to get from wechat");
			return null;
		}
		return wechatAccessToken;
	}

	public static void main(String[] args) {
	}
}
