//
//  CtrlCollectionViewCell.m
//  GizwitsDeviceControlWidget
//
//  Created by william Zhang on 2020/4/10.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "CtrlCollectionViewCell.h"

@interface CtrlCollectionViewCell ()<GizConfigItemDelegate>

@property (weak, nonatomic) IBOutlet UIView *bgView;

@property (weak, nonatomic) IBOutlet UIImageView *iconView;

@property (weak, nonatomic) IBOutlet UILabel *nameLabel;

@property (nonatomic, strong) NSString* iconUrl;

@property (nonatomic, strong) GizConfigItem* currentConfigItem;

@end

@implementation CtrlCollectionViewCell

- (void)awakeFromNib {
    [super awakeFromNib];
    self.nameLabel.textColor = [UIColor colorWithRed:51/255.0 green:51/255.0 blue:51/255.0 alpha:1];
    self.nameLabel.adjustsFontSizeToFitWidth = YES;
    self.tintColor = [UIColor colorWithRed:255/255.0 green:179/255.0 blue:46/255.0 alpha:1];
    // Initialization code
}

-(void)layoutSubviews
{
    [super layoutSubviews];
    self.bgView.layer.cornerRadius = (self.frame.size.width-6)/2;
}

-(void)setConfigItem:(GizConfigItem *)configItem{
    self.currentConfigItem = configItem;
    self.currentConfigItem.delegate = self;
    [self.nameLabel setText:self.currentConfigItem.editName];
    [self setIcon:[self.currentConfigItem currentControlIcon]];
    if ([configItem isInOptionRange]) {
        [self setItemHighLightStyle];
    } else{
        [self setItemNorStyle];
    }
}

-(void)setIcon:(NSString *)url{
    if(self.iconUrl && [self.iconUrl isEqualToString:url]){
        return;
    }
    self.iconUrl = url;
    NSData* data = [NSData dataWithContentsOfURL:[NSURL URLWithString:self.iconUrl]];
    UIImage* image = [UIImage imageWithData:data];
    image = [image imageWithRenderingMode:UIImageRenderingModeAlwaysTemplate];
    [self.iconView setImage:image];
}


#pragma mark - 设置功能Item样式
-(void)setItemNorStyle{
    self.bgView.backgroundColor = [UIColor colorWithRed:248/255.0 green:248/255.0 blue:248/255.0 alpha:1];
    self.bgView.layer.borderColor = [UIColor colorWithRed:51/255.0 green:51/255.0 blue:51/255.0 alpha:1].CGColor;
    self.bgView.layer.borderWidth = 0.5;
    self.iconView.tintColor = [UIColor colorWithRed:51/255.0 green:51/255.0 blue:51/255.0 alpha:1];
}

-(void)setItemHighLightStyle{
    self.bgView.backgroundColor = self.tintColor;
    self.bgView.layer.borderWidth = 0;
    self.iconView.tintColor = [UIColor whiteColor];
}

#pragma mark - GizConfigItemDelegate
-(void)attrValueChange{
    if(self.currentConfigItem){
        if([self.currentConfigItem isInOptionRange]){
            [self setItemHighLightStyle];
        } else{
            [self setItemNorStyle];
        }
    }
}

@end
