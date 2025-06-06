package com.audio.streamplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Base64;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioStreamPlayerModule extends ReactContextBaseJavaModule {
  private static final int SAMPLE_RATE = 24000;

  private final ReactApplicationContext reactContext;

  private AudioTrack audioTrack;
  private Thread playbackThread;
  private BlockingQueue<byte[]> currentQueue;

  private final Object lock = new Object();

  private volatile boolean isPlaying = false;
  private volatile boolean isCancelled = false;
  private long sessionId = 0;

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
    byte[] pcmData = Base64.decode(base64, Base64.NO_WRAP);

    synchronized (lock) {
      if (!isPlaying) {
        startNewPlaybackSession();
      }

      if (currentQueue != null) {
        try {
          currentQueue.put(pcmData);
          if (isLastChunk) {
            currentQueue.put(new byte[0]); // dấu hiệu kết thúc
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void startNewPlaybackSession() {
    synchronized (lock) {
      stopInternal(); // Hủy phiên cũ nếu còn

      isPlaying = true;
      isCancelled = false;
      sessionId++;
      final long thisSessionId = sessionId;
      currentQueue = new LinkedBlockingQueue<>();

      playbackThread = new Thread(() -> {
        int bufferSize = AudioTrack.getMinBufferSize(
          SAMPLE_RATE,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
        );

        audioTrack = new AudioTrack(
          AudioManager.STREAM_MUSIC,
          SAMPLE_RATE,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT,
          bufferSize,
          AudioTrack.MODE_STREAM
        );

        audioTrack.play();

        try {
          while (!isCancelled) {
            byte[] data = currentQueue.take();
            if (isCancelled || data.length == 0) {
              break;
            }
            audioTrack.write(data, 0, data.length);
          }
        } catch (InterruptedException e) {
          // Bị dừng bằng interrupt
        } finally {
          synchronized (lock) {
            if (thisSessionId == sessionId) {
              stopAudioTrack();
              sendPlaybackFinished();
              isPlaying = false;
              currentQueue = null;
            }
          }
        }
      });

      playbackThread.start();
    }
  }

  @ReactMethod
  public void stop() {
    synchronized (lock) {
      stopInternal();
    }
  }

  private void stopInternal() {
    sessionId++; // Vô hiệu hóa mọi phiên đang phát
    isCancelled = true;

    if (playbackThread != null && playbackThread.isAlive()) {
      playbackThread.interrupt();
      try {
        playbackThread.join(2000); // đợi tối đa 2s
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    playbackThread = null;
    isPlaying = false;
    stopAudioTrack();

    if (currentQueue != null) {
      currentQueue.clear();
      currentQueue = null;
    }
  }

  private void stopAudioTrack() {
    if (audioTrack != null) {
      try {
        audioTrack.pause(); // API 21+
        audioTrack.flush(); // API 23+ nếu muốn xóa buffer nội bộ
      } catch (Exception ignored) {}
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
