//
//  GizWidgetAppManager.m
//  WidgetTest
//
//  Created by william Zhang on 2019/12/25.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "GizWidgetAppManager.h"
#import "GizWebSocket.h"

NSString * const kWidgetUserId = @"kWidgetAppGroupUserId";
NSString * const kWidgetUserToken = @"kWidgetAppGroupUserToken";
NSString * const kWidgetAppId = @"kWidgetAppGroupAppId";
NSString * const kWidgetOpenUrl = @"kWidgetAppGroupOpenUrl";
NSString * const kWidgetM2MUrl = @"kWidgetAppGroupM2MUrl";
NSString * const kWidgetM2MStageUrl = @"kWidgetAppGroupM2MStageUrl";
NSString * const kWidgetAepUrl = @"kWidgetAppGroupAepUrl";


NSString * const kDeviceControlList = @"kWidgetAppGroupDeviceControlList";

@interface GizWidgetAppManager ()<GizWebSocketDelegate>

@property (nonatomic, strong) NSDictionary* appInfoKeyInfo;

@property (nonatomic, strong) NSString *groupId;

@property (nonatomic, strong) NSUserDefaults *userDefaults;

@property (nonatomic, strong) NSArray* bindDeviceList;  //openAPI请求结果

@property (nonatomic, strong) GizWebSocket* webSocket;

@property (nonatomic, strong) NSArray* contrlDeviceList;

@property (nonatomic, strong) NSMutableArray* connections; //存放websocket

@end

@implementation GizWidgetAppManager

+ (instancetype)defaultManager {
    static GizWidgetAppManager *_defaultManager = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _defaultManager = [[GizWidgetAppManager alloc] init];
        NSDictionary *dict = [NSBundle mainBundle].infoDictionary;
        _defaultManager.appInfoKeyInfo = @{
            @"uid":kWidgetUserId,
            @"token":kWidgetUserToken,
            @"appKey":kWidgetAppId,
            @"openUrl":kWidgetOpenUrl,
            @"m2mUrl":kWidgetM2MUrl,
            @"m2mStageUrl":kWidgetM2MStageUrl,
            @"aepUrl":kWidgetAepUrl};
        _defaultManager.bindDeviceList = [NSArray array];
        _defaultManager.connections = [NSMutableArray array];
        [_defaultManager setupWithGroupId:dict[@"AppGroupId"]];
    });
    
    return _defaultManager;
}

- (void)setupWithGroupId:(NSString *)groupId {
    NSAssert([groupId length] > 0, @"App Group ID 不能为空...");
    self.groupId = groupId;
    self.userDefaults = [[NSUserDefaults alloc] initWithSuiteName:groupId];
    NSArray* list =[self.userDefaults objectForKey:kDeviceControlList];
    NSMutableArray* arr = [NSMutableArray new];
    for (NSDictionary* dic in list) {
        GizControlConfig* config = [GizControlConfig configByDictionary:dic];
        [arr addObject:config];
    }
    self.contrlDeviceList = arr;
}

-(void)startSocket{
    if([self getToken] && [self getAppId] && [self getOpenApi]){
        [self getUserBindDeviceList];
    } else{
        NSLog(@"开启Socket失败，初始化信息不能为空...");
    }
}

-(void)stopSocket{
    if(self.webSocket){
      [self.webSocket closeWebSocket];
    }
}

- (void)setUpAppInfo:(NSDictionary*)params{
    if (!self.userDefaults) {
           NSLog(@"请先调用初始化方法 setupWithGroupId: ");
           return;
    }
    for (NSString* infoKey in self.appInfoKeyInfo) {
        NSString* value = params[infoKey];
        NSString* savekey = self.appInfoKeyInfo[infoKey];
        if(value){
           [self.userDefaults setObject:value forKey:savekey];
        }
    }
    [self.userDefaults synchronize];
}

#pragma mark - private
- (NSString *)getToken {
    return [self.userDefaults stringForKey:kWidgetUserToken];
}

-(NSString*)getOpenApi{
    return [self.userDefaults objectForKey:kWidgetOpenUrl];
}

-(NSString*)getM2mUrl{
    return [self.userDefaults objectForKey:kWidgetM2MUrl];
}

-(NSString*)getM2mStageUrl{
    return [self.userDefaults objectForKey:kWidgetM2MStageUrl];
}

-(NSString*)getAppId{
    return [self.userDefaults objectForKey:kWidgetAppId];
}

-(NSString*)getUid{
    return [self.userDefaults objectForKey:kWidgetUserId];
}

-(GizDevice*)findDeviceFromDid:(NSString*)did{
    for (int i = 0; i<self.bindDeviceList.count; i++) {
        GizDevice* device = self.bindDeviceList[i];
        if([device.did isEqualToString:did]){
            return device;
        }
    }
    return NULL;
}

-(GizWebSocket*)getSocketByDevuceHost:(NSString*)host{
    NSString* targetUrl;
    NSRange range = [host rangeOfString:@"stage"];
    if(range.location != NSNotFound){
        targetUrl = [self getM2mStageUrl];
    } else{
        targetUrl = [self getM2mUrl];
    }
    GizWebSocket* socket;
    for (int i = 0; i<self.connections.count; i++) {
        GizWebSocket* socketItem = self.connections[i];
        if([socketItem.url isEqualToString:targetUrl]){
            socket = socketItem;
            break;
        }
    }
    if(!socket){
        socket = [[GizWebSocket alloc]init];
        [socket setUpByUid:[self getUid] Token:[self getToken] m2mUrl:targetUrl AppId:[self getAppId]];
        socket.delegate = self;
        [self.connections addObject:socket];
    }
    return socket;
}

-(void)updateUserBindDeviceList:(NSArray*)devices{
    if(devices && devices.count > 0){
        NSMutableArray* arr = [NSMutableArray new];
        for (int i = 0; i < devices.count; i++) {
            BOOL isExist = NO;
            NSDictionary* dic = devices[i];
            for (int j = 0; j<self.bindDeviceList.count; j++) {
                GizDevice* oldDevice = self.bindDeviceList[j];
                if([oldDevice isSameFromDictionary:dic]){
                    isExist = YES;
                    [oldDevice updateByDictionary:devices[i]];
                    [arr addObject:oldDevice];
                    break;
                }
            }
            if(!isExist){
                GizDevice* device = [GizDevice deviceByDictionary:dic];
                [arr addObject:device];
            }
        }
        self.bindDeviceList = [NSArray arrayWithArray:arr];
    } else{
        self.bindDeviceList = [NSArray array];
    }
    [self subscribeDevices];
}

-(void)subscribeDevices{
    //以url为key，需要订阅的did数组为值
    NSMutableDictionary* didDic = [NSMutableDictionary dictionary];
    if(self.contrlDeviceList && self.contrlDeviceList.count > 0){
        NSMutableArray* contrlDevices = [NSMutableArray new];
        for (GizControlConfig* config in self.contrlDeviceList) {
            for (int i = 0; i<self.bindDeviceList.count; i++) {
                GizDevice* d = self.bindDeviceList[i];
                if([d isSameFromDictionary:[config deviceInfo]]){
                    GizWebSocket* s = [self getSocketByDevuceHost:d.host];
                    NSMutableArray* arr = didDic[s.url];
                    if(!arr){
                        arr = [NSMutableArray new];
                        [didDic setValue:arr forKey:s.url];
                    }
                    [arr addObject:@{@"did":d.did}];
                    [contrlDevices addObject:d];
                }
            }
        }
        if([self.controlDeviceListdelegate respondsToSelector:@selector(controlDeviceListChange:)]){
            [self.controlDeviceListdelegate controlDeviceListChange:contrlDevices];
        }
    }
    for (NSString* key in [didDic allKeys]) {
        GizWebSocket* s = [self getSocketByDevuceHost:key];
        [s subscribeDevices:didDic[key]];
    }
}

#pragma mark - 设备快捷控制模块
-(void)saveDeviceControlList:(NSArray *)list{
    NSMutableArray* arr = [NSMutableArray new];
    for (NSDictionary* dic in list) {
        GizControlConfig* config = [GizControlConfig configByDictionary:dic];
        [arr addObject:config];
    }
    self.contrlDeviceList = arr;
    if([self.controlDeviceListdelegate respondsToSelector:@selector(configDeviceListChange:)]){
        [self.controlDeviceListdelegate configDeviceListChange:self.contrlDeviceList];
    }
    [self.userDefaults setObject:list forKey:kDeviceControlList];
    [self.userDefaults synchronize];
    if (self.webSocket) {
        //如果是启动状态，就刷新设备列表
        [self getUserBindDeviceList];
    }
}

-(NSArray *)getDeviceControlDictionaryList{
    return [self.userDefaults objectForKey:kDeviceControlList];
}

-(NSArray *)getDeviceControlList{
    return self.contrlDeviceList;
}

-(void)clearDeviceControlList{
    [self.userDefaults removeObjectForKey:kDeviceControlList];
}

-(void)controlDevice:(GizDevice*)device Attrs:(NSDictionary*)attrs{
    device.deviceData = attrs;  //预设数据点
    GizWebSocket* s = [self getSocketByDevuceHost:device.host];
    if(s){
        [s device:device.did sendCmd:attrs];
    }
}

#pragma mark - GizWebSocketDelegate
-(void)device:(NSString *)did ReceivedAttrs:(NSDictionary *)attrs{
    GizDevice* device = [self findDeviceFromDid:did];
    if(device){
        device.deviceData = attrs;
    }
}

-(void)device:(NSString *)did OnlineChange:(BOOL)isOnline{
    GizDevice* device = [self findDeviceFromDid:did];
    if(device){
        device.is_online = isOnline;
    }
}

#pragma mark - OpenApi
-(void)openApiRequest:(NSString*)url parmas:(NSDictionary*)params  Method:(NSString*)method completion:(void (^)(GizOpenApiResult * _Nonnull))completionHandler {
    NSURLSessionConfiguration *sessionConfig = [NSURLSessionConfiguration defaultSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfig];
    NSString* targetUrl = [[self getOpenApi] stringByAppendingString:url];
    NSURL *URL = [NSURL URLWithString:targetUrl];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:URL];
    request.HTTPMethod = method;
    [request addValue:[self getAppId] forHTTPHeaderField:@"X-Gizwits-Application-Id"];
    [request addValue:[self getToken] forHTTPHeaderField:@"X-Gizwits-User-token"];
     [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    if(params){
         [request setHTTPBody:[NSJSONSerialization dataWithJSONObject:params options:NSJSONWritingPrettyPrinted error:nil]];
    }
    NSURLSessionDataTask *task = [session dataTaskWithRequest:request completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        GizOpenApiResult* r = [GizOpenApiResult resultFromData:data Response:response Error:error];
        completionHandler(r);
    }];
    
    [task resume];
}


-(void)getUserBindDeviceList{
    [self openApiRequest:@"app/bindings?skip=0&&limit=1000" parmas:NULL Method:@"GET" completion:^(GizOpenApiResult * _Nonnull result) {
        if(result.success){
            NSLog(@"getUserBindDeviceList:%@",result.data[@"devices"]);
            NSArray* devices = result.data[@"devices"];
            [self updateUserBindDeviceList:devices];
        }
    }];
}

@end
