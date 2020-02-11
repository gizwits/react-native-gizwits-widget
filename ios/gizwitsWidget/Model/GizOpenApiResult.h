//
//  GizOpenApiResult.h
//  WidgetTest
//
//  Created by william Zhang on 2019/12/30.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface GizOpenApiResult : NSObject

@property (nonatomic, assign) BOOL success;

@property (nonatomic, strong) NSDictionary* data;

@property (nonatomic, strong) NSError* error;

+(GizOpenApiResult*)resultFromData:(NSData*)data Response:(NSURLResponse *)response Error:(NSError *)error;

@end

NS_ASSUME_NONNULL_END
