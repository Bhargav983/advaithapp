package ai.moonshine.voice;

@FunctionalInterface
public interface TranscriptEventCallback {
    void onEvent(TranscriptEvent event);
}
