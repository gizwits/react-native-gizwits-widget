//
//  FuntionCollectionViewCell.m
//  GizwitsDeviceControlWidget
//
//  Created by william Zhang on 2020/4/10.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "FuntionCollectionViewCell.h"
#import "CtrlCollectionViewCell.h"
#import "GizWidgetAppManager.h"

const NSUInteger lineCount = 5;
const CGFloat margin = 12;
const CGFloat spacing = 12;
const CGFloat collectionViewMargin = 20;

@interface FuntionCollectionViewCell ()<UICollectionViewDelegate,UICollectionViewDataSource>

@property (weak, nonatomic) IBOutlet NSLayoutConstraint *arrowIconLeftConstraints;

@property (weak, nonatomic) IBOutlet UICollectionView *collectionView;

@end

@implementation FuntionCollectionViewCell

+(CGSize)itemSizeByWidth:(CGFloat)width{
    CGFloat itemWidth = (width - 20 - 2*collectionViewMargin - (lineCount+1)*margin)/lineCount;
    CGFloat itemHeight = itemWidth+15;
    return CGSizeMake(itemWidth, itemHeight);
}

+(CGFloat)heightFromCount:(NSUInteger)count AndWidth:(CGFloat)width{
    CGFloat arrowHeight = 13;
    CGFloat height = 3+arrowHeight+spacing*2;
    NSUInteger line = count/lineCount;
    if(count%lineCount > 0){
        line++;
    }
    CGSize itemSize = [FuntionCollectionViewCell itemSizeByWidth:width];
    height += (itemSize.height*line+(line-1)*spacing);
    return height;
}

- (void)awakeFromNib {
    [super awakeFromNib];
    [self setUp];
    // Initialization code
}

-(void)setUp{
    self.tintColor = [UIColor colorWithRed:255/255.0 green:179/255.0 blue:46/255.0 alpha:1];
    self.collectionView.dataSource = self;
    self.collectionView.delegate = self;
    UINib* cell = [UINib nibWithNibName:@"CtrlCollectionViewCell" bundle:nil];
    [self.collectionView registerNib:cell forCellWithReuseIdentifier:@"CtrlCollectionViewCell"];
    self.collectionView.layer.cornerRadius = 15;
    self.width = self.bounds.size.width;
}

-(void)updateLayout:(CGFloat)width{
    UICollectionViewFlowLayout *layout = [UICollectionViewFlowLayout new];
    layout.scrollDirection = UICollectionViewScrollDirectionVertical;
    layout.itemSize = [FuntionCollectionViewCell itemSizeByWidth:width];
    layout.minimumLineSpacing = spacing;
    layout.minimumInteritemSpacing = margin;
    layout.sectionInset = UIEdgeInsetsMake(spacing, collectionViewMargin, spacing, collectionViewMargin);
    [self.collectionView setCollectionViewLayout:layout];
}

-(void)setWidth:(CGFloat)width{
    if(_width != width){
        _width= width;
        [self updateLayout:_width];
    }
}

-(void)setArrowLeft:(CGFloat)arrowLeft{
    if(_arrowLeft != arrowLeft){
        _arrowLeft = arrowLeft;
        self.arrowIconLeftConstraints.constant= _arrowLeft;
    }
}

-(void)setConfig:(GizControlConfig *)config{
    if(_config != config){
        [self.collectionView reloadData];
    }
    _config = config;
}

#pragma mark - UICollectionViewDelegate
- (NSInteger)collectionView:(UICollectionView *)collectionView numberOfItemsInSection:(NSInteger)section
{
    return self.config.config.count;
}

-(UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath
{
    CtrlCollectionViewCell* cell = [collectionView dequeueReusableCellWithReuseIdentifier:@"CtrlCollectionViewCell" forIndexPath:indexPath];
    cell.tintColor = self.tintColor;
    GizConfigItem* item = self.config.config[indexPath.row];
    [cell setConfigItem:item];
    return cell;
}


-(void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath
{
    GizConfigItem* configItem = self.config.config[indexPath.item];
    NSDictionary* nextOption = [configItem getNextOption];
    NSDictionary* dic = [configItem getCmdFormOption:nextOption];
    if(dic){
        [self.config willSendCmd:nextOption];
        BOOL result = [[GizWidgetAppManager defaultManager] controlDevice:self.config.device Attrs:dic];
        [self.config sendCmd:nextOption result:result];
    }
}


@end
