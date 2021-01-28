package org.lummerland.bamboo.hangoutsNotifications;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.chains.branches.BranchStatusService;
import com.atlassian.bamboo.notification.Notification;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.google.common.collect.Maps;

public class HangoutsNotificationTransport implements NotificationTransport {

	private static final Logger log = LoggerFactory.getLogger(HangoutsNotificationTransport.class);

	private static final String COLORED_STRING = "<font color='%s'>%s</font>";
	private static final String FREEMARKER_TEMPLATE = "/templates/hangoutsNotification.ftl";

	private final String webhookUrl;
	private final ResultsSummary resultsSummary;
	private final TemplateRenderer templateRenderer;
	private final BranchStatusService branchStatusService;

	private HangoutsNotificationTransport(
			final String webhookUrl,
			final ResultsSummary summary,
			final TemplateRenderer templateRenderer,
			final BranchStatusService branchStatusService
	) {
		this.webhookUrl = webhookUrl;
		this.resultsSummary = summary;
		this.templateRenderer = templateRenderer;
		this.branchStatusService = branchStatusService;
	}

	static HangoutsNotificationTransport build(
			final String webhookUrl,
			final ResultsSummary summary,
			final TemplateRenderer templateRenderer,
			final BranchStatusService branchStatusService
	) {
		return new HangoutsNotificationTransport(webhookUrl, summary, templateRenderer, branchStatusService);
	}

	@Override
	public void sendNotification(@NotNull final Notification notification) {
		try (final CloseableHttpClient client = HttpClients.createDefault()) {
			final HttpPost post = new HttpPost(webhookUrl + "&threadKey=" + new ChatThreadKey(resultsSummary).get());
			final StringEntity requestEntity = new StringEntity(getMessageJson(), ContentType.APPLICATION_JSON);
			post.setEntity(requestEntity);
			log.debug("> send request");
			final CloseableHttpResponse response = client.execute(post);
			log.debug("> got {} response: {}", response.getStatusLine().getStatusCode(), response);
		}
		catch (final IOException e) {
			log.error("> Error sending notification", e);
		}
	}

	private String getMessageJson() {
		final ImmutablePlan plan = resultsSummary.getImmutablePlan();
		final Map<String, Object> context = Maps.newHashMap();
		context.put("projectName", plan.getProject().getName());
		context.put("planName", plan.getName());
		context.put("projectKey", plan.getProject().getKey());
		context.put("planKey", resultsSummary.getPlanKey().toString());
		context.put("buildNumber", resultsSummary.getBuildNumber());
		context.put("buildState", getBuildState(resultsSummary));
		context.put("buildDuration", resultsSummary.getDurationDescription());
		context.put("reason", replaceQuotes(resultsSummary.getReasonSummary()));
		context.put("tests", replaceQuotes(resultsSummary.getTestSummary()));

		// TODO: Debug or log everything to see what data is available

		//if (branchStatusService.shouldDisplayBranchStatusLink(plan)) {
			//context.put("branchStatusLink", branchStatusService.getBranchStatusLinkInfo(plan, null).getPath());
		//}
		//log.debug(">>> BRANCH STATUS: {}", branchStatusService.getBranchStatusLinkInfo(plan,null).getPath());
		/*log.debug(">>> DISPLAY BRANCH STATUS LINK? {}", branchStatusService.shouldDisplayBranchStatusLink(plan));
		log.debug(">>> BRANCH STATUS LINK INFO: {}", branchStatusService.getBranchStatusLinkInfo(plan,null));
		log.debug(">>> Show link? {}", branchStatusService.getBranchStatusLinkInfo(plan,null).isShowLink());*/

		if (isNotBlank(resultsSummary.getChangesListSummary())) {
			context.put("changes", replaceQuotes(resultsSummary.getChangesListSummary()));
		}

		return templateRenderer.render(FREEMARKER_TEMPLATE, context);
	}

	private String getBuildState(final ResultsSummary resultsSummary) {
		final BuildState buildState = resultsSummary.getBuildState();
		if (buildState == BuildState.SUCCESS) {
			return String.format(COLORED_STRING, "#00aa00", "&#10004; " + buildState.toString());
		}
		if (buildState == BuildState.FAILED) {
			return String.format(COLORED_STRING, "#aa0000", "&#10008; " + buildState.toString());
		}
		return buildState.toString();
	}

	private String replaceQuotes(final String text) {
		return text.replace("\"", "\\\"");
	}

}
