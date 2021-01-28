package org.lummerland.bamboo.hangoutsNotifications;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.resultsummary.ResultsSummary;

@RunWith(MockitoJUnitRunner.class)
public class ChatThreadKeyTest {

	@Test
	public void generate_threadkey() {
		final PlanKey planKey = mock(PlanKey.class);
		when(planKey.getKey()).thenReturn("BUIL-PLAN-JOB1");

		final ResultsSummary resultsSummary = mock(ResultsSummary.class);
		when(resultsSummary.getBuildCompletedDate())
				.thenReturn(Date.from(LocalDate.of(2020, 5, 15)
					.atStartOfDay()
					.atZone(ZoneId.systemDefault())
					.toInstant()));
		when(resultsSummary.getPlanKey()).thenReturn(planKey);

		final ChatThreadKey chatThreadKey = new ChatThreadKey(resultsSummary);
		assertThat(chatThreadKey.get(), is("BUIL-PLAN-JOB1-5"));
	}
}
