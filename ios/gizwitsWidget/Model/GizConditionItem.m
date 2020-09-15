//
//  GizConditionItem.m
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/2/4.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//


#import "GizConditionItem.h"
#import "Constant.h"

@implementation GizConditionItem

+(GizConditionItem *)itemFroDictionary:(NSDictionary *)dic
{
    GizConditionItem* item = [GizConditionItem new];
    item.value = dic[@"value"];
    item.opt = dic[@"opt"];
    return item;
}

-(BOOL)isInRange:(id)value
{
    if([self.type isEqualToString:Type_Boolean]){
        //布尔值只能是等于不等于比较
      NSNumber* v1 = value;
      NSNumber* v2 = self.value;
        if ([self.opt isEqualToString:Opt_equal]) {
            return [v1 boolValue] == [v2 boolValue];
        } else if([self.opt isEqualToString:Opt_no_equal]){
            return [v1 boolValue] != [v2 boolValue];
        }
    } else if([self.type isEqualToString:Type_Number] || [self.type isEqualToString:Type_Enumeration]){
        if([self.type isEqualToString:Type_Enumeration] && [self.value isKindOfClass:[NSString class]]){
          NSString* s1 = value;
          NSString* s2 = self.value;
          if ([self.opt isEqualToString:Opt_equal]) {
              return [s1 isEqualToString:s2];
          } else if([self.opt isEqualToString:Opt_no_equal]){
              return ![s1 isEqualToString:s2];
          }
          return NO;
        }
        // 数值型处理
        NSInteger v1 = [value integerValue] ;
        NSInteger v2 = [self.value integerValue];
        if ([self.opt isEqualToString:Opt_equal]) {
            return v1 == v2;
        } else if([self.opt isEqualToString:Opt_no_equal]){
            return v1 != v2;
        } else if([self.opt isEqualToString:Opt_less]){
             return v1 < v2;
        } else if([self.opt isEqualToString:Opt_less_equal]){
            return v1 <= v2;
        } else if([self.opt isEqualToString:Opt_greater]){
            return v1 > v2;
        }else if([self.opt isEqualToString:Opt_greater_equal]){
            return v1 >= v2;
        }
    }
    return NO;
}
@end
