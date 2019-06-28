import 'dart:async';

import 'package:flutter/services.dart';

class IdentificationCardIdentify {
  static const MethodChannel _channel =
      const MethodChannel('identification_card_identify');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  // static Future register(
  //     {String appId,
  //     bool doOnIOS: true,
  //     doOnAndroid: true,
  //     enableMTA: false}) async {
  //   return await _channel.invokeMethod("registerApp", {
  //     "appId": appId,
  //     "iOS": doOnIOS,
  //     "android": doOnAndroid,
  //     "enableMTA": enableMTA
  //   });
  // }

  // static Future initialize(String ak, String sk) async {
  //   return await _channel.invokeMethod("registerApp", {
  //     "AK": ak,
  //     "SK": sk,
  //   });
  // }

  static Future initialize(String ak, String sk) async {
    return await _channel.invokeMethod("Initialize", {
      "AK": ak,
      "SK": sk,
    });
  }

  static Future<Map> idcardIdentifyFont() async {
    Map result;
    try {
      result =
          await _channel.invokeMethod('IDCard_identify', 'CardTypeIdCardFont');
    } catch (e) {
      print('调用失败');
    }
    return result;
  }

  static Future<Map> idcardIdentifyBack() async {
    Map result;
    try {
      result =
          await _channel.invokeMethod('IDCard_identify', 'CardTypeIdCardBack');
    } catch (e) {
      print('调用失败');
    }
    return result;
  }

  static Future<Map> idcardIdentifyBankCard() async {
    Map result;
    try {
      result =
          await _channel.invokeMethod('IDCard_identify', 'CardTypeBankCard');
    } catch (e) {
      print('调用失败');
    }
    return result;
  }
}
