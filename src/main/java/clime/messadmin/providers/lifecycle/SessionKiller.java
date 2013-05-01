/**
 * 
 */
package clime.messadmin.providers.lifecycle;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import clime.messadmin.model.Application;
import clime.messadmin.model.IApplicationInfo;
import clime.messadmin.model.Server;
import clime.messadmin.model.Session;
import clime.messadmin.providers.spi.ApplicationLifeCycleProvider;
import clime.messadmin.utils.SessionUtils;

/**
 * Manually expire zombie Tomcat HttpSessions...
 * @author C&eacute;drik LIME
 */
public class SessionKiller implements ApplicationLifeCycleProvider {
	/**
	 * Default time to wait between two sweeps: {@value} ms.
	 */
	public static final long DELAY = 20*60*1000; // 20 minutes
	private static Timer timer;
	private static TimerTask timerTask;
	final static Set/*<String>*/ contexts = Collections.synchronizedSet(new HashSet());

	/**
	 * 
	 */
	public SessionKiller() {
		super();
		maybeInitialize();
	}

	protected static synchronized void maybeInitialize() {
		if (timerTask != null || timer != null) {
			return;
		}
		timerTask = new TimerTask() {
			/**
			 * {@inheritDoc}
			 */
			public void run() {
				try {
					synchronized (contexts) {
						Iterator appIter = contexts.iterator();
						while (appIter.hasNext()) {
							Application application = Server.getInstance().getApplication((String)appIter.next());
							if (application == null) {
								//shouldn't ever happen, but let's play safe
								continue;
							}
							try {
								Iterator sessIter = application.getActiveSessions().iterator();
								while (sessIter.hasNext()) {
									Session session = (Session) sessIter.next();
									if (session.getSessionInfo().getTTL() < 0) {
										reportExpiringSession(application.getApplicationInfo(), session.getSessionInfo());
										session.getSessionInfo().invalidate();
									}
								}
							} catch (RuntimeException rte) {
								//swallow
							}
						}
					}
				} catch (Exception e) {
					//swallow
				}
			}
		};
		timer = new Timer(true);//"Zombie Tomcat HttpSessions killer", true);
		timer.schedule(timerTask, DELAY, DELAY);
	}

	protected static void reportExpiringSession(IApplicationInfo application, HttpSession session) {
		String id = "";
		String contextPath = application.getContextPath();
		try {
			id = session.getId();
		} catch (IllegalStateException ise) {
		}
		System.out.println("MessAdmin INFO: invalidating expired session " + id + " for context " + contextPath);
	}


	/**
	 * {@inheritDoc}
	 */
	public void contextInitialized(ServletContext servletContext) {
		contexts.add(SessionUtils.getContext(servletContext));
	}

	/**
	 * {@inheritDoc}
	 */
	public void contextDestroyed(ServletContext servletContext) {
		contexts.remove(SessionUtils.getContext(servletContext));
	}

	/**
	 * {@inheritDoc}
	 */
	public int getPriority() {
		// no need for a priority, really
		return 0;
	}

}
