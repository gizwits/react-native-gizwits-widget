//
//  GizDevice.m
//  WidgetTest
//
//  Created by william Zhang on 2019/12/27.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import "GizDevice.h"

@interface GizDevice ()

@property (nonatomic, strong) NSPointerArray* listenerList;

@end

@implementation GizDevice


+(GizDevice *)deviceByDictionary:(NSDictionary *)dic{
    GizDevice* device = [[GizDevice alloc]init];
    device.did = dic[@"did"];
    NSNumber* online = dic[@"is_online"];
    device.is_online = [online boolValue];
    device.dev_alias = dic[@"dev_alias"];
    device.mac = dic[@"mac"];
    device.product_key = dic[@"product_key"];
    device.product_name = dic[@"product_name"];
    device.host = dic[@"host"];
    if(dic[@"deviceData"]){
        device.deviceData = dic[@"deviceData"];
    } else{
        device.deviceData = @{};
    }
    return device;
}
-(void)updateByDictionary:(NSDictionary *)dic{
    NSNumber* online = dic[@"is_online"];
    self.is_online = [online boolValue];
    self.dev_alias = dic[@"dev_alias"];
    self.host = dic[@"host"];
}

-(BOOL)isSame:(GizDevice *)device{
    return [self.product_key isEqualToString:device.product_key] && [self.mac isEqualToString:device.mac];
}

-(BOOL)isSameFromDictionary:(NSDictionary *)dic{
    return [self.product_key isEqualToString:dic[@"product_key"]] && [self.mac caseInsensitiveCompare:dic[@"mac"]]== NSOrderedSame;
}

-(void)addListener:(id<GizDeviceWidgetDelegate>)listener{
    if(!self.listenerList){
        self.listenerList  = [NSPointerArray weakObjectsPointerArray];
    }
    [self.listenerList compact];
    for (int i = 0; i<self.listenerList.count; i++) {
        void * item = [self.listenerList pointerAtIndex:i];
        if(item == (__bridge void *)(listener)){
            NSLog(@"listener已经存在");
            return;
        }
    }
    [self.listenerList addPointer:(__bridge void *)(listener)];

//    if(![self.listenerList containsObject:listener]){
//         __weak typeof(self) weakListener = listener;
//        [self.listenerList addObject:weakListener];
//    }
}

-(NSString *)name{
    if(self.dev_alias && self.dev_alias.length > 0){
        return self.dev_alias;
    }
    return self.product_name;
}

-(NSDictionary *)deviceInfo{
    NSMutableDictionary* info = [NSMutableDictionary new];
    if(self.did){
        [info setObject:self.did forKey:@"did"];
    }
    [info setObject:[NSNumber numberWithBool:self.is_online] forKey:@"is_online"];
    if(self.dev_alias){
        [info setObject:self.dev_alias forKey:@"dev_alias"];
    }
    if(self.mac){
        [info setObject:self.mac forKey:@"mac"];
    }
    if(self.product_key){
        [info setObject:self.product_key forKey:@"product_key"];
    }
    if(self.product_name){
        [info setObject:self.product_name forKey:@"product_name"];
    }
    if(self.host){
        [info setObject:self.product_name forKey:@"host"];
    }
    [info setObject:self.deviceData forKey:@"deviceData"];
    return info;
}


#pragma mark - Setter
-(void)setDeviceData:(NSDictionary *)deviceData{
    NSMutableDictionary* dic = [NSMutableDictionary dictionaryWithDictionary:_deviceData];
    for (NSString* key in [deviceData allKeys]) {
        [dic setValue:[deviceData valueForKey:key] forKey:key];
    }
    _deviceData = [NSDictionary dictionaryWithDictionary:dic];
    if(self.listenerList && self.listenerList.count){
        for (id<GizDeviceWidgetDelegate> listeren in self.listenerList) {
            if([listeren respondsToSelector:@selector(deviceDataChange:)]){
                 [listeren deviceDataChange:deviceData];
            }
        }
    }
}

-(void)setIs_online:(BOOL)is_online{
    if(_is_online != is_online){
        _is_online = is_online;
        if(self.listenerList && self.listenerList.count){
            for (id<GizDeviceWidgetDelegate> listeren in self.listenerList) {
                if([listeren respondsToSelector:@selector(deviceOnlineStatusChange:)]){
                     [listeren deviceOnlineStatusChange:is_online];
                }
            }
        }
    }
}


@end
