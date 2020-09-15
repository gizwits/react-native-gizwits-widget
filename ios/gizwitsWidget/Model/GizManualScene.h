//
//  GizManualScene.h
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/2/18.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

#pragma mark - GizManualSceneDelegate

@protocol GizManualSceneDelegate <NSObject>

@optional
- (void)sceneStateChange:(NSString*_Nullable)state highLight:(BOOL)highLight;

@end

NS_ASSUME_NONNULL_BEGIN

@interface GizManualScene : NSObject

/** 场景id */
@property (nonatomic, strong) NSString* sid;

/** 场景所在家庭id */
@property (nonatomic, strong) NSString* homeId;

/** 场景所在家庭名称 */
@property (nonatomic, strong) NSString* homeName;

/** 场景名称 */
@property (nonatomic, strong) NSString* name;

/** 场景icon */
@property (nonatomic, strong) NSString* icon;

/** 场景执行url */
@property (nonatomic, strong) NSString* url;

@property (nonatomic, weak) id<GizManualSceneDelegate> delegate;

@property (nonatomic, assign, readonly) BOOL highLight;  //标识高亮

@property (nonatomic, strong, readonly) NSString* stateName;

@property (nonatomic, strong) NSDictionary* detail;

+(GizManualScene*)sceneByDictionary:(NSDictionary*)dic;

//修改场景执行状态
-(void)excuteResult:(BOOL)result;

//-(BOOL)iSame:(NSDictionary*)info;

@end

NS_ASSUME_NONNULL_END
