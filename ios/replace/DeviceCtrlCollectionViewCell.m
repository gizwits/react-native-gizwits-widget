//
//  DeviceCtrlCollectionViewCell.m
//  DeviceControlWidget
//
//  Created by william Zhang on 2019/12/25.
//  Copyright © 2019 Gziwits. All rights reserved.
//

#import "DeviceCtrlCollectionViewCell.h"

@interface DeviceCtrlCollectionViewCell ()<GizConfigDelegate>

@property (weak, nonatomic) IBOutlet UIView *iconView;

@property (weak, nonatomic) IBOutlet UIImageView *imageView;

@property (weak, nonatomic) IBOutlet UILabel *titleLabel;

@property (nonatomic, strong) GizControlConfig* currentConfig;

@property (nonatomic, strong) NSString* iconUrl;

@end


@implementation DeviceCtrlCollectionViewCell

-(void)setConfig:(GizControlConfig *)config{
    if(config){
        self.currentConfig = config;
        self.currentConfig.delegate = self;
        NSString* icon = [config currentControlIcon];
        [self setTitle:config.device.dev_alias IconUrl:icon];
    }
}

-(void)setTitle:(NSString *)title IconUrl:(NSString *)url{
    [self.titleLabel setText:title];
    [self setIcon:url];
}

-(void)setIcon:(NSString *)url{
    if(self.iconUrl && [self.iconUrl isEqualToString:url]){
        return;
    }
    self.iconUrl = url;
    NSData* data = [NSData dataWithContentsOfURL:[NSURL URLWithString:self.iconUrl]];
    [self.imageView setImage:[UIImage imageWithData:data]];
}

-(void)layoutSubviews
{
    [super layoutSubviews];
    self.iconView.layer.cornerRadius = 15;
//    self.iconView.layer.shadowColor = [UIColor blackColor].CGColor;
//    self.iconView.layer.shadowOpacity = 0.25;
//    self.iconView.layer.shadowRadius = 5;
//    self.iconView.layer.shadowOffset = CGSizeMake(0, 0);
}

#pragma mark - GizConfigDelegate
-(void)attrValueChange{
    NSString* icon = [self.currentConfig currentControlIcon];
    [self setIcon:icon];
}


@end
