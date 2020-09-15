//
//  GizConfigItem.h
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/1/19.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

#pragma mark - GizConfigItemDelegate

@protocol GizConfigItemDelegate <NSObject>

@optional
- (void)attrValueChange;

@end


NS_ASSUME_NONNULL_BEGIN

@interface GizConfigItem : NSObject

@property (nonatomic, strong) NSString* attrs;

@property (nonatomic, strong) NSString* attrsIcon;

@property (nonatomic, strong) NSString* type;

@property (nonatomic, strong) NSString* editName;

@property (nonatomic, strong) NSArray* option;

@property (nonatomic, weak) id<GizConfigItemDelegate> delegate;

+(GizConfigItem*)configItemByDictionary:(NSDictionary*)dic;

//设置最新的设备数据
-(void)setDeviceData:(NSDictionary*)data;

//获取当前设备状态的控制信息
-(NSString*)currentControlIcon;

//根据当前状态获取下发的可选项
-(NSDictionary*)getNextOption;

-(NSDictionary*)getCmdFormOption:(NSDictionary*)option;

//判断当前是否处于可选择范围内
-(BOOL)isInOptionRange;

@end

NS_ASSUME_NONNULL_END
