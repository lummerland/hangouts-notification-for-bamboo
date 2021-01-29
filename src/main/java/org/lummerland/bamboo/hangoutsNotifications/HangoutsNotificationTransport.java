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

import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.chains.branches.BranchStatusService;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.notification.Notification;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HangoutsNotificationTransport implements NotificationTransport {

	private static final String COLORED_STRING = "<font color='%s'>%s</font>";
	private static final String FREEMARKER_TEMPLATE = "/templates/hangoutsNotification.ftl";
	private static final String DEPLOYMENT_TEMPLATE = "/templates/deploymentNotification.ftl";

	private final ConfigDto config;
	private final ResultsSummary resultsSummary;
	private final TemplateRenderer templateRenderer;
	private final BranchStatusService branchStatusService;
	private final DeploymentResult deploymentResult;

	private HangoutsNotificationTransport(
			final ConfigDto config,
			final ResultsSummary summary,
			final TemplateRenderer templateRenderer,
			final BranchStatusService branchStatusService,
			final DeploymentResult deploymentResult
	) {
		this.config = config;
		this.resultsSummary = summary;
		this.templateRenderer = templateRenderer;
		this.branchStatusService = branchStatusService;
		this.deploymentResult = deploymentResult;
		log.debug(">>> created notification transport with config {}", config);
	}

	static HangoutsNotificationTransport build(
			final ConfigDto config,
			final ResultsSummary summary,
			final TemplateRenderer templateRenderer,
			final BranchStatusService branchStatusService,
			final DeploymentResult deploymentResult
	) {
		return new HangoutsNotificationTransport(config, summary, templateRenderer, branchStatusService, deploymentResult);
	}

	@Override
	public void sendNotification(@NotNull final Notification notification) {
		log.debug(">> send notification to google chat");
		try (final CloseableHttpClient client = HttpClients.createDefault()) {
			final String threadKey = (resultsSummary != null)
					? ChatThreadKey.forBuild(resultsSummary)
					:	ChatThreadKey.forDeployment(deploymentResult);

			final HttpPost post = new HttpPost(config.getUrl());

			final String message = (resultsSummary != null)
					? getMessageJson()
					: getMessageForDeployment();

			final StringEntity requestEntity = new StringEntity(message, ContentType.APPLICATION_JSON);
			post.setEntity(requestEntity);
			log.debug("> send request");
			final CloseableHttpResponse response = client.execute(post);
			log.debug("> got {} response: {}", response.getStatusLine().getStatusCode(), response);
		}
		catch (final Exception e) {
			log.error("> Error sending notification", e);
		}
	}

	private String getMessageForDeployment() {
		final Map<String, Object> context = Maps.newHashMap();
		context.put("environment", deploymentResult.getEnvironment().getName());
		context.put("deploymentVersionName", deploymentResult.getDeploymentVersionName());
		context.put("state", getDeployState());
		final String rendered = templateRenderer.render(DEPLOYMENT_TEMPLATE, context);
		log.debug(">>>Rendered template:\n {}", rendered);
		return rendered;
	}

	private String getDeployState() {
		final BuildState buildState = deploymentResult.getDeploymentState();
		if (buildState == BuildState.SUCCESS) {
			return String.format(COLORED_STRING, "#00aa00", "&#10004; " + buildState.toString());
		}
		if (buildState == BuildState.FAILED) {
			return String.format(COLORED_STRING, "#aa0000", "&#10008; " + buildState.toString());
		}
		return buildState.toString();
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
		if (config.isShowR()) {
			context.put("reason", replaceQuotes(resultsSummary.getReasonSummary()));
		}
		if (config.isShowTS()) {
			context.put("tests", replaceQuotes(resultsSummary.getTestSummary()));
		}
		if (config.isShowBD()) {
			context.put("buildDuration", resultsSummary.getDurationDescription());
		}

		// TODO: Debug or log everything to see what data is available

		//if (branchStatusService.shouldDisplayBranchStatusLink(plan)) {
			//context.put("branchStatusLink", branchStatusService.getBranchStatusLinkInfo(plan, null).getPath());
		//}
		//log.debug(">>> BRANCH STATUS: {}", branchStatusService.getBranchStatusLinkInfo(plan,null).getPath());
		/*log.debug(">>> DISPLAY BRANCH STATUS LINK? {}", branchStatusService.shouldDisplayBranchStatusLink(plan));
		log.debug(">>> BRANCH STATUS LINK INFO: {}", branchStatusService.getBranchStatusLinkInfo(plan,null));
		log.debug(">>> Show link? {}", branchStatusService.getBranchStatusLinkInfo(plan,null).isShowLink());*/

		if (config.isShowC() && isNotBlank(resultsSummary.getChangesListSummary())) {
			context.put("changes", replaceQuotes(resultsSummary.getChangesListSummary()));
		}

		if (config.isAtAll() && resultsSummary.getBuildState() == BuildState.FAILED) {
			context.put("mentionAllUsers", "mentionAllUsers");
		}

		final String rendered = templateRenderer.render(FREEMARKER_TEMPLATE, context);
		log.debug(">>>Rendered template:\n {}", rendered);
		return rendered;
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
