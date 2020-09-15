//
//  GizControlConfig.h
//  WidgetTest
//
//  Created by william Zhang on 2020/1/2.
//  Copyright © 2020 Gziwits. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "GizDevice.h"
#import "GizConfigItem.h"

#pragma mark - GizConfigDelegate

@protocol GizConfigDelegate <NSObject>

@optional
- (void)deviceDataChange:(NSDictionary*_Nullable)data;

- (void)deviceOnlineChange:(BOOL)is_online;

- (void)deviceControlStateChange:(NSString*_Nullable)stateName;

@end


NS_ASSUME_NONNULL_BEGIN

@interface GizControlConfig : NSObject

@property (nonatomic, strong) NSString* cid;

@property (nonatomic, strong) NSString* did;

@property (nonatomic, strong) NSString* mac;

@property (nonatomic, strong) NSString* productKey;

@property (nonatomic, strong) NSString* icon;

@property (nonatomic, strong) NSString* offlineIcon;

@property (nonatomic, strong) NSArray* config;

@property (nonatomic, strong) GizDevice* device;

@property (nonatomic, weak) id<GizConfigDelegate> delegate;

@property (nonatomic, strong) NSString* languageKey;

+(GizControlConfig*)configByDictionary:(NSDictionary*)dic;

-(NSDictionary*)deviceInfo;

-(void)willSendCmd:(NSDictionary*)option;

-(void)sendCmd:(NSDictionary*)option result:(BOOL)result;

-(NSString*)getStateName;

-(NSString*)getStringByKey:(NSString*)key;

-(void)offlineTip;

@end

NS_ASSUME_NONNULL_END
