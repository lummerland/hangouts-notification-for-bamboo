package org.lummerland.bamboo.hangoutsNotifications;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.atlassian.bamboo.notification.NotificationRecipient.RequiresPlan;
import com.atlassian.bamboo.notification.NotificationRecipient.RequiresResultSummary;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.notification.recipients.AbstractNotificationRecipient;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.resultsummary.ResultsSummary;

public class HangoutsNotificationRecipient extends AbstractNotificationRecipient implements RequiresPlan, RequiresResultSummary {

	private ImmutablePlan plan;
	private ResultsSummary summary;

	@NotNull
	@Override
	public List<NotificationTransport> getTransports() {
		return Collections.singletonList(HangoutsNotificationTransport.build(plan, summary));
	}

	@Override
	public void setPlan(@Nullable final ImmutablePlan immutablePlan) {
		this.plan = immutablePlan;
	}

	@Override
	public void setResultsSummary(@Nullable final ResultsSummary resultsSummary) {
		this.summary = resultsSummary;
	}
}
