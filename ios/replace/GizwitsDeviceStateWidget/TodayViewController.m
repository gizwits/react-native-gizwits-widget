//
//  TodayViewController.m
//  DeviceControlWidget
//
//  Created by william Zhang on 2019/12/24.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import "TodayViewController.h"
#import <NotificationCenter/NotificationCenter.h>
#import "DeviceStateCollectionViewCell.h"
#import "GizWidgetAppManager.h"

@interface TodayViewController () <NCWidgetProviding,UICollectionViewDelegate, UICollectionViewDataSource,GizDeviceStateWidgetDelegate>

@property (weak, nonatomic) IBOutlet UICollectionView *collectionView;

@property (strong, nonatomic) GizWidgetAppManager* widgetAppManager;

@property (strong, nonatomic) NSArray* deviceList;

@property (strong, nonatomic) NSArray* configDevieList;

@property (assign, nonatomic) CGSize viewSize;

@property (assign, nonatomic) CGFloat unitHeight;

@property (assign, nonatomic) NCWidgetDisplayMode widgetMode;

@property (strong, nonatomic) NSArray* dataSource;

@property (weak, nonatomic) IBOutlet UIView *noneTipView;

@property (weak, nonatomic) IBOutlet UILabel *noneTiltleLabel;

@property (weak, nonatomic) IBOutlet UIButton *addButton;


@end

@implementation TodayViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    NSLog(@"小组件 viewDidLoad");
    self.addButton.layer.borderWidth = 1;
    self.addButton.layer.cornerRadius = 15;
    self.addButton.layer.borderColor = [UIColor colorNamed:@"textWhiteColor"].CGColor;
    self.widgetAppManager = [GizWidgetAppManager defaultManager];
    self.widgetAppManager.stateDeviceListdelegate = self;
    [self initData];
    [self.widgetAppManager startSocket];
    self.collectionView.delegate = self;
    self.collectionView.dataSource = self;
    self.extensionContext.widgetLargestAvailableDisplayMode = NCWidgetDisplayModeExpanded;
    // Do any additional setup after loading the view.
}

-(void)viewWillAppear:(BOOL)animated{
    [super viewWillAppear:animated];
    NSLog(@"小组件 viewWillAppear");
    [self.widgetAppManager startSocket];
    self.configDevieList = [self.widgetAppManager getDeviceStateList];
    [self updateDevicesDataSource:YES];
}

-(void)viewWillDisappear:(BOOL)animated{
    [super viewWillDisappear:animated];
    NSLog(@"小组件 viewWillDisappear");
    [self.widgetAppManager stopSocket];
}

-(void)viewWillLayoutSubviews{
    [super viewWillLayoutSubviews];
    self.addButton.layer.borderColor = [UIColor colorNamed:@"textWhiteColor"].CGColor;
}

-(void)initData{
    self.configDevieList = [self.widgetAppManager getDeviceStateList];
    self.deviceList = [self.widgetAppManager getBindDeviceList];
    [self updateDevicesDataSource:NO];
}

-(void)updateDevicesDataSource:(BOOL)needReload{
    NSMutableArray* data = [NSMutableArray new];
    for (GizStateConfig* config in self.configDevieList) {
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
    if(needReload){
        dispatch_async(dispatch_get_main_queue(), ^{
               [self.collectionView reloadData];
        });
    }
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
        CGFloat height = (width/1.2)+17+5;
        self.unitHeight = height;
        UICollectionViewFlowLayout *layout = [UICollectionViewFlowLayout new];
        layout.scrollDirection = UICollectionViewScrollDirectionVertical;
        layout.itemSize = CGSizeMake(width, height);
        layout.minimumLineSpacing = spacing;
        layout.minimumInteritemSpacing = spacing;
        layout.sectionInset = UIEdgeInsetsMake(0, 0, spacing, 0);
        [self.collectionView setCollectionViewLayout:layout];
    }
    NSUInteger line = self.dataSource.count/lineCount;
    if(self.dataSource.count%lineCount > 0){
        line++;
    }
    if(self.widgetMode == NCWidgetDisplayModeCompact){
       self.preferredContentSize = CGSizeMake(self.viewSize.width,(self.unitHeight+spacing)*1);
    } else {
       self.preferredContentSize = CGSizeMake(self.viewSize.width,(self.unitHeight+spacing)*line);
    }
}

-(void)showNoDeviceTip{
    self.noneTipView.hidden = NO;
}

-(void)hiddenDeviceTip{
    self.noneTipView.hidden = YES;
}

- (IBAction)addButtonClick:(id)sender {
    //打开app
    [self.extensionContext openURL:[NSURL URLWithString:@"GizSmartHome://action=widgetStateDevice"] completionHandler:^(BOOL success) {
        NSLog(@"GizSmartHome open url result:%d",success);
    }];
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
    if(self.dataSource.count < 1){
       [self showNoDeviceTip];
   } else{
       [self hiddenDeviceTip];
   }
   NSLog(@"self.dataSource.count:%d",self.dataSource.count);
   return self.dataSource.count;
}

-(UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath
{
    DeviceStateCollectionViewCell* cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"DeviceStateCollectionViewCell" forIndexPath:indexPath];
    GizStateConfig* config = self.dataSource[indexPath.item];
    [cell setConfig:config];
    return cell;
}


//-(void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath
//{
//}

#pragma mark - GizDeviceStateWidgetDelegate
-(void)controlDeviceListChange:(NSArray *)deviceList{
    NSLog(@"controlDeviceListChange:%ld",deviceList.count);
    self.deviceList = deviceList;
    [self updateDevicesDataSource:YES];
}

-(void)configDeviceListChange:(NSArray *)configDeviceList{
    NSLog(@"configDeviceListChange:%ld",configDeviceList.count);
    self.configDevieList = configDeviceList;
    [self updateDevicesDataSource:YES];
}

@end
