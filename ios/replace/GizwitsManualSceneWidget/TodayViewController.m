//
//  TodayViewController.m
//  GizwitsManualSceneWidget
//
//  Created by william Zhang on 2020/2/16.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "TodayViewController.h"
#import <NotificationCenter/NotificationCenter.h>
#import "ManualSceneCollectionViewCell.h"
#import "GizWidgetAppManager.h"

@interface TodayViewController () <NCWidgetProviding,UICollectionViewDelegate, UICollectionViewDataSource,GizManualSceneWidgetDelegate>

@property (weak, nonatomic) IBOutlet UICollectionView *collectionView;

@property (strong, nonatomic) GizWidgetAppManager* widgetAppManager;

@property (strong, nonatomic) NSArray* configSceneList;

@property (assign, nonatomic) CGSize viewSize;

@property (assign, nonatomic) CGFloat unitHeight;

@property (assign, nonatomic) NCWidgetDisplayMode widgetMode;

@property (weak, nonatomic) IBOutlet UIView *noneTipView;

@property (weak, nonatomic) IBOutlet UILabel *noneTiltleLabel;

@property (weak, nonatomic) IBOutlet UIButton *addButton;

@end

@implementation TodayViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    self.addButton.layer.borderWidth = 1;
    self.addButton.layer.cornerRadius = 15;
    self.addButton.layer.borderColor = [UIColor colorNamed:@"textWhiteColor"].CGColor;
    self.widgetAppManager = [GizWidgetAppManager defaultManager];
    self.widgetAppManager.manualSceneListdelegate = self;
    [self initData];
    [self.widgetAppManager startSocket];
    self.collectionView.delegate = self;
    self.collectionView.dataSource = self;
    self.extensionContext.widgetLargestAvailableDisplayMode = NCWidgetDisplayModeExpanded;
    // Do any additional setup after loading the view.
}

-(void)viewWillAppear:(BOOL)animated{
    [super viewWillAppear:animated];
    [self updateDataSource:[self.widgetAppManager getSceneList]];
}

-(void)viewWillLayoutSubviews{
    [super viewWillLayoutSubviews];
    self.addButton.layer.borderColor = [UIColor colorNamed:@"textWhiteColor"].CGColor;
}

-(void)initData{
    self.configSceneList = [self.widgetAppManager getSceneList];
}

-(void)showNoDeviceTip{
    self.noneTipView.hidden = NO;
}

-(void)hiddenDeviceTip{
    self.noneTipView.hidden = YES;
}

-(void)updateDataSource:(NSArray*)data{
    self.configSceneList = data;
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
    NSUInteger line = self.configSceneList.count/lineCount;
    if(self.configSceneList.count%lineCount > 0){
        line++;
    }
    if(self.widgetMode == NCWidgetDisplayModeCompact){
       self.preferredContentSize = CGSizeMake(self.viewSize.width,(self.unitHeight+spacing)*1);
    } else {
       self.preferredContentSize = CGSizeMake(self.viewSize.width,(self.unitHeight+spacing)*line);
    }
}


- (IBAction)addButtonClick:(id)sender {
    //打开app
    [self.extensionContext openURL:[NSURL URLWithString:@"GizSmartHome://action=widgetManualScene"] completionHandler:^(BOOL success) {
        NSLog(@"GizSmartHome open url result:%d",success);
    }];
}


-(void)excuteManualScene:(GizManualScene*)scene{
    [self.widgetAppManager excuteManualScene:scene completion:^(GizAepApiResult * _Nonnull result) {
        [scene excuteResult:result.success];
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
    if(self.configSceneList.count < 1){
           [self showNoDeviceTip];
       } else{
           [self hiddenDeviceTip];
       }
    NSLog(@"self.configSceneList.count:%d",self.configSceneList.count);
    return self.configSceneList.count;
}

-(UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath
{
    ManualSceneCollectionViewCell* cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"ManualSceneCollectionViewCell" forIndexPath:indexPath];
    cell.tintColor  = self.widgetAppManager.tintColor;
    GizManualScene* scene = self.configSceneList[indexPath.item];
    [cell setScene:scene];
    return cell;
}

-(void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath
{
    GizManualScene* scene = self.configSceneList[indexPath.item];
    [self excuteManualScene:scene];
}

#pragma mark - GizManualSceneWidgetDelegate
-(void)manualSceneListChange:(NSArray *)sceneList{
    [self updateDataSource:sceneList];
}


@end
