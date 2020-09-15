//
//  GizConfigItem.m
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/1/19.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "GizConfigItem.h"
#import "Constant.h"

@interface GizConfigItem ()

@property (nonatomic, strong) id currentValue;

@end

@implementation GizConfigItem

+(GizConfigItem *)configItemByDictionary:(NSDictionary *)dic
{
    GizConfigItem* item = [GizConfigItem new];
    item.attrs = dic[@"attrs"];
    item.option = dic[@"option"];
    item.type = dic[@"type"];
    item.attrsIcon = dic[@"attrsIcon"];
    item.editName = dic[@"editName"];
    return item;
}

- (void)setDeviceData:(NSDictionary *)data{
    if(data){
        id value = [data objectForKey:self.attrs];
        if(value){
            if(self.currentValue){
                if(![self value:self.currentValue EqualTo:value valueType:self.type]){
                    self.currentValue = value;
                }
            } else{
                self.currentValue = value;
            }
        }
    }
}

-(void)setCurrentValue:(id)currentValue{
    _currentValue = currentValue;
    if([self.delegate respondsToSelector:@selector(attrValueChange)]){
        [self.delegate attrValueChange];
    }
}

-(NSString *)currentControlIcon{
    NSDictionary* dic = [self getCurrentAttrs];
    NSString* image = dic[@"image"];
    if(image){
        return image;
    }
    return self.attrsIcon;
}

-(NSDictionary *)getNextOption{
    NSArray* option = self.option;
    if(option && option.count > 0){
        NSDictionary* dic = [self getCurrentAttrs];
        NSUInteger index = 0;
        if(dic){
            NSNumber* indexNum = dic[@"index"];
            index = [indexNum integerValue];
            index = index +1;
            index = index%option.count;
        }
        NSDictionary* optionItem = option[index];
        return optionItem;
    }
    return NULL;
}

-(NSDictionary *)getCmdFormOption:(NSDictionary *)option{
    if(option){
        return @{self.attrs:option[@"value"]};
    }
    return NULL;
}

-(BOOL)value:(id)value EqualTo:(id)value2 valueType:(NSString*)type{
    if([type isEqualToString:Type_Boolean]){
       NSNumber* itemValue = value2;
       NSNumber* v = value;
       if([v boolValue] == [itemValue boolValue]){
           return YES;
       }
    } else if([type isEqualToString:Type_Number]){
       NSNumber* itemValue = value2;
       NSNumber* v = value;
       if([v integerValue] == [itemValue integerValue]){
           return YES;
       }
    } else if([type isEqualToString:Type_Enumeration]){
      if([value isKindOfClass:[NSString class]]){
        NSString* itemValue = value2;
        NSString* v = value;
        if([itemValue isEqualToString:v]){
            return YES;
        }
      } else if([value isKindOfClass:[NSNumber class]]){
        NSNumber* itemValue = value2;
        NSNumber* v = value;
        if([v integerValue] == [itemValue integerValue]){
            return YES;
        }
      }
    }
    
    return NO;
}


//根据当前数据点，获取设备处于哪个配置状态，没有则返回空
-(NSDictionary*)getCurrentAttrs{
    NSArray* option = self.option;
    if(option && option.count > 0){
        if(self.currentValue){
            NSInteger index = -1;
            for (int i = 0; i<option.count; i++) {
                NSDictionary* item = option[i];
                if([self value:self.currentValue EqualTo:item[@"value"] valueType:self.type])
                {
                    index = i;
                    break;
                }
            }
            if(index > -1){
                NSMutableDictionary* dic = [NSMutableDictionary dictionaryWithDictionary:option[index]];
                NSNumber* notInOption = dic[@"notInOption"];
                if(notInOption && [notInOption boolValue]){
                   return NULL;
                }
                [dic setObject:[NSNumber numberWithInteger:index] forKey:@"index"];
                return dic;
            }
        }
    }
    return NULL;
}


-(BOOL)isInOptionRange{
    if([self.type isEqualToString:@"Boolean"]){
      NSArray* option = self.option;
      if(option.count == 1){
        // 布尔值单独配置一个的时候，代表需要按配置的值来显示
        NSDictionary* item = option[0];
        return [self value:self.currentValue EqualTo:item[@"value"] valueType:self.type];
      } else{
        //布尔值直接判断是true就行了,false不计算在内
        NSNumber* v = self.currentValue;
        return [v boolValue];
      }
       
    } else{
        //其他类型，只要有index就代表当前值在可选范围内
        NSDictionary* dic = [self getCurrentAttrs];
        return dic[@"index"];
    }
}


@end
