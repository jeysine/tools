package cn.com.architecture.listener.impl;

import cn.com.architecture.entity.Event;
import cn.com.architecture.listener.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TestListener implements EventListener {
	private static Logger logger = LoggerFactory.getLogger(TestListener.class);

	@Override
	public String getId() {
		return "TEST_LISTENER";
	}

	@Override
	public void handleEvent(Event event) {
		logger.debug("handler: {}", event);
		logger.info("处理监听器逻辑");
	}
}
