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


NSString * const kWidgetLanguageKey = @"kWidgetLanguageKey";
NSString * const kWidgetUserId = @"kWidgetAppGroupUserId";
NSString * const kWidgetUserToken = @"kWidgetAppGroupUserToken";
NSString * const kWidgetAppId = @"kWidgetAppGroupAppId";
NSString * const kWidgetOpenUrl = @"kWidgetAppGroupOpenUrl";
NSString * const kWidgetM2MUrl = @"kWidgetAppGroupM2MUrl";
NSString * const kWidgetM2MStageUrl = @"kWidgetAppGroupM2MStageUrl";
NSString * const kWidgetAepUrl = @"kWidgetAppGroupAepUrl";
NSString * const kWidgetTintColor = @"kWidgetAppGroupTintColor";

NSString * const kDeviceControlList = @"kWidgetAppGroupDeviceControlList";
NSString * const kDeviceStateList = @"kWidgetAppGroupDeviceStateList";
NSString * const kSceneList = @"kWidgetAppGroupSceneList";

NSString * const kBindDeviceList = @"kBindDeviceList";

@interface GizWidgetAppManager ()<GizWebSocketDelegate>

@property (nonatomic, strong) NSDictionary* appInfoKeyInfo;

@property (nonatomic, strong) NSString *groupId;

@property (nonatomic, strong) NSUserDefaults *userDefaults;

@property (nonatomic, strong) NSMutableDictionary *userDefaultsDic;

@property (nonatomic, strong) NSArray* bindDeviceList;  //openAPI请求结果

@property (nonatomic, strong) GizWebSocket* webSocket;

@property (nonatomic, strong) NSArray* contrlDeviceList;  //配置的快捷控制设备列表

@property (nonatomic, strong) NSArray* stateDeviceList;   //配置的设备状态列表

@property (nonatomic, strong) NSArray* manualSceneList;   //配置的手动场景列表
 
@property (nonatomic, strong) NSMutableArray* connections; //存放websocket

@property (nonatomic, strong) NSString* languageKey;

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
            @"aepUrl":kWidgetAepUrl,
            @"languageKey":kWidgetLanguageKey,
            @"tintColor":kWidgetTintColor,
        };
        _defaultManager.languageKey = @"zh";
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
//    self.userDefaultsDic = [NSMutableDictionary dictionary]; //在这里初始化可变字典会导致闪退
    NSString* languageKey = [self.userDefaults objectForKey:kWidgetLanguageKey];
    if(languageKey){
        self.languageKey = languageKey;
    }
    NSString* tintColorString = [self.userDefaults objectForKey:kWidgetTintColor];
    self.tintColor = [self colorWithHexString:tintColorString];
    {
        [self getDeviceControlList];
    }
    {
        [self getDeviceStateList];
    }
    {
        [self getSceneList];
    }
    {
        NSArray* list = [self.userDefaults objectForKey:kBindDeviceList];
        NSMutableArray* deviceList = [NSMutableArray new];
        for (int i = 0; i<list.count; i++) {
            NSDictionary* dic = list[i];
            GizDevice* device = [GizDevice deviceByDictionary:dic];
            [deviceList addObject:device];
        }
        self.bindDeviceList = deviceList;
    }
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

-(NSArray*)getBindDeviceList{
    return self.bindDeviceList;
}

- (void)setUpAppInfo:(NSDictionary*)params{
    for (NSString* infoKey in self.appInfoKeyInfo) {
        NSString* value = params[infoKey];
        NSString* savekey = self.appInfoKeyInfo[infoKey];
        if(value){
           [self.userDefaults setObject:value forKey:savekey];
        }
    }
    [self.userDefaults synchronize];
}

-(BOOL)checkSetUp{
    return [self getAppId] && [self getToken] && [self getUid] && [self getOpenApi] && [self getM2mUrl] && [self getM2mStageUrl];
}

#pragma mark - private
- (NSString *)getToken {
    return [self.userDefaults stringForKey:kWidgetUserToken];
}

-(NSString*)getOpenApi{
    return [self.userDefaults objectForKey:kWidgetOpenUrl];
}

-(NSString*)getAepApi{
    return [self.userDefaults objectForKey:kWidgetAepUrl];
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

-(GizWebSocket*)getSocketByDeviceHost:(NSString*)host{
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
    [self saveBindDeviceList];
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
                    GizWebSocket* s = [self getSocketByDeviceHost:d.host];
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
    if(self.stateDeviceList && self.stateDeviceList.count > 0){
        NSMutableArray* contrlDevices = [NSMutableArray new];
        for (GizStateConfig* config in self.stateDeviceList) {
            for (int i = 0; i<self.bindDeviceList.count; i++) {
                GizDevice* d = self.bindDeviceList[i];
                if([d isSameFromDictionary:[config deviceInfo]]){
                    GizWebSocket* s = [self getSocketByDeviceHost:d.host];
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
        if([self.stateDeviceListdelegate respondsToSelector:@selector(controlDeviceListChange:)]){
            [self.stateDeviceListdelegate controlDeviceListChange:contrlDevices];
        }
    }
    for (NSString* key in [didDic allKeys]) {
        GizWebSocket* s = [self getSocketByDeviceHost:key];
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
    [self saveObject:list forKey:kDeviceControlList];
    if (self.webSocket) {
        //如果是启动状态，就刷新设备列表
        [self getUserBindDeviceList];
    }
}

-(NSArray *)getDeviceControlDictionaryList{
    return [self objectForKey:kDeviceControlList];
//    return [self.userDefaults objectForKey:kDeviceControlList];
}

-(NSArray *)getDeviceControlList{
    NSArray* list =[self.userDefaults objectForKey:kDeviceControlList];
    NSMutableArray* arr = [NSMutableArray new];
    for (NSDictionary* dic in list) {
        GizControlConfig* config = [GizControlConfig configByDictionary:dic];
        config.languageKey = self.languageKey;
        [arr addObject:config];
    }
    self.contrlDeviceList = arr;
    return self.contrlDeviceList;
}

-(void)clearDeviceControlList{
    [self.userDefaults removeObjectForKey:kDeviceControlList];
    [self.userDefaults synchronize];
}

-(BOOL)controlDevice:(GizDevice*)device Attrs:(NSDictionary*)attrs{
    device.deviceData = attrs;  //预设数据点
    GizWebSocket* s = [self getSocketByDeviceHost:device.host];
    if(s){
       NSError* err =  [s device:device.did sendCmd:attrs];
       return err == NULL;
    }
    return NO;
}


#pragma mark - 设备状态模块
- (void)saveDeviceStateList:(NSArray*)list{
    NSMutableArray* arr = [NSMutableArray new];
    for (NSDictionary* dic in list) {
       GizStateConfig* config = [GizStateConfig configByDictionary:dic];
       [arr addObject:config];
    }
    self.stateDeviceList = arr;
    if([self.stateDeviceListdelegate respondsToSelector:@selector(configDeviceListChange:)]){
       [self.stateDeviceListdelegate configDeviceListChange:self.stateDeviceList];
    }
    [self saveObject:list forKey:kDeviceStateList];
//    [self.userDefaults setObject:list forKey:kDeviceStateList];
//    [self.userDefaults synchronize];
    if (self.webSocket) {
       //如果是启动状态，就刷新设备列表
       [self getUserBindDeviceList];
    }
}

- (NSArray*)getDeviceStateDictionaryList{
    return [self objectForKey:kDeviceStateList];
//     return [self.userDefaults objectForKey:kDeviceStateList];
}

- (NSArray*)getDeviceStateList{
    NSArray* list =[self.userDefaults objectForKey:kDeviceStateList];
    NSMutableArray* arr = [NSMutableArray new];
    for (NSDictionary* dic in list) {
        GizStateConfig* config = [GizStateConfig configByDictionary:dic];
        config.languageKey = self.languageKey;
        [arr addObject:config];
    }
    self.stateDeviceList = arr;
    return self.stateDeviceList;
}

- (void)clearDeviceStateList{
    [self.userDefaults removeObjectForKey:kDeviceStateList];
    [self.userDefaults synchronize];
}

#pragma mark - 场景模块
-(NSArray *)getSceneDictionaryList{
    return [self objectForKey:kSceneList];
//    return [self.userDefaults objectForKey:kSceneList];
}

-(void)saveSceneList:(NSArray *)list{
    NSMutableArray* arr = [NSMutableArray new];
    for (NSDictionary* dic in list) {
       GizManualScene* config = [GizManualScene sceneByDictionary:dic];
       [arr addObject:config];
    }
    self.manualSceneList = arr;
    if([self.manualSceneListdelegate respondsToSelector:@selector(manualSceneListChange:)]){
       [self.manualSceneListdelegate manualSceneListChange:self.manualSceneList];
    }
    [self saveObject:list forKey:kSceneList];
//    [self.userDefaults setObject:list forKey:kSceneList];
//    [self.userDefaults synchronize];
}

-(NSArray *)getSceneList{
    NSArray* list = [self.userDefaults objectForKey:kSceneList];
    NSMutableArray* arr = [NSMutableArray new];
    for (NSDictionary* dic in list) {
        GizManualScene* config = [GizManualScene sceneByDictionary:dic];
        [arr addObject:config];
    }
    self.manualSceneList = arr;
    return self.manualSceneList;
}

-(void)clearSceneList{
    [self.userDefaults removeObjectForKey:kSceneList];
    [self.userDefaults synchronize];
}

-(void)clearAllData{
    [self.userDefaults removeObjectForKey:kSceneList];
    [self.userDefaults removeObjectForKey:kDeviceStateList];
    [self.userDefaults removeObjectForKey:kDeviceControlList];
    [self.userDefaults synchronize];
}

#pragma mark - GizWebSocketDelegate
-(void)device:(NSString *)did ReceivedAttrs:(NSDictionary *)attrs{
    GizDevice* device = [self findDeviceFromDid:did];
    if(device){
        device.deviceData = attrs;
    }
    [self saveBindDeviceList];
}

-(void)device:(NSString *)did OnlineChange:(NSNumber*)isOnline{
    GizDevice* device = [self findDeviceFromDid:did];
    if(device){
        device.is_online = [isOnline boolValue]; 
    }
    [self saveBindDeviceList];
}

-(void)deviceBindChange
{
    // 设备绑定发生变化
    [self getUserBindDeviceList];
}

#pragma mark - AepApi
-(void)aepApiRequest:(NSString*)url parmas:(NSDictionary*)params Method:(NSString*)method completion:(void (^)(GizAepApiResult * _Nonnull))completionHandler{
    NSURLSessionConfiguration *sessionConfig = [NSURLSessionConfiguration defaultSessionConfiguration];
    NSURLSession *session = [NSURLSession sessionWithConfiguration:sessionConfig];
    NSString* targetUrl = [[self getAepApi] stringByAppendingString:url];
    NSURL *URL = [NSURL URLWithString:targetUrl];
    NSMutableURLRequest *request = [NSMutableURLRequest requestWithURL:URL];
    request.HTTPMethod = method;
    [request addValue:[self getAppId] forHTTPHeaderField:@"X-Gizwits-Application-Id"];
    [request addValue:[self getToken] forHTTPHeaderField:@"Authorization"];
    [request addValue:@"1.0" forHTTPHeaderField:@"Version"];
    [request setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
    NSDictionary* data;
    if(params){
        data = params;
    } else {
        data = @{};
    }
    NSDictionary* dic = @{
        @"appKey":[self getAppId],
        @"data":data,
        @"version":@"1.0"
    };
    [request setHTTPBody:[NSJSONSerialization dataWithJSONObject:dic options:NSJSONWritingPrettyPrinted error:nil]];
    
    NSURLSessionDataTask *task = [session dataTaskWithRequest:request completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        GizAepApiResult* r = [GizAepApiResult resultFromData:data Response:response Error:error];
        completionHandler(r);
    }];
    [task resume];
}

-(void)queryManualDetail:(GizManualScene *)scene completion:(void (^)(GizAepApiResult * _Nonnull))completionHandler {
    NSString* urlString = [NSString stringWithFormat:@"app/smartHome/homes/%@/manual_scenes/%@",scene.homeId,scene.sid];
    [self aepApiRequest:urlString parmas:NULL Method:@"POST" completion:completionHandler];
}

-(void)excuteManualScene:(GizManualScene *)scene completion:(void (^)(GizAepApiResult * _Nonnull))completionHandler {
    NSString* urlString = [NSString stringWithFormat:@"app/smartHome/homes/%@/manual_scenes/%@/execute",scene.homeId,scene.sid];
    [self aepApiRequest:urlString parmas:NULL Method:@"POST" completion:completionHandler];
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

-(void)saveBindDeviceList{
    NSMutableArray* list = [NSMutableArray new];
    for (GizDevice* d in self.bindDeviceList) {
        [list addObject:[d deviceInfo]];
    }
    [self.userDefaults setObject:list forKey:kBindDeviceList];
    [self.userDefaults synchronize];
}


#pragma mark - 存储数据使用方法
/**
  NSUserDefault存储后马上读取可能会导致失败，建立NSDictionary作为中间过去会比较好
 */
-(void)saveObject:(nullable id)obj forKey:(NSString*)key{
    if(!self.userDefaultsDic){
        self.userDefaultsDic = [NSMutableDictionary dictionary];
    }
    [self.userDefaults setObject:obj forKey:key];
    [self.userDefaultsDic setObject:obj forKey:key];
    [self.userDefaults synchronize];
}

-(id)objectForKey:(NSString*)key{
    if(self.userDefaultsDic && [self.userDefaultsDic objectForKey:key]){
        return [self.userDefaultsDic objectForKey:key];
    }else{
        return [self.userDefaults objectForKey:key];
    }
}

-(UIColor*)colorWithHexString:(NSString *)hexString {

    if (!hexString || hexString.length == 0) {
      return nil;
    }

    if ([hexString hasPrefix:@"#"]) {
      hexString = [hexString substringFromIndex:1];
    }

    if (hexString.length != 6) {
      return nil;
    }

    unsigned int hexValue;

    [[NSScanner scannerWithString:hexString] scanHexInt:&hexValue];

  return [UIColor colorWithRed:((hexValue >> 16) & 0x000000FF) / 255.0f
                         green:((hexValue >> 8) & 0x000000FF) / 255.0f
                          blue:(hexValue & 0x000000FF) / 255.0f
                         alpha:1.0f];
}

@end

