//
//  GizWidgetAppManager.h
//  WidgetTest
//
//  Created by william Zhang on 2019/12/25.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "GizOpenApiResult.h"
#import "GizDevice.h"
#import "GizControlConfig.h"

#pragma mark - GizDeviceControlWidgetDelegate

@protocol GizDeviceControlWidgetDelegate <NSObject>

@optional
- (void)controlDeviceListChange:(NSArray*_Nullable)deviceList;

- (void)configDeviceListChange:(NSArray*_Nullable)configDeviceList;

@end

NS_ASSUME_NONNULL_BEGIN

@interface GizWidgetAppManager : NSObject

@property (nonatomic, weak) id<GizDeviceControlWidgetDelegate> controlDeviceListdelegate;

+ (instancetype)defaultManager;

- (void)setUpAppInfo:(NSDictionary*)params;

//开启socket，widget调用
- (void)startSocket;

- (void)stopSocket;

//设备控制widget
- (void)saveDeviceControlList:(NSArray*)list;

- (NSArray*)getDeviceControlDictionaryList;

- (NSArray*)getDeviceControlList;

- (void)clearDeviceControlList;

-(void)controlDevice:(GizDevice*)did Attrs:(NSDictionary*)attrs;

//设备状态widget


@end

NS_ASSUME_NONNULL_END
