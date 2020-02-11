//
//  GizDevice.h
//  WidgetTest
//
//  Created by william Zhang on 2019/12/27.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import <Foundation/Foundation.h>

#pragma mark - GizDeviceWidgetDelegate

@protocol GizDeviceWidgetDelegate <NSObject>

@optional

- (void)deviceDataChange:(NSDictionary*_Nullable)data;

-(void)deviceOnlineStatusChange:(BOOL)is_online;

@end

NS_ASSUME_NONNULL_BEGIN

@interface GizDevice : NSObject

@property (nonatomic, weak) id<GizDeviceWidgetDelegate> delegate;

@property (nonatomic, assign) BOOL is_online;

@property (nonatomic, strong) NSString* dev_alias;

@property (nonatomic, strong) NSString* did;

@property (nonatomic, strong) NSString* mac;

@property (nonatomic, strong) NSString* product_name;

@property (nonatomic, strong) NSString* product_key;

@property (nonatomic, strong) NSDictionary* deviceData;

@property (nonatomic, strong) NSString* host;


+(GizDevice*)deviceByDictionary:(NSDictionary*)dic;

-(void)updateByDictionary:(NSDictionary*)dic;

-(BOOL)isSame:(GizDevice*)device;

-(BOOL)isSameFromDictionary:(NSDictionary*)dic;

-(void)addListener:(id<GizDeviceWidgetDelegate>)listener;

////获取当前设备状态的控制信息
//-(NSString*)currentControlIcon;
//
////根据当前状态获取下发的指令值
//-(NSDictionary*)getNextAttrs;

@end

NS_ASSUME_NONNULL_END
