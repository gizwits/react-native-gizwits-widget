//
//  GizControlConfig.m
//  WidgetTest
//
//  Created by william Zhang on 2020/1/2.
//  Copyright © 2020 Gziwits. All rights reserved.
//

#import "GizControlConfig.h"

@interface GizControlConfig ()<GizDeviceWidgetDelegate>

@end

@implementation GizControlConfig

+(GizControlConfig *)configByDictionary:(NSDictionary *)dic{
    GizControlConfig* config = [[GizControlConfig alloc]init];
    config.attrs = dic[@"attrs"];
    config.option = dic[@"option"];
    config.type = dic[@"type"];
    config.did = dic[@"did"];
    config.mac = dic[@"mac"];
    config.productKey = dic[@"productKey"];
    config.icon = dic[@"icon"];
    return config;
}

-(NSDictionary *)deviceInfo{
    return @{@"mac":self.mac,@"product_key":self.productKey};
}

-(void)setDevice:(GizDevice *)device{
    if([device isSameFromDictionary:[self deviceInfo]]){
        _device = device;
        [_device addListener:self];
    }
}

-(NSString *)currentControlIcon{
    if(self.device){
        if(self.device.is_online){
            NSDictionary* dic = [self getCurrentAttrs];
            NSString* image = dic[@"image"];
            if(image){
                return image;
            }
        }
        return self.icon;
    }
    return NULL;
}

-(NSDictionary *)getNextAttrs{
    NSArray* option = self.option;
    NSString* attrsKey = self.attrs;
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
        return @{attrsKey:optionItem[@"value"]};
    }
    return NULL;
}


//根据当前数据点，获取设备处于哪个配置状态，没有则返回空
-(NSDictionary*)getCurrentAttrs{
    NSArray* option = self.option;
    if(self.device && self.device.deviceData && option && option.count > 0){
        NSString* attrsKey = self.attrs;
        NSString* attrsType = self.type;
        id value = [self.device.deviceData objectForKey:attrsKey];
        if(value){
            NSInteger index = -1;
            for (int i = 0; i<option.count; i++) {
                NSDictionary* item = option[i];
                if([attrsType isEqualToString:@"Boolean"]){
                    NSNumber* itemValue = item[@"value"];
                    NSNumber* v = (NSNumber*)value;
                    if([v boolValue] == [itemValue boolValue]){
                        index = i;
                        break;
                    }
                } else if([attrsType isEqualToString:@"Number"]){
                    NSNumber* itemValue = item[@"value"];
                    NSNumber* v = (NSNumber*)value;
                    if([v integerValue] == [itemValue integerValue]){
                        index = i;
                        break;
                    }
                } else{
                   NSString* itemValue = item[@"value"];
                   NSString* v = (NSString*)value;
                   if([v isEqualToString:itemValue]){
                        index = i;
                        break;
                   }
               }
            }
            if(index > -1){
                NSMutableDictionary* dic = [NSMutableDictionary dictionaryWithDictionary:option[index]];
                [dic setObject:[NSNumber numberWithInteger:index] forKey:@"index"];
                return dic;
            }
        }
    }
    return NULL;
}

#pragma mark - GizDeviceWidgetDelegate
-(void)deviceDataChange:(NSDictionary *)data{
    if([self.delegate respondsToSelector:@selector(attrValueChange)]){
        [self.delegate attrValueChange];
    }
}

@end
