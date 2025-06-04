import AVFoundation
import Foundation

@objc(AudioStreamPlayer)
class AudioStreamPlayer: RCTEventEmitter {
  private var audioEngine: AVAudioEngine?
  private var playerNode: AVAudioPlayerNode?
  private var audioFormat: AVAudioFormat?

  override static func requiresMainQueueSetup() -> Bool {
    return true
  }

  override func supportedEvents() -> [String]! {
    return ["playbackFinished"]
  }

  private func setupAudio(sampleRate: Double = 24000) {
    if audioEngine != nil { return }

    do {
      try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default)
      try AVAudioSession.sharedInstance().setActive(true)
    } catch {
      // print("AVAudioSession error: \(error)")
      return
    }

    let engine = AVAudioEngine()
    let node = AVAudioPlayerNode()

    guard
      let format = AVAudioFormat(
        commonFormat: .pcmFormatFloat32,
        sampleRate: sampleRate,
        channels: 1,
        interleaved: false)
    else {
      // print("Failed to create AVAudioFormat")
      return
    }

    engine.attach(node)
    engine.connect(node, to: engine.mainMixerNode, format: format)

    do {
      try engine.start()
    } catch {
      // print("Engine failed to start: \(error)")
      return
    }

    self.audioEngine = engine
    self.playerNode = node
    self.audioFormat = format
    node.play()
  }

  @objc(playBase64Audio:isLastChunk:)
  func playBase64Audio(_ base64: String, isLastChunk: Bool) {
    guard let data = Data(base64Encoded: base64) else {
      print("Failed to decode base64 string")
      return
    }
    playBuffer(data as NSData, isLastChunk: isLastChunk)
  }

  private func playBuffer(_ buffer: NSData, isLastChunk: Bool) {
    if audioEngine == nil || playerNode == nil || audioFormat == nil {
      setupAudio()
    }

    guard let format = audioFormat, let player = playerNode else {
      // print("Audio not ready")
      return
    }

    let bytesPerFrameInt16 = MemoryLayout<Int16>.size
    let frameCount = buffer.length / bytesPerFrameInt16

    guard
      let audioBuffer = AVAudioPCMBuffer(
        pcmFormat: format, frameCapacity: AVAudioFrameCount(frameCount))
    else {
      // print("Failed to create AVAudioPCMBuffer")
      return
    }

    audioBuffer.frameLength = AVAudioFrameCount(frameCount)

    let int16Pointer = buffer.bytes.assumingMemoryBound(to: Int16.self)
    let float32Pointer = audioBuffer.floatChannelData![0]

    for i in 0..<frameCount {
      float32Pointer[i] = Float(int16Pointer[i]) / 32768.0
    }

    player.scheduleBuffer(
      audioBuffer,
      completionHandler: isLastChunk
        ? { [weak self] in
          DispatchQueue.main.async {
            self?.sendEvent(withName: "playbackFinished", body: ["status": "done"])
          }
        } : nil)
  }

  @objc
  func stop() {
    playerNode?.stop()
    audioEngine?.stop()
    playerNode = nil
    audioEngine = nil
    audioFormat = nil
  }
}
