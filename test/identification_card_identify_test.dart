import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:identification_card_identify/identification_card_identify.dart';

void main() {
  const MethodChannel channel = MethodChannel('identification_card_identify');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await IdentificationCardIdentify.platformVersion, '42');
  });
}
