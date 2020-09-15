//
//  GizStateConfig.h
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/2/4.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "GizDevice.h"

#pragma mark - GizStateConfigDelegate

@protocol GizStateConfigDelegate <NSObject>

@optional
- (void)attrsChange;

- (void)deviceOnlineStatusChange:(BOOL)is_online;

@end

NS_ASSUME_NONNULL_BEGIN

@interface GizStateConfig : NSObject

@property (nonatomic, strong) NSString* cid;

@property (nonatomic, strong) NSString* did;

@property (nonatomic, strong) NSString* mac;

@property (nonatomic, strong) NSString* productKey;

@property (nonatomic, strong) NSString* icon;

@property (nonatomic, strong) NSString* offlineIcon;

@property (nonatomic, strong) NSString* attrs;

@property (nonatomic, strong) NSString* type;

@property (nonatomic, strong) NSString* editName;

@property (nonatomic, strong) NSArray* content;

@property (nonatomic, strong) GizDevice* device;

@property (nonatomic, strong) NSString* languageKey;

@property (nonatomic, weak) id<GizStateConfigDelegate> delegate;

+(GizStateConfig*)configByDictionary:(NSDictionary*)dic;

-(NSDictionary*)deviceInfo;

-(NSString*)getStringByKey:(NSString*)key;

/**
 获取当前状态的显示信息，image，title，value
 */
-(NSDictionary*)currentInfo;

@end

NS_ASSUME_NONNULL_END
