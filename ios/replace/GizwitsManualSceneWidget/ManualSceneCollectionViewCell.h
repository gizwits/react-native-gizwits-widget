//
//  ManualSceneCollectionViewCell.h
//  GizwitsManualSceneWidget
//
//  Created by william Zhang on 2020/2/17.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import <UIKit/UIKit.h>
#import "GizManualScene.h"

NS_ASSUME_NONNULL_BEGIN

@interface ManualSceneCollectionViewCell : UICollectionViewCell

@property(nonatomic, strong) UIColor* tintColor;

-(void)setScene:(GizManualScene*)scene;

-(void)setHighLightStyle;

@end

NS_ASSUME_NONNULL_END
