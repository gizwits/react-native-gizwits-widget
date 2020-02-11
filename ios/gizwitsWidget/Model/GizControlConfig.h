//
//  GizControlConfig.h
//  WidgetTest
//
//  Created by william Zhang on 2020/1/2.
//  Copyright © 2020 Gziwits. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "GizDevice.h"

#pragma mark - GizConfigDelegate

@protocol GizConfigDelegate <NSObject>

@optional
- (void)attrValueChange;

@end


NS_ASSUME_NONNULL_BEGIN

@interface GizControlConfig : NSObject

@property (nonatomic, strong) NSString* did;

@property (nonatomic, strong) NSString* mac;

@property (nonatomic, strong) NSString* productKey;

@property (nonatomic, strong) NSString* icon;

@property (nonatomic, strong) NSString* attrs;

@property (nonatomic, strong) NSString* type;

@property (nonatomic, strong) NSArray* option;

@property (nonatomic, weak) id<GizConfigDelegate> delegate;

@property (nonatomic, strong) GizDevice* device;

+(GizControlConfig*)configByDictionary:(NSDictionary*)dic;

-(NSDictionary*)deviceInfo;

//获取当前设备状态的控制信息
-(NSString*)currentControlIcon;

//根据当前状态获取下发的指令值
-(NSDictionary*)getNextAttrs;

@end

NS_ASSUME_NONNULL_END
