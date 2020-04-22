// Copyright (c) 2020 Boris Onchev
//
// Distributed under the Boost Software License, Version 1.0.
// (See accompanying file LICENSE_1_0.txt or http://www.boost.org/LICENSE_1_0.txt)
import 'dart:async';

import 'package:flutter/services.dart';

class FacebookSignInAccount {
  final String accessToken;
  final String userId;
  final DateTime expires;
  final List<String> permissions;
  final List<String> declinedPermissions;

  FacebookSignInAccount._fromMap(Map<String, dynamic> map)
      : accessToken = map['access_token'],
        userId = map['user_id'],
        expires = DateTime.fromMillisecondsSinceEpoch(
          map['expires'],
          isUtc: true,
        ),
        permissions = map['permissions'].cast<String>(),
        declinedPermissions = map['declined_permissions'].cast<String>();
}

class FacebookSignIn {
  static const MethodChannel _channel = MethodChannel('facebook_sign_in');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// Starts the interactive sign-in process.
  ///
  /// Returned Future resolves to an instance of [FacebookSignInAccount] for a
  /// successful sign in or `null` in case sign in process was aborted.
  ///
  /// Authentication process is triggered only if there is no currently signed in
  /// user (that is when `currentUser == null`), otherwise this method returns
  /// a Future which resolves to the same user instance.
  ///
  /// Re-authentication can be triggered only after [signOut] or [disconnect].
  /// Sign in the user with the requested read permissions.
  ///
  /// This will throw an exception from the native side if the [permissions]
  /// list contains any non read permissions.
  Future<FacebookSignInAccount> signIn(List<String> permissions) async {
    bool isCanceled(dynamic error) =>
        error is PlatformException && error.code == "cancelled";
    return _channel
        .invokeMethod('sign_in', {'permissions': permissions})
        .then((result) =>
        _deliverResult(
            FacebookSignInAccount._fromMap(result.cast<String, dynamic>())))
        .catchError((dynamic _) => null, test: isCanceled);
  }

  Future<void> signOut() async => _channel.invokeMethod('sign_out');

  /// There's a weird bug where calling Navigator.push (or any similar method)
  /// straight after getting a result from the method channel causes the app
  /// to hang.
  ///
  /// As a hack/workaround, we add a new task to the task queue with a slight
  /// delay, using the [Future.delayed] constructor.
  ///
  /// For more context, see this issue:
  /// https://github.com/roughike/flutter_facebook_login/issues/14
  Future<T> _deliverResult<T>(T result) {
    return Future.delayed(const Duration(milliseconds: 500), () => result);
  }
}
