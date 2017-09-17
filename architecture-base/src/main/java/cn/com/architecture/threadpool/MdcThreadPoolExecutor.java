package cn.com.architecture.threadpool;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 会协助处理传递MDC的ThreadPoolExecutor
 *
 *
 */
public class MdcThreadPoolExecutor extends ThreadPoolExecutor {
	final private boolean useFixedContext;
	final private Map<String, String> fixedContext;

	/**
	 * Pool where task threads take MDC from the submitting thread.
	 */
	public static MdcThreadPoolExecutor newWithInheritedMdc(int corePoolSize, int maximumPoolSize, long keepAliveTime,
															TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		return new MdcThreadPoolExecutor(null, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	/**
	 * Pool where task threads take fixed MDC from the thread that creates the
	 * pool. 使用创建thread pool时的MDC
	 */
	public static MdcThreadPoolExecutor newWithCurrentMdc(int corePoolSize, int maximumPoolSize, long keepAliveTime,
														  TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		return new MdcThreadPoolExecutor(MDC.getCopyOfContextMap(), corePoolSize, maximumPoolSize, keepAliveTime, unit,
				workQueue);
	}

	/**
	 * Pool where task threads always have a specified, fixed MDC. 创建thread
	 * pool时指定的context
	 */
	public static MdcThreadPoolExecutor newWithFixedMdc(Map<String, String> fixedContext, int corePoolSize,
														int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		return new MdcThreadPoolExecutor(fixedContext, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	private MdcThreadPoolExecutor(Map<String, String> fixedContext, int corePoolSize, int maximumPoolSize,
								  long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
		this.fixedContext = fixedContext;
		useFixedContext = (fixedContext != null);
	}

	private Map<String, String> getContextForTask() {
		return useFixedContext ? fixedContext : MDC.getCopyOfContextMap();
	}

	/**
	 * All executions will have MDC injected. {@code ThreadPoolExecutor}'s
	 * submission methods ({@code submit()} etc.) all delegate to this.
	 */
	@Override
	public void execute(Runnable command) {
		super.execute(wrap(command, getContextForTask()));
	}

	public static Runnable wrap(final Runnable runnable, final Map<String, String> context) {
		return new Runnable() {
			@Override
			public void run() {
				Map<String, String> previous = MDC.getCopyOfContextMap();
				if (context == null) {
					MDC.clear();
				} else {
					MDC.setContextMap(context);
				}
				try {
					runnable.run();
				} finally {
					if (previous == null) {
						MDC.clear();
					} else {
						MDC.setContextMap(previous);
					}
				}
			}
		};
	}
}

