//
//  TodayViewController.m
//  DeviceControlWidget
//
//  Created by william Zhang on 2019/12/24.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import "TodayViewController.h"
#import <NotificationCenter/NotificationCenter.h>
#import "DeviceCtrlCollectionViewCell.h"
#import "GizWidgetAppManager.h"

@interface TodayViewController () <NCWidgetProviding,UICollectionViewDelegate, UICollectionViewDataSource,GizDeviceControlWidgetDelegate>

@property (weak, nonatomic) IBOutlet UICollectionView *collectionView;

@property (strong, nonatomic) GizWidgetAppManager* widgetAppManager;

@property (strong, nonatomic) NSString* groupId;

@property (strong, nonatomic) NSArray* deviceList;

@property (strong, nonatomic) NSArray* configDevieList;

@property (assign, nonatomic) CGSize viewSize;

@property (assign, nonatomic) CGFloat unitHeight;

@property (assign, nonatomic) BOOL isInitCollectionLayout;

@property (assign, nonatomic) NCWidgetDisplayMode widgetMode;

@property (strong, nonatomic) NSArray* dataSource;


@end

@implementation TodayViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.dataSource = [NSArray array];
    self.widgetAppManager = [GizWidgetAppManager defaultManager];
    self.widgetAppManager.controlDeviceListdelegate = self;
    [self initData];
    [self.widgetAppManager startSocket];
    self.collectionView.delegate = self;
    self.collectionView.dataSource = self;
    self.extensionContext.widgetLargestAvailableDisplayMode = NCWidgetDisplayModeExpanded;

    // Do any additional setup after loading the view.
}

-(void)viewWillDisappear:(BOOL)animated{
    [super viewWillDisappear:animated];
    NSLog(@"小组件 viewWillDisappear");
    [self.widgetAppManager stopSocket];
}

-(void)initData{
    self.configDevieList = [self.widgetAppManager getDeviceControlList];
    self.deviceList = [NSArray array];
}

-(void)updateDataSource{
    NSMutableArray* data = [NSMutableArray new];
    for (GizControlConfig* config in self.configDevieList) {
        for (int i = 0; i<self.deviceList.count; i++) {
            GizDevice* device = self.deviceList[i];
            if([device isSameFromDictionary:[config deviceInfo]]){
                config.device = device;
                [data addObject:config];
                break;
            }
        }
    }
    self.dataSource = data;
    dispatch_async(dispatch_get_main_queue(), ^{
        [self.collectionView reloadData];
    });
}

-(void)initCollectionView:(CGSize)size{
    NSLog(@"initCollectionView");
    CGFloat spacing = 20;
    NSUInteger lineCount = 4;
    if(self.viewSize.width != size.width){
        self.viewSize = size;
        CGFloat screenWidth = self.viewSize.width;
        CGFloat margin = 30;
        CGFloat width = (screenWidth - 20 - ((lineCount-1)*margin))/lineCount;
        CGFloat height = width+20;
        self.unitHeight = height;
        UICollectionViewFlowLayout *layout = [UICollectionViewFlowLayout new];
        layout.scrollDirection = UICollectionViewScrollDirectionVertical;
        layout.itemSize = CGSizeMake(width, height);
        layout.minimumLineSpacing = spacing;
        layout.minimumInteritemSpacing = spacing;
        layout.sectionInset = UIEdgeInsetsMake(0, 0, spacing, 0);
        [self.collectionView setCollectionViewLayout:layout];
    }
    NSUInteger line = self.deviceList.count/lineCount;
    if(self.deviceList.count%lineCount > 0){
        line++;
    }
    if(self.widgetMode == NCWidgetDisplayModeCompact){
       self.preferredContentSize = CGSizeMake(self.viewSize.width,(self.unitHeight+spacing)*1);
    } else {
       self.preferredContentSize = CGSizeMake(self.viewSize.width,(self.unitHeight+spacing)*line);
    }
}


#pragma mark - NCWidgetProviding
- (void)widgetPerformUpdateWithCompletionHandler:(void (^)(NCUpdateResult))completionHandler {
    // Perform any setup necessary in order to update the view.
    
    // If an error is encountered, use NCUpdateResultFailed
    // If there's no update required, use NCUpdateResultNoData
    // If there's an update, use NCUpdateResultNewData
    NSLog(@"widgetPerformUpdateWithCompletionHandler");
    completionHandler(NCUpdateResultNewData);
}

-(void)widgetActiveDisplayModeDidChange:(NCWidgetDisplayMode)activeDisplayMode withMaximumSize:(CGSize)maxSize{
    NSLog(@"widgetActiveDisplayModeDidChange:%ld %.2f %.2f",activeDisplayMode,maxSize.width,maxSize.height);
    self.widgetMode = activeDisplayMode;
    [self initCollectionView:maxSize];
}


#pragma mark - UICollectionViewDelegate
- (NSInteger)collectionView:(UICollectionView *)collectionView numberOfItemsInSection:(NSInteger)section
{
    return self.dataSource.count;
}

-(UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath
{
    DeviceCtrlCollectionViewCell* cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"DeviceCtrlCollectionViewCell" forIndexPath:indexPath];
    GizControlConfig* config = self.dataSource[indexPath.item];
    [cell setConfig:config];
    return cell;
}


-(void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath
{
    GizControlConfig* config = self.dataSource[indexPath.item];
    NSLog(@"设备点击");
    NSDictionary* dic = [config getNextAttrs];
    if(dic && config.device.is_online){
        [self.widgetAppManager controlDevice:config.device Attrs:dic];
    }
}

#pragma mark - GizDeviceControlWidgetDelegate
-(void)controlDeviceListChange:(NSArray *)deviceList{
    self.deviceList = deviceList;
    [self updateDataSource];
}

-(void)configDeviceListChange:(NSArray *)configDeviceList{
    self.configDevieList = configDeviceList;
    [self updateDataSource];
}

@end
