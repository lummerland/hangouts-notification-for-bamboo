package org.lummerland.bamboo.hangoutsNotifications;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConfigDto {

	/**
	 * Webhook URL. Name shortened because of database column length constraints.
	 */
	String url;

	/**
	 * Show notification reason? Name shortened because of database column length constraints.
	 */
	boolean showR;

	/**
	 * Show tests summary? Name shortened because of database column length constraints.
	 */
	boolean showTS;

	/**
	 * Show changes? Name shortened because of database column length constraints.
	 */
	boolean showC;

	/**
	 * Show build duration? Name shortened because of database column length constraints.
	 */
	boolean showBD;

	/**
	 * Send @all mention for failed builds? Name shortened because of database column length constraints.
	 */
	boolean atAll;

}
