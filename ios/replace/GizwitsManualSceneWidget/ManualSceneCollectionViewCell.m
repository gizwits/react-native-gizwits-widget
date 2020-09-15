//
//  ManualSceneCollectionViewCell.m
//  GizwitsManualSceneWidget
//
//  Created by william Zhang on 2020/2/17.
//  Copyright © 2020 650 Industries, Inc. All rights reserved.
//

#import "ManualSceneCollectionViewCell.h"

@interface ManualSceneCollectionViewCell ()<GizManualSceneDelegate>

@property (weak, nonatomic) IBOutlet UIView *iconView;

@property (weak, nonatomic) IBOutlet UIImageView *imageView;

@property (weak, nonatomic) IBOutlet UILabel *titleLabel;

@property (nonatomic, strong) NSString* iconUrl;

@property (nonatomic, strong) GizManualScene* currentScene;

@end

@implementation ManualSceneCollectionViewCell

-(void)awakeFromNib{
  [super awakeFromNib];
  self.tintColor = [UIColor colorWithRed:255/255.0 green:179/255.0 blue:46/255.0 alpha:1];
}

-(void)setScene:(GizManualScene *)scene{
    self.currentScene = scene;
    self.currentScene.delegate = self;
    NSString *bundlePath = [[NSBundle mainBundle].resourcePath stringByAppendingPathComponent:@"SceneIcon.bundle"];
    NSBundle *bundle = [NSBundle bundleWithPath:bundlePath];
    NSString *img_path = [bundle pathForResource:scene.icon ofType:@"png"];
    UIImage* icon = [UIImage imageWithContentsOfFile:img_path];
    icon = [icon imageWithRenderingMode:UIImageRenderingModeAlwaysTemplate];
    self.imageView.image = icon;
    self.imageView.contentMode = UIViewContentModeScaleAspectFit;
    self.titleLabel.text = self.currentScene.stateName;
    if(self.currentScene.highLight){
        [self setHighLightStyle];
    } else{
        [self setNorStyle];
    }
}

-(void)layoutSubviews
{
    [super layoutSubviews];
    self.iconView.layer.cornerRadius = 15;
}

#pragma mark - 设置样式
-(void)setNorStyle{
    self.iconView.backgroundColor = [UIColor clearColor];
    self.iconView.tintColor = [UIColor whiteColor];
}

-(void)setHighLightStyle{
    self.iconView.backgroundColor = [UIColor whiteColor];
    if(self.tintColor){
        self.iconView.tintColor = self.tintColor;
    }
}

#pragma mark - GizManualSceneDelegate
-(void)sceneStateChange:(NSString *)state highLight:(BOOL)highLight{
    dispatch_async(dispatch_get_main_queue(), ^{
         if(highLight){
               [self setHighLightStyle];
           } else{
               [self setNorStyle];
           }
        self.titleLabel.text = state;
    });
}

@end
