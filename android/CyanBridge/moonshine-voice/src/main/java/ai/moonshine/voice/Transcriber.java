package ai.moonshine.voice;

public final class Transcriber {
    public void loadFromFiles(String modelPath, int modelArch) {
        throw new UnsupportedOperationException(
                "Moonshine native sources are not bundled in this checkout."
        );
    }

    public void addListener(TranscriptEventCallback callback) {
    }

    public void start() {
    }

    public void stop() {
    }

    public void addAudio(float[] samples, int sampleRate) {
    }
}
