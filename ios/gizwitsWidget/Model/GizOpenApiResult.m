//
//  GizOpenApiResult.m
//  WidgetTest
//
//  Created by william Zhang on 2019/12/30.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import "GizOpenApiResult.h"

@implementation GizOpenApiResult

+(GizOpenApiResult *)resultFromData:(NSData *)data Response:(NSURLResponse *)response Error:(NSError *)error{
    GizOpenApiResult* result = [GizOpenApiResult new];
    
    if(error){
        result.error = error;
        result.success = NO;
    } else{
        NSHTTPURLResponse* r =(NSHTTPURLResponse*)response;
        NSInteger statusCode = r.statusCode;
        result.success = statusCode == 200;
        NSDictionary *dict = [NSJSONSerialization JSONObjectWithData:data options:kNilOptions error:NULL];
//        NSLog(@"response data: %@", dict);
        if(result.success){
            result.data = dict;
        } else{
            result.error = [NSError errorWithDomain:@"openApi" code:statusCode userInfo:dict];
        }
    }
    return result;
}

@end
