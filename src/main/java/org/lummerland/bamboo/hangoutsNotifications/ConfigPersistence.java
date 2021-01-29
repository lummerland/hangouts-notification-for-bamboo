package org.lummerland.bamboo.hangoutsNotifications;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.bamboo.bandana.PlanAwareBandanaContext;
import com.atlassian.bandana.BandanaManager;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Named
public class ConfigPersistence {

	private static final String KEY_PREFIX = "org.lummerland.bamboo.hangoutsNotifications.config.";
	private static final Gson GSON = new Gson();
	private static final Type CONFIG_TYPE = new TypeToken<ConfigDto>() {}.getType();

	@ComponentImport
	private final BandanaManager bandanaManager;

	@Inject
	public ConfigPersistence(final BandanaManager bandanaManager) {
		this.bandanaManager = bandanaManager;
	}

	public ConfigDto load(final String key) {
		if (isBlank(key)) {
			log.debug("No key given");
			return null;
		}
		final String serializedConfig = (String) bandanaManager.getValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, KEY_PREFIX + key);
		if (isBlank(serializedConfig)) {
			log.debug("No config found");
			return null;
		}
		try {
			return GSON.fromJson(serializedConfig, CONFIG_TYPE);
		} catch (final JsonSyntaxException e) {
			log.debug("Config seems to be corrupted");
			return null;
		}
	}

	public void save(final String key, final ConfigDto config) {
		if (isBlank(key)) {
			log.debug("No key given");
			return;
		}
		if (config == null) {
			log.debug("No config given");
			return;
		}
		final String serializedConfig = GSON.toJson(config);
		bandanaManager.setValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, KEY_PREFIX + key, serializedConfig);
	}

}
