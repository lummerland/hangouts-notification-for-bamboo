package org.lummerland.bamboo.hangoutsNotifications;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.atlassian.bamboo.deployments.DeploymentResultKey;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.resultsummary.ResultsSummary;

@RunWith(MockitoJUnitRunner.class)
public class ChatThreadKeyTest {

	@Test
	public void generate_threadkey_forBuild() {
		final PlanKey planKey = mock(PlanKey.class);
		when(planKey.getKey()).thenReturn("BUIL-PLAN-JOB1");

		final ResultsSummary resultsSummary = mock(ResultsSummary.class);
		when(resultsSummary.getBuildCompletedDate())
				.thenReturn(Date.from(LocalDate.of(2020, 5, 15)
					.atStartOfDay()
					.atZone(ZoneId.systemDefault())
					.toInstant()));
		when(resultsSummary.getPlanKey()).thenReturn(planKey);

		assertThat(ChatThreadKey.forBuild(resultsSummary), is("BUIL-PLAN-JOB1-5"));
	}

	@Test
	public void generate_threadkey_forDeployment() {
		final DeploymentResultKey deploymentResultKey = mock(DeploymentResultKey.class);
		when(deploymentResultKey.getKey()).thenReturn("DEPL");

		final DeploymentResult deploymentResult = mock(DeploymentResult.class);
		when(deploymentResult.getKey()).thenReturn(deploymentResultKey);
		when(deploymentResult.getFinishedDate())
				.thenReturn(Date.from(LocalDate.of(2020, 5, 15)
					.atStartOfDay()
					.atZone(ZoneId.systemDefault())
					.toInstant()));

		assertThat(ChatThreadKey.forDeployment(deploymentResult), is("DEPL-5"));
	}

	@Test
	public void generate_threadkey_null() {
		assertThat(ChatThreadKey.forBuild(null), is(""));
		assertThat(ChatThreadKey.forDeployment(null), is(""));
	}
}
