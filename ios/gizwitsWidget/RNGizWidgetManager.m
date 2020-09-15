//
//  RNGizWidgetManager.m
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/1/6.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "RNGizWidgetManager.h"
#import "GizWidgetAppManager.h"

NSString * const resultError_Not_SetUp = @"Not Set Up";

@interface RNGizWidgetManager ()

@property (nonatomic, strong) GizWidgetAppManager* widgetmanager;

@end


@implementation RNGizWidgetManager

RCT_EXPORT_MODULE();

- (NSArray<NSString *> *)supportedEvents{
    return @[];
}

- (dispatch_queue_t)methodQueue{
    return dispatch_get_main_queue();
}

static id _instace;
+ (instancetype)allocWithZone:(struct _NSZone *)zone {
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _instace = [super allocWithZone:zone];
    });
    return _instace;
}

-(GizWidgetAppManager *)widgetmanager{
    if(!_widgetmanager){
        _widgetmanager = [GizWidgetAppManager defaultManager];
    }
    return _widgetmanager;
}

RCT_EXPORT_METHOD(setUpAppInfo:(NSDictionary*)info) {
    [self.widgetmanager setUpAppInfo:info];
}

RCT_EXPORT_METHOD(getControlDeviceList:(RCTPromiseResolveBlock)resolve
                   rejecter:(RCTPromiseRejectBlock)reject) {
    if([self.widgetmanager checkSetUp]){
        NSArray* list = [self.widgetmanager getDeviceControlDictionaryList];
        if(!list){
            list = @[];
        }
        resolve(@{@"data":list});
    } else{
        resolve(@{@"error":resultError_Not_SetUp});
    }
}



RCT_EXPORT_METHOD(saveControlDeviceList:(NSArray *)deviceList result:(RCTResponseSenderBlock)result) {
    if([self.widgetmanager checkSetUp]){
        [self.widgetmanager saveDeviceControlList:deviceList];
        result(@[[NSNull null]]);
   } else{
        result(@[resultError_Not_SetUp]);
   }
}


RCT_EXPORT_METHOD(getStateDeviceList:(RCTPromiseResolveBlock)resolve
                   rejecter:(RCTPromiseRejectBlock)reject) {
    if([self.widgetmanager checkSetUp]){
        NSArray* list = [self.widgetmanager getDeviceStateDictionaryList];
        if(!list){
            list = @[];
        }
        resolve(@{@"data":list});
    } else{
        resolve(@{@"error":resultError_Not_SetUp});
    }
}


RCT_EXPORT_METHOD(saveStateDeviceList:(NSArray *)deviceList result:(RCTResponseSenderBlock)result) {
    if([self.widgetmanager checkSetUp]){
        [self.widgetmanager saveDeviceStateList:deviceList];
        result(@[[NSNull null]]);
   } else{
        result(@[resultError_Not_SetUp]);
   }
}

RCT_EXPORT_METHOD(getSceneList:(RCTPromiseResolveBlock)resolve
                   rejecter:(RCTPromiseRejectBlock)reject) {
    if([self.widgetmanager checkSetUp]){
        NSArray* list = [self.widgetmanager getSceneDictionaryList];
        if(!list){
            list = @[];
        }
        resolve(@{@"data":list});
    } else{
        resolve(@{@"error":resultError_Not_SetUp});
    }
}


RCT_EXPORT_METHOD(saveSceneList:(NSArray *)deviceList result:(RCTResponseSenderBlock)result) {
    if([self.widgetmanager checkSetUp]){
        [self.widgetmanager saveSceneList:deviceList];
        result(@[[NSNull null]]);
   } else{
        result(@[resultError_Not_SetUp]);
   }
}

RCT_EXPORT_METHOD(clearAllData:(RCTResponseSenderBlock)result) {
  [self.widgetmanager clearAllData];
  result(@[[NSNull null]]);
}




@end
