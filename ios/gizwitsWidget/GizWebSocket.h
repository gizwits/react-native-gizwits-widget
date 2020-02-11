//
//  GizWebSocket.h
//  WidgetTest
//
//  Created by william Zhang on 2019/12/27.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import <Foundation/Foundation.h>

#pragma mark - GizWebSocketDelegate

@protocol GizWebSocketDelegate <NSObject>

@optional
- (void)device:(NSString*_Nullable)did ReceivedAttrs:(NSDictionary*_Nullable)attrs;

- (void)device:(NSString*_Nullable)did OnlineChange:(BOOL)isOnline;

@end


NS_ASSUME_NONNULL_BEGIN

@interface GizWebSocket : NSObject

@property (nonatomic, strong, readonly) NSString* url;  //获取当前连接的m2mUrl，只读，设置需要通过初始化方法

@property (nonatomic, weak) id<GizWebSocketDelegate> delegate;

//初始化方法
- (void)setUpByUid:(NSString*)uid Token:(NSString*)token m2mUrl:(NSString*)m2mUrl AppId:(NSString*)appId;

//批量订阅设备
- (void)subscribeDevices:(NSArray*)devices;

//发送设备指令
- (NSError*)device:(NSString*)did sendCmd:(NSDictionary*)attrs;

//关闭连接
- (void)closeWebSocket;

@end

NS_ASSUME_NONNULL_END
