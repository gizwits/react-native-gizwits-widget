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
    device.is_online = dic[@"is_online"];
    device.dev_alias = dic[@"dev_alias"];
    device.mac = dic[@"mac"];
    device.product_key = dic[@"product_key"];
    device.product_name = dic[@"product_name"];
    device.host = dic[@"host"];
    device.deviceData = @{};
    return device;
}
-(void)updateByDictionary:(NSDictionary *)dic{
    self.is_online = dic[@"is_online"];
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

//-(NSString *)currentControlIcon{
//    if(self.is_online){
//        NSDictionary* dic = [self getCurrentAttrs];
//       NSString* image = dic[@"image"];
//        if(image){
//            return image;
//        }
//    }
//    return self.icon;
//}


//-(NSDictionary *)getNextAttrs{
//    NSArray* option = self.controlConfig[@"option"];
//    NSString* attrsKey = self.controlConfig[@"attrs"];
//    if(option && option.count > 0){
//        NSDictionary* dic = [self getCurrentAttrs];
//        NSUInteger index = 0;
//        if(dic){
//            NSNumber* indexNum = dic[@"index"];
//            index = [indexNum integerValue];
//            index = index +1;
//            index = index%option.count;
//        }
//        NSDictionary* optionItem = option[index];
//        return @{attrsKey:optionItem[@"value"]};
//    }
//    return NULL;
//}
//
////根据当前数据点，获取设备处于哪个配置状态，没有则返回空
//-(NSDictionary*)getCurrentAttrs{
//    NSArray* option = self.controlConfig[@"option"];
//    if(option && option.count > 0){
//        NSString* attrsKey = self.controlConfig[@"attrs"];
//        NSString* attrsType = self.controlConfig[@"type"];
//        id value = [self.deviceData objectForKey:attrsKey];
//        if(value){
//            NSInteger index = -1;
//            for (int i = 0; i<option.count; i++) {
//                NSDictionary* item = option[i];
//                if([attrsType isEqualToString:@"Boolean"]){
//                    NSNumber* itemValue = item[@"value"];
//                    NSNumber* v = (NSNumber*)value;
//                    if([v boolValue] == [itemValue boolValue]){
//                        index = i;
//                        break;
//                    }
//                } else if([attrsType isEqualToString:@"Number"]){
//                    NSNumber* itemValue = item[@"value"];
//                    NSNumber* v = (NSNumber*)value;
//                    if([v integerValue] == [itemValue integerValue]){
//                        index = i;
//                        break;
//                    }
//                } else{
//                   NSString* itemValue = item[@"value"];
//                   NSString* v = (NSString*)value;
//                   if([v isEqualToString:itemValue]){
//                        index = i;
//                        break;
//                   }
//               }
//            }
//            if(index > -1){
//                NSMutableDictionary* dic = [NSMutableDictionary dictionaryWithDictionary:option[index]];
//                [dic setObject:[NSNumber numberWithInteger:index] forKey:@"index"];
//                return dic;
//            }
//        }
//    }
//    return NULL;
//}


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
//    if([self.delegate respondsToSelector:@selector(deviceDataChange:)]){
//        [self.delegate deviceDataChange:deviceData];
//    }
}

-(void)setIs_online:(BOOL)is_online{
    if(self.is_online != is_online){
        _is_online = is_online;
        if([self.delegate respondsToSelector:@selector(deviceOnlineStatusChange:)]){
            [self.delegate deviceOnlineStatusChange:is_online];
        }
    }
}

@end
