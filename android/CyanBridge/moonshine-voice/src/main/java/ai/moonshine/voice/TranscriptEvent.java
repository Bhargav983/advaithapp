package ai.moonshine.voice;

public abstract class TranscriptEvent {
    public abstract void accept(TranscriptEventListener listener);

    public static final class Line {
        public final String text;

        public Line(String text) {
            this.text = text;
        }
    }

    public static final class LineCompleted extends TranscriptEvent {
        public final Line line;

        public LineCompleted(Line line) {
            this.line = line;
        }

        @Override
        public void accept(TranscriptEventListener listener) {
            listener.onLineCompleted(this);
        }
    }

    public static final class Error extends TranscriptEvent {
        public final Throwable cause;

        public Error(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public void accept(TranscriptEventListener listener) {
            listener.onError(this);
        }
    }
}
