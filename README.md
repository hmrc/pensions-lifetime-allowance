
# pensions-lifetime-allowance

[![Apache-2.0 license](http://img.shields.io/badge/license-Apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html) [![Build Status](https://travis-ci.org/hmrc/pensions-lifetime-allowance.svg?branch=master)](https://travis-ci.org/hmrc/pensions-lifetime-allowance) [ ![Download](https://api.bintray.com/packages/hmrc/releases/pensions-lifetime-allowance/images/download.svg) ](https://bintray.com/hmrc/releases/pensions-lifetime-allowance/_latestVersion)

This is a backend microservice for Pensions Lifetime Allowance.

#### Included scripts

##### `run.sh`

* Starts the Play! server on [localhost:9011](http://localhost:9011) with test routes enabled.

##### `start-dependencies.sh`

* Starts required dependencies with [`service-manager`](https://github.com/hmrc/service-manager/)

### Start dependencies via Service Manager

To start all dependencies and services for pensions lifetime allowance, use one of the following commands:
```
sm --start PLA_ALL -r
sm --start PLA_DEP -r (starts only dependencies).
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").