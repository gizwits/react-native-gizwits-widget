//
//  CtrlCollectionViewFlowLayout.m
//  GizwitsDeviceControlWidget
//
//  Created by william Zhang on 2020/4/12.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "CtrlCollectionViewFlowLayout.h"

@interface CtrlCollectionViewFlowLayout ()

@property (nonatomic, assign) CGFloat width;

@property (nonatomic, assign) CGFloat itemWidth;

@property (nonatomic, assign) CGFloat spacing;

@property (nonatomic, assign) CGFloat margin;

@property (nonatomic, assign) CGSize cellSize;

@end

@implementation CtrlCollectionViewFlowLayout

-(instancetype)initWithWidth:(CGFloat)width{
    if(self = [super init]){
        [self setUp:width];
    }
    return self;
}


// 自定义设置
-(void)setUp:(CGFloat)width{
    _lineCount = 4;
    self.width = width;
    self.spacing = 20;
    _itemSpacing = self.spacing;
    CGFloat screenWidth = width;
    self.margin = 30;
    CGFloat itemWidth = (screenWidth - 20 - ((_lineCount-1)*self.margin))/_lineCount;
    CGFloat itemHeight = itemWidth+20;
    _itemHeight = itemHeight;
    _itemWidth = itemWidth;
    self.scrollDirection = UICollectionViewScrollDirectionVertical;
    self.itemSize = CGSizeMake(itemWidth, itemHeight);
    self.minimumLineSpacing = self.spacing;
    self.minimumInteritemSpacing = self.spacing;
    self.sectionInset = UIEdgeInsetsMake(0, 0, self.spacing, 0);
    self.cellSize = CGSizeMake(_itemWidth, _itemHeight);
}

-(CGFloat)getCenterXFromIndex:(NSUInteger)index{
    NSUInteger realIndex = index%_lineCount;
    return realIndex*(_itemWidth+self.margin-3.5)+_itemWidth/2-13;
}

#pragma mark - override

-(void)prepareLayout{
    NSLog(@"prepareLayout");
    [super prepareLayout];
}

-(NSArray<UICollectionViewLayoutAttributes *> *)layoutAttributesForElementsInRect:(CGRect)rect{
    //拿到当前视图内的layout组成的数组
    NSLog(@"layoutAttributesForElementsInRect");
   NSArray *temp  =  [super  layoutAttributesForElementsInRect:rect];
   NSMutableArray *attAtrray = [NSMutableArray array];
    for (int i = 0; i < temp.count; i ++) {
        UICollectionViewLayoutAttributes *att = [temp[i] copy]; //做copy操作 消除一下警告
        NSLog(@"UICollectionViewLayoutAttributes:%ld,%.2f,%.2f",att.indexPath.row,att.frame.origin.x,att.frame.origin.y);
        if(self.activeIndexPath){
            if(self.activeIndexPath.row == att.indexPath.row){
                // 计算菜单的 frame
                NSUInteger line =(self.activeIndexPath.row/_lineCount);
                if(self.activeIndexPath.row%_lineCount > 0){
                    line++;
                }
                CGFloat y = line*_itemHeight+(line-1)*self.spacing;
                att.frame = CGRectMake(0, y, self.width-30, self.extraHeight);
            } else{
                if(att.indexPath.row > self.activeIndexPath.row){
                    // 菜单后的frame需要加上菜单高度的偏移量
                    NSUInteger realIndex = att.indexPath.row-1-self.activeIndexPath.row; // 去掉菜单后的Index
                    NSUInteger line = (att.indexPath.row-1)/_lineCount;
                    NSUInteger index = (realIndex)%_lineCount;
                    att.frame = CGRectMake(index*(_itemWidth+self.margin-3.5), line*(_itemHeight+self.spacing)+self.extraHeight, _itemWidth, _itemHeight);
                } else{
                    att.size = self.cellSize;
                }
            }
        } else{
            att.size = self.cellSize;
        }
        [attAtrray addObject:att];
    }
   return  attAtrray;
}

-(UICollectionViewLayoutAttributes *)initialLayoutAttributesForAppearingItemAtIndexPath:(NSIndexPath *)itemIndexPath{
    if(itemIndexPath.row == self.activeIndexPath.row){
        UICollectionViewLayoutAttributes* att = [UICollectionViewLayoutAttributes new];
        NSUInteger line =(self.activeIndexPath.row/_lineCount);
        if(self.activeIndexPath.row%_lineCount > 0){
           line++;
        }
        CGFloat y = line*_itemHeight+(line-1)*self.spacing;
        att.frame = CGRectMake(0, y, self.width-30, 20);
        return att;
    } else{
        return [super initialLayoutAttributesForAppearingItemAtIndexPath:itemIndexPath];
    }
}



@end
