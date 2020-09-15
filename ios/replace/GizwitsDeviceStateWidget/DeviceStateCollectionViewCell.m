//
//  DeviceStateCollectionViewCell.m
//  GizwitsDeviceStateWidget
//
//  Created by william Zhang on 2020/2/3.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "DeviceStateCollectionViewCell.h"

@interface DeviceStateCollectionViewCell ()<GizStateConfigDelegate>

@property (weak, nonatomic) IBOutlet UIView *iconView;

@property (weak, nonatomic) IBOutlet UIImageView *imageView;

@property (weak, nonatomic) IBOutlet UILabel *valueLabel;

@property (weak, nonatomic) IBOutlet UILabel *titleLabel;

@property (weak, nonatomic) IBOutlet UILabel *nameLabel;

@property (nonatomic, strong) NSString* iconUrl;

@property (nonatomic, strong) GizStateConfig* currentConfig;

@end


@implementation DeviceStateCollectionViewCell

-(void)awakeFromNib{
  [super awakeFromNib];
  self.tintColor = [UIColor colorWithRed:255/255.0 green:179/255.0 blue:46/255.0 alpha:1];
}

-(void)setConfig:(GizStateConfig *)config{
  self.currentConfig = config;
  self.currentConfig.delegate = self;
  [self updateInfo];
  self.nameLabel.text = config.device.name;
  self.valueLabel.adjustsFontSizeToFitWidth = YES;
  self.titleLabel.adjustsFontSizeToFitWidth = YES;
}

-(void)setIcon:(NSString *)url{
    self.imageView.hidden = NO;
    if(self.iconUrl && [self.iconUrl isEqualToString:url]){
        return;
    }
    self.iconUrl = url;
    NSData* data = [NSData dataWithContentsOfURL:[NSURL URLWithString:self.iconUrl]];
    [self.imageView setImage:[UIImage imageWithData:data]];
}


-(void)updateInfo{
    NSDictionary* info = [self.currentConfig currentInfo];
    NSString* title = info[@"title"];
    if(title){
        self.titleLabel.text = title;
    } else{
        self.titleLabel.text = @"";
    }
    NSString* image = info[@"image"];
    if(image){
        self.valueLabel.hidden = YES;
        [self setIcon:image];
    } else{
        self.valueLabel.hidden = NO;
        self.imageView.hidden = YES;
        NSString* value = info[@"value"];
//        if(value.length < 2){
//            [self.valueLabel setFont:[UIFont systemFontOfSize:35]];
//        } else{
//            [self.valueLabel setFont:[UIFont systemFontOfSize:25]];
//        }
        self.valueLabel.text = value;
    }
    self.contentView.alpha = self.currentConfig.device.is_online?1:0.5;
}

#pragma mark - GizStateConfigDelegate
-(void)deviceOnlineStatusChange:(BOOL)is_online{
    [self updateInfo];
}

-(void)attrsChange{
    [self updateInfo];
}

@end
