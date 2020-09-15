//
//  GizManualScene.m
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/2/18.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "GizManualScene.h"
#import "GizWidgetAppManager.h"

@interface GizManualScene ()

@property (nonatomic, strong) NSString* currentState;

@property (nonatomic, assign) BOOL isLoadingDetail;

@end

@implementation GizManualScene

+(GizManualScene *)sceneByDictionary:(NSDictionary *)dic
{
    GizManualScene* s = [GizManualScene new];
    s.sid= dic[@"id"];
    s.homeId = dic[@"homeId"];
    s.homeName = dic[@"homeName"];
    s.name = dic[@"name"];
    s.icon = dic[@"icon"];
    s.url = dic[@"url"];
    s.currentState = s.name;
    return s;
}

//-(void)queryDetail{
//    typeof(self) __weak weakSelf = self;
//    [[GizWidgetAppManager defaultManager] queryManualDetail:self completion:^(GizAepApiResult * _Nonnull result) {
//        if(result.success){
//            weakSelf.detail = result.data;
//        }
//        [weakSelf checkSelfIsHighLight];
//    }];
//}

//-(void)checkSelfIsHighLight{
//    if(self.detail){
//        //有详情才判断
//        NSArray* actions = self.detail[@"actions"];
//        if(actions && actions.count > 0){
//            BOOL allActionIsControlDevice = YES;
//            for (int i = 0; i<actions.count; i++) {
//                NSDictionary* item = actions[i];
//                NSString* type = item[@"type"];
//                NSDictionary* controlDevice = item[@"controlDevice"];
//                if([type isEqualToString:@"control"] && controlDevice){
//                    continue;
//                } else{
//                    allActionIsControlDevice = NO;
//                    break;
//                }
//            }
//            if(allActionIsControlDevice){
//                //全部条件都是设备控制，才需要判断
//
//            }
//        }
//    }
//    [self setStateAsSceneName];
//}

-(NSString *)stateName{
    return self.currentState;
}

-(void)notiDelegate{
    if([self.delegate respondsToSelector:@selector(sceneStateChange:highLight:)]){
        [self.delegate sceneStateChange:self.currentState highLight:self.highLight];
    }
}

-(void)setStateAsSceneName{
    self.currentState = self.name;
    _highLight = NO;
    [self notiDelegate];
}

-(void)cancelSetState{
    [NSObject cancelPreviousPerformRequestsWithTarget:self selector:@selector(setStateAsSceneName) object:nil];
}

-(void)excuteResult:(BOOL)result{
//    [self queryDetail];
    [self cancelSetState];
    NSString* resultName;
    if(result){
        resultName = @"执行成功";
    } else{
        resultName = @"执行失败";
    }
    self.currentState = resultName;
    _highLight = result;
    dispatch_async(dispatch_get_main_queue(), ^{
        [self performSelector:@selector(setStateAsSceneName) withObject:nil afterDelay:2];
    });
    [self notiDelegate];
}

//
//-(BOOL)iSame:(NSDictionary*)info{
//    NSString* sid= info[@"id"];
//    NSString* homeId = info[@"homeId"];
//    return [self.sid isEqualToString:sid]&&[self.homeId isEqualToString:homeId];
//}

@end
