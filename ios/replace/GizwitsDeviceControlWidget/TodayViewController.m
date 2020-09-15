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
#import "CtrlCollectionViewFlowLayout.h"
#import "FuntionCollectionViewCell.h"

@interface TodayViewController () <NCWidgetProviding,UICollectionViewDelegate, UICollectionViewDataSource,GizDeviceControlWidgetDelegate>

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

//@property (assign, nonatomic) BOOL isShowControl;  //当前显示是控制配置

@property (nonatomic, strong) GizControlConfig* currentConfig;

@property (nonatomic, strong) CtrlCollectionViewFlowLayout* layout;

@property (nonatomic, assign) BOOL showMenu;

@property (nonatomic, strong) NSIndexPath* activeIndexPath;

@property (nonatomic, strong) NSIndexPath* selIndexPath;

@property (nonatomic, assign) CGFloat arrowLeft;

@property (nonatomic, assign) CGFloat menuHeight;

@property (nonatomic, strong) FuntionCollectionViewCell* menuCell;

@end

@implementation TodayViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.menuHeight = 0;
    self.addButton.layer.borderWidth = 1;
    self.addButton.layer.cornerRadius = 15;
    self.addButton.layer.borderColor = [UIColor colorNamed:@"textWhiteColor"].CGColor;
//    self.isShowControl = NO;
    self.widgetAppManager = [GizWidgetAppManager defaultManager];
    self.widgetAppManager.controlDeviceListdelegate = self;
    [self initData];
    [self.widgetAppManager startSocket];
    UINib* nib = [UINib nibWithNibName:@"FuntionCollectionViewCell" bundle:nil];
    [self.collectionView registerNib:nib forCellWithReuseIdentifier:@"FuntionCollectionViewCell"];
    self.collectionView.delegate = self;
    self.collectionView.dataSource = self;
    self.extensionContext.widgetLargestAvailableDisplayMode = NCWidgetDisplayModeExpanded;

    // Do any additional setup after loading the view.
}

-(void)viewWillAppear:(BOOL)animated{
    [super viewWillAppear:animated];
    NSLog(@"小组件 viewWillAppear");
    [self.widgetAppManager startSocket];
    self.configDevieList = [self.widgetAppManager getDeviceControlList];
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
    self.deviceList = [self.widgetAppManager getBindDeviceList];
    self.configDevieList = [self.widgetAppManager getDeviceControlList];
    [self updateDevicesDataSource:NO];
}

-(void)updateDevicesDataSource:(BOOL)needReload{
    NSLog(@"updateDevicesDataSource:%ld %ld ",self.configDevieList.count,self.deviceList.count);
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
    if(needReload){
        dispatch_async(dispatch_get_main_queue(), ^{
             [self.collectionView reloadData];
        });
    }
}

-(void)initCollectionView:(CGSize)size{
    NSLog(@"initCollectionView");
   
    if(self.viewSize.width != size.width){
        self.viewSize = size;
        CtrlCollectionViewFlowLayout* layout = [[CtrlCollectionViewFlowLayout alloc]initWithWidth:size.width];
        self.layout = layout;
        [self.collectionView setCollectionViewLayout:layout];
    }
    [self updatePreferredContentSize];
}

-(void)showNoDeviceTip{
    self.noneTipView.hidden = NO;
}

-(void)hiddenDeviceTip{
    self.noneTipView.hidden = YES;
}

-(void)updatePreferredContentSize{
    CGFloat spacing = self.layout.itemSpacing;
    NSUInteger line = self.dataSource.count/self.layout.lineCount;
    if(self.dataSource.count%self.layout.lineCount > 0){
        line++;
    }
    
    if(self.widgetMode == NCWidgetDisplayModeCompact){
       self.preferredContentSize = CGSizeMake(self.viewSize.width,(self.layout.itemHeight+spacing)*1);
    } else {
        if(self.activeIndexPath){
            self.preferredContentSize = CGSizeMake(self.viewSize.width,(self.layout.itemHeight+spacing)*line+self.menuHeight);
        } else{
            self.preferredContentSize = CGSizeMake(self.viewSize.width,(self.layout.itemHeight+spacing)*line);
        }
    }
}

-(void)showControlMenu:(NSIndexPath *)indexPath{
    BOOL showMenu = NO;
    NSUInteger row = indexPath.row;
    if(self.activeIndexPath){
        if(indexPath.row > self.activeIndexPath.row){
            row--;
        }
        showMenu = indexPath.row != self.selIndexPath.row;
    } else{
        showMenu = YES;
    }

    self.showMenu = showMenu;
    
    if(self.showMenu){
        self.selIndexPath = [NSIndexPath indexPathForRow:row inSection:indexPath.section];
        GizControlConfig* config = self.dataSource[row];
        self.arrowLeft = [self.layout getCenterXFromIndex:row];
        
        self.currentConfig = config;
        self.menuHeight = [FuntionCollectionViewCell heightFromCount:config.config.count AndWidth:self.viewSize.width];
        
        NSUInteger line = row/self.layout.lineCount;
        NSUInteger activeRow = (line+1)*self.layout.lineCount;
        if(activeRow > self.dataSource.count){
           activeRow = self.dataSource.count;
        }
        NSIndexPath*  nActiveIndexPath = [NSIndexPath indexPathForRow:activeRow inSection:indexPath.section];
        self.layout.activeIndexPath = self.activeIndexPath;
        self.layout.extraHeight = self.menuHeight;
        
        if(self.activeIndexPath){
            if(self.activeIndexPath.row != nActiveIndexPath.row){
                // 不同行再切换
                [self.collectionView performBatchUpdates:^{
                    NSIndexPath* oldIndexPath = self.activeIndexPath;
                    self.activeIndexPath = nActiveIndexPath;
                    self.layout.activeIndexPath = nActiveIndexPath;
                    [self.collectionView deleteItemsAtIndexPaths:@[oldIndexPath]];
                    [self.collectionView insertItemsAtIndexPaths:@[nActiveIndexPath]];
                    [self updatePreferredContentSize];
                } completion:^(BOOL finished) {
                    NSLog(@"deleteItemsAtIndexPaths completion：%d",finished);
                }];
            } else{
                // 同行就reload
                [self.collectionView performBatchUpdates:^{
                    self.activeIndexPath = nActiveIndexPath;
                    self.layout.activeIndexPath = nActiveIndexPath;
                    [self.collectionView reloadItemsAtIndexPaths:@[nActiveIndexPath]];
                    [self updatePreferredContentSize];
                } completion:^(BOOL finished) {
                    NSLog(@"deleteItemsAtIndexPaths completion：%d",finished);
                }];
            }
           
        } else{
            // 未显示就新增
            [self.collectionView performBatchUpdates:^{
                self.activeIndexPath = nActiveIndexPath;
                self.layout.activeIndexPath = nActiveIndexPath;
                [self.collectionView insertItemsAtIndexPaths:@[nActiveIndexPath]];
                [self updatePreferredContentSize];
            } completion:^(BOOL finished) {
                NSLog(@"deleteItemsAtIndexPaths completion：%d",finished);
            }];
        }
//           dispatch_async(dispatch_get_main_queue(), ^{
//             [self.collectionView reloadData];
//           });
    } else{
        [self hideMenu];
    }
}

-(void)hideMenu{
    if(self.activeIndexPath){
        [self.collectionView performBatchUpdates:^{
            self.menuHeight = 0;
            self.currentConfig = nil;
            NSIndexPath* oldIndexPath = self.activeIndexPath;
            self.activeIndexPath = nil;
            self.selIndexPath = nil;
            self.layout.activeIndexPath = nil;
            [self.collectionView deleteItemsAtIndexPaths:@[oldIndexPath]];
            [self updatePreferredContentSize];
        } completion:^(BOOL finished) {
            NSLog(@"deleteItemsAtIndexPaths completion：%d",finished);
        }];
    }
}

- (IBAction)addButtonClick:(id)sender {
    //打开app
    [self.extensionContext openURL:[NSURL URLWithString:@"GizSmartHome://action=widgetControlDevice"] completionHandler:^(BOOL success) {
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
    NSLog(@"self.dataSource.count:%d",self.dataSource.count);
    if(self.dataSource.count < 1){
        [self showNoDeviceTip];
        return 0;
    } else{
        NSUInteger count = self.dataSource.count;
        if(self.activeIndexPath){
            count++;
        }
        [self hiddenDeviceTip];
        return count;
    }
}

-(UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath
{
    NSLog(@"cellForItemAtIndexPath:%d,%d",self.activeIndexPath.row,indexPath.row);
    if(self.activeIndexPath && self.activeIndexPath.row == indexPath.row){
        FuntionCollectionViewCell* fCell = [collectionView dequeueReusableCellWithReuseIdentifier:@"FuntionCollectionViewCell" forIndexPath:indexPath];
        fCell.tintColor = self.widgetAppManager.tintColor;
        fCell.width = self.viewSize.width;
        fCell.arrowLeft = self.arrowLeft;
        fCell.config = self.currentConfig;
        return fCell;
    }
    DeviceCtrlCollectionViewCell* cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"DeviceCtrlCollectionViewCell" forIndexPath:indexPath];
    cell.tintColor = self.widgetAppManager.tintColor;
    NSUInteger index = indexPath.row;
    if(self.activeIndexPath && index > self.activeIndexPath.row){
        index--;
    }
    GizControlConfig* config = self.dataSource[index];
    [cell setConfig:config];
    return cell;
}


-(void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath
{
    NSUInteger row = indexPath.row;
    if(self.activeIndexPath && indexPath.row > self.activeIndexPath.row){
       row--;
    }
    GizControlConfig* config = self.dataSource[row];
    NSLog(@"设备点击");
    if(config.device.is_online){
        if(config.config && config.config.count > 0){
            if(config.config.count > 1){
                //多个的话，显示菜单
                [self showControlMenu:indexPath];
            } else{
                [self hideMenu];
                GizConfigItem* item = config.config[0];
                NSDictionary* nextOption = [item getNextOption];
                NSDictionary* dic = [item getCmdFormOption:nextOption];
                if(dic){
                    [config willSendCmd:nextOption];
                    BOOL result = [self.widgetAppManager controlDevice:config.device Attrs:dic];
                    [config sendCmd:nextOption result:result];

                }
            }
        }
    } else{
        [config offlineTip];
    }
}


#pragma mark - GizDeviceControlWidgetDelegate
-(void)controlDeviceListChange:(NSArray *)deviceList{
    NSLog(@"controlDeviceListChange",deviceList.count);
    self.deviceList = deviceList;
    [self updateDevicesDataSource:YES];
}

-(void)configDeviceListChange:(NSArray *)configDeviceList{
    NSLog(@"configDeviceListChange",configDeviceList.count);
    self.configDevieList = configDeviceList;
    [self updateDevicesDataSource:YES];
}

@end
