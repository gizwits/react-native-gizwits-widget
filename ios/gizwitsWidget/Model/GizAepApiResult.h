//
//  GizAepApiResult.h
//  gizwits-super-app-rn
//
//  Created by william Zhang on 2020/2/21.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface GizAepApiResult : NSObject

@property (nonatomic, assign) BOOL success;

@property (nonatomic, strong) NSDictionary* data;

@property (nonatomic, strong) NSError* error;

+(GizAepApiResult*)resultFromData:(NSData*)data Response:(NSURLResponse *)response Error:(NSError *)error;


@end

NS_ASSUME_NONNULL_END
