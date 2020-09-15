//
//  GizControlConfig.m
//  WidgetTest
//
//  Created by william Zhang on 2020/1/2.
//  Copyright © 2020 Gziwits. All rights reserved.
//

#import "GizControlConfig.h"

@interface GizControlConfig ()<GizDeviceWidgetDelegate>

@property (nonatomic, strong) NSDictionary* language;

@property (nonatomic, strong) NSString* currentState;

@end

@implementation GizControlConfig

+(GizControlConfig *)configByDictionary:(NSDictionary *)dic{
    GizControlConfig* config = [[GizControlConfig alloc]init];
    config.cid = dic[@"id"];
    config.did = dic[@"did"];
    config.mac = dic[@"mac"];
    config.productKey = dic[@"productKey"];
    config.language = dic[@"language"];
    config.icon = dic[@"icon"];
    config.offlineIcon = dic[@"offlineIcon"];
    NSMutableArray* arr = [NSMutableArray new];
    NSArray* configs = dic[@"config"];
    if(configs && configs.count > 0){
        for (NSDictionary* dic in configs) {
            GizConfigItem* item = [GizConfigItem configItemByDictionary:dic];
            item.editName = [config getStringByKey:item.editName];
            [arr addObject:item];
        }
    }
    config.config = arr;
    return config;
}

-(NSDictionary *)deviceInfo{
    return @{@"mac":self.mac,@"product_key":self.productKey};
}

-(void)setDevice:(GizDevice *)device{
    if([device isSameFromDictionary:[self deviceInfo]]){
        _device = device;
        [_device addListener:self];
        if(self.config && self.config.count > 0 && device.deviceData){
           for (GizConfigItem* item in self.config) {
               [item setDeviceData:device.deviceData];
           }
        }
        if(!self.currentState){
            [self setStateAsDevicenName];
        }
    }
}

-(void)willSendCmd:(NSDictionary *)option{
    [self cancelSetState];
    self.currentState = @"执行中";
}

-(void)sendCmd:(NSDictionary *)option result:(BOOL)result{
    NSString* name = option[@"name"];
    if(!result){
        name = @"失败";
    } else{
        name = [self getStringByKey:name];
    }
    [self cancelSetState];
    self.currentState = name;
    [self performSelector:@selector(setStateAsDevicenName) withObject:nil afterDelay:2];
}

-(void)setCurrentState:(NSString *)currentState{
    _currentState = currentState;
    if([self.delegate respondsToSelector:@selector(deviceControlStateChange:)]){
           [self.delegate deviceControlStateChange:_currentState];
    }
}

-(void)setStateAsDevicenName{
    self.currentState = self.device.name;
}

-(void)cancelSetState{
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(setStateAsDevicenName) object:nil];
}

-(NSString *)getStateName{
    return self.currentState;
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

-(void)offlineTip
{
    if(self.device && self.device.is_online == NO){
        [self cancelSetState];
        self.currentState = @"离线";
        [self performSelector:@selector(setStateAsDevicenName) withObject:nil afterDelay:2];
    }
}

#pragma mark - GizDeviceWidgetDelegate
-(void)deviceDataChange:(NSDictionary *)data{
    if(self.config && self.config.count > 0){
        for (GizConfigItem* item in self.config) {
            [item setDeviceData:data];
        }
    }
    if([self.delegate respondsToSelector:@selector(deviceDataChange:)]){
        [self.delegate deviceDataChange:data];
    }
}

-(void)deviceOnlineStatusChange:(BOOL)is_online{
    if([self.delegate respondsToSelector:@selector(deviceOnlineChange:)]){
        [self.delegate deviceOnlineChange:is_online];
    }
}

@end
