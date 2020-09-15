//
//  CtrlCollectionViewCell.h
//  GizwitsDeviceControlWidget
//
//  Created by william Zhang on 2020/4/10.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "GizConfigItem.h"

NS_ASSUME_NONNULL_BEGIN

@interface CtrlCollectionViewCell : UICollectionViewCell

@property(nonatomic, strong) UIColor* tintColor;

-(void)setConfigItem:(GizConfigItem*)configItem;

@end

NS_ASSUME_NONNULL_END
