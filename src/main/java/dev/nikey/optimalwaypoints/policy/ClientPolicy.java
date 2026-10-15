package dev.nikey.optimalwaypoints.policy;

import dev.nikey.optimalwaypoints.protocol.Feature;
import dev.nikey.optimalwaypoints.protocol.ServerPolicy;
import dev.nikey.optimalwaypoints.ui.Messages;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

public final class ClientPolicy {
	private static final long ANNOUNCE_DELAY_MS = 750L;

	private ServerPolicy policy = ServerPolicy.ALLOW_ALL;
	private boolean serverManaged;
	private boolean announcedRestrictions;
	private long announceAt = -1L;

	public boolean allows(Feature feature) {
		return policy.allows(feature);
	}

	public boolean modAllowed() {
		return policy.allows(Feature.MOD);
	}

	public int maxRenderDistance() {
		return policy.maxRenderDistance();
	}

	public boolean serverManaged() {
		return serverManaged;
	}

	public ServerPolicy current() {
		return policy;
	}

	public void reset() {
		policy = ServerPolicy.ALLOW_ALL;
		serverManaged = false;
		announcedRestrictions = false;
		announceAt = -1L;
	}

	public void apply(ServerPolicy incoming) {
		if (!incoming.equals(policy) || !serverManaged) {
			announceAt = System.currentTimeMillis() + ANNOUNCE_DELAY_MS;
		}
		policy = incoming;
		serverManaged = true;
	}

	public void tick() {
		if (announceAt >= 0 && System.currentTimeMillis() >= announceAt) {
			announceAt = -1L;
			announce();
		}
	}

	private void announce() {
		if (policy.isUnrestricted()) {
			if (announcedRestrictions) {
				Messages.chat(Messages.success(Component.translatable("optimalwaypoints.policy.unrestricted")));
			}
			announcedRestrictions = false;
			return;
		}
		announcedRestrictions = true;
		MutableComponent line;
		if (!policy.allows(Feature.MOD)) {
			line = Messages.error(Component.translatable("optimalwaypoints.policy.mod_disabled"));
		} else {
			line = Messages.warning(Component.translatable("optimalwaypoints.policy.restricted", summary()));
		}
		if (!policy.message().isBlank()) {
			line.append(Component.literal("  " + policy.message()).withColor(Messages.MUTED));
		}
		Messages.chat(line.withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(details(policy)))));
	}

	private MutableComponent summary() {
		MutableComponent summary = Component.empty();
		List<Feature> disabled = policy.disabledFeatures().stream().toList();
		for (int i = 0; i < disabled.size(); i++) {
			if (i > 0) {
				summary.append(Component.literal(", ").withColor(Messages.MUTED));
			}
			summary.append(Component.translatable(disabled.get(i).translationKey()).withColor(Messages.HIGHLIGHT));
		}
		if (policy.maxRenderDistance() > 0) {
			if (!disabled.isEmpty()) {
				summary.append(Component.literal(" · ").withColor(Messages.SEPARATOR));
			}
			summary.append(Component.translatable("optimalwaypoints.policy.max_distance", Messages.value(policy.maxRenderDistance())));
		}
		return summary;
	}

	public static MutableComponent details(ServerPolicy policy) {
		MutableComponent text = Component.translatable("optimalwaypoints.policy.details").withColor(Messages.ACCENT);
		for (Feature feature : Feature.values()) {
			boolean allowed = !policy.isDisabled(feature);
			text.append("\n").append(Component.literal(allowed ? "✔ " : "✘ ").withColor(allowed ? Messages.GOOD : Messages.BAD))
				.append(Component.translatable(feature.translationKey()).withColor(allowed ? Messages.BODY : Messages.MUTED));
		}
		if (policy.maxRenderDistance() > 0) {
			text.append("\n").append(Component.translatable("optimalwaypoints.policy.max_distance", policy.maxRenderDistance()).withColor(Messages.BODY));
		}
		return text;
	}
}
