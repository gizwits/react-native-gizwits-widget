//
//  FuntionCollectionViewCell.h
//  GizwitsDeviceControlWidget
//
//  Created by william Zhang on 2020/4/10.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "GizControlConfig.h"

NS_ASSUME_NONNULL_BEGIN

@interface FuntionCollectionViewCell : UICollectionViewCell

@property (nonatomic, strong) GizControlConfig* config;

@property (nonatomic, strong) UIColor* tintColor;

@property (nonatomic, assign) CGFloat width;

@property (nonatomic, assign) CGFloat arrowLeft; // 箭头位置

// 通过数量计算出需要的cell高度
+(CGFloat)heightFromCount:(NSUInteger)count AndWidth:(CGFloat)width;

@end

NS_ASSUME_NONNULL_END
