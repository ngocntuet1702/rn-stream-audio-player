//
//  AudioStreamPlayerBridge.m
//  miniappassistant
//
//  Created by ngocnguyen07 on 3/6/25.
//

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_REMAP_MODULE(AudioStreamPlayer, AudioStreamPlayer, RCTEventEmitter)

RCT_EXTERN_METHOD(playBase64Audio:(NSString *)base64 isLastChunk:(BOOL)isLastChunk)
RCT_EXTERN_METHOD(stop)

@end
