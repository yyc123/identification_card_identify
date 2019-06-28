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
    
    [self configCallback:cardType];
    UIViewController * vc;
    if ([cardType isEqualToString:@"CardTypeIdCardFont"]) {
       vc =  [self goCardTypeIdCardFont];
    } else if ([cardType isEqualToString:@"CardTypeBankCard"]){
        vc = [self goBankCardOCR];
    }
    else {
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
                                             [[AipOcrService shardService] detectIdCardBackFromImage:image withOptions:nil successHandler:_successHandler failHandler:_failHandler];
                                             
                                         }];
}

- (UIViewController *)goBankCardOCR {
    return  [AipCaptureCardVC ViewControllerWithCardType:CardTypeBankCard
                                         andImageHandler:^(UIImage *image) {
                                             NSString *path =  [self saveImage:image];
                                             self.imageDagaPath  = path;
                                             [[AipOcrService shardService] detectBankCardFromImage:image successHandler:_successHandler failHandler:_failHandler];
                                             
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

- (void)configCallback:(NSString *)cardType {
    __weak typeof(self) weakSelf = self;
    // 这是默认的识别成功的回调
    _successHandler = ^(id result){
        NSLog(@"%@", result);
        NSDictionary *words_result =result[@"words_result"];
        NSDictionary *resultV;
      
        if ([cardType isEqualToString:@"CardTypeIdCardFont"]) {
            if (!words_result[@"姓名"]||!words_result[@"住址"]||!words_result[@"出生"]||!words_result[@"公民身份号码"]) {
                [weakSelf dismissViewController:@{@"error":@"失败",@"code":@"-1"}];

                return ;
            }
            resultV = @{@"姓名":words_result[@"姓名"][@"words"],
                                      @"户籍地":words_result[@"住址"][@"words"],
                                      @"身份证号":words_result[@"公民身份号码"][@"words"],
                                      @"生日":words_result[@"出生"][@"words"],
                                      };
        }else if ([cardType isEqualToString:@"CardTypeIdCardBack"]) {
            if (!words_result[@"签发机关"]||!words_result[@"失效日期"]||!words_result[@"签发日期"]) {
                [weakSelf dismissViewController:@{@"error":@"失败",@"code":@"-1"}];

                return ;
            }
            resultV = @{@"签发机关":words_result[@"签发机关"][@"words"],
                        @"有效期":words_result[@"失效日期"][@"words"],
                        @"签发日期":words_result[@"签发日期"][@"words"],
                        };
        } else if ([cardType isEqualToString:@"CardTypeBankCard"]) {
            NSDictionary *words_result =result[@"result"];
            
            if (!words_result[@"bank_card_type"]||!words_result[@"bank_name"]||!words_result[@"bank_card_number"]) {
                [weakSelf dismissViewController:@{@"error":@"失败",@"code":@"-1"}];
                
                return ;
            }
            resultV = @{@"卡号":words_result[@"bank_card_number"],
                        @"类型":words_result[@"bank_card_type"],
                        @"发卡行":words_result[@"bank_name"],
                        };
        }
      
        [weakSelf dismissViewController:resultV.count > 0 ? @{@"image":self.imageDagaPath,@"result":resultV}: @{@"error":@"失败",@"code":@"-1"}];

    };
    
    _failHandler = ^(NSError *error){
        NSLog(@"%@", error);
        NSString *msg = [NSString stringWithFormat:@"%li:%@", (long)[error code], [error localizedDescription]];

        [weakSelf dismissViewController:@{@"error":@"失败",@"code":msg}];
    };
}

- (void)dismissViewController:(NSDictionary *)result {
    self.result(result);
    [self.hostViewController dismissViewControllerAnimated:YES completion:nil];

}

@end
