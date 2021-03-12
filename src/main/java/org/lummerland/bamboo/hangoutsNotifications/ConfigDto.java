package org.lummerland.bamboo.hangoutsNotifications;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConfigDto {

	String webhookUrl;
	boolean showNotificationReason;
	boolean showTestsSummary;
	boolean showChanges;
	boolean showBuildDuration;
	boolean mentionAllOnFailed;

}
