package org.lummerland.bamboo.hangoutsNotifications;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

@Named
public class HangoutsNotificationRecipient
		extends AbstractNotificationRecipient
		implements DeploymentResultAwareNotificationRecipient, RequiresPlan, RequiresResultSummary {

	private static final Logger log = LoggerFactory.getLogger(HangoutsNotificationRecipient.class);

	private String webhookUrl = null;
	private static String WEBHOOK_URL = "webhookUrl";

	private ImmutablePlan plan;
	private ResultsSummary summary;
	private DeploymentResult deploymentResult;

	@ComponentImport
	private TemplateRenderer templateRenderer;

	@ComponentImport
	private final BranchStatusService branchStatusService;

	@Inject
	public HangoutsNotificationRecipient(
			final TemplateRenderer templateRenderer,
			final BranchStatusService branchStatusService
	) {
		this.templateRenderer = templateRenderer;
		this.branchStatusService = branchStatusService;
	}

	@NotNull
	public List<NotificationTransport> getTransports() {
		final String config = getRecipientConfig();
		log.debug("> config: {}", config);
		return Collections.singletonList(HangoutsNotificationTransport.build(config, summary, templateRenderer, branchStatusService));
	}

	@Override
	public void populate(@NotNull final Map<String, String[]> params) {
		log.debug("> populated config: {}", params);
		if (params.containsKey(WEBHOOK_URL)) {
			this.webhookUrl = params.get(WEBHOOK_URL)[0];
		}
	}

	@NotNull
	@Override
	public String getRecipientConfig() {
		final StringBuilder recipientConfig = new StringBuilder();
		if (StringUtils.isNotBlank(this.webhookUrl)) {
			recipientConfig.append(this.webhookUrl);
		}
		log.debug("> get configuration: {}", recipientConfig);
		return recipientConfig.toString();
	}

	@Override
	public void init(@Nullable final String configurationData) {
		log.debug("> got configuration on init: {}", configurationData);
		if (StringUtils.isNotBlank(configurationData)) {
			this.webhookUrl = configurationData;
		}
	}

	@Override
	public String getEditHtml() {
		final String editTemplateLocation = ((NotificationRecipientModuleDescriptor)this.getModuleDescriptor()).getEditTemplate();
		return this.templateRenderer.render(editTemplateLocation, populateContext());
	}

	@Override
	public String getViewHtml() {
		final String viewTemplateLocation = ((NotificationRecipientModuleDescriptor)this.getModuleDescriptor()).getViewTemplate();
		return this.templateRenderer.render(viewTemplateLocation, populateContext());
	}

	private Map<String, Object> populateContext() {
		final Map<String, Object> context = Maps.newHashMap();
		if (this.webhookUrl != null) {
			context.put(WEBHOOK_URL, this.webhookUrl);
		}
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

	public void setDeploymentResult(@Nullable	final DeploymentResult deploymentResult) {
		this.deploymentResult = deploymentResult;
	}
}
