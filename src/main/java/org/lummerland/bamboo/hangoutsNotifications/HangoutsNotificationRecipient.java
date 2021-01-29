package org.lummerland.bamboo.hangoutsNotifications;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.atlassian.bamboo.build.artifact.ArtifactLinkManager;
import com.atlassian.bamboo.chains.branches.BranchStatusService;
import com.atlassian.bamboo.deployments.notification.DeploymentResultAwareNotificationRecipient;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.notification.NotificationRecipient.RequiresPlan;
import com.atlassian.bamboo.notification.NotificationRecipient.RequiresResultSummary;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.notification.recipients.AbstractNotificationRecipient;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.plugin.descriptor.NotificationRecipientModuleDescriptor;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import lombok.extern.slf4j.Slf4j;

@Named
@Slf4j
public class HangoutsNotificationRecipient
		extends AbstractNotificationRecipient
		implements DeploymentResultAwareNotificationRecipient, RequiresPlan, RequiresResultSummary {

	private static final String PARAM_WEBHOOK_URL = "webhookUrl";
	private static final String PARAM_SHOW_REASON = "showReason";
	private static final String PARAM_SHOW_TESTS_SUMMARY = "showTestsSummary";
	private static final String PARAM_SHOW_CHANGES = "showChanges";
	private static final String PARAM_SHOW_BUILD_DURATION = "showBuildDuration";
	private static final String PARAM_MENTION_ALL_ON_FAILED = "mentionAllOnFailed";
	private static final String[] BOOLEAN_DEFAULT = new String[] { "false" };

	private static final Gson GSON = new Gson();
	private static final Type CONFIG_TYPE = new TypeToken<ConfigDto>() {}.getType();

	private ConfigDto config = ConfigDto.builder().build();

	private ImmutablePlan plan;
	private ResultsSummary summary;
	private DeploymentResult deploymentResult;

	@ComponentImport
	private final TemplateRenderer templateRenderer;
	@ComponentImport
	private final ArtifactLinkManager artifactLinkManager;

	@Inject
	public HangoutsNotificationRecipient(
			final TemplateRenderer templateRenderer,
			final ArtifactLinkManager artifactLinkManager
	) {
		this.templateRenderer = templateRenderer;
		this.artifactLinkManager = artifactLinkManager;
	}

	@NotNull
	public List<NotificationTransport> getTransports() {
		return Collections.singletonList(
				HangoutsNotificationTransport.build(config, summary, templateRenderer, artifactLinkManager)
		);
	}

	@Override
	public void populate(@NotNull final Map<String, String[]> params) {
		log.debug("> received parameters: {}", params);
		this.config = ConfigDto.builder()
				.url(params.getOrDefault(PARAM_WEBHOOK_URL, new String[] { "" })[0])
				.showR(Boolean.parseBoolean(params.getOrDefault(PARAM_SHOW_REASON, BOOLEAN_DEFAULT)[0]))
				.showTS(Boolean.parseBoolean(params.getOrDefault(PARAM_SHOW_TESTS_SUMMARY, BOOLEAN_DEFAULT)[0]))
				.showC(Boolean.parseBoolean(params.getOrDefault(PARAM_SHOW_CHANGES, BOOLEAN_DEFAULT)[0]))
				.showBD(Boolean.parseBoolean(params.getOrDefault(PARAM_SHOW_BUILD_DURATION, BOOLEAN_DEFAULT)[0]))
				.atAll(Boolean.parseBoolean(params.getOrDefault(PARAM_MENTION_ALL_ON_FAILED, BOOLEAN_DEFAULT)[0]))
				.build();
		log.debug("> populated config: {}", config);
	}

	@NotNull
	@Override
	public String getRecipientConfig() {
		// serialize config into a string that will be persisted
		// Attention: the max. length of this string is 255 chars because of database constraints.
		// TODO: Only persist a key here and load the real config from elsewhere?
		final String serializedConfig = GSON.toJson(config);
		log.debug("> serialized config: {}", serializedConfig);
		return serializedConfig;
	}

	@Override
	public void init(@Nullable final String serializedConfig) {
		// deserialize persisted config
		log.debug("> got serialized configuration on init: {}", serializedConfig);
		try {
			config = GSON.fromJson(serializedConfig, CONFIG_TYPE);
		} catch (final JsonSyntaxException e) {
			log.debug("Found legacy config that only has webhook URL");
			config = ConfigDto.builder()
					.url(serializedConfig)
					.showR(true)
					.showTS(true)
					.showC(true)
					.showBD(true)
					.atAll(false)
					.build();
		}
	}

	@Override
	public String getEditHtml() {
		final String editTemplateLocation = ((NotificationRecipientModuleDescriptor) this.getModuleDescriptor()).getEditTemplate();
		return this.templateRenderer.render(editTemplateLocation, populateContext());
	}

	@Override
	public String getViewHtml() {
		final String viewTemplateLocation = ((NotificationRecipientModuleDescriptor) this.getModuleDescriptor()).getViewTemplate();
		return this.templateRenderer.render(viewTemplateLocation, populateContext());
	}

	private Map<String, Object> populateContext() {
		final Map<String, Object> context = Maps.newHashMap();
		context.put(PARAM_WEBHOOK_URL, this.config.getUrl());
		context.put(PARAM_SHOW_REASON, this.config.isShowR());
		context.put(PARAM_SHOW_TESTS_SUMMARY, this.config.isShowTS());
		context.put(PARAM_SHOW_CHANGES, this.config.isShowC());
		context.put(PARAM_SHOW_BUILD_DURATION, this.config.isShowBD());
		context.put(PARAM_MENTION_ALL_ON_FAILED, this.config.isAtAll());
		log.debug("> populateContext = " + context.toString());
		return context;
	}

	public void setPlan(@Nullable final Plan plan) {
		this.plan = plan;
	}

	public void setPlan(@Nullable final ImmutablePlan immutablePlan) {
		this.plan = immutablePlan;
	}

	public void setResultsSummary(@Nullable final ResultsSummary resultsSummary) {
		this.summary = resultsSummary;
	}

	public void setDeploymentResult(@Nullable final DeploymentResult deploymentResult) {
		this.deploymentResult = deploymentResult;
	}

}
