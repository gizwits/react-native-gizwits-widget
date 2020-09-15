//
//  GizWebSocket.m
//  WidgetTest
//
//  Created by william Zhang on 2019/12/27.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import "GizWebSocket.h"
#import <SocketRocket/SocketRocket.h>
#import <Foundation/Foundation.h>

NSString * const commType = @"attrs_v4";
NSTimeInterval const keepaliveTime = 180;
BOOL const auto_subscribe = NO;

@interface GizWebSocket ()<SRWebSocketDelegate>

@property (nonatomic, assign) BOOL finishInit;

@property (nonatomic, strong) NSTimer* heartBeatTimer;

@property (nonatomic, assign) NSTimeInterval reConnectTime;

@property (nonatomic,strong) SRWebSocket* webSocket;

@property (nonatomic, assign) BOOL isLogin; //是否已经登录

@property (nonatomic, assign) NSUInteger loginFailedTimes;  //记录失败次数

@property (nonatomic, strong) NSString* token;

@property (nonatomic, strong) NSString* m2mUrl;

@property (nonatomic, strong) NSString* uid;

@property (nonatomic, strong) NSString* appId;

@property (nonatomic, strong) NSMutableArray* subscribeDids;  //记录订阅好的设备

@end

@implementation GizWebSocket

-(instancetype)init{
    if(self = [super init]){
        [self setUp];
    }
    return self;
}

-(void)setUp{
    self.isLogin = NO;
    self.finishInit = NO;
    self.subscribeDids = [NSMutableArray new];
}

-(void)setUpByUid:(NSString *)uid Token:(NSString *)token m2mUrl:(NSString *)m2mUrl AppId:(NSString *)appId{
    if(uid && token && m2mUrl && appId){
        self.uid = uid;
        self.token = token;
        self.m2mUrl = m2mUrl;
        self.appId = appId;
        self.finishInit = YES;
        [self connectWithURLString:self.m2mUrl];
    }
}

-(void)subscribeDevices:(NSArray *)devices{
    if(devices && devices.count){
    NSMutableArray* needSubDevices = [NSMutableArray new];
    for (NSDictionary* device in devices) {
        BOOL isExist = NO;
        for (int i = 0; i<self.subscribeDids.count; i++) {
            NSDictionary* oldDevice = self.subscribeDids[i];
            if([device[@"did"] isEqualToString:oldDevice[@"did"]]){
                isExist = YES;
                break;
            }
        }
        if(!isExist){
            [needSubDevices addObject:device];
        }
    }
    if(needSubDevices.count > 0){
        [self.subscribeDids addObjectsFromArray:needSubDevices];
        [self subscribeByDids:needSubDevices];
    }
    }
}

-(NSError*)device:(NSString *)did sendCmd:(NSDictionary *)attrs{
    BOOL isExist = NO;
    for (int i = 0; i < self.subscribeDids.count; i++) {
        NSDictionary* subDevice = self.subscribeDids[i];
        if([did isEqualToString:subDevice[@"did"]]){
            isExist = YES;
            break;
        }
    }
    if(isExist){
        NSDictionary* params = @{@"cmd":@"c2s_write",@"data":@{@"did":did,@"attrs":attrs}};
        [self sendDictionary:params];
        return NULL;
    }
    return [NSError errorWithDomain:@"websocket" code:-1 userInfo:@{@"error_message":@"设备未订阅"}];
}

-(void)closeWebSocket{
    if (self.webSocket){
            //断开连接
            [self.webSocket close];
            self.webSocket = nil;
            //断开连接时销毁心跳
            [self cancelHeartBeat];
    }
}

#pragma mark - private
-(NSString *)url{
    return self.m2mUrl;
}

-(NSData*)dataFromObject:(id)obj error:(NSError **)error{
    return [NSJSONSerialization dataWithJSONObject:obj options:NSJSONWritingPrettyPrinted error:error];
}

-(NSString*)stringFromObject:(id)obj{
    NSString* str = [[NSString alloc]initWithData:[self dataFromObject:obj error:nil] encoding:NSUTF8StringEncoding];
    return str;
}

- (void)connectWithURLString:(NSString *)urlString {
    if (!urlString.length) {
        return;
    }
    if (self.webSocket) {
        return;
    }
    NSURLRequest *urlReq = [NSURLRequest requestWithURL:[NSURL URLWithString:urlString]];
    self.webSocket = [[SRWebSocket alloc]initWithURLRequest:urlReq];
    self.webSocket.delegate = self;
    //开始连接
    [self.webSocket open];
}


- (void)sendDictionary:(NSDictionary*)dic{
    NSString* data = [self stringFromObject:dic];
    [self sendData:data];
}

- (void)sendData:(id)data{
    dispatch_queue_t queue =  dispatch_queue_create("send.queue", NULL);
    __weak typeof(self) weakSelf = self;
    dispatch_async(queue, ^{
        if (weakSelf.webSocket != nil) {
            // 只有 SR_OPEN 开启状态才能调 send 方法，不然要崩
            if (weakSelf.webSocket.readyState == SR_OPEN) {
                [weakSelf.webSocket send:data];    // 发送数据
            } else if (weakSelf.webSocket.readyState == SR_CONNECTING) {
                NSLog(@"正在连接中");
//                [self reconnect];
            } else if (weakSelf.webSocket.readyState == SR_CLOSING || weakSelf.webSocket.readyState == SR_CLOSED) {
                // websocket 断开了，调用 reConnect 方法重连
                [self reconnect];
            }
        } else {
            NSLog(@"没网络，发送失败");
        }
    });
}

-(void)subscribeByDids:(NSArray*)devices{
    if(devices && devices.count > 0){
        [self sendDictionary:@{
            @"cmd":@"subscribe_req",
            @"data":devices
        }];
    }
}

-(void)readDevice:(NSDictionary*)device{
    [self sendDictionary:@{
        @"cmd":@"c2s_read",
        @"data":@{@"did":device[@"did"]}
    }];
}

-(void)login{
    NSDictionary* dic = @{
        @"cmd": @"login_req",
        @"data": @{
                @"appid": self.appId,
                @"uid": self.uid,
                @"token": self.token,
                @"p0_type":commType,
                @"heartbeat_interval": [NSNumber numberWithInteger:keepaliveTime],
                @"auto_subscribe":@NO,
        }};
    [self sendDictionary:dic];
}

-(void)tryLoginAgain{
    //超过一分钟就不再重连 所以只会重连5次 2^5 = 64
    if (self.loginFailedTimes > 3) {
        [self closeWebSocket];
    return;
    }
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(self.loginFailedTimes*5* NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [self login];
        NSLog(@"重新登陆");
    });
    self.loginFailedTimes++;
}

- (void)creatHeartBeat
{
    NSLog(@"创建心跳计时器");
    [self sendHeart];
    dispatch_async(dispatch_get_main_queue(), ^{
        if(self.heartBeatTimer){
            [self cancelHeartBeat];
        }
        __weak typeof(self) weakSelf = self;
        //心跳设置为3分钟，NAT超时一般为5分钟
        NSTimeInterval time = 1*60;
        self.heartBeatTimer = [NSTimer timerWithTimeInterval:time repeats:YES block:^(NSTimer * _Nonnull timer) {
              //发送心跳
              [weakSelf sendHeart];
        }];
        [[NSRunLoop mainRunLoop]addTimer:self.heartBeatTimer forMode:NSRunLoopCommonModes];
    });
}

-(void)sendHeart{
    NSLog(@"发送心跳包");
    [self sendDictionary:@{@"cmd": @"ping"}];
}

-(void)cancelHeartBeat{
    if (self.heartBeatTimer) {
        [self.heartBeatTimer invalidate];
        self.heartBeatTimer = nil;
    }
}

//重连,当发现客户端和服务端断开连接时发起重连
- (void)reconnect
{
    [self closeWebSocket];
    //超过一分钟就不再重连 所以只会重连5次 2^5 = 64
    if (self.reConnectTime > 64) {
        return;
    }
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(self.reConnectTime * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        self.isLogin= NO;
        [self connectWithURLString:self.m2mUrl];
        NSLog(@"重连");
    });
    
    //重连时间2的指数级增长
    if (self.reConnectTime == 0) {
        self.reConnectTime = 2;
    }else{
        self.reConnectTime *= 2;
    }
}


#pragma mark - SRWebSocketDelegate
- (void)webSocketDidOpen:(SRWebSocket *)webSocket {
    NSLog(@"连接成功");
    _reConnectTime = 0;
    [self login];
}


//连接失败
- (void)webSocket:(SRWebSocket *)webSocket didFailWithError:(NSError *)error {
    NSLog(@"连接失败");
    [self reconnect];
}

//被一方关闭连接了
- (void)webSocket:(SRWebSocket *)webSocket didCloseWithCode:(NSInteger)code reason:(NSString *)reason wasClean:(BOOL)wasClean{
    NSLog(@"连接断开了");
    [self reconnect];
}

//接收服务器的pong消息
- (void)webSocket:(SRWebSocket *)webSocket didReceivePong:(nullable NSData *)pongData{
     NSString *reply = [[NSString alloc] initWithData:pongData encoding:NSUTF8StringEncoding];
    NSLog(@"接收到pong消息：%@",reply);
    
}

- (void)webSocket:(SRWebSocket *)webSocket didReceiveMessage:(id)message  {
    //收到服务器发过来的数据
    NSLog(@"接收到发过来的消息%@",message);
    NSData *jsonData = [message dataUsingEncoding:NSUTF8StringEncoding];
    NSError *err;
    NSDictionary* result = [NSJSONSerialization JSONObjectWithData:jsonData options:NSJSONReadingMutableContainers error:&err];;
    NSString* cmd = result[@"cmd"];
    NSDictionary* data = result[@"data"];
    if([cmd isEqualToString:@"login_res"]){
        if(data[@"success"]){
            //开启心跳 心跳是发送pong的消息 我这里根据后台的要求发送data给后台
            [self creatHeartBeat];
            [self subscribeByDids:self.subscribeDids];
        } else{
            [self tryLoginAgain];
        }
    } else if([cmd isEqualToString:@"subscribe_res"]){
        NSArray* successList = data[@"success"];
        for (NSDictionary* device in successList) {
            [self readDevice:device];
        }
       
        NSArray* failedList = data[@"failed"];
        for (NSDictionary* device in failedList) {
            for (int i = 0; i<self.subscribeDids.count; i++) {
                NSDictionary* subDevice = self.subscribeDids[i];
                if([subDevice[@"did"] isEqualToString:device[@"did"]]){
                    [self.subscribeDids removeObjectAtIndex:i];
                    break;
                }
            }
        }
    } else if([cmd isEqualToString:@"s2c_noti"]){
        if([self.delegate respondsToSelector:@selector(device:ReceivedAttrs:)]){
            [self.delegate device:data[@"did"] ReceivedAttrs:data[@"attrs"]];
        }
    } else if([cmd isEqualToString:@"s2c_online_status"]){
        if([self.delegate respondsToSelector:@selector(device:OnlineChange:)]){
            [self.delegate device:data[@"did"] OnlineChange:data[@"online"]];
        }
    }else if([cmd isEqualToString:@"s2c_binding_changed"]){
        if([self.delegate respondsToSelector:@selector(deviceBindChange)]){
            [self.delegate deviceBindChange];
        }
    }  else if([cmd isEqualToString:@"s2c_invalid_msg"]){
        NSNumber* error_code = data[@"error_code"];
        NSLog(@"websocket Error:%@",data);
        if([error_code integerValue] == 1009){
            [self tryLoginAgain];
        }
    }
}

@end
