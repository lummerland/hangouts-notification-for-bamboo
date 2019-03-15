package org.lummerland.bamboo.hangoutsNotifications;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.notification.Notification;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.google.gson.JsonObject;

public class HangoutsNotificationTransport implements NotificationTransport {

	private static final Logger log = LoggerFactory.getLogger(HangoutsNotificationTransport.class);

	private ImmutablePlan plan;
	private ResultsSummary resultsSummary;
	private String webhookUrl;

	private HangoutsNotificationTransport(
			final String webhookUrl,
			final ImmutablePlan plan,
			final ResultsSummary summary) {
		this.webhookUrl = webhookUrl;
		this.plan = plan;
		this.resultsSummary= summary;
	}

	static HangoutsNotificationTransport build(final String webhookUrl, final ImmutablePlan plan, final ResultsSummary summary) {
		return new HangoutsNotificationTransport(webhookUrl, plan, summary);
	}

	@Override
	public void sendNotification(@NotNull final Notification notification) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty(
				"text",
				notification.getIMContent() +
						"Duration: " + resultsSummary.getDurationDescription());

		CloseableHttpClient client = HttpClients.createDefault();
		StringEntity requestEntity = new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON);
		HttpPost post = new HttpPost(webhookUrl);
		post.setEntity(requestEntity);
		try {
			log.debug("> send request");
			CloseableHttpResponse response = client.execute(post);
			log.debug("> got {} response: {}", response.getStatusLine().getStatusCode(), response);
		}
		catch (IOException e) {
			log.error("> Error sending notification: {}", e);
		}
	}
}
