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

-(void)setConfig:(GizControlConfig*)config;

@end

NS_ASSUME_NONNULL_END
