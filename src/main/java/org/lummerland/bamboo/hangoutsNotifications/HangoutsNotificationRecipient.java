package org.lummerland.bamboo.hangoutsNotifications;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.atlassian.bamboo.build.artifact.ArtifactLinkManager;
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

	private ConfigDto config = ConfigDto.builder().build();
	private String configKey = null;

	private ImmutablePlan plan;
	private ResultsSummary summary;
	private DeploymentResult deploymentResult;

	@ComponentImport
	private final TemplateRenderer templateRenderer;
	@ComponentImport
	private final ArtifactLinkManager artifactLinkManager;
	private final ConfigPersistence configPersistence;

	@Inject
	public HangoutsNotificationRecipient(
			final TemplateRenderer templateRenderer,
			final ArtifactLinkManager artifactLinkManager,
			final ConfigPersistence configPersistence
	) {
		this.templateRenderer = templateRenderer;
		this.artifactLinkManager = artifactLinkManager;
		this.configPersistence = configPersistence;
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
				.webhookUrl(params.getOrDefault(PARAM_WEBHOOK_URL, new String[] { "" })[0])
				.showNotificationReason(Boolean.parseBoolean(params.getOrDefault(PARAM_SHOW_REASON, BOOLEAN_DEFAULT)[0]))
				.showTestsSummary(Boolean.parseBoolean(params.getOrDefault(PARAM_SHOW_TESTS_SUMMARY, BOOLEAN_DEFAULT)[0]))
				.showChanges(Boolean.parseBoolean(params.getOrDefault(PARAM_SHOW_CHANGES, BOOLEAN_DEFAULT)[0]))
				.showBuildDuration(Boolean.parseBoolean(params.getOrDefault(PARAM_SHOW_BUILD_DURATION, BOOLEAN_DEFAULT)[0]))
				.mentionAllOnFailed(Boolean.parseBoolean(params.getOrDefault(PARAM_MENTION_ALL_ON_FAILED, BOOLEAN_DEFAULT)[0]))
				.build();
		log.debug("> populated config: {}", config);
		configPersistence.save(getRecipientConfig(), config);
		log.debug("> saved config {} under key {}", config, configKey);
	}

	@NotNull
	@Override
	public String getRecipientConfig() {
		// serialize config into a string that will be persisted
		// Attention: the max. length of this string is 255 chars because of database constraints.
		// so we save the config ourselves and only persist a key here.

		if (isBlank(configKey)) {
			configKey = createConfigKey();
			log.debug("> created config key {}", configKey);
		}
		return configKey;
	}

	@Override
	public void init(@Nullable final String persistedConfigKey) {
		if (isNotBlank(persistedConfigKey)) {
			configKey = persistedConfigKey;
			config = configPersistence.load(configKey);
			log.debug("> loaded config {} for key {}", config, configKey);
			if (config == null) {
				log.debug("> found legacy config that only contains a webhook URL");
				configKey = createConfigKey();
				config = ConfigDto.builder()
						.webhookUrl(persistedConfigKey)
						.showNotificationReason(true)
						.showTestsSummary(true)
						.showChanges(true)
						.showBuildDuration(true)
						.mentionAllOnFailed(false)
						.build();
			}
		} else {
			configKey = createConfigKey();
			log.debug("> creating default config using key {}", configKey);
			config = ConfigDto.builder()
					.webhookUrl("")
					.showNotificationReason(true)
					.showTestsSummary(true)
					.showChanges(true)
					.showBuildDuration(true)
					.mentionAllOnFailed(false)
					.build();
		}
	}

	private String createConfigKey() {
		return UUID.randomUUID().toString();
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
		context.put(PARAM_WEBHOOK_URL, this.config.getWebhookUrl());
		context.put(PARAM_SHOW_REASON, this.config.isShowNotificationReason());
		context.put(PARAM_SHOW_TESTS_SUMMARY, this.config.isShowTestsSummary());
		context.put(PARAM_SHOW_CHANGES, this.config.isShowChanges());
		context.put(PARAM_SHOW_BUILD_DURATION, this.config.isShowBuildDuration());
		context.put(PARAM_MENTION_ALL_ON_FAILED, this.config.isMentionAllOnFailed());
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
