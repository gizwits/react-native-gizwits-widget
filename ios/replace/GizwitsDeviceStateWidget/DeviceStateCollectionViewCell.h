//
//  DeviceStateCollectionViewCell.h
//  GizwitsDeviceStateWidget
//
//  Created by william Zhang on 2020/2/3.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "GizStateConfig.h"

NS_ASSUME_NONNULL_BEGIN

@interface DeviceStateCollectionViewCell : UICollectionViewCell

-(void)setConfig:(GizStateConfig*)config;

@end

NS_ASSUME_NONNULL_END
