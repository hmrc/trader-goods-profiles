#!/bin/sh

sm2 --start AUTH_LOGIN_API \
 AUTH_LOGIN_STUB \
 AUTH \
 USER_DETAILS \
 ASSETS_FRONTEND_2 \
 IDENTITY_VERIFICATION

sbt "run -Drun.mode=Dev -Dhttp.port=10902 $*"