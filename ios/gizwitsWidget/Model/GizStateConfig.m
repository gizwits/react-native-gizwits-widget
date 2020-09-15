//
//  GizStateConfig.m
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/2/4.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "GizStateConfig.h"
#import "GizStateContentItem.h"
#import "Constant.h"

@interface GizStateConfig ()<GizDeviceWidgetDelegate>

@property (nonatomic, strong) NSDictionary* title;

@property (nonatomic, strong) NSDictionary* language;

@property (nonatomic, strong) id currentValue;

@end

@implementation GizStateConfig

+(GizStateConfig *)configByDictionary:(NSDictionary *)dic{
    GizStateConfig* config = [GizStateConfig new];
    config.did = dic[@"did"];
    config.cid = dic[@"id"];
    config.mac = dic[@"mac"];
    config.productKey = dic[@"productKey"];
    config.language = dic[@"language"];
    config.icon = dic[@"icon"];
    config.offlineIcon = dic[@"offlineIcon"];
    config.attrs = dic[@"attrs"];
    config.editName = dic[@"editName"];
    config.type = dic[@"type"];
    config.title = dic[@"title"];
    NSArray* arr = dic[@"content"];
    NSMutableArray* content = [NSMutableArray new];
    for (NSDictionary* item in arr) {
        GizStateContentItem* contentItem = [GizStateContentItem itemFromDictionary:item];
        contentItem.type = config.type;
        [content addObject:contentItem];
    }
    config.content = content;
    return config;
}

-(NSDictionary *)deviceInfo{
    return @{@"mac":self.mac,@"product_key":self.productKey};
}

-(void)setDevice:(GizDevice *)device{
    if([device isSameFromDictionary:[self deviceInfo]]){
        _device = device;
        [_device addListener:self];
        if(device.deviceData){
          id value = device.deviceData[self.attrs];
          if(value){
              self.currentValue = value;
          }
        }
    }
}

-(void)setCurrentValue:(id)currentValue
{
    BOOL needUpdate = NO;
    if(_currentValue){
        if([self.type  isEqualToString:Type_Boolean]){
            NSNumber* cs = currentValue;
            NSNumber* _cs = _currentValue;
            needUpdate = [cs boolValue] != [_cs boolValue];
        } else if([self.type isEqualToString:Type_Number]){
            NSNumber* cs = currentValue;
            float f_1 = [cs doubleValue];
            NSString* value_1 = [NSString stringWithFormat:@"%@",@(f_1)];
          
            NSNumber* _cs = _currentValue;
            float f_2 = [_cs doubleValue];
            NSString* value_2 = [NSString stringWithFormat:@"%@",@(f_2)];
            needUpdate = ![value_1 isEqualToString:value_2];
        } else if([self.type isEqualToString:Type_Enumeration]){
          if([currentValue isKindOfClass:[NSString class]]){
            NSString* cs = currentValue;
            NSString* _cs = _currentValue;
            needUpdate = ![cs isEqualToString:_cs];
          } else if([currentValue isKindOfClass:[NSNumber class]]){
            NSNumber* cs = currentValue;
            NSNumber* _cs = _currentValue;
            needUpdate = [cs integerValue] != [_cs integerValue];
          }
        }
    } else{
        needUpdate = YES;
    }
    
    if(needUpdate){
        _currentValue = currentValue;
        if([self.delegate respondsToSelector:@selector(attrsChange)]){
            [self.delegate attrsChange];
        }
    }
}

-(NSString*)getStringByKey:(NSString*)key{
    NSString* languageKey = @"zh";
    if(self.languageKey){
        languageKey = self.languageKey;
    }
    NSDictionary* strings = self.language[languageKey];
    if(strings){
        return strings[key];
    }
    return key;
}

-(NSDictionary *)currentInfo{
    NSMutableDictionary* dic = [NSMutableDictionary new];
    id value;
    NSString* valueString;
    if(self.currentValue){
        value = self.currentValue;
    } else{
        value = [NSNumber numberWithInt:0];
    }
    
    NSString* title;
    if(self.title && self.title[@"text"]){
        title = [self getStringByKey:self.title[@"text"]];
    }
    
    NSString* image;
    
    GizStateContentItem* item;
    for (int i = 0; i<self.content.count; i++) {
        GizStateContentItem* cItem = self.content[i];
        if([cItem isInRange:value]){
            item = cItem;
            break;
        }
    }
    BOOL showValue = NO;
    if(item){
        NSString* text = item.text;
        if(text){
            showValue = YES;
            if(![text isEqualToString:@"{value}"]){
                //不是显示当前值的话，替换掉value
                valueString = [self getStringByKey:text];
            }
        }
        if (item.image) {
            image = item.image;
        }
        if(item.formatTitle){
            //目前只支持type，且type==Date
            NSString* type = item.formatTitle[@"type"];
            if(type && [type isEqualToString:@"Date"]){
                NSDateFormatter *format = [[NSDateFormatter alloc] init];
                format.dateFormat = @"HH:mm";
                title = [format stringFromDate: [NSDate date]];
            }
        }
    } else {
        //没有条件符合，又是数值型与枚举型，则显示具体数据
        showValue = [self.type isEqualToString:Type_Number] || [self.type isEqualToString:Type_Enumeration];
    }
    
    if(!showValue && !image){
        if(self.device.is_online){
            image = self.icon;
        }
//        image = self.device.is_online?self.icon:self.offlineIcon;
    }
    
    if(!valueString){
      if([self.type isEqualToString:Type_Number]){
        NSNumber* v = value;
        float f = [v doubleValue];
        valueString = [NSString stringWithFormat:@"%@",@(f)];
      } else if([self.type isEqualToString:Type_Enumeration]){
        valueString = [NSString stringWithFormat:@"%@",[value stringValue]];
      } else{
        valueString = [NSString stringWithFormat:@"%@",[value stringValue]];
      }
      
    }
    
    if(!self.device.is_online){
        valueString = [self.languageKey isEqualToString:@"zh"]?@"离线":@"Offline";
      image = nil;
    }
    
    [dic setValue:valueString forKey:@"value"];
    if(title){
        [dic setValue:title forKey:@"title"];
    }
    
    if(image){
        [dic setValue:image forKey:@"image"];
    }
    
    return dic;
}

#pragma mark - GizDeviceWidgetDelegate
-(void)deviceDataChange:(NSDictionary *)data{
    id value = data[self.attrs];
    if(value){
        self.currentValue = value;
    }
}

-(void)deviceOnlineStatusChange:(BOOL)is_online{
    if([self.delegate respondsToSelector:@selector(deviceOnlineStatusChange:)]){
        [self.delegate deviceOnlineStatusChange:is_online];
    }
}

@end
