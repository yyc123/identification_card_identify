 #import "IdentificationCardIdentifyPlugin.h"
 #import <AipOcrSdk/AipOcrSdk.h>

// 默认的识别成功的回调
void (^_successHandler)(id);
// 默认的识别失败的回调
void (^_failHandler)(NSError *);
@implementation IdentificationCardIdentifyPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"identification_card_identify"
            binaryMessenger:[registrar messenger]];
  IdentificationCardIdentifyPlugin* instance = [[IdentificationCardIdentifyPlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
    instance.hostViewController = [UIApplication sharedApplication].delegate.window.rootViewController;

}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getPlatformVersion" isEqualToString:call.method]) {
    result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
  }else if ([@"Initialize" isEqualToString:call.method]){
      if (call.arguments) {
          NSDictionary *arguments = call.arguments;
          [[AipOcrService shardService] authWithAK:arguments[@"AK"] andSK:arguments[@"SK"]];
      }
      //      self.result = result;
  } else if ([@"IDCard_identify" isEqualToString:call.method]){
      if (call.arguments) {
          NSString *type = call.arguments;
          [self cardOCR:type];
      }
      self.result = result;
  }else {
      result(FlutterMethodNotImplemented);

  }

}

- (void)cardOCR:(NSString *)cardType {
    
    [self configCallback];
    UIViewController * vc;
    if ([cardType isEqualToString:@"CardTypeIdCardFont"]) {
       vc =  [self goCardTypeIdCardFont];
    }else {
        vc = [self goCardTypeIdCardBack];
        
        
    }
    [self.hostViewController presentViewController:vc animated:YES completion:nil];
}

- (UIViewController *)goCardTypeIdCardFont {
   return  [AipCaptureCardVC ViewControllerWithCardType:CardTypeIdCardFont
                                 andImageHandler:^(UIImage *image) {
                                     NSString *path =  [self saveImage:image];
                                     self.imageDagaPath  = path;
                                     [[AipOcrService shardService] detectIdCardFrontFromImage:image
                                                                                  withOptions:nil
                                                                               successHandler:_successHandler
                                                                                  failHandler:_failHandler];
                                 }];
}


- (UIViewController *)goCardTypeIdCardBack {
   return  [AipCaptureCardVC ViewControllerWithCardType:CardTypeIdCardBack
                                 andImageHandler:^(UIImage *image) {
                                    NSString *path =  [self saveImage:image];
                                     self.imageDagaPath  = path;
                                     [[AipOcrService shardService] detectIdCardBackFromImage:image
                                                                                 withOptions:nil
                                                                              successHandler:_successHandler
                                                                                 failHandler:_failHandler];
                                 }];
}

- (NSString *)saveImage:(UIImage*)image {
    NSData *data = UIImagePNGRepresentation(image);
    NSString *documents = [NSHomeDirectory() stringByAppendingPathComponent:@"Documents"];
    //拼接文件绝对路径
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"HH:mm:ss"];
    NSDate *datenow = [NSDate date];
    NSString *currentTimeString = [formatter stringFromDate:datenow];
    NSString *path = [documents stringByAppendingPathComponent:[NSString stringWithFormat:@"%@.png",currentTimeString]];
    //保存
    [data writeToFile:path atomically:YES];
    return path;
}

- (void)configCallback {
    __weak typeof(self) weakSelf = self;
    // 这是默认的识别成功的回调
    _successHandler = ^(id result){
        NSLog(@"%@", result);
        self.result(@{@"image":self.imageDagaPath,@"result":result[@"words_result"]});
        [weakSelf.hostViewController dismissViewControllerAnimated:YES completion:nil];

    };
    
    _failHandler = ^(NSError *error){
        NSLog(@"%@", error);
        NSString *msg = [NSString stringWithFormat:@"%li:%@", (long)[error code], [error localizedDescription]];
        self.result(@{@"error":@"失败",@"code":msg});
        [weakSelf.hostViewController dismissViewControllerAnimated:YES completion:nil];

    };
}

@end
