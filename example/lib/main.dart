import 'dart:async';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:identification_card_identify/identification_card_identify.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  Map callBack = {};
  @override
  void initState() {
    super.initState();
    initPlatformState();
    IdentificationCardIdentify.initialize(
        'VeXfVI6rrsMBKtY0RQTeYGEZ', '6jBZCkMyasrx8ZZEMRjSUGtsbZngKfMq');
  }

  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await IdentificationCardIdentify.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }
    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    if (callBack.length > 0) {
      String fileString = callBack['image'];

      Image _currentImage = Image.file(File(fileString));
      dynamic strResult = callBack['result'];

      return MaterialApp(
        home: Scaffold(
          body: ListView(
            children: <Widget>[
              Text(strResult.toString()),
              _currentImage,
            ],
          ),
        ),
      );
    }
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: <Widget>[
              Text('Running on: $_platformVersion\n'),
              FlatButton(
                child: Text('身份证识别'),
                onPressed: () {
                  IdentificationCardIdentify.idcardIdentifyFont()
                      .then((result) {
                    setState(() {
                      callBack = result;
                    });
                  });
                },
              ),
              FlatButton(
                child: Text('反面'),
                onPressed: () {
                  IdentificationCardIdentify.idcardIdentifyBack()
                      .then((result) {
                    setState(() {
                      callBack = result;
                    });
                  });
                },
              )
            ],
          ),
        ),
      ),
    );
  }
}
