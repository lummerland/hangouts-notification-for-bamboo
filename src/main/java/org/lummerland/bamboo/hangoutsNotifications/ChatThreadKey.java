package org.lummerland.bamboo.hangoutsNotifications;

import java.time.ZoneId;

import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.resultsummary.ResultsSummary;

/**
 * Representation of a thread key of the chat
 */
public class ChatThreadKey {

	public static String forBuild(final ResultsSummary resultsSummary) {
		return (resultsSummary == null)
				? ""
				: resultsSummary.getPlanKey().getKey() + "-" +
						resultsSummary.getBuildCompletedDate().toInstant()
								.atZone(ZoneId.systemDefault())
								.toLocalDate()
								.getMonthValue();
	}

	public static String forDeployment(final DeploymentResult deploymentResult) {
		return (deploymentResult == null)
				? ""
				: deploymentResult.getKey().getKey() + "-" +
						deploymentResult.getFinishedDate().toInstant()
								.atZone(ZoneId.systemDefault())
								.toLocalDate()
								.getMonthValue();
	}
}
