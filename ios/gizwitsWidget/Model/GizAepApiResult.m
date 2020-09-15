
//
//  GizAepApiResult.m
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/2/21.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "GizAepApiResult.h"

@implementation GizAepApiResult

+(GizAepApiResult *)resultFromData:(NSData *)data Response:(NSURLResponse *)response Error:(NSError *)error{
    GizAepApiResult* result = [GizAepApiResult new];
    if(error){
        result.error = error;
        result.success = NO;
    } else{
        NSHTTPURLResponse* r =(NSHTTPURLResponse*)response;
        NSInteger statusCode = r.statusCode;
        result.success = statusCode == 200;
        NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:data options:kNilOptions error:NULL];
        NSLog(@"response data: %@", dict);
        if(result.success){
            NSNumber* aepCode = dict[@"code"];
            if (aepCode && [aepCode integerValue] == 200) {
                result.success = YES;
                NSError* jsonError;
                id aepResultData =dict[@"data"];
                if([aepResultData isKindOfClass:[NSString class]]){
                    NSString* dataStr = aepResultData;
                    NSData* aepData = [dataStr dataUsingEncoding:NSUTF8StringEncoding];
                    NSDictionary *aepDataDic = [NSJSONSerialization JSONObjectWithData:aepData options:kNilOptions error:&jsonError];
                    if(!jsonError){
                        result.data = aepDataDic;
                    } else{
                        //返回格式不是json的，暂不处理
                        NSLog(@"解析json出错%@",jsonError);
                    }
                } else {
                    //返回格式不是json的，暂不处理
                    NSLog(@"返回不是json%@",aepResultData);
                }
            } else{
                result.success = NO;
                result.data = dict;
            }
        } else{
            result.error = [NSError errorWithDomain:@"aepApi" code:statusCode userInfo:dict];
        }
    }
    return result;
}


@end
