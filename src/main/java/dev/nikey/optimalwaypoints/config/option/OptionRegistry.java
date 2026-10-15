package dev.nikey.optimalwaypoints.config.option;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OptionRegistry {
	private final Map<String, OptionCategory> categories = new LinkedHashMap<>();

	public OptionCategory category(String key) {
		return categories.computeIfAbsent(key, OptionCategory::new);
	}

	public List<OptionCategory> categories() {
		return new ArrayList<>(categories.values());
	}

	public void resetAll() {
		for (OptionCategory category : categories.values()) {
			category.options().forEach(ConfigOption::reset);
		}
	}
}
