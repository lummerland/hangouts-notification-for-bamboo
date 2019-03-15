package org.lummerland.bamboo.hangoutsNotifications;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.notification.Notification;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.resultsummary.ResultsSummary;

public class HangoutsNotificationTransport implements NotificationTransport {

	private static final Logger log = LoggerFactory.getLogger(HangoutsNotificationTransport.class);

	private ImmutablePlan plan;
	private ResultsSummary resultsSummary;

	private HangoutsNotificationTransport(
			final ImmutablePlan plan,
			final ResultsSummary summary) {
		this.plan = plan;
		this.resultsSummary= summary;
	}

	static HangoutsNotificationTransport build(final ImmutablePlan plan, final ResultsSummary summary) {
		return new HangoutsNotificationTransport(plan, summary);
	}

	@Override
	public void sendNotification(@NotNull final Notification notification) {
		// send this via hangouts webhook
		log.debug("> Send notification for build {} with state {}, duration {}ms & number {};",
				plan.getBuildKey(),
				resultsSummary.getBuildState(),
				resultsSummary.getProcessingDuration(),
				resultsSummary.getBuildNumber());
	}
}
