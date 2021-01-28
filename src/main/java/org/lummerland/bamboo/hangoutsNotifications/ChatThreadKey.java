package org.lummerland.bamboo.hangoutsNotifications;

import java.time.ZoneId;

import com.atlassian.bamboo.resultsummary.ResultsSummary;

/**
 * Representation of a thread key of the chat
 */
public class ChatThreadKey {

	private final ResultsSummary resultsSummary;

	public ChatThreadKey(final ResultsSummary resultsSummary) {
		this.resultsSummary = resultsSummary;
	}

	public String get() {
		return
				resultsSummary.getPlanKey().getKey() + "-" +
				resultsSummary.getBuildCompletedDate().toInstant()
						.atZone(ZoneId.systemDefault())
						.toLocalDate()
						.getMonthValue();
	}
}
