//
//  GizStateContentItem.h
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/2/4.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "GizConditionItem.h"

NS_ASSUME_NONNULL_BEGIN

@interface GizStateContentItem : NSObject

@property (nonatomic, strong) NSArray* conditions;

@property (nonatomic, strong) NSString* text;

@property (nonatomic, strong) NSString* image;

@property (nonatomic, strong) NSDictionary* formatTitle;

@property (nonatomic, strong) NSString* type;

+(GizStateContentItem*)itemFromDictionary:(NSDictionary*)dic;


-(BOOL)isInRange:(NSNumber*)value;


@end

NS_ASSUME_NONNULL_END
