import { NativeModules, NativeEventEmitter } from 'react-native';

const { AudioStreamPlayer } = NativeModules;
const emitter = new NativeEventEmitter(AudioStreamPlayer);

export const playBase64Audio = (base64, isLastChunk) => {
  AudioStreamPlayer.playBase64Audio(base64, isLastChunk);
};

export const stop = () => {
  AudioStreamPlayer.stop();
};

export const addPlaybackFinishedListener = (callback) =>
  emitter.addListener('playbackFinished', callback);

export { AudioStreamPlayer };
