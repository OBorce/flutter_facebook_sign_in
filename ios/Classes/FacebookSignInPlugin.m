#import "FacebookSignInPlugin.h"
#if __has_include(<facebook_sign_in/facebook_sign_in-Swift.h>)
#import <facebook_sign_in/facebook_sign_in-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "facebook_sign_in-Swift.h"
#endif

@implementation FacebookSignInPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFacebookSignInPlugin registerWithRegistrar:registrar];
}
@end
