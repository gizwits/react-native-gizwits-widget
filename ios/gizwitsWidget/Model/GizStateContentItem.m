//
//  GizStateContentItem.m
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/2/4.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "GizStateContentItem.h"

@interface GizStateContentItem ()

@end

@implementation GizStateContentItem

+(GizStateContentItem *)itemFromDictionary:(NSDictionary *)dic
{
    GizStateContentItem* item = [GizStateContentItem new];
    item.text = dic[@"text"];
    item.image = dic[@"image"];
    item.formatTitle = dic[@"formatTitle"];
    NSArray* arr = dic[@"conditions"];
    NSMutableArray* conditions = [NSMutableArray new];
    for (NSDictionary* cItem in arr) {
        GizConditionItem* conItem = [GizConditionItem itemFroDictionary:cItem];
        [conditions addObject:conItem];
    }
    item.conditions = conditions;
    return item;
}

-(void)setType:(NSString *)type{
    _type = type;
    for (GizConditionItem* item in self.conditions) {
        item.type = type;
    }
}

-(BOOL)isInRange:(NSNumber *)value
{
    for (GizConditionItem* item in self.conditions) {
        if(![item isInRange:value]){
            return NO;
        }
    }
    return YES;
}

@end
