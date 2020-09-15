//
//  GizWidgetAppManager.h
//  WidgetTest
//
//  Created by william Zhang on 2019/12/25.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "GizOpenApiResult.h"
#import "GizAepApiResult.h"
#import "GizDevice.h"
#import "GizControlConfig.h"
#import "GizStateConfig.h"
#import "GizManualScene.h"

#pragma mark - GizDeviceControlWidgetDelegate

@protocol GizDeviceControlWidgetDelegate <NSObject>

@optional
- (void)controlDeviceListChange:(NSArray*_Nullable)deviceList;

- (void)configDeviceListChange:(NSArray*_Nullable)configDeviceList;

@end

#pragma mark - GizDeviceStateWidgetDelegate

@protocol GizDeviceStateWidgetDelegate <NSObject>

@optional
- (void)controlDeviceListChange:(NSArray*_Nullable)deviceList;

- (void)configDeviceListChange:(NSArray*_Nullable)configDeviceList;

@end

#pragma mark - GizManualSceneWidgetDelegate

@protocol GizManualSceneWidgetDelegate <NSObject>

@optional
- (void)manualSceneListChange:(NSArray*_Nullable)sceneList;

@end

NS_ASSUME_NONNULL_BEGIN

@interface GizWidgetAppManager : NSObject

@property (nonatomic, weak) id<GizDeviceControlWidgetDelegate> controlDeviceListdelegate;

@property (nonatomic, weak) id<GizDeviceStateWidgetDelegate> stateDeviceListdelegate;

@property (nonatomic, weak) id<GizManualSceneWidgetDelegate> manualSceneListdelegate;

@property (nonatomic, strong) UIColor* tintColor;

+ (instancetype)defaultManager;

- (void)setUpAppInfo:(NSDictionary*)params;

- (BOOL)checkSetUp;

//开启socket，widget调用
- (void)startSocket;

- (void)stopSocket;

- (NSArray*)getBindDeviceList;

//设备控制widget
- (void)saveDeviceControlList:(NSArray*)list;

- (NSArray*)getDeviceControlDictionaryList;

- (NSArray*)getDeviceControlList;

- (void)clearDeviceControlList;

-(BOOL)controlDevice:(GizDevice*)did Attrs:(NSDictionary*)attrs;

//设备状态widget
- (void)saveDeviceStateList:(NSArray*)list;

- (NSArray*)getDeviceStateDictionaryList;

- (NSArray*)getDeviceStateList;

- (void)clearDeviceStateList;

//场景widget
- (void)saveSceneList:(NSArray*)list;

- (NSArray*)getSceneDictionaryList;

- (NSArray*)getSceneList;

- (void)clearSceneList;

-(void)excuteManualScene:(GizManualScene *)scene completion:(void (^)(GizAepApiResult *))completionHandler;

-(void)queryManualDetail:(GizManualScene *)scene completion:(void (^)(GizAepApiResult * _Nonnull))completionHandler;

// 清除数据
-(void)clearAllData;

@end

NS_ASSUME_NONNULL_END
