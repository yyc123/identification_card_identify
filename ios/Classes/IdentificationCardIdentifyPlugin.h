#import <Flutter/Flutter.h>

@interface IdentificationCardIdentifyPlugin : NSObject<FlutterPlugin>
@property (nonatomic, assign) UIViewController *hostViewController;
@property (nonatomic, copy)FlutterResult result;
@property (nonatomic, strong)NSString *imageDagaPath;

@end
