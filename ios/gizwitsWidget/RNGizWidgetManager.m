//
//  RNGizWidgetManager.m
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/1/6.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "RNGizWidgetManager.h"
#import "GizWidgetAppManager.h"

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

RCT_EXPORT_METHOD(getControlDeviceList:(RCTResponseSenderBlock)result) {
    NSArray* list = [self.widgetmanager getDeviceControlDictionaryList];
    result(@[list]);
}

RCT_EXPORT_METHOD(saveControlDeviceList:(NSArray *)deviceList result:(RCTResponseSenderBlock)result) {
    [self.widgetmanager saveDeviceControlList:deviceList];
    result(@[@"success"]);
}


@end
