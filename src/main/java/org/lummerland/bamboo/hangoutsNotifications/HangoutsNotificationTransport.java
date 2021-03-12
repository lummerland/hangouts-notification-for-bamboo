package org.lummerland.bamboo.hangoutsNotifications;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.artifact.MutableArtifact;
import com.atlassian.bamboo.build.artifact.ArtifactLink;
import com.atlassian.bamboo.build.artifact.ArtifactLinkDataProvider;
import com.atlassian.bamboo.build.artifact.ArtifactLinkManager;
import com.atlassian.bamboo.builder.BuildState;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.notification.Notification;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.atlassian.spring.container.ContainerManager;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HangoutsNotificationTransport implements NotificationTransport {

	private static final String COLORED_STRING = "<font color='%s'>%s</font>";
	private static final String BUILD_TEMPLATE = "/templates/buildNotification.ftl";
	private static final String DEPLOYMENT_TEMPLATE = "/templates/deploymentNotification.ftl";

	private final ConfigDto config;
	private final ResultsSummary resultsSummary;
	private final TemplateRenderer templateRenderer;
	private final ArtifactLinkManager artifactLinkManager;
	private final DeploymentResult deploymentResult;

	private final String baseUrl;

	private HangoutsNotificationTransport(
			final ConfigDto config,
			final ResultsSummary summary,
			final TemplateRenderer templateRenderer,
			final ArtifactLinkManager artifactLinkManager,
			final DeploymentResult deploymentResult
	) {
		this.config = config;
		this.resultsSummary = summary;
		this.templateRenderer = templateRenderer;
		this.artifactLinkManager = artifactLinkManager;
		this.deploymentResult = deploymentResult;
		this.baseUrl = getAdministrationConfiguration().getBaseUrl();
		log.debug("> created notification transport with config {}", config);
	}

	static HangoutsNotificationTransport build(
			final ConfigDto config,
			final ResultsSummary summary,
			final TemplateRenderer templateRenderer,
			final ArtifactLinkManager artifactLinkManager,
			final DeploymentResult deploymentResult
	) {
		return new HangoutsNotificationTransport(config, summary, templateRenderer, artifactLinkManager, deploymentResult);
	}

	private static AdministrationConfiguration getAdministrationConfiguration() {
		return (AdministrationConfiguration) ContainerManager.getComponent("administrationConfiguration");
	}

	@Override
	public void sendNotification(@NotNull final Notification notification) {

		// break on deployment notifications
		if(resultsSummary == null) {
			log.info("Only build notifications are supported right now, deployment notifications may come.");
			return;
		}

		try (final CloseableHttpClient client = HttpClients.createDefault()) {
			final String threadKey = (resultsSummary != null)
					? ChatThreadKey.forBuild(resultsSummary)
					:	ChatThreadKey.forDeployment(deploymentResult);

			final HttpPost post = new HttpPost(config.getWebhookUrl() + "&threadKey=" + threadKey);

			final String message = (resultsSummary != null)
					? getMessageJson()
					: getMessageForDeployment();

			final StringEntity requestEntity = new StringEntity(message, ContentType.APPLICATION_JSON);
			post.setEntity(requestEntity);
			log.debug("> send request");
			final CloseableHttpResponse response = client.execute(post);
			log.debug("> got {} response: {}", response.getStatusLine().getStatusCode(), response);
		}
		catch (final IOException e) {
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
		if (config.isShowNotificationReason()) {
			context.put("reason", replaceQuotes(resultsSummary.getReasonSummary()));
		}
		if (config.isShowTestsSummary()) {
			context.put("tests", replaceQuotes(resultsSummary.getTestSummary()));
		}
		if (config.isShowBuildDuration()) {
			context.put("buildDuration", resultsSummary.getDurationDescription());
		}

		// TODO: Debug or log everything to see what data is available

		if (config.isShowChanges() && isNotBlank(resultsSummary.getChangesListSummary())) {
			context.put("changes", replaceQuotes(resultsSummary.getChangesListSummary()));
		}

		if (config.isMentionAllOnFailed() && resultsSummary.getBuildState() == BuildState.FAILED) {
			context.put("mentionAllUsers", "mentionAllUsers");
		}

		if (resultsSummary.getBuildState() == BuildState.SUCCESS) {
			// TODO: This currently only works for Job notifications. We need to look into ChainResultsSummary...
			final String[] artifacts = resultsSummary.getArtifactLinks().stream()
					.map(ArtifactLink::getArtifact)
					.map(this::getArtifactLinkHtml)
					.filter(Objects::nonNull)
					.toArray(String[]::new);

			if (artifacts.length > 0) {
				context.put("artifacts", artifacts);
			}
		}

		final String rendered = templateRenderer.render(BUILD_TEMPLATE, context);
		log.debug(">>> Rendered template:\n {}", rendered);
		return rendered;
	}

	private String getArtifactLinkHtml(final MutableArtifact artifact) {
		final ArtifactLinkDataProvider artifactLinkDataProvider = artifactLinkManager.getArtifactLinkDataProvider(artifact);
		if (artifactLinkDataProvider == null || isBlank(artifactLinkDataProvider.getRootUrl())) {
			return null;
		}
		final String url = artifactLinkDataProvider.getRootUrl().replaceFirst("BASE_URL", baseUrl);
		return "<a href='" + url + "'>" + artifact.getLabel() + "</a>";
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
