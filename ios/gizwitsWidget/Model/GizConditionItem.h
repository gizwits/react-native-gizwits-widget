//
//  GizConditionItem.h
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/2/4.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface GizConditionItem : NSObject

@property (nonatomic, strong) NSString* opt;

@property (nonatomic, strong) id value;

@property (nonatomic, strong) NSString* type;

+(GizConditionItem*)itemFroDictionary:(NSDictionary*)dic;

-(BOOL)isInRange:(id)value;

@end

NS_ASSUME_NONNULL_END
