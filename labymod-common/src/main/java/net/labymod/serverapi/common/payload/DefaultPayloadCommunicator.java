package net.labymod.serverapi.common.payload;

import com.google.gson.JsonElement;
import java.util.UUID;
import net.labymod.serverapi.api.payload.PayloadBuffer;
import net.labymod.serverapi.api.payload.PayloadCommunicator;
import net.labymod.serverapi.api.player.LabyModPlayerService;

public abstract class DefaultPayloadCommunicator implements PayloadCommunicator {

  private static final String LEGACY_CHANNEL = "LMC";
  private static final String MODERN_LEGACY_CHANNEL = "legacy:lmc";

  private final LabyModPlayerService<?> labyModPlayerService;

  public DefaultPayloadCommunicator(LabyModPlayerService<?> labyModPlayerService) {
    this.labyModPlayerService = labyModPlayerService;
  }

  @Override
  public void sendLabyModMessage(UUID uniqueId, String messageKey, JsonElement messageContent) {
    this.labyModPlayerService
        .getPlayer(uniqueId)
        .ifPresent(
            player -> {
              PayloadBuffer payloadBuffer = new DefaultPayloadBuffer();

              payloadBuffer.writeString(messageKey);
              payloadBuffer.writeString(messageContent.toString());

              String version = player.getVersion();

              if (this.isLabyModV3(version) && !this.shouldSupportModernChannel(version)) {
                this.send(uniqueId, LEGACY_CHANNEL, payloadBuffer.getBytes());
                return;
              }

              this.send(uniqueId, MODERN_LEGACY_CHANNEL, payloadBuffer.getBytes());
            });
  }

  private boolean isLabyModV3(String version) {
    int[] splitVersion = this.splitVersion(version);
    return splitVersion[0] == 3;
  }

  private boolean shouldSupportModernChannel(String version) {
    int[] splitVersion = this.splitVersion(version);
    int[] nonLegacySupportVersion = LabyModPlayerService.NON_LEGACY_SUPPORT_VERSION;
    return splitVersion[0] == 3
        && nonLegacySupportVersion[1] < splitVersion[1]
        && nonLegacySupportVersion[2] < splitVersion[2];
  }

  @SuppressWarnings("ConstantConditions")
  private int[] splitVersion(String version) {
    if (!version.contains(".")) {
      throw new IllegalStateException(String.format("Not valid version! (%s)", version));
    }

    int[] versionArray = new int[3];
    String[] split = version.split("\\.");

    for (int i = 0; i < split.length; i++) {

      int versionNumber;
      try {
        versionNumber = Integer.parseInt(split[i]);
      } catch (NumberFormatException exception) {
        throw new IllegalArgumentException(String.format("Not valid version! (%s)", version));
      }

      if (i <= 2) {
        versionArray[i] = versionNumber;
      }
    }

    if (split.length < 3) {
      versionArray[2] = 0;
    }

    return versionArray;
  }
}