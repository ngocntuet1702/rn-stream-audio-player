package vn.base.assistant.audio;

import android.util.Base64;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.facebook.react.modules.core.DeviceEventManagerModule;

public class AudioStreamPlayerModule extends ReactContextBaseJavaModule {
  private static final int SAMPLE_RATE = 24000;
  private AudioTrack audioTrack;
  private boolean isLastChunk = false;
  private final ReactApplicationContext reactContext;

  public AudioStreamPlayerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "AudioStreamPlayer";
  }

  @ReactMethod
  public void playBase64Audio(String base64, boolean isLastChunk) {
    this.isLastChunk = isLastChunk;

    byte[] pcmData = Base64.decode(base64, Base64.NO_WRAP);

    if (audioTrack == null) {
      int minBufferSize = AudioTrack.getMinBufferSize(
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      );

      audioTrack = new AudioTrack(
        AudioManager.STREAM_MUSIC,
        SAMPLE_RATE,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        minBufferSize,
        AudioTrack.MODE_STREAM
      );

      audioTrack.play();
    }

    audioTrack.write(pcmData, 0, pcmData.length);

    // Nếu là chunk cuối thì đợi xíu rồi gửi event (phát xong buffer)
    if (this.isLastChunk) {
      new Thread(() -> {
        try {
          int durationMs = (pcmData.length / 2) * 1000 / SAMPLE_RATE;
          Thread.sleep(durationMs + 30); // buffer + delay nhỏ
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        sendPlaybackFinished();
      }).start();
    }
  }

  @ReactMethod
  public void stop() {
    if (audioTrack != null) {
      audioTrack.stop();
      audioTrack.release();
      audioTrack = null;
    }
  }

  private void sendPlaybackFinished() {
    WritableMap payload = Arguments.createMap();
    payload.putString("status", "done");

    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit("playbackFinished", payload);
  }
}