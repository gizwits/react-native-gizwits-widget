//
//  DeviceCtrlCollectionViewCell.h
//  DeviceControlWidget
//
//  Created by william Zhang on 2019/12/25.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "GizControlConfig.h"

NS_ASSUME_NONNULL_BEGIN

@interface DeviceCtrlCollectionViewCell : UICollectionViewCell

@property(nonatomic, strong) UIColor* tintColor;

-(void)setConfig:(GizControlConfig*)config;

//-(void)setConfigItem:(GizConfigItem*)configItem;

@end

NS_ASSUME_NONNULL_END
