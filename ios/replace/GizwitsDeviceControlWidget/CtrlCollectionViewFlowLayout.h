//
//  CtrlCollectionViewFlowLayout.h
//  GizwitsDeviceControlWidget
//
//  Created by william Zhang on 2020/4/12.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface CtrlCollectionViewFlowLayout : UICollectionViewFlowLayout

@property (nonatomic, assign, readonly)NSUInteger lineCount; // 一行多少item，默认4；

@property (nonatomic, assign, readonly)NSUInteger itemHeight; // item的高度；

@property (nonatomic, assign, readonly)CGFloat itemSpacing;

@property (nonatomic, assign)CGFloat extraHeight; // 菜单的高度

@property (nonatomic, strong) NSIndexPath* activeIndexPath;

-(instancetype)initWithWidth:(CGFloat)width;

// 通过下标获取单元格的centerX
-(CGFloat)getCenterXFromIndex:(NSUInteger)index;


@end

NS_ASSUME_NONNULL_END
