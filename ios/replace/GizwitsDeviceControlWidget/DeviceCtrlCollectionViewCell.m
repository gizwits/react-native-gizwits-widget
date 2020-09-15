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

@property (weak, nonatomic) IBOutlet UIImageView *offlineImageView;

@property (nonatomic, strong) GizControlConfig* currentConfig;

@property (nonatomic, strong) NSString* iconUrl;

//@property (nonatomic, strong) GizConfigItem* currentConfigItem;

@end


@implementation DeviceCtrlCollectionViewCell

-(void)awakeFromNib{
  [super awakeFromNib];
  self.tintColor = [UIColor colorWithRed:255/255.0 green:179/255.0 blue:46/255.0 alpha:1];
}

-(void)setConfig:(GizControlConfig *)config{
    self.currentConfig = config;
    self.currentConfig.delegate = self;
    self.titleLabel.text = [config getStateName];
    if(config.device.is_online == YES){
        [self setOnlineStyle];
    } else{
        [self setOfflineStyle];
    }
}

//-(void)setConfigItem:(GizConfigItem *)configItem{
//    if(self.currentConfig){
//        self.currentConfig.delegate = NULL;
//        self.currentConfig = NULL;
//    }
//    if(configItem){
//        self.currentConfigItem = configItem;
//        self.currentConfigItem.delegate = self;
//        [self.titleLabel setText:configItem.editName];
//        [self setIcon:[self.currentConfigItem currentControlIcon]];
//        if ([configItem isInOptionRange]) {
//            [self setItemHighLightStyle];
//        } else{
//            [self setItemNorStyle];
//        }
//    }
//}

-(void)setIcon:(NSString *)url{
    if(self.iconUrl && [self.iconUrl isEqualToString:url]){
        return;
    }
    self.iconUrl = url;
    NSData* data = [NSData dataWithContentsOfURL:[NSURL URLWithString:self.iconUrl]];
    UIImage* image = [UIImage imageWithData:data];
    image = [image imageWithRenderingMode:UIImageRenderingModeAlwaysTemplate];
    [self.imageView setImage:image];
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


#pragma mark - 设置设备样式
-(void)setNorStyle{
    [self setIcon:self.currentConfig.icon];
    self.iconView.backgroundColor = [UIColor clearColor];
    self.imageView.tintColor = [UIColor whiteColor];
}

-(void)setHighLightStyle{
    [self setIcon:self.currentConfig.icon];
    self.iconView.backgroundColor = [UIColor whiteColor];
    self.imageView.tintColor = self.tintColor;
}

-(void)setOnlineStyle{
    self.offlineImageView.hidden = YES;
    for (GizConfigItem* item in self.currentConfig.config) {
        if([item isInOptionRange]){
            [self setHighLightStyle];
            return;
        }
    }
    [self setNorStyle];
}

-(void)setOfflineStyle{
    self.offlineImageView.hidden = NO;
    [self setIcon:self.currentConfig.offlineIcon];
    self.iconView.backgroundColor = [UIColor clearColor];
    self.imageView.tintColor = [UIColor whiteColor];
}

#pragma mark - GizConfigDelegate
-(void)deviceOnlineChange:(BOOL)is_online{
    if(self.currentConfig){
        if (is_online == YES) {
            [self setOnlineStyle];
        } else{
            [self setOfflineStyle];
        }
    }
}

-(void)deviceDataChange:(NSDictionary*)data{
    if(self.currentConfig){
        [self setOnlineStyle];
    }
}

-(void)deviceControlStateChange:(NSString *)stateName{
    self.titleLabel.text = stateName;
}

//#pragma mark - 设置功能Item样式
//-(void)setItemNorStyle{
//    self.iconView.backgroundColor = [UIColor colorWithRed:248/255.0 green:248/255.0 blue:248/255.0 alpha:1];
//}
//
//-(void)setItemHighLightStyle{
//    self.iconView.backgroundColor = [UIColor colorWithRed:255/255.0 green:178/255.0 blue:69/255.0 alpha:1];
//}

//#pragma mark - GizConfigItemDelegate
//-(void)attrValueChange{
//    if(self.currentConfigItem){
//        if([self.currentConfigItem isInOptionRange]){
//            [self setHighLightStyle];
//        } else{
//            [self setItemNorStyle];
//        }
//    }
//}

@end
